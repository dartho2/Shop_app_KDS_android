# 🔧 FIX: Duplikacja Drukowania Przy Zmianie Statusu Przez Socket

## 🐛 Problem

**Zgłoszenie:** Przy ustawieniach:
- ❌ Automatyczne drukowanie: OFF
- ✅ Drukowanie po zaakceptowaniu: ON
- ✅ Drukowanie na kuchni: ON

Zamówienie PROCESSING → socket zmienia na ACCEPTED → drukuje się **2x każde**:
- 2x Główna
- 2x Kuchnia

**Oczekiwane:** 1x Główna + 1x Kuchnia

---

## 🔍 Diagnoza

### Co Się Dzieje?

Backend wysyła **2 eventy** przez socket:

#### Event 1: socketEventsRepo.statuses
```kotlin
// Zmiana statusu: PROCESSING → ACCEPTED
{ orderId: "123", newStatus: "ACCEPTED" }
```

#### Event 2: socketEventsRepo.orders  
```kotlin
// Całe zamówienie z nowym statusem
{ orderId: "123", orderStatus: { slug: "ACCEPTED" }, ... }
```

### Kod Problematyczny

#### Ścieżka 1: observeSocketEvents() - linia ~1191
```kotlin
// Nasłuchuje na socketEventsRepo.orders
if (orderStatus == OrderStatusEnum.ACCEPTED && 
    appPreferencesManager.getAutoPrintAcceptedEnabled()) {
    
    printerService.printAfterOrderAccepted(order)
    // ✅ DRUKUJE #1
}
```

#### Ścieżka 2: socketEventsRepo.statuses - linia ~1238
```kotlin
// Nasłuchuje na socketEventsRepo.statuses
if (s.newStatus == OrderStatusEnum.ACCEPTED &&
    appPreferencesManager.getAutoPrintAcceptedEnabled()) {
    
    printerService.printAfterOrderAccepted(orderModel)
    // ✅ DRUKUJE #2 (DUPLIKACJA!)
}
```

**Problem:**
- Oba Flow nasłuchują jednocześnie
- Oba wykrywają ACCEPTED
- **Oba drukują** → 2x duplikacja!

---

## ✅ Rozwiązanie

### Kluczowa Zmiana: Synchroniczne Dodawanie do printedOrderIds

**PRZED** (błędny kod):
```kotlin
viewModelScope.launch {
    printerService.printAfterOrderAccepted(order)
    printedOrderIds.add(orderId) // ❌ Asynchroniczne - za późno!
}
```

**Problem:**
1. Ścieżka 1 sprawdza: `printedOrderIds.contains(orderId)` → NIE (false)
2. Ścieżka 2 sprawdza: `printedOrderIds.contains(orderId)` → NIE (false)
3. Ścieżka 1: `viewModelScope.launch` → drukuje → dodaje do setu
4. Ścieżka 2: `viewModelScope.launch` → drukuje → dodaje do setu
5. **OBA wykonują się!** → DUPLIKACJA

---

**PO** (naprawiony kod):
```kotlin
// Sprawdź PRZED
if (printedOrderIds.contains(orderId)) {
    return@onEach // POMIŃ
}

// Dodaj NATYCHMIAST (synchronicznie, PRZED viewModelScope.launch)
printedOrderIds.add(orderId)

viewModelScope.launch {
    try {
        printerService.printAfterOrderAccepted(order)
    } catch (e: Exception) {
        // W przypadku błędu usuń żeby mogło spróbować ponownie
        printedOrderIds.remove(orderId)
    }
}
```

**Jak to działa:**
1. Ścieżka 1 sprawdza: `printedOrderIds.contains(orderId)` → NIE
2. Ścieżka 1 dodaje: `printedOrderIds.add(orderId)` ✅ (SYNCHRONICZNIE!)
3. Ścieżka 2 sprawdza: `printedOrderIds.contains(orderId)` → **TAK!**
4. Ścieżka 2: `return@onEach` → **POMIJA** drukowanie ✅
5. Tylko Ścieżka 1 drukuje → **Brak duplikacji!**

---

## 🔧 Zmiany w Kodzie

### 1. observeSocketEvents() - ACCEPTED (linia ~1201)

**PRZED:**
```kotlin
if (orderStatus == OrderStatusEnum.ACCEPTED && ...) {
    if (printedOrderIds.contains(order.orderId)) {
        return@onEach
    }
    
    viewModelScope.launch {
        printerService.printAfterOrderAccepted(order)
        printedOrderIds.add(order.orderId) // ❌ Za późno
    }
}
```

**PO:**
```kotlin
if (orderStatus == OrderStatusEnum.ACCEPTED && ...) {
    if (printedOrderIds.contains(order.orderId)) {
        return@onEach
    }
    
    // ✅ Dodaj NATYCHMIAST (przed launch)
    printedOrderIds.add(order.orderId)
    
    viewModelScope.launch {
        try {
            printerService.printAfterOrderAccepted(order)
        } catch (e: Exception) {
            printedOrderIds.remove(order.orderId) // Usuń w razie błędu
        }
    }
}
```

---

### 2. socketEventsRepo.statuses (linia ~1247)

**PRZED:**
```kotlin
if (s.newStatus == OrderStatusEnum.ACCEPTED && ...) {
    viewModelScope.launch {
        if (printedOrderIds.contains(s.orderId)) {
            return@launch // ❌ Za późno - launch już się wykonał
        }
        
        printerService.printAfterOrderAccepted(orderModel)
        printedOrderIds.add(s.orderId)
    }
}
```

**PO:**
```kotlin
if (s.newStatus == OrderStatusEnum.ACCEPTED && ...) {
    // ✅ Dodaj NATYCHMIAST (przed launch)
    printedOrderIds.add(s.orderId)
    
    viewModelScope.launch {
        try {
            val orderModel = ... // pobierz z bazy
            printerService.printAfterOrderAccepted(orderModel)
        } catch (e: Exception) {
            printedOrderIds.remove(s.orderId) // Usuń w razie błędu
        }
    }
}
```

---

### 3. observeSocketEvents() - DINE_IN (linia ~1167)

**To samo dla DINE_IN:**
```kotlin
if (order.deliveryType == DINE_IN && ...) {
    if (printedOrderIds.contains(order.orderId)) {
        return@onEach
    }
    
    // ✅ Dodaj NATYCHMIAST
    printedOrderIds.add(order.orderId)
    
    viewModelScope.launch {
        try {
            printerService.printKitchenTicket(order)
        } catch (e: Exception) {
            printedOrderIds.remove(order.orderId)
        }
    }
}
```

---

## 📊 Przepływ Po Naprawie

### Timeline

```
00:00.000 - Backend wysyła zmianę statusu przez socket
  │
  ├─> Event 1: socketEventsRepo.statuses
  │   └─> { orderId: "123", newStatus: "ACCEPTED" }
  │
  └─> Event 2: socketEventsRepo.orders
      └─> { orderId: "123", orderStatus: { slug: "ACCEPTED" } }

00:00.001 - Obie ścieżki wykrywają ACCEPTED jednocześnie
  │
  ├─> Ścieżka 1 (statuses):
  │   ├─> Sprawdza: printedOrderIds.contains("123")? → NIE
  │   ├─> Dodaje: printedOrderIds.add("123") ✅
  │   └─> viewModelScope.launch { drukuje }
  │
  └─> Ścieżka 2 (orders):
      ├─> Sprawdza: printedOrderIds.contains("123")? → TAK! ✅
      └─> return@onEach (POMIJA drukowanie)

00:00.100 - Ścieżka 1 kończy drukowanie
  └─> ✅ Wydruk 1x Główna + 1x Kuchnia

RAZEM: 2 wydruki (prawidłowo!)
```

---

## 🧪 Testy

### Test 1: PROCESSING → Socket ACCEPTED

**Ustawienia:**
- ❌ Auto-print
- ✅ Po akcept.
- ✅ Kuchnia

**Kroki:**
```
1. Wyślij zamówienie PROCESSING
2. Backend zmienia status na ACCEPTED (przez socket)
3. Sprawdź logi i wydruki
```

**Oczekiwane logi:**
```
AUTO_PRINT_STATUS_CHANGE: 🔔 Status zmieniony na ACCEPTED przez socket: 123
🖨️ Auto-druk po zmianie statusu na ACCEPTED: ORDER-123
✅ Auto-druk zakończony dla ORDER-123

AUTO_PRINT_ACCEPTED: ⏭️ Pomijam drukowanie - zamówienie już było wydrukowane: ORDER-123
```

**Oczekiwane wydruki:**
- ✅ 1x Główna
- ✅ 1x Kuchnia

---

### Test 2: Nowe Zamówienie Już ACCEPTED

**Kroki:**
```
1. Wyślij zamówienie już jako ACCEPTED (z Uber Eats)
2. Sprawdź logi i wydruki
```

**Oczekiwane logi:**
```
AUTO_PRINT_ACCEPTED: 🔔 Zamówienie przyszło już ACCEPTED: UBER-456
🖨️ Auto-druk zamówienia już zaakceptowanego: UBER-456
✅ Auto-druk zakończony dla UBER-456
```

**Oczekiwane wydruki:**
- ✅ 1x Główna
- ✅ 1x Kuchnia

---

### Test 3: Manualna Akceptacja

**Kroki:**
```
1. Wyślij zamówienie PROCESSING
2. Zaakceptuj RĘCZNIE w aplikacji
3. Sprawdź logi
```

**Oczekiwane logi:**
```
🖨️ OrdersViewModel: Wywołuję printAfterOrderAccepted dla ORDER-789
✅ Drukowanie zakończone

(socket confirmation)
AUTO_PRINT_STATUS_CHANGE: ⏭️ Pomijam drukowanie - zamówienie było manualnie zaakceptowane: 789
```

**Oczekiwane wydruki:**
- ✅ 1x Główna
- ✅ 1x Kuchnia

---

## 🔍 Diagnostyka

### Sprawdź Logi

```bash
# Filtr na duplikację
adb logcat | grep -E "AUTO_PRINT_ACCEPTED|AUTO_PRINT_STATUS_CHANGE"

# Sprawdź "Pomijam drukowanie"
adb logcat | grep "⏭️ Pomijam drukowanie"
```

**Jeśli widzisz:**
```
⏭️ Pomijam drukowanie - zamówienie już było wydrukowane
```
→ ✅ Fix działa poprawnie!

**Jeśli NIE widzisz** tego logu:
→ Problem może być gdzie indziej (sprawdź inne ścieżki)

---

## ⚠️ Race Condition

### Dlaczego Synchroniczne Dodawanie Jest Kluczowe?

**Asynchroniczne (błędne):**
```kotlin
Thread 1: if (!contains("123")) {      // TRUE
Thread 2: if (!contains("123")) {      // TRUE (jeszcze nie dodane!)
Thread 1:   launch { add("123") }      // Dodaje
Thread 2:   launch { add("123") }      // Dodaje PONOWNIE
```
**Rezultat:** OBA drukują ❌

**Synchroniczne (poprawne):**
```kotlin
Thread 1: if (!contains("123")) {      // TRUE
Thread 1:   add("123")                 // Dodaje NATYCHMIAST
Thread 2: if (!contains("123")) {      // FALSE (już dodane!)
Thread 2:   return                     // POMIJA
Thread 1:   launch { print() }         // Tylko Thread 1 drukuje
```
**Rezultat:** Tylko jeden drukuje ✅

---

## ✅ Podsumowanie

### Przed Naprawą
```
Socket: PROCESSING → ACCEPTED
  ├─> Ścieżka 1 (statuses) → DRUKUJE
  └─> Ścieżka 2 (orders) → DRUKUJE PONOWNIE
= 2x Główna + 2x Kuchnia (4 wydruki!) ❌
```

### Po Naprawie
```
Socket: PROCESSING → ACCEPTED
  ├─> Ścieżka 1 → Dodaje do setu → DRUKUJE
  └─> Ścieżka 2 → Sprawdza set → POMIJA
= 1x Główna + 1x Kuchnia (2 wydruki) ✅
```

### Korzyści
- ✅ Brak duplikacji przy zmianie statusu przez socket
- ✅ Obsługa race condition (synchroniczne dodawanie)
- ✅ Error handling (usuwa z setu w razie błędu)
- ✅ Działa dla wszystkich ścieżek (DINE_IN, ACCEPTED, socket)

---

**Data naprawy:** 2026-02-04  
**Wersja:** 1.6  
**Status:** ✅ Naprawione  
**Issue:** Duplikacja drukowania przy zmianie statusu przez socket

---

## 📞 Powiązane

- `FIX_DOUBLE_PRINTING_MANUAL_ACCEPTANCE.md` - Fix duplikacji manualnej
- `FIX_RESTART_PRINTING.md` - Fix drukowania przy restarcie
- `DOKUMENTACJA_AUTOMATYCZNEGO_DRUKOWANIA.md` - Główna dokumentacja

