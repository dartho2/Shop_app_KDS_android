//package com.itsorderkds.ui.order
//
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.Build
//import android.util.Log
//import android.widget.Toast
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.google.gson.Gson
//import com.google.maps.android.PolyUtil
//import com.itsorderkds.data.entity.mapper.OrderMapper
//import com.itsorderkds.data.model.BatchUpdateStatusRequest
//import com.itsorderkds.data.model.DispatchCancelCourier
//import com.itsorderkds.data.model.DispatchCourier
//import com.itsorderkds.data.model.GeoPoint
//import com.itsorderkds.data.model.Order
//import com.itsorderkds.data.model.OrderTras
//import com.itsorderkds.data.model.OrderWrapper
//import com.itsorderkds.data.model.ShiftCheckIn
//import com.itsorderkds.data.model.ShiftCheckOut
//import com.itsorderkds.data.model.ShiftResponse
//import com.itsorderkds.data.model.UpdateCourierOrder
//import com.itsorderkds.data.model.UpdateOrderData
//import com.itsorderkds.data.model.Vehicle
//import com.itsorderkds.data.network.Resource
//import com.itsorderkds.data.network.preferences.TokenProvider
//import com.itsorderkds.data.repository.OrdersRepository
//import com.itsorderkds.data.responses.UserRole
//import com.itsorderkds.data.util.AppStateManager
//import com.itsorderkds.data.util.LocationException
//import com.itsorderkds.data.util.LocationProvider
//import com.itsorderkds.service.OrderAlarmService
//import com.itsorderkds.service.SocketEventsRepository
//import com.itsorderkds.data.repository.SettingsRepository
//import com.itsorderkds.ui.settings.print.PrinterService
//import com.itsorderkds.ui.theme.home.AppDestinations
//import com.itsorderkds.util.AppPrefs
//import dagger.hilt.android.lifecycle.HiltViewModel
//import dagger.hilt.android.qualifiers.ApplicationContext
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.withTimeoutOrNull
//import kotlinx.coroutines.flow.MutableSharedFlow
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asSharedFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.collect
//import kotlinx.coroutines.flow.combine
//import kotlinx.coroutines.flow.debounce
//import kotlinx.coroutines.flow.distinctUntilChanged
//import kotlinx.coroutines.flow.emptyFlow
//import kotlinx.coroutines.flow.flatMapLatest
//import kotlinx.coroutines.flow.launchIn
//import kotlinx.coroutines.flow.map
//import kotlinx.coroutines.flow.onEach
//import kotlinx.coroutines.flow.stateIn
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.sync.Mutex
//import kotlinx.coroutines.sync.withLock
//import kotlinx.coroutines.withContext
//import timber.log.Timber
//import java.time.Instant
//import java.time.LocalDate
//import java.time.ZoneId
//import java.time.ZonedDateTime
//import javax.inject.Inject
//
//// ... (Data classes OrdersUiState, OrderRouteState, OrderEvent bez zmian) ...
//data class OrdersUiState(
//    val isInitializing: Boolean = true,
//    val routeState: OrderRouteState = OrderRouteState.Loading,
//    val shiftStatus: ShiftResponse? = null,
//    val isShiftActive: Boolean = false,
//    val isShiftLoading: Boolean = false,
//    val socketConnected: Boolean = false,
//    val isShiftStatusKnown: Boolean = false,
//    val isGlobalLoading: Boolean = false,
//    val showAssignmentPrompt: Boolean = true,
//    val orderToShowInDialog: Order? = null,
//    val userRole: UserRole? = null,
//    val userId: String? = null,
//    val routeError: String? = null,
//    val isFetchingVehicles: Boolean = false,
//    val isPrinting: Boolean = false,  // Flaga blokująca przycisk drukarki podczas drukowania
//    val showPrinterSelectionDialog: Boolean = false, // Flaga do wyświetlania dialogu wyboru drukarki
//    val selectedOrderForPrinting: Order? = null // Zamówienie wybrane do drukowania
//)
//data class Callbacks(
//    val onTimeSelected: ((String) -> Unit)? = null,
//    val onStatusChange: ((OrderStatusEnum) -> Unit)? = null,
//    val onCourierChange: ((Boolean) -> Unit)? = null,
//    val onPrintOrder: ((Order) -> Unit)? = null,
//    val onSendExternalCourier: ((Order, DeliveryEnum, Int, String?) -> Unit)? = null,
//    val onCancelExternalCourier: ((Order) -> Unit)? = null,
//    val isPrinting: Boolean = false  // Flaga blokująca przycisk drukarki
//)
//sealed interface OrderRouteState {
//    object Loading : OrderRouteState
//    data class Success(val route: List<OrderTras>) : OrderRouteState
//    data class Error(val code: Int?, val body: Any?) : OrderRouteState
//}
//
//sealed interface OrderEvent {
//    object Loading : OrderEvent
//    data class Success(val message: String) : OrderEvent
//    data class Error(val code: Int?, val body: Any?) : OrderEvent
//    object RequestLocationPermission : OrderEvent // Nowy event dla żądania uprawnień GPS
//}
//
//@HiltViewModel
//class OrdersViewModel @Inject constructor(
//    @ApplicationContext private val context: Context,
//    private val repository: OrdersRepository,
//    private val socketEventsRepo: SocketEventsRepository,
//    private val tokenProvider: TokenProvider,
//    private val printerService: PrinterService,
//    private val locationProvider: LocationProvider,
//    private val settingsRepository: SettingsRepository,
//    private val appPreferencesManager: com.itsorderkds.data.preferences.AppPreferencesManager,
//    private val savedStateHandle: androidx.lifecycle.SavedStateHandle
//) : ViewModel() {
//    private var lastRangOrderId: String? = null
//    private var lastAlarmStartOrderId: String? = null  // ✅ FIX: Debounce start alarmu
//    private var lastAlarmStartAtMs: Long = 0L          // ✅ FIX: Timestamp debounce
//    // ... (Wszystkie prywatne _StateFlows bez zmian) ...
//    private var statusAutoRefreshJob: Job? = null
//    private val gson = Gson()
//    private val _navigationEvent = MutableSharedFlow<String>()
//    val navigationEvent = _navigationEvent.asSharedFlow()
//
//    //    private val _restaurantStatus = MutableStateFlow<RestaurantStatusUi?>(null)
////    val restaurantStatus: StateFlow<RestaurantStatusUi?> = _restaurantStatus.asStateFlow()
//    private val _lockedOrderIds = MutableStateFlow<Set<String>>(emptySet())
//    val lockedOrderIds: StateFlow<Set<String>> = _lockedOrderIds.asStateFlow()
//    private val lockJobs = mutableMapOf<String, Job>()
//
//    // Zamówienia zaakceptowane manualnie (żeby pominąć duplikację drukowania przez socket)
//    private val manuallyAcceptedOrders = mutableSetOf<String>()
//
//    // Zamówienia które już zostały wydrukowane (żeby nie drukować przy restarcie app)
//    private val printedOrderIds = mutableSetOf<String>()
//
//    // Mutex dla thread-safe dostępu do printedOrderIds
//    private val printedOrdersMutex = Mutex()
//
//    // Flaga - czy printedOrderIds zostały załadowane z DataStore (chroni przed drukowaniem przy starcie app)
//    @Volatile
//    private var printedOrdersLoaded = false
//
//    // ✅ DEDUPLIKACJA: Okno czasowe dla eventów drukowania (zapobiega duplikatom z backendu)
//    private val recentPrintEvents = mutableMapOf<String, Long>() // orderId -> timestamp
//    private val printEventWindowMs = 3000L // 3 sekundy - blokuje duplikaty w tym oknie
//    private val recentPrintMutex = Mutex()
//
//    private val _selectedOrderIds = MutableStateFlow<Set<String>>(emptySet())
//    val selectedOrderIds: StateFlow<Set<String>> = _selectedOrderIds.asStateFlow()
//    private val _currentBatchStatus = MutableStateFlow<OrderStatusEnum?>(null)
//    val currentBatchStatus: StateFlow<OrderStatusEnum?> = _currentBatchStatus.asStateFlow()
//    private val _routeClusters = MutableStateFlow<List<RouteCluster>>(emptyList())
//    val routeClusters: StateFlow<List<RouteCluster>> = _routeClusters.asStateFlow()
//    private val _selectedClusterIndex = MutableStateFlow<Int?>(null)
//    val selectedClusterIndex: StateFlow<Int?> = _selectedClusterIndex.asStateFlow()
//    fun selectCluster(index: Int?) {
//        _selectedClusterIndex.value = index
//    }
//
//    val visibleRouteClusters: StateFlow<List<RouteCluster>> =
//        combine(routeClusters, selectedClusterIndex) { clusters, selectedIndex ->
//            if (selectedIndex == null) clusters else clusters.getOrNull(selectedIndex)
//                ?.let { listOf(it) } ?: emptyList()
//        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
//    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
//    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()
//    private val _uiState = MutableStateFlow(OrdersUiState())
//    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()
//    private val _event = MutableSharedFlow<OrderEvent>()
//    val event = _event.asSharedFlow()
//    private val _isHomeReady = MutableStateFlow(false)
//    val isHomeReady: StateFlow<Boolean> = _isHomeReady.asStateFlow()
//
//
//    // --- PRZEPŁYWY DANYCH (Filtrowanie po dacie) ---
//
//    private val allOrdersFromDb: StateFlow<List<Order>> = repository.getAllOrdersFlow()
//        .map { entityList ->
//            Timber.d("📂 Baza danych (getAllOrdersFlow) wyemitowała ${entityList.size} encji.")
//            entityList.map { entity ->
//                val order = OrderMapper.toOrder(entity)
//                if (order.externalDelivery != null) {
//                    Timber.d("🚚 Order ${order.orderId} has externalDelivery: status=${order.externalDelivery?.status}, pickupEta=${order.externalDelivery?.pickupEta}, dropoffEta=${order.externalDelivery?.dropoffEta}")
//                }
//                order
//            }
//        }
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
//
//    val orders: StateFlow<List<Order>> = allOrdersFromDb
//        .map { all ->
//            val startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
//            val now = Instant.now()
//            Timber.d("VM: Filtruję listę ${all.size} zamówień. Pokazuję tylko nowsze niż $startOfToday (lub aktywne/zaplanowane).")
//
//            all.filter { order ->
//                try {
//                    // Warunek 1: Aktywne statusy
//                    val slugEnum = order.orderStatus.slug?.let {
//                        runCatching { OrderStatusEnum.valueOf(it) }.getOrNull()
//                    }
//                    if (slugEnum == OrderStatusEnum.PROCESSING || slugEnum == OrderStatusEnum.PENDING) {
//                        return@filter true
//                    }
//
//                    // Warunek 2: Zamówienia zaplanowane na dziś lub w przyszłości
//                    val deliveryIntervalInstant = order.deliveryInterval?.let {
//                        runCatching { ZonedDateTime.parse(it).toInstant() }.getOrNull()
//                    }
//                    if (deliveryIntervalInstant != null && !deliveryIntervalInstant.isBefore(startOfToday)) {
//                        return@filter true
//                    }
//
//                    // Warunek 3: Zamówienia utworzone dzisiaj
//                    val orderInstant = order.createdAt?.let {
//                        runCatching { ZonedDateTime.parse(it).toInstant() }.getOrNull()
//                    }
//                    if (orderInstant != null && !orderInstant.isBefore(startOfToday)) {
//                        return@filter true
//                    }
//
//                    // Nie pokazuj jeśli nie spełnia żadnego warunku
//                    false
//
//                } catch (e: Exception) {
//                    Timber.e(e, "Nie można sparsować daty dla zamówienia ${order.orderId}")
//                    false
//                }
//            }
//        }
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
//
//    // --- Listy dla zakładek ---
//
//    val activeOrdersList: StateFlow<List<Order>> = orders
//        .map { all ->
//            val activeStatuses = setOf(
//                OrderStatusEnum.PROCESSING,
//                OrderStatusEnum.ACCEPTED,
//                OrderStatusEnum.OUT_FOR_DELIVERY
//            )
//            all
//                .filter {
//                    val slugEnum = it.orderStatus.slug?.let { slug ->
//                        runCatching { OrderStatusEnum.valueOf(slug) }.getOrNull()
//                    }
//                    slugEnum in activeStatuses
//                }
//                .sortedByDescending { it.createdAt }
//        }
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
//
//    val completedOrdersList: StateFlow<List<Order>> = orders
//        .map { all ->
//            all
//                .filter {
//                    val slugEnum = it.orderStatus.slug?.let { slug ->
//                        runCatching { OrderStatusEnum.valueOf(slug) }.getOrNull()
//                    }
//                    slugEnum == OrderStatusEnum.COMPLETED
//                }
//                .sortedByDescending { it.createdAt }
//        }
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
//
//    val dineInOrdersList: StateFlow<List<Order>> = orders
//        .map { all ->
//            all
//                .filter {
//                    it.deliveryType == OrderDelivery.DINE_IN ||
//                    it.deliveryType == OrderDelivery.ROOM_SERVICE
//                }
//                .sortedByDescending { it.createdAt }
//        }
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
//
//    val groupedOrdersMap: StateFlow<Map<SourceEnum?, List<Order>>> = orders
//        .map { all ->
//            all
//                .sortedByDescending { it.createdAt }
//                .groupBy { it.source?.name }
//        }
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
//
//    val pendingOrdersQueue: StateFlow<List<Order>> =
//        orders
//            .map { allOrders ->
//                Timber.tag("QUEUE_FILTER").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
//                Timber.tag("QUEUE_FILTER").d("📋 QUEUE UPDATE - Total orders: ${allOrders.size}")
//
//                allOrders.filter {
//                    val slugEnum = it.orderStatus?.slug?.let { slug ->
//                        runCatching { OrderStatusEnum.valueOf(slug) }.getOrNull()
//                    }
//                    val isProcessing = slugEnum == OrderStatusEnum.PROCESSING
//
//                    Timber.tag("QUEUE_FILTER").d("   Order ${it.orderNumber}: status=${it.orderStatus?.slug}, orderId=${it.orderId}, inQueue=$isProcessing")
//                    isProcessing
//                }
//                    .sortedBy { it.createdAt }
//                    .also { filtered ->
//                        Timber.tag("QUEUE_FILTER").d("   └─ Filtered PROCESSING: ${filtered.size}")
//                        if (filtered.isNotEmpty()) {
//                            Timber.tag("QUEUE_FILTER").d("   📝 Orders in queue:")
//                            filtered.forEach { order ->
//                                Timber.tag("QUEUE_FILTER").d("      - ${order.orderNumber} (${order.orderId})")
//                            }
//                        }
//                    }
//            }
//            .distinctUntilChanged { old, new ->
//                // ✅ POPRAWKA: Porównaj zawartość kolejki (orderIds), nie same obiekty
//                old.map { it.orderId } == new.map { it.orderId }
//            }
//            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
//
//    private val _suppressAutoOpen = MutableStateFlow(false)
//    private var autoOpenGuardJob: Job? = null
//    private var lastClosedOrderId: String? = null
//
//    init {
//        // ✅ POPRAWKA #1: Przywracanie stanu dialogu z SavedStateHandle
//        restoreDialogState()
//
//        initUserSession()
//        loadPrintedOrderIds() // Załaduj wydrukowane zamówienia z DataStore
//        observeSocketEvents()
//        observePendingOrdersQueue()
//        monitorHomeReadiness()
//        observeSocketConnection()
//        observeCurrentlyOpenDialogStatus()
//    }
//
//    /**
//     * ✅ POPRAWKA #1: Przywracanie stanu dialogu po restarcie aplikacji
//     * Odczytuje currentDialogId z SavedStateHandle i sprawdza czy zamówienie nadal wymaga reakcji
//     */
//    private fun restoreDialogState() {
//        val restoredDialogId = savedStateHandle.get<String?>("currentDialogId")
//
//        Timber.tag("DIALOG_PERSISTENCE").d("═══════════════════════════════════════")
//        Timber.tag("DIALOG_PERSISTENCE").d("🔄 ViewModel INIT - Restore state")
//        Timber.tag("DIALOG_PERSISTENCE").d("   ├─ restoredDialogId: $restoredDialogId")
//        Timber.tag("DIALOG_PERSISTENCE").d("   ├─ Thread: ${Thread.currentThread().name}")
//        Timber.tag("DIALOG_PERSISTENCE").d("   └─ Timestamp: ${System.currentTimeMillis()}")
//
//        if (restoredDialogId != null) {
//            viewModelScope.launch {
//                try {
//                    // Poczekaj na załadowanie zamówień z DB (max 5 sekund)
//                    val ordersLoaded = withTimeoutOrNull(5000) {
//                        orders.first { it.isNotEmpty() }
//                    }
//
//                    if (ordersLoaded != null) {
//                        val order = orders.value.find { it.orderId == restoredDialogId }
//
//                        Timber.tag("DIALOG_PERSISTENCE").d("🔍 Znaleziono zamówienie: ${order?.orderNumber}")
//                        Timber.tag("DIALOG_PERSISTENCE").d("   ├─ status: ${order?.orderStatus?.slug}")
//
//                        if (order != null) {
//                            val slugEnum = order.orderStatus?.slug?.let { slug ->
//                                runCatching { OrderStatusEnum.valueOf(slug) }.getOrNull()
//                            }
//
//                            if (slugEnum == OrderStatusEnum.PROCESSING) {
//                                // Zamówienie nadal PROCESSING → przywróć dialog
//                                _uiState.update { it.copy(orderToShowInDialog = order) }
//                                Timber.tag("DIALOG_PERSISTENCE").d("✅ Przywrócono dialog dla: ${order.orderNumber}")
//                            } else {
//                                // Zamówienie już nie PROCESSING → wyczyść
//                                savedStateHandle["currentDialogId"] = null
//                                Timber.tag("DIALOG_PERSISTENCE").d("🗑️ Zamówienie nie jest już PROCESSING - wyczyszczono SavedStateHandle")
//                            }
//                        } else {
//                            // Zamówienie nie znalezione → wyczyść
//                            savedStateHandle["currentDialogId"] = null
//                            Timber.tag("DIALOG_PERSISTENCE").d("⚠️ Zamówienie nie znalezione w DB - wyczyszczono SavedStateHandle")
//                        }
//                    } else {
//                        Timber.tag("DIALOG_PERSISTENCE").w("⏱️ Timeout - zamówienia nie załadowały się w 5s")
//                    }
//                } catch (e: Exception) {
//                    Timber.tag("DIALOG_PERSISTENCE").e(e, "❌ Błąd przywracania dialogu")
//                    savedStateHandle["currentDialogId"] = null
//                }
//            }
//        }
//    }
//
//    /**
//     * Ładuje listę wydrukowanych zamówień z DataStore przy starcie
//     */
//    private fun loadPrintedOrderIds() {
//        viewModelScope.launch {
//            val stored = appPreferencesManager.getPrintedOrderIds()
//            synchronized(printedOrderIds) {
//                printedOrderIds.addAll(stored)
//            }
//            printedOrdersLoaded = true
//            Timber.d("📂 Załadowano ${stored.size} wydrukowanych zamówień z DataStore")
//        }
//    }
//
//    /**
//     * Dodaje orderId do listy wydrukowanych (w pamięci + DataStore)
//     * UWAGA: Suspend function - używa Mutex dla thread-safety
//     */
//    private suspend fun markAsPrinted(orderId: String) {
//        printedOrdersMutex.withLock {
//            printedOrderIds.add(orderId)
//        }
//
//        // Zapisz asynchronicznie do DataStore (poza mutexem)
//        viewModelScope.launch {
//            try {
//                appPreferencesManager.addPrintedOrderId(orderId)
//                Timber.d("💾 Zapisano wydrukowane zamówienie do DataStore: $orderId")
//            } catch (e: Exception) {
//                Timber.e(e, "❌ Błąd zapisu do DataStore: $orderId")
//            }
//        }
//    }
//
//    /**
//     * SYNCHRONICZNA wersja markAsPrinted - używana PRZED viewModelScope.launch
//     * Zapobiega race condition gdy dwa handlery startują równolegle
//     * Używa synchronized zamiast mutex (nie wymaga suspend)
//     */
//    private fun markAsPrintedSync(orderId: String) {
//        // synchronized jest OK tutaj - szybka operacja (dodanie do Set)
//        synchronized(printedOrderIds) {
//            printedOrderIds.add(orderId)
//        }
//
//        // Zapisz asynchronicznie do DataStore
//        viewModelScope.launch {
//            try {
//                appPreferencesManager.addPrintedOrderId(orderId)
//                Timber.d("💾 Zapisano wydrukowane zamówienie do DataStore: $orderId")
//            } catch (e: Exception) {
//                Timber.e(e, "❌ Błąd zapisu do DataStore: $orderId")
//            }
//        }
//    }
//
//    /**
//     * Sprawdza czy zamówienie było już wydrukowane (thread-safe)
//     */
//    private suspend fun wasPrinted(orderId: String): Boolean {
//        return printedOrdersMutex.withLock {
//            printedOrderIds.contains(orderId)
//        }
//    }
//
//    /**
//     * SYNCHRONICZNA wersja wasPrinted - używana PRZED viewModelScope.launch
//     * Zapobiega race condition gdy dwa handlery startują równolegle
//     * Używa synchronized zamiast mutex (nie wymaga suspend)
//     */
//    private fun wasPrintedSync(orderId: String): Boolean {
//        // ✅ CRITICAL: Jeśli dane jeszcze nie załadowane z DataStore, uznaj że BYŁO wydrukowane
//        // Zapobiega drukowaniu starych zamówień przy starcie aplikacji
//        if (!printedOrdersLoaded) {
//            Timber.tag("PRINT_GUARD").d("⏳ PrintedOrderIds nie załadowane jeszcze - pomijam drukowanie: $orderId")
//            return true  // Blokuj drukowanie dopóki dane nie zostaną załadowane!
//        }
//
//        // synchronized jest OK tutaj - szybka operacja (sprawdzenie Set)
//        return synchronized(printedOrderIds) {
//            printedOrderIds.contains(orderId)
//        }
//    }
//
//    /**
//     * ✅ DEDUPLIKACJA: Sprawdza czy event drukowania nie jest duplikatem w oknie czasowym
//     * Blokuje wielokrotne drukowanie tego samego zamówienia w okresie printEventWindowMs
//     * Używa synchronized zamiast suspend mutex dla natychmiastowej reakcji
//     */
//    private fun shouldAllowPrintEvent(orderId: String): Boolean {
//        val now = System.currentTimeMillis()
//
//        synchronized(recentPrintEvents) {
//            val lastPrintTime = recentPrintEvents[orderId]
//
//            return if (lastPrintTime == null || (now - lastPrintTime) > printEventWindowMs) {
//                // Dozwolone - zapisz timestamp
//                recentPrintEvents[orderId] = now
//
//                // Cleanup starych wpisów (starsze niż 10s)
//                val cleanupThreshold = now - 10000L
//                recentPrintEvents.entries.removeIf { it.value < cleanupThreshold }
//
//                Timber.tag("DEDUPLICATION").d("✅ Dozwolony event drukowania: $orderId")
//                true
//            } else {
//                val elapsedMs = now - lastPrintTime
//                Timber.tag("DEDUPLICATION").w("⏭️ Zablokowany duplikat! orderId=$orderId, elapsed=${elapsedMs}ms (okno=${printEventWindowMs}ms)")
//                false
//            }
//        }
//    }
//
//    private fun observeCurrentlyOpenDialogStatus() {
//        viewModelScope.launch {
//            combine(
//                orders,
//                _uiState.map { it.orderToShowInDialog }.distinctUntilChanged()
//            ) { allOrders, currentDialogOrder ->
//                if (currentDialogOrder != null) {
//                    val updatedOrderInList =
//                        allOrders.find { it.orderId == currentDialogOrder.orderId }
//                    if (updatedOrderInList != null) {
//                        val slugEnum = updatedOrderInList.orderStatus.slug?.let {
//                            runCatching { OrderStatusEnum.valueOf(it) }.getOrNull()
//                        }
//                        val isStillProcessing = slugEnum == OrderStatusEnum.PROCESSING
//                        if (isStillProcessing) {
//                            _uiState.update { it.copy(orderToShowInDialog = updatedOrderInList) }
//                        } else {
//                            Timber.d("observeCurrentlyOpenDialogStatus: Zamówienie ${currentDialogOrder.orderId} nie jest już PROCESSING (nowy status: ${updatedOrderInList.orderStatus.slug}). AKTUALIZUJĘ dialog i ZATRZYMUJĘ alarm.")
//                            _uiState.update { it.copy(orderToShowInDialog = updatedOrderInList) }
//                            stopAlarmService()
//                        }
//                    } else {
//                        Timber.d("observeCurrentlyOpenDialogStatus: Zamówienie ${currentDialogOrder.orderId} nie istnieje już na liście. Zamykam dialog.")
//                        withContext(Dispatchers.Main) {
//                            dismissDialog()
//                        }
//                    }
//                }
//            }.collect()
//        }
//    }
//
//    /**
//     * ✅ POPRAWIONA FUNKCJA:
//     * Ta funkcja jest teraz odpowiedzialna TYLKO za synchronizację danych po
//     * ponownym połączeniu. Nie uruchamia już alarmu.
//     */
//    private fun observeSocketConnection() {
//        viewModelScope.launch {
//            // ✅ UŻYWAMY 'combine', aby reagować na ZMIANY ROLI lub ZMIANY POŁĄCZENIA
//            combine(
//                socketEventsRepo.connection,
//                _uiState.map { it.userRole }.distinctUntilChanged()
//            ) { isConnected, role ->
//                Pair(isConnected, role)
//            }
//                .distinctUntilChanged() // Reaguj tylko jeśli para (isConnected, role) faktycznie się zmieni
//                .collect { (isConnected, role) ->
//
//                    // ✅ Sprawdzamy oba warunki jednocześnie
//                    if (isConnected && role != UserRole.COURIER) {
//                        Timber.tag(TAG)
//                            .d("[emit] Socket POŁĄCZONY i rola ($role) to nie kurier. Rozpoczynam synchronizację.")
//                        // Uruchom synchronizację
//                        syncOrdersFromApiStartOfDay()
//                    } else if (!isConnected) {
//                        Timber.tag(TAG).d("[emit] Socket ROZŁĄCZONY.")
//                    } else if (role == UserRole.COURIER) {
//                        Timber.tag(TAG)
//                            .d("[emit] Socket POŁĄCZONY, ale rola to COURIER. Nie synchronizuję zamówień.")
//                    }
//                }
//        }
//    }
//
//    fun sendToExternalApi(order: Order, courierDelivery: DeliveryEnum, timePrepare: Int, timeDelivery: String? = null) = viewModelScope.launch {
//        Timber.d("Wysyłanie zamówienia ${order.orderId} do kuriera $courierDelivery z czasem przygotowania: $timePrepare min i czasem dostawy: $timeDelivery")
//        val body = DispatchCourier(courier = courierDelivery, options = null, timePrepare = timePrepare, timeDelivery = timeDelivery)
//        when (repository.sendToExternalCourier(order.orderId, body)) {
//            is Resource.Success -> {
//                Timber.i("Dispatch queued")
//            }
//
//            is Resource.Failure -> {}
//            Resource.Loading -> {}
//        }
//    }
//
//    fun cancelExternalCourier(order: Order) = viewModelScope.launch {
//        Timber.tag("CANCEL_COURRIER").d("Anulowanie dostawy zewnętrznej dla zamówienia ${order.orderId}")
//        val body = DispatchCancelCourier(courier = order.externalDelivery?.courier, reason = "Cancelled by staff")
//        when (val result = repository.cancelExternalCourier(order.orderId, body)) {
//            is Resource.Success -> {
//                Timber.tag("CANCEL_COURRIER").i("✅ Dispatch cancelled successfully for order ${order.orderId} ${order.externalDelivery?.courier}")
//                // Pobierz zaktualizowane zamówienie
//                when (val updatedOrder = repository.fetchOrderByIdFromApi(order.orderId)) {
//                    is Resource.Success -> {
//                        repository.insertOrUpdateOrder(OrderMapper.fromOrder(updatedOrder.value))
//                        Timber.tag("CANCEL_COURRIER").d("📂 Updated order saved to database")
//                    }
//                    is Resource.Failure -> {
//                        Timber.tag("CANCEL_COURRIER").w("Failed to fetch updated order: for order ${order.orderId} ${order.externalDelivery?.courier}")
//                    }
//                    Resource.Loading -> {}
//                }
//            }
//            is Resource.Failure -> {
//                Timber.tag("CANCEL_COURRIER").e("❌ Failed to cancel external courier: ${result.errorMessage} for order ${order.orderId} ${order.externalDelivery?.courier}")
//            }
//            Resource.Loading -> {
//                Timber.tag("CANCEL_COURRIER").d("Cancelling external courier...")
//            }
//        }
//    }
//
//    fun handleNewOrderFromIntent(orderJson: String) {
//        Timber.d("ViewModel przetwarza orderJson z Intentu: $orderJson")
//
//        // ⛔️ USUWAMY TĘ LOGIKĘ.
//        // Otwieranie dialogu i startowanie alarmu powinno dziać się
//        // TYLKO w 'observePendingOrdersQueue', aby uniknąć konfliktów.
//        // 'handleNewOrderFromIntent' jest wywoływany, gdy aplikacja jest w tle,
//        // aby ją wybudzić. 'observePendingOrdersQueue' przejmie pałeczkę,
//        // gdy baza danych się zaktualizuje.
//        /*
//        try {
//            val orderWrapper = gson.fromJson(orderJson, OrderWrapper::class.java)
//            _uiState.update { currentState ->
//                currentState.copy(orderToShowInDialog = orderWrapper.order)
//            }
//        } catch (e: Exception) {
//            Timber.e(e, "Błąd parsowania orderJson w ViewModelu")
//        }
//        */
//
//        // Zostawiamy tylko nawigację do 'home'
//        viewModelScope.launch {
//            _navigationEvent.emit(AppDestinations.HOME)
//        }
//    }
//
////    fun fetchGeneralSettings() = viewModelScope.launch {
////        val role = uiState.value.userRole
////        Timber.d("Pobieram ustawienia dla roli: $role")
////        when (role) {
////            UserRole.COURIER -> {
////                Timber.d("Pobieram ustawienia dla Kuriera...")
////                when (val response = settingsRepository.getCourierSettings()) {
////                    is Resource.Success -> {
////                        Timber.d("Pobrano ustawienia kuriera: ${response.value}")
////                    }
////
////                    is Resource.Failure -> {
////                        _event.emit(OrderEvent.Error(null, "Nie udało się pobrać ustawień kuriera"))
////                    }
////
////                    else -> { /* Loading... */
////                    }
////                }
////            }
////            UserRole.ADMIN -> {
////                Timber.d("Pobieram ustawienia dla Admin...")
////                when (val response = settingsRepository.getSettings()) {
////                    is Resource.Success -> {
////                        val settings = response.value
////                        Log.e("fetchGeneralSettings", "Pobrano ustawienia: $response")
////
////                        // Waluta
////                        val currency = settings.values.general.default_currency?.symbol
////                        if (currency != null) {
////                            AppPrefs.setCurrency(currency)
////                        } else {
////                            Log.w(
////                                "fetchGeneralSettings",
////                                "Serwer zwrócił 'null' jako walutę. Ustawiam domyślny PLN."
////                            )
////                            AppPrefs.setCurrency("PLN")
////                        }
////
////                        // Integracje kurierskie
////                        settings.values.integrations.stava?.let { stava ->
////                            AppPrefs.setStavaIntegrationActive(stava.isActive ?: false)
////                            Timber.d("Stava integration: ${stava.isActive}")
////                        }
////                        settings.values.integrations.woltDrive?.let { woltDrive ->
////                            AppPrefs.setWoltDriveIntegrationActive(woltDrive.isActive ?: false)
////                            Timber.d("WoltDrive integration: ${woltDrive.isActive}")
////                        }
////                        settings.values.integrations.stuart?.let { stuart ->
////                            AppPrefs.setStuartIntegrationActive(stuart.isActive ?: false)
////                            Timber.d("Stuart integration: ${stuart.isActive}")
////                        }
////                    }
////
////                    is Resource.Failure -> {
////                        _event.emit(OrderEvent.Error(null, "Nie udało się pobrać ustawień"))
////                    }
////
////                    else -> {
////                        Log.w(
////                            "fetchGeneralSettings",
////                            "Serwer zwrócił 'null' jako walutę. Ustawiam domyślny PLN."
////                        )
////                    }
////                }
////            }
////            UserRole.STAFF -> {
////                Timber.d("Pobieram ustawienia dla Staff...")
////                when (val response = settingsRepository.getSettings()) {
////                    is Resource.Success -> {
////                        val settings = response.value
////                        Log.e("fetchGeneralSettings", "Pobrano ustawienia: $response")
////
////                        // Waluta
////                        val currency = settings.values.general.default_currency?.symbol
////                        if (currency != null) {
////                            AppPrefs.setCurrency(currency)
////                        } else {
////                            Log.w(
////                                "fetchGeneralSettings",
////                                "Serwer zwrócił 'null' jako walutę. Ustawiam domyślny PLN."
////                            )
////                            AppPrefs.setCurrency("PLN")
////                        }
////
////                        // Integracje kurierskie
////                        settings.values.integrations.stava?.let { stava ->
////                            AppPrefs.setStavaIntegrationActive(stava.isActive ?: false)
////                            Timber.d("Stava integration: ${stava.isActive}")
////                        }
////                        settings.values.integrations.woltDrive?.let { woltDrive ->
////                            AppPrefs.setWoltDriveIntegrationActive(woltDrive.isActive ?: false)
////                            Timber.d("WoltDrive integration: ${woltDrive.isActive}")
////                        }
////                        settings.values.integrations.stuart?.let { stuart ->
////                            AppPrefs.setStuartIntegrationActive(stuart.isActive ?: false)
////                            Timber.d("Stuart integration: ${stuart.isActive}")
////                        }
////                    }
////
////                    is Resource.Failure -> {
////                        _event.emit(OrderEvent.Error(null, "Nie udało się pobrać ustawień"))
////                    }
////
////                    else -> {
////                        Log.w(
////                            "fetchGeneralSettings",
////                            "Serwer zwrócił 'null' jako walutę. Ustawiam domyślny PLN."
////                        )
////                    }
////                }
////            }
////
////            else -> {
////                Timber.w("fetchGeneralSettings: Nie rozpoznano roli '$role' lub jest null.")
////            }
////        }
////    }
//
//
//    override fun onCleared() {
//        statusAutoRefreshJob?.cancel()
//        lockJobs.values.forEach { it.cancel() }
//        lockJobs.clear()
//        super.onCleared()
//    }
//
//
//
//
//
//
//
//
//    fun updateSelectedOrders(newSelection: Set<String>, newStatus: OrderStatusEnum?) {
//        val filtered = newSelection - _lockedOrderIds.value
//        _selectedOrderIds.value = filtered
//        _currentBatchStatus.value = if (filtered.isEmpty()) null else newStatus
//    }
//
//
//    fun updateOrderCourier(orderId: String, body: UpdateCourierOrder) =
//        executeOrderUpdate { repository.assignCourier(orderId, body) }
//
//    fun updateOrder(orderId: String, status: OrderStatusEnum, data: UpdateOrderData) {
//        // ✅ FIX: Oznacz jako manualnie zaakceptowane OD RAZU (przed API request!)
//        // Chroni przed drukowaniem przez socket który przychodzi szybciej niż API response
//        if (status == OrderStatusEnum.ACCEPTED) {
//            manuallyAcceptedOrders.add(orderId)
//            Timber.tag("AUTO_PRINT").d("🔐 Dodano ${orderId} do manuallyAcceptedOrders (przed API request)")
//
//            // Usuń z setu po 30 sekundach
//            viewModelScope.launch {
//                delay(30_000)
//                manuallyAcceptedOrders.remove(orderId)
//                Timber.tag("AUTO_PRINT").d("🗑️ Usunięto ${orderId} z manuallyAcceptedOrders (po 30s)")
//            }
//        }
//
//        executeOrderUpdate { repository.updateOrder(orderId, status, data) }
//    }
//
//    fun syncOrdersFromApiStartOfDay() {
//        val zoneId = ZoneId.systemDefault()
//        val todayLocalDate = LocalDate.now(zoneId)
//        val startOfToday = todayLocalDate.atStartOfDay(zoneId)
//        val startOfTodayIso = startOfToday.toInstant().toString()
//        val datePrefix = todayLocalDate.toString()
//        Timber.d("syncOrdersFromApiStartOfDay: Dziś jest $todayLocalDate. Wysyłam do API datę UTC: $startOfTodayIso, a do DAO prefix: $datePrefix")
//        syncOrdersFromApi(startOfTodayIso)
//    }
//
//    fun printOrder(order: Order) {
//        // Zabezpieczenie przed wielokrotnym kliknięciem
//        if (_uiState.value.isPrinting) {
//            Timber.d("🚫 Drukowanie już trwa, ignoruję kolejne kliknięcie")
//            return
//        }
//
//        // ✅ NOWE: Sprawdź liczbę włączonych drukarek
//        viewModelScope.launch {
//            val printers = withContext(Dispatchers.IO) {
//                com.itsorderkds.data.preferences.PrinterPreferences.getPrinters(context)
//            }
//            val enabledPrinters = printers.filter { it.enabled && it.deviceId.isNotBlank() }
//
//            when {
//                enabledPrinters.isEmpty() -> {
//                    // Brak drukarek - drukuj normalnie (pokaże toast o braku drukarki)
//                    Timber.d("🖨️ Brak skonfigurowanych drukarek - drukuję standardowo")
//                    printOrderDirectly(order)
//                }
//                enabledPrinters.size == 1 -> {
//                    // Tylko 1 drukarka - drukuj od razu bez pytania
//                    Timber.d("🖨️ Jedna drukarka - drukuję od razu na: ${enabledPrinters[0].name}")
//                    printOrderDirectly(order)
//                }
//                else -> {
//                    // Więcej niż 1 drukarka - pokaż dialog wyboru
//                    Timber.d("🖨️ ${enabledPrinters.size} drukarek - pokazuję dialog wyboru")
//                    _uiState.update {
//                        it.copy(
//                            showPrinterSelectionDialog = true,
//                            selectedOrderForPrinting = order
//                        )
//                    }
//                }
//            }
//        }
//    }
//
//    /**
//     * ✅ NOWE: Drukuje zamówienie bez dialogu wyboru (stara logika)
//     */
//    private fun printOrderDirectly(order: Order) {
//        viewModelScope.launch {
//            try {
//                // Ustaw flagę drukowania
//                _uiState.update { it.copy(isPrinting = true) }
//                Timber.d("🖨️ Rozpoczynam drukowanie zamówienia ${order.orderNumber}")
//
//                withContext(Dispatchers.IO) {
//                    runCatching {
//                        printerService.printOrder(order)
//                        true
//                    }.getOrElse {
//                        Timber.e("❌ Błąd drukowania: $it")
//                        false
//                    }
//                }
//
//                stopAlarmService()
//                dismissDialog()
//
//                Timber.d("✅ Drukowanie zakończone")
//            } finally {
//                // Zawsze zdejmij flagę drukowania (nawet przy błędzie)
//                _uiState.update { it.copy(isPrinting = false) }
//            }
//        }
//    }
//
//    fun clearSelection() {
//        _selectedOrderIds.value = emptySet(); _currentBatchStatus.value = null
//    }
//
//    fun dismissPrinterSelectionDialog() {
//        _uiState.update { it.copy(showPrinterSelectionDialog = false, selectedOrderForPrinting = null) }
//    }
//
//    fun printOrderOnSelectedPrinter(order: Order, printerIndex: Int) {
//        // Zabezpieczenie przed wielokrotnym kliknięciem
//        if (_uiState.value.isPrinting) {
//            Timber.d("🚫 Drukowanie już trwa, ignoruję kolejne kliknięcie")
//            return
//        }
//
//        viewModelScope.launch {
//            try {
//                // Ustaw flagę drukowania
//                _uiState.update { it.copy(isPrinting = true, showPrinterSelectionDialog = false) }
//
//                // ✅ NOWE: Obsługa printerIndex = -1 (wszystkie drukarki)
//                if (printerIndex == -1) {
//                    Timber.d("🖨️ Rozpoczynam drukowanie zamówienia ${order.orderNumber} na WSZYSTKICH drukarkach")
//
//                    withContext(Dispatchers.IO) {
//                        runCatching {
//                            printerService.printOrderOnAllPrinters(order)
//                            true
//                        }.getOrElse {
//                            Timber.e("❌ Błąd drukowania: $it")
//                            false
//                        }
//                    }
//                } else {
//                    Timber.d("🖨️ Rozpoczynam drukowanie zamówienia ${order.orderNumber} na drukarce $printerIndex")
//
//                    withContext(Dispatchers.IO) {
//                        runCatching {
//                            printerService.printOrderOnPrinter(order, printerIndex)
//                            true
//                        }.getOrElse {
//                            Timber.e("❌ Błąd drukowania: $it")
//                            false
//                        }
//                    }
//                }
//
//                stopAlarmService()
//                dismissDialog()
//
//                Timber.d("✅ Drukowanie zakończone")
//            } finally {
//                // Zawsze zdejmij flagę drukowania (nawet przy błędzie)
//                _uiState.update { it.copy(isPrinting = false) }
//            }
//        }
//    }
//
//    private fun syncOrdersFromApi(
//        startDateIso: String
//    ) = viewModelScope.launch {
//        when (val res = repository.getOrdersFromApi(startDateIso)) {
//            is Resource.Success -> {
//                Timber.tag("OrderAlarmService").d(" syncOrdersFromApi: Success")
//                repository.syncLocalDatabaseWithRemote(
//                    remoteOrders = res.value.orders.map(OrderMapper::fromOrder),
//                    syncDate = startDateIso,
//                )
//            }
//
//            is Resource.Failure -> Log.e(TAG, "Sync failed: ${res.errorCode} ${res.errorBody}")
//            is Resource.Loading -> Unit
//        }
//    }
//
//    fun triggerOpenDialog(order: Order?) {
//        Timber.tag("DIALOG_PERSISTENCE").d("💾 SAVE currentDialogId=${order?.orderId} (orderNumber: ${order?.orderNumber})")
//
//        _uiState.update { it.copy(orderToShowInDialog = order) }
//        // ✅ Persystuj ID dialogu w SavedStateHandle (survives Activity recreation)
//        savedStateHandle["currentDialogId"] = order?.orderId
//        Timber.d("🔒 Zapisano currentDialogId=${order?.orderId} w SavedStateHandle")
//    }
//
//    fun dismissDialog() {
//        Timber.tag("OrderAlarmService").d("dismissDialog()")
//
//        val closedId = _uiState.value.orderToShowInDialog?.orderId
//        lastClosedOrderId = closedId
//
//        Timber.tag("DIALOG_PERSISTENCE").d("🗑️ CLEAR currentDialogId (was: $closedId)")
//
//        // 🔎 sprawdź, czy jest kolejne zamówienie w kolejce (inne niż zamykane)
//        val next = pendingOrdersQueue.value.firstOrNull { it.orderId != closedId }
//
//        if (next == null) {
//            Timber.tag("OrderAlarmService").d("dismissDialog: brak kolejnych PROCESSING → STOP alarm")
//            stopAlarmService()
//        } else {
//            Timber.tag("OrderAlarmService").d("dismissDialog: next=${next.orderId} → nie wyłączam alarmu, RING dla next")
//            // ➜ nie STOPujemy; odświeżamy notyfikację i „bumpujemy" dźwięk bez przerwy
//            startAlarmService(next, ringOnly = true)
//        }
//
//        // czyść dialog
//        _uiState.update { it.copy(orderToShowInDialog = null) }
//        // ✅ Czyszczenie persystencji
//        savedStateHandle["currentDialogId"] = null
//        Timber.d("🔓 Usunięto currentDialogId z SavedStateHandle")
//
//        // okno suppress po zamknięciu (nie blokuje RING)
//        autoOpenGuardJob?.cancel()
//        autoOpenGuardJob = viewModelScope.launch {
//            _suppressAutoOpen.value = true
//            delay(700)
//            _suppressAutoOpen.value = false
//        }
//    }
//
//    // ...existing code...
//
//    // ------------------- Private -------------------
//    private fun canPostNotifications(): Boolean {
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            ContextCompat.checkSelfPermission(
//                context, android.Manifest.permission.POST_NOTIFICATIONS
//            ) == PackageManager.PERMISSION_GRANTED
//        } else {
//            true
//        }
//    }
//
//    // ⬇️ PODMIENIĆ Twoją obecną wersję NA TĘ:
//    private fun startAlarmService(order: Order, ringOnly: Boolean = false) {
//        // 🔍 LOG 1: Punkt wejścia - zawsze loguj na początku
//        Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
//        Timber.tag("ALARM START").w("🚨 [OrdersViewModel] ALARM CALL")
//        Timber.tag("ALARM START").w("   ├─ orderId: ${order.orderId}")
//        Timber.tag("ALARM START").w("   ├─ orderNumber: ${order.orderNumber}")
//        Timber.tag("ALARM START").w("   ├─ ringOnly: $ringOnly")
//        Timber.tag("ALARM START").w("   ├─ orderStatus: ${order.orderStatus.slug}")
//        Timber.tag("ALARM START").w("   ├─ createdAt: ${order.createdAt}")
//        Timber.tag("ALARM START").w("   ├─ action: ${if (ringOnly) "ACTION_RING" else "ACTION_START"}")
//        Timber.tag("ALARM START").w("   ├─ Thread: ${Thread.currentThread().name}")
//        Timber.tag("ALARM START").w("   ├─ Stack trace:")
//        Thread.currentThread().stackTrace.take(8).forEachIndexed { idx, element ->
//            Timber.tag("ALARM START").w("   │  $idx: $element")
//        }
//        Timber.tag("ALARM START").w("   └─ Timestamp: ${System.currentTimeMillis()}")
//
//        // 🔐 zgoda na notyfikacje
//        val hasPermission = canPostNotifications()
//        Timber.tag("ALARM START").d("🔐 Sprawdzam uprawnienia POST_NOTIFICATIONS: $hasPermission")
//        if (!hasPermission) {
//            Timber.tag("ALARM START").e("❌ ALARM BLOCKED: Brak uprawnień POST_NOTIFICATIONS")
//            Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
//            return
//        }
//
//        // 🛑 UWAGA: suppress dotyczy tylko pełnego otwarcia; RING ma go ignorować
//        val isSuppressed = _suppressAutoOpen.value
//        Timber.tag("ALARM START").d("🛑 Sprawdzam suppress: $isSuppressed")
//        if (!ringOnly && isSuppressed) {
//            Timber.tag("ALARM START").e("❌ ALARM BLOCKED: Okno suppress aktywne (700ms po zamknięciu dialogu)")
//            Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
//            return
//        }
//
//        // 📦 Przygotowanie payloadu
//        Timber.tag("ALARM START").d("📦 Tworzę Intent dla OrderAlarmService...")
//        val jsonPayload = gson.toJson(OrderWrapper(order))
//        val intent = Intent(context, OrderAlarmService::class.java).apply {
//            action = if (ringOnly) OrderAlarmService.ACTION_RING else OrderAlarmService.ACTION_START
//            putExtra(OrderAlarmService.EXTRA_ORDER_ID, order.orderId)
//            putExtra(OrderAlarmService.EXTRA_ORDER_JSON, jsonPayload)
//        }
//
//        // 🚀 Uruchomienie serwisu
//        val isForeground = AppStateManager.isAppInForeground
//        Timber.tag("ALARM START").d("🚀 App w foreground: $isForeground")
//
//        try {
//            if (isForeground) {
//                Timber.tag("ALARM START").i("✅ Uruchamiam OrderAlarmService (startService)")
//                context.startService(intent)              // UI na wierzchu
//            } else {
//                Timber.tag("ALARM START").i("✅ Uruchamiam OrderAlarmService (startForegroundService)")
//                ContextCompat.startForegroundService(context, intent) // tło → FGS
//            }
//            Timber.tag("ALARM START").w("✅ ALARM STARTED SUCCESSFULLY!")
//            Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
//        } catch (e: Exception) {
//            Timber.tag("ALARM START").e(e, "❌ BŁĄD podczas uruchamiania OrderAlarmService!")
//            Timber.tag("ALARM START").e("   ├─ orderId: ${order.orderId}")
//            Timber.tag("ALARM START").e("   ├─ ringOnly: $ringOnly")
//            Timber.tag("ALARM START").e("   └─ error: ${e.message}")
//            Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
//        }
//    }
//    private fun stopAlarmService() {
//        Timber.d("ViewModel: Wysyłam polecenie ACTION_STOP_ALARM do OrderAlarmService.")
//        val intent = Intent(context, OrderAlarmService::class.java).apply {
//            action = OrderAlarmService.ACTION_STOP_ALARM
//        }
//        context.startService(intent)
//    }
//
//    private fun initUserSession() = viewModelScope.launch {
//        val role = tokenProvider.getRole() ?: UserRole.STAFF
//        _uiState.update { it.copy(userRole = role, userId = tokenProvider.getUserID()) }
//
////        fetchGeneralSettings()
//
//        _uiState.update { it.copy(isInitializing = false) }
//    }
//
//    private suspend fun checkShiftStatusSuspend() {
//        _uiState.update { it.copy(isGlobalLoading = true) }
//        when (val res = repository.getShiftStatus(LocalDate.now().toString())) {
//            is Resource.Success -> {
//                val active = res.value.shift?.status == CourierStatus.ACTIVE
//                _uiState.update {
//                    it.copy(
//                        isGlobalLoading = false,
//                        isShiftActive = active,
//                        showAssignmentPrompt = !active,
//                        isShiftStatusKnown = true
//                    )
//                }
//            }
//
//            is Resource.Failure -> {
//                _uiState.update {
//                    it.copy(
//                        isGlobalLoading = false,
//                        showAssignmentPrompt = true,
//                        isShiftStatusKnown = true
//                    )
//                }
//            }
//
//            is Resource.Loading -> Unit
//        }
//    }
//
//    private fun monitorHomeReadiness() {
//        uiState
//            .map { s ->
//                when (s.userRole) {
//                    UserRole.COURIER ->
//                        if (!s.isShiftStatusKnown) false
//                        else if (s.isShiftActive) s.routeState is OrderRouteState.Success
//                        else !s.isFetchingVehicles
//
//                    else -> !s.isInitializing
//                }
//            }
//            .distinctUntilChanged()
//            .onEach { ready -> if (ready) _isHomeReady.value = true }
//            .launchIn(viewModelScope)
//    }
//
//    // ✅ ZAKTUALIZOWANA FUNKCJA
//    /**
//     * Ta funkcja jest JEDYNYM źródłem prawdy o tym, kiedy pokazać dialog
//     * i uruchomić alarm (w odpowiedzi na zmiany w bazie danych).
//     */
//    // ✅ ZAKTUALIZOWANA FUNKCJA
//    private fun observePendingOrdersQueue() {
//        // KDS nie używa dialogów zamówień ani OrderAlarmService — celowo wyłączone
//    }
//
//    private fun observeSocketEvents() {
//        socketEventsRepo.connection
//            .onEach { connected -> _uiState.update { it.copy(socketConnected = connected) } }
//            .launchIn(viewModelScope)
//
//        uiState.map { it.userRole != UserRole.COURIER }
//            .distinctUntilChanged()
//            .flatMapLatest { enabled ->
//                if (enabled) {
//                    // ✅ FIX: Deduplikacja + debounce zabezpiecza przed wielokrotnymi emisjami tego samego zamówienia
//                    socketEventsRepo.orders
//                        .distinctUntilChanged { old, new -> old.orderId == new.orderId }
//                        .debounce(500) // Czekaj 500ms aby zgrupować wielokrotne emisje (zwiększone z 100ms → pokrywa opóźnienia do 500ms)
//                } else {
//                    emptyFlow<Order>() // Typ Order musi być explicit dla type inference
//                }
//            }
//            .onEach { order ->
//                Timber.d("📥 Received order from socket: orderId=${order.orderId}, externalDelivery.status=${order.externalDelivery?.status}")
//                repository.insertOrUpdateOrder(OrderMapper.fromOrder(order))
//                Timber.d("💾 Order saved to database: orderId=${order.orderId}")
//
//                // AUTO-DRUK DLA DINE_IN / ROOM_SERVICE (omija akceptację)
//                if (order.deliveryType == OrderDelivery.DINE_IN || order.deliveryType == OrderDelivery.ROOM_SERVICE) {
//
//                    viewModelScope.launch {
//                        // Sprawdź ustawienie (suspend function - musi być w launch)
//                        if (!appPreferencesManager.getAutoPrintDineInEnabled()) {
//                            Timber.tag("AUTO_PRINT_DINE_IN").d("⏭️ Auto-druk DINE_IN wyłączony w ustawieniach")
//                            return@launch
//                        }
//
//                        // Sprawdź czy już było wydrukowane (thread-safe)
//                        if (wasPrinted(order.orderId)) {
//                            Timber.tag("AUTO_PRINT_DINE_IN").d("⏭️ Pomijam drukowanie - zamówienie już było wydrukowane: ${order.orderNumber}")
//                            return@launch
//                        }
//
//                        Timber.tag("AUTO_PRINT_DINE_IN").w("🔔 Warunek auto-druku DINE_IN spełniony dla ${order.orderNumber}")
//                        Timber.tag("AUTO_PRINT_DINE_IN").w("   deliveryType=${order.deliveryType}")
//
//                        // Oznacz jako wydrukowane (thread-safe, zapobieganie race condition)
//                        markAsPrinted(order.orderId)
//
//                        try {
//                            val printersSelection = appPreferencesManager.getAutoPrintDineInPrinters()
//                            Timber.tag("PRINT_DEBUG").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
//                            Timber.tag("PRINT_DEBUG").w("🔔 AUTO-DRUK DINE_IN")
//                            Timber.tag("PRINT_DEBUG").w("   ├─ order: %s", order.orderNumber)
//                            Timber.tag("PRINT_DEBUG").w("   ├─ deliveryType: %s", order.deliveryType)
//                            Timber.tag("PRINT_DEBUG").w("   └─ printers: %s", printersSelection)
//                            Timber.tag("PRINT_DEBUG").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
//
//                            when (printersSelection) {
//                                "main" -> {
//                                    Timber.tag("PRINT_DEBUG").w("🖨️ WYBRANO: Tylko główna")
//                                    printerService.printOrder(order)
//                                    Timber.tag("PRINT_DEBUG").w("✅ Wydrukowano na drukarce głównej")
//                                }
//                                "kitchen" -> {
//                                    Timber.tag("PRINT_DEBUG").w("🍳 WYBRANO: Tylko kuchnia")
//                                    printerService.printKitchenTicket(order)
//                                    Timber.tag("PRINT_DEBUG").w("✅ Wydrukowano na drukarce kuchennej")
//                                }
//                                "both" -> {
//                                    Timber.tag("PRINT_DEBUG").w("📄🍳 WYBRANO: Obie drukarki")
//                                    printerService.printOrder(order)
//                                    Timber.tag("PRINT_DEBUG").w("✅ Wydrukowano na drukarce głównej")
//                                    printerService.printKitchenTicket(order)
//                                    Timber.tag("PRINT_DEBUG").w("✅ Wydrukowano na drukarce kuchennej")
//                                }
//                                else -> {
//                                    // Domyślnie obie (dla kompatybilności wstecznej)
//                                    Timber.tag("PRINT_DEBUG").w("⚠️ NIEZNANA OPCJA: %s - drukuję na obu", printersSelection)
//                                    printerService.printOrder(order)
//                                    printerService.printKitchenTicket(order)
//                                }
//                            }
//
//                            Timber.tag("PRINT_DEBUG").w("✅ Auto-druk DINE_IN/ROOM_SERVICE zakończony dla %s", order.orderNumber)
//                        } catch (e: Exception) {
//                            Timber.e(e, "❌ Błąd auto-druku DINE_IN/ROOM_SERVICE dla %s", order.orderNumber)
//                            // W przypadku błędu usuń z setu
//                            printedOrdersMutex.withLock {
//                                printedOrderIds.remove(order.orderId)
//                            }
//                        }
//                    }
//                }
//
//                // AUTO-DRUK DLA ZAMÓWIEŃ PRZYCHODZĄCYCH JUŻ ZAAKCEPTOWANYCH
//                // (np. z platform Uber Eats, Glovo - przychodzą już ze statusem ACCEPTED)
//                // UWAGA: To NIE dotyczy zmiany statusu przez socket! (socketEventsRepo.statuses obsługuje to)
//                val orderStatus = order.orderStatus.slug?.let { slug ->
//                    runCatching { OrderStatusEnum.valueOf(slug) }.getOrNull()
//                }
//
//                if (orderStatus == OrderStatusEnum.ACCEPTED &&
//                    order.deliveryType != OrderDelivery.DINE_IN &&
//                    order.deliveryType != OrderDelivery.ROOM_SERVICE) {
//
//                    // ✅ DEDUPLIKACJA #1: Okno czasowe - blokuje duplikaty z backendu (np. 2x ORDER_ACCEPTED)
//                    if (!shouldAllowPrintEvent(order.orderId)) {
//                        Timber.tag("AUTO_PRINT_ACCEPTED").w("⏭️ Pomijam drukowanie - duplikat w oknie czasowym: ${order.orderNumber}")
//                        return@onEach
//                    }
//
//                    // ✅ FIX: Sprawdź czy to nie było manualne zaakceptowanie TUTAJ (przed viewModelScope.launch!)
//                    // MUSI BYĆ tutaj żeby nie było race condition z asynchronicznym launch
//                    if (manuallyAcceptedOrders.contains(order.orderId)) {
//                        Timber.tag("AUTO_PRINT_ACCEPTED").d("⏭️ Pomijam drukowanie - zamówienie było manualnie zaakceptowane: ${order.orderNumber}")
//                        return@onEach
//                    }
//
//                    // ✅ CRITICAL FIX: Sprawdź i oznacz SYNCHRONICZNIE (PRZED viewModelScope.launch!)
//                    // Zapobiega race condition gdy socket wysyła ORDER i STATUS_CHANGE jednocześnie
//                    if (wasPrintedSync(order.orderId)) {
//                        Timber.tag("AUTO_PRINT_ACCEPTED").d("⏭️ Pomijam drukowanie - zamówienie już było wydrukowane: ${order.orderNumber}")
//                        return@onEach
//                    }
//
//                    // Oznacz jako wydrukowane NATYCHMIAST (synchronicznie!)
//                    markAsPrintedSync(order.orderId)
//
//                    viewModelScope.launch {
//                        // Sprawdź ustawienie (suspend function)
//                        if (!appPreferencesManager.getAutoPrintAcceptedEnabled()) {
//                            Timber.tag("AUTO_PRINT_ACCEPTED").d("⏭️ Auto-druk ACCEPTED wyłączony w ustawieniach")
//                            // Rollback - usuń z printedOrderIds bo nie drukujemy
//                            printedOrdersMutex.withLock {
//                                printedOrderIds.remove(order.orderId)
//                            }
//                            return@launch
//                        }
//
//                        Timber.tag("AUTO_PRINT_ACCEPTED").d("🔔 Zamówienie przyszło już ACCEPTED: ${order.orderNumber}")
//
//
//                        try {
//                            Timber.d("🖨️ OrdersViewModel: Auto-druk zamówienia już zaakceptowanego: %s", order.orderNumber)
//                            printerService.printAfterOrderAccepted(order)
//                            Timber.d("✅ Auto-druk zakończony dla %s", order.orderNumber)
//                        } catch (e: Exception) {
//                            Timber.e(e, "❌ Błąd auto-druku dla %s", order.orderNumber)
//                            // W przypadku błędu usuń z setu
//                            printedOrdersMutex.withLock {
//                                printedOrderIds.remove(order.orderId)
//                            }
//                        }
//                    }
//                }
//            }
//            .launchIn(viewModelScope)
//
//        socketEventsRepo.ordersTras
//            .onEach(::processRouteData)
//            .launchIn(viewModelScope)
//
//        socketEventsRepo.statuses
//            .distinctUntilChanged { old, new -> old.orderId == new.orderId && old.newStatus == new.newStatus }
//            .debounce(100) // ✅ ZMNIEJSZONE: 100ms zamiast 500ms (szybsza deduplikacja)
//            .onEach { s ->
//                updateOrderStatusInDb(s.orderId, s.newStatus)
//
//                // AUTO-DRUK gdy status zmienia się na ACCEPTED przez socket (zewnętrzna akceptacja)
//                if (s.newStatus == OrderStatusEnum.ACCEPTED) {
//
//                    // ✅ DEDUPLIKACJA #1: Okno czasowe - blokuje duplikaty z backendu (np. 2x ORDER_ACCEPTED)
//                    // CRITICAL: Musi być PRZED viewModelScope.launch aby zapobiec race condition!
//                    if (!shouldAllowPrintEvent(s.orderId)) {
//                        Timber.tag("AUTO_PRINT_STATUS_CHANGE").w("⏭️ Pomijam drukowanie - duplikat w oknie czasowym: ${s.orderId}")
//                        return@onEach
//                    }
//
//                    // Sprawdź czy to nie było manualne zaakceptowanie (żeby uniknąć duplikacji)
//                    // ✅ FIX: Sprawdź i oznacz SYNCHRONICZNIE (PRZED viewModelScope.launch!)
//                    // Zapobiega race condition gdy dwa eventy przychodzą jednocześnie
//                    if (wasPrintedSync(s.orderId)) {
//                        Timber.tag("AUTO_PRINT_STATUS_CHANGE").d("⏭️ Pomijam drukowanie - zamówienie już było wydrukowane: ${s.orderId}")
//                        return@onEach
//                    }
//
//                    // Oznacz jako wydrukowane NATYCHMIAST (synchronicznie!)
//                    markAsPrintedSync(s.orderId)
//
//                    viewModelScope.launch {
//                        // Sprawdź ustawienie (suspend function)
//                        if (!appPreferencesManager.getAutoPrintAcceptedEnabled()) {
//                            Timber.tag("AUTO_PRINT_STATUS_CHANGE").d("⏭️ Auto-druk ACCEPTED wyłączony w ustawieniach")
//                            // Rollback - usuń z printedOrderIds bo nie drukujemy
//                            printedOrdersMutex.withLock {
//                                printedOrderIds.remove(s.orderId)
//                            }
//                            return@launch
//                        }
//
//                        Timber.tag("AUTO_PRINT_STATUS_CHANGE").d("🔔 Status zmieniony na ACCEPTED przez socket: ${s.orderId}")
//
//
//                        try {
//                            // Pobierz pełne zamówienie z bazy przez Flow
//                            val allOrders = repository.getAllOrdersFlow().first()
//                            val entity = allOrders.find { it.orderId == s.orderId }
//
//                            if (entity != null) {
//                                val orderModel = OrderMapper.toOrder(entity)
//
//                                // Wykluczamy DINE_IN/ROOM_SERVICE (mają własną logikę)
//                                if (orderModel.deliveryType != OrderDelivery.DINE_IN &&
//                                    orderModel.deliveryType != OrderDelivery.ROOM_SERVICE) {
//
//                                    Timber.d("🖨️ Auto-druk po zmianie statusu na ACCEPTED: %s", orderModel.orderNumber)
//                                    printerService.printAfterOrderAccepted(orderModel)
//                                    Timber.d("✅ Auto-druk zakończony dla %s", orderModel.orderNumber)
//                                } else {
//                                    Timber.d("⏭️ Pomijam drukowanie - typ ${orderModel.deliveryType} ma własną logikę")
//                                }
//                            } else {
//                                Timber.w("⚠️ Nie znaleziono zamówienia w bazie: ${s.orderId}")
//                            }
//                        } catch (e: Exception) {
//                            Timber.e(e, "❌ Błąd auto-druku po zmianie statusu dla ${s.orderId}")
//                            // W przypadku błędu usuń z setu
//                            printedOrdersMutex.withLock {
//                                printedOrderIds.remove(s.orderId)
//                            }
//                        }
//                    }
//                }
//            }
//            .launchIn(viewModelScope)
//    }
//
//    private fun processRouteData(routeStops: List<OrderTras>) {
//        unlockAllLocks()
//        val clusters = mutableListOf<RouteCluster>()
//        var current = mutableListOf<RouteSegment>()
//        var idx = 0
//        routeStops.forEach { stop ->
//            stop.polyline.takeIf { it.isNotBlank() }?.let {
//                current.add(
//                    RouteSegment(
//                        points = PolyUtil.decode(it),
//                        status = stop.status,
//                        orderNumber = stop.orderNumber
//                    )
//                )
//            }
//            if (stop.orderNumber.equals("RETURN", ignoreCase = true) && current.isNotEmpty()) {
//                clusters.add(RouteCluster(current.toList(), idx))
//                current = mutableListOf()
//                idx++
//            }
//        }
//        if (current.isNotEmpty()) clusters.add(RouteCluster(current.toList(), idx))
//        _routeClusters.value = clusters
//        _uiState.update { it.copy(routeState = OrderRouteState.Success(routeStops)) }
//    }
//
//    private fun executeOrderUpdate(block: suspend () -> Resource<Order>) = viewModelScope.launch {
//        Timber.tag("OrderAlarmService").d(" executeOrderUpdate stopAlarmService()")
//        _event.emit(OrderEvent.Loading)
//        when (val res = block()) {
//            is Resource.Success -> {
//                stopAlarmService()
//                repository.insertOrUpdateOrder(OrderMapper.fromOrder(res.value))
//                _event.emit(OrderEvent.Success(res.value.orderNumber))
//
//                // AUTO-DRUK PO AKCEPTACJI
//                if (res.value.orderStatus.slug == OrderStatusEnum.ACCEPTED.name &&
//                    appPreferencesManager.getAutoPrintAcceptedEnabled()) {
//
//                    // ✅ NOTE: manuallyAcceptedOrders już dodane w updateOrder() PRZED API request!
//                    // Nie trzeba dodawać tutaj ponownie
//
//                    viewModelScope.launch {
//                        try {
//                            // ✅ FIX: Oznacz jako wydrukowane PRZED drukowaniem (żeby zabezpieczyć przed race condition)
//                            markAsPrinted(res.value.orderId)
//
//                            Timber.d("🖨️ OrdersViewModel: Wywołuję printAfterOrderAccepted dla %s", res.value.orderNumber)
//                            printerService.printAfterOrderAccepted(res.value)
//                        } catch (e: Exception) {
//                            Timber.e(e, "❌ OrdersViewModel: Błąd drukowania po zaakceptowaniu: %s", res.value.orderNumber)
//
//                            // W przypadku błędu usuń z wydrukowanych (żeby można było ponowić)
//                            printedOrdersMutex.withLock {
//                                printedOrderIds.remove(res.value.orderId)
//                            }
//
//                            withContext(Dispatchers.Main) {
//                                Toast.makeText(context, "Błąd drukowania: ${e.message}", Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                    }
//                }
//            }
//
//            is Resource.Failure -> _event.emit(OrderEvent.Error(res.errorCode, res.errorBody))
//            else -> Unit
//        }
//    }
//
//
//    private fun lockOrders(ids: Set<String>) {
//        if (ids.isEmpty()) return
//        _lockedOrderIds.update { it + ids }
//        ids.forEach { id ->
//            lockJobs[id]?.cancel()
//            lockJobs[id] = viewModelScope.launch {
//                delay(LOCK_TIMEOUT_MS)
//                unlockOrders(setOf(id))
//            }
//        }
//        _selectedOrderIds.update { it - ids }
//        if (_selectedOrderIds.value.isEmpty()) _currentBatchStatus.value = null
//    }
//
//    private fun unlockOrders(ids: Set<String>) {
//        if (ids.isEmpty()) return
//        ids.forEach { id -> lockJobs.remove(id)?.cancel() }
//        _lockedOrderIds.update { it - ids }
//    }
//
//    private fun unlockAllLocks() {
//        lockJobs.values.forEach { it.cancel() }
//        lockJobs.clear()
//        _lockedOrderIds.value = emptySet()
//    }
//
//    private fun updateOrderStatusInDb(orderId: String, newStatus: OrderStatusEnum) =
//        viewModelScope.launch { repository.updateOrderStatusSlug(orderId, newStatus) }
//
//    companion object {
//        private const val TAG = "OrdersViewModel"
//        private const val LOCK_TIMEOUT_MS = 10_000L
//    }
//}
