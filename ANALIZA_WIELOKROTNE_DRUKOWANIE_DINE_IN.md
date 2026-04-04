# Analiza Problemu: Wielokrotne Drukowanie na Drukarce Kuchennej

## 🔍 Problem

Zamówienie DINE_IN drukuje się **3 razy** na drukarce kuchennej mimo że użytkownik ma włączone:
- ✅ Automatyczne drukowanie
- ✅ Auto-drukuj zamówienia na miejscu (drukarka kuchenna)
- ✅ Drukowanie na kuchni

## 📊 Analiza Logów

```
23:31:41.579 - Pierwszy wydruk (5258ms)
  └─ PrinterSTEP: [ENTRY] target=KITCHEN order=00420505
  └─ ✅ Wydruk zakończony kuchnia

23:31:47.552 - Drugi wydruk (1018ms)  
  └─ PrinterSTEP: [ENTRY] target=KITCHEN order=00420505
  └─ ✅ Wydruk zakończony kuchnia

23:31:49.287 - Trzeci wydruk (1029ms)
  └─ PrinterSTEP: [ENTRY] target=KITCHEN order=00420505
  └─ ✅ Wydruk zakończony kuchnia
```

**Każdy wydruk zaczyna się od**: `PrinterPreferences: Wczytano 2 drukarek`

To oznacza że **cała funkcja drukowania jest wywoływana 3 razy**, nie że jedna funkcja drukuje 3 razy.

---

## 🕵️ Źródła Automatycznego Drukowania

W kodzie istnieją **4 miejsca** gdzie może nastąpić automatyczne drukowanie:

### 1️⃣ SocketStaffEventsHandler.kt (linia 311)
```kotlin
// AUTO-PRINT DLA NOWYCH ZAMÓWIEŃ
if (isProcessing) {
    val autoPrintEnabled = appPreferencesManager.getAutoPrintEnabled()
    if (autoPrintEnabled) {
        printerService.printOrder(order, useDeliveryInterval = true)
    }
}
```
**Warunek**: 
- Status = PROCESSING
- `getAutoPrintEnabled()` = TRUE ✅

**Problem**: Drukuje WSZYSTKIE zamówienia, w tym DINE_IN

---

### 2️⃣ OrdersViewModel.kt (linia 1150-1165)
```kotlin
// AUTO-DRUK DLA DINE_IN / ROOM_SERVICE (omija akceptację)
if ((order.deliveryType == OrderDelivery.DINE_IN || order.deliveryType == OrderDelivery.ROOM_SERVICE) &&
    appPreferencesManager.getAutoPrintDineInEnabled()) {
    
    val printerType = appPreferencesManager.getAutoPrintDineInPrinter()
    when (printerType) {
        "kitchen" -> printerService.printKitchenTicket(order)
        else -> printerService.printOrder(order)
    }
}
```
**Warunek**:
- deliveryType = DINE_IN lub ROOM_SERVICE
- `getAutoPrintDineInEnabled()` = TRUE ✅
- `getAutoPrintDineInPrinter()` = "kitchen" ✅

**Problem**: To jest dedykowana logika dla DINE_IN - OK

---

### 3️⃣ OrdersViewModel.kt (linia 1218-1222)
```kotlin
// AUTO-DRUK PO AKCEPTACJI
if (res.value.orderStatus.slug == OrderStatusEnum.ACCEPTED.name &&
    appPreferencesManager.getAutoPrintAcceptedEnabled()) {
    
    printerService.printAfterOrderAccepted(res.value)
}
```
**Warunek**:
- Status = ACCEPTED
- `getAutoPrintAcceptedEnabled()` = TRUE ✅

**Problem**: To nie powinno się wykonać dla DINE_IN bo nie akceptujemy tych zamówień

---

### 4️⃣ PrinterService.printAfterOrderAccepted() (linia 230-250)
```kotlin
suspend fun printAfterOrderAccepted(order: Order) {
    // Drukuje na standardowej
    standardTargets.forEach { printOne(it, order) }
    
    // Jeśli włączone, drukuje też na kuchennej
    if (autoPrintKitchen) {
        kitchenTargets.forEach { printOne(it, order) }
    }
}
```
**Warunek**:
- `getAutoPrintKitchenEnabled()` = TRUE ✅

---

## 💥 Przyczyna Problemu

Dla zamówienia DINE_IN z włączonymi ustawieniami drukuje się:

### Scenariusz 1: Nowe zamówienie DINE_IN przychodzi
1. **SocketStaffEventsHandler** (linia 311)
   - Warunek: PROCESSING + autoPrintEnabled = TRUE
   - Wywołuje: `printerService.printOrder(order)` 
   - Drukuje na: KITCHEN (bo wybrano "drukarka kuchenna")
   - **Wydruk #1** ✅

2. **OrdersViewModel.observeSocketEvents()** (linia 1150)
   - Warunek: DINE_IN + autoPrintDineInEnabled = TRUE
   - Wywołuje: `printerService.printKitchenTicket(order)`
   - Drukuje na: KITCHEN
   - **Wydruk #2** ✅

### Scenariusz 2: Użytkownik (przypadkowo?) akceptuje zamówienie DINE_IN
3. **OrdersViewModel.executeOrderUpdate()** (linia 1218)
   - Warunek: ACCEPTED + autoPrintAcceptedEnabled = TRUE
   - Wywołuje: `printerService.printAfterOrderAccepted(order)`
   - Drukuje na: STANDARD + KITCHEN (bo autoPrintKitchen = TRUE)
   - **Wydruk #3** ✅

---

## ✅ ROZWIĄZANIE

### Fix #1: Wykluczyć DINE_IN z SocketStaffEventsHandler ✅ ZROBIONE

**Plik**: `SocketStaffEventsHandler.kt`

```kotlin
// AUTO-PRINT - WYKLUCZAMY DINE_IN/ROOM_SERVICE
if (isProcessing && 
    order.deliveryType != OrderDelivery.DINE_IN &&
    order.deliveryType != OrderDelivery.ROOM_SERVICE) {
    
    val autoPrintEnabled = appPreferencesManager.getAutoPrintEnabled()
    if (autoPrintEnabled) {
        printerService.printOrder(order, useDeliveryInterval = true)
    }
}
```

**Uzasadnienie**: 
- DINE_IN/ROOM_SERVICE mają dedykowaną logikę w `OrdersViewModel`
- Nie powinny być drukowane przez ogólny auto-print

---

### Fix #2: Dodać Logi Debugowe ✅ ZROBIONE

**Plik**: `OrdersViewModel.kt`

```kotlin
if ((order.deliveryType == OrderDelivery.DINE_IN || ...) && ...) {
    Timber.tag("AUTO_PRINT_DINE_IN").w("🔔 Warunek auto-druku DINE_IN spełniony")
    Timber.tag("AUTO_PRINT_DINE_IN").w("   Stack trace:", Exception("Stack trace"))
    // ...
}
```

**Cel**: Zobaczyć stack trace i dowiedzieć się skąd pochodzi wielokrotne wywołanie

---

### Fix #3: Zapobiec Akceptacji DINE_IN? 🤔 OPCJONALNIE

Jeśli zamówienia DINE_IN **NIE WYMAGAJĄ** akceptacji, można:

**Opcja A**: Wyłączyć przycisk "Akceptuj" dla DINE_IN w UI

**Opcja B**: Dodać warunek w `executeOrderUpdate`:
```kotlin
// AUTO-DRUK PO AKCEPTACJI - wykluczamy DINE_IN
if (res.value.orderStatus.slug == OrderStatusEnum.ACCEPTED.name &&
    appPreferencesManager.getAutoPrintAcceptedEnabled() &&
    res.value.deliveryType != OrderDelivery.DINE_IN &&
    res.value.deliveryType != OrderDelivery.ROOM_SERVICE) {
    
    printerService.printAfterOrderAccepted(res.value)
}
```

---

## 📋 Checklist Naprawy

- [x] **Fix #1**: Wykluczyć DINE_IN z `SocketStaffEventsHandler`
- [x] **Fix #2**: Dodać logi debugowe w `OrdersViewModel`
- [ ] **Fix #3**: Przetestować czy DINE_IN jest akceptowane
- [ ] **Test 1**: Sprawdzić logi z tagiem `AUTO_PRINT_DINE_IN`
- [ ] **Test 2**: Wyłączyć "Automatyczne drukowanie" i sprawdzić czy drukuje 1x
- [ ] **Test 3**: Wyłączyć "Auto-drukuj DINE_IN" i sprawdzić czy drukuje 0x
- [ ] **Dokumentacja**: Zaktualizować dokumentację ustawień

---

## 🧪 Testy

### Test 1: Podstawowy (po Fix #1)
1. Włącz: "Auto-drukuj DINE_IN" (kuchenna)
2. Wyłącz: "Automatyczne drukowanie"
3. Wyłącz: "Drukowanie po zaakceptowaniu"
4. Wyślij zamówienie DINE_IN

**Oczekiwane**: 1 wydruk na kuchni

### Test 2: Z Automatycznym Drukowaniem
1. Włącz: "Auto-drukuj DINE_IN" (kuchenna)
2. Włącz: "Automatyczne drukowanie"
3. Wyślącz: "Drukowanie po zaakceptowaniu"
4. Wyślij zamówienie DINE_IN

**Oczekiwane**: 1 wydruk na kuchni (Fix #1 powinien zapobiec duplikacji)

### Test 3: Wszystko Włączone
1. Włącz: "Auto-drukuj DINE_IN" (kuchenna)
2. Włącz: "Automatyczne drukowanie"
3. Włącz: "Drukowanie po zaakceptowaniu"
4. Wyślij zamówienie DINE_IN

**Oczekiwane**: 1 wydruk na kuchni (DINE_IN nie powinno być akceptowane)

---

## 📝 Zalecenia

### Krótkoterminowe
1. ✅ Zastosuj Fix #1 (wykluczenie DINE_IN)
2. 📊 Sprawdź logi z `AUTO_PRINT_DINE_IN` tag
3. 🧪 Przetestuj wszystkie 3 scenariusze

### Długoterminowe
1. **Refaktoryzacja**: Scentralizować logikę automatycznego drukowania w jednym miejscu
2. **Konfiguracja**: Dodać UI do zarządzania priorytetami drukowania
3. **Dokumentacja**: Opisać która opcja ma priorytet
4. **Walidacja**: Zapobiec konfliktom ustawień (np. ostrzegać gdy włączone 2 opcje dla tego samego typu zamówienia)

---

## 🎯 Podsumowanie

**Problem**: 3x drukowanie DINE_IN na kuchni

**Przyczyna**: 
- SocketStaffEventsHandler drukuje wszystkie nowe zamówienia (w tym DINE_IN)
- OrdersViewModel drukuje DINE_IN dedykowaną logiką
- Możliwa dodatkowa duplikacja przy akceptacji

**Rozwiązanie**:
- ✅ Wykluczyć DINE_IN z ogólnego auto-printu
- 📊 Dodać logi do diagnostyki
- 🧪 Przetestować nowe zachowanie

---

**Data**: 2026-02-03  
**Status**: Fix #1 i #2 zaimplementowane, czekam na testy  
**Następny krok**: Sprawdź logi po wysłaniu nowego zamówienia DINE_IN

