package com.itsorderkds.ui.order.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.maps.android.PolyUtil
import com.itsorderkds.data.model.GeoPoint
import com.itsorderkds.data.model.OrderTras
import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.repository.OrdersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel zarządzający trasami kurierskimi.
 * Wydzielony z OrdersViewModel dla lepszej separacji odpowiedzialności.
 */
@HiltViewModel
class OrderRouteViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository
) : ViewModel() {

    private val _routeState = MutableStateFlow<OrderRouteState>(OrderRouteState.Loading)
    val routeState: StateFlow<OrderRouteState> = _routeState.asStateFlow()

    /**
     * Pobiera trasę dla kuriera z wszystkimi zamówieniami.
     */
    fun fetchRoute() = viewModelScope.launch {
        _routeState.value = OrderRouteState.Loading

        when (val result = ordersRepository.getOrderTras()) {
            is Resource.Success -> {
                _routeState.value = OrderRouteState.Success(result.value)
                Timber.d("Route fetched successfully with ${result.value.size} orders")
            }
            is Resource.Failure -> {
                _routeState.value = OrderRouteState.Error(
                    code = result.errorCode,
                    body = result.errorBody
                )
                Timber.e("Failed to fetch route: ${result.errorCode} ${result.errorBody}")
            }
            is Resource.Loading -> {
                // Already in loading state
            }
        }
    }

    /**
     * Dekoduje polyline do listy punktów geograficznych.
     */
    fun decodePolyline(encodedPolyline: String): List<GeoPoint> {
        return try {
            PolyUtil.decode(encodedPolyline).map { latLng ->
                GeoPoint(lat = latLng.latitude, lng = latLng.longitude)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode polyline")
            emptyList()
        }
    }

    /**
     * Oblicza całkowitą odległość trasy w kilometrach.
     */
    fun calculateTotalDistance(route: List<OrderTras>): Double {
        return route.sumOf { it.distance ?: 0.0 }
    }
}

/**
 * Stan UI dla trasy kurierskiej.
 */
sealed interface OrderRouteState {
    object Loading : OrderRouteState
    data class Success(val route: List<OrderTras>) : OrderRouteState
    data class Error(val code: Int?, val body: Any?) : OrderRouteState
}

