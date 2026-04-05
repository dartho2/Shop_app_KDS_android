package com.itsorderkds.ui.kds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsorderkds.data.model.KdsTicket
import com.itsorderkds.data.model.KdsTicketItem
import com.itsorderkds.data.model.KdsTicketWithItemsResponse
import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.repository.KdsRepository
import com.itsorderkds.service.KdsItemStateEvent
import com.itsorderkds.service.KdsSocketEventsRepository
import com.itsorderkds.service.KdsTicketStateEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// ─── Stan UI ────────────────────────────────────────────────────────────────

data class KdsUiState(
    val isLoading: Boolean = true,
    val socketConnected: Boolean = false,
    val errorMessage: String? = null,
    /** Aktywny filtr: null = aktywne, "SCHEDULED" = zaplanowane, stan KDS = filtr stanu */
    val activeFilter: String? = null,
    /** Dialog potwierdzenia anulowania */
    val cancelDialogTicketId: String? = null
)

/** Ticket + jego pozycje trzymane razem w mapie */
data class KdsTicketEntry(
    val ticket: KdsTicket,
    val items: List<KdsTicketItem>
)

// ─── ViewModel ──────────────────────────────────────────────────────────────

@HiltViewModel
class KdsViewModel @Inject constructor(
    private val kdsRepository: KdsRepository,
    private val kdsSocketEventsRepo: KdsSocketEventsRepository
) : ViewModel() {

    private val TAG = "KdsViewModel"

    // Stan UI
    private val _uiState = MutableStateFlow(KdsUiState())
    val uiState: StateFlow<KdsUiState> = _uiState.asStateFlow()

    // Mapa ticketId → KdsTicketEntry (core state)
    private val _ticketsMap = MutableStateFlow<Map<String, KdsTicketEntry>>(emptyMap())

    /**
     * Ticker co minutę — wymusza re-emisję _ticketsMap żeby filteredTickets
     * przeliczył się gdy zaplanowane tickety wchodzą w okno czasowe.
     */
    private val _minuteTick = MutableStateFlow(0L)

    // Lista przefiltrowana wg activeFilter
    val filteredTickets: StateFlow<List<KdsTicketEntry>> =
        combine(_ticketsMap, _uiState, _minuteTick) { map, uiState, _ ->
            val filter = uiState.activeFilter
            val nowMs  = System.currentTimeMillis()

            when (filter) {
                "SCHEDULED" -> {
                    // Zakładka "Zaplanowane" — tylko te które są > 30 min w przyszłości
                    map.values
                        .filter { it.ticket.isScheduled() && it.ticket.state in listOf("NEW", "ACKED") && it.ticket.isScheduledFuture(nowMs) }
                        .sortedBy { it.ticket.scheduledFor }
                }
                else -> {
                    map.values
                        .filter { entry ->
                            val state = entry.ticket.state
                            if (state !in listOf("NEW", "ACKED", "IN_PROGRESS", "READY")) return@filter false

                            // Filtr po stanie (NEW, ACKED, IN_PROGRESS, READY)
                            if (filter != null) return@filter state == filter

                            // Widok "Aktywne" (filter == null):
                            // - Pokaż ZAWSZE jeśli IN_PROGRESS lub READY
                            // - Pokaż jeśli zaplanowane i zostało ≤ 30 min do realizacji
                            // - Ukryj jeśli zaplanowane i jeszcze daleko (> 30 min)
                            if (state in listOf("IN_PROGRESS", "READY")) return@filter true
                            if (entry.ticket.isScheduled()) {
                                return@filter !entry.ticket.isScheduledFuture(nowMs)
                            }
                            true
                        }
                        .sortedWith(
                            compareByDescending<KdsTicketEntry> { it.ticket.priority == "rush" }
                                .thenBy { entry ->
                                    // Zaplanowane sortujemy wg scheduledFor, reszta wg createdAt
                                    entry.ticket.scheduledFor ?: entry.ticket.createdAt
                                }
                        )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Licznik zaplanowanych ukrytych (do badge na filtrze "Zaplanowane") */
    val scheduledFutureCount: StateFlow<Int> =
        combine(_ticketsMap, _minuteTick) { map, _ ->
            val nowMs = System.currentTimeMillis()
            map.values.count {
                it.ticket.isScheduled() &&
                it.ticket.state in listOf("NEW", "ACKED") &&
                it.ticket.isScheduledFuture(nowMs)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // Eventy (snackbar, dźwięk nowego ticketu)
    private val _events = MutableSharedFlow<KdsEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    init {
        loadActiveTickets()
        observeSocketEvents()
    }

    // ─── Ładowanie przy starcie ─────────────────────────────────────────────

    fun loadActiveTickets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Pobierz wszystkie aktywne tickety (NEW, ACKED, IN_PROGRESS, READY)
                val states = listOf("NEW", "ACKED", "IN_PROGRESS", "READY")
                val newMap = mutableMapOf<String, KdsTicketEntry>()

                for (state in states) {
                    when (val res = kdsRepository.getTickets(state = state, limit = 100)) {
                        is Resource.Success -> {
                            res.value.data.forEach { ticket ->
                                // Pobierz items dla każdego ticketu
                                when (val itemsRes = kdsRepository.getTicketWithItems(ticket.id)) {
                                    is Resource.Success -> {
                                        newMap[ticket.id] = KdsTicketEntry(
                                            ticket = itemsRes.value.ticket,
                                            items = itemsRes.value.items
                                        )
                                    }
                                    is Resource.Failure -> {
                                        // Wstaw ticket bez items
                                        newMap[ticket.id] = KdsTicketEntry(ticket = ticket, items = emptyList())
                                        Timber.tag(TAG).w("Failed to fetch items for ticket ${ticket.id}")
                                    }
                                    else -> Unit
                                }
                            }
                        }
                        is Resource.Failure -> {
                            Timber.tag(TAG).w("Failed to load tickets (state=$state): ${res.errorMessage}")
                        }
                        else -> Unit
                    }
                }

                _ticketsMap.value = newMap
                Timber.tag(TAG).i("✅ Loaded ${newMap.size} active KDS tickets")
                checkScheduledAlerts()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error loading active tickets")
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** Emituje alert gdy zaplanowany ticket wchodzi w okno 30 min */
    private fun checkScheduledAlerts() {
        _ticketsMap.value.values
            .filter { it.ticket.isScheduledSoon() && it.ticket.state in listOf("NEW", "ACKED") }
            .forEach { entry ->
                val mins = entry.ticket.minutesUntilScheduled() ?: return@forEach
                Timber.tag(TAG).i("⏰ Scheduled soon: ${entry.ticket.orderNumber}, ${mins}min left")
                _events.tryEmit(
                    KdsEvent.ScheduledSoon(
                        orderNumber = entry.ticket.orderNumber,
                        minutesLeft = mins.toInt()
                    )
                )
            }
    }

    // ─── Obsługa Socket.IO eventów ──────────────────────────────────────────

    private fun observeSocketEvents() {
        // Ticker co minutę — wymusza przeliczenie filteredTickets (zaplanowane wchodzą do aktywnych)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000)
                _minuteTick.value = System.currentTimeMillis()
                checkScheduledAlerts()
            }
        }

        // Nowy ticket z pełnymi items
        viewModelScope.launch {
            kdsSocketEventsRepo.ticketCreated.collect { response ->
                Timber.tag(TAG).i("🆕 Socket: KDS_TICKET_CREATED ${response.ticket.id}")
                addOrUpdateTicketEntry(response)
                _events.tryEmit(KdsEvent.NewTicket(response.ticket.orderNumber))
            }
        }

        // Zmiana stanu ticketu
        viewModelScope.launch {
            kdsSocketEventsRepo.ticketStateChanged.collect { event ->
                Timber.tag(TAG).i("🔄 Socket: ticket state change ${event.ticketId} → ${event.newState}")
                updateTicketState(event)
            }
        }

        // Zmiana stanu pozycji
        viewModelScope.launch {
            kdsSocketEventsRepo.itemStateChanged.collect { event ->
                Timber.tag(TAG).i("🔄 Socket: item state change ${event.itemId} → ${event.newState}")
                updateItemState(event)
            }
        }

        // Stan połączenia
        viewModelScope.launch {
            kdsSocketEventsRepo.connection.collect { connected ->
                _uiState.update { it.copy(socketConnected = connected) }
                if (connected) {
                    // Po reconnect odśwież dane
                    Timber.tag(TAG).i("🔌 Socket reconnected - refreshing tickets")
                    loadActiveTickets()
                }
            }
        }
    }

    private fun addOrUpdateTicketEntry(response: KdsTicketWithItemsResponse) {
        _ticketsMap.update { current ->
            current.toMutableMap().apply {
                put(response.ticket.id, KdsTicketEntry(response.ticket, response.items))
            }
        }
    }

    private fun updateTicketState(event: KdsTicketStateEvent) {
        _ticketsMap.update { current ->
            val entry = current[event.ticketId] ?: return@update current
            val updatedTicket = entry.ticket.copy(
                state       = event.newState,
                startedAt   = event.startedAt ?: entry.ticket.startedAt,
                readyAt     = event.readyAt ?: entry.ticket.readyAt,
                handedOffAt = event.handedOffAt ?: entry.ticket.handedOffAt,
                cancelledAt = event.cancelledAt ?: entry.ticket.cancelledAt
            )
            current.toMutableMap().apply {
                put(event.ticketId, entry.copy(ticket = updatedTicket))
            }
        }
    }

    private fun updateItemState(event: KdsItemStateEvent) {
        _ticketsMap.update { current ->
            val entry = current[event.ticketId] ?: return@update current
            val updatedItems = entry.items.map { item ->
                if (item.id == event.itemId) {
                    item.copy(
                        state   = event.newState,
                        firedAt = event.firedAt ?: item.firedAt,
                        doneAt  = event.doneAt ?: item.doneAt
                    )
                } else {
                    item
                }
            }
            current.toMutableMap().apply {
                put(event.ticketId, entry.copy(items = updatedItems))
            }
        }
    }

    // ─── Akcje kucharza ─────────────────────────────────────────────────────

    fun ackTicket(ticketId: String) {
        viewModelScope.launch {
            when (val res = kdsRepository.ackTicket(ticketId)) {
                is Resource.Success -> {
                    updateLocalTicket(res.value)
                    Timber.tag(TAG).d("✅ ACK ticket $ticketId")
                }
                is Resource.Failure -> {
                    _events.tryEmit(KdsEvent.Error(res.errorMessage ?: "Błąd ACK"))
                    Timber.tag(TAG).w("❌ ACK failed for $ticketId: ${res.errorMessage}")
                }
                else -> Unit
            }
        }
    }

    fun startTicket(ticketId: String) {
        viewModelScope.launch {
            when (val res = kdsRepository.startTicket(ticketId)) {
                is Resource.Success -> {
                    updateLocalTicket(res.value)
                    Timber.tag(TAG).d("✅ START ticket $ticketId")
                }
                is Resource.Failure -> {
                    _events.tryEmit(KdsEvent.Error(res.errorMessage ?: "Błąd START"))
                }
                else -> Unit
            }
        }
    }

    fun readyTicket(ticketId: String) {
        viewModelScope.launch {
            when (val res = kdsRepository.readyTicket(ticketId)) {
                is Resource.Success -> {
                    updateLocalTicket(res.value)
                    Timber.tag(TAG).d("✅ READY ticket $ticketId")
                }
                is Resource.Failure -> {
                    _events.tryEmit(KdsEvent.Error(res.errorMessage ?: "Błąd READY"))
                }
                else -> Unit
            }
        }
    }

    fun handoffTicket(ticketId: String) {
        viewModelScope.launch {
            when (val res = kdsRepository.handoffTicket(ticketId)) {
                is Resource.Success -> {
                    updateLocalTicket(res.value)
                    Timber.tag(TAG).d("✅ HANDOFF ticket $ticketId")
                }
                is Resource.Failure -> {
                    _events.tryEmit(KdsEvent.Error(res.errorMessage ?: "Błąd HANDOFF"))
                }
                else -> Unit
            }
        }
    }

    fun cancelTicket(ticketId: String, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cancelDialogTicketId = null) }
            when (val res = kdsRepository.cancelTicket(ticketId, reason)) {
                is Resource.Success -> {
                    updateLocalTicket(res.value)
                    Timber.tag(TAG).d("✅ CANCEL ticket $ticketId")
                }
                is Resource.Failure -> {
                    _events.tryEmit(KdsEvent.Error(res.errorMessage ?: "Błąd CANCEL"))
                }
                else -> Unit
            }
        }
    }

    fun startItem(itemId: String, ticketId: String) {
        viewModelScope.launch {
            when (val res = kdsRepository.startItem(itemId)) {
                is Resource.Success -> {
                    updateLocalItem(ticketId, res.value)
                }
                is Resource.Failure -> {
                    _events.tryEmit(KdsEvent.Error(res.errorMessage ?: "Błąd START pozycji"))
                }
                else -> Unit
            }
        }
    }

    fun readyItem(itemId: String, ticketId: String) {
        viewModelScope.launch {
            when (val res = kdsRepository.readyItem(itemId)) {
                is Resource.Success -> {
                    updateLocalItem(ticketId, res.value)
                }
                is Resource.Failure -> {
                    _events.tryEmit(KdsEvent.Error(res.errorMessage ?: "Błąd READY pozycji"))
                }
                else -> Unit
            }
        }
    }

    // ─── Filtr ──────────────────────────────────────────────────────────────

    fun setFilter(state: String?) {
        _uiState.update { it.copy(activeFilter = state) }
    }

    // ─── Dialog anulowania ───────────────────────────────────────────────────

    fun showCancelDialog(ticketId: String) {
        _uiState.update { it.copy(cancelDialogTicketId = ticketId) }
    }

    fun dismissCancelDialog() {
        _uiState.update { it.copy(cancelDialogTicketId = null) }
    }

    // ─── Pomocnicze ─────────────────────────────────────────────────────────

    private fun updateLocalTicket(ticket: KdsTicket) {
        _ticketsMap.update { current ->
            val entry = current[ticket.id]
            current.toMutableMap().apply {
                put(ticket.id, (entry ?: KdsTicketEntry(ticket, emptyList())).copy(ticket = ticket))
            }
        }
    }

    private fun updateLocalItem(ticketId: String, item: KdsTicketItem) {
        _ticketsMap.update { current ->
            val entry = current[ticketId] ?: return@update current
            val updatedItems = entry.items.map { if (it.id == item.id) item else it }
            current.toMutableMap().apply {
                put(ticketId, entry.copy(items = updatedItems))
            }
        }
    }
}

// ─── Eventy ─────────────────────────────────────────────────────────────────

sealed interface KdsEvent {
    data class NewTicket(val orderNumber: String) : KdsEvent
    data class Error(val message: String) : KdsEvent
    data class Success(val message: String) : KdsEvent
    data class ScheduledSoon(val orderNumber: String, val minutesLeft: Int) : KdsEvent
}
