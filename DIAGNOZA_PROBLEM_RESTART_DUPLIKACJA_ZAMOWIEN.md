# 🔍 DIAGNOZA: Problem z duplikacją zamówień po restarcie i ponownym alarmem

## 📋 Opis symptomów

### Problem #1: Po restarcie aplikacji zamówienie ponownie się otwiera
- Zamówienie zostaje zaakceptowane (status: ACCEPTED)
- Restart aplikacji
- To samo zamówienie pojawia się ponownie jako "nowe" i wymaga reakcji

### Problem #2: Po czasie zamówienie znowu uruchamia alarm
- Zamówienie jest już zaakceptowane
- Po pewnym czasie (np. 10 minut) zamówienie ponownie uruchamia alarm
- Zachowuje się jakby było "nowe" mimo statusu ACCEPTED

---

## 🎯 ANALIZA MOŻLIWYCH PRZYCZYN

### 📊 **TOP 5 NAJBARDZIEJ PRAWDOPODOBNYCH SCENARIUSZY**

---

## ⭐ **SCENARIUSZ #1: Brak persystencji stanu otwartego dialogu** 
**Prawdopodobieństwo: 95%** ✅

### Dlaczego pasuje do objawów:
- ✅ Kod w `OrdersViewModel.kt` zapisuje `currentDialogId` do `SavedStateHandle` (linia 1117)
- ❌ **PROBLEM**: NIE MA ODCZYTU tego stanu przy starcie aplikacji/ViewModel
- ✅ Tłumaczy dlaczego po restarcie otwiera zaakceptowane zamówienie
- ✅ Tłumaczy dlaczego nie działa "pamięć" o zamkniętym dialogu

### Mechanizm:
```kotlin
// ZAPISYWANIE - OK (linia 1117):
fun triggerOpenDialog(order: Order?) {
    savedStateHandle["currentDialogId"] = order?.orderId  // ✅
}

// BRAK ODCZYTU przy init ViewModel! ❌
init {
    // NIE MA: 
    // val restoredDialogId = savedStateHandle.get<String?>("currentDialogId")
    // if (restoredDialogId != null) { /* przywróć stan */ }
}
```

### Co się dzieje:
1. Użytkownik otwiera zamówienie → zapisane do `savedStateHandle["currentDialogId"]`
2. Zamówienie zostaje zaakceptowane → status w DB zmienia się na ACCEPTED
3. **Restart aplikacji**
4. ViewModel startuje od nowa → `savedStateHandle["currentDialogId"]` ma wartość ale NIE JEST ODCZYTANA
5. `observePendingOrdersQueue()` nie wie że to zamówienie było już obsłużone
6. ❌ **Zamówienie NIE jest w kolejce `pendingOrdersQueue` (bo status=ACCEPTED, a kolejka filtruje tylko PROCESSING)**
7. ✅ **ALE**: Jeśli jest jakiekolwiek inne zamówienie PROCESSING → otwiera je
8. ❌ **PROBLEM**: Jeśli backend/socket wyśle ponownie update tego zamówienia → może trafić do kolejki

---

## ⭐ **SCENARIUSZ #2: Synchronizacja z API nadpisuje lokalny status**
**Prawdopodobieństwo: 85%** ✅

### Dlaczego pasuje do objawów:
- ✅ Tłumaczy "po pewnym czasie alarm się uruchamia ponownie"
- ✅ `syncOrdersFromApiStartOfDay()` wywołuje się przy reconnect WebSocket (linia 499)
- ❌ Synchronizacja może nadpisać status lokalny jeśli API ma starsze dane

### Mechanizm:
```kotlin
// observeSocketConnection() - linia 482-505
private fun observeSocketConnection() {
    combine(
        socketEventsRepo.connection,
        _uiState.map { it.userRole }
    ) { isConnected, role ->
        if (isConnected && role != UserRole.COURIER) {
            syncOrdersFromApiStartOfDay()  // ⚠️ RYZYKO NADPISANIA
        }
    }
}
```

### Race condition:
1. Zamówienie zaakceptowane lokalnie → DB ma status ACCEPTED
2. Backend ma opóźnienie w synchronizacji (np. 2 sekundy)
3. WebSocket reconnect → `syncOrdersFromApiStartOfDay()`
4. API zwraca zamówienie ze statusem PROCESSING (backend jeszcze nie zaktualizował)
5. ❌ `syncLocalDatabaseWithRemote()` → **NADPISUJE** lokalny ACCEPTED na PROCESSING
6. Zamówienie wraca do kolejki `pendingOrdersQueue` (filtr: status=PROCESSING)
7. ✅ Alarm się uruchamia ponownie!

### Kod w `OrdersRepository.kt` (linia 110-125):
```kotlin
suspend fun syncLocalDatabaseWithRemote(
    remoteOrders: List<OrderEntity>,
    syncDate: String
) {
    database.withTransaction {
        orderDao.insertOrUpdateAll(remoteOrders)  // ⚠️ NADPISUJE wszystko!
        orderDao.deleteStaleOrdersForDate(remoteOrderIds)
    }
}
```

**BRAK LOGIKI**: Nie sprawdza czy lokalny status jest "nowszy" niż zdalny!

---

## ⭐ **SCENARIUSZ #3: Ponowne przetwarzanie eventów z WebSocket po reconnect**
**Prawdopodobieństwo: 75%** ✅

### Dlaczego pasuje do objawów:
- ✅ Backend wysyła eventy typu `ORDER_PROCESSING` / `ORDER_CREATED` ponownie po reconnect
- ✅ Logi pokazują `emitCount=11` dla `ORDER_ACCEPTED` (linia z user context: API logs)
- ✅ Brak deduplikacji eventów po `eventId` lub `timestamp`

### Mechanizm:
```kotlin
// SocketStaffEventsHandler.kt - linia 295-370
private fun handleNewOrProcessingOrder(args: Array<Any>) {
    val order = orderWrapper.order
    val isProcessing = slugEnum == OrderStatusEnum.PROCESSING
    
    if (isProcessing) {
        // ⚠️ BRAK SPRAWDZENIA: Czy to zamówienie nie było już obsłużone?
        ordersRepository.insertOrUpdateOrder(OrderMapper.fromOrder(order))
        socketEventsRepo.emitOrder(order)  // Emit do Flow
    }
}
```

### Brak idempotencji:
- Backend może wysłać ten sam event wielokrotnie (np. po reconnect)
- **NIE MA** sprawdzenia `createdAt` czy zamówienie jest "świeże"
- **NIE MA** sprawdzenia `eventId` dla deduplikacji
- Zamówienie wchodzi do DB → trafia do `orders` Flow → `observePendingOrdersQueue()` reaguje

---

## ⭐ **SCENARIUSZ #4: `lastClosedOrderId` resetuje się po restarcie**
**Prawdopodobieństwo: 70%** ✅

### Dlaczego pasuje do objawów:
- ✅ `lastClosedOrderId` jest zmienną w pamięci (nie persystowana)
- ✅ Po restarcie aplikacji = `null`
- ✅ Logika w `observePendingOrdersQueue()` używa tego do filtrowania

### Mechanizm:
```kotlin
// OrdersViewModel.kt - linia 149
private var lastClosedOrderId: String? = null  // ⚠️ Tylko w pamięci!

// dismissDialog() - linia 1125
fun dismissDialog() {
    lastClosedOrderId = closedId  // Zapisane tylko do zmiennej
}

// observePendingOrdersQueue() - linia 1329
val next = queue.firstOrNull { it.orderId != lastClosedOrderId }
//                                           ↑ Po restarcie = null!
```

### Co się dzieje:
1. Użytkownik zamyka dialog zamówienia X → `lastClosedOrderId = "X"`
2. Restart aplikacji
3. `lastClosedOrderId = null` (nie ma persystencji!)
4. `observePendingOrdersQueue()` → filtr `it.orderId != lastClosedOrderId` przepuszcza wszystko
5. ✅ Zamówienie X wraca do kolejki (jeśli status=PROCESSING)

---

## ⭐ **SCENARIUSZ #5: Duplikacja handlerów WebSocket**
**Prawdopodobieństwo: 40%** ⚠️

### Dlaczego może pasować:
- ⚠️ Mniej prawdopodobne - logi pokazują `emitCount` (kumulatywny licznik, nie duplikacja handlerów)
- ⚠️ `SocketStaffEventsHandler.register()` wywołane raz (`@Singleton`)

### Gdzie sprawdzić:
```kotlin
// SocketStaffEventsHandler.kt - linia 94-100
fun register() {
    eventHandlers.forEach { (event, handler) ->
        SocketManager.on(event.name, handler)  // ⚠️ Czy .off() jest wywoływane?
    }
}
```

**NIE MA**: `.off()` przed ponowną rejestracją → teoretycznie przy restart SocketService może dodać drugi handler.

---

## 🔍 SZCZEGÓŁOWE MIEJSCA DO SPRAWDZENIA

### 📂 **1. GDZIE USTAWIANY JEST STATUS ZAMÓWIENIA**

| Plik | Funkcja | Linia | Co robi | Problem |
|------|---------|-------|---------|---------|
| `OrdersViewModel.kt` | `updateOrder()` | 824-835 | Wysyła API request zmiany statusu | ✅ Dodaje do `manuallyAcceptedOrders` (30s) |
| `OrdersViewModel.kt` | `executeOrderUpdate()` | 1667-1710 | Callback po API response | ✅ Zapisuje do DB przez `insertOrUpdateOrder()` |
| `OrdersRepository.kt` | `updateOrderStatusSlug()` | 144 | Update statusu w DB (tylko slug) | ✅ Działa poprawnie |
| `SocketStaffEventsHandler.kt` | `handleStatusUpdate()` | 370-404 | Zmiana statusu z WebSocket | ⚠️ NADPISUJE lokalny status! |
| `OrdersRepository.kt` | `syncLocalDatabaseWithRemote()` | 110-125 | Sync z API | ❌ **NADPISUJE wszystko bez weryfikacji!** |

### 📂 **2. GDZIE OTWIERANY JEST DIALOG ZAMÓWIENIA**

| Plik | Funkcja | Linia | Trigger | Problem |
|------|---------|-------|---------|---------|
| `OrdersViewModel.kt` | `triggerOpenDialog()` | 1116-1120 | Manualne otwarcie | ✅ Zapisuje do `SavedStateHandle` |
| `OrdersViewModel.kt` | `observePendingOrdersQueue()` | 1306-1404 | Auto-open dla PROCESSING | ❌ **NIE ODCZYTUJE `SavedStateHandle` przy init!** |
| `HomeActivity.kt` | `onNewIntent()` | ? | Intent z notyfikacji | ❓ Nie znaleziono logiki |

### 📂 **3. GDZIE URUCHAMIANY JEST ALARM**

| Plik | Funkcja | Linia | Trigger | Czy ma deduplikację? |
|------|---------|-------|---------|---------------------|
| `OrdersViewModel.kt` | `startAlarmService()` | 1168-1217 | Z `observePendingOrdersQueue()` | ✅ Debounce 500ms (`lastAlarmStartOrderId`) |
| `SocketStaffEventsHandler.kt` | `startAlarmServiceSafely()` | 420-445 | Z WebSocket REMINDER | ❌ Brak deduplikacji! |
| `SocketStaffEventsHandler.kt` | `handleNewOrProcessingOrder()` | 295-340 | ORDER_CREATED socket | ❌ Wywołuje tylko jeśli app w tle |

### 📂 **4. GDZIE OBSŁUGIWANE SĄ EVENTY Z WEBSOCKET**

| Plik | Event | Handler | Linia | Problem |
|------|-------|---------|-------|---------|
| `SocketStaffEventsHandler.kt` | `ORDER_CREATED` | `handleNewOrProcessingOrder()` | 295 | ❌ Brak sprawdzenia `createdAt` (czy świeże) |
| `SocketStaffEventsHandler.kt` | `ORDER_PROCESSING` | `handleStatusUpdate()` | 370 | ❌ Nadpisuje lokalny status |
| `SocketStaffEventsHandler.kt` | `ORDER_ACCEPTED` | `handleStatusUpdate()` | 370 | ✅ Deduplikacja w `observeSocketEvents()` |
| `SocketStaffEventsHandler.kt` | `ORDER_REMINDER` | `handleOrderReminderAlarm()` | 259 | ❌ Brak deduplikacji! |

### 📂 **5. SYNCHRONIZACJA PO STARCIE (BOOTSTRAP)**

| Plik | Funkcja | Linia | Kiedy wywołana | Czy nadpisuje statusy? |
|------|---------|-------|----------------|----------------------|
| `OrdersViewModel.kt` | `initUserSession()` | 1233-1254 | `init{}` | ✅ NIE wywołuje sync (zakomentowana linia 1249) |
| `OrdersViewModel.kt` | `observeSocketConnection()` | 482-505 | Po reconnect WebSocket | ❌ **TAK! Wywołuje `syncOrdersFromApiStartOfDay()`** |
| `OrdersViewModel.kt` | `syncOrdersFromApiStartOfDay()` | 952-960 | Przy reconnect | ❌ Pobiera z API i NADPISUJE DB |
| `OrdersRepository.kt` | `syncLocalDatabaseWithRemote()` | 110-125 | Z sync funkcji | ❌ **Nadpisuje bez sprawdzania który status jest nowszy!** |

---

## 🐛 RETRY/REPLAY/BUFORY

### ❌ **NIE ZNALEZIONO**:
- Queue eventów WebSocket z replay
- Buffer emisji socketowych
- Retry mechanizm dla failed eventów

### ⚠️ **ZNALEZIONO**:
```kotlin
// OrdersViewModel.kt - linia 1421
socketEventsRepo.orders
    .distinctUntilChanged { old, new -> old.orderId == new.orderId }
    .debounce(500)  // ✅ Deduplikacja na poziomie Flow
```

**PROBLEM**: Działa tylko dla tego samego `orderId` w krótkim czasie. NIE chroni przed:
- Tym samym zamówieniem po 10 minutach (reconnect)
- Zmianą statusu ACCEPTED → PROCESSING (backend delay)

---

## 📍 KONKRETNE MIEJSCA LOGÓW DO DODANIA

### 🔍 **TAG: "DIALOG_PERSISTENCE"**
```kotlin
// OrdersViewModel.kt - init block
init {
    val restoredDialogId = savedStateHandle.get<String?>("currentDialogId")
    Timber.tag("DIALOG_PERSISTENCE").d("═══════════════════════════════════════")
    Timber.tag("DIALOG_PERSISTENCE").d("🔄 ViewModel INIT - Restore state")
    Timber.tag("DIALOG_PERSISTENCE").d("   ├─ restoredDialogId: $restoredDialogId")
    Timber.tag("DIALOG_PERSISTENCE").d("   ├─ Thread: ${Thread.currentThread().name}")
    Timber.tag("DIALOG_PERSISTENCE").d("   └─ Timestamp: ${System.currentTimeMillis()}")
}

// triggerOpenDialog()
Timber.tag("DIALOG_PERSISTENCE").d("💾 SAVE currentDialogId=${order?.orderId}")

// dismissDialog()
Timber.tag("DIALOG_PERSISTENCE").d("🗑️ CLEAR currentDialogId (was: $closedId)")
```

### 🔍 **TAG: "SYNC_CONFLICT"**
```kotlin
// OrdersRepository.syncLocalDatabaseWithRemote()
suspend fun syncLocalDatabaseWithRemote(...) {
    remoteOrders.forEach { remote ->
        val local = orderDao.getOrderById(remote.orderId)
        
        Timber.tag("SYNC_CONFLICT").d("═══════════════════════════════════════")
        Timber.tag("SYNC_CONFLICT").d("🔄 SYNC ORDER: ${remote.orderId}")
        Timber.tag("SYNC_CONFLICT").d("   ├─ Remote status: ${remote.orderStatusSlug}")
        Timber.tag("SYNC_CONFLICT").d("   ├─ Local status: ${local?.orderStatusSlug}")
        Timber.tag("SYNC_CONFLICT").d("   ├─ Remote updatedAt: ${remote.updatedAt}")
        Timber.tag("SYNC_CONFLICT").d("   ├─ Local updatedAt: ${local?.updatedAt}")
        Timber.tag("SYNC_CONFLICT").d("   └─ Will overwrite: ${local != null}")
    }
}
```

### 🔍 **TAG: "QUEUE_FILTER"**
```kotlin
// OrdersViewModel.kt - pendingOrdersQueue definition (linia 300)
val pendingOrdersQueue: StateFlow<List<Order>> =
    orders.map { allOrders ->
        Timber.tag("QUEUE_FILTER").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag("QUEUE_FILTER").d("📋 QUEUE UPDATE - Total orders: ${allOrders.size}")
        
        val filtered = allOrders.filter {
            val slugEnum = it.orderStatus?.slug?.let { slug ->
                runCatching { OrderStatusEnum.valueOf(slug) }.getOrNull()
            }
            val isProcessing = slugEnum == OrderStatusEnum.PROCESSING
            
            Timber.tag("QUEUE_FILTER").d("   Order ${it.orderNumber}: status=${it.orderStatus?.slug}, inQueue=$isProcessing")
            isProcessing
        }
        
        Timber.tag("QUEUE_FILTER").d("   └─ Filtered PROCESSING: ${filtered.size}")
        filtered.sortedBy { it.createdAt }
    }
```

### 🔍 **TAG: "SOCKET_EVENT"**
```kotlin
// SocketStaffEventsHandler.kt - handleNewOrProcessingOrder()
private fun handleNewOrProcessingOrder(args: Array<Any>) {
    val order = orderWrapper.order
    val createdAtMs = runCatching { 
        Instant.parse(order.createdAt).toEpochMilli() 
    }.getOrNull() ?: 0L
    val ageSeconds = (System.currentTimeMillis() - createdAtMs) / 1000
    
    Timber.tag("SOCKET_EVENT").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    Timber.tag("SOCKET_EVENT").d("📥 ORDER_CREATED/PROCESSING received")
    Timber.tag("SOCKET_EVENT").d("   ├─ orderId: ${order.orderId}")
    Timber.tag("SOCKET_EVENT").d("   ├─ orderNumber: ${order.orderNumber}")
    Timber.tag("SOCKET_EVENT").d("   ├─ status: ${order.orderStatus.slug}")
    Timber.tag("SOCKET_EVENT").d("   ├─ createdAt: ${order.createdAt}")
    Timber.tag("SOCKET_EVENT").d("   ├─ age: ${ageSeconds}s")
    Timber.tag("SOCKET_EVENT").d("   ├─ isProcessing: $isProcessing")
    Timber.tag("SOCKET_EVENT").d("   └─ Will emit to Flow: $isProcessing")
}

// handleStatusUpdate()
Timber.tag("SOCKET_EVENT").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
Timber.tag("SOCKET_EVENT").d("📊 STATUS UPDATE received")
Timber.tag("SOCKET_EVENT").d("   ├─ orderId: $orderId")
Timber.tag("SOCKET_EVENT").d("   ├─ action: $action")
Timber.tag("SOCKET_EVENT").d("   ├─ newStatus: ${wrapper.orderStatus.slug}")
Timber.tag("SOCKET_EVENT").d("   └─ Source: WebSocket")
```

---

## 🧪 PLAN DEBUGOWANIA - KROK PO KROKU

### **TEST #1: Restart aplikacji po zaakceptowaniu**
**Cel**: Wykryć Problem #1 (brak persystencji dialogu)

#### Kroki:
1. Uruchom aplikację
2. Otrzymaj nowe zamówienie (status: PROCESSING)
3. Dialog się otwiera automatycznie
4. **NIE AKCEPTUJ** - zostaw dialog otwarty
5. Zakończ proces aplikacji (Force Stop w ustawieniach Androida)
6. Uruchom aplikację ponownie
7. **OBSERWUJ**:
   - Czy dialog jest ponownie otwarty? → ❌ NIE (bo brak odczytu `SavedStateHandle`)
   - Czy alarm się uruchamia? → ✅ TAK (bo zamówienie nadal PROCESSING)

#### Oczekiwane logi:
```
[DIALOG_PERSISTENCE] ViewModel INIT - Restore state
   ├─ restoredDialogId: <ORDER_ID>  ← POWINNO BYĆ!
   
[QUEUE_FILTER] Filtered PROCESSING: 1  ← Zamówienie w kolejce
[OrderAlarmService] observePendingOrdersQueue TRIGGERED
   ├─ currentDialogId: null  ← PROBLEM! Powinno być <ORDER_ID>
```

---

### **TEST #2: Reconnect WebSocket po akceptacji**
**Cel**: Wykryć Problem #2 (sync nadpisuje status)

#### Kroki:
1. Otrzymaj zamówienie (PROCESSING)
2. Zaakceptuj zamówienie (status → ACCEPTED)
3. Poczekaj 2 sekundy
4. Wyłącz WiFi na 10 sekund
5. Włącz WiFi → WebSocket reconnect
6. **OBSERWUJ**:
   - Czy zamówienie wraca do kolejki?
   - Czy alarm się uruchamia ponownie?

#### Oczekiwane logi:
```
[SYNC_CONFLICT] SYNC ORDER: <ORDER_ID>
   ├─ Remote status: PROCESSING  ← Backend opóźniony!
   ├─ Local status: ACCEPTED
   ├─ Will overwrite: true  ← ❌ PROBLEM!

[QUEUE_FILTER] Order #123: status=PROCESSING, inQueue=true  ← Wrócił!
[ALARM START] ALARM CALL for orderId=<ORDER_ID>  ← ❌ NIE POWINNO BYĆ!
```

---

### **TEST #3: Sprawdzenie wieku zamówienia przy starcie**
**Cel**: Wykryć czy stare zamówienia są przetwarzane ponownie

#### Kroki:
1. Zaakceptuj wszystkie zamówienia
2. Restart aplikacji
3. Poczekaj 30 sekund
4. **OBSERWUJ logi** przy starcie:
   - Czy są zamówienia starsze niż 10 minut w `QUEUE_FILTER`?

#### Oczekiwane logi:
```
[SOCKET_EVENT] ORDER_CREATED/PROCESSING received
   ├─ orderNumber: #123
   ├─ age: 650s  ← 10+ minut!
   ├─ isProcessing: true
   └─ Will emit to Flow: true  ← ❌ PROBLEM! Powinno być false dla starych

[QUEUE_FILTER] Order #123: status=PROCESSING, inQueue=true  ← Stare zamówienie!
```

---

### **TEST #4: Duplikacja eventów z WebSocket**
**Cel**: Sprawdzić czy backend wysyła duplikaty

#### Kroki:
1. Zaakceptuj zamówienie
2. Włącz filtrowanie logów: `adb logcat -s "SOCKET_EVENT:*"`
3. Wyłącz/włącz WiFi (reconnect)
4. **OBSERWUJ**:
   - Ile eventów `ORDER_ACCEPTED` przychodzi dla tego samego zamówienia?

#### Oczekiwane logi:
```
[SOCKET_EVENT] STATUS UPDATE received
   ├─ orderId: <ORDER_ID>
   ├─ action: ORDER_ACCEPTED
   └─ Timestamp: 1234567890

[SOCKET_EVENT] STATUS UPDATE received  ← DUPLIKAT!
   ├─ orderId: <ORDER_ID>
   ├─ action: ORDER_ACCEPTED
   └─ Timestamp: 1234567891  ← +1ms później
```

---

## 🎯 PROPOZYCJE POPRAWEK (KRÓTKO)

### ✅ **POPRAWKA #1: Przywracanie stanu dialogu**
```kotlin
// OrdersViewModel.kt - init{}
init {
    val restoredDialogId = savedStateHandle.get<String?>("currentDialogId")
    if (restoredDialogId != null) {
        viewModelScope.launch {
            // Poczekaj na załadowanie zamówień z DB
            orders.first { it.isNotEmpty() }
            val order = orders.value.find { it.orderId == restoredDialogId }
            if (order?.orderStatus?.slug == OrderStatusEnum.PROCESSING.name) {
                _uiState.update { it.copy(orderToShowInDialog = order) }
            } else {
                // Zamówienie już nie PROCESSING → wyczyść
                savedStateHandle["currentDialogId"] = null
            }
        }
    }
}
```

### ✅ **POPRAWKA #2: Inteligentna synchronizacja (nie nadpisuj nowszych statusów)**
```kotlin
// OrdersRepository.kt
suspend fun syncLocalDatabaseWithRemote(remoteOrders: List<OrderEntity>, syncDate: String) {
    database.withTransaction {
        remoteOrders.forEach { remote ->
            val local = orderDao.getOrderById(remote.orderId)
            
            val shouldUpdate = when {
                local == null -> true  // Nowe zamówienie
                local.updatedAt == null -> true  // Brak timestampa
                remote.updatedAt == null -> false  // Nie nadpisuj bez timestampa
                remote.updatedAt > local.updatedAt -> true  // Remote nowsze
                else -> {
                    Timber.tag("SYNC").d("Pomijam ${remote.orderId}: local jest nowszy")
                    false
                }
            }
            
            if (shouldUpdate) {
                orderDao.insertOrUpdate(remote)
            }
        }
    }
}
```

### ✅ **POPRAWKA #3: Filtrowanie starych eventów**
```kotlin
// SocketStaffEventsHandler.kt - handleNewOrProcessingOrder()
private fun handleNewOrProcessingOrder(args: Array<Any>) {
    val order = orderWrapper.order
    
    // ✅ Sprawdź wiek zamówienia
    val createdAtMs = runCatching { 
        Instant.parse(order.createdAt).toEpochMilli() 
    }.getOrNull() ?: return  // Brak createdAt → odrzuć
    
    val ageMinutes = (System.currentTimeMillis() - createdAtMs) / 60000
    
    if (ageMinutes > 10) {
        Timber.tag(TAG).d("⏭️ Pomijam stare zamówienie (${ageMinutes}min): ${order.orderNumber}")
        return
    }
    
    // ... reszta logiki
}
```

### ✅ **POPRAWKA #4: Persystencja lastClosedOrderId**
```kotlin
// OrdersViewModel.kt
private val closedOrderIds = mutableSetOf<String>()  // Zamiast single ID

init {
    viewModelScope.launch {
        val stored = appPreferencesManager.getClosedOrderIds()
        closedOrderIds.addAll(stored)
    }
}

fun dismissDialog() {
    val closedId = _uiState.value.orderToShowInDialog?.orderId
    if (closedId != null) {
        closedOrderIds.add(closedId)
        viewModelScope.launch {
            appPreferencesManager.addClosedOrderId(closedId)
        }
    }
}

// W observePendingOrdersQueue():
val next = queue.firstOrNull { it.orderId !in closedOrderIds }
```

---

## 📊 PODSUMOWANIE

### **Najbardziej prawdopodobne przyczyny (ranking):**

1. ⭐⭐⭐⭐⭐ **Brak odczytu `SavedStateHandle` przy init** (95%)
2. ⭐⭐⭐⭐ **Synchronizacja nadpisuje lokalny status** (85%)
3. ⭐⭐⭐⭐ **WebSocket wysyła duplikaty/stare eventy** (75%)
4. ⭐⭐⭐ **`lastClosedOrderId` nie jest persystowane** (70%)
5. ⭐⭐ **Duplikacja handlerów WebSocket** (40%)

### **Kluczowe pliki wymagające zmian:**
- `OrdersViewModel.kt` - init, observePendingOrdersQueue, dismissDialog
- `OrdersRepository.kt` - syncLocalDatabaseWithRemote (inteligentny merge)
- `SocketStaffEventsHandler.kt` - filtrowanie starych eventów
- `AppPreferencesManager.kt` - persystencja closedOrderIds

### **Najważniejsze logi do dodania:**
- `DIALOG_PERSISTENCE` - śledzenie stanu dialogu
- `SYNC_CONFLICT` - konflikty podczas synchronizacji
- `SOCKET_EVENT` - wiek zamówień z WebSocket
- `QUEUE_FILTER` - dlaczego zamówienie trafia do kolejki

---

## ⚠️ UWAGA: NIE IMPLEMENTUJ NA ŚLEPO

Przed implementacją poprawek:
1. ✅ Dodaj wszystkie logi diagnostyczne
2. ✅ Wykonaj wszystkie 4 testy debugowania
3. ✅ Zbierz logi i potwierdź konkretną przyczynę
4. ⚠️ Implementuj poprawkę **TYLKO** dla potwierdzonego problemu
5. ✅ Testuj każdą poprawkę osobno

**Implementacja "na ślepo" może wprowadzić nowe błędy!**

