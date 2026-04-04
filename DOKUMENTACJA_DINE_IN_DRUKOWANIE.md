# 📖 Dokumentacja: Zamówienia DINE_IN/ROOM_SERVICE - Jak Działają?

## 🎯 Przegląd

Zamówienia **DINE_IN** (na miejscu) i **ROOM_SERVICE** (obsługa pokojowa) mają **specjalną logikę drukowania**, która **omija akceptację**.

---

## 🔍 Kluczowa Różnica

### DELIVERY/PICKUP (normalne zamówienia)
```
Nowe PROCESSING → Czeka na akceptację → Po zaakceptowaniu → Drukuje
```

### DINE_IN/ROOM_SERVICE (na miejscu)
```
Nowe ACCEPTED → Drukuje NATYCHMIAST (bez akceptacji!)
```

**Dlaczego?**
- Zamówienia na miejscu **przychodzą już zaakceptowane** z backendu
- Nie wymagają potwierdzenia przez personel
- Muszą być wydrukowane od razu (kuchnia czeka!)

---

## 📊 Twój Przypadek: Wszystkie Opcje Zaznaczone

### Twoje Ustawienia
```
✅ Automatyczne drukowanie: ON
✅ Drukowanie po zaakceptowaniu: ON
✅ Auto-drukuj zamówienia na miejscu: ON → Drukarka kuchenna
✅ Drukowanie na kuchni: ON
```

### Co się dzieje gdy przychodzi DINE_IN (status ACCEPTED)?

#### Ścieżka 1: AUTO-DRUK DINE_IN (socketEventsRepo.orders)

**Kod:** `OrdersViewModel.kt` linia ~1205

```kotlin
// Socket wysyła zamówienie
socketEventsRepo.orders.onEach { order ->
    
    // Sprawdzenie typu zamówienia
    if (order.deliveryType == DINE_IN && 
        appPreferencesManager.getAutoPrintDineInEnabled()) {  // ✅ TRUE
        
        viewModelScope.launch {
            // Sprawdź czy już było wydrukowane
            if (wasPrinted(order.orderId)) {
                Timber.d("⏭️ Pomijam - już było wydrukowane")
                return@launch
            }
            
            // Oznacz jako wydrukowane (ATOMOWO - Mutex)
            markAsPrinted(order.orderId)
            
            // Pobierz ustawienie drukarki
            val printerType = getAutoPrintDineInPrinter() // "kitchen"
            
            when (printerType) {
                "kitchen" -> printerService.printKitchenTicket(order) // ✅ DRUKUJE
                else -> printerService.printOrder(order)
            }
        }
    }
}
```

**Wyzwalacz:** Socket wysyła nowe zamówienie DINE_IN  
**Warunek:** `deliveryType == DINE_IN` + "Auto-drukuj zamówienia na miejscu" = ON  
**Drukarka:** Kuchenna (zgodnie z ustawieniem)  
**Ile razy:** **1x** (dzięki Mutex i `wasPrinted()`)

---

#### Ścieżka 2: AUTO-DRUK ACCEPTED (socketEventsRepo.orders) - WYKLUCZONY!

**Kod:** `OrdersViewModel.kt` linia ~1255

```kotlin
// Ta sama ścieżka socket - kontynuacja
if (orderStatus == OrderStatusEnum.ACCEPTED &&
    order.deliveryType != OrderDelivery.DINE_IN &&  // ❌ WYKLUCZONY!
    order.deliveryType != OrderDelivery.ROOM_SERVICE &&  // ❌ WYKLUCZONY!
    appPreferencesManager.getAutoPrintAcceptedEnabled()) {
    
    // Ta ścieżka NIE wykona się dla DINE_IN!
    printerService.printAfterOrderAccepted(order)
}
```

**Wyzwalacz:** Socket wysyła zamówienie ACCEPTED  
**Warunek:** Status ACCEPTED + **NIE** DINE_IN + **NIE** ROOM_SERVICE  
**Dla DINE_IN:** ❌ **WYKLUCZONY** (warunek nie spełniony)  

---

#### Ścieżka 3: socketEventsRepo.statuses - NIE DOTYCZY

**Kod:** `OrdersViewModel.kt` linia ~1310

```kotlin
socketEventsRepo.statuses.onEach { s ->
    // Ta ścieżka wykrywa ZMIANĘ statusu (PROCESSING → ACCEPTED)
    // DINE_IN przychodzi JUŻ jako ACCEPTED → brak zmiany statusu
    
    if (s.newStatus == OrderStatusEnum.ACCEPTED) {
        // NIE wykona się dla nowych DINE_IN (nie ma zmiany statusu)
    }
}
```

**Wyzwalacz:** Socket wysyła ZMIANĘ statusu  
**Dla DINE_IN:** ❌ **NIE DOTYCZY** (nie ma zmiany, przychodzi już jako ACCEPTED)

---

## 📋 Podsumowanie Przepływu

### Zamówienie DINE_IN przychodzi (status ACCEPTED)

```
00:00.000 - Backend tworzy zamówienie DINE_IN (już jako ACCEPTED)
              └─> Socket wysyła event do aplikacji

00:00.100 - socketEventsRepo.orders emituje zamówienie
              ├─> Zapisuje do bazy
              └─> Wykrywa: deliveryType == DINE_IN ✅

00:00.200 - Sprawdzenie warunków:
              ├─> Auto-drukuj zamówienia na miejscu? → ON ✅
              ├─> wasPrinted(orderId)? → NIE ✅
              └─> markAsPrinted(orderId) → Dodaje do setu

00:00.300 - Drukowanie:
              ├─> Pobiera ustawienie: "kitchen" ✅
              └─> printKitchenTicket(order) → DRUKUJE NA KUCHNI

00:00.500 - Kończy:
              └─> ✅ 1x Wydruk na kuchni (zgodnie z ustawieniem)

POZOSTAŁE ŚCIEŻKI:
  ├─> AUTO-DRUK ACCEPTED → Wykluczony (warunek: != DINE_IN)
  └─> socketEventsRepo.statuses → Nie dotyczy (brak zmiany statusu)

RAZEM: 1x Kuchnia ✅
```

---

## ❓ Dlaczego TYLKO 1x Kuchnia?

### Twoje Ustawienia vs Rzeczywistość

**Ustawienia:**
```
✅ Automatyczne drukowanie: ON
✅ Drukowanie po zaakceptowaniu: ON
✅ Auto-drukuj zamówienia na miejscu: ON → Kuchenna
✅ Drukowanie na kuchni: ON
```

**Ale dla DINE_IN:**

| Ustawienie | Czy Działa? | Dlaczego? |
|------------|-------------|-----------|
| Automatyczne drukowanie | ❌ NIE | DINE_IN wykluczony (SocketStaffEventsHandler) |
| Drukowanie po zaakceptowaniu | ❌ NIE | DINE_IN wykluczony (warunek `!= DINE_IN`) |
| **Auto-drukuj zamówienia na miejscu** | ✅ **TAK** | **To jest właściwa opcja!** |
| Drukowanie na kuchni | ⚠️ Ignorowane | DINE_IN ma własny wybór drukarki |

**Wniosek:** Dla DINE_IN **TYLKO** "Auto-drukuj zamówienia na miejscu" ma znaczenie!

---

## 🔧 Jak To Działa Techniczne

### 1. Socket Wysyła Zamówienie

**Backend → Socket:**
```json
{
  "orderId": "DINE-123",
  "orderNumber": "00420505",
  "deliveryType": "DINE_IN",
  "orderStatus": { "slug": "ACCEPTED" },
  ...
}
```

---

### 2. Aplikacja Odbiera (socketEventsRepo.orders)

**OrdersViewModel.kt:**
```kotlin
socketEventsRepo.orders.onEach { order ->
    // order.deliveryType = DINE_IN ✅
    // order.orderStatus.slug = ACCEPTED ✅
    
    repository.insertOrUpdateOrder(order) // Zapisz do bazy
}
```

---

### 3. Sprawdzenie Warunków

**Warunek 1: Typ zamówienia**
```kotlin
if (order.deliveryType == OrderDelivery.DINE_IN) // ✅ TRUE
```

**Warunek 2: Ustawienie włączone**
```kotlin
if (appPreferencesManager.getAutoPrintDineInEnabled()) // ✅ TRUE
```

**Warunek 3: Nie było wydrukowane (Mutex!)**
```kotlin
viewModelScope.launch {
    if (wasPrinted(order.orderId)) { // ❌ FALSE (pierwsze wydrukowanie)
        return@launch
    }
    
    // ✅ Warunki spełnione - kontynuuj
}
```

---

### 4. Oznaczenie jako Wydrukowane (Thread-Safe)

```kotlin
markAsPrinted(order.orderId)

// Wewnętrznie:
printedOrdersMutex.withLock {
    printedOrderIds.add(order.orderId) // Atomowo
}

// Dodatkowo:
viewModelScope.launch {
    appPreferencesManager.addPrintedOrderId(order.orderId) // DataStore
}
```

---

### 5. Drukowanie

```kotlin
val printerType = appPreferencesManager.getAutoPrintDineInPrinter() // "kitchen"

when (printerType) {
    "kitchen" -> printerService.printKitchenTicket(order) // ✅ WYKONUJE SIĘ
    else -> printerService.printOrder(order)
}
```

**Rezultat:** Drukuje na **kuchennej drukarce** (szablon kuchenny)

---

## 🧪 Logi Diagnostyczne

### Oczekiwane Logi (Sukces)

```
📥 Received order from socket: orderId=DINE-123, externalDelivery.status=null
💾 Order saved to database: orderId=DINE-123

AUTO_PRINT_DINE_IN: 🔔 Warunek auto-druku DINE_IN spełniony dla 00420505
AUTO_PRINT_DINE_IN:    deliveryType=DINE_IN

🖨️ OrdersViewModel: Auto-druk DINE_IN/ROOM_SERVICE dla 00420505 na drukarce: kitchen
💾 Zapisano wydrukowane zamówienie do DataStore: DINE-123

(Drukowanie na drukarce...)

✅ Auto-druk DINE_IN/ROOM_SERVICE zakończony dla 00420505
```

---

### Logi do Sprawdzenia

```bash
# Sprawdź czy DINE_IN jest wykrywany
adb logcat | grep "AUTO_PRINT_DINE_IN"

# Sprawdź czy drukuje
adb logcat | grep "Auto-druk DINE_IN/ROOM_SERVICE"

# Sprawdź czy zapisuje do DataStore
adb logcat | grep "💾 Zapisano wydrukowane zamówienie"
```

---

## ⚠️ Częste Problemy

### Problem 1: DINE_IN NIE drukuje się

**Możliwe przyczyny:**

1. **"Auto-drukuj zamówienia na miejscu" = OFF**
   - Sprawdź: Ustawienia → Drukowanie → Auto-drukuj zamówienia na miejscu

2. **Zamówienie już było wydrukowane**
   - Log: `⏭️ Pomijam - zamówienie już było wydrukowane`
   - Rozwiązanie: Normalne (zapobiega duplikacji)

3. **Brak drukarki kuchennej**
   - Sprawdź: Ustawienia → Drukarki → Kuchnia

4. **Błąd drukowania**
   - Log: `❌ Błąd auto-druku DINE_IN/ROOM_SERVICE`
   - Sprawdź połączenie z drukarką

---

### Problem 2: DINE_IN drukuje się 2x lub więcej

**Możliwe przyczyny:**

1. **Przed v1.7.2 (brak Mutex)**
   - Race condition między socketEventsRepo.orders
   - Rozwiązanie: Aktualizuj do v1.7.2

2. **Resetowanie printedOrderIds**
   - Force kill aplikacji → traci pamięć
   - Rozwiązanie: v1.7+ ma DataStore (przetrwa restart)

3. **Socket wysyła duplikaty**
   - Backend problem
   - Log: Sprawdź ile razy przychodzi to samo orderId

---

### Problem 3: DINE_IN drukuje na złej drukarce

**Sprawdź ustawienie:**
```
Ustawienia → Drukowanie → Auto-drukuj zamówienia na miejscu
  └─> Wybór drukarki: [Główna] lub [Kuchenna]
```

**Kod sprawdzający:**
```kotlin
val printerType = appPreferencesManager.getAutoPrintDineInPrinter()
// Powinno zwrócić: "kitchen" lub "main"
```

---

## 📊 Tabela Decyzyjna - DINE_IN

| Status | Auto DINE_IN | Drukarka | Rezultat |
|--------|--------------|----------|----------|
| PROCESSING | ON | Kuchnia | ❌ NIE drukuje (czeka na ACCEPTED) |
| ACCEPTED | OFF | - | ❌ NIE drukuje (opcja wyłączona) |
| ACCEPTED | ON | Kuchnia | ✅ **Drukuje 1x na kuchni** |
| ACCEPTED | ON | Główna | ✅ Drukuje 1x na głównej |

---

## 🎓 Best Practices dla DINE_IN

### ✅ Zalecana Konfiguracja

```
Ustawienia dla DINE_IN/ROOM_SERVICE:

✅ Auto-drukuj zamówienia na miejscu: ON
   └─> Wybierz: Drukarka Kuchenna

❌ NIE akceptuj ręcznie zamówień DINE_IN!
   (przychodzą już zaakceptowane)

⚠️ "Drukowanie po zaakceptowaniu": ON/OFF
   (nie ma znaczenia dla DINE_IN - są wykluczeni)
```

---

### ❌ Częste Błędy

1. **Ręczna akceptacja DINE_IN**
   - DINE_IN przychodzi już ACCEPTED
   - Ręczna akceptacja może spowodować duplikację

2. **Wyłączenie "Auto-drukuj zamówienia na miejscu"**
   - DINE_IN nie będzie drukował się wcale!
   - Kuchnia nie dostanie zamówienia

3. **Mylenie z "Drukowanie po zaakceptowaniu"**
   - "Drukowanie po zaakceptowaniu" = dla DELIVERY/PICKUP
   - "Auto-drukuj zamówienia na miejscu" = dla DINE_IN/ROOM_SERVICE

---

## 🔑 Kluczowe Punkty

### 1. DINE_IN Ma Własną Ścieżkę
- **NIE** używa "Automatyczne drukowanie"
- **NIE** używa "Drukowanie po zaakceptowaniu"
- **UŻYWA** tylko "Auto-drukuj zamówienia na miejscu"

### 2. Przychodzą Już ACCEPTED
- Nie ma statusu PROCESSING
- Nie wymagają akceptacji
- Drukują się natychmiast

### 3. Thread-Safe (Mutex)
- Jeden wydruk na zamówienie
- Brak race condition
- Persystencja w DataStore

### 4. Wybór Drukarki
- Niezależny od "Drukowanie na kuchni"
- Konfigurowalny: Główna lub Kuchenna
- Domyślnie: Główna

---

## 📝 Podsumowanie Przepływu

```
┌─────────────────────────────────┐
│ Backend: Nowe DINE_IN (ACCEPTED)│
└────────────┬────────────────────┘
             │
             ▼
    ┌────────────────┐
    │ Socket Event   │
    └────────┬───────┘
             │
             ▼
┌────────────────────────────────┐
│ socketEventsRepo.orders        │
│ emituje zamówienie             │
└────────┬───────────────────────┘
         │
         ▼
  ┌──────────────┐
  │ Zapisz do DB │
  └──────┬───────┘
         │
         ▼
┌────────────────────────────────┐
│ Sprawdź: deliveryType?         │
│ DINE_IN ✅                     │
└────────┬───────────────────────┘
         │
         ▼
┌────────────────────────────────┐
│ Sprawdź: Auto DINE_IN?         │
│ ON ✅                          │
└────────┬───────────────────────┘
         │
         ▼
┌────────────────────────────────┐
│ wasPrinted()? (Mutex)          │
│ NIE ✅                         │
└────────┬───────────────────────┘
         │
         ▼
┌────────────────────────────────┐
│ markAsPrinted() (Mutex)        │
│ Dodaje do setu + DataStore     │
└────────┬───────────────────────┘
         │
         ▼
┌────────────────────────────────┐
│ Pobierz ustawienie drukarki    │
│ "kitchen" ✅                   │
└────────┬───────────────────────┘
         │
         ▼
┌────────────────────────────────┐
│ printKitchenTicket(order)      │
│ DRUKUJE 1x NA KUCHNI ✅        │
└────────────────────────────────┘
```

---

**Data:** 2026-02-05  
**Wersja:** 1.0  
**Status:** ✅ Kompletna dokumentacja DINE_IN  
**Powiązane:**
- `FINAL_FIX_v1.7.2_MUTEX_THREAD_SAFETY.md`
- `DOKUMENTACJA_AUTOMATYCZNEGO_DRUKOWANIA.md`
- `TABELA_REFERENCYJNA_DRUKOWANIE.md`

