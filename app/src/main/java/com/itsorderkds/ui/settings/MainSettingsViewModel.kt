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
 * Zarządza Kiosk Mode, Auto-restart i Task Reopen settings
 */
@HiltViewModel
class MainSettingsViewModel @Inject constructor(
    private val appPreferencesManager: AppPreferencesManager
) : ViewModel() {

    val kioskModeEnabled: StateFlow<Boolean> = appPreferencesManager.kioskModeEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val autoRestartEnabled: StateFlow<Boolean> = appPreferencesManager.autoRestartEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val taskReopenEnabled: StateFlow<Boolean> = appPreferencesManager.taskReopenEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, true) // domyślnie włączone

    fun setKioskModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesManager.setKioskModeEnabled(enabled)
        }
    }

    fun setAutoRestartEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesManager.setAutoRestartEnabled(enabled)
        }
    }

    fun setTaskReopenEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesManager.setTaskReopenEnabled(enabled)
        }
    }
}

