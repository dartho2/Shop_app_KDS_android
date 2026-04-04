package com.itsorderkds.ui.settings.print

import android.content.Context
import android.widget.Toast
import com.dantsu.escposprinter.EscPosCharsetEncoding
import com.dantsu.escposprinter.EscPosPrinter
import com.itsorderkds.data.model.Order
import com.itsorderkds.data.model.Printer
import com.itsorderkds.data.preferences.AppPreferencesManager
import com.itsorderkds.data.preferences.PrinterPreferences
import com.itsorderkds.ui.settings.printer.PrinterManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrinterService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferencesManager: AppPreferencesManager
) {
    enum class DocumentType { KITCHEN_TICKET, RECEIPT, TEST }

    private val connectionManager = PrinterConnectionManager()
    private val printMutex = Mutex()

    // ✅ DEDUPLIKACJA: Blokuje wielokrotne drukowanie tego samego zamówienia (backend emituje 11x duplikatów!)
    private val printAfterAcceptedMutex = Mutex()
    private val recentPrintAfterAccepted = mutableMapOf<String, Long>() // orderId -> timestamp ostatniego drukowania
    private val printAfterAcceptedWindowMs = 5000L // 5 sekund - jeśli to samo zamówienie w tym oknie = blokuj

    data class TargetConfig(
        val printer: Printer,
        val lineChars: Int = 32
    )

    private suspend fun resolveTargetsFor(doc: DocumentType): List<TargetConfig> {
        val printers = PrinterPreferences.getPrinters(context)
        val enabled = printers.filter { it.enabled && it.deviceId.isNotBlank() }
        val kitchen = enabled.filter { it.printerType == com.itsorderkds.data.model.PrinterType.KITCHEN }
        val standard = enabled.filter { it.printerType == com.itsorderkds.data.model.PrinterType.STANDARD }
        val test = enabled

        // Sprawdź czy drukowanie na kuchni jest włączone
        val autoPrintKitchenEnabled = appPreferencesManager.getAutoPrintKitchenEnabled()

        val selected = when (doc) {
            DocumentType.KITCHEN_TICKET -> if (autoPrintKitchenEnabled) kitchen else emptyList()
            DocumentType.RECEIPT -> standard
            DocumentType.TEST -> test
        }
        return selected.sortedBy { it.order }.map { TargetConfig(it) }
    }

    private suspend fun getConnectionFor(printer: Printer): com.dantsu.escposprinter.connection.DeviceConnection? {
        Timber.d("📍 getConnectionFor: type=${printer.connectionType}, deviceId=${printer.deviceId}")

        return when (printer.connectionType) {
            com.itsorderkds.data.model.PrinterConnectionType.BLUETOOTH -> {
                // Bluetooth: używamy starej metody z PrinterManager
                Timber.d("📶 Łączę się przez Bluetooth: ${printer.deviceId}")
                var conn = PrinterManager.getConnectionById(context, printer.deviceId)
                if (conn == null) {
                    Timber.d("⚠️ Retry BT after 200ms for ${printer.deviceId}")
                    delay(200)  // OPTYMALIZACJA: zmniejszone z 500ms → oszczędność 300ms
                    conn = PrinterManager.getConnectionById(context, printer.deviceId)
                }
                if (conn != null) {
                    Timber.d("✅ getConnectionFor: Bluetooth OK")
                } else {
                    Timber.w("❌ getConnectionFor: Bluetooth FAIL")
                }
                conn
            }
            com.itsorderkds.data.model.PrinterConnectionType.NETWORK -> {
                // Sieć: tworzymy TCP connection
                try {
                    Timber.d("🌐 Tworzę połączenie sieciowe: ${printer.networkIp}:${printer.networkPort}")
                    val conn = PrinterConnectionManager.createConnection(printer)
                    Timber.d("✅ getConnectionFor: Network connection utworzone")
                    conn
                } catch (e: Exception) {
                    Timber.e(e, "❌ getConnectionFor: Network connection failed")
                    null
                }
            }
            com.itsorderkds.data.model.PrinterConnectionType.BUILTIN -> {
                // Wbudowana: drukujemy przez port szeregowy
                Timber.d("🖨️ Wbudowana drukarka (port szeregowy): ${printer.deviceId}")
                // Dla portów szeregowych nie zwracamy DeviceConnection, ale null
                // Drukowanie obsługujemy inaczej (patrz printOneSerial)
                null
            }
        }
    }

    private fun toCharsetEncoding(p: Printer): EscPosCharsetEncoding {
        return EscPosCharsetEncoding(p.encoding, p.codepage ?: 255)
    }

    internal suspend fun printOne(target: TargetConfig, order: Order, useDeliveryInterval: Boolean) {
        val cfg = target.printer

        // MONITORING: Rozpocznij pomiar czasu
        val printStartTime = System.currentTimeMillis()

        // Sprawdź czy to drukarka na porcie szeregowym
        if (cfg.connectionType == com.itsorderkds.data.model.PrinterConnectionType.BUILTIN) {
            Timber.d("🖨️ Drukarka wbudowana (serial port)")
            printOneSerial(target, order, useDeliveryInterval)

            // MONITORING: Zakończ pomiar
            val printDuration = System.currentTimeMillis() - printStartTime
            Timber.d("⏱️ Print duration (BUILTIN): ${printDuration}ms, printer=${cfg.name}")
            return
        }

        Timber.d("🔒 PrinterSTEP: [ENTRY] target=${target.printer.printerType} order=${order.orderNumber}")
        val connection = getConnectionFor(cfg)
        if (connection == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Nie udało się połączyć z drukarką (${cfg.name})", Toast.LENGTH_SHORT).show()
            }
            Timber.w("❌ PrinterSTEP: brak połączenia ${cfg.deviceId}")

            // MONITORING: Błąd połączenia
            val failDuration = System.currentTimeMillis() - printStartTime
            Timber.e("⏱️ Print FAILED after ${failDuration}ms, printer=${cfg.name}, reason=no_connection")
            return
        }

        try {
            val template = PrintTemplate.fromId(cfg.templateId)
            val ticket = PrintTemplateFactory.buildTicket(context, order, template, useDeliveryInterval)
            val escCharset = toCharsetEncoding(cfg)
            Timber.d("✅ Dokument gotowy (template=${cfg.templateId}, autoCut=${cfg.autoCut})")

            connectionManager.withConnection(connection, cfg) { conn ->
                val printer = EscPosPrinter(conn, 203, 58f, target.lineChars, escCharset)
                if (cfg.autoCut) {
                    printer.printFormattedTextAndCut(ticket)
                } else {
                    printer.printFormattedText(ticket)
                }
            }

            // MONITORING: Sukces drukowania
            val printDuration = System.currentTimeMillis() - printStartTime
            Timber.d("✅ Wydruk zakończony ${cfg.name}")
            Timber.d("⏱️ Print duration (SUCCESS): ${printDuration}ms, printer=${cfg.name}, type=${cfg.connectionType}")

        } catch (e: Exception) {
            // MONITORING: Błąd drukowania
            val printDuration = System.currentTimeMillis() - printStartTime
            Timber.e(e, "❌ Błąd drukowania na ${cfg.name}")
            Timber.e("⏱️ Print duration (ERROR): ${printDuration}ms, printer=${cfg.name}, error=${e.message}")

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Błąd drukowania: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            try {
                connection.disconnect()
                delay(200)
            } catch (e: Exception) {
                Timber.w(e, "⚠️ disconnect error ${cfg.deviceId}")
            }
            Timber.d("🔓 PrinterSTEP: [EXIT] target=${cfg.printerType}")
        }
    }

    suspend fun printOrder(order: Order, useDeliveryInterval: Boolean = false, docType: DocumentType = DocumentType.RECEIPT) {
        printMutex.lock()
        try {
            val targets = resolveTargetsFor(docType)
            if (targets.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Brak skonfigurowanej drukarki", Toast.LENGTH_SHORT).show()
                }
                return
            }
            for (t in targets) {
                printOne(t, order, useDeliveryInterval)
                // mała przerwa między drukarkami (dual-mode BT)
                delay(500)
            }
        } finally {
            printMutex.unlock()
        }
    }

    suspend fun printKitchenTicket(order: Order, useDeliveryInterval: Boolean = false) {
        printOrder(order, useDeliveryInterval, DocumentType.KITCHEN_TICKET)
    }

    suspend fun printReceipt(order: Order, useDeliveryInterval: Boolean = false) {
        printOrder(order, useDeliveryInterval, DocumentType.RECEIPT)
    }

    /**
     * Drukuje zamówienie na konkretnej wybranej drukarce (manual print)
     */
    suspend fun printOrderOnPrinter(order: Order, printerIndex: Int) {
        printMutex.lock()
        try {
            val printers = PrinterPreferences.getPrinters(context)
            val enabled = printers.filter { it.enabled && it.deviceId.isNotBlank() }

            if (printerIndex < 0 || printerIndex >= enabled.size) {
                Timber.w("❌ Nieistniejący indeks drukarki: $printerIndex")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Błąd: drukarka nie znaleziona", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val selectedPrinter = enabled[printerIndex]
            Timber.d("🖨️ Drukowanie na wybranej drukarce: ${selectedPrinter.name} (index=$printerIndex)")

            val targetConfig = TargetConfig(selectedPrinter)
            printOne(targetConfig, order, useDeliveryInterval = false)

            Timber.d("✅ Drukowanie na drukarce ${selectedPrinter.name} zakończone")
        } finally {
            printMutex.unlock()
        }
    }

    /**
     * ✅ NOWE: Drukuje zamówienie na WSZYSTKICH włączonych drukarkach
     */
    suspend fun printOrderOnAllPrinters(order: Order) {
        printMutex.lock()
        try {
            val printers = PrinterPreferences.getPrinters(context)
            val enabled = printers.filter { it.enabled && it.deviceId.isNotBlank() }

            if (enabled.isEmpty()) {
                Timber.w("❌ Brak włączonych drukarek")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Brak skonfigurowanych drukarek", Toast.LENGTH_SHORT).show()
                }
                return
            }

            Timber.d("🖨️ Rozpoczynam drukowanie na wszystkich drukarkach (${enabled.size})")

            // Drukuj na każdej drukarce z małą przerwą między nimi
            enabled.forEachIndexed { index, printer ->
                Timber.d("🖨️ [$index/${enabled.size}] Drukowanie na: ${printer.name}")

                val targetConfig = TargetConfig(printer)
                try {
                    printOne(targetConfig, order, useDeliveryInterval = false)
                    Timber.d("✅ [$index/${enabled.size}] Drukowanie na ${printer.name} zakończone")
                } catch (e: Exception) {
                    Timber.e(e, "❌ Błąd drukowania na ${printer.name}")
                    // Kontynuuj mimo błędu - drukuj na pozostałych
                }

                // Przerwa między drukarkami (dla dual-mode BT)
                if (index < enabled.size - 1) {
                    delay(500)
                }
            }

            Timber.d("✅ Drukowanie na wszystkich drukarkach zakończone")

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Wydrukowano na ${enabled.size} ${if (enabled.size == 1) "drukarce" else "drukarkach"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } finally {
            printMutex.unlock()
        }
    }

    /**
     * Drukuje zamówienie po zaakceptowaniu - używa printOrder z odpowiednim docType
     * Drukuje na drukarce standardowej, a jeśli włączone drukowanie na kuchni - również tam
     */
    suspend fun printAfterOrderAccepted(order: Order) {
        // ✅ DEDUPLIKACJA #1: Blokuj wielokrotne wywołania tego samego zamówienia (backend emituje 11x duplikatów!)
        val now = System.currentTimeMillis()
        val shouldBlock = printAfterAcceptedMutex.withLock {
            val lastPrintTime = recentPrintAfterAccepted[order.orderId]
            if (lastPrintTime != null && (now - lastPrintTime) < printAfterAcceptedWindowMs) {
                // Zablokuj - to samo zamówienie było drukowane niedawno
                val elapsed = now - lastPrintTime
                Timber.tag("PRINT_DEBUG").w("⏭️ DUPLIKAT ZABLOKOWANY! orderId=${order.orderId}, elapsed=${elapsed}ms (okno=${printAfterAcceptedWindowMs}ms)")
                true
            } else {
                // Dozwolone - zapisz timestamp
                recentPrintAfterAccepted[order.orderId] = now

                // Cleanup starych wpisów (starsze niż 10s)
                val cleanupThreshold = now - 10000L
                recentPrintAfterAccepted.entries.removeIf { it.value < cleanupThreshold }

                false
            }
        }

        if (shouldBlock) {
            return // Zablokowane - to duplikat!
        }

        val autoPrintAccepted = appPreferencesManager.getAutoPrintAcceptedEnabled()
        val printersSelection = appPreferencesManager.getAutoPrintAcceptedPrinters()

        Timber.tag("PRINT_DEBUG").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag("PRINT_DEBUG").w("📋 printAfterOrderAccepted START")
        Timber.tag("PRINT_DEBUG").w("   ├─ order: %s", order.orderNumber)
        Timber.tag("PRINT_DEBUG").w("   ├─ enabled: %s", autoPrintAccepted)
        Timber.tag("PRINT_DEBUG").w("   ├─ printers: %s", printersSelection)
        Timber.tag("PRINT_DEBUG").w("   └─ Thread: ${Thread.currentThread().name}")
        Timber.tag("PRINT_DEBUG").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        if (!autoPrintAccepted) {
            Timber.tag("PRINT_DEBUG").d("⏭️ Automatyczne drukowanie po zaakceptowaniu wyłączone – pomijam")
            return
        }

        printMutex.lock()
        try {
            when (printersSelection) {
                "main" -> {
                    Timber.tag("PRINT_DEBUG").w("🖨️ WYBRANO: Drukowanie tylko na drukarce głównej")
                    val standardTargets = resolveTargetsFor(DocumentType.RECEIPT)
                    Timber.tag("PRINT_DEBUG").w("   → Znaleziono drukarek głównych: ${standardTargets.size}")
                    if (standardTargets.isEmpty()) {
                        Timber.w("⚠️ Brak skonfigurowanej drukarki standardowej")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Brak skonfigurowanej drukarki standardowej", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        standardTargets.forEach { t ->
                            Timber.tag("PRINT_DEBUG").w("   → 🖨️ Drukuję na standardowej: %s", t.printer.name)
                            printOne(t, order, useDeliveryInterval = false)
                            delay(500)
                        }
                        Timber.tag("PRINT_DEBUG").w("✅ Drukowanie standardowe zakończone")
                    }
                }

                "kitchen" -> {
                    Timber.tag("PRINT_DEBUG").w("🍳 WYBRANO: Drukowanie tylko na drukarce kuchennej")
                    val kitchenTargets = resolveTargetsFor(DocumentType.KITCHEN_TICKET)
                    Timber.tag("PRINT_DEBUG").w("   → Znaleziono drukarek kuchennych: ${kitchenTargets.size}")
                    if (kitchenTargets.isEmpty()) {
                        Timber.w("⚠️ Brak skonfigurowanej drukarki kuchennej")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Brak skonfigurowanej drukarki kuchennej", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        kitchenTargets.forEach { t ->
                            Timber.tag("PRINT_DEBUG").w("   → 🍳 Drukuję paragon kuchenny: %s", t.printer.name)
                            printOne(t, order, useDeliveryInterval = false)
                            delay(500)
                        }
                        Timber.d("✅ Drukowanie kuchenne zakończone")
                    }
                }

                "both" -> {
                    Timber.d("🖨️ Drukowanie na obu drukarkach (główna + kuchnia)")

                    // 1. Drukuj na drukarce głównej
                    val standardTargets = resolveTargetsFor(DocumentType.RECEIPT)
                    if (standardTargets.isEmpty()) {
                        Timber.w("⚠️ Brak skonfigurowanej drukarki standardowej")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Brak skonfigurowanej drukarki standardowej", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        standardTargets.forEach { t ->
                            Timber.d("🖨️ Drukuję na standardowej: %s", t.printer.name)
                            printOne(t, order, useDeliveryInterval = false)
                            delay(500)
                        }
                        Timber.d("✅ Drukowanie standardowe zakończone")
                    }

                    // 2. Drukuj na drukarce kuchennej
                    val kitchenTargets = resolveTargetsFor(DocumentType.KITCHEN_TICKET)
                    if (kitchenTargets.isEmpty()) {
                        Timber.w("⚠️ Brak skonfigurowanej drukarki kuchennej")
                    } else {
                        // OPTYMALIZACJA: Inteligentne opóźnienie przed kuchnią
                        val standardDeviceIds = standardTargets.map { it.printer.deviceId }.toSet()
                        val kitchenDeviceIds = kitchenTargets.map { it.printer.deviceId }.toSet()
                        val samePrinter = standardDeviceIds.any { it in kitchenDeviceIds }

                    if (samePrinter) {
                        Timber.d("🍳 Drukuję na kuchni (ta sama drukarka, opóźnienie 1s)")
                        delay(1000)  // ZMNIEJSZONE z 2000ms → oszczędność 1s
                    } else {
                        Timber.d("🍳 Drukuję na kuchni (inna drukarka, bez opóźnienia)")
                        // Różne drukarki → mogą drukować równolegle, brak opóźnienia
                    }

                    kitchenTargets.forEach { t ->
                        Timber.d("🍳 Drukuję na kuchni: %s", t.printer.name)
                        printOne(t, order, useDeliveryInterval = false)
                        delay(500)
                    }
                    Timber.d("✅ Drukowanie kuchenne zakończone")
                    }
                }

                else -> {
                    // Domyślnie "both" dla kompatybilności wstecznej
                    Timber.d("🖨️ Nieznana opcja drukarek: $printersSelection, drukuję na obu")

                    val standardTargets = resolveTargetsFor(DocumentType.RECEIPT)
                    if (standardTargets.isNotEmpty()) {
                        standardTargets.forEach { t ->
                            Timber.d("🖨️ Drukuję na standardowej: %s", t.printer.name)
                            printOne(t, order, useDeliveryInterval = false)
                            delay(500)
                        }
                    }

                    val kitchenTargets = resolveTargetsFor(DocumentType.KITCHEN_TICKET)
                    if (kitchenTargets.isNotEmpty()) {
                        delay(1000)
                        kitchenTargets.forEach { t ->
                            Timber.d("🍳 Drukuję na kuchni: %s", t.printer.name)
                            printOne(t, order, useDeliveryInterval = false)
                            delay(500)
                        }
                    }
                }
            }
        } finally {
            printMutex.unlock()
        }
    }

    suspend fun printTest(deviceId: String, profile: PrinterProfile, templateId: String, autoCut: Boolean) {
        val printer = Printer(
            id = UUID.randomUUID().toString(),
            name = "Test",
            deviceId = deviceId,
            profileId = profile.id,
            templateId = templateId,
            encoding = profile.encodingName,
            codepage = profile.codepageNumber,
            autoCut = autoCut,
            enabled = true,
            order = 1,
            printerType = com.itsorderkds.data.model.PrinterType.STANDARD
        )
        printMutex.lock()
        try {
            printOne(TargetConfig(printer), buildFakeOrder(), useDeliveryInterval = false)
        } finally {
            printMutex.unlock()
        }
    }

    // Prosty builder testowego zamówienia do wydruku próbnego
    private fun buildFakeOrder(): Order {
        return Order(
            orderId = "test",
            status = true,
            total = 0.0,
            consumer = com.itsorderkds.data.model.Consumer(
                name = "Test Customer",
                email = "test@example.com",
                phone = "+48000000000",
                countryCode = "48"
            ),
            orderNumber = "TEST-PRINT",
            orderStatus = com.itsorderkds.data.model.OrderStatus(
                name = "accepted",
                sequence = 0,
                slug = "ACCEPTED"
            ),
            orderStatusActivities = emptyList(),
            paymentMethod = null,
            paymentStatus = null,
            paymentStatusRank = null,
            amount = 0.0,
            taxTotal = 0.0,
            shippingTotal = 0.0,
            walletBalance = 0.0,
            additionalFeeTotal = null,
            additionalFees = emptyList(),
            couponTotalDiscount = null,
            currency = "PLN",
            isGuest = false,
            pointsAmount = 0.0,
            usedPoint = 0.0,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z",
            shippingAddress = com.itsorderkds.data.model.ShippingAddress(
                street = "Testowa",
                city = "Kraków",
                numberHome = "1",
                numberFlat = "",
                coordinates = null
            ),
            deliveryInterval = null,
            deliveryTime = null,
            isAsap = true,
            products = emptyList(),
            note = "Test ticket",
            deliveryType = null,
            courier = null,
            source = null,
            orderKey = null,
            ip = null,
            externalDelivery = null,
            type = null,
            isScheduled = false
        )
    }

    /**
     * Drukuje na porcie szeregowym (dla drukarek wbudowanych).
     */
    private suspend fun printOneSerial(target: TargetConfig, order: Order, useDeliveryInterval: Boolean) {
        val printer = target.printer
        Timber.d("🖨️ Serial Port Print: port=${printer.deviceId}, printer=${printer.name}")

        return withContext(Dispatchers.IO) {
            try {
                val template = PrintTemplate.fromId(printer.templateId)
                val ticket = PrintTemplateFactory.buildTicket(context, order, template, useDeliveryInterval)

                Timber.d("📋 Serial: Dokument przygotowany (${ticket.length} znaków)")

                // Drukuj na porcie szeregowym używając DantSu
                val encoding = printer.encoding ?: "UTF-8"
                val autoCut = printer.autoCut

                Timber.d("⚙️ Serial: encoding=$encoding, autoCut=$autoCut")
                Timber.d("📤 Wysyłam zawartość do DantSu...")

                val success = SerialPortPrinter.printFormattedText(
                    context = context,
                    portPath = printer.deviceId,
                    formattedContent = ticket,
                    encoding = encoding,
                    autoCut = autoCut
                )

                if (success) {
                    Timber.d("✅ Serial print zakończony pomyślnie")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "✅ Wydruk na ${printer.name}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Timber.e("❌ Serial print nieudany")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "❌ B��ąd drukowania na ${printer.name}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Wyjątek Serial print")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Błąd: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
