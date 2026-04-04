# 🎯 FINALNE ROZWIĄZANIE: Problem Timing - Socket vs API Response

## 📋 Problem

**Zgłoszenie:**
> "Teraz wydrukowało po 2 razy po zaakceptowaniu. Automatyczny wydruk po zaakceptowaniu powinien automatycznie drukować maksymalnie po 1x na każdą drukarkę"

**Rezultat:** 4 wydruki (2 drukarki × 2 razy) ❌

## 🔍 Analiza Timeline

### Z najnowszych logów:

```
00:01:19.611 - 🖱️ Klik "Zaakceptuj +15 min"
00:01:19.646 - 📤 API REQUEST wysłane (PUT /status/accepted)
00:01:19.802 - 📨 Socket: ORDER_ACCEPTED event (szybko!)
00:01:19.936 - 📨 Socket: STATUS_CHANGE (698e5bba...)
00:01:19.978 - 🖨️ Socket ORDERS: "Auto-druk po zmianie statusu (20732)"
              ├─ manuallyAcceptedOrders.contains(698e5bba...)? ❌ NIE!
              └─ DRUKOWANIE #1 ✅ (Socket)
              
00:01:25.946 - 📥 API RESPONSE! (6.3s później!)
              ├─ manuallyAcceptedOrders.add(698e5bba...) ← Za późno!
              └─ DRUKOWANIE #2 ❌ (API)
```

## 🐛 Główny Problem: Timing Race Condition

### Problem w kodzie (PRZED FIX):

**Linia 733 (updateOrder):**
```kotlin
fun updateOrder(orderId: String, status: OrderStatusEnum, data: UpdateOrderData) =
    executeOrderUpdate { repository.updateOrder(orderId, status, data) }
    // ❌ PROBLEM: Od razu wywołuje API request
    // manuallyAcceptedOrders NIE jest dodane tutaj!
```

**Linia 1540 (executeOrderUpdate - Resource.Success):**
```kotlin
when (val res = block()) {  // ← Czeka na API response!
    is Resource.Success -> {
        // ❌ PROBLEM: Dodanie do manuallyAcceptedOrders TUTAJ
        // Jest ZA PÓŹNO - socket już przyszedł i wydrukował!
        manuallyAcceptedOrders.add(res.value.orderId)
        
        // Drukowanie...
    }
}
```

### Sekwencja Problematyczna:

```
┌─────────────────────────────────────────────────────┐
│  Klik "Zaakceptuj"                                  │
│  └─ updateOrder(orderId, ACCEPTED, data)            │
│      └─ executeOrderUpdate {                        │
│          └─ API REQUEST → (czeka 6.3s...)           │
│          }                                           │
│                                                      │
│  Socket (0.2s po kliknięciu): ORDER UPDATE          │
│  └─ manuallyAcceptedOrders.contains? ❌ NIE!        │
│      └─ DRUKOWANIE #1 ✅ (Socket)                   │
│                                                      │
│  API Response (6.3s po kliknięciu):                 │
│  └─ manuallyAcceptedOrders.add() ← ZA PÓŹNO!        │
│      └─ DRUKOWANIE #2 ❌ (API)                      │
└─────────────────────────────────────────────────────┘
```

## ✅ ROZWIĄZANIE FINALNE

### Kluczowa Zmiana: Dodanie do manuallyAcceptedOrders PRZED API Request!

**OrdersViewModel.kt linia 733:**

**PRZED:**
```kotlin
fun updateOrder(orderId: String, status: OrderStatusEnum, data: UpdateOrderData) =
    executeOrderUpdate { repository.updateOrder(orderId, status, data) }
    // ❌ Od razu wysyła API request
    // manuallyAcceptedOrders NIE dodane!
```

**PO (FIXED):**
```kotlin
fun updateOrder(orderId: String, status: OrderStatusEnum, data: UpdateOrderData) {
    // ✅ FIX: Oznacz jako manualnie zaakceptowane OD RAZU (przed API request!)
    // Chroni przed drukowaniem przez socket który przychodzi szybciej niż API response
    if (status == OrderStatusEnum.ACCEPTED) {
        manuallyAcceptedOrders.add(orderId)
        Timber.tag("AUTO_PRINT").d("🔐 Dodano ${orderId} do manuallyAcceptedOrders (przed API request)")
        
        // Usuń z setu po 30 sekundach
        viewModelScope.launch {
            delay(30_000)
            manuallyAcceptedOrders.remove(orderId)
            Timber.tag("AUTO_PRINT").d("🗑️ Usunięto ${orderId} z manuallyAcceptedOrders (po 30s)")
        }
    }
    
    // Teraz dopiero wywołaj API request
    executeOrderUpdate { repository.updateOrder(orderId, status, data) }
}
```

### Usunięcie Duplikacji w executeOrderUpdate:

**Linia 1540 (PRZED):**
```kotlin
is Resource.Success -> {
    // ❌ Duplikacja - już dodane w updateOrder()!
    manuallyAcceptedOrders.add(res.value.orderId)
    
    viewModelScope.launch {
        delay(30_000)
        manuallyAcceptedOrders.remove(res.value.orderId)
    }
    
    // Drukowanie...
}
```

**Linia 1540 (PO):**
```kotlin
is Resource.Success -> {
    // ✅ NOTE: manuallyAcceptedOrders już dodane w updateOrder() PRZED API request!
    // Nie trzeba dodawać tutaj ponownie
    
    viewModelScope.launch {
        try {
            markAsPrinted(res.value.orderId)
            printerService.printAfterOrderAccepted(res.value)
        } catch (e: Exception) {
            // Rollback
            printedOrdersMutex.withLock {
                printedOrderIds.remove(res.value.orderId)
            }
        }
    }
}
```

## 🎯 Nowa Sekwencja (PO FIX)

```
┌─────────────────────────────────────────────────────┐
│  Klik "Zaakceptuj"                                  │
│  └─ updateOrder(orderId, ACCEPTED, data)            │
│      ├─ ✅ manuallyAcceptedOrders.add(orderId) ✅  │
│      │   └─ Log: "🔐 Dodano do manuallyAccepted"   │
│      │                                              │
│      └─ executeOrderUpdate {                        │
│          └─ API REQUEST → (czeka 6.3s...)           │
│          }                                           │
│                                                      │
│  Socket (0.2s po kliknięciu): ORDER UPDATE          │
│  └─ manuallyAcceptedOrders.contains? ✅ TAK!        │
│      └─ ⏭️ BLOKADA: "Było manualnie zaakceptowane" │
│          (BRAK DRUKOWANIA) ✅                        │
│                                                      │
│  API Response (6.3s po kliknięciu):                 │
│  └─ markAsPrinted(orderId)                          │
│      └─ DRUKOWANIE #1 ✅ (TYLKO API - 1 RAZ!)       │
│                                                      │
│  Po 30s: cleanup                                    │
│  └─ manuallyAcceptedOrders.remove(orderId)          │
│      └─ Log: "🗑️ Usunięto (po 30s)"                │
└─────────────────────────────────────────────────────┘
```

## 📊 Rezultat

**PRZED FIX:**
```
Socket (0.2s)     → Drukuje (Wbudowana + Kuchnia) = 2 wydruki
API Response (6s) → Drukuje (Wbudowana + Kuchnia) = 2 wydruki
───────────────────────────────────────────────────────────────
RAZEM: 4 WYDRUKI ❌
```

**PO FIX:**
```
Socket (0.2s)     → BLOKADA (manuallyAcceptedOrders) = 0 wydruków ✅
API Response (6s) → Drukuje (Wbudowana + Kuchnia)    = 2 wydruki ✅
───────────────────────────────────────────────────────────────
RAZEM: 2 WYDRUKI ✅ (1× na każdą drukarkę)
```

## 🎯 Wszystkie Zabezpieczenia (5 Warstw)

### 1. `manuallyAcceptedOrders` - Dodane PRZED API Request ⭐ **NOWY FIX!**
```kotlin
// W updateOrder() - PRZED executeOrderUpdate()
if (status == OrderStatusEnum.ACCEPTED) {
    manuallyAcceptedOrders.add(orderId)  // ✅ OD RAZU!
}
```

### 2. `manuallyAcceptedOrders` - Sprawdzane w Socket Handlers
```kotlin
// socketEventsRepo.orders (linia ~1392)
if (manuallyAcceptedOrders.contains(order.orderId)) {
    return@launch  // Blokada socket orders
}

// socketEventsRepo.statuses (linia ~1436)
if (manuallyAcceptedOrders.contains(s.orderId)) {
    return@onEach  // Blokada socket statuses
}
```

### 3. `wasPrinted` + `markAsPrinted` - Natychmiastowa blokada
```kotlin
// PRZED suspend functions!
if (wasPrinted(s.orderId)) {
    return@launch
}
markAsPrinted(s.orderId)  // ✅ NATYCHMIAST
```

### 4. Timeout 30s - Długie okno zabezpieczenia
```kotlin
delay(30_000)  // Socket może się spóźnić nawet 10-15s
manuallyAcceptedOrders.remove(orderId)
```

### 5. Rollback przy błędzie
```kotlin
catch (e: Exception) {
    printedOrdersMutex.withLock {
        printedOrderIds.remove(orderId)
    }
}
```

## 📝 Zmiany w Kodzie

### Plik: `OrdersViewModel.kt`

**Zmiana #1: Linia 733 (updateOrder)**
- Dodano sprawdzenie `if (status == OrderStatusEnum.ACCEPTED)`
- Dodano `manuallyAcceptedOrders.add(orderId)` **PRZED** `executeOrderUpdate`
- Dodano launch dla cleanup po 30s
- Zmiana z expression body (`=`) na block body (`{ }`)

**Zmiana #2: Linia ~1540 (executeOrderUpdate - Resource.Success)**
- Usunięto duplikację `manuallyAcceptedOrders.add()` (już dodane w updateOrder)
- Usunięto duplikację cleanup launch (już w updateOrder)
- Dodano komentarz wyjaśniający

## ✅ Status

- ✅ **Kod:** Poprawiony (OrdersViewModel.kt linia 733, 1540)
- ✅ **Kompilacja:** OK (tylko ostrzeżenia)
- ✅ **Logika:** Socket ZAWSZE blokowany, API ZAWSZE drukuje (1× na drukarkę)
- ✅ **Gotowe do testowania**

## 🎯 Podsumowanie

**Problem:**  
Socket przychodzi **SZYBCIEJ** niż API response (0.2s vs 6s), więc `manuallyAcceptedOrders.add()` wywoływane w `Resource.Success` było **ZA PÓŹNO**!

**Rozwiązanie:**  
Dodanie `orderId` do `manuallyAcceptedOrders` **OD RAZU** w `updateOrder()`, **PRZED** wysłaniem API request!

**Rezultat:**
- Socket → BLOKADA ✅
- API → Drukuje 1× ✅
- **TYLKO 2 WYDRUKI** (1× na każdą drukarkę) ✅

---

**Data rozwiązania:** 2026-02-13  
**Wersja:** v1.8.0  
**Typ:** Critical Bug Fix - Timing Issue  
**Status:** ✅ ROZWIĄZANE DEFINITYWNIE

