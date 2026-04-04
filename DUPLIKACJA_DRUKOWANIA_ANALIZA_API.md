# 🔴 DUPLIKACJA DRUKOWANIA - ANALIZA LOGÓW API

## Data analizy: 2026-02-13

---

## 🎯 PROBLEM

Po zaakceptowaniu zamówienia **zewnętrznie** (przez inną aplikację), zamówienie drukuje się **2 razy** na każdej drukarce.

**Ustawienia użytkownika:**
- ✅ Automatyczne drukowanie po zaakceptowaniu: `TRUE`
- ❌ Automatyczne drukowanie: `FALSE`
- ✅ Auto-drukuj zamówienia na miejscu (DINE_IN) - drukarka kuchenna: `TRUE`
- ✅ Drukowanie na kuchni: `TRUE`

---

## 🔍 ANALIZA LOGÓW API

### Zamówienie: `698f0539fdf9812564734b12`

#### ⏰ Timeline zdarzeń:

```
11:04:25.688 - Scheduled reminder check (90s)
11:04:28.401 - [Inventory] Processing ORDER_ACCEPTED (doc: 698f053c79b4a971d62486b4)
11:04:28.402 - ✅ EMIT #162: ORDER_ACCEPTED → tenant:krakow_pointsushi_pl
11:04:28.418 - ORDER_STATUS_SYNC (ACCEPTED)
11:04:28.471 - [Inventory] Successfully processed ORDER_ACCEPTED

⏱️ OPÓŹNIENIE: ~2 sekundy

11:04:30.170 - New outbox event: ORDER_ACCEPTED (doc: 698f053e2732a81602f4a8ba) ⚠️ NOWY DOKUMENT!
11:04:30.171 - ✅ EMIT #163: ORDER_ACCEPTED → tenant:krakow_pointsushi_pl ⚠️ DUPLIKAT!
11:04:30.172 - [Inventory] Processing ORDER_ACCEPTED (doc: 698f053e2732a81602f4a8ba)
11:04:30.240 - [Inventory] Successfully processed ORDER_ACCEPTED
```

---

## 🚨 GŁÓWNA PRZYCZYNA: BACKEND EMITUJE DUPLIKATY

### Problem po stronie API:

**Backend tworzy DWA różne dokumenty outbox dla tego samego eventu ORDER_ACCEPTED:**

1. **Pierwszy dokument:** `698f053c79b4a971d62486b4` → EMIT #162 (11:04:28.402)
2. **Drugi dokument:** `698f053e2732a81602f4a8ba` → EMIT #163 (11:04:30.171)

**Oba dokumenty dotyczą tego samego zamówienia i tego samego eventu!**

---

## 📊 SEKWENCJA ZDARZEŃ W APLIKACJI ANDROID

### Gdy aplikacja odbiera oba eventy przez WebSocket:

#### **Pierwszy ORDER_ACCEPTED (11:04:28.402):**
```
1. socketEventsRepo.statuses emituje: OrderStatusChange(orderId, ACCEPTED)
2. observeSocketEvents() → distinctUntilChanged + debounce(500ms)
3. wasPrintedSync(orderId) = FALSE → oznacz jako wydrukowane
4. printAfterOrderAccepted() → DRUKOWANIE #1 ✅
```

#### **Drugi ORDER_ACCEPTED (11:04:30.171):**
```
1. socketEventsRepo.statuses emituje: OrderStatusChange(orderId, ACCEPTED) 
2. observeSocketEvents() → distinctUntilChanged + debounce(500ms)
3. ❌ PROBLEM: To NOWY event z NOWYM timestampem!
4. distinctUntilChanged sprawdza: old.orderId == new.orderId && old.newStatus == new.newStatus
5. orderId i status są TAKIE SAME, ALE timestamp/event jest INNY
6. ⚠️ Event przechodzi przez filtr!
7. wasPrintedSync(orderId) = TRUE → powinien się zatrzymać... 
8. ALE: Jeśli debounce zgrupował oba eventy, mechanizm deduplikacji może zawieść
9. printAfterOrderAccepted() → DRUKOWANIE #2 ❌ DUPLIKAT
```

---

## 🧪 DLACZEGO ZABEZPIECZENIA NIE DZIAŁAJĄ?

### 1. **distinctUntilChanged** - NIE WYSTARCZY

Kod w `OrdersViewModel.kt:1500-1501`:
```kotlin
socketEventsRepo.statuses
    .distinctUntilChanged { old, new -> old.orderId == new.orderId && old.newStatus == new.newStatus }
```

**Problem:** To filtruje tylko KOLEJNE identyczne wartości. Jeśli między emitami jest minimalne opóźnienie lub inny event, `distinctUntilChanged` ich nie złapie jako duplikaty.

### 2. **debounce(500ms)** - POMAGA ALE NIE ROZWIĄZUJE

```kotlin
.debounce(500ms)
```

**Analiza:**
- Pierwszy event: 11:04:28.402
- Drugi event: 11:04:30.171
- **Różnica: 1769ms** (1.77 sekundy)

❌ **Opóźnienie większe niż debounce → oba eventy przechodzą!**

### 3. **wasPrintedSync + markAsPrintedSync** - DZIAŁA CZĘŚCIOWO

```kotlin
if (wasPrintedSync(s.orderId)) {
    return@onEach
}
markAsPrintedSync(s.orderId)
```

**To POWINNO działać**, ale:
- Jeśli `debounce` nie zgrupował eventów (opóźnienie > 500ms)
- Oba eventy są przetwarzane SEKWENCYJNIE
- Race condition może wystąpić jeśli:
  - Pierwszy event jeszcze nie oznaczył jako printed
  - Drugi event już przeszedł przez wasPrintedSync

---

## 🔧 PRZYCZYNY TECHNICZNE

### Backend (API):
1. ✅ **Podwójne tworzenie dokumentu outbox** - prawdopodobnie:
   - Event ORDER_ACCEPTED jest tworzony przez dwa różne handlery
   - Lub mechanizm retry tworzy duplikat
   - Lub race condition w systemie eventów

### Frontend (Android):
1. ⚠️ **debounce za krótkie** - 500ms nie pokrywa opóźnienia 1.77s między duplikatami
2. ⚠️ **distinctUntilChanged nie rozpoznaje duplikatów** - sprawdza tylko kolejne wartości
3. ⚠️ **wasPrintedSync może zawieść** przy race condition

---

## ✅ ROZWIĄZANIA

### 🎯 NAJLEPSZE ROZWIĄZANIE: **NAPRAW BACKEND**

**Backend NIE POWINIEN emitować duplikatów ORDER_ACCEPTED!**

Sprawdź w kodzie API:
- `app/src/modules/websocket/outbox-processor.ts` lub podobny
- `app/src/modules/orders/events/` - handlery eventów ORDER_ACCEPTED
- System kolejkowania eventów (outbox pattern)

**Zidentyfikuj dlaczego powstają dwa dokumenty outbox dla tego samego eventu.**

---

### 🛡️ ROZWIĄZANIE TYMCZASOWE: **WZMOCNIJ DEDUPLIKACJĘ W ANDROID**

#### Opcja 1: **Zwiększ debounce do 2-3 sekund**

```kotlin
socketEventsRepo.statuses
    .distinctUntilChanged { old, new -> old.orderId == new.orderId && old.newStatus == new.newStatus }
    .debounce(2000) // 2 sekundy - pokrywa opóźnienia do 2s
```

**Plusy:** Prosty fix, skuteczny dla większości przypadków
**Minusy:** Opóźnia reakcję UI o 2s

---

#### Opcja 2: **Zapamiętaj ostatnie eventy w oknie czasowym**

Dodaj do `OrdersViewModel`:

```kotlin
// Okno czasowe dla deduplikacji eventów
private val recentPrintEvents = mutableMapOf<String, Long>() // orderId -> timestamp
private val printEventWindowMs = 5000L // 5 sekund

private fun shouldAllowPrint(orderId: String): Boolean {
    val now = System.currentTimeMillis()
    val lastPrint = recentPrintEvents[orderId]
    
    return if (lastPrint == null || (now - lastPrint) > printEventWindowMs) {
        recentPrintEvents[orderId] = now
        true
    } else {
        Timber.tag("DEDUPLICATION").w("⏭️ Blokuję drukowanie - event w oknie ${printEventWindowMs}ms: $orderId")
        false
    }
}
```

Użyj w obsłudze statusów:

```kotlin
if (s.newStatus == OrderStatusEnum.ACCEPTED) {
    if (!shouldAllowPrint(s.orderId)) {
        return@onEach
    }
    
    if (wasPrintedSync(s.orderId)) {
        return@onEach
    }
    // ... reszta kodu
}
```

**Plusy:** Bardzo skuteczne, blokuje duplikaty w oknie czasowym
**Minusy:** Więcej kodu, trzeba zarządzać czyszczeniem mapy

---

#### Opcja 3: **Dodaj ID eventu z backendu**

Jeśli backend wysyła unique `eventId` w payload:

```kotlin
data class OrderStatusChange(
    val orderId: String,
    val newStatus: OrderStatusEnum,
    val eventId: String? = null // Dodaj to
)
```

Deduplikuj po `eventId`:

```kotlin
private val processedEventIds = mutableSetOf<String>()

socketEventsRepo.statuses
    .filter { event ->
        event.eventId?.let { id ->
            if (processedEventIds.contains(id)) {
                Timber.tag("DEDUPLICATION").w("⏭️ Event już przetworzony: $id")
                false
            } else {
                processedEventIds.add(id)
                true
            }
        } ?: true // Jeśli brak eventId, przepuść
    }
```

**Plusy:** Najbardziej niezawodne, wykorzystuje dane z backendu
**Minusy:** Wymaga zmian w API i modelu danych

---

## 📋 REKOMENDACJE

### 1. **PILNE (Backend):**
- [ ] Zbadaj kod API który tworzy dokumenty outbox dla ORDER_ACCEPTED
- [ ] Zidentyfikuj dlaczego powstają dwa dokumenty
- [ ] Napraw mechanizm outbox pattern aby unikał duplikatów

### 2. **TYMCZASOWE (Android):**
- [ ] Zwiększ debounce z 500ms → 2000ms (natychmiastowy fix)
- [ ] Dodaj okno czasowe deduplikacji (opcja 2)

### 3. **DŁUGOTERMINOWE:**
- [ ] Dodaj `eventId` w payload WebSocket
- [ ] Implementuj pełną deduplikację po `eventId`
- [ ] Dodaj monitoring duplikatów (metrics)

---

## 🧪 JAK ZWERYFIKOWAĆ FIX?

### Test 1: **Zwiększony debounce**
1. Zmień debounce na 2000ms
2. Zaakceptuj zamówienie zewnętrznie
3. Sprawdź logi - powinien być tylko JEDEN log drukowania

### Test 2: **Okno czasowe**
1. Zaimplementuj `shouldAllowPrint()`
2. Zaakceptuj zamówienie zewnętrznie
3. W logach szukaj: `"⏭️ Blokuję drukowanie - event w oknie"`

### Test 3: **Backend fix**
1. Napraw backend
2. Sprawdź logi API - powinien być tylko JEDEN `emitCount` dla ORDER_ACCEPTED
3. Testuj aplikację - drukowanie 1x

---

## 📊 PODSUMOWANIE

| Typ akceptacji | Oczekiwane drukowanie | Rzeczywiste | Status |
|----------------|----------------------|-------------|--------|
| Manualna (w aplikacji) | 1x na drukarce | 2x | ❌ BUG |
| Zewnętrzna (inna app) | 1x na drukarce | 2x | ❌ BUG |

**Główny problem:** Backend emituje duplikaty ORDER_ACCEPTED

**Najlepszy fix:** Napraw backend

**Tymczasowy fix:** Zwiększ debounce + dodaj okno czasowe deduplikacji

---

## 🔗 POWIĄZANE PLIKI

- `L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\order\OrdersViewModel.kt:1500-1570`
- `L:\SHOP APP\app\src\main\java\com\itsorderchat\service\SocketStaffEventsHandler.kt:384-450`
- Backend API: `modules/websocket/outbox-processor.ts` (przypuszczalnie)

---

**Autor analizy:** GitHub Copilot  
**Data:** 2026-02-13

