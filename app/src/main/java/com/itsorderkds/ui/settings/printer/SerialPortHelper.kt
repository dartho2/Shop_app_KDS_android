package com.itsorderkds.ui.settings.printer

import timber.log.Timber
import java.io.File

/**
 * Helper do wykrywania i testowania wbudowanych drukarek szeregowych.
 * Obsługuje urządzenia takie jak Sunmi H10 z wbudowaną drukarką.
 */
object SerialPortHelper {

    /**
     * Lista popularnych portów szeregowych używanych w urządzeniach POS.
     */
    private val COMMON_SERIAL_PORTS = listOf(
        "/dev/ttyS1",      // Sunmi H10, inne urządzenia POS
        "/dev/ttyS0",      // Standardowy COM1
        "/dev/ttyS2",      // Alternatywny port
        "/dev/ttyUSB0",    // USB-Serial
        "/dev/ttyMT0",     // MediaTek devices
        "/dev/ttyMT1",     // MediaTek devices
        "/dev/ttyMT2"      // MediaTek devices
    )

    /**
     * Popularne prędkości transmisji dla drukarek szeregowych.
     */
    private val COMMON_BAUD_RATES = listOf(
        115200,  // Najczęściej używana
        9600,    // Standard RS-232
        19200,
        38400,
        57600
    )

    /**
     * Wynik skanowania portów szeregowych.
     */
    data class SerialPortInfo(
        val path: String,
        val exists: Boolean,
        val readable: Boolean,
        val writable: Boolean,
        val isCharDevice: Boolean,
        val permissions: String
    )

    /**
     * Skanuje dostępne porty szeregowe i zwraca informacje o nich.
     */
    fun scanSerialPorts(): List<SerialPortInfo> {
        Timber.d("SerialPortHelper: Rozpoczynam skanowanie portów szeregowych...")

        val results = COMMON_SERIAL_PORTS.map { path ->
            val file = File(path)
            val info = SerialPortInfo(
                path = path,
                exists = file.exists(),
                readable = file.canRead(),
                writable = file.canWrite(),
                isCharDevice = isCharacterDevice(file),
                permissions = getFilePermissions(path)
            )

            Timber.d("SerialPortHelper: Port $path -> exists=${info.exists}, readable=${info.readable}, writable=${info.writable}, permissions=${info.permissions}")
            info
        }

        val availablePorts = results.filter { it.exists }
        Timber.d("SerialPortHelper: Znaleziono ${availablePorts.size} portów: ${availablePorts.map { it.path }}")

        return results
    }

    /**
     * Sprawdza czy plik jest urządzeniem znakowym (character device).
     */
    private fun isCharacterDevice(file: File): Boolean {
        return try {
            if (!file.exists()) return false
            // W Linuxie urządzenia znakowe zaczynają się od 'c' w ls -l
            val process = Runtime.getRuntime().exec(arrayOf("ls", "-l", file.absolutePath))
            val output = process.inputStream.bufferedReader().readText()
            output.startsWith("c")
        } catch (e: Exception) {
            Timber.w(e, "SerialPortHelper: Nie można sprawdzić typu pliku ${file.path}")
            false
        }
    }

    /**
     * Pobiera uprawnienia pliku (chmod format).
     */
    private fun getFilePermissions(path: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("ls", "-l", path))
            val output = process.inputStream.bufferedReader().readText().trim()
            if (output.isNotEmpty()) {
                // Pierwsza linia z ls -l zawiera uprawnienia
                output.split("\\s+".toRegex()).firstOrNull() ?: "unknown"
            } else {
                "not found"
            }
        } catch (e: Exception) {
            Timber.w(e, "SerialPortHelper: Nie można odczytać uprawnień dla $path")
            "error"
        }
    }

    /**
     * Sprawdza czy możemy otworzyć dany port (wymaga uprawnień root lub odpowiednich SELinux rules).
     */
    fun canOpenSerialPort(path: String, baudRate: Int = 115200): Boolean {
        Timber.d("SerialPortHelper: Próba otwarcia portu $path z baudRate=$baudRate")

        return try {
            val file = File(path)
            if (!file.exists()) {
                Timber.w("SerialPortHelper: Port $path nie istnieje")
                return false
            }

            // Próba otwarcia pliku do zapisu
            // Uwaga: To NIE otworzy prawdziwego SerialPort, tylko sprawdzi uprawnienia do pliku
            file.canWrite().also {
                if (it) {
                    Timber.d("SerialPortHelper: ✅ Port $path jest dostępny do zapisu")
                } else {
                    Timber.w("SerialPortHelper: ❌ Brak uprawnień do zapisu w $path")
                }
            }
        } catch (e: SecurityException) {
            Timber.e(e, "SerialPortHelper: SecurityException przy próbie otwarcia $path")
            false
        } catch (e: Exception) {
            Timber.e(e, "SerialPortHelper: Błąd przy próbie otwarcia $path")
            false
        }
    }

    /**
     * Zwraca najlepszy kandydat na port drukarki (najczęściej /dev/ttyS1 dla Sunmi).
     */
    fun getBestSerialPortCandidate(): String? {
        val ports = scanSerialPorts()

        // Preferuj porty które istnieją i są dostępne do zapisu
        val writable = ports.firstOrNull { it.exists && it.writable }
        if (writable != null) {
            Timber.d("SerialPortHelper: Najlepszy kandydat (writable): ${writable.path}")
            return writable.path
        }

        // Jeśli żaden nie jest writable, zwróć pierwszy który istnieje
        val existing = ports.firstOrNull { it.exists }
        if (existing != null) {
            Timber.d("SerialPortHelper: Najlepszy kandydat (exists): ${existing.path}")
            return existing.path
        }

        Timber.w("SerialPortHelper: Nie znaleziono żadnego dostępnego portu szeregowego")
        return null
    }

    /**
     * Informacje diagnostyczne o wsparciu dla drukarek szeregowych.
     */
    fun getDiagnosticInfo(): String {
        val sb = StringBuilder()
        sb.appendLine("=== DIAGNOSTYKA PORTU SZEREGOWEGO ===")
        sb.appendLine()

        val ports = scanSerialPorts()

        if (ports.none { it.exists }) {
            sb.appendLine("❌ Brak dostępnych portów szeregowych")
            sb.appendLine("   To urządzenie prawdopodobnie NIE ma wbudowanej drukarki.")
            sb.appendLine()
        } else {
            sb.appendLine("✅ Znaleziono ${ports.count { it.exists }} portów:")
            sb.appendLine()

            ports.filter { it.exists }.forEach { port ->
                sb.appendLine("📍 ${port.path}")
                sb.appendLine("   Uprawnienia: ${port.permissions}")
                sb.appendLine("   Odczyt: ${if (port.readable) "✅" else "❌"}")
                sb.appendLine("   Zapis: ${if (port.writable) "✅" else "❌"}")
                sb.appendLine("   Typ: ${if (port.isCharDevice) "Character Device" else "Unknown"}")
                sb.appendLine()
            }
        }

        val best = getBestSerialPortCandidate()
        if (best != null) {
            sb.appendLine("🎯 Zalecany port: $best")
        } else {
            sb.appendLine("⚠️ Brak zalecanego portu")
        }

        sb.appendLine()
        sb.appendLine("ℹ️ Uwaga: Jeśli porty istnieją ale brak uprawnień do zapisu,")
        sb.appendLine("   aplikacja wymaga uprawnień root lub specjalnych SELinux rules.")

        return sb.toString()
    }

    /**
     * Sprawdza czy to jest urządzenie Sunmi (ma wbudowaną drukarkę).
     */
    fun isSunmiDevice(): Boolean {
        return try {
            val manufacturer = android.os.Build.MANUFACTURER.lowercase()
            val model = android.os.Build.MODEL.lowercase()

            val isSunmi = manufacturer.contains("sunmi") ||
                         model.contains("sunmi") ||
                         model.contains("h10")

            Timber.d("SerialPortHelper: Manufacturer=$manufacturer, Model=$model, isSunmi=$isSunmi")
            isSunmi
        } catch (e: Exception) {
            Timber.e(e, "SerialPortHelper: Błąd sprawdzania producenta")
            false
        }
    }

    /**
     * Test wydruku (wymaga biblioteki android-serialport-api).
     * UWAGA: To jest tylko przykład - wymaga dodania biblioteki do build.gradle.
     */
    fun testPrintExample(): String {
        return """
            Aby włączyć drukowanie przez port szeregowy:

            1. Dodaj do build.gradle (app):
               implementation 'com.github.licheedev:Android-SerialPort-API:2.1.3'

            2. Dodaj do settings.gradle:
               maven { url 'https://jitpack.io' }

            3. Kod testowy:
               val serialPort = SerialPort(File("/dev/ttyS1"), 115200, 0)
               val out = serialPort.outputStream
               out.write("Test druku\n\n\n".toByteArray(Charset.forName("GBK")))
               out.flush()
               serialPort.close()

            4. Uprawnienia w AndroidManifest.xml:
               <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

            5. SELinux: Może wymagać custom ROM lub roota
        """.trimIndent()
    }
}

