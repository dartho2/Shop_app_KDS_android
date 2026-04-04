package com.itsorderkds.ui.settings.printer
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.os.IBinder
import timber.log.Timber
import java.net.Socket
import java.nio.charset.Charset

// IMPORTY AIDL
import com.senraise.printer.IService
import recieptservice.com.recieptservice.PrinterInterface
import woyou.aidlservice.jiuiv5.IWoyouService
import java.io.FileOutputStream

class AidlPrinterService(private val context: Context) {

    private var senraiseService: IService? = null
    private var cloneService: PrinterInterface? = null
    private var woyouService: IWoyouService? = null

    private var isConnected = false
    private var currentServiceType = ServiceType.NONE

    enum class ServiceType {
        NONE, SENRAISE, CLONE, WOYOU
    }

    fun currentType(): ServiceType = currentServiceType

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                val pkgName = name?.packageName ?: ""

                Timber.d("🔗 ===== POŁĄCZENIE Z DRUKARKĄ AIDL =====")
                Timber.d("📦 Pakiet: $pkgName")

                if (pkgName.contains("recieptservice")) {
                    cloneService = PrinterInterface.Stub.asInterface(service)
                    currentServiceType = ServiceType.CLONE
                    Timber.d("✅ Typ: KLON (recieptservice)")
                }
                else if (pkgName.contains("senraise")) {
                    senraiseService = IService.Stub.asInterface(service)
                    currentServiceType = ServiceType.SENRAISE
                    Timber.d("✅ Typ: SENRAISE")
                }
                else if (pkgName.contains("woyou") || pkgName.contains("sunmi")) {
                    woyouService = IWoyouService.Stub.asInterface(service)
                    currentServiceType = ServiceType.WOYOU
                    Timber.d("✅ Typ: SUNMI/WOYOU")
                }
                else {
                    Timber.w("⚠️  Typ: NIEZNANY - $pkgName")
                }

                isConnected = true
            } catch (@Suppress("SwallowedException") e: Exception) {
                Timber.e(e, "❌ BŁĄD PODCZAS POŁĄCZENIA")
                isConnected = false
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.d("❌ ROZŁĄCZENIE Z DRUKARKĄ AIDL")
            isConnected = false
            currentServiceType = ServiceType.NONE
            cloneService = null
            senraiseService = null
            woyouService = null
        }
    }

    fun connect() {
        if (isConnected) return

        Timber.d("🔄 Łączenie z usługą...")

        // Priorytetyzacja prób połączenia
        if (isConnected) return
        Timber.d("🔄 Łączenie z usługą...")

        // ✅ NAJPIERW ORYGINAŁ
        if (bindToSenraiseOriginal()) return

        // potem klon
        if (bindToCloneExplicit()) return

        // potem Sunmi
        if (bindToSunmi()) return

        Timber.e("❌ Nie udało się połączyć z żadną znaną usługą.")
    }

    private fun bindToCloneExplicit(): Boolean {
        val intent = Intent()
        intent.component = ComponentName(
            "recieptservice.com.recieptservice",
            "recieptservice.com.recieptservice.service.PrinterService"
        )
        return tryBind(intent, "Klon H10")
    }

    private fun bindToSenraiseOriginal(): Boolean = tryBind(
        Intent().apply {
            setPackage("com.senraise.printer")
            action = "com.senraise.printer.IService"
        }, "Senraise"
    )

    private fun bindToSunmi(): Boolean = tryBind(
        Intent().apply {
            setPackage("woyou.aidlservice.jiuiv5")
            action = "woyou.aidlservice.jiuiv5.IWoyouService"
        }, "Sunmi"
    )

    private fun tryBind(intent: Intent, name: String): Boolean {
        return try {
            val result = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            Timber.d("   Próba $name: $result")
            result
        } catch (@Suppress("SwallowedException") e: Exception) {
            false
        }
    }

    fun disconnect() {
        try { context.unbindService(connection) } catch (_: Exception) {}
        isConnected = false
        cloneService = null
        senraiseService = null
        woyouService = null
        Timber.d("🔌 Rozłączono")
    }

    fun printText(text: String, autoCut: Boolean = false): Boolean {
        Timber.d("🖨️ AIDL print start, serviceType=$currentServiceType")

        if (!isConnected) {
            val connectStartTime = System.currentTimeMillis()
            connect()

            // OPTYMALIZACJA: Czekaj maksymalnie 500ms (zamiast 800ms)
            // Sprawdzaj co 50ms czy już połączony
            var elapsed = 0L
            val checkInterval = 50L
            val maxWait = 500L

            while (!isConnected && elapsed < maxWait) {
                Thread.sleep(checkInterval)
                elapsed = System.currentTimeMillis() - connectStartTime
            }

            if (isConnected) {
                Timber.d("✅ AIDL connected after ${elapsed}ms (saved ${800 - elapsed}ms)")
            } else {
                Timber.w("⚠️ AIDL connection timeout after ${elapsed}ms")
            }
        }

        if (!isConnected) {
            return printViaRawSocket(text, autoCut = autoCut)
        }

        return try {
            when (currentServiceType) {
                ServiceType.CLONE -> handleClonePrint(text, autoCut)

                ServiceType.SENRAISE -> {
                    senraiseService?.let { s ->
                        Timber.d("📋 [SENRAISE] Używam parsera formatowania AIDL")
                        val segments = com.itsorderkds.ui.settings.print.AidlFormattingParser.parse(text)
                        val success = com.itsorderkds.ui.settings.print.AidlFormattingRenderer.renderSenraise(
                            service = s,
                            segments = segments,
                            autoCut = autoCut
                        )
                        return success
                    }
                    false
                }

                ServiceType.WOYOU -> {
                    woyouService?.let { s ->
                        Timber.d("📋 [WOYOU] Używam parsera formatowania AIDL")
                        val segments = com.itsorderkds.ui.settings.print.AidlFormattingParser.parse(text)
                        val success = com.itsorderkds.ui.settings.print.AidlFormattingRenderer.renderWoyou(
                            service = s,
                            segments = segments,
                            autoCut = autoCut
                        )
                        return success
                    }
                    false
                }

                else -> false
            }
        } catch (e: Exception) {
            Timber.e(e, "Błąd druku AIDL")
            false
        }
    }





    /**
     * BEZPIECZNA obsługa KLONA – drukuj z formatowaniem przez parser AIDL
     *
     * NOWA IMPLEMENTACJA (2026-01-24):
     * - Parsuje tagi ESC/POS ([C], <b>, <font>, etc.)
     * - Renderuje przez dedykowane wywołania AIDL (setAlignment, setTextBold, etc.)
     * - Wspiera pełne formatowanie: centrowanie, pogrubienie, podwójną szerokość/wysokość
     */
    private fun handleClonePrint(text: String, autoCut: Boolean): Boolean {
        val s = cloneService ?: return false

        return runCatching {
            Timber.d("🧾 [CLONE] version=${s.getServiceVersion()}")
            Timber.d("📋 [CLONE] Używam parsera formatowania AIDL")

            // 1. Parsuj tekst z tagami ESC/POS na segmenty
            val segments = com.itsorderkds.ui.settings.print.AidlFormattingParser.parse(text)

            Timber.d("✅ [CLONE] Sparsowano ${segments.size} segmentów")

            // 2. Renderuj segmenty przez AIDL interface
            val success = com.itsorderkds.ui.settings.print.AidlFormattingRenderer.renderClone(
                service = s,
                segments = segments,
                autoCut = autoCut
            )

            if (success) {
                Timber.d("✅ [CLONE] Renderowanie zakończone sukcesem")
            } else {
                Timber.e("❌ [CLONE] Renderowanie nieudane")
            }

            success

        }.onFailure {
            Timber.e(it, "❌ [CLONE] print failed")
        }.getOrDefault(false)
    }




    // === DIAGNOSTYKA SYSTEMOWA ===

    /**
     * Sprawdza uprawnienia i zabezpieczenia pakietu drukarki.
     * WAŻNE: Jeśli serwis wymaga signature/privileged, Twoja apka może być zablokowana!
     */
//    @Suppress("DEPRECATION")
//    fun dumpServiceSecurity(packageName: String = "recieptservice.com.recieptservice") {
//        val pm = context.packageManager
//        runCatching {
//            // GET_SERVICES = 4, GET_PERMISSIONS = 4096
//            val pi: PackageInfo = pm.getPackageInfo(packageName, 4 or 4096)
//            Timber.e("=== 🔒 PACKAGE SECURITY DUMP: $packageName ===")
//
//            // Wypisz wszystkie serwisy i ich uprawnienia
//            val servicesSnapshot = pi.services?.toList().orEmpty()
//            if (servicesSnapshot.isEmpty()) {
//                Timber.e("⚠️ Brak zarejestrowanych serwisów w tym pakiecie!")
//            } else {
//                servicesSnapshot.forEach { s ->
//                    Timber.e("📦 service=${s.name}")
//                    Timber.e("   exported=${s.exported}")
//                    Timber.e("   permission=${s.permission ?: "BRAK (public)"}")
//                }
//            }
//
//            // Wypisz requested permissions
//            val permissionsSnapshot = pi.requestedPermissions?.toList().orEmpty()
//            if (permissionsSnapshot.isEmpty()) {
//                Timber.e("⚠️ Brak requested permissions")
//            } else {
//                permissionsSnapshot.forEach { p ->
//                    Timber.e("🔑 requestedPerm=$p")
//                }
//            }
//
//            Timber.e("=== END SECURITY DUMP ===")
//        }.onFailure {
//            Timber.e(it, "❌ dumpServiceSecurity($packageName) failed - pakiet nie istnieje lub brak dostępu")
//        }
//    }

    /**
     * Wypisuje wszystkie dostępne serwisy w danym pakiecie.
     * Używaj tego, aby znaleźć właściwą nazwę klasy serwisu do bind.
     */
//    @Suppress("DEPRECATION")
//    fun dumpPackageServices(packageName: String) {
//        val pm = context.packageManager
//        runCatching {
//            // GET_SERVICES = 4
//            val pi: PackageInfo = pm.getPackageInfo(packageName, 4)
//            Timber.e("=== 📋 SERVICES in $packageName ===")
//
//            val servicesSnapshot = pi.services?.toList().orEmpty()
//            if (servicesSnapshot.isEmpty()) {
//                Timber.e("⚠️ Pakiet istnieje, ale NIE MA zarejestrowanych serwisów!")
//                Timber.e("   To może oznaczać że:")
//                Timber.e("   - Pakiet nie jest driverem drukarki")
//                Timber.e("   - Serwisy są ukryte (require system signature)")
//                Timber.e("   - Manifest pakietu nie eksportuje serwisów")
//            } else {
//                servicesSnapshot.forEach { s ->
//                    Timber.e("📦 service=${s.name}")
//                    Timber.e("   exported=${s.exported}")
//                    Timber.e("   permission=${s.permission ?: "BRAK"}")
//                }
//            }
//
//            Timber.e("=== END SERVICES DUMP ===")
//        }.onFailure {
//            Timber.e(it, "❌ dumpPackageServices($packageName) failed")
//        }
//    }

    /**
     * Diagnostyka wszystkich znanych pakietów drukarki.
     * Odpal to JEDEN RAZ po starcie ekranu.
     */
//    fun runFullDiagnostics() {
//        Timber.e("🔍 === ROZPOCZYNAM PEŁNĄ DIAGNOSTYKĘ DRUKARKI ===")
//
//        // Sprawdź wszystkie znane pakiety
//        listOf(
//            "recieptservice.com.recieptservice",
//            "com.senraise.printer",
//            "woyou.aidlservice.jiuiv5"
//        ).forEach { pkg ->
//            dumpPackageServices(pkg)
//            dumpServiceSecurity(pkg)
//        }
//
//        Timber.e("🔍 === KONIEC DIAGNOSTYKI ===")
//    }

    /**
     * Próba explicit bind przez ComponentName (zamiast action).
     * Użyj tego, gdy już wiesz dokładną nazwę klasy serwisu z dumpPackageServices.
     */
    fun bindToServiceExplicit(packageName: String, serviceClassName: String, name: String = "Explicit"): Boolean {
        val intent = Intent().apply {
            component = ComponentName(packageName, serviceClassName)
        }
        return tryBind(intent, name)
    }

    @Suppress("UNUSED_PARAMETER")
    fun printViaRawSocket(text: String, autoCut: Boolean = false, host: String = "127.0.0.1", port: Int = 9100): Boolean {
        return try {
            Socket(host, port).use { socket ->
                socket.getOutputStream().use { out ->
                    // ESC @ (reset)
                    out.write(byteArrayOf(0x1B, 0x40))

                    // ESC t 0 (encoding)
                    out.write(byteArrayOf(0x1B, 0x74, 0x00))

                    // Tekst
                    out.write(text.toByteArray(Charset.forName("UTF-8")))

                    // Feed
                    out.write("\n\n\n".toByteArray())

                    if (autoCut) {
                        out.write(byteArrayOf(0x1D, 0x56, 0x00))
                    }

                    out.flush()
                }
            }
            Timber.d("✅ Raw socket druk (port $port) powiódł się")
            true
        } catch (e: Exception) {
            Timber.w(e, "⚠️ Raw socket druk nie powiódł się: ${e.message}")
            false
        }
    }

    fun printHelloViaPrintManager(text: String = "HELLO 123\n") {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal,
                callback: LayoutResultCallback,
                extras: android.os.Bundle?
            ) {
                val info = PrintDocumentInfo.Builder("hello.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal,
                callback: WriteResultCallback
            ) {
                val pdf = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(384, 800, 1).create()
                val page = pdf.startPage(pageInfo)
                val canvas: Canvas = page.canvas
                val paint = Paint().apply { textSize = 24f }
                var y = 40f
                text.split("\n").forEach { line ->
                    canvas.drawText(line, 10f, y, paint)
                    y += 30f
                }
                pdf.finishPage(page)
                FileOutputStream(destination.fileDescriptor).use { out ->
                    pdf.writeTo(out)
                }
                pdf.close()
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            }
        }

        val attrs = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.UNKNOWN_PORTRAIT)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print("HELLO_TEST", adapter, attrs)
    }
}
