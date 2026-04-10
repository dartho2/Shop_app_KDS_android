package com.itsorderkds.ui.kds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsorderkds.data.dao.OrderDao
import com.itsorderkds.data.entity.mapper.OrderMapper
import com.itsorderkds.data.model.KdsTicket
import com.itsorderkds.data.model.KdsTicketItem
import com.itsorderkds.data.model.KdsTicketWithItemsResponse
import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.preferences.AppPreferencesManager
import com.itsorderkds.data.repository.KdsRepository
import com.itsorderkds.service.KdsItemStateEvent
import com.itsorderkds.service.KdsSocketEventsRepository
import com.itsorderkds.service.KdsTicketStateEvent
import com.itsorderkds.service.SocketManager
import com.itsorderkds.ui.settings.print.PrinterService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// ─── Tryby pracy KDS ────────────────────────────────────────────────────────

/**
 * false = tryb prosty: tylko ACTIVE / READY (domyślny, dla sushi/bar/wydawka)
 * true  = tryb kolejkowy: NEW → ACKED → IN_PROGRESS → READY → HANDED_OFF
 */
typealias QueueMode = Boolean

/**
 * Tryb wyświetlania zamówień w siatce.
 *
 * COMPACT_FLOW  — standardowy LazyVerticalGrid, elementy przesuwają się automatycznie
 * STABLE_GRID   — stałe sloty; usunięcie zamówienia nie zmienia pozycji pozostałych
 * COLUMN_MODE   — niezależne kolumny (round-robin); usunięcie nie wpływa na inne kolumny
 */
enum class KdsDisplayMode { COMPACT_FLOW, STABLE_GRID, COLUMN_MODE }

// ─── Mapa slotów (STABLE_GRID) ───────────────────────────────────────────────

/**
 * Przechowuje przypisanie slot-index → ticketId.
 * Null w slocie = wolne miejsce (ticket zniknął, inne nie zmieniają pozycji).
 */
data class SlotMap(
    val slots: Map<Int, String?> = emptyMap()
) {
    /** Przypisz ticket do pierwszego wolnego slotu lub na koniec */
    fun assign(ticketId: String, fillGaps: Boolean): SlotMap {
        if (fillGaps) {
            val freeSlot = slots.entries.firstOrNull { it.value == null }?.key
            if (freeSlot != null) {
                return copy(slots = slots + (freeSlot to ticketId))
            }
        }
        val nextSlot = if (slots.isEmpty()) 0 else slots.keys.max() + 1
        return copy(slots = slots + (nextSlot to ticketId))
    }

    /** Zwolnij slot zajmowany przez ticket — inne sloty NIE zmieniają pozycji */
    fun release(ticketId: String): SlotMap {
        val slot = slots.entries.firstOrNull { it.value == ticketId }?.key ?: return this
        return copy(slots = slots + (slot to null))
    }

    /** Posortowana lista (slotIndex, ticketId?) do renderowania */
    fun toSortedList(): List<Pair<Int, String?>> =
        slots.entries.sortedBy { it.key }.map { it.key to it.value }

    /** Usuń końcowe puste sloty */
    fun compact(): SlotMap {
        val trimmed = slots.entries
            .sortedBy { it.key }
            .dropLastWhile { it.value == null }
        return copy(slots = trimmed.associate { it.key to it.value })
    }
}

// ─── Stan UI ────────────────────────────────────────────────────────────────

data class KdsUiState(
    val isLoading: Boolean = true,
    val socketConnected: Boolean = false,
    val errorMessage: String? = null,
    /** Aktywny filtr: null = aktywne, "SCHEDULED" = zaplanowane, stan KDS = filtr stanu */
    val activeFilter: String? = null,
    /** Dialog potwierdzenia anulowania */
    val cancelDialogTicketId: String? = null,
    /** Dialog potwierdzenia GOTOWE (wymaga confirm w ustawieniach) */
    val readyConfirmTicketId: String? = null,
    /** Tryb pracy: false = prosty, true = kolejkowy */
    val queueMode: Boolean = false,
    /** Czy wyświetlać zakładkę zaplanowane */
    val showScheduled: Boolean = true,
    /** Liczba kolumn gridu: 0 = auto */
    val gridColumns: Int = 0,
    /** Indeks zaznaczonego bloczka (fizyczne przyciski), -1 = brak */
    val focusedIndex: Int = -1,
    /** Czy pokazać panel Production Summary */
    val showProductionSummary: Boolean = false,
    /** Tryb wyświetlania zamówień */
    val displayMode: KdsDisplayMode = KdsDisplayMode.STABLE_GRID,
    /** Czy nowe zamówienia wypełniają wolne sloty (STABLE_GRID) */
    val fillGaps: Boolean = true,
    /** Mapa slotów dla STABLE_GRID */
    val slotMap: SlotMap = SlotMap(),
    /** Minuty przed odbiorem osobistym — kiedy zacząć gotować */
    val prepTimePickupMin: Int = 30,
    /** Minuty przed dostawą — kiedy zacząć gotować */
    val prepTimeDeliveryMin: Int = 60,
    /** Czy przycisk ANULUJ jest widoczny na karcie (domyślnie false) */
    val cancelEnabled: Boolean = false,
    /** Czy notatki/modyfikacje przy pozycjach są widoczne (domyślnie true) */
    val showNotes: Boolean = true,
    /** Klik nagłówka zmienia status — bez przycisków akcji (domyślnie false) */
    val headerTapMode: Boolean = false,
    /**
     * Za ile minut przed scheduledFor zamówienie przechodzi z "Plan" do "Aktywne".
     * Domyślnie 60 min. Konfigurowalne w ustawieniach.
     */
    val scheduledActiveWindowMin: Int = 60,
    /** Production Summary: minimalna ilość pozycji do wyświetlenia (1 = wszystkie, 2 = tylko qty>=2) */
    val productionMinQty: Int = 1,
    /** Production Summary: liczba kolumn (1 lub 2) */
    val productionColumns: Int = 1,
    /** Czy panel historii jest otwarty */
    val showHistoryPanel: Boolean = false,
    /**
     * Słowa kluczowe (lowercase) do ukrywania produktów na bloczkach i w Production Summary.
     * Lista parsowana z ustawień (przecinkami). Domyślnie ["opłata"].
     */
    val excludedKeywords: List<String> = listOf("opłata"),
    /** Skrócony bloczek kuchenny — mały nagłówek + lista pozycji bez zbędnych detali */
    val compactCardMode: Boolean = false,
    /** Czy dźwięk nowego zamówienia jest wyciszony (szybki toggle z panelu bocznego) */
    val soundMuted: Boolean = false,
    /** Czy drukowanie bloczków jest tymczasowo wstrzymane (szybki toggle z panelu bocznego) */
    val printingPaused: Boolean = false,
    /** Zestaw ticketId które mają aktualnie akcję w toku — do wizualnego blokowania przycisków */
    val inFlightIds: Set<String> = emptySet()
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
    private val kdsSocketEventsRepo: KdsSocketEventsRepository,
    private val prefs: AppPreferencesManager,
    private val printerService: PrinterService,
    private val orderDao: OrderDao
) : ViewModel() {

    private val TAG = "KdsViewModel"

    // Stan UI
    private val _uiState = MutableStateFlow(KdsUiState())
    val uiState: StateFlow<KdsUiState> = _uiState.asStateFlow()

    // Mapa ticketId → KdsTicketEntry (core state)
    private val _ticketsMap = MutableStateFlow<Map<String, KdsTicketEntry>>(emptyMap())

    /**
     * Guard przed podwójnym kliknięciem — zestaw ticketId które mają aktualnie
     * w locie żądanie HTTP. Jeśli ticketId jest w tym secie, kolejne kliknięcie
     * jest ignorowane do czasu zakończenia poprzedniego żądania.
     */
    private val _inFlightActions = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /** Dodaje ticketId do inFlight i synchronizuje z uiState (dla Compose recomposition) */
    private fun markInFlight(ticketId: String): Boolean {
        val added = _inFlightActions.add(ticketId)
        if (added) _uiState.update { it.copy(inFlightIds = _inFlightActions.toSet()) }
        return added
    }

    /** Usuwa ticketId z inFlight i synchronizuje z uiState */
    private fun clearInFlight(ticketId: String) {
        _inFlightActions.remove(ticketId)
        _uiState.update { it.copy(inFlightIds = _inFlightActions.toSet()) }
    }

    // Historia (HANDED_OFF + CANCELLED) — ładowana na żądanie
    private val _historyTickets = MutableStateFlow<List<KdsTicket>>(emptyList())
    val historyTickets: StateFlow<List<KdsTicket>> = _historyTickets.asStateFlow()

    private val _historyLoading = MutableStateFlow(false)
    val historyLoading: StateFlow<Boolean> = _historyLoading.asStateFlow()

    /**
     * Parsuje datę ISO 8601 do epochMs. Obsługuje:
     * - "2026-04-07T07:00:00.000Z" (Instant z ms)
     * - "2026-04-07T07:00:00Z"     (Instant bez ms)
     * - "2026-04-07T09:00:00+02:00" (ZonedDateTime)
     */
    private fun parseIsoToEpochMs(isoDate: String): Long? = runCatching {
        java.time.Instant.parse(isoDate).toEpochMilli()
    }.recoverCatching {
        java.time.ZonedDateTime.parse(isoDate).toInstant().toEpochMilli()
    }.recoverCatching {
        java.time.OffsetDateTime.parse(isoDate).toInstant().toEpochMilli()
    }.getOrNull()

    /**
     * Ticker co minutę — wymusza re-emisję _ticketsMap żeby filteredTickets
     * przeliczył się gdy zaplanowane tickety wchodzą w okno czasowe.
     */
    private val _minuteTick = MutableStateFlow(0L)

    // Lista przefiltrowana wg activeFilter
    val filteredTickets: StateFlow<List<KdsTicketEntry>> =
        combine(_ticketsMap, _uiState, _minuteTick) { map, uiState, _ ->
            val filter = uiState.activeFilter
            val queueMode = uiState.queueMode
            val nowMs = System.currentTimeMillis()

            // Aktywne stany zależne od trybu
            val activeStates = if (queueMode)
                listOf("NEW", "ACKED", "IN_PROGRESS", "READY")
            else
                listOf("ACTIVE", "READY", "NEW", "ACKED", "IN_PROGRESS")

            // Próg wejścia do Aktywnych — konfigurowalne w ustawieniach (domyślnie 60 min)
            val activeWindowMin = uiState.scheduledActiveWindowMin

            when (filter) {
                "SCHEDULED" -> {
                    // Zakładka "Plan" — zamówienia zaplanowane które są DALEJ niż activeWindowMin.
                    val result = map.values
                        .filter { entry ->
                            if (!entry.ticket.isScheduled()) {
                                Timber.tag("KDS_SCHED").d("❌ ${entry.ticket.orderNumber}: brak scheduledFor")
                                return@filter false
                            }
                            if (entry.ticket.state !in listOf("NEW", "ACKED", "ACTIVE", "IN_PROGRESS")) {
                                Timber.tag("KDS_SCHED").d("❌ ${entry.ticket.orderNumber}: zły stan=${entry.ticket.state}")
                                return@filter false
                            }
                            val minsLeft = entry.ticket.minutesUntilScheduled(nowMs)
                            if (minsLeft == null) {
                                Timber.tag("KDS_SCHED").d("❌ ${entry.ticket.orderNumber}: minsLeft=null")
                                return@filter false
                            }
                            val ok = minsLeft > activeWindowMin
                            Timber.tag("KDS_SCHED").d(
                                "${if (ok) "✅" else "❌"} ${entry.ticket.orderNumber}: " +
                                "state=${entry.ticket.state}, minsLeft=$minsLeft, window=$activeWindowMin, ok=$ok"
                            )
                            ok
                        }
                        .sortedBy { it.ticket.scheduledFor }
                    Timber.tag("KDS_SCHED").d("📋 SCHEDULED filter → ${result.size} ticketów (map.size=${map.size}, window=$activeWindowMin)")
                    result
                }
                else -> {
                    map.values
                        .filter { entry ->
                            val state = entry.ticket.state
                            if (state !in activeStates) return@filter false

                            // Filtr po konkretnym stanie (np. "NEW", "READY")
                            if (filter != null) return@filter state == filter

                            // Widok "Aktywne" (filter == null):
                            // Zamówienie zaplanowane chowamy do Planu gdy scheduledFor
                            // jest DALEJ niż activeWindowMin minut od teraz.
                            if (entry.ticket.isScheduled()) {
                                val minsLeft = entry.ticket.minutesUntilScheduled(nowMs)
                                if (minsLeft != null && minsLeft > activeWindowMin) {
                                    // Za daleko — idzie do Planu, nie do Aktywnych
                                    return@filter false
                                }
                            }
                            true
                        }
                        .sortedWith(
                            compareByDescending<KdsTicketEntry> { it.ticket.priority == "rush" }
                                .thenBy { entry ->
                                    // Klucz sortowania: "kiedy NAJPÓŹNIEJ trzeba zacząć gotować"
                                    // Im wcześniej trzeba zacząć → niższy klucz → wyżej na liście
                                    //
                                    // Dla zamówień zaplanowanych:
                                    //   startCookMs = scheduledFor - prepTime
                                    //   np. zamówienie na 9:00 z prepTime 30min → startCook = 8:30
                                    //   np. zamówienie na za 1h z prepTime 30min → startCook = za 30min
                                    //
                                    // Dla zamówień "na już" (bez scheduledFor):
                                    //   startCookMs = createdAt (już powinno być robione)
                                    val ticket = entry.ticket
                                    if (ticket.scheduledFor != null) {
                                        val scheduledMs = parseIsoToEpochMs(ticket.scheduledFor)
                                        if (scheduledMs != null) {
                                            val prepMin = when (ticket.orderType?.lowercase()) {
                                                "delivery" -> uiState.prepTimeDeliveryMin
                                                else       -> uiState.prepTimePickupMin
                                            }
                                            scheduledMs - prepMin * 60_000L
                                        } else {
                                            parseIsoToEpochMs(ticket.createdAt) ?: nowMs
                                        }
                                    } else {
                                        parseIsoToEpochMs(ticket.createdAt) ?: nowMs
                                    }
                                }
                        )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Licznik zaplanowanych (do badge na ikonie Plan) — spójny z filtrem SCHEDULED */
    val scheduledFutureCount: StateFlow<Int> =
        combine(_ticketsMap, _uiState, _minuteTick) { map, uiState, _ ->
            val nowMs = System.currentTimeMillis()
            val activeWindowMin = uiState.scheduledActiveWindowMin
            map.values.count { entry ->
                if (!entry.ticket.isScheduled()) return@count false
                if (entry.ticket.state !in listOf("NEW", "ACKED", "ACTIVE", "IN_PROGRESS")) return@count false
                val minsLeft = entry.ticket.minutesUntilScheduled(nowMs) ?: return@count false
                minsLeft > activeWindowMin
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // Eventy (snackbar, dźwięk nowego ticketu)
    private val _events = MutableSharedFlow<KdsEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    /**
     * Timestamp ostatniego pełnego loadActiveTickets().
     * Używany przez refreshOnResume() do throttlowania — max raz na 30s.
     */
    @Volatile
    private var lastLoadedMs = 0L

    init {
        // Inicjalizuj stan połączenia od razu — nie czekaj na pierwszy event
        _uiState.update { it.copy(socketConnected = SocketManager.isConnected()) }
        loadSettings()
        loadActiveTickets()
        observeSocketEvents()
    }

    /**
     * Wywołuj przy każdym ON_RESUME (powrót z tła / ustawień Androida).
     * Odświeża dane tylko jeśli od ostatniego załadowania minęło > 30 sekund.
     * Chroni przed zbędnymi requestami przy krótkich przełączeniach.
     */
    fun refreshOnResume() {
        val nowMs = System.currentTimeMillis()
        val elapsed = nowMs - lastLoadedMs
        if (elapsed > 30_000L) {
            Timber.tag(TAG).i("🔄 refreshOnResume: elapsed=${elapsed}ms — odświeżam tickets")
            loadActiveTickets()
        } else {
            Timber.tag(TAG).d("⏭ refreshOnResume: elapsed=${elapsed}ms < 30s — pomijam")
        }
    }

    // ─── Ładowanie ustawień ─────────────────────────────────────────────────

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                prefs.kdsQueueModeFlow,
                prefs.kdsShowScheduledFlow,
                prefs.kdsGridColumnsFlow,
                prefs.kdsShowProductionSummaryFlow,
                prefs.kdsDisplayModeFlow,
                prefs.kdsFillGapsFlow
            ) { arr ->
                @Suppress("UNCHECKED_CAST")
                arrayOf(
                    arr[0] as Boolean,  // queueMode
                    arr[1] as Boolean,  // showScheduled
                    arr[2] as String,   // gridColumnsStr
                    arr[3] as Boolean,  // showProd
                    arr[4] as String,   // displayModeStr
                    arr[5] as Boolean   // fillGaps
                )
            }.combine(
                combine(
                    prefs.kdsPrepTimePickupFlow,
                    prefs.kdsPrepTimeDeliveryFlow,
                    prefs.kdsCancelEnabledFlow,
                    prefs.kdsShowNotesFlow,
                    prefs.kdsHeaderTapModeFlow
                ) { pickup, delivery, cancel, showNotes, headerTap ->
                    arrayOf<Any>(pickup, delivery, cancel, showNotes, headerTap)
                }
            ) { base, extra ->
                base to extra
            }.combine(prefs.kdsScheduledActiveWindowFlow) { (base, extra), window ->
                Triple(base, extra, window)
            }.combine(
                combine(prefs.kdsProductionMinQtyFlow, prefs.kdsProductionColumnsFlow) { minQty, cols -> minQty to cols }
            ) { (base, extra, scheduledWindow), (prodMinQty, prodCols) ->
                object {
                    val b = base; val e = extra; val sw = scheduledWindow
                    val pmq = prodMinQty; val pc = prodCols
                }
            }.combine(prefs.kdsExcludedKeywordsFlow) { p, excludedKeywordsStr ->
                object {
                    val b = p.b; val e = p.e; val sw = p.sw
                    val pmq = p.pmq; val pc = p.pc
                    val ek = excludedKeywordsStr
                }
            }.combine(prefs.kdsCompactCardModeFlow) { p, compactCard ->
                object {
                    val b = p.b; val e = p.e; val sw = p.sw
                    val pmq = p.pmq; val pc = p.pc
                    val ek = p.ek; val cc = compactCard
                }
            }.combine(
                combine(prefs.kdsSoundMutedFlow, prefs.kdsPrintingPausedFlow) { sm, pp -> sm to pp }
            ) { p, (soundMuted, printingPaused) ->
                object {
                    val b = p.b; val e = p.e; val sw = p.sw
                    val pmq = p.pmq; val pc = p.pc
                    val ek = p.ek; val cc = p.cc
                    val sm = soundMuted; val pp = printingPaused
                }
            }.collect { p ->
                val base = p.b; val extra = p.e
                val scheduledWindow = p.sw
                val prodMinQty = p.pmq; val prodCols = p.pc
                val excludedKeywordsStr = p.ek
                val compactCard = p.cc
                val soundMuted  = p.sm
                val printingPaused = p.pp
                val queueMode       = base[0] as Boolean
                val showScheduled   = base[1] as Boolean
                val gridColumnsStr  = base[2] as String
                val showProd        = base[3] as Boolean
                val displayModeStr  = base[4] as String
                val fillGaps        = base[5] as Boolean
                val pickupMin       = extra[0] as Int
                val deliveryMin     = extra[1] as Int
                val cancelEnabled   = extra[2] as Boolean
                val showNotes       = extra[3] as Boolean
                val headerTapMode   = extra[4] as Boolean
                val cols = when (gridColumnsStr) { "2" -> 2; "3" -> 3; "4" -> 4; else -> 0 }
                val displayMode = runCatching {
                    KdsDisplayMode.valueOf(displayModeStr)
                }.getOrDefault(KdsDisplayMode.STABLE_GRID)
                val excludedKeywords = excludedKeywordsStr
                    .split(",")
                    .map { it.trim().lowercase() }
                    .filter { it.isNotBlank() }
                _uiState.update {
                    it.copy(
                        queueMode                = queueMode,
                        showScheduled            = showScheduled,
                        gridColumns              = cols,
                        showProductionSummary    = showProd,
                        displayMode              = displayMode,
                        fillGaps                 = fillGaps,
                        prepTimePickupMin        = pickupMin,
                        prepTimeDeliveryMin      = deliveryMin,
                        cancelEnabled            = cancelEnabled,
                        showNotes                = showNotes,
                        headerTapMode            = headerTapMode,
                        scheduledActiveWindowMin = scheduledWindow,
                        productionMinQty         = prodMinQty,
                        productionColumns        = prodCols,
                        excludedKeywords         = excludedKeywords,
                        compactCardMode          = compactCard,
                        soundMuted               = soundMuted,
                        printingPaused           = printingPaused
                    )
                }
            }
        }
    }

    // ─── Ładowanie przy starcie ─────────────────────────────────────────────

    fun loadActiveTickets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val states = listOf("NEW", "ACKED", "IN_PROGRESS", "READY", "ACTIVE")
                val newMap = mutableMapOf<String, KdsTicketEntry>()

                for (state in states) {
                    when (val res = kdsRepository.getTickets(state = state, limit = 100)) {
                        is Resource.Success -> {
                            res.value.data.forEach { ticket ->
                                // Zachowaj istniejące items jako fallback (np. z wcześniejszego socket eventu)
                                val existingItems = _ticketsMap.value[ticket.id]?.items ?: emptyList()

                                when (val itemsRes = kdsRepository.getTicketWithItems(ticket.id)) {
                                    is Resource.Success -> {
                                        val fetchedItems = itemsRes.value.items
                                        // WAŻNE: jeśli API zwróciło pustą listę items (race condition
                                        // podczas tworzenia ticketu) — zachowaj istniejące items z mapy.
                                        // Zapobiega to "pustym bloczkom" gdy API jeszcze nie ma items.
                                        val finalItems = if (fetchedItems.isEmpty() && existingItems.isNotEmpty()) {
                                            Timber.tag(TAG).w(
                                                "⚠️ API zwróciło 0 items dla ${ticket.id} " +
                                                "(existingItems=${existingItems.size}) — zachowuję istniejące"
                                            )
                                            existingItems
                                        } else {
                                            fetchedItems
                                        }
                                        newMap[ticket.id] = KdsTicketEntry(
                                            ticket = itemsRes.value.ticket,
                                            items  = finalItems
                                        )
                                    }
                                    is Resource.Failure -> {
                                        // HTTP fail — zachowaj istniejące items (mogą być z socket)
                                        Timber.tag(TAG).w(
                                            "⚠️ getTicketWithItems failed for ${ticket.id}: ${itemsRes.errorMessage} " +
                                            "— keeping ${existingItems.size} existing items"
                                        )
                                        newMap[ticket.id] = KdsTicketEntry(ticket = ticket, items = existingItems)
                                    }
                                    else -> {
                                        newMap[ticket.id] = KdsTicketEntry(ticket = ticket, items = existingItems)
                                    }
                                }
                            }
                        }
                        is Resource.Failure -> {
                            Timber.tag(TAG).w("Failed to load tickets (state=$state): ${res.errorMessage}")
                        }
                        else -> Unit
                    }
                }

                // WAŻNE: Scal nową mapę z istniejącą, zamiast całkowicie zastępować.
                // Zachowuje tickets które dotarły przez socket PODCZAS ładowania
                // i nie weszły jeszcze do nowej mapy (np. ticket stworzony podczas fetch).
                val currentMap = _ticketsMap.value
                val mergedMap = currentMap.toMutableMap()
                newMap.forEach { (id, entry) ->
                    val current = mergedMap[id]
                    // Jeśli nowy entry ma więcej items niż obecny — użyj nowego
                    // Jeśli obecny ma więcej items (z socket) — zachowaj obecne items
                    if (current != null && entry.items.isEmpty() && current.items.isNotEmpty()) {
                        mergedMap[id] = entry.copy(items = current.items)
                        Timber.tag(TAG).d("🔀 Merge: zachowuję ${current.items.size} items z socket dla $id")
                    } else {
                        mergedMap[id] = entry
                    }
                }
                // Usuń z mapy tickety które zniknęły z aktywnych (zmieniły stan)
                // ale zachowaj te które nie były w zapytaniu (np. właśnie przyszły przez socket)
                val loadedIds = newMap.keys
                val socketRecentIds = mergedMap.keys - loadedIds
                // Tickety z socket (nie w odpowiedzi HTTP) — usuń tylko jeśli są w stanie końcowym
                socketRecentIds.forEach { id ->
                    val entry = mergedMap[id]
                    if (entry != null && entry.ticket.state in listOf("HANDED_OFF", "CANCELLED")) {
                        mergedMap.remove(id)
                    }
                }

                _ticketsMap.value = mergedMap

                // Zapisz czas ostatniego załadowania (dla throttlowania refreshOnResume)
                lastLoadedMs = System.currentTimeMillis()

                // Przebuduj slotMapę po pełnym odświeżeniu
                rebuildSlotMap()

                Timber.tag(TAG).i("✅ Loaded ${newMap.size} tickets (merged: ${mergedMap.size})")
                checkScheduledAlerts()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error loading active tickets")
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** Emituje alert gdy zaplanowany ticket wchodzi w okno 60 min */
    private fun checkScheduledAlerts() {
        _ticketsMap.value.values
            .filter { it.ticket.isScheduledSoon() && it.ticket.state in listOf("NEW", "ACKED", "ACTIVE") }
            .forEach { entry ->
                val mins = entry.ticket.minutesUntilScheduled() ?: return@forEach
                Timber.tag(TAG).i("⏰ Scheduled soon: ${entry.ticket.orderNumber}, ${mins}min left")
                _events.tryEmit(
                    KdsEvent.ScheduledSoon(
                        displayNumber = entry.ticket.displayNumber,
                        minutesLeft   = mins.toInt()
                    )
                )
            }
    }

    // ─── Obsługa Socket.IO eventów ──────────────────────────────────────────

    private fun observeSocketEvents() {
        // Ticker co minutę
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000)
                _minuteTick.value = System.currentTimeMillis()
                checkScheduledAlerts()
            }
        }

        // Nowy ticket
        viewModelScope.launch {
            kdsSocketEventsRepo.ticketCreated.collect { response ->
                Timber.tag(TAG).i("🆕 Socket: KDS_TICKET_CREATED ${response.ticket.id}")
                addOrUpdateTicketEntry(response)
                _events.tryEmit(KdsEvent.NewTicket(response.ticket.displayNumber))
            }
        }

        // Zmiana stanu ticketu
        viewModelScope.launch {
            kdsSocketEventsRepo.ticketStateChanged.collect { event ->
                Timber.tag(TAG).i("🔄 Socket: ticket state change ${event.ticketId} → ${event.newState}")
                updateTicketState(event)
                // UWAGA: usunięcia ticketu po HANDED_OFF/CANCELLED obsługują handoffTicket()
                // i cancelTicket() z poziomu akcji kucharza. Socket event NIE usuwa samodzielnie,
                // bo handoffTicket już to zaplanował — podwójne usunięcie było przyczyną
                // race condition i znikania produktów z bloczków.
                //
                // Wyjątek: jeśli ticket trafił w stan HANDED_OFF/CANCELLED przez zewnętrzną
                // akcję (inny KDS, panel admina) — wtedy ticket nie jest w trakcie akcji kucharza
                // i trzeba go usunąć tutaj. Sprawdzamy czy ticket nadal jest w mapie po 4s
                // (akcja kucharza usunie go w 3s).
                if (event.newState in listOf("HANDED_OFF", "CANCELLED")) {
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(4_000)
                        // Jeśli ticket nadal istnieje po 4s — usuń (zewnętrzna akcja)
                        if (_ticketsMap.value.containsKey(event.ticketId)) {
                            Timber.tag(TAG).d("🗑️ Socket fallback remove: ${event.ticketId} (external action)")
                            removeTicket(event.ticketId)
                        }
                    }
                }
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
                    Timber.tag(TAG).i("🔌 Socket reconnected - refreshing tickets")
                    loadActiveTickets()
                }
            }
        }
    }

    private fun addOrUpdateTicketEntry(response: KdsTicketWithItemsResponse) {
        val ticketId = response.ticket.id
        val incomingItems = response.items

        _ticketsMap.update { current ->
            val existingEntry = current[ticketId]
            val finalItems = when {
                // Nowe items są niepuste — użyj nowych
                incomingItems.isNotEmpty() -> incomingItems
                // Nowe items puste, ale mamy stare — zachowaj stare (race condition na API)
                existingEntry != null && existingEntry.items.isNotEmpty() -> {
                    Timber.tag(TAG).w(
                        "⚠️ Socket: ticket $ticketId przyszedł z 0 items, " +
                        "zachowuję ${existingEntry.items.size} istniejących"
                    )
                    existingEntry.items
                }
                // Brak items w ogóle — prawdopodobnie API race condition, zaplanuj retry
                else -> {
                    Timber.tag(TAG).w("⚠️ Socket: ticket $ticketId — brak items, zaplanowano retry fetch")
                    emptyList()
                }
            }
            current.toMutableMap().apply {
                put(ticketId, KdsTicketEntry(response.ticket, finalItems))
            }
        }

        // Jeśli items były puste — ponów fetch po 2s (API może jeszcze tworzyć items)
        if (incomingItems.isEmpty()) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(2_000)
                val currentEntry = _ticketsMap.value[ticketId]
                if (currentEntry != null && currentEntry.items.isEmpty()) {
                    Timber.tag(TAG).d("🔄 Retry fetch items dla $ticketId (po 2s, items nadal puste)")
                    when (val res = kdsRepository.getTicketWithItems(ticketId)) {
                        is Resource.Success -> {
                            val fetchedItems = res.value.items
                            if (fetchedItems.isNotEmpty()) {
                                _ticketsMap.update { current ->
                                    val entry = current[ticketId] ?: return@update current
                                    current.toMutableMap().apply {
                                        put(ticketId, entry.copy(
                                            ticket = res.value.ticket,
                                            items  = fetchedItems
                                        ))
                                    }
                                }
                                Timber.tag(TAG).i("✅ Retry: załadowano ${fetchedItems.size} items dla $ticketId")
                            }
                        }
                        else -> Timber.tag(TAG).w("⚠️ Retry fetch failed dla $ticketId")
                    }
                }
            }
        }

        // Przebuduj slotMapę z uwzględnieniem priorytetu
        rebuildSlotMap()
    }

    private fun removeTicket(ticketId: String) {
        _ticketsMap.update { current ->
            current.toMutableMap().apply { remove(ticketId) }
        }
        rebuildSlotMap()
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

    /**
     * Przebudowuje SlotMap zgodnie z aktualnym priorytetem kolejki.
     *
     * Używa tego samego klucza sortowania co filteredTickets:
     *   1. RUSH → pierwsze
     *   2. startCookMs (scheduledFor - prepTime, lub createdAt dla "na już") → rosnąco
     *
     * Efekt: nowe pilne zamówienie zawsze trafia na właściwą pozycję w siatce,
     * a nie na koniec jak przy zwykłym assign().
     */
    private fun rebuildSlotMap() {
        val state    = _uiState.value
        val nowMs    = System.currentTimeMillis()
        val fillGaps = state.fillGaps

        val activeStates = if (state.queueMode)
            listOf("NEW", "ACKED", "IN_PROGRESS", "READY")
        else
            listOf("ACTIVE", "READY", "NEW", "ACKED", "IN_PROGRESS")

        val sorted = _ticketsMap.value.values
            .filter { entry ->
                val s = entry.ticket.state
                if (s !in activeStates) return@filter false
                // Ukryj zaplanowane które są dalej niż activeWindowMin — idą do Planu
                if (entry.ticket.isScheduled()) {
                    val minsLeft = entry.ticket.minutesUntilScheduled(nowMs)
                    if (minsLeft != null && minsLeft > state.scheduledActiveWindowMin) {
                        return@filter false
                    }
                }
                true
            }
            .sortedWith(
                compareByDescending<KdsTicketEntry> { it.ticket.priority == "rush" }
                    .thenBy { entry ->
                        // Ten sam klucz co w filteredTickets
                        val ticket = entry.ticket
                        if (ticket.scheduledFor != null) {
                            val scheduledMs = parseIsoToEpochMs(ticket.scheduledFor)
                            if (scheduledMs != null) {
                                val prepMin = when (ticket.orderType?.lowercase()) {
                                    "delivery" -> state.prepTimeDeliveryMin
                                    else       -> state.prepTimePickupMin
                                }
                                scheduledMs - prepMin * 60_000L
                            } else {
                                parseIsoToEpochMs(ticket.createdAt) ?: nowMs
                            }
                        } else {
                            parseIsoToEpochMs(ticket.createdAt) ?: nowMs
                        }
                    }
            )

        // Przypisz sloty 0, 1, 2... według posortowanej kolejki
        var rebuilt = SlotMap()
        sorted.forEach { entry -> rebuilt = rebuilt.assign(entry.ticket.id, fillGaps) }

        _uiState.update { it.copy(slotMap = rebuilt) }
    }

    private fun updateItemState(event: KdsItemStateEvent) {        _ticketsMap.update { current ->
            val entry = current[event.ticketId] ?: return@update current
            val updatedItems = entry.items.map { item ->
                if (item.id == event.itemId) {
                    item.copy(
                        state   = event.newState,
                        firedAt = event.firedAt ?: item.firedAt,
                        doneAt  = event.doneAt ?: item.doneAt
                    )
                } else item
            }
            current.toMutableMap().apply {
                put(event.ticketId, entry.copy(items = updatedItems))
            }
        }
    }


    // ─── Akcje kucharza ─────────────────────────────────────────────────────

    fun ackTicket(ticketId: String) {
        if (!markInFlight(ticketId)) {
            Timber.tag(TAG).w("⏭ ackTicket $ticketId — już w toku, ignoruję")
            return
        }
        viewModelScope.launch {
            try {
                when (val res = kdsRepository.ackTicket(ticketId)) {
                    is Resource.Success -> {
                        updateLocalTicket(res.value)
                        Timber.tag(TAG).d("✅ ACK ticket $ticketId")
                        printByRuleForTicket(ticketId, "ACCEPTED")
                    }
                    is Resource.Failure -> {
                        _events.tryEmit(KdsEvent.Error(res.errorMessage ?: "Błąd ACK"))
                        Timber.tag(TAG).w("❌ ACK failed for $ticketId: ${res.errorMessage}")
                    }
                    else -> Unit
                }
            } finally {
                clearInFlight(ticketId)
            }
        }
    }

    fun startTicket(ticketId: String) {
        if (!markInFlight(ticketId)) {
            Timber.tag(TAG).w("⏭ startTicket $ticketId — już w toku, ignoruję")
            return
        }
        viewModelScope.launch {
            try {
                when (val res = kdsRepository.startTicket(ticketId)) {
                    is Resource.Success -> {
                        updateLocalTicket(res.value)
                        Timber.tag(TAG).d("✅ START ticket $ticketId")
                        printByRuleForTicket(ticketId, "IN_PROGRESS")
                    }
                    is Resource.Failure -> {
                        _events.tryEmit(KdsEvent.Error(res.errorMessage ?: "Błąd START"))
                    }
                    else -> Unit
                }
            } finally {
                clearInFlight(ticketId)
            }
        }
    }

    fun readyTicket(ticketId: String) {
        if (!markInFlight(ticketId)) {
            Timber.tag(TAG).w("⏭ readyTicket $ticketId — już w toku, ignoruję")
            return
        }
        viewModelScope.launch {
            try {
                val requireConfirm = prefs.kdsRequireReadyConfirmFlow.stateIn(
                    viewModelScope, SharingStarted.Eagerly, false
                ).value
                if (requireConfirm) {
                    _uiState.update { it.copy(readyConfirmTicketId = ticketId) }
                    clearInFlight(ticketId)
                    return@launch
                }
                doReadyTicket(ticketId)
            } catch (e: Exception) {
                clearInFlight(ticketId)
                throw e
            }
        }
    }

    fun confirmReady(ticketId: String) {
        _uiState.update { it.copy(readyConfirmTicketId = null) }
        if (!markInFlight(ticketId)) return
        viewModelScope.launch {
            try { doReadyTicket(ticketId) }
            finally { clearInFlight(ticketId) }
        }
    }

    fun dismissReadyConfirm() {
        _uiState.update { it.copy(readyConfirmTicketId = null) }
    }

    private suspend fun doReadyTicket(ticketId: String) {
        try {
            when (val res = kdsRepository.readyTicket(ticketId)) {
                is Resource.Success -> {
                    updateLocalTicket(res.value)
                    Timber.tag(TAG).d("✅ READY ticket $ticketId")
                    if (prefs.isKdsAutoPrintOnReady()) printTicketById(ticketId)
                    printByRuleForTicket(ticketId, "READY")
                }
                is Resource.Failure -> {
                    _events.tryEmit(KdsEvent.Error(res.errorMessage ?: "Błąd READY"))
                }
                else -> Unit
            }
        } finally {
            clearInFlight(ticketId)
        }
    }

    /**
     * Sprawdza reguły drukowania v2 dla podanego statusu i drukuje jeśli dopasowane.
     * Przekazuje [KdsTicketItem] do drukarki — umożliwia filtrowanie KITCHEN_ONLY.
     */
    private fun printByRuleForTicket(ticketId: String, state: String) {
        if (_uiState.value.printingPaused) {
            Timber.tag(TAG).d("🖨️ Drukowanie wstrzymane — pomijam bloczek dla $ticketId state=$state")
            return
        }
        viewModelScope.launch {
            try {
                val entry = _ticketsMap.value[ticketId] ?: return@launch
                val entity = orderDao.getOrderById(entry.ticket.orderId) ?: return@launch
                val order = OrderMapper.toOrder(entity)
                // Przekaż items — PrinterService użyje ich gdy szablon = KITCHEN_ONLY
                printerService.printOnTicketStatusChangeWithItems(order, state, entry.items)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ printByRuleForTicket: $ticketId state=$state")
            }
        }
    }

    /**
     * Pobiera zamówienie powiązane z ticketem i drukuje bloczek kuchenny.
     * Kolejność: lokalny Room → pomiń drukowanie jeśli brak zamówienia.
     */
    private fun printTicketById(ticketId: String) {
        if (_uiState.value.printingPaused) {
            Timber.tag(TAG).d("🖨️ Drukowanie wstrzymane — pomijam printTicketById dla $ticketId")
            return
        }
        viewModelScope.launch {
            try {
                val entry = _ticketsMap.value[ticketId]
                if (entry == null) {
                    Timber.tag(TAG).w("🖨️ Brak ticketu $ticketId w mapie — pomijam druk")
                    return@launch
                }
                val orderId = entry.ticket.orderId
                Timber.tag(TAG).d("🖨️ Drukuję bloczek dla ticketu=$ticketId orderId=$orderId")

                val entity = orderDao.getOrderById(orderId)
                if (entity == null) {
                    Timber.tag(TAG).w("🖨️ Brak zamówienia $orderId w bazie — pomijam druk")
                    return@launch
                }

                val order = OrderMapper.toOrder(entity)
                Timber.tag(TAG).d("🖨️ Zamówienie znalezione: #${order.orderNumber} — startuję druk")
                // Przekaż items aby drukarka mogła filtrować KITCHEN_ONLY
                printerService.printKitchenTicketWithItems(order, entry.items, useDeliveryInterval = false)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "❌ Błąd drukowania bloczka dla ticketu $ticketId")
            }
        }
    }

    fun handoffTicket(ticketId: String) {
        if (!markInFlight(ticketId)) {
            Timber.tag(TAG).w("⏭ handoffTicket $ticketId — już w toku, ignoruję")
            return
        }
        viewModelScope.launch {
            try {
                when (val res = kdsRepository.handoffTicket(ticketId)) {
                    is Resource.Success -> {
                        updateLocalTicket(res.value)
                        Timber.tag(TAG).d("✅ HANDOFF ticket $ticketId")
                        printByRuleForTicket(ticketId, "HANDED_OFF")
                        kotlinx.coroutines.delay(3_000)
                        removeTicket(ticketId)
                    }
                    is Resource.Failure -> {
                        _events.tryEmit(KdsEvent.Error(res.errorMessage ?: "Błąd HANDOFF"))
                    }
                    else -> Unit
                }
            } finally {
                clearInFlight(ticketId)
            }
        }
    }

    fun cancelTicket(ticketId: String, reason: String) {
        if (!markInFlight(ticketId)) {
            Timber.tag(TAG).w("⏭ cancelTicket $ticketId — już w toku, ignoruję")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cancelDialogTicketId = null) }
            try {
                when (val res = kdsRepository.cancelTicket(ticketId, reason)) {
                    is Resource.Success -> {
                        updateLocalTicket(res.value)
                        Timber.tag(TAG).d("✅ CANCEL ticket $ticketId")
                        kotlinx.coroutines.delay(3_000)
                        removeTicket(ticketId)
                    }
                    is Resource.Failure -> {
                        _events.tryEmit(KdsEvent.Error(res.errorMessage ?: "Błąd CANCEL"))
                    }
                    else -> Unit
                }
            } finally {
                clearInFlight(ticketId)
            }
        }
    }

    fun startItem(itemId: String, ticketId: String) {
        viewModelScope.launch {
            when (val res = kdsRepository.startItem(itemId)) {
                is Resource.Success -> updateLocalItem(ticketId, res.value)
                is Resource.Failure -> _events.tryEmit(KdsEvent.Error(res.errorMessage ?: "Błąd START pozycji"))
                else -> Unit
            }
        }
    }

    fun readyItem(itemId: String, ticketId: String) {
        viewModelScope.launch {
            when (val res = kdsRepository.readyItem(itemId)) {
                is Resource.Success -> updateLocalItem(ticketId, res.value)
                is Resource.Failure -> _events.tryEmit(KdsEvent.Error(res.errorMessage ?: "Błąd READY pozycji"))
                else -> Unit
            }
        }
    }

    // ─── Filtr ──────────────────────────────────────────────────────────────

    fun setFilter(state: String?) {
        Timber.tag(TAG).d("🔍 setFilter($state) — ticketsMap.size=${_ticketsMap.value.size}")
        _uiState.update { it.copy(activeFilter = state, focusedIndex = -1) }
        Timber.tag(TAG).d("🔍 After setFilter — uiState.activeFilter=${_uiState.value.activeFilter}, filteredTickets.size=${filteredTickets.value.size}")
    }

    // ─── Fizyczne przyciski (HID / Bluetooth) ───────────────────────────────

    /** NEXT — przesuń focus na następny bloczek */
    fun navigateNext() {
        val count = filteredTickets.value.size
        if (count == 0) return
        _uiState.update { state ->
            val next = if (state.focusedIndex < 0) 0
                       else (state.focusedIndex + 1).coerceAtMost(count - 1)
            state.copy(focusedIndex = next)
        }
    }

    /** PREV — przesuń focus na poprzedni bloczek */
    fun navigatePrev() {
        val count = filteredTickets.value.size
        if (count == 0) return
        _uiState.update { state ->
            val prev = if (state.focusedIndex < 0) 0
                       else (state.focusedIndex - 1).coerceAtLeast(0)
            state.copy(focusedIndex = prev)
        }
    }

    /** CONFIRM — potwierdź akcję na zaznaczonym bloczku (GOTOWE / WYDAJ) */
    fun confirmFocused() {
        val idx = _uiState.value.focusedIndex
        if (idx < 0) return
        val entry = filteredTickets.value.getOrNull(idx) ?: return
        when (entry.ticket.state) {
            "NEW", "ACTIVE", "ACKED", "IN_PROGRESS" -> readyTicket(entry.ticket.id)
            "READY"                                  -> handoffTicket(entry.ticket.id)
        }
    }

    /** CANCEL — anuluj zaznaczony bloczek */
    fun cancelFocused() {
        val idx = _uiState.value.focusedIndex
        if (idx < 0) return
        val entry = filteredTickets.value.getOrNull(idx) ?: return
        showCancelDialog(entry.ticket.id)
    }

    /** Wyczyść focus (dotknięcie ekranu) */
    fun clearFocus() {
        _uiState.update { it.copy(focusedIndex = -1) }
    }

    fun toggleHistoryPanel() {
        val opening = !_uiState.value.showHistoryPanel
        _uiState.update { it.copy(showHistoryPanel = opening) }
        if (opening) loadHistoryTickets()
    }

    fun loadHistoryTickets() {
        viewModelScope.launch {
            _historyLoading.value = true
            try {
                val handedOff = kdsRepository.getHistoryTickets(limit = 50)
                val cancelled = kdsRepository.getCancelledTickets(limit = 20)
                val combined = mutableListOf<KdsTicket>()
                if (handedOff is Resource.Success) combined.addAll(handedOff.value.data)
                if (cancelled is Resource.Success) combined.addAll(cancelled.value.data)
                // Sortuj po czasie wydania/anulowania — najnowsze pierwsze
                _historyTickets.value = combined.sortedByDescending {
                    it.handedOffAt ?: it.cancelledAt ?: it.updatedAt
                }
                Timber.tag(TAG).d("📜 Historia: ${combined.size} ticketów")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Błąd ładowania historii")
            } finally {
                _historyLoading.value = false
            }
        }
    }

    // ─── Dialog anulowania ───────────────────────────────────────────────────

    fun showCancelDialog(ticketId: String) {
        _uiState.update { it.copy(cancelDialogTicketId = ticketId) }
    }

    fun dismissCancelDialog() {
        _uiState.update { it.copy(cancelDialogTicketId = null) }
    }

    // ─── Szybkie przełączniki z panelu bocznego ─────────────────────────────

    /** Czy dany ticket ma aktualnie akcję w toku (guard podwójnego kliknięcia) */
    fun isInFlight(ticketId: String): Boolean = _inFlightActions.contains(ticketId)

    /** Przełącz wyciszenie dźwięku nowego zamówienia */
    fun toggleSoundMuted() {
        val newVal = !_uiState.value.soundMuted
        _uiState.update { it.copy(soundMuted = newVal) }
        viewModelScope.launch { prefs.setKdsSoundMuted(newVal) }
    }

    /** Przełącz wstrzymanie drukowania bloczków */
    fun togglePrintingPaused() {
        val newVal = !_uiState.value.printingPaused
        _uiState.update { it.copy(printingPaused = newVal) }
        viewModelScope.launch { prefs.setKdsPrintingPaused(newVal) }
    }

    // ─── Pomocnicze ─────────────────────────────────────────────────────────

    private fun updateLocalTicket(ticket: KdsTicket) {
        val current = _ticketsMap.value
        val entry = current[ticket.id]

        if (entry == null) {
            // Ticket nie istnieje w mapie — zaplanuj pełny fetch z items
            Timber.tag(TAG).w(
                "⚠️ updateLocalTicket: ticket ${ticket.id} not in map — scheduling full fetch"
            )
            viewModelScope.launch {
                when (val res = kdsRepository.getTicketWithItems(ticket.id)) {
                    is Resource.Success -> {
                        val fetchedItems = res.value.items
                        _ticketsMap.update { cur ->
                            val existing = cur[ticket.id]
                            val items = when {
                                fetchedItems.isNotEmpty()                          -> fetchedItems
                                existing != null && existing.items.isNotEmpty()   -> existing.items
                                else                                               -> emptyList()
                            }
                            cur.toMutableMap().apply {
                                put(ticket.id, KdsTicketEntry(res.value.ticket, items))
                            }
                        }
                        rebuildSlotMap()
                        Timber.tag(TAG).i(
                            "✅ Fetched missing ticket ${ticket.id} with ${fetchedItems.size} items"
                        )
                    }
                    else -> Timber.tag(TAG).w("⚠️ Full fetch failed for missing ticket ${ticket.id}")
                }
            }
            return
        }

        // Ticket istnieje — aktualizuj TYLKO dane ticketu, NIE items
        _ticketsMap.update { cur ->
            val e = cur[ticket.id] ?: return@update cur
            cur.toMutableMap().apply {
                put(ticket.id, e.copy(ticket = ticket))
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
    /** displayNumber = kdsTicketNumber jeśli dostępny, inaczej orderNumber */
    data class NewTicket(val displayNumber: String) : KdsEvent
    data class Error(val message: String) : KdsEvent
    data class Success(val message: String) : KdsEvent
    data class ScheduledSoon(val displayNumber: String, val minutesLeft: Int) : KdsEvent
    data class PrintTicket(val ticketId: String) : KdsEvent
}
