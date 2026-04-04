package com.itsorderkds.ui.settings.printer

import android.content.Context
import android.widget.Toast
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.EscPosCharsetEncoding
import com.dantsu.escposprinter.EscPosPrinter
import com.itsorderkds.util.AppPrefs
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.nio.charset.Charset

/**
 * Uproszczony PrinterTestHelper — tylko essential funkcje do drukowania.
 * Wszystkie testy i funkcje diagnostyczne zostały usunięte.
 */
object PrinterTestHelper {
    fun printTest(context: Context, connection: DeviceConnection) {
        try {
            val printer = EscPosPrinter(connection, 203, 48f, 32)
            printer.printFormattedText("[C]<b>Test wydruku</b>\n[L]Bluetooth/USB działa\n")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Błąd podczas drukowania: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    /** Zwraca charset name na podstawie codepage */
    private fun selectCharsetName(encodingHint: String?, codepage: Int?): String {
        if (codepage != null) {
            when (codepage) {
                13 -> return "Cp852" // Polski
                0 -> return "Cp437"
                255 -> {} // brak selecji
            }
        }
        return when {
            encodingHint?.contains("cp852") == true -> "Cp852"
            encodingHint?.contains("cp1250") == true -> "Cp1250"
            encodingHint?.contains("utf-8") == true -> "UTF-8"
            else -> "UTF-8"
        }
    }

    /** Drukuje treść na drukarce */
    suspend fun printOrder(context: Context, connection: DeviceConnection, ticketContent: String) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Pobierz konfigurację drukarki
                val (_, configuredPrinterId) = AppPrefs.getPrinter(context)
                val config = AppPrefs.getPrinterConfigFor(configuredPrinterId)
                val enc = config.encoding.ifBlank { "UTF-8" }
                val cp = config.codepage ?: 255

                Timber.d("PrinterTestHelper.printOrder: enc=%s, cp=%s", enc, cp)

                // Utwórz charset encoding
                val escCharset = try {
                    EscPosCharsetEncoding(enc, cp)
                } catch (ex: Exception) {
                    Timber.w(ex, "Failed to create charset, fallback to UTF-8")
                    EscPosCharsetEncoding("UTF-8", 255)
                }

                // Utwórz printer
                val printer = EscPosPrinter(connection, 203, 58f, 32, escCharset)

                // Drukuj treść
                printer.printFormattedText(ticketContent)

                // Opcjonalnie: cięcie papieru
                if (config.hasCutter) {
                    try {
                        PrinterManager.feedAndCut(connection, feedLines = 3)
                    } catch (ex: Exception) {
                        Timber.w(ex, "Paper cut failed")
                    }
                }

                printer.disconnectPrinter()
                Timber.d("PrinterTestHelper.printOrder: success")

            } catch (t: Throwable) {
                Timber.e(t, "PrinterTestHelper.printOrder: failed")
                throw t
            }
        }
    }
}

