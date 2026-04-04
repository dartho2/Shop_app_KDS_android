# 🔧 FIX: Drukowanie Przy Restarcie Aplikacji

## 🐛 Problem

**Zgłoszenie:** Gdy wyłączasz i włączasz aplikację, ostatnie zamówienie drukuje się ponownie.

**Scenariusz:**
```
1. Zamówienie DINE_IN przychodzi → Drukuje się ✅
2. Użytkownik wyłącza aplikację
3. Użytkownik włącza aplikację
4. Zamówienie DINE_IN drukuje się PONOWNIE ❌
```

---

## 🔍 Diagnoza

### Przyczyna Problemu

**Co się dzieje przy restarcie:**

```
1. Aplikacja startuje
   └─> Socket reconnect do backendu
   
2. Backend wysyła ostatnie zamówienia (synchronizacja)
   └─> SocketStaffEventsHandler.handleOrder()
       └─> socketEventsRepo.emitOrder(order)
   
3. OrdersViewModel.observeSocketEvents() nasłuchuje
   └─> Wykrywa zamówienie ACCEPTED lub DINE_IN
       └─> ✅ DRUKUJE PONOWNIE!
```

**Problem:** 
- Brak mechanizmu sprawdzającego **czy zamówienie już było kiedyś wydrukowane**
- System traktuje "stare" zamówienia jak nowe przy restarcie

---

### Kod Problematyczny

#### 1. observeSocketEvents() - DINE_IN
```kotlin
// OrdersViewModel.kt - linia ~1156
if ((order.deliveryType == OrderDelivery.DINE_IN || ...) &&
    appPreferencesManager.getAutoPrintDineInEnabled()) {
    
    // ❌ BRAK sprawdzenia czy już było wydrukowane
    printerService.printKitchenTicket(order)
}
```

#### 2. observeSocketEvents() - ACCEPTED
```kotlin
// OrdersViewModel.kt - linia ~1191
if (orderStatus == OrderStatusEnum.ACCEPTED &&
    appPreferencesManager.getAutoPrintAcceptedEnabled()) {
    
    // ❌ BRAK sprawdzenia czy już było wydrukowane
    printerService.printAfterOrderAccepted(order)
}
```

#### 3. socketEventsRepo.statuses - Zmiana statusu
```kotlin
// OrdersViewModel.kt - linia ~1238
if (s.newStatus == OrderStatusEnum.ACCEPTED &&
    appPreferencesManager.getAutoPrintAcceptedEnabled()) {
    
    // ❌ BRAK sprawdzenia czy już było wydrukowane
    printerService.printAfterOrderAccepted(orderModel)
}
```

---

## ✅ Rozwiązanie

### Dodano Set do Śledzenia Wydrukowanych Zamówień

**Plik:** `OrdersViewModel.kt` (linia ~143)

```kotlin
// Set do śledzenia zamówień które już zostały wydrukowane
private val printedOrderIds = mutableSetOf<String>()
```

**Jak działa:**
- Gdy zamówienie zostanie wydrukowane → `printedOrderIds.add(orderId)`
- Przed drukowaniem → sprawdzamy `printedOrderIds.contains(orderId)`
- Jeśli zawiera → pomijamy drukowanie

---

### Zmiany w Kodzie

#### 1. DINE_IN - Dodano Sprawdzenie i Oznaczanie

**PRZED:**
```kotlin
if ((order.deliveryType == OrderDelivery.DINE_IN || ...) && ...) {
    printerService.printKitchenTicket(order)
}
```

**PO:**
```kotlin
if ((order.deliveryType == OrderDelivery.DINE_IN || ...) && ...) {
    
    // Sprawdź czy już było wydrukowane
    if (printedOrderIds.contains(order.orderId)) {
        Timber.d("⏭️ Pomijam drukowanie - zamówienie już było wydrukowane")
        return@onEach
    }
    
    printerService.printKitchenTicket(order)
    
    // Oznacz jako wydrukowane
    printedOrderIds.add(order.orderId)
}
```

---

#### 2. Zamówienia ACCEPTED - Dodano Sprawdzenie i Oznaczanie

**PRZED:**
```kotlin
if (orderStatus == OrderStatusEnum.ACCEPTED && ...) {
    printerService.printAfterOrderAccepted(order)
}
```

**PO:**
```kotlin
if (orderStatus == OrderStatusEnum.ACCEPTED && ...) {
    
    // Sprawdź czy już było wydrukowane
    if (printedOrderIds.contains(order.orderId)) {
        Timber.d("⏭️ Pomijam drukowanie - zamówienie już było wydrukowane")
        return@onEach
    }
    
    printerService.printAfterOrderAccepted(order)
    
    // Oznacz jako wydrukowane
    printedOrderIds.add(order.orderId)
}
```

---

#### 3. Manualna Akceptacja - Dodano Oznaczanie

**PRZED:**
```kotlin
viewModelScope.launch {
    printerService.printAfterOrderAccepted(res.value)
}
```

**PO:**
```kotlin
viewModelScope.launch {
    printerService.printAfterOrderAccepted(res.value)
    
    // Oznacz jako wydrukowane
    printedOrderIds.add(res.value.orderId)
}
```

---

#### 4. Zmiana Statusu przez Socket - Dodano Sprawdzenie i Oznaczanie

**PRZED:**
```kotlin
viewModelScope.launch {
    val allOrders = repository.getAllOrdersFlow().first()
    val entity = allOrders.find { it.orderId == s.orderId }
    if (entity != null) {
        printerService.printAfterOrderAccepted(orderModel)
    }
}
```

**PO:**
```kotlin
viewModelScope.launch {
    // Sprawdź czy już było wydrukowane
    if (printedOrderIds.contains(s.orderId)) {
        Timber.d("⏭️ Pomijam drukowanie - zamówienie już było wydrukowane")
        return@launch
    }
    
    val allOrders = repository.getAllOrdersFlow().first()
    val entity = allOrders.find { it.orderId == s.orderId }
    if (entity != null) {
        printerService.printAfterOrderAccepted(orderModel)
        
        // Oznacz jako wydrukowane
        printedOrderIds.add(s.orderId)
    }
}
```

---

## 📊 Przepływ Po Naprawie

### Scenariusz 1: Pierwsze Drukowanie (Nowe Zamówienie)

```
1. Zamówienie DINE_IN przychodzi
   └─> observeSocketEvents()
       ├─> Sprawdzenie: printedOrderIds.contains(orderId)? → NIE
       ├─> ✅ DRUKUJE
       └─> printedOrderIds.add(orderId)
```

---

### Scenariusz 2: Restart Aplikacji

```
1. Użytkownik restartuje aplikację
   
2. Socket reconnect → Backend wysyła ostatnie zamówienia
   └─> observeSocketEvents()
       ├─> Sprawdzenie: printedOrderIds.contains(orderId)? → TAK
       └─> ⏭️ POMIJA drukowanie
```

**Rezultat:** Brak duplikacji! ✅

---

### Scenariusz 3: Nowe Zamówienie Po Restarcie

```
1. Restart aplikacji
   └─> printedOrderIds zachowuje wartości (w pamięci)
   
2. NOWE zamówienie przychodzi (inne orderId)
   └─> observeSocketEvents()
       ├─> Sprawdzenie: printedOrderIds.contains(newOrderId)? → NIE
       ├─> ✅ DRUKUJE
       └─> printedOrderIds.add(newOrderId)
```

---

## ⚠️ Ograniczenia Rozwiązania

### 1. Dane w Pamięci (In-Memory)

**Charakterystyka:**
- `printedOrderIds` jest w pamięci RAM (MutableSet)
- **NIE jest** zapisywane w SharedPreferences ani bazie danych

**Konsekwencje:**

#### ✅ Działa:
- Restart aplikacji (soft restart)
- Przełączanie między ekranami
- App w tle (suspended)

#### ❌ Resetuje się:
- Force kill aplikacji (kill process)
- Restart urządzenia
- Czyszczenie pamięci przez system

---

### 2. Co się stanie po Force Kill?

```
1. Force kill aplikacji
   └─> printedOrderIds.clear() (pamięć zwolniona)
   
2. Ponowne uruchomienie
   └─> printedOrderIds = emptySet()
   
3. Socket reconnect → Backend wysyła ostatnie zamówienia
   └─> printedOrderIds.contains(orderId)? → NIE (bo puste!)
       └─> ✅ DRUKUJE PONOWNIE
```

**Rezultat:** Po force kill zamówienia mogą wydrukować się ponownie

---

## 🔄 Alternatywne Rozwiązania (Opcjonalne)

### Opcja A: Zapisywanie w SharedPreferences

**Zalety:**
- Przetrwa force kill
- Przetrwa restart urządzenia

**Wady:**
- Potencjalnie duży rozmiar (setki zamówień)
- Trzeba okresowo czyścić (stare zamówienia)

**Implementacja:**
```kotlin
// Przy drukowaniu
printedOrderIds.add(orderId)
prefs.savePrintedOrderIds(printedOrderIds.toList())

// Przy starcie
printedOrderIds.addAll(prefs.loadPrintedOrderIds())
```

---

### Opcja B: Dodanie Flagi w Bazie Danych

**Zalety:**
- Trwałe przechowywanie
- Można filtrować w zapytaniach SQL

**Wady:**
- Wymaga zmiany schematu bazy (migration)
- Bardziej skomplikowane

**Implementacja:**
```kotlin
// Dodaj kolumnę do OrderEntity
@Entity
data class OrderEntity(
    @ColumnInfo(name = "was_printed") val wasPrinted: Boolean = false
)

// Przy drukowaniu
repository.markOrderAsPrinted(orderId)
```

---

### Opcja C: Sprawdzanie Czasu Utworzenia

**Zalety:**
- Nie wymaga dodatkowego stanu
- Proste

**Wady:**
- Wymaga założenia (np. "nie drukuj zamówień starszych niż 1h")
- Może pominąć zamówienia jeśli app była wyłączona długo

**Implementacja:**
```kotlin
val orderAge = System.currentTimeMillis() - order.createdAt
if (orderAge > TimeUnit.HOURS.toMillis(1)) {
    Timber.d("⏭️ Pomijam - zamówienie starsze niż 1h")
    return@onEach
}
```

---

## 🧪 Testy

### Test 1: Soft Restart (Działa)

```
1. Wyślij zamówienie DINE_IN
   └─> Sprawdź: Drukuje się ✅
   
2. Zamknij aplikację (recent apps → swipe)
3. Otwórz aplikację ponownie
   
4. Sprawdź logi:
   grep "⏭️ Pomijam drukowanie - zamówienie już było wydrukowane"
   
5. Rezultat: NIE drukuje się ponownie ✅
```

---

### Test 2: Force Kill (Reset)

```
1. Wyślij zamówienie DINE_IN
   └─> Sprawdź: Drukuje się ✅
   
2. Force kill aplikacji:
   adb shell am force-stop com.itsorderchat
   
3. Otwórz aplikację ponownie
   
4. Sprawdź logi:
   (BRAK "⏭️ Pomijam drukowanie")
   
5. Rezultat: Może wydrukować się ponownie ⚠️
   (bo printedOrderIds został wyczyszczony)
```

---

### Test 3: Nowe Zamówienie Po Restarcie

```
1. Restart aplikacji (soft)
2. Wyślij NOWE zamówienie (inne orderId)
   
3. Sprawdź logi:
   (BRAK "⏭️ Pomijam drukowanie")
   ✅ "DRUKUJE"
   
4. Rezultat: Nowe zamówienie drukuje się normalnie ✅
```

---

## 🔍 Diagnostyka

### Logi do Monitorowania

```bash
# Sprawdź czy zamówienie jest pomijane
adb logcat | grep "⏭️ Pomijam drukowanie"

# Sprawdź DINE_IN
adb logcat | grep "AUTO_PRINT_DINE_IN"

# Sprawdź ACCEPTED
adb logcat | grep "AUTO_PRINT_ACCEPTED"

# Sprawdź socket status change
adb logcat | grep "AUTO_PRINT_STATUS_CHANGE"
```

### Oczekiwane Logi (Restart)

**Przy pierwszym drukowaniu:**
```
AUTO_PRINT_DINE_IN: 🔔 Warunek auto-druku DINE_IN spełniony
🖨️ Auto-druk DINE_IN/ROOM_SERVICE dla ORDER-123
✅ Auto-druk zakończony dla ORDER-123
```

**Przy restarcie (to samo zamówienie):**
```
AUTO_PRINT_DINE_IN: ⏭️ Pomijam drukowanie - zamówienie już było wydrukowane: ORDER-123
```

---

## ✅ Podsumowanie

### Przed Naprawą
```
Restart aplikacji → Backend wysyła ostatnie zamówienia
  └─> ✅ DRUKUJE PONOWNIE ❌
```

### Po Naprawie
```
Restart aplikacji → Backend wysyła ostatnie zamówienia
  ├─> Sprawdzenie: printedOrderIds.contains(orderId)?
  └─> TAK → ⏭️ POMIJA drukowanie ✅
```

### Korzyści
- ✅ Brak duplikacji przy soft restart
- ✅ Minimalne zmiany w kodzie (1 Set + 4 sprawdzenia)
- ✅ Brak zmian w bazie danych
- ✅ Nie wymaga SharedPreferences

### Ograniczenia
- ⚠️ Dane w pamięci (resetuje się po force kill)
- ⚠️ Może wydrukować ponownie po restarcie urządzenia

### Dalsze Usprawnienia (Opcjonalne)
- Opcja A: Zapisywanie w SharedPreferences (trwałe)
- Opcja B: Flaga w bazie danych (najbardziej solidne)
- Opcja C: Sprawdzanie wieku zamówienia (proste)

---

**Data naprawy:** 2026-02-04  
**Wersja:** 1.5  
**Status:** ✅ Naprawione (in-memory solution)  
**Issue:** Drukowanie przy restarcie aplikacji

---

## 📞 Powiązane

- `FIX_DOUBLE_PRINTING_MANUAL_ACCEPTANCE.md` - Fix duplikacji manualnej
- `FIX_EXTERNAL_ACCEPTANCE_PRINTING.md` - Fix zewnętrznej akceptacji
- `DOKUMENTACJA_AUTOMATYCZNEGO_DRUKOWANIA.md` - Główna dokumentacja

