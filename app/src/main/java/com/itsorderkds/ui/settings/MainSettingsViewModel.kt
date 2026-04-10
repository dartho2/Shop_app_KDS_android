package com.itsorderkds.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsorderkds.data.preferences.AppPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel dla MainSettingsScreen
 * Zarządza Kiosk Mode, Auto-restart, Task Reopen oraz KDS Workflow settings
 */
@HiltViewModel
class MainSettingsViewModel @Inject constructor(
    private val appPreferencesManager: AppPreferencesManager
) : ViewModel() {

    // ─── Terminal ────────────────────────────────────────────────────────────

    val kioskModeEnabled: StateFlow<Boolean> = appPreferencesManager.kioskModeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val autoRestartEnabled: StateFlow<Boolean> = appPreferencesManager.autoRestartEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val taskReopenEnabled: StateFlow<Boolean> = appPreferencesManager.taskReopenEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    fun setKioskModeEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferencesManager.setKioskModeEnabled(enabled) }
    }

    fun setAutoRestartEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferencesManager.setAutoRestartEnabled(enabled) }
    }

    fun setTaskReopenEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferencesManager.setTaskReopenEnabled(enabled) }
    }

    // ─── KDS Workflow ────────────────────────────────────────────────────────

    /** false = tryb prosty ACTIVE/READY (domyślny), true = pełny kolejkowy */
    val kdsQueueMode: StateFlow<Boolean> = appPreferencesManager.kdsQueueModeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    /** Drukuj automatycznie gdy ticket → READY */
    val kdsAutoPrintOnReady: StateFlow<Boolean> = appPreferencesManager.kdsAutoPrintOnReadyFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    /** Pokazuj zakładkę "Zaplanowane" */
    val kdsShowScheduled: StateFlow<Boolean> = appPreferencesManager.kdsShowScheduledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    /** Wymagaj potwierdzenia przed GOTOWE */
    val kdsRequireReadyConfirm: StateFlow<Boolean> = appPreferencesManager.kdsRequireReadyConfirmFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    /** Liczba kolumn: "auto", "2", "3", "4" */
    val kdsGridColumns: StateFlow<String> = appPreferencesManager.kdsGridColumnsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "auto")

    fun setKdsQueueMode(enabled: Boolean) {
        viewModelScope.launch { appPreferencesManager.setKdsQueueMode(enabled) }
    }

    fun setKdsAutoPrintOnReady(enabled: Boolean) {
        viewModelScope.launch { appPreferencesManager.setKdsAutoPrintOnReady(enabled) }
    }

    fun setKdsShowScheduled(enabled: Boolean) {
        viewModelScope.launch { appPreferencesManager.setKdsShowScheduled(enabled) }
    }

    fun setKdsRequireReadyConfirm(enabled: Boolean) {
        viewModelScope.launch { appPreferencesManager.setKdsRequireReadyConfirm(enabled) }
    }

    fun setKdsGridColumns(columns: String) {
        viewModelScope.launch { appPreferencesManager.setKdsGridColumns(columns) }
    }

    /** Tryb wyświetlania zamówień: COMPACT_FLOW | STABLE_GRID | COLUMN_MODE */
    val kdsDisplayMode: StateFlow<String> = appPreferencesManager.kdsDisplayModeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "STABLE_GRID")

    fun setKdsDisplayMode(mode: String) {
        viewModelScope.launch { appPreferencesManager.setKdsDisplayMode(mode) }
    }

    /** Czy nowe zamówienia wypełniają wolne sloty */
    val kdsFillGaps: StateFlow<Boolean> = appPreferencesManager.kdsFillGapsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    fun setKdsFillGaps(enabled: Boolean) {
        viewModelScope.launch { appPreferencesManager.setKdsFillGaps(enabled) }
    }

    /** Czas przygotowania dla odbioru osobistego (minuty) */
    val kdsPrepTimePickup: StateFlow<Int> = appPreferencesManager.kdsPrepTimePickupFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 30)

    fun setKdsPrepTimePickup(minutes: Int) {
        viewModelScope.launch { appPreferencesManager.setKdsPrepTimePickup(minutes) }
    }

    /** Czas przygotowania dla dostawy (minuty) */
    val kdsPrepTimeDelivery: StateFlow<Int> = appPreferencesManager.kdsPrepTimeDeliveryFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 60)

    fun setKdsPrepTimeDelivery(minutes: Int) {
        viewModelScope.launch { appPreferencesManager.setKdsPrepTimeDelivery(minutes) }
    }

    /** Czy przycisk ANULUJ jest widoczny na karcie KDS */
    val kdsCancelEnabled: StateFlow<Boolean> = appPreferencesManager.kdsCancelEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setKdsCancelEnabled(enabled: Boolean) {
        viewModelScope.launch { appPreferencesManager.setKdsCancelEnabled(enabled) }
    }

    /** Czy pokazywać notatki/modyfikacje przy pozycjach na karcie KDS */
    val kdsShowNotes: StateFlow<Boolean> = appPreferencesManager.kdsShowNotesFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    fun setKdsShowNotes(enabled: Boolean) {
        viewModelScope.launch { appPreferencesManager.setKdsShowNotes(enabled) }
    }

    /** Tryb tapowania nagłówka zamiast przycisków */
    val kdsHeaderTapMode: StateFlow<Boolean> = appPreferencesManager.kdsHeaderTapModeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setKdsHeaderTapMode(enabled: Boolean) {
        viewModelScope.launch { appPreferencesManager.setKdsHeaderTapMode(enabled) }
    }

    /** Panel agregacji pozycji (Production Summary) */
    val kdsShowProductionSummary: StateFlow<Boolean> = appPreferencesManager.kdsShowProductionSummaryFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setKdsShowProductionSummary(enabled: Boolean) {
        viewModelScope.launch { appPreferencesManager.setKdsShowProductionSummary(enabled) }
    }

    /** Production Summary: minimalna ilość do wyświetlenia (1 = wszystkie, 2 = tylko >1) */
    val kdsProductionMinQty: StateFlow<Int> = appPreferencesManager.kdsProductionMinQtyFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 1)

    fun setKdsProductionMinQty(minQty: Int) {
        viewModelScope.launch { appPreferencesManager.setKdsProductionMinQty(minQty) }
    }

    /** Production Summary: liczba kolumn (1 lub 2) */
    val kdsProductionColumns: StateFlow<Int> = appPreferencesManager.kdsProductionColumnsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 1)

    fun setKdsProductionColumns(columns: Int) {
        viewModelScope.launch { appPreferencesManager.setKdsProductionColumns(columns) }
    }

    /**
     * Za ile minut przed scheduledFor zamówienie przechodzi z Planu do Aktywnych.
     * Domyślnie 60 min.
     */
    val kdsScheduledActiveWindow: StateFlow<Int> = appPreferencesManager.kdsScheduledActiveWindowFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 60)

    fun setKdsScheduledActiveWindow(minutes: Int) {
        viewModelScope.launch { appPreferencesManager.setKdsScheduledActiveWindow(minutes) }
    }

    /**
     * Słowa kluczowe do ukrywania produktów na bloczkach KDS i w Production Summary.
     * Domyślnie "opłata". Format: przecinkami, np. "opłata,fee"
     */
    val kdsExcludedKeywords: StateFlow<String> = appPreferencesManager.kdsExcludedKeywordsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "opłata")

    fun setKdsExcludedKeywords(keywords: String) {
        viewModelScope.launch { appPreferencesManager.setKdsExcludedKeywords(keywords) }
    }

    /** Skrócony bloczek kuchenny — kompaktowy nagłówek + lista pozycji */
    val kdsCompactCardMode: StateFlow<Boolean> = appPreferencesManager.kdsCompactCardModeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun setKdsCompactCardMode(enabled: Boolean) {
        viewModelScope.launch { appPreferencesManager.setKdsCompactCardMode(enabled) }
    }
}

