# Kompletna Dokumentacja Automatycznego Drukowania Zamówień

## 📋 Spis Treści
1. [Przegląd Ustawień](#przegląd-ustawień)
2. [Macierz Decyzyjna Drukowania](#macierz-decyzyjna-drukowania)
3. [Szczegółowe Scenariusze](#szczegółowe-scenariusze)
4. [Flowcharty](#flowcharty)
5. [Przykłady](#przykłady)
6. [Rozwiązywanie Problemów](#rozwiązywanie-problemów)

---

## 🎛️ Przegląd Ustawień

### Opcje Automatycznego Drukowania

| Ustawienie | Lokalizacja | Opis | Typ Drukarki |
|------------|-------------|------|--------------|
| **Automatyczne drukowanie** | Ustawienia → Drukowanie | Drukuje wszystkie nowe zamówienia (PROCESSING) | Główna |
| **Drukuj po zaakceptowaniu** | Ustawienia → Drukowanie | Drukuje po zaakceptowaniu zamówienia | Główna |
| **Auto-drukuj DINE_IN** | Ustawienia → Drukowanie | Drukuje zamówienia DINE_IN/ROOM_SERVICE automatycznie | Główna/Kuchenna (wybór) |
| **Drukowanie na kuchni** | Ustawienia → Drukowanie | Dodatkowo drukuje na drukarce kuchennej po zaakceptowaniu | Kuchenna |

---

## 🎯 Macierz Decyzyjna Drukowania

### Legenda
- ✅ = Drukuje
- ❌ = Nie drukuje
- 🔄 = Zależy od dodatkowych warunków

### Tabela Scenariuszy

| # | Typ Zamówienia | Status | Auto-Print | Auto-DINE_IN | Po Akceptacji | Kuchnia | Rezultat |
|---|---------------|--------|------------|--------------|---------------|---------|----------|
| 1 | DELIVERY | PROCESSING | ✅ ON | ❌ OFF | ❌ OFF | ❌ OFF | **1x Główna** |
| 2 | DELIVERY | PROCESSING | ✅ ON | ❌ OFF | ❌ OFF | ✅ ON | **1x Główna** (kuchnia nie, bo nie zaakceptowane) |
| 3 | DELIVERY | ACCEPTED | ❌ OFF | ❌ OFF | ✅ ON | ❌ OFF | **1x Główna** |
| 4 | DELIVERY | ACCEPTED | ❌ OFF | ❌ OFF | ✅ ON | ✅ ON | **1x Główna + 1x Kuchnia** |
| 5 | DINE_IN | PROCESSING | ❌ OFF | ✅ ON (główna) | ❌ OFF | ❌ OFF | **1x Główna** |
| 6 | DINE_IN | PROCESSING | ❌ OFF | ✅ ON (kuchenna) | ❌ OFF | ❌ OFF | **1x Kuchnia** |
| 7 | DINE_IN | PROCESSING | ✅ ON | ✅ ON (kuchenna) | ❌ OFF | ❌ OFF | **1x Kuchnia** (auto-print wykluczony) |
| 8 | DINE_IN | ACCEPTED | ❌ OFF | ❌ OFF | ✅ ON | ✅ ON | **1x Główna + 1x Kuchnia** |
| 9 | ROOM_SERVICE | PROCESSING | ❌ OFF | ✅ ON (kuchenna) | ❌ OFF | ❌ OFF | **1x Kuchnia** |
| 10 | PICKUP | PROCESSING | ✅ ON | ❌ OFF | ❌ OFF | ❌ OFF | **1x Główna** |

---

## 📖 Szczegółowe Scenariusze

### Scenariusz 1: Nowe Zamówienie DELIVERY (Standard)

**Ustawienia:**
- ✅ Automatyczne drukowanie: ON
- ❌ Auto-drukuj DINE_IN: OFF
- ❌ Drukuj po zaakceptowaniu: OFF
- ❌ Drukowanie na kuchni: OFF

**Przepływ:**
```
1. Zamówienie DELIVERY przychodzi (status: PROCESSING)
   └─> SocketStaffEventsHandler.handleNewOrder()
       └─> Warunek: isProcessing && autoPrintEnabled && deliveryType != DINE_IN
           └─> ✅ DRUKUJ NA GŁÓWNEJ
```

**Rezultat:** **1 wydruk na drukarce głównej**

**Kod:** `SocketStaffEventsHandler.kt:311`
```kotlin
if (isProcessing && 
    order.deliveryType != OrderDelivery.DINE_IN &&
    order.deliveryType != OrderDelivery.ROOM_SERVICE) {
    val autoPrintEnabled = appPreferencesManager.getAutoPrintEnabled()
    if (autoPrintEnabled) {
        printerService.printOrder(order, useDeliveryInterval = true)
        // ✅ Drukuje na GŁÓWNEJ
    }
}
```

---

### Scenariusz 2: Zamówienie Po Zaakceptowaniu (DELIVERY)

**Ustawienia:**
- ❌ Automatyczne drukowanie: OFF
- ❌ Auto-drukuj DINE_IN: OFF
- ✅ Drukuj po zaakceptowaniu: ON
- ✅ Drukowanie na kuchni: ON

**Przepływ:**
```
1. Pracownik akceptuje zamówienie (status: PROCESSING → ACCEPTED)
   └─> OrdersViewModel.executeOrderUpdate()
       └─> Warunek: status == ACCEPTED && autoPrintAcceptedEnabled
           └─> printerService.printAfterOrderAccepted(order)
               ├─> ✅ DRUKUJ NA GŁÓWNEJ
               └─> Warunek: autoPrintKitchen == true
                   └─> ✅ DRUKUJ NA KUCHENNEJ
```

**Rezultat:** **2 wydruki (1x Główna + 1x Kuchnia)**

**Kod:** `OrdersViewModel.kt:1218` + `PrinterService.kt:204`
```kotlin
// OrdersViewModel
if (res.value.orderStatus.slug == OrderStatusEnum.ACCEPTED.name &&
    appPreferencesManager.getAutoPrintAcceptedEnabled()) {
    printerService.printAfterOrderAccepted(res.value)
}

// PrinterService
suspend fun printAfterOrderAccepted(order: Order) {
    // 1. Drukuj na standardowej
    standardTargets.forEach { printOne(it, order) }
    // ✅ Drukuje na GŁÓWNEJ
    
    // 2. Jeśli włączone, drukuj na kuchennej
    if (autoPrintKitchen) {
        kitchenTargets.forEach { printOne(it, order) }
        // ✅ Drukuje na KUCHENNEJ
    }
}
```

---

### Scenariusz 3: Zamówienie DINE_IN (Drukarka Główna)

**Ustawienia:**
- ❌ Automatyczne drukowanie: OFF
- ✅ Auto-drukuj DINE_IN: ON → **Drukarka główna**
- ❌ Drukuj po zaakceptowaniu: OFF
- ❌ Drukowanie na kuchni: OFF

**Przepływ:**
```
1. Zamówienie DINE_IN przychodzi (status: PROCESSING)
   └─> OrdersViewModel.observeSocketEvents()
       └─> Warunek: deliveryType == DINE_IN && autoPrintDineInEnabled
           └─> printerType = appPreferencesManager.getAutoPrintDineInPrinter()
               └─> printerType == "main"
                   └─> ✅ DRUKUJ NA GŁÓWNEJ
```

**Rezultat:** **1 wydruk na drukarce głównej**

**Kod:** `OrdersViewModel.kt:1150`
```kotlin
if ((order.deliveryType == OrderDelivery.DINE_IN || 
     order.deliveryType == OrderDelivery.ROOM_SERVICE) &&
    appPreferencesManager.getAutoPrintDineInEnabled()) {
    
    val printerType = appPreferencesManager.getAutoPrintDineInPrinter()
    when (printerType) {
        "kitchen" -> printerService.printKitchenTicket(order)
        else -> printerService.printOrder(order)
        // ✅ Drukuje na GŁÓWNEJ
    }
}
```

---

### Scenariusz 4: Zamówienie DINE_IN (Drukarka Kuchenna)

**Ustawienia:**
- ❌ Automatyczne drukowanie: OFF
- ✅ Auto-drukuj DINE_IN: ON → **Drukarka kuchenna**
- ❌ Drukuj po zaakceptowaniu: OFF
- ❌ Drukowanie na kuchni: OFF

**Przepływ:**
```
1. Zamówienie DINE_IN przychodzi (status: PROCESSING)
   └─> OrdersViewModel.observeSocketEvents()
       └─> Warunek: deliveryType == DINE_IN && autoPrintDineInEnabled
           └─> printerType = "kitchen"
               └─> ✅ DRUKUJ NA KUCHENNEJ
```

**Rezultat:** **1 wydruk na drukarce kuchennej**

**Kod:** `OrdersViewModel.kt:1150`
```kotlin
val printerType = appPreferencesManager.getAutoPrintDineInPrinter()
when (printerType) {
    "kitchen" -> printerService.printKitchenTicket(order)
    // ✅ Drukuje na KUCHENNEJ
    else -> printerService.printOrder(order)
}
```

---

### Scenariusz 5: DINE_IN + Automatyczne Drukowanie (Konflikt Unikany)

**Ustawienia:**
- ✅ Automatyczne drukowanie: ON
- ✅ Auto-drukuj DINE_IN: ON → Drukarka kuchenna
- ❌ Drukuj po zaakceptowaniu: OFF
- ❌ Drukowanie na kuchni: OFF

**Przepływ:**
```
1. Zamówienie DINE_IN przychodzi (status: PROCESSING)
   
   A. SocketStaffEventsHandler.handleNewOrder()
      └─> Warunek: deliveryType == DINE_IN
          └─> ❌ POMIŃ (wykluczenie DINE_IN)
   
   B. OrdersViewModel.observeSocketEvents()
      └─> Warunek: deliveryType == DINE_IN && autoPrintDineInEnabled
          └─> ✅ DRUKUJ NA KUCHENNEJ
```

**Rezultat:** **1 wydruk na drukarce kuchennej** (bez duplikacji!)

**Kod zabezpieczający:** `SocketStaffEventsHandler.kt:311`
```kotlin
// WYKLUCZENIE DINE_IN/ROOM_SERVICE z ogólnego auto-printu
if (isProcessing && 
    order.deliveryType != OrderDelivery.DINE_IN &&
    order.deliveryType != OrderDelivery.ROOM_SERVICE) {
    // ❌ DINE_IN jest pomijane tutaj
    if (autoPrintEnabled) {
        printerService.printOrder(order)
    }
}
```

---

### Scenariusz 6: Wszystkie Opcje Włączone (DELIVERY)

**Ustawienia:**
- ✅ Automatyczne drukowanie: ON
- ❌ Auto-drukuj DINE_IN: OFF (nie dotyczy DELIVERY)
- ✅ Drukuj po zaakceptowaniu: ON
- ✅ Drukowanie na kuchni: ON

**Przepływ:**
```
1. Nowe zamówienie DELIVERY (PROCESSING)
   └─> SocketStaffEventsHandler
       └─> ✅ DRUKUJ NA GŁÓWNEJ (#1)

2. Pracownik akceptuje (PROCESSING → ACCEPTED)
   └─> OrdersViewModel.executeOrderUpdate()
       └─> printerService.printAfterOrderAccepted()
           ├─> ✅ DRUKUJ NA GŁÓWNEJ (#2)
           └─> ✅ DRUKUJ NA KUCHENNEJ (#3)
```

**Rezultat:** **3 wydruki (2x Główna + 1x Kuchnia)**
- Przy nowym zamówieniu: 1x Główna
- Po zaakceptowaniu: 1x Główna + 1x Kuchnia

---

### Scenariusz 7: ROOM_SERVICE (Identycznie jak DINE_IN)

**Ustawienia:**
- ❌ Automatyczne drukowanie: OFF
- ✅ Auto-drukuj DINE_IN: ON → Drukarka kuchenna
- ❌ Drukuj po zaakceptowaniu: OFF
- ❌ Drukowanie na kuchni: OFF

**Przepływ:**
```
1. Zamówienie ROOM_SERVICE przychodzi
   └─> OrdersViewModel.observeSocketEvents()
       └─> Warunek: deliveryType == ROOM_SERVICE
           └─> ✅ DRUKUJ NA KUCHENNEJ
```

**Rezultat:** **1 wydruk na drukarce kuchennej**

**Uwaga:** ROOM_SERVICE jest traktowane identycznie jak DINE_IN

---

## 📊 Flowcharty

### Flow 1: Nowe Zamówienie Przychodzi

```
┌─────────────────────────┐
│ Nowe zamówienie socket  │
└──────────┬──────────────┘
           │
           ▼
    ┌──────────────┐
    │ deliveryType?│
    └──┬───────┬───┘
       │       │
  DINE_IN   DELIVERY/
  ROOM_     PICKUP/etc.
  SERVICE      │
       │       │
       │       ▼
       │  ┌─────────────────┐
       │  │ Auto-print ON?  │
       │  └────┬──────┬─────┘
       │      YES    NO
       │       │      │
       │       ▼      └──> ❌ KONIEC
       │  ┌─────────────┐
       │  │DRUKUJ GŁÓWNA│
       │  └─────────────┘
       │
       ▼
  ┌──────────────────┐
  │Auto-DINE_IN ON?  │
  └────┬──────┬──────┘
      YES    NO
       │      └──> ❌ KONIEC
       ▼
  ┌─────────────┐
  │ Printer?    │
  └──┬──────┬───┘
     │      │
  "main" "kitchen"
     │      │
     ▼      ▼
┌─────────┐ ┌─────────┐
│GŁÓWNA   │ │KUCHNIA  │
└─────────┘ └─────────┘
```

### Flow 2: Akceptacja Zamówienia

```
┌────────────────────────┐
│ Akceptacja zamówienia  │
│ (PROCESSING→ACCEPTED)  │
└──────────┬─────────────┘
           │
           ▼
    ┌─────────────────────┐
    │Auto-print after     │
    │acceptance ON?       │
    └────┬──────┬─────────┘
        YES    NO
         │      └──> ❌ KONIEC
         ▼
    ┌─────────────┐
    │DRUKUJ GŁÓWNA│
    └──────┬──────┘
           │
           ▼
    ┌─────────────────┐
    │Print kitchen ON?│
    └────┬──────┬─────┘
        YES    NO
         │      └──> ✅ KONIEC
         ▼
    ┌──────────────┐
    │DRUKUJ KUCHNIA│
    └──────────────┘
```

---

## 🔍 Priorytety i Wykluczenia

### Reguła 1: DINE_IN/ROOM_SERVICE ma Wyższy Priorytet

Gdy zamówienie jest typu **DINE_IN** lub **ROOM_SERVICE**:
- ❌ **NIE** jest drukowane przez "Automatyczne drukowanie"
- ✅ **JEST** drukowane tylko przez "Auto-drukuj DINE_IN" (jeśli włączone)

**Uzasadnienie:** Unikanie duplikacji - DINE_IN ma dedykowaną logikę

### Reguła 2: Drukowanie Po Akceptacji Jest Niezależne

"Drukuj po zaakceptowaniu" działa dla **WSZYSTKICH** typów zamówień, w tym DINE_IN:
- Jeśli pracownik zaakceptuje zamówienie DINE_IN i ma włączone "Drukuj po zaakceptowaniu"
- Wykona się dodatkowe drukowanie (główna + opcjonalnie kuchnia)

**Uwaga:** W przypadku DINE_IN to może być nieoczekiwane zachowanie!

### Reguła 3: "Drukowanie na kuchni" Działa Tylko Po Akceptacji

Opcja "Drukowanie na kuchni":
- ✅ Działa tylko w połączeniu z "Drukuj po zaakceptowaniu"
- ❌ NIE działa z "Automatyczne drukowanie"
- ❌ NIE działa z "Auto-drukuj DINE_IN"

---

## 💡 Przykłady Konfiguracji

### Konfiguracja A: Restauracja z Kuchnią

**Cel:** 
- Wszystkie nowe zamówienia → drukarka główna (kasjer)
- Po zaakceptowaniu → dodatkowo drukarka kuchenna
- DINE_IN → bezpośrednio do kuchni (bez akceptacji)

**Ustawienia:**
```
✅ Automatyczne drukowanie: ON
✅ Drukuj po zaakceptowaniu: ON
✅ Auto-drukuj DINE_IN: ON → Kuchnia
✅ Drukowanie na kuchni: ON
```

**Rezultat:**
- DELIVERY: 2x Główna (nowe + akceptacja) + 1x Kuchnia (po akceptacji) = **3 wydruki**
- DINE_IN: 1x Kuchnia (automatycznie) = **1 wydruk**

---

### Konfiguracja B: Mała Restauracja (1 Drukarka)

**Cel:**
- Wszystkie zamówienia drukują się automatycznie
- Jedna drukarka obsługuje wszystko

**Ustawienia:**
```
✅ Automatyczne drukowanie: ON
❌ Drukuj po zaakceptowaniu: OFF
✅ Auto-drukuj DINE_IN: ON → Główna
❌ Drukowanie na kuchni: OFF
```

**Rezultat:**
- DELIVERY: 1x Główna = **1 wydruk**
- DINE_IN: 1x Główna = **1 wydruk**

---

### Konfiguracja C: Tylko Manualne Drukowanie

**Cel:**
- Bez automatycznego drukowania
- Pracownik drukuje ręcznie po zaakceptowaniu

**Ustawienia:**
```
❌ Automatyczne drukowanie: OFF
✅ Drukuj po zaakceptowaniu: ON
❌ Auto-drukuj DINE_IN: OFF
❌ Drukowanie na kuchni: OFF
```

**Rezultat:**
- DELIVERY: 0 (nowe), 1x Główna (po akceptacji) = **1 wydruk**
- DINE_IN: 0 (nowe), 1x Główna (po akceptacji) = **1 wydruk**

---

### Konfiguracja D: DINE_IN Osobno, Reszta Normalnie

**Cel:**
- DINE_IN → kuchnia bez akceptacji
- DELIVERY/PICKUP → normalny przepływ z akceptacją

**Ustawienia:**
```
❌ Automatyczne drukowanie: OFF
✅ Drukuj po zaakceptowaniu: ON
✅ Auto-drukuj DINE_IN: ON → Kuchnia
✅ Drukowanie na kuchni: ON
```

**Rezultat:**
- DELIVERY: 0 (nowe), 1x Główna + 1x Kuchnia (po akceptacji) = **2 wydruki**
- DINE_IN: 1x Kuchnia (automatycznie) = **1 wydruk**

---

## 🚨 Rozwiązywanie Problemów

### Problem 1: Zamówienie Drukuje Się Wielokrotnie

**Objawy:** To samo zamówienie drukuje się 2-3 razy

**Diagnoza:**
1. Sprawdź logi z tagiem `AUTO_PRINT_DINE_IN`
2. Sprawdź które ustawienia są włączone
3. Sprawdź typ zamówienia (DINE_IN vs DELIVERY)

**Możliwe Przyczyny:**

#### A. DINE_IN z włączonym "Automatyczne drukowanie"
**Rozwiązanie:** To już naprawione! DINE_IN jest wykluczony z ogólnego auto-printu.

#### B. Wszystkie opcje włączone dla DELIVERY
```
Automatyczne drukowanie: ON     → +1 wydruk
Drukuj po zaakceptowaniu: ON    → +2 wydruki (główna + kuchnia)
= 3 wydruki RAZEM
```
**Rozwiązanie:** Wyłącz "Automatyczne drukowanie" jeśli chcesz tylko po akceptacji.

#### C. DINE_IN przypadkowo zaakceptowane
```
Auto-drukuj DINE_IN: ON         → +1 wydruk
Drukuj po zaakceptowaniu: ON    → +2 wydruki
= 3 wydruki RAZEM
```
**Rozwiązanie:** Nie akceptuj zamówień DINE_IN ręcznie (są już wydrukowane).

---

### Problem 2: Zamówienie Nie Drukuje Się Wcale

**Diagnoza:**
1. Sprawdź czy drukarka jest włączona (`enabled = true`)
2. Sprawdź czy odpowiednie ustawienie jest ON
3. Sprawdź logi błędów drukowania

**Możliwe Przyczyny:**

#### A. Wszystkie opcje wyłączone
**Rozwiązanie:** Włącz przynajmniej jedną opcję auto-druku.

#### B. DINE_IN ale wyłączone "Auto-drukuj DINE_IN"
**Rozwiązanie:** Włącz "Auto-drukuj DINE_IN" lub włącz "Drukuj po zaakceptowaniu".

#### C. Drukarka wyłączona w zarządzaniu drukarkami
**Rozwiązanie:** Włącz drukarkę w Ustawienia → Drukarki.

---

### Problem 3: Drukuje na Złej Drukarce

**Diagnoza:**
1. Sprawdź ustawienie "Auto-drukuj DINE_IN" → Wybór drukarki
2. Sprawdź czy masz skonfigurowaną drukarkę kuchenną

**Możliwe Przyczyny:**

#### A. DINE_IN drukuje na głównej zamiast kuchennej
**Rozwiązanie:** 
- Otwórz Ustawienia → Drukowanie
- W sekcji "Auto-drukuj DINE_IN" wybierz "Drukarka kuchenna"

#### B. Brak drukarki kuchennej
**Rozwiązanie:**
- Skonfiguruj drukarkę kuchenną w Zarządzanie drukarkami
- Ustaw typ drukarki na "KITCHEN"

---

## 📝 Kod Źródłowy - Lokalizacje

### 1. SocketStaffEventsHandler.kt
**Linia:** 311  
**Funkcja:** `handleNewOrder()`  
**Odpowiada za:** Automatyczne drukowanie nowych zamówień (DELIVERY, PICKUP, etc.)

```kotlin
if (isProcessing && 
    order.deliveryType != OrderDelivery.DINE_IN &&
    order.deliveryType != OrderDelivery.ROOM_SERVICE) {
    val autoPrintEnabled = appPreferencesManager.getAutoPrintEnabled()
    if (autoPrintEnabled) {
        printerService.printOrder(order, useDeliveryInterval = true)
    }
}
```

---

### 2. OrdersViewModel.kt (observeSocketEvents)
**Linia:** 1150  
**Funkcja:** `observeSocketEvents()`  
**Odpowiada za:** Automatyczne drukowanie DINE_IN/ROOM_SERVICE

```kotlin
if ((order.deliveryType == OrderDelivery.DINE_IN || 
     order.deliveryType == OrderDelivery.ROOM_SERVICE) &&
    appPreferencesManager.getAutoPrintDineInEnabled()) {
    
    val printerType = appPreferencesManager.getAutoPrintDineInPrinter()
    when (printerType) {
        "kitchen" -> printerService.printKitchenTicket(order)
        else -> printerService.printOrder(order)
    }
}
```

---

### 3. OrdersViewModel.kt (executeOrderUpdate)
**Linia:** 1218  
**Funkcja:** `executeOrderUpdate()`  
**Odpowiada za:** Drukowanie po zaakceptowaniu

```kotlin
if (res.value.orderStatus.slug == OrderStatusEnum.ACCEPTED.name &&
    appPreferencesManager.getAutoPrintAcceptedEnabled()) {
    printerService.printAfterOrderAccepted(res.value)
}
```

---

### 4. PrinterService.kt
**Linia:** 204  
**Funkcja:** `printAfterOrderAccepted()`  
**Odpowiada za:** Drukowanie na głównej + opcjonalnie kuchennej

```kotlin
suspend fun printAfterOrderAccepted(order: Order) {
    // 1. Drukuj na standardowej
    standardTargets.forEach { printOne(it, order) }
    
    // 2. Drukuj na kuchennej (jeśli włączone)
    if (autoPrintKitchen) {
        kitchenTargets.forEach { printOne(it, order) }
    }
}
```

---

## 🎓 Najlepsze Praktyki

### ✅ Zalecane Konfiguracje

**Dla restauracji z kuchnią:**
```
✅ Automatyczne drukowanie: OFF
✅ Drukuj po zaakceptowaniu: ON
✅ Auto-drukuj DINE_IN: ON → Kuchnia
✅ Drukowanie na kuchni: ON
```

**Dla małego lokalu (1 drukarka):**
```
✅ Automatyczne drukowanie: ON
❌ Drukuj po zaakceptowaniu: OFF
✅ Auto-drukuj DINE_IN: ON → Główna
❌ Drukowanie na kuchni: OFF
```

### ❌ Niezalecane Kombinacje

**Konflikt 1: Wszystko włączone**
```
⚠️ Automatyczne drukowanie: ON
⚠️ Drukuj po zaakceptowaniu: ON
```
**Problem:** Każde zamówienie drukuje się 2 razy (nowe + akceptacja)

**Konflikt 2: DINE_IN bez dedykowanej opcji**
```
⚠️ Auto-drukuj DINE_IN: OFF
⚠️ Drukuj po zaakceptowaniu: OFF
```
**Problem:** DINE_IN nie drukuje się wcale automatycznie

---

## 📅 Historia Zmian

### v1.1 (2026-02-03)
- ✅ Naprawiono duplikację DINE_IN
- ✅ Dodano wykluczenie DINE_IN/ROOM_SERVICE z SocketStaffEventsHandler
- ✅ Dodano logi debugowe `AUTO_PRINT_DINE_IN`

### v1.0 (2026-02-02)
- Dodano opcję "Auto-drukuj DINE_IN"
- Dodano wybór drukarki dla DINE_IN (główna/kuchenna)

---

**Data:** 2026-02-03  
**Wersja:** 1.1  
**Autor:** AI Assistant  
**Status:** ✅ Aktualna i przetestowana

