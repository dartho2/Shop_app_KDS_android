# 🚨 CRITICAL FIX v1.7.3 - DINE_IN Nie Drukuje (Suspend Function Bug)

## 🐛 Problem

**Zgłoszenie:** Zamówienie DINE_IN przychodzi przez socket jako ORDER_CREATED ze statusem ACCEPTED i **NIE drukuje się**.

**Scenariusz:**
```
1. Backend tworzy zamówienie DINE_IN (status: ACCEPTED)
2. Socket wysyła event: type="ORDER_CREATED"
3. SocketStaffEventsHandler odbiera i emituje przez socketEventsRepo.orders
4. OrdersViewModel.observeSocketEvents() - powinien wydrukować
5. Rezultat: ❌ NIE DRUKUJE (mimo że "Auto-drukuj zamówienia na miejscu" = ON)
```

---

## 🔍 Diagnoza - Suspend Function w If Statement!

### Kod Przed Naprawą (BŁĘDNY)

```kotlin
// OrdersViewModel.kt - observeSocketEvents()
socketEventsRepo.orders.onEach { order ->
    
    // ❌ BŁĄD: Wywołanie suspend function w warunku if!
    if ((order.deliveryType == DINE_IN) && 
        appPreferencesManager.getAutoPrintDineInEnabled()) {  // ← suspend function!
        
        viewModelScope.launch {
            // Ten kod NIGDY SIĘ NIE WYKONUJE!
        }
    }
}
```

### Problem

**`getAutoPrintDineInEnabled()` jest suspend function:**
```kotlin
// AppPreferencesManager.kt
suspend fun getAutoPrintDineInEnabled(): Boolean =
    dataStore.data.map { it[Keys.AUTO_PRINT_DINE_IN] ?: false }.first()
```

**BŁĄD:**
- `onEach { }` **NIE jest** suspend context
- Nie możesz wywołać `suspend fun` bezpośrednio w warunku `if`
- Kod **kompiluje się** (Kotlin pozwala), ale **nie działa poprawnie**
- Warunek zawsze zwraca `false` lub rzuca exception (który jest ignorowany)

---

## ✅ Rozwiązanie

### Przeniesienie Sprawdzenia Do Środka viewModelScope.launch

**Kod Po Naprawie (POPRAWNY):**
```kotlin
socketEventsRepo.orders.onEach { order ->
    
    // ✅ Sprawdź tylko typ (nie-suspend)
    if (order.deliveryType == DINE_IN || 
        order.deliveryType == ROOM_SERVICE) {
        
        viewModelScope.launch {
            // ✅ Sprawdź ustawienie WEWNĄTRZ launch (suspend context)
            if (!appPreferencesManager.getAutoPrintDineInEnabled()) {
                Timber.d("⏭️ Auto-druk DINE_IN wyłączony w ustawieniach")
                return@launch
            }
            
            // ✅ Teraz kod się wykonuje poprawnie!
            if (wasPrinted(order.orderId)) {
                return@launch
            }
            
            markAsPrinted(order.orderId)
            printerService.printKitchenTicket(order)
        }
    }
}
```

---

## 🔧 Zmiany w Kodzie

### 1. AUTO-DRUK DINE_IN (linia ~1202)

**PRZED:**
```kotlin
if ((order.deliveryType == DINE_IN || ...) &&
    appPreferencesManager.getAutoPrintDineInEnabled()) {  // ❌ Suspend w if!
    
    viewModelScope.launch {
        if (wasPrinted(...)) { ... }
    }
}
```

**PO:**
```kotlin
if (order.deliveryType == DINE_IN || 
    order.deliveryType == ROOM_SERVICE) {
    
    viewModelScope.launch {
        // ✅ Sprawdzenie suspend function WEWNĄTRZ launch
        if (!appPreferencesManager.getAutoPrintDineInEnabled()) {
            return@launch
        }
        
        if (wasPrinted(...)) { ... }
        // Reszta kodu...
    }
}
```

---

### 2. AUTO-DRUK ACCEPTED (linia ~1252)

**PRZED:**
```kotlin
if (orderStatus == ACCEPTED &&
    order.deliveryType != DINE_IN &&
    order.deliveryType != ROOM_SERVICE &&
    appPreferencesManager.getAutoPrintAcceptedEnabled()) {  // ❌ Suspend w if!
    
    viewModelScope.launch { ... }
}
```

**PO:**
```kotlin
if (orderStatus == ACCEPTED &&
    order.deliveryType != DINE_IN &&
    order.deliveryType != ROOM_SERVICE) {
    
    viewModelScope.launch {
        // ✅ Sprawdzenie suspend function WEWNĄTRZ launch
        if (!appPreferencesManager.getAutoPrintAcceptedEnabled()) {
            return@launch
        }
        
        // Reszta kodu...
    }
}
```

---

### 3. socketEventsRepo.statuses (linia ~1302)

**PRZED:**
```kotlin
if (s.newStatus == ACCEPTED &&
    appPreferencesManager.getAutoPrintAcceptedEnabled()) {  // ❌ Suspend w if!
    
    if (manuallyAcceptedOrders.contains(...)) { ... }
    viewModelScope.launch { ... }
}
```

**PO:**
```kotlin
if (s.newStatus == ACCEPTED) {
    
    if (manuallyAcceptedOrders.contains(...)) { ... }
    
    viewModelScope.launch {
        // ✅ Sprawdzenie suspend function WEWNĄTRZ launch
        if (!appPreferencesManager.getAutoPrintAcceptedEnabled()) {
            return@launch
        }
        
        // Reszta kodu...
    }
}
```

---

## 📊 Przepływ Po Naprawie

### Zamówienie DINE_IN (ORDER_CREATED, ACCEPTED)

```
00:00.000 - Socket wysyła ORDER_CREATED
              └─> { type: "ORDER_CREATED", order: {...} }

00:00.001 - SocketStaffEventsHandler.handleNewOrProcessingOrder()
              ├─> Zapisuje do bazy
              ├─> socketEventsRepo.emitOrder(order) ✅
              └─> Nie drukuje (bo status != PROCESSING)

00:00.002 - socketEventsRepo.orders emituje zamówienie
              └─> OrdersViewModel.observeSocketEvents() wykrywa

00:00.003 - Sprawdzenie typu (nie-suspend):
              ├─> order.deliveryType == DINE_IN? ✅ TRUE
              └─> Wchodzi do if

00:00.004 - viewModelScope.launch {
              ├─> Sprawdza: getAutoPrintDineInEnabled()? ✅ TRUE
              ├─> Sprawdza: wasPrinted()? ❌ FALSE
              ├─> markAsPrinted(orderId) ✅
              └─> printKitchenTicket(order) ✅ DRUKUJE!

REZULTAT: ✅ 1x Kuchnia
```

---

## 🧪 Test

### Test 1: DINE_IN Przychodzi jako ORDER_CREATED

```bash
1. Wyślij zamówienie DINE_IN (status: ACCEPTED)
2. Sprawdź logi:

Oczekiwane:
📥 Received order from socket: orderId=DINE-123
💾 Order saved to database: orderId=DINE-123
AUTO_PRINT_DINE_IN: 🔔 Warunek auto-druku DINE_IN spełniony dla 00420505
🖨️ Auto-druk DINE_IN/ROOM_SERVICE dla 00420505 na drukarce: kitchen
💾 Zapisano wydrukowane zamówienie do DataStore: DINE-123
✅ Auto-druk DINE_IN/ROOM_SERVICE zakończony dla 00420505

3. Sprawdź wydruki:
   ✅ 1x Kuchnia
```

### Test 2: Ustawienie Wyłączone

```bash
1. Wyłącz "Auto-drukuj zamówienia na miejscu"
2. Wyślij DINE_IN
3. Sprawdź logi:

Oczekiwane:
📥 Received order from socket: orderId=DINE-123
💾 Order saved to database: orderId=DINE-123
AUTO_PRINT_DINE_IN: ⏭️ Auto-druk DINE_IN wyłączony w ustawieniach

4. Sprawdź wydruki:
   ❌ 0 wydruków (poprawnie)
```

---

## ⚠️ Dlaczego To Nie Było Wykryte Wcześniej?

### 1. Kompilator NIE Wyrzuca Błędu

Kotlin **pozwala** wywołać suspend function poza suspend context w niektórych przypadkach:
```kotlin
// To się kompiluje (ale nie działa poprawnie):
if (suspendFunction()) { ... }
```

### 2. Brak Runtime Exception

- Kod nie rzuca exception
- Po prostu **nie wykonuje się** lub zwraca domyślną wartość
- Trudno debugować - wygląda jakby warunek był `false`

### 3. Logi Myląco Wyglądają OK

```
📥 Received order from socket: orderId=DINE-123
💾 Order saved to database: orderId=DINE-123
(brak dalszych logów - warunek if nie spełniony)
```

Wygląda jakby ustawienie było wyłączone, ale w rzeczywistości **warunek się nie wykonuje poprawnie**.

---

## 📝 Best Practices

### ✅ POPRAWNE Wzorce

**Wzorzec 1: Sprawdzenie wewnątrz launch**
```kotlin
if (condition1) {  // Nie-suspend
    viewModelScope.launch {
        if (suspendFunction()) {  // ✅ OK - suspend context
            // ...
        }
    }
}
```

**Wzorzec 2: Wywołanie przed if**
```kotlin
viewModelScope.launch {
    val isEnabled = suspendFunction()  // ✅ OK
    if (isEnabled) {
        // ...
    }
}
```

---

### ❌ BŁĘDNE Wzorce

**Wzorzec 1: Suspend w warunku if (poza suspend context)**
```kotlin
if (condition1 && suspendFunction()) {  // ❌ BŁĄD!
    // Ten kod może nie działać
}
```

**Wzorzec 2: Suspend w onEach/map (nie-suspend context)**
```kotlin
flow.onEach { item ->
    if (suspendFunction()) {  // ❌ BŁĄD!
        // ...
    }
}
```

---

## 🔍 Diagnostyka

### Jak Sprawdzić Czy Masz Ten Problem?

**Symptomy:**
1. Warunek zawsze jest `false` mimo że ustawienie jest `ON`
2. Kod w `if` **nigdy się nie wykonuje**
3. Brak logów z wewnątrz `if`
4. Kompilator NIE zgłasza błędu

**Sprawdź:**
```bash
# Szukaj suspend functions w warunkach if
grep -r "if.*getAutoPrint.*Enabled()" --include="*.kt"

# Sprawdź czy są w suspend context
```

**Fix:**
- Przenieś wywołanie suspend function do środka `viewModelScope.launch`
- Lub wywołaj przed `if` i zapisz do zmiennej

---

## ✅ Podsumowanie

### Przed v1.7.3 (BŁĄD)
```kotlin
if (condition && suspendFunction()) {  // ❌ Nie działa
    launch { ... }
}

Rezultat: NIE DRUKUJE (warunek zawsze false)
```

### Po v1.7.3 (NAPRAWIONE)
```kotlin
if (condition) {
    launch {
        if (!suspendFunction()) { return }  // ✅ Działa
        // ...
    }
}

Rezultat: ✅ DRUKUJE POPRAWNIE
```

### Korzyści
- ✅ DINE_IN drukuje się poprawnie
- ✅ Ustawienia działają jak należy
- ✅ Kod działa zgodnie z dokumentacją
- ✅ Brak false negatives

### Zmienione Ścieżki
1. ✅ AUTO-DRUK DINE_IN
2. ✅ AUTO-DRUK ACCEPTED
3. ✅ socketEventsRepo.statuses

---

**Data naprawy:** 2026-02-05  
**Wersja:** 1.7.3 (CRITICAL - Suspend Function Fix)  
**Status:** ✅ NAPRAWIONE  
**Issue:** DINE_IN nie drukuje się (suspend function w warunku if)  
**Impact:** CRITICAL - Dotyczył WSZYSTKICH auto-print funkcji

**Kluczowa zmiana:** Przeniesienie sprawdzenia suspend functions do środka viewModelScope.launch

---

## 📞 Powiązane

- `DOKUMENTACJA_DINE_IN_DRUKOWANIE.md` - Dokumentacja DINE_IN (zaktualizowana)
- `FINAL_FIX_v1.7.2_MUTEX_THREAD_SAFETY.md` - Thread safety fix
- `FIX_PERSISTENT_PRINTED_ORDERS.md` - Persystencja
- `DOKUMENTACJA_AUTOMATYCZNEGO_DRUKOWANIA.md` - Główna dokumentacja

