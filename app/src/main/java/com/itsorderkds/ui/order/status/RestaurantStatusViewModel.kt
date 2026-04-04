package com.itsorderkds.ui.order.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsorderkds.data.network.Resource
import com.itsorderkds.ui.order.SourceEnum
import com.itsorderkds.ui.theme.status.OpenHoursRepository
import com.itsorderkds.ui.theme.status.RestaurantStatusUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel zarządzający statusem restauracji (otwarte/zamknięte/pauza).
 * Wydzielony z OrdersViewModel dla lepszej separacji odpowiedzialności.
 */
@HiltViewModel
class RestaurantStatusViewModel @Inject constructor(
    private val openHoursRepository: OpenHoursRepository
) : ViewModel() {

    private val _statusState = MutableStateFlow(RestaurantStatusState())
    val statusState: StateFlow<RestaurantStatusState> = _statusState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        startAutoRefresh()
    }

    /**
     * Uruchamia automatyczne odświeżanie statusu co 30 sekund.
     */
    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                refreshStatus()
                delay(30_000L) // 30 sekund
            }
        }
    }

    /**
     * Odświeża status restauracji z serwera.
     */
    fun refreshStatus(kind: String? = null) = viewModelScope.launch {
        _statusState.value = _statusState.value.copy(isLoading = true)

        when (val result = openHoursRepository.refresh(kind)) {
            is Resource.Success -> {
                _statusState.value = _statusState.value.copy(
                    status = result.value,
                    isLoading = false,
                    error = null
                )
                Timber.d("Restaurant status refreshed: ${result.value}")
            }
            is Resource.Failure -> {
                val errorMsg = "Failed to fetch status: ${result.errorCode}"
                Timber.e(errorMsg)
                _statusState.value = _statusState.value.copy(
                    isLoading = false,
                    error = errorMsg
                )
            }
            is Resource.Loading -> {
                // Ignore loading state
            }
        }
    }

    /**
     * Ustawia pauzę w przyjmowaniu zamówień.
     */
    fun setPause(
        minutes: Int,
        reason: String? = null,
        portals: List<SourceEnum>? = null
    ) = viewModelScope.launch {
        _statusState.value = _statusState.value.copy(isLoading = true)

        when (val result = openHoursRepository.pause(
            minutes = minutes,
            reason = reason,
            portals = portals
        )) {
            is Resource.Success -> {
                Timber.d("Pause set successfully")
                _statusState.value = _statusState.value.copy(
                    status = result.value,
                    isLoading = false,
                    error = null
                )
            }
            is Resource.Failure -> {
                Timber.e("Failed to set pause: ${result.errorCode}")
                _statusState.value = _statusState.value.copy(
                    isLoading = false,
                    error = "Failed to set pause: ${result.errorCode}"
                )
            }
            is Resource.Loading -> {
                // Ignore loading state
            }
        }
    }

    /**
     * Usuwa pauzę.
     */
    fun clearPause() = viewModelScope.launch {
        _statusState.value = _statusState.value.copy(isLoading = true)

        when (val result = openHoursRepository.clearPause()) {
            is Resource.Success -> {
                Timber.d("Pause cleared successfully")
                _statusState.value = _statusState.value.copy(
                    status = result.value,
                    isLoading = false,
                    error = null
                )
            }
            is Resource.Failure -> {
                Timber.e("Failed to clear pause: ${result.errorCode}")
                _statusState.value = _statusState.value.copy(
                    isLoading = false,
                    error = "Failed to clear pause"
                )
            }
            is Resource.Loading -> {
                // Ignore loading state
            }
        }
    }

    /**
     * Ustawia restaurację jako zamkniętą/otwartą.
     */
    fun setClosed(closed: Boolean) = viewModelScope.launch {
        _statusState.value = _statusState.value.copy(isLoading = true)

        when (val result = openHoursRepository.setClosed(closed)) {
            is Resource.Success -> {
                Timber.d("Closed status set to: $closed")
                refreshStatus("setClosed")
            }
            is Resource.Failure -> {
                Timber.e("Failed to set closed: ${result.errorCode}")
                _statusState.value = _statusState.value.copy(
                    isLoading = false,
                    error = "Failed to set closed status"
                )
            }
            is Resource.Loading -> {
                // Ignore loading state
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}

/**
 * Stan UI dla statusu restauracji.
 */
data class RestaurantStatusState(
    val status: RestaurantStatusUi? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

