package com.itsorderkds.service

import com.itsorderkds.data.model.KdsTicketWithItemsResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repozytorium eventów Socket.IO dla KDS (Kitchen Display System).
 * Emituje zdarzenia do KdsViewModel przez SharedFlow.
 */
@Singleton
class KdsSocketEventsRepository @Inject constructor() {

    // ──── Nowy ticket z items (emitowany po KDS_TICKET_CREATED + fetch HTTP) ────
    private val _ticketCreated = MutableSharedFlow<KdsTicketWithItemsResponse>(replay = 0, extraBufferCapacity = 32)
    val ticketCreated = _ticketCreated.asSharedFlow()

    // ──── Zmiana stanu ticketu (emitowana przy KDS_TICKET_*) ────
    private val _ticketStateChanged = MutableSharedFlow<KdsTicketStateEvent>(replay = 0, extraBufferCapacity = 64)
    val ticketStateChanged = _ticketStateChanged.asSharedFlow()

    // ──── Zmiana stanu pozycji (emitowana przy KDS_ITEM_*) ────
    private val _itemStateChanged = MutableSharedFlow<KdsItemStateEvent>(replay = 0, extraBufferCapacity = 64)
    val itemStateChanged = _itemStateChanged.asSharedFlow()

    // ──── Połączenie Socket.IO ────
    private val _connection = MutableSharedFlow<Boolean>(replay = 1)
    val connection = _connection.asSharedFlow()

    fun emitTicketCreated(response: KdsTicketWithItemsResponse) {
        Timber.tag(TAG).d("emitTicketCreated: ticketId=${response.ticket.id}")
        _ticketCreated.tryEmit(response)
    }

    fun emitTicketStateChanged(event: KdsTicketStateEvent) {
        Timber.tag(TAG).d("emitTicketStateChanged: ticketId=${event.ticketId}, state=${event.newState}")
        _ticketStateChanged.tryEmit(event)
    }

    fun emitItemStateChanged(event: KdsItemStateEvent) {
        Timber.tag(TAG).d("emitItemStateChanged: itemId=${event.itemId}, state=${event.newState}")
        _itemStateChanged.tryEmit(event)
    }

    fun emitConnected() = _connection.tryEmit(true)
    fun emitDisconnected() = _connection.tryEmit(false)

    private companion object {
        const val TAG = "KdsSocketEventsRepo"
    }
}

/**
 * Event zmiany stanu ticketu KDS (z Socket.IO payload)
 */
data class KdsTicketStateEvent(
    val ticketId: String,
    val newState: String,
    val startedAt: String? = null,
    val readyAt: String? = null,
    val handedOffAt: String? = null,
    val cancelledAt: String? = null,
    val actorId: String? = null
)

/**
 * Event zmiany stanu pozycji KDS (z Socket.IO payload)
 */
data class KdsItemStateEvent(
    val itemId: String,
    val ticketId: String,
    val newState: String,
    val firedAt: String? = null,
    val doneAt: String? = null,
    val actorId: String? = null
)


