package com.itsorderkds.ui.settings.printer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsorderkds.data.model.Printer
import com.itsorderkds.data.preferences.PrinterPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel dla ekranu zarządzania drukarkami.
 * Obsługuje CRUD operacje na liście drukarek.
 */
@HiltViewModel
class PrintersViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _printers = MutableStateFlow<List<Printer>>(emptyList())
    val printers: StateFlow<List<Printer>> = _printers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPrinters()
    }

    /**
     * Wczytuje listę drukarek z PreferHences.
     */
    fun loadPrinters() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val printers = PrinterPreferences.getPrinters(context)
                _printers.value = printers
                Timber.d("PrintersViewModel: Wczytano ${printers.size} drukarek")
            } catch (e: Exception) {
                Timber.e(e, "PrintersViewModel: Błąd wczytywania drukarek")
                _errorMessage.value = "Błąd wczytywania drukarek: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Dodaje nową drukarkę.
     */
    fun addPrinter(printer: Printer) {
        viewModelScope.launch {
            try {
                PrinterPreferences.addPrinter(context, printer)
                loadPrinters() // Odśwież listę
                Timber.d("PrintersViewModel: Dodano drukarkę '${printer.name}'")
            } catch (e: Exception) {
                Timber.e(e, "PrintersViewModel: Błąd dodawania drukarki")
                _errorMessage.value = "Błąd dodawania drukarki: ${e.message}"
            }
        }
    }

    /**
     * Aktualizuje istniejącą drukarkę.
     */
    fun updatePrinter(id: String, printer: Printer) {
        viewModelScope.launch {
            try {
                PrinterPreferences.updatePrinter(context, id, printer)
                loadPrinters() // Odśwież listę
                Timber.d("PrintersViewModel: Zaktualizowano drukarkę '${printer.name}'")
            } catch (e: Exception) {
                Timber.e(e, "PrintersViewModel: Błąd aktualizacji drukarki")
                _errorMessage.value = "Błąd aktualizacji drukarki: ${e.message}"
            }
        }
    }

    /**
     * Usuwa drukarkę.
     */
    fun deletePrinter(id: String) {
        viewModelScope.launch {
            try {
                PrinterPreferences.deletePrinter(context, id)
                loadPrinters() // Odśwież listę
                Timber.d("PrintersViewModel: Usunięto drukarkę ID=$id")
            } catch (e: Exception) {
                Timber.e(e, "PrintersViewModel: Błąd usuwania drukarki")
                _errorMessage.value = "Błąd usuwania drukarki: ${e.message}"
            }
        }
    }

    /**
     * Zmienia kolejność drukarek (drag-and-drop).
     */
    fun reorderPrinters(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            try {
                PrinterPreferences.reorderPrinters(context, fromIndex, toIndex)
                loadPrinters() // Odśwież listę
                Timber.d("PrintersViewModel: Zmieniono kolejność: $fromIndex → $toIndex")
            } catch (e: Exception) {
                Timber.e(e, "PrintersViewModel: Błąd zmiany kolejności")
                _errorMessage.value = "Błąd zmiany kolejności: ${e.message}"
            }
        }
    }

    /**
     * Przełącza stan enabled drukarki.
     */
    fun toggleEnabled(id: String) {
        viewModelScope.launch {
            try {
                PrinterPreferences.toggleEnabled(context, id)
                loadPrinters() // Odśwież listę
                Timber.d("PrintersViewModel: Przełączono enabled dla ID=$id")
            } catch (e: Exception) {
                Timber.e(e, "PrintersViewModel: Błąd przełączania enabled")
                _errorMessage.value = "Błąd przełączania enabled: ${e.message}"
            }
        }
    }

    /**
     * Czyści komunikat o błędzie.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

