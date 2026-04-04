package com.itsorderkds.data.preferences

import android.content.Context
import com.itsorderkds.data.model.Printer
import com.itsorderkds.data.model.PrinterProfile
import com.itsorderkds.util.AppPrefs
import timber.log.Timber
import java.util.UUID

/**
 * Migracja starych ustawień drukarek (STANDARD/KITCHEN) do nowego systemu listy drukarek.
 * Wykonywana tylko raz przy pierwszym uruchomieniu po aktualizacji.
 */
object PrinterMigration {

    /**
     * Główna funkcja migracji.
     * Sprawdza czy migracja jest potrzebna i jeśli tak, przenosi stare ustawienia.
     */
    fun migrateOldPrintersToNewSystem(context: Context) {
        // Sprawdź czy migracja już się odbyła
        val existingPrinters = PrinterPreferences.getPrinters(context)
        if (existingPrinters.isNotEmpty()) {
            Timber.d("PrinterMigration: Migracja już wykonana (znaleziono ${existingPrinters.size} drukarek)")
            return
        }

        Timber.d("PrinterMigration: Rozpoczynam migrację starych ustawień...")

        val migratedPrinters = mutableListOf<Printer>()

        // 1. Migruj STANDARD (główną drukarkę)
        val standardPrinter = migrateStandardPrinter(context)
        if (standardPrinter != null) {
            migratedPrinters.add(standardPrinter)
            Timber.d("PrinterMigration: ✅ Zmigowano STANDARD: ${standardPrinter.name}")
        }

        // 2. Migruj KITCHEN (drukarka kuchenna)
        val kitchenPrinter = migrateKitchenPrinter(context)
        if (kitchenPrinter != null) {
            migratedPrinters.add(kitchenPrinter)
            Timber.d("PrinterMigration: ✅ Zmigowano KITCHEN: ${kitchenPrinter.name}")
        }

        // Zapisz zmigowane drukarki
        if (migratedPrinters.isNotEmpty()) {
            PrinterPreferences.savePrinters(context, migratedPrinters)
            Timber.d("PrinterMigration: 🎯 Migracja zakończona: ${migratedPrinters.size} drukarek")
        } else {
            Timber.d("PrinterMigration: ℹ️ Brak starych ustawień do migracji")
        }
    }

    /**
     * Migruje ustawienia drukarki STANDARD.
     */
    private fun migrateStandardPrinter(context: Context): Printer? {
        try {
            // Stare klucze dla STANDARD
            val prefs = context.getSharedPreferences("itsorderchat_prefs", Context.MODE_PRIVATE)

            // Próba odczytu starego deviceId
            val deviceId = prefs.getString("printer_id", null)
                ?: prefs.getString("PRINTER_MAIN_ID", null)
                ?: return null

            // Próba odczytu encoding
            val encoding = prefs.getString("printer_encoding", null)
                ?: prefs.getString("PRINTER_MAIN_ENCODING", "UTF-8")
                ?: "UTF-8"

            // Próba odczytu codepage
            val codepage = prefs.getInt("PRINTER_MAIN_CODEPAGE", -1).let {
                if (it == -1) null else it
            }

            // Określ profil na podstawie encoding i codepage
            val profile = when {
                encoding == "Cp852" && codepage == 13 -> PrinterProfile.POS_8390_DUAL
                encoding == "UTF-8" -> PrinterProfile.MOBILE_SSP
                else -> PrinterProfile.CUSTOM
            }

            // Szablon wydruku
            val templateId = AppPrefs.getPrintTemplate("template_standard")

            return Printer(
                id = UUID.randomUUID().toString(),
                name = "Drukarka Główna",
                deviceId = deviceId,
                profileId = profile.id,
                templateId = templateId,
                encoding = encoding,
                codepage = codepage,
                autoCut = false, // STANDARD nie miał autoCut
                enabled = true,
                order = 1
            )
        } catch (e: Exception) {
            Timber.e(e, "PrinterMigration: Błąd migracji STANDARD")
            return null
        }
    }

    /**
     * Migruje ustawienia drukarki KITCHEN.
     */
    private fun migrateKitchenPrinter(context: Context): Printer? {
        try {
            val prefs = context.getSharedPreferences("itsorderchat_prefs", Context.MODE_PRIVATE)

            // Sprawdź czy KITCHEN była włączona
            val kitchenEnabled = prefs.getBoolean("PRINTER_KITCHEN_ENABLED", false)
            if (!kitchenEnabled) {
                Timber.d("PrinterMigration: KITCHEN była wyłączona, pomijam")
                return null
            }

            // Odczyt deviceId
            val deviceId = prefs.getString("PRINTER_KITCHEN_ID", null)
                ?: return null

            // Odczyt encoding
            val encoding = prefs.getString("PRINTER_KITCHEN_ENCODING", "Cp852") ?: "Cp852"

            // Odczyt codepage
            val codepage = prefs.getInt("PRINTER_KITCHEN_CODEPAGE", 13).let {
                if (it == -1) null else it
            }

            // Odczyt autoCut
            val autoCut = prefs.getBoolean("PRINTER_KITCHEN_AUTO_CUT", true)

            // Określ profil
            val profile = when {
                encoding == "Cp852" && codepage == 13 && autoCut -> PrinterProfile.POS_8390_DUAL
                encoding == "UTF-8" -> PrinterProfile.MOBILE_SSP
                else -> PrinterProfile.CUSTOM
            }

            // Szablon - KITCHEN używał zazwyczaj standard lub compact
            val templateId = "template_standard"

            return Printer(
                id = UUID.randomUUID().toString(),
                name = "Drukarka Kuchenna",
                deviceId = deviceId,
                profileId = profile.id,
                templateId = templateId,
                encoding = encoding,
                codepage = codepage,
                autoCut = autoCut,
                enabled = true,
                order = 2 // KITCHEN drukuje jako druga
            )
        } catch (e: Exception) {
            Timber.e(e, "PrinterMigration: Błąd migracji KITCHEN")
            return null
        }
    }

    /**
     * Czyści stare klucze z SharedPreferences (opcjonalne).
     * Użyj tylko jeśli jesteś pewien że migracja przebiegła pomyślnie.
     */
    fun cleanupOldKeys(context: Context) {
        val prefs = context.getSharedPreferences("itsorderchat_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Stare klucze STANDARD
        editor.remove("printer_id")
        editor.remove("printer_type")
        editor.remove("printer_encoding")
        editor.remove("PRINTER_MAIN_ID")
        editor.remove("PRINTER_MAIN_ENCODING")
        editor.remove("PRINTER_MAIN_CODEPAGE")

        // Stare klucze KITCHEN
        editor.remove("PRINTER_KITCHEN_TYPE")
        editor.remove("PRINTER_KITCHEN_ID")
        editor.remove("PRINTER_KITCHEN_ENCODING")
        editor.remove("PRINTER_KITCHEN_CODEPAGE")
        editor.remove("PRINTER_KITCHEN_AUTO_CUT")
        editor.remove("PRINTER_KITCHEN_ENABLED")

        editor.apply()
        Timber.d("PrinterMigration: Wyczyszczono stare klucze")
    }
}

