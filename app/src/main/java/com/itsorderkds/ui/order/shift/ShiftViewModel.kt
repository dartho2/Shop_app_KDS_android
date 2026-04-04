package com.itsorderkds.ui.order.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsorderkds.data.model.ShiftCheckIn
import com.itsorderkds.data.model.ShiftCheckOut
import com.itsorderkds.data.model.ShiftResponse
import com.itsorderkds.data.model.Vehicle
import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.repository.OrdersRepository
import com.itsorderkds.data.repository.VehiclesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel zarządzający zmianami kurierskimi (check-in/check-out).
 * Wydzielony z OrdersViewModel dla lepszej separacji odpowiedzialności.
 */
@HiltViewModel
class ShiftViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val vehiclesRepository: VehiclesRepository
) : ViewModel() {

    private val _shiftState = MutableStateFlow(ShiftUiState())
    val shiftState: StateFlow<ShiftUiState> = _shiftState.asStateFlow()

    private val _availableVehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val availableVehicles: StateFlow<List<Vehicle>> = _availableVehicles.asStateFlow()

    /**
     * Pobiera dostępne pojazdy do wyboru przy check-in.
     */
    fun fetchAvailableVehicles() = viewModelScope.launch {
        _shiftState.value = _shiftState.value.copy(isFetchingVehicles = true)

        when (val result = vehiclesRepository.getVehicles()) {
            is Resource.Success -> {
                _availableVehicles.value = result.value
                Timber.d("Fetched ${result.value.size} vehicles")
            }
            is Resource.Failure -> {
                Timber.e("Failed to fetch vehicles: ${result.errorCode} ${result.errorBody}")
            }
            is Resource.Loading -> {
                // Ignore loading state
            }
        }

        _shiftState.value = _shiftState.value.copy(isFetchingVehicles = false)
    }

    /**
     * Rozpoczyna zmianę (check-in) z wybranym pojazdem i lokalizacją.
     */
    fun checkIn(vehicle: Vehicle, latitude: Double, longitude: Double) = viewModelScope.launch {
        _shiftState.value = _shiftState.value.copy(isLoading = true, error = null)

        val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val body = ShiftCheckIn(
            date = currentDate,
            latitude = latitude,
            longitude = longitude,
            vehicleId = vehicle.id
        )

        when (val result = ordersRepository.checkIn(body)) {
            is Resource.Success -> {
                Timber.d("Check-in successful: ${result.value}")
                _shiftState.value = _shiftState.value.copy(
                    shiftStatus = result.value,
                    isActive = true,
                    isStatusKnown = true,
                    isLoading = false
                )
            }
            is Resource.Failure -> {
                val errorMsg = "Check-in failed: ${result.errorCode} ${result.errorBody}"
                Timber.e(errorMsg)
                _shiftState.value = _shiftState.value.copy(
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
     * Kończy zmianę (check-out).
     */
    fun checkOut() = viewModelScope.launch {
        _shiftState.value = _shiftState.value.copy(isLoading = true, error = null)

        val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val body = ShiftCheckOut(date = currentDate)

        when (val result = ordersRepository.checkOut(body)) {
            is Resource.Success -> {
                Timber.d("Check-out successful")
                _shiftState.value = _shiftState.value.copy(
                    shiftStatus = null,
                    isActive = false,
                    isStatusKnown = true,
                    isLoading = false
                )
            }
            is Resource.Failure -> {
                val errorMsg = "Check-out failed: ${result.errorCode} ${result.errorBody}"
                Timber.e(errorMsg)
                _shiftState.value = _shiftState.value.copy(
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
     * Pobiera aktualny status zmiany z serwera.
     */
    fun refreshShiftStatus() = viewModelScope.launch {
        _shiftState.value = _shiftState.value.copy(isLoading = true)

        val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        when (val result = ordersRepository.getShiftStatus(currentDate)) {
            is Resource.Success -> {
                val isActive = result.value != null
                _shiftState.value = _shiftState.value.copy(
                    shiftStatus = result.value,
                    isActive = isActive,
                    isStatusKnown = true,
                    isLoading = false
                )
            }
            is Resource.Failure -> {
                Timber.e("Failed to fetch shift status: ${result.errorCode}")
                _shiftState.value = _shiftState.value.copy(
                    isLoading = false,
                    isStatusKnown = false
                )
            }
            is Resource.Loading -> {
                // Ignore loading state
            }
        }
    }
}

/**
 * Stan UI dla zarządzania zmianami.
 */
data class ShiftUiState(
    val shiftStatus: ShiftResponse? = null,
    val isActive: Boolean = false,
    val isStatusKnown: Boolean = false,
    val isLoading: Boolean = false,
    val isFetchingVehicles: Boolean = false,
    val error: String? = null
)

