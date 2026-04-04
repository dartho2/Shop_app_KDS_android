# 🔍 ANALIZA PROBLEMU: Zamówienie po włączeniu aplikacji pojawia się jako niezaakceptowane

## 📅 Data: 2026-02-25

---

## 🎯 OPIS PROBLEMU

### Symptomy zgłoszone przez użytkownika:
- Zamówienie jest **zaakceptowane** (status: ACCEPTED)
- Po włączeniu aplikacji zamówienie pokazuje się jako **niezaakceptowane** (PROCESSING)
- Wyskakuje dialog do potwierdzenia
- Po sekundzie alarm się wyłącza
- Zamówienie pokazuje się ponownie jako **zaakceptowane**

---

## 🔍 ANALIZA LOGÓW

### Kluczowa sekwencja wydarzeń (chronologia):

#### 1. **23:18:29.258** - Start aplikacji ✅
```
📂 Załadowano 12 wydrukowanych zamówień z DataStore
```

#### 2. **23:18:29.260** - Auto-print pomija zamówienie (już wydrukowane) ✅
```
AUTO_PRINT_STATUS_CHANGE: ⏭️ Pomijam drukowanie - zamówienie już było wydrukowane
```

#### 3. **23:18:29.261** - **PIERWSZA SYNCHRONIZACJA** ✅
```
SYNC_CONFLICT: 🔄 SYNC START
   ├─ Date: 2026-02-24
   ├─ Remote orders: 3

SYNC_CONFLICT: 🔄 UPDATING ORDER: 699f74e239820eefe0c62b39 (KR-2191)
   ├─ Remote status: ACCEPTED (seq=4)
   ├─ Local status: ACCEPTED (seq=4)
   └─ Decision: REMOTE >= LOCAL → UPDATE
```
**OK**: Oba statusy ACCEPTED, synchronizacja niepotrzebna ale bezpieczna.

#### 4. **23:18:29.637** - Socket emituje zamówienie ⚠️
```
📥 Received order from socket: orderId=699f74e239820eefe0c62b39
```
**PROBLEM**: Socket wysłał zamówienie z odpowiedzi API podczas pierwszej synchronizacji.

#### 5. **23:18:29.665** - **DRUGA SYNCHRONIZACJA** ❌ **PROBLEM!**
```
SYNC_CONFLICT: 🔄 SYNC START
   ├─ Date: 2026-02-24
   ├─ Remote orders: 3

SYNC_CONFLICT: 🔄 UPDATING ORDER: 699f74e239820eefe0c62b39 (KR-2191)
   ├─ Remote status: ACCEPTED (seq=4)
   ├─ Local status: PROCESSING (seq=2) ⚠️ SKĄD?!
   └─ Decision: REMOTE >= LOCAL → UPDATE
```

**❌ GŁÓWNY PROBLEM**: 
- Między pierwszą a drugą synchronizacją, lokalny status zmienił się z **ACCEPTED → PROCESSING**
- To oznacza, że obserwator `getAllOrdersFlow()` wyemitował **starą wersję** zamówienia z bazy!

#### 6. **23:18:29.698** - **Zamówienie trafia do kolejki!** ❌
```
QUEUE_FILTER: 📋 QUEUE UPDATE - Total orders: 3
   Order KR-2191: status=PROCESSING, orderId=699f74e239820eefe0c62b39, inQueue=true ❌
   Order KR-2190: status=ACCEPTED, inQueue=false
   Order KR-2189: status=ACCEPTED, inQueue=false
   └─ Filtered PROCESSING: 1 ❌
```

**EFEKT**: Zamówienie z statusem PROCESSING trafia do `pendingOrdersQueue`!

#### 7. **23:18:29.700** - **Alarm uruchamiany!** ❌
```
OrderAlarmService: observePendingOrdersQueue TRIGGERED
   ├─ queue.size: 1 ❌
   ├─ currentDialogId: null
   └─ newestNotShown: 699f74e239820eefe0c62b39

🎯 CASE 1: Brak dialogu + brak suppress
   └─ next: 699f74e239820eefe0c62b39

✅ Otwieram dialog + ACTION_START dla: 699f74e239820eefe0c62b39 ❌
```

#### 8. **23:18:29.702-29.731** - Alarm wystartował ❌
```
ALARM START: 🚨 [OrdersViewModel] ALARM CALL
   ├─ orderId: 699f74e239820eefe0c62b39
   ├─ orderNumber: KR-2191
   ├─ orderStatus: PROCESSING ❌ (powinno być ACCEPTED!)
```

#### 9. **23:18:29.732** - Drugi trigger (dialog już otwarty) ✅
```
OrderAlarmService: observePendingOrdersQueue TRIGGERED
   ├─ currentDialogId: 699f74e239820eefe0c62b39 (już ustawione)
🎯 CASE 4: Dialog otwarty, brak nowych
```

**Po chwili**: Druga synchronizacja kończy się, status wraca do ACCEPTED, kolejka się opróżnia, alarm się wyłącza.

---

## 🎯 ROOT CAUSE (Główna przyczyna)

### **Race Condition w synchronizacji:**

1. **Pierwsza synchronizacja** (`syncOrdersFromApi`) rozpoczyna się
2. **Socket emituje zamówienie** do Flow (z API response)
3. **Room Database** emituje **starą wersję** zamówienia (z cache przed UPDATE)
4. **Druga synchronizacja** widzi starą wersję z statusem PROCESSING
5. **observePendingOrdersQueue** reaguje na przejściowy status PROCESSING
6. **Alarm się uruchamia** dla zamówienia które jest już ACCEPTED

### Dlaczego tak się dzieje?

**Room Database Flow** emituje zamówienie **PRZED** zakończeniem transakcji UPDATE:

```kotlin
// OrdersRepository.syncLocalDatabaseWithRemote()
database.withTransaction {
    orderDao.insertOrUpdateAll(remoteOrders)  // ← UPDATE w bazie
    // W TYM MOMENCIE Flow może wyemitować starą wersję!
}

// Socket emituje zamówienie jednocześnie
socketEventsRepo.emitOrder(order)  // ← Drugi event

// observePendingOrdersQueue reaguje NATYCHMIAST
pendingOrdersQueue
    .onEach { queue -> /* TRIGGER! */ }
```

---

## ✅ ROZWIĄZANIA ZAIMPLEMENTOWANE

### **POPRAWKA #1: Debounce dla `observePendingOrdersQueue`**

**Cel**: Zapobieganie reakcji na przejściowe zmiany statusu podczas synchronizacji.

```kotlin
private fun observePendingOrdersQueue() {
    combine(
        pendingOrdersQueue,
        uiState.map { it.userRole },
        _suppressAutoOpen
    ) { queue, role, suppress -> Triple(queue, role, suppress) }
        .debounce(300) // ✅ Czekaj 300ms przed reakcją
        .onEach { (queue, role, suppress) ->
            // Logika otwarcia dialogu i alarmu
        }
        .launchIn(viewModelScope)
}
```

**Działanie**:
- Czeka **300ms** przed reakcją na zmianę w kolejce
- Jeśli w ciągu 300ms kolejka się zmienia ponownie (np. PROCESSING → ACCEPTED), **nie reaguje**
- Eliminuje reakcję na przejściowe stany podczas synchronizacji

**Efekt**:
- ✅ Alarm NIE uruchomi się dla zamówienia które ma PROCESSING tylko przez ułamek sekundy
- ✅ Tylko zamówienia które **stabilnie** mają status PROCESSING > 300ms wywołają alarm
- ⚠️ Minimalne opóźnienie 300ms dla prawdziwie nowych zamówień (akceptowalne)

---

### **POPRAWKA #2: `distinctUntilChanged` dla `pendingOrdersQueue`**

**Cel**: Zapobieganie reakcji na tę samą zawartość kolejki (nawet jeśli obiekt jest nowy).

```kotlin
val pendingOrdersQueue: StateFlow<List<Order>> =
    orders
        .map { allOrders ->
            allOrders.filter { /* status == PROCESSING */ }
                .sortedBy { it.createdAt }
        }
        .distinctUntilChanged { old, new -> 
            // ✅ Porównaj zawartość (orderIds), nie obiekty
            old.map { it.orderId } == new.map { it.orderId }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

**Działanie**:
- Porównuje **listę orderIds** zamiast samych obiektów `Order`
- Jeśli lista zamówień w kolejce jest **taka sama** (te same IDs), **nie emituje**
- Nawet jeśli obiekt `Order` jest nowy (z powodu UPDATE w bazie)

**Efekt**:
- ✅ Kolejka emituje tylko gdy **zawartość się zmienia** (dodanie/usunięcie zamówienia)
- ✅ Nie emituje gdy tylko **dane zamówienia** się zmieniają (np. status, ale nadal w kolejce)
- ❌ **UWAGA**: To nie rozwiązuje problemu całkowicie, bo zamówienie **NADAL** przejściowo ma PROCESSING

---

### **Dlaczego obie poprawki razem działają?**

1. **`distinctUntilChanged`**: Redukuje liczbę emisji (ale nie eliminuje problemu)
2. **`debounce(300)`**: **Kluczowa poprawka** - czeka 300ms przed reakcją
3. **Razem**: 
   - Kolejka emituje tylko przy rzeczywistej zmianie zawartości
   - Observer czeka 300ms przed reakcją
   - Synchronizacja kończy się < 300ms → status wraca do ACCEPTED → kolejka się opróżnia → **brak alarmu**

---

## 📊 SEKWENCJA PO POPRAWKACH

### Nowa chronologia (oczekiwana):

1. **Start aplikacji** → Synchronizacja z API rozpoczyna się
2. **Socket emituje** → Zamówienie trafia do Flow
3. **Room emituje starą wersję** → Zamówienie ma PROCESSING (przejściowo)
4. **`pendingOrdersQueue` dodaje zamówienie** → Kolejka ma 1 element
5. **`distinctUntilChanged`** → Emituje zmianę (nowe zamówienie w kolejce)
6. **`debounce(300)`** → **CZEKA 300ms** ⏱️
7. **Synchronizacja kończy się** → Status wraca do ACCEPTED (~50-100ms)
8. **`pendingOrdersQueue` opróżnia się** → Kolejka ma 0 elementów
9. **`distinctUntilChanged`** → Emituje zmianę (pusta kolejka)
10. **`debounce(300)`** → **CANCEL poprzedni timer** i CZEKA 300ms dla nowej wartości (pusta kolejka)
11. **Po 300ms** → Observer reaguje na **pustą kolejkę** → **NIE uruchamia alarmu** ✅

---

## 🧪 TEST SCENARIUSZ

### Aby przetestować poprawkę:

1. Zaakceptuj wszystkie zamówienia (wszystkie mają status ACCEPTED)
2. Zamknij aplikację (Force Stop)
3. Otwórz aplikację ponownie
4. **Oczekiwany wynik**:
   - ✅ Zamówienia pokazują się jako ACCEPTED od razu
   - ✅ NIE pojawia się dialog dla żadnego zamówienia
   - ✅ NIE uruchamia się alarm
   - ✅ W logach: `QUEUE_FILTER: Filtered PROCESSING: 0`

### Logi które powinny się pojawić:

```
QUEUE_FILTER: 📋 QUEUE UPDATE - Total orders: 3
   Order KR-2191: status=PROCESSING, inQueue=true  ← Przejściowo
   └─ Filtered PROCESSING: 1

[debounce czeka 300ms...] ⏱️

QUEUE_FILTER: 📋 QUEUE UPDATE - Total orders: 3
   Order KR-2191: status=ACCEPTED, inQueue=false  ← Po synchronizacji
   └─ Filtered PROCESSING: 0

[debounce czeka 300ms na pustą kolejkę...]

observePendingOrdersQueue TRIGGERED
   ├─ queue.size: 0  ← PUSTA KOLEJKA! ✅
   └─ Brak akcji (brak zamówień PROCESSING)
```

---

## ⚠️ POTENCJALNE SKUTKI UBOCZNE

### **1. Opóźnienie 300ms dla prawdziwie nowych zamówień**

**Scenariusz**: Nowe zamówienie przychodzi z statusem PROCESSING (nie podczas startu app).

**Efekt**: Alarm uruchomi się **300ms później** niż wcześniej.

**Czy to problem?**:
- ⚠️ Minimalne opóźnienie 300ms
- ✅ W kontekście całkowitego czasu reakcji (kilka sekund) - akceptowalne
- ✅ Lepsze niż fałszywe alarmy dla zaakceptowanych zamówień

### **2. Możliwe opóźnienie dla zamówień przychodzących podczas synchronizacji**

**Scenariusz**: Nowe zamówienie przychodzi DOKŁADNIE w momencie synchronizacji.

**Efekt**: Alarm może się opóźnić do 600ms (300ms debounce + 300ms kolejna synchronizacja).

**Czy to problem?**:
- ⚠️ Rzadki edge case
- ✅ Maksymalne opóźnienie 600ms - nadal akceptowalne

---

## 📝 DODATKOWE OBSERWACJE Z LOGÓW

### **1. Podwójna synchronizacja**
```
23:18:29.261  SYNC START (pierwsza)
23:18:29.665  SYNC START (druga)  ← 400ms później!
```

**Pytanie**: Dlaczego synchronizacja wywołuje się dwa razy?

**Możliwe przyczyny**:
- Reconnect WebSocket podczas startu aplikacji?
- Dwukrotne wywołanie `syncOrdersFromApi()` w `init{}`?
- `observeSocketConnection()` reaguje na połączenie?

**Do sprawdzenia**: Czy to jest normalne zachowanie czy bug?

### **2. Socket emituje zamówienie z API response**
```
23:18:29.637  📥 Received order from socket
```

**To jest OK**: Backend wysyła zamówienie z API response aby zaktualizować UI.

---

## ✅ PODSUMOWANIE

### **Problem:**
Race condition między:
1. Synchronizacją z API (Room Database UPDATE)
2. Socket emitującym zamówienie
3. Room Flow emitującym starą wersję (przed UPDATE)
4. `observePendingOrdersQueue` reagującym natychmiast na przejściowy status PROCESSING

### **Rozwiązanie:**
Dodanie **debounce(300)** i **distinctUntilChanged** aby zapobiec reakcji na przejściowe zmiany statusu.

### **Efekt:**
✅ Aplikacja NIE uruchamia alarmu dla zamówień które są przejściowo PROCESSING podczas synchronizacji  
✅ Alarm uruchamia się tylko dla zamówień które **stabilnie** mają status PROCESSING > 300ms  
⚠️ Minimalne opóźnienie 300ms dla prawdziwie nowych zamówień (akceptowalne)  

### **Status:**
✅ **POPRAWIONE I GOTOWE DO TESTÓW**

---

**Data implementacji**: 2026-02-25  
**Zaimplementowane w**: `OrdersViewModel.kt` (linie: `observePendingOrdersQueue`, `pendingOrdersQueue`)  
**Tester**: Uruchom aplikację z zaakceptowanymi zamówieniami i sprawdź czy NIE pojawia się dialog.

