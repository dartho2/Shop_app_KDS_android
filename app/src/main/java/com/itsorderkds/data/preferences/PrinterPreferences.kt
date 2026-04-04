package com.itsorderkds.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.itsorderkds.data.model.Printer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Zarządzanie listą drukarek w SharedPreferences.
 * Serializacja/deserializacja do JSON.
 */
object PrinterPreferences {
    private const val PREFS_NAME = "printer_preferences"
    private const val KEY_PRINTERS_LIST = "printers_list_json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Pobiera listę wszystkich drukarek.
     * @return Lista drukarek, posortowana po `order`
     */
    fun getPrinters(context: Context): List<Printer> {
        val prefs = getPrefs(context)
        val jsonString = prefs.getString(KEY_PRINTERS_LIST, null)

        if (jsonString.isNullOrBlank()) {
            Timber.d("PrinterPreferences: Brak zapisanych drukarek")
            return emptyList()
        }

        return try {
            val printers = json.decodeFromString<List<Printer>>(jsonString)
            Timber.d("PrinterPreferences: Wczytano ${printers.size} drukarek")
            printers.sortedBy { it.order }
        } catch (e: Exception) {
            Timber.e(e, "PrinterPreferences: Błąd deserializacji drukarek")
            emptyList()
        }
    }

    /**
     * Zapisuje listę drukarek.
     * @param printers Lista drukarek do zapisania
     */
    fun savePrinters(context: Context, printers: List<Printer>) {
        val prefs = getPrefs(context)
        val jsonString = json.encodeToString(printers)

        prefs.edit()
            .putString(KEY_PRINTERS_LIST, jsonString)
            .apply()

        Timber.d("PrinterPreferences: Zapisano ${printers.size} drukarek")
    }

    /**
     * Dodaje nową drukarkę na koniec listy.
     * @param printer Drukarka do dodania
     */
    fun addPrinter(context: Context, printer: Printer) {
        val currentPrinters = getPrinters(context).toMutableList()

        // Ustaw order jako następny wolny numer
        val maxOrder = currentPrinters.maxOfOrNull { it.order } ?: 0
        val newPrinter = printer.copy(order = maxOrder + 1)

        currentPrinters.add(newPrinter)
        savePrinters(context, currentPrinters)

        Timber.d("PrinterPreferences: Dodano drukarkę '${printer.name}' (order=${newPrinter.order})")
    }

    /**
     * Aktualizuje istniejącą drukarkę.
     * @param id ID drukarki do zaktualizowania
     * @param updatedPrinter Nowe dane drukarki
     */
    fun updatePrinter(context: Context, id: String, updatedPrinter: Printer) {
        val currentPrinters = getPrinters(context).toMutableList()
        val index = currentPrinters.indexOfFirst { it.id == id }

        if (index == -1) {
            Timber.w("PrinterPreferences: Nie znaleziono drukarki o ID=$id")
            return
        }

        currentPrinters[index] = updatedPrinter.copy(id = id) // zachowaj oryginalne ID
        savePrinters(context, currentPrinters)

        Timber.d("PrinterPreferences: Zaktualizowano drukarkę '${updatedPrinter.name}'")
    }

    /**
     * Usuwa drukarkę z listy.
     * @param id ID drukarki do usunięcia
     */
    fun deletePrinter(context: Context, id: String) {
        val currentPrinters = getPrinters(context).toMutableList()
        val removed = currentPrinters.removeAll { it.id == id }

        if (removed) {
            // Przelicz order po usunięciu
            val reordered = currentPrinters.mapIndexed { index, printer ->
                printer.copy(order = index + 1)
            }
            savePrinters(context, reordered)
            Timber.d("PrinterPreferences: Usunięto drukarkę ID=$id")
        } else {
            Timber.w("PrinterPreferences: Nie znaleziono drukarki o ID=$id do usunięcia")
        }
    }

    /**
     * Zmienia kolejność drukarki (drag-and-drop).
     * @param fromIndex Początkowy indeks
     * @param toIndex Docelowy indeks
     */
    fun reorderPrinters(context: Context, fromIndex: Int, toIndex: Int) {
        val currentPrinters = getPrinters(context).toMutableList()

        if (fromIndex < 0 || fromIndex >= currentPrinters.size ||
            toIndex < 0 || toIndex >= currentPrinters.size
        ) {
            Timber.w("PrinterPreferences: Nieprawidłowe indeksy reorder: $fromIndex → $toIndex")
            return
        }

        val movedPrinter = currentPrinters.removeAt(fromIndex)
        currentPrinters.add(toIndex, movedPrinter)

        // Przelicz order po przesunięciu
        val reordered = currentPrinters.mapIndexed { index, printer ->
            printer.copy(order = index + 1)
        }

        savePrinters(context, reordered)
        Timber.d("PrinterPreferences: Zmieniono kolejność drukarki: $fromIndex → $toIndex")
    }

    /**
     * Przełącza stan enabled drukarki.
     * @param id ID drukarki
     */
    fun toggleEnabled(context: Context, id: String) {
        val currentPrinters = getPrinters(context).toMutableList()
        val index = currentPrinters.indexOfFirst { it.id == id }

        if (index == -1) {
            Timber.w("PrinterPreferences: Nie znaleziono drukarki o ID=$id")
            return
        }

        val printer = currentPrinters[index]
        currentPrinters[index] = printer.copy(enabled = !printer.enabled)
        savePrinters(context, currentPrinters)

        Timber.d("PrinterPreferences: Przełączono enabled dla '${printer.name}': ${!printer.enabled}")
    }

    /**
     * Pobiera tylko aktywne drukarki, posortowane po order.
     * Używane przy drukowaniu zamówień.
     */
    fun getEnabledPrinters(context: Context): List<Printer> {
        return getPrinters(context)
            .filter { it.enabled }
            .sortedBy { it.order }
    }

    /**
     * Czyści wszystkie drukarki (użyj ostrożnie!).
     * Używane głównie do testów.
     */
    fun clearAll(context: Context) {
        getPrefs(context).edit().remove(KEY_PRINTERS_LIST).apply()
        Timber.d("PrinterPreferences: Wyczyszczono wszystkie drukarki")
    }
}

