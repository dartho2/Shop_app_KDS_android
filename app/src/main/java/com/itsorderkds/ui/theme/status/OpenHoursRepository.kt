// com/itsorderchat/ui/theme/status/OpenHoursRepository.kt
package com.itsorderkds.ui.theme.status

import com.itsorderkds.data.enums.RestaurantStatus
import com.itsorderkds.data.enums.SourceOrder
import com.itsorderkds.data.model.AvailabilityNowDto
import com.itsorderkds.data.model.OpenHoursAdminDto
import com.itsorderkds.data.model.OpenHoursDto
import com.itsorderkds.data.model.PauseRequest
import com.itsorderkds.data.model.UpdateOpenHoursRequest
import com.itsorderkds.data.network.OpenHoursApi
import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.repository.BaseRepository
import com.itsorderkds.ui.order.SourceEnum
import com.itsorderkds.ui.theme.GlobalMessageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenHoursRepository @Inject constructor(
    private val api: OpenHoursApi,
    messageManager: GlobalMessageManager
) : BaseRepository(messageManager) {

    private val _status = MutableStateFlow<RestaurantStatusUi?>(null)
    val status: StateFlow<RestaurantStatusUi?> = _status.asStateFlow()

    // === PUBLIC ===
    fun updateStatusFromSocket(dto: AvailabilityNowDto) {
        // Używamy tej samej prywatnej logiki mapowania, co w funkcji refresh()
        val uiModel = dto.toUi()
        _status.value = uiModel
    }

    suspend fun refresh(kind: String? = null): Resource<RestaurantStatusUi> =
        safeApiCall { api.getAvailabilityNow(kind) }
            .mapSuccess { dto ->
                dto.toUi().also { _status.value = it }
            }

    suspend fun pause(
        minutes: Int,
        reason: String?,
        portals: List<SourceEnum>? // <-- NOWY PARAMETR
    ): Resource<RestaurantStatusUi> {

        // Konwertujemy Enum na listę Stringów, np. ["UBER", "GLOVO"]
        val portalStrings = portals?.map { it.name }

        val request = PauseRequest(
            minutes = minutes,
            reason = reason,
            portals = portalStrings // <-- Przekazujemy nową listę do API
        )

        return safeApiCall { api.setPause(request) }
            .mapSuccess { dto -> dto.toUi().also { _status.value = it } }
    }

    suspend fun clearPause(): Resource<RestaurantStatusUi> =
        safeApiCall { api.clearPause() }
            .mapSuccess { dto -> dto.toUi().also { _status.value = it } }

    suspend fun setClosed(
        closed: Boolean,
        optimistic: Boolean = true,
        portals: List<SourceEnum>? = null // <-- NOWY PARAMETR dla wyboru portali
    ): Resource<Unit> {
        val prev = _status.value
        if (optimistic) {
            _status.value = RestaurantStatusUi(
                status = if (closed) RestaurantStatus.CLOSED else RestaurantStatus.OPEN,
                untilIso = null,
                message = null,
                source = SourceOrder.MANUAL
            )
        }

        // Konwertujemy Enum na listę Stringów, np. ["UBER", "GLOVO"]
        val portalStrings = portals?.map { it.name }

        val requestBody = UpdateOpenHoursRequest(
            isOpen = !closed,
            portals = portalStrings
        )

        timber.log.Timber.d("🔄 setClosed REQUEST: closed=$closed, portals=$portals")
        timber.log.Timber.d("🔄 Request body: isOpen=${requestBody.isOpen}, portals=${requestBody.portals}")

        val res = safeApiCall {
            api.updateOpenHours(requestBody)
        }.mapSuccess { Unit }
        if (optimistic && res is Resource.Failure) _status.value = prev
        return res
    }

    // ADMIN
    suspend fun getOpenHoursPublic(): Resource<OpenHoursDto> =
        safeApiCall { api.getOpenHoursPublic() }

    suspend fun getOpenHoursAdmin(): Resource<OpenHoursAdminDto> =
        safeApiCall { api.getOpenHoursAdmin() }

    suspend fun updateOpenHours(body: UpdateOpenHoursRequest): Resource<OpenHoursAdminDto> =
        safeApiCall { api.updateOpenHours(body) }

    suspend fun createOpenHours(body: UpdateOpenHoursRequest): Resource<OpenHoursAdminDto> =
        safeApiCall { api.createOpenHours(body) }
}

/** Lekki model do UI (chip + sheet). */
data class RestaurantStatusUi(
    val status: RestaurantStatus,
    val untilIso: String?,
    val message: String?,        // tutaj trafia np. "TEMPORARY-PAUSE"
    val source: SourceOrder?
)

// ---- MAPOWANIE na UI ----
private fun AvailabilityNowDto.toUi(): RestaurantStatusUi {
    val normalizedReason = reason
        .replace('-', '_')
        .uppercase()

    val status = when {
        accepting -> RestaurantStatus.OPEN
        normalizedReason == "TEMPORARY_PAUSE" -> RestaurantStatus.PAUSED
        else -> RestaurantStatus.CLOSED
    }

    return RestaurantStatusUi(
        status = status,
        // do timera używamy nextChangeAt, bo to „kiedy się zmieni”
        untilIso = nextChangeAt,
        message = null, // (nowe API nie zwraca tu free-text; jeśli chcesz, możesz zbudować opis sam)
        source = source // już jest enumem: SourceOrder?
    )
}
