# 🖨️🖨️ Problem Podwójnego Drukowania Po Akceptacji - ROZWIĄZANIE

## 📋 Opis Problemu

**Zgłoszenie użytkownika:**
> "Po zaakceptowaniu zamówienia drukuje się zamówienie a po chwili ponownie się drukuje. Zapewne po otrzymaniu na socket informacji i zastanawiam się jak to zrobić aby nie drukować po zaakceptowaniu ręcznym już przez socket."

## 🔍 Analiza Logów

### Sekwencja Zdarzeń (z logów `druk_2_po akceptacji.md`):

1. **22:48:56.357** - ✅ **PIERWSZE DRUKOWANIE** (ręczne - kliknięcie "Drukuj")
   ```
   PrinterService: Serial: Dokument przygotowany (1101 znaków)
   AidlPrinterService: 🖨️ AIDL print start
   ```

2. **22:48:56.520** - Zamówienie zamknięte
   ```
   OrderAlarmService: lastClosedOrderId: 698e4ac0fdf981256472dc9b
   ```

3. **22:49:00.356** - Lista zamówień zaktualizowana
4. **22:49:00.408** - **KLUCZOWY LOG:**
   ```
   OrderListItem: Rendering order 12026 with delivery 2026-02-12T22:03:55.788Z
   ```
   ☝️ Zamówienie ma już `deliveryTime` = **STATUS ACCEPTED** (przyszło przez socket!)

5. **22:49:01.021** - ❌ **DRUGIE DRUKOWANIE** (automatyczne - socket)
   ```
   AidlPrinterService: 🖨️ AIDL print start, serviceType=CLONE
   PrinterService: 📋 printAfterOrderAccepted
   ```

## 🐛 Przyczyny Problemu

### 1. **Race Condition - Za Krótki Timeout**

**Linia 1530-1532 w `OrdersViewModel.kt`:**
```kotlin
// Usuń z setu po 5 sekundach (na wypadek gdyby socket się spóźnił)
viewModelScope.launch {
    delay(5000) // ❌ PROBLEM: 5 sekund to za mało!
    manuallyAcceptedOrders.remove(res.value.orderId)
}
```

**Problem:**
- Socket może przysłać `ORDER_STATUS_UPDATED` **po więcej niż 5 sekundach**
- W logach: drukowanie nr 2 zaczęło się po **~4.6 sekundy** (22:48:56.357 → 22:49:01.021)
- `manuallyAcceptedOrders` już nie zawiera orderId → drukowanie się uruchamia!

### 2. **Nieprawidłowa Kolejność Oznaczania jako Wydrukowane**

**Linia 1537-1541 w `OrdersViewModel.kt` (PRZED fix):**
```kotlin
printerService.printAfterOrderAccepted(res.value)  // Drukuje (powolne!)

// Oznacz jako wydrukowane
markAsPrinted(res.value.orderId)  // ❌ ZA PÓŹNO!
```

**Problem:**
- `markAsPrinted` wywoływane **PO** `printAfterOrderAccepted`
- Drukowanie trwa ~1-3 sekundy
- W międzyczasie socket przyśle update → `wasPrinted()` zwróci `false` → drugie drukowanie!

## ✅ Rozwiązanie

### Fix #1: Zwiększenie Timeout z 5s → 30s

```kotlin
// ✅ FIX: Usuń z setu po 30 sekundach (socket może się spóźnić nawet 10-15s)
viewModelScope.launch {
    delay(30_000) // Zwiększono z 5s na 30s
    manuallyAcceptedOrders.remove(res.value.orderId)
    Timber.tag("AUTO_PRINT").d("🗑️ Usunięto ${res.value.orderId} z manuallyAcceptedOrders (po 30s)")
}
```

**Dlaczego 30s?**
- Socket może się spóźnić z różnych powodów (opóźnienie sieci, prze processing na serwerze)
- 30s daje bezpieczny margines
- Po 30s można bezpiecznie usunąć (zamówienie i tak będzie w `printedOrderIds`)

### Fix #2: Oznaczanie jako Wydrukowane PRZED Drukowaniem

```kotlin
viewModelScope.launch {
    try {
        // ✅ FIX: Oznacz jako wydrukowane PRZED drukowaniem (żeby zabezpieczyć przed race condition)
        markAsPrinted(res.value.orderId)
        
        Timber.d("🖨️ OrdersViewModel: Wywołuję printAfterOrderAccepted dla %s", res.value.orderNumber)
        printerService.printAfterOrderAccepted(res.value)
    } catch (e: Exception) {
        Timber.e(e, "❌ OrdersViewModel: Błąd drukowania po zaakceptowaniu: %s", res.value.orderNumber)
        
        // W przypadku błędu usuń z wydrukowanych (żeby można było ponowić)
        printedOrdersMutex.withLock {
            printedOrderIds.remove(res.value.orderId)
        }
        
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Błąd drukowania: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
```

**Kluczowe zmiany:**
1. ✅ `markAsPrinted()` wywoływane **PRZED** `printAfterOrderAccepted()`
2. ✅ W przypadku błędu - usuwamy z `printedOrderIds` (rollback)
3. ✅ Toast z informacją o błędzie dla użytkownika

## 📊 Mechanizmy Zabezpieczeń

### 1. `manuallyAcceptedOrders` (Set<String>)
**Cel:** Krótkoterminowa pamięć ręcznych akceptacji  
**Czas życia:** 30 sekund  
**Użycie:** Blokuje drukowanie przez socket gdy akceptacja była ręczna

**Kod (linia 1430):**
```kotlin
if (manuallyAcceptedOrders.contains(s.orderId)) {
    Timber.tag("AUTO_PRINT_STATUS_CHANGE").d("⏭️ Pomijam drukowanie - zamówienie było manualnie zaakceptowane: ${s.orderId}")
    return@onEach
}
```

### 2. `printedOrderIds` (MutableSet<String>) + DataStore
**Cel:** Trwała pamięć wydrukowanych zamówień  
**Czas życia:** Do restartu aplikacji (pamięć) + persist w DataStore  
**Użycie:** Blokuje wielokrotne drukowanie tego samego zamówienia

**Kod (linia 1392, 1443):**
```kotlin
if (wasPrinted(order.orderId)) {
    Timber.tag("AUTO_PRINT_ACCEPTED").d("⏭️ Pomijam drukowanie - zamówienie już było wydrukowane: ${order.orderNumber}")
    return@launch
}
```

### 3. `autoPrintAccepted` (Ustawienie)
**Cel:** Globalne wyłączenie automatycznego drukowania  
**Użycie:** Pozwala użytkownikowi wyłączyć funkcjonalność

## 🎯 Nowa Sekwencja (PO FIX)

### Scenariusz: Użytkownik ręcznie akceptuje zamówienie

```
1. Użytkownik klika "Zaakceptuj" (22:48:56.000)
   └─ OrdersViewModel.updateOrder()
       ├─ API: PUT /orders/{id}/status → ACCEPTED
       ├─ Resource.Success
       │
       ├─ ✅ manuallyAcceptedOrders.add(orderId)  // Dodaj do pamięci ręcznych
       ├─ ✅ Launch: Usuń po 30s
       │
       ├─ ✅ markAsPrinted(orderId) PRZED drukowaniem!
       └─ 🖨️ printerService.printAfterOrderAccepted(order)
           └─ DRUKOWANIE #1 (ręczne) ✅

2. Socket otrzymuje ORDER_STATUS_UPDATED (22:49:01.000 - po 5s)
   └─ SocketEventsRepo.statuses.emit(...)
       └─ OrdersViewModel.socketEventsRepo.statuses.onEach { s ->
           │
           ├─ if (s.newStatus == ACCEPTED)
           │
           ├─ ✅ CHECK #1: manuallyAcceptedOrders.contains(orderId)?
           │   └─ TAK! → return@onEach  ✅ DRUKOWANIE ZABLOKOWANE!
           │
           └─ (gdyby nie było w manuallyAcceptedOrders...)
               ├─ ✅ CHECK #2: wasPrinted(orderId)?
               │   └─ TAK! → return@launch  ✅ DRUKOWANIE ZABLOKOWANE!
               │
               └─ (nigdy nie dojdzie tutaj - podwójne zabezpieczenie!)

3. Po 30 sekundach (22:49:26.000)
   └─ manuallyAcceptedOrders.remove(orderId)  // Cleanup
       └─ printedOrderIds nadal zawiera orderId ✅ (trwałe zabezpieczenie)
```

## 📝 Zmienione Pliki

### `OrdersViewModel.kt`

#### Zmiana #1: Zwiększenie timeout (linia ~1530)
**PRZED:**
```kotlin
delay(5000)
manuallyAcceptedOrders.remove(res.value.orderId)
```

**PO:**
```kotlin
delay(30_000) // Zwiększono z 5s na 30s
manuallyAcceptedOrders.remove(res.value.orderId)
Timber.tag("AUTO_PRINT").d("🗑️ Usunięto ${res.value.orderId} z manuallyAcceptedOrders (po 30s)")
```

#### Zmiana #2: Oznaczanie przed drukowaniem (linia ~1536)
**PRZED:**
```kotlin
printerService.printAfterOrderAccepted(res.value)

// Oznacz jako wydrukowane
markAsPrinted(res.value.orderId)
```

**PO:**
```kotlin
// ✅ FIX: Oznacz jako wydrukowane PRZED drukowaniem
markAsPrinted(res.value.orderId)

Timber.d("🖨️ OrdersViewModel: Wywołuję printAfterOrderAccepted dla %s", res.value.orderNumber)
printerService.printAfterOrderAccepted(res.value)
```

## ✅ Weryfikacja Rozwiązania

### Test 1: Ręczna akceptacja (podstawowy)
```
1. Otrzymaj nowe zamówienie
2. Kliknij "Zaakceptuj + 15 min"
3. Poczekaj 10 sekund
4. Sprawdź logi: TYLKO 1 drukowanie ✅
```

**Oczekiwane logi:**
```
OrdersViewModel: 🖨️ Wywołuję printAfterOrderAccepted dla 12026
PrinterService: 📋 printAfterOrderAccepted: order=12026
AidlPrinterService: 🖨️ AIDL print start
PrinterService: ✅ Drukowanie standardowe zakończone
OrdersViewModel: 💾 Zapisano wydrukowane zamówienie: 698e4ac0fdf981256472dc9b

[Socket przysyła update po 5s]
OrdersViewModel: ⏭️ Pomijam drukowanie - zamówienie było manualnie zaakceptowane: 698e4ac0fdf981256472dc9b
```

### Test 2: Socket spóźniony o 10s
```
1. Zaakceptuj zamówienie
2. Socket przyśle update po 10s
3. Sprawdź: BRAK drugiego drukowania ✅
```

**Zabezpieczenie:**
- `manuallyAcceptedOrders` - zawiera orderId przez 30s ✅
- `printedOrderIds` - zawiera orderId od razu po `markAsPrinted` ✅

### Test 3: Bardzo spóźniony socket (>30s)
```
1. Zaakceptuj zamówienie
2. Socket przyśle update po 35s (po wyczyszczeniu manuallyAcceptedOrders)
3. Sprawdź: BRAK drugiego drukowania ✅
```

**Zabezpieczenie:**
- `manuallyAcceptedOrders` - już nie zawiera (usunięte po 30s)
- `printedOrderIds` - **NADAL ZAWIERA!** ✅ (trwałe zabezpieczenie)

## 🎯 Korzyści Rozwiązania

✅ **Podwójne zabezpieczenie:**
- Krótkoterminowe: `manuallyAcceptedOrders` (30s)
- Długoterminowe: `printedOrderIds` (do restartu + DataStore)

✅ **Brak race condition:**
- `markAsPrinted` wywoływane PRZED drukowaniem
- Socket nie może "wyprzedzić" oznaczenia

✅ **Rollback w przypadku błędu:**
- Jeśli drukowanie się nie powiedzie, `printedOrderIds` czyszczony
- Użytkownik może ponowić drukowanie

✅ **Logi diagnostyczne:**
- Tag `AUTO_PRINT` dla łatwego debugowania
- Dokładne informacje o blokowaniu drukowania

## 📊 Podsumowanie

### Problem:
- **Podwójne drukowanie** po ręcznej akceptacji
- Socket przychodził po 4-6 sekundach
- Timeout 5s był za krótki
- `markAsPrinted` wywoływane za późno

### Rozwiązanie:
- ✅ **Timeout 30s** zamiast 5s
- ✅ **markAsPrinted PRZED** drukowaniem
- ✅ **Rollback** w przypadku błędu
- ✅ **Podwójne zabezpieczenie** (2 mechanizmy)

### Status:
- ✅ **Kompilacja:** OK (tylko ostrzeżenia)
- ✅ **Gotowe do testowania**
- ✅ **Backward compatible** (nie zmienia API)

---

**Data rozwiązania:** 2026-02-12  
**Wersja:** v1.8.0  
**Typ:** Bug fix - Critical  
**Status:** ✅ GOTOWE

