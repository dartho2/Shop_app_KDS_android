package com.itsorderkds.ui.settings.print

import android.content.Context
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import timber.log.Timber
import java.io.File
import com.itsorderkds.ui.settings.printer.AidlPrinterService

object SerialPortPrinter {
    private const val BUILTIN_ID = "builtin"

    /**
     * Lista portów do sprawdzenia w trybie AUTO (fallback).
     */
    private val PORTS_TO_CHECK = listOf(
        "/dev/ttyS0",
        "/dev/ttyMT0",
        "/dev/ttyS1",
        "/dev/ttyS2",
        "/dev/ttyUSB0"
    )

    fun printFormattedText(
        context: Context,
        portPath: String,
        formattedContent: String,
        encoding: String = "UTF-8",
        autoCut: Boolean = false
    ): Boolean {

        // 1. OBSŁUGA DRUKARKI WBUDOWANEJ (AIDL - Senraise/Sunmi/Klon)
        // To jest główna ścieżka dla Twojego terminala H10
        if (portPath.equals(BUILTIN_ID, ignoreCase = true)) {
            Timber.d("🚀 Wykryto builtin -> Uruchamiam AidlPrinterService")

            val aidlService = com.itsorderkds.ui.settings.printer.AidlPrinterService(context)
            aidlService.connect()

            // Dajemy chwilę na nawiązanie połączenia z usługą
            try { Thread.sleep(800) } catch (_: InterruptedException) {}

            val success = aidlService.printText(formattedContent, autoCut)

            aidlService.disconnect()

            if (success) {
                Timber.d("✅ Wydrukowano pomyślnie przez AIDL")
                return true
            } else {
                Timber.w("⚠️ AIDL zawiódł, próbuję metod zapasowych (USB/Serial)...")
            }
        }

        // 2. USB (Dla zewnętrznych drukarek termicznych)
        if (portPath.equals(BUILTIN_ID, ignoreCase = true)) {
            if (printToUsb(context, formattedContent, autoCut)) return true
        }

        // 3. SERIAL PORT - Tryb AUTO (Przeszukiwanie portów)
        // Uruchamia się tylko, jeśli AIDL i USB zawiodły
        if (portPath.equals(BUILTIN_ID, ignoreCase = true)) {
            Timber.d("🔄 Tryb Auto Serial: Sprawdzam fizyczne porty...")
            for (port in PORTS_TO_CHECK) {
                // Najpierw standardowa prędkość 115200
                if (attemptSerialPrint(port, 115200, formattedContent, encoding, autoCut)) return true
                // Potem wolniejsza 9600
                if (attemptSerialPrint(port, 9600, formattedContent, encoding, autoCut)) return true
            }
            return false
        }

        // 4. SERIAL PORT - Tryb Ręczny (konkretna ścieżka, np. /dev/ttyS1)
        else {
            if (attemptSerialPrint(portPath, 115200, formattedContent, encoding, autoCut)) return true
            if (attemptSerialPrint(portPath, 9600, formattedContent, encoding, autoCut)) return true
            return false
        }
    }

    private fun attemptSerialPrint(
        path: String,
        baud: Int,
        content: String,
        encoding: String,
        autoCut: Boolean
    ): Boolean {
        val file = File(path)
        if (!file.exists() || !file.canWrite()) {
            return false
        }

        var connection: SerialConnection? = null
        try {
            Timber.d("⚡ Próba Serial: $path @ $baud")
            connection = SerialConnection(path, baud)
            connection.connect()

            val printer = EscPosPrinter(connection, 203, 48f, 32)

            if (encoding.equals("CP852", ignoreCase = true)) {
                connection.write(byteArrayOf(0x1B, 0x74, 13))
            }

            val finalContent = buildString {
                append("[C]")
                append(content)
                append("\n\n\n")
            }

            if (autoCut) printer.printFormattedTextAndCut(finalContent)
            else printer.printFormattedText(finalContent)

            return true

        } catch (_: Exception) {
            return false
        } finally {
            try { connection?.disconnect() } catch (_: Exception) {}
        }
    }

    private fun printToUsb(context: Context, formattedContent: String, autoCut: Boolean): Boolean {
        try {
            val connection = UsbPrintersConnections.selectFirstConnected(context) ?: return false
            val printer = EscPosPrinter(connection, 203, 48f, 32)
            val finalContent = "[C]$formattedContent\n\n\n"
            if (autoCut) printer.printFormattedTextAndCut(finalContent)
            else printer.printFormattedText(finalContent)
            return true
        } catch (_: Exception) { return false }
    }
}
