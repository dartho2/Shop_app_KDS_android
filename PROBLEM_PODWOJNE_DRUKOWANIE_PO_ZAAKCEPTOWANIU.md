# 🐛 PROBLEM: Podwójne drukowanie po zaakceptowaniu zamówienia

## Data: 2026-02-13 15:51

---

## 📋 OPIS PROBLEMU

**Objaw:** Po zaakceptowaniu zamówienia przez **zewnętrzną aplikację** (nie manualnie), zamówienie drukuje się **2 RAZY** na tej samej drukarce.

**Logi pokazujące problem:**

```
15:51:47.481  DEDUPLICATION: ✅ Dozwolony event drukowania: 698f3a61fdf9812564737a8a (PIERWSZY)
15:51:47.492  DEDUPLICATION: ✅ Dozwolony event drukowania: 698f3a61fdf9812564737a8a (DRUGI - 11ms później!)

15:51:47.506  AUTO_PRINT_STATUS_CHANGE: 🔔 Status zmieniony na ACCEPTED (PIERWSZY)
15:51:47.518  AUTO_PRINT_STATUS_CHANGE: 🔔 Status zmieniony na ACCEPTED (DRUGI - 12ms później!)

15:51:47.603  PRINT_DEBUG: 📋 printAfterOrderAccepted START (PIERWSZY)
15:51:47.746  PRINT_DEBUG: 📋 printAfterOrderAccepted START (DRUGI - 143ms później!)

[Drukowanie 1389ms]
15:51:49.728  PRINT_DEBUG: ✅ Drukowanie standardowe zakończone

15:51:49.732  PRINT_DEBUG: 🖨️ WYBRANO: Drukowanie tylko na drukarce głównej (ZNOWU!)
[Drukowanie 1149ms]
15:51:51.609  PRINT_DEBUG: ✅ Drukowanie standardowe zakończone (ZNOWU!)
```

---

## 🔍 ANALIZA PRZYCZYNY

### 1. Flow emituje DWA eventy

`socketEventsRepo.statuses` emituje **DWA identyczne eventy** w odstępie **11-12ms**.

### 2. `distinctUntilChanged()` NIE blokuje

```kotlin
socketEventsRepo.statuses
    .distinctUntilChanged { old, new -> 
        old.orderId == new.orderId && old.newStatus == new.newStatus 
    }
```

**Problem:** `distinctUntilChanged()` blokuje tylko **kolejne IDENTYCZNE** emisje, ale jeśli:
1. Event A (orderId=X, status=ACCEPTED)
2. Event B (orderId=X, status=ACCEPTED) - **11ms później**

To oba przechodzą przez `distinctUntilChanged()` bo porównuje **poprzedni** z **current**, a między nimi może być niewielka przerwa.

### 3. `debounce(500)` NIE pomaga

```kotlin
.debounce(500) // Czekaj 500ms aby zgrupować wielokrotne emisje
```

**Jak działa debounce:**
- Czeka 500ms **PRZED** emisją
- Jeśli w tym czasie przyjdzie kolejny event → **resetuje timer**

**Problem:**
- Event 1 (T=0ms) → czeka 500ms
- Event 2 (T=12ms) → resetuje timer, czeka 500ms
- Po 512ms → emituje Event 2
- **ALE** Event 1 może już zostać wyemitowany wcześniej jeśli flow jest aktywny!

### 4. Deduplikacja `shouldAllowPrintEvent()` przepuszcza OBA

```kotlin
private fun shouldAllowPrintEvent(orderId: String): Boolean {
    val now = System.currentTimeMillis()
    synchronized(recentPrintEvents) {
        val lastPrintTime = recentPrintEvents[orderId]
        
        return if (lastPrintTime == null || (now - lastPrintTime) > 3000) {
            recentPrintEvents[orderId] = now
            true  // Dozwolone
        } else {
            false // Zablokowane
        }
    }
}
```

**Dlaczego przepuszcza OBA eventy?**

Timing:
```
T=0ms:    Event 1 → shouldAllowPrintEvent() → lastPrintTime=NULL → dozwolone → zapisz timestamp=T0
T=11ms:   Event 2 → shouldAllowPrintEvent() → lastPrintTime=T0 → elapsed=11ms < 3000ms → POWINIEN ZABLOKOWAĆ!
```

**ALE** w logach widzimy:
```
15:51:47.481  DEDUPLICATION: ✅ Dozwolony (PIERWSZY)
15:51:47.492  DEDUPLICATION: ✅ Dozwolony (DRUGI - 11ms później!) ← DLACZEGO?!
```

**Możliwe przyczyny:**
1. **Race condition** - dwa wątki wywołują `shouldAllowPrintEvent()` jednocześnie, oba widzą `lastPrintTime=NULL`
2. **Synchronized nie działa w coroutines** - `synchronized` blokuje tylko wątki systemowe, nie coroutines
3. **Wywołania z różnych Dispatchers** - Main vs IO

---

## ✅ ROZWIĄZANIE ZAIMPLEMENTOWANE (v1)

### Zmiana 1: Usunięcie sprawdzania `manuallyAcceptedOrders`

**Przed:**
```kotlin
if (manuallyAcceptedOrders.contains(s.orderId)) {
    Timber.tag("AUTO_PRINT_STATUS_CHANGE").d("⏭️ Pomijam - manualne")
    return@onEach
}
```

**Po:**
```kotlin
// Usunięte - powodowało problemy z synchronizacją
```

**Powód:** Ta flaga była używana do blokowania drukowania po manualnej akceptacji, ale powodowała więcej problemów niż korzyści.

### Zmiana 2: Przeniesienie deduplikacji PRZED `viewModelScope.launch`

**Przed:**
```kotlin
if (s.newStatus == OrderStatusEnum.ACCEPTED) {
    viewModelScope.launch {
        if (!shouldAllowPrintEvent(s.orderId)) return@launch
        // ...drukowanie...
    }
}
```

**Po:**
```kotlin
if (s.newStatus == OrderStatusEnum.ACCEPTED) {
    // ✅ Sprawdź SYNCHRONICZNIE (przed launch)
    if (!shouldAllowPrintEvent(s.orderId)) return@onEach
    
    if (wasPrintedSync(s.orderId)) return@onEach
    markAsPrintedSync(s.orderId)
    
    viewModelScope.launch {
        // ...drukowanie...
    }
}
```

**Korzyść:** Wszystkie sprawdzenia wykonują się **PRZED** utworzeniem coroutine, zapobiegając race condition.

---

## ⚠️ DLACZEGO TO NADAL NIE DZIAŁA?

Powyższe zmiany **NIE rozwiązały problemu** bo:

1. **Flow nadal emituje DWA eventy** w odstępie 11ms
2. **`.distinctUntilChanged()` i `.debounce(500)` nie blokują** duplikatów
3. **`synchronized` nie działa poprawnie z coroutines**

---

## 🎯 OSTATECZNE ROZWIĄZANIE (v2 - DO ZAIMPLEMENTOWANIA)

### Opcja A: Zmiana `debounce()` na `throttleFirst()`

```kotlin
socketEventsRepo.statuses
    .distinctUntilChanged { old, new -> 
        old.orderId == new.orderId && old.newStatus == new.newStatus 
    }
    .throttleFirst(1000) // ✅ Emituj pierwszy event, ignoruj kolejne przez 1s
    .onEach { s ->
        // ...
    }
```

**`throttleFirst()`:**
- Emituje **PIERWSZY** event
- Ignoruje wszystkie kolejne przez określony czas (1s)
- Idealny do blokowania duplikatów

### Opcja B: Dodanie `distinctBy { it.orderId }` PO debounce

```kotlin
socketEventsRepo.statuses
    .distinctUntilChanged { old, new -> 
        old.orderId == new.orderId && old.newStatus == new.newStatus 
    }
    .debounce(500)
    .distinctBy { "${it.orderId}_${it.newStatus}" } // ✅ Dodatkowa deduplikacja
    .onEach { s ->
        // ...
    }
```

### Opcja C: Zmiana `synchronized` na `Mutex` (CO ZROBILIŚMY)

**Problem:** `synchronized` nie gwarantuje thread-safety w coroutines!

**Rozwiązanie:**
```kotlin
private val recentPrintMutex = Mutex() // ✅ Już istnieje w kodzie

private suspend fun shouldAllowPrintEvent(orderId: String): Boolean {
    val now = System.currentTimeMillis()

    return recentPrintMutex.withLock {
        val lastPrintTime = recentPrintEvents[orderId]

        if (lastPrintTime == null || (now - lastPrintTime) > printEventWindowMs) {
            recentPrintEvents[orderId] = now
            true
        } else {
            false
        }
    }
}
```

**Zmiana:** Funkcja z `fun` → `suspend fun` i użycie `Mutex.withLock()` zamiast `synchronized`.

---

## 📊 PLAN NAPRAWY - KROK PO KROKU

### ✅ Krok 1: Zmienić `shouldAllowPrintEvent()` na suspend function z Mutex

**Status:** ZAIMPLEMENTOWANE (ale nie zbudowane z powodu błędu R.jar)

### ⏳ Krok 2: Dodać `throttleFirst()` lub zmniejszyć `debounce()`

**Opcja 1:** Zmień `debounce(500)` → `debounce(100)` (szybsza reakcja)

**Opcja 2:** Dodaj Kotlinx Coroutines Flow Extensions i użyj `throttleFirst(1000)`

### ⏳ Krok 3: Dodać dodatkową deduplikację w `printAfterOrderAccepted()`

```kotlin
private val printAfterAcceptedMutex = Mutex()
private val recentPrintAfterAccepted = mutableSetOf<String>()

suspend fun printAfterOrderAccepted(order: Order) {
    // ✅ Blokada ponownego wywołania dla tego samego orderId
    if (!printAfterAcceptedMutex.withLock {
        if (recentPrintAfterAccepted.contains(order.orderId)) {
            false // Zablokowane
        } else {
            recentPrintAfterAccepted.add(order.orderId)
            true // Dozwolone
        }
    }) {
        Timber.w("⏭️ printAfterOrderAccepted już w toku dla ${order.orderNumber}")
        return
    }

    try {
        // ...normalne drukowanie...
    } finally {
        // Usuń z setu po 5 sekundach
        delay(5000)
        printAfterAcceptedMutex.withLock {
            recentPrintAfterAccepted.remove(order.orderId)
        }
    }
}
```

---

## 🔧 CO ZROBIĆ TERAZ?

1. ✅ Zainstaluj nową wersję APK (z usuniętym `manuallyAcceptedOrders`)
2. ⏳ Przetestuj czy problem nadal występuje
3. ⏳ Jeśli TAK → zaimplementuj Opcję C (suspend shouldAllowPrintEvent)
4. ⏳ Jeśli NADAL problem → dodaj `throttleFirst()` do flow

---

## 📝 PLIKI DO EDYCJI

| Plik | Zmiany |
|------|--------|
| `OrdersViewModel.kt` | ✅ Usunięto `manuallyAcceptedOrders` check |
| `OrdersViewModel.kt` | ⏳ Zmienić `shouldAllowPrintEvent()` na suspend |
| `OrdersViewModel.kt` | ⏳ Dodać throttleFirst lub zmniejszyć debounce |
| `PrinterService.kt` | ⏳ Dodać deduplikację w `printAfterOrderAccepted()` |

---

## 🎯 OCZEKIWANY REZULTAT

Po naprawie logi powinny wyglądać:

```
15:51:47.481  DEDUPLICATION: ✅ Dozwolony event drukowania: 698f3a61fdf9812564737a8a
15:51:47.492  DEDUPLICATION: ⏭️ Zablokowany duplikat! elapsed=11ms (okno=3000ms)

15:51:47.506  AUTO_PRINT_STATUS_CHANGE: 🔔 Status zmieniony na ACCEPTED
(BRAK drugiego wywołania!)

15:51:47.603  PRINT_DEBUG: 📋 printAfterOrderAccepted START
[Drukowanie 1389ms]
15:51:49.728  PRINT_DEBUG: ✅ Drukowanie standardowe zakończone

(BRAK drugiego drukowania!)
```

---

**Autor:** GitHub Copilot  
**Data:** 2026-02-13  
**Status:** W TRAKCIE NAPRAWY  
**Priorytet:** 🔴 HIGH

