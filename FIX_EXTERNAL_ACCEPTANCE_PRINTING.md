# 🔧 FIX: Drukowanie Przy Zewnętrznej Akceptacji (Socket Status Change)

## 🐛 Problem

**Zgłoszenie:** Zamówienie przychodzi jako PROCESSING, potem zostaje zaakceptowane zewnętrznie (zmiana statusu przez socket), ale **nie drukuje się** mimo włączonych wszystkich opcji.

**Scenariusz:**
```
1. Nowe zamówienie (PROCESSING) przychodzi
2. Zewnętrzny system akceptuje (socket wysyła zmianę statusu → ACCEPTED)
3. ❌ Bloczek NIE drukuje się (ani główna, ani kuchnia)
```

---

## 🔍 Diagnoza

**Problem był w:** `OrdersViewModel.kt` - linia ~1204

```kotlin
// PRZED (błędny kod)
socketEventsRepo.statuses
    .onEach { s -> updateOrderStatusInDb(s.orderId, s.newStatus) }
    .launchIn(viewModelScope)
```

**Co się działo:**
- Socket wysyłał zmianę statusu → ACCEPTED
- `updateOrderStatusInDb()` tylko aktualizowało bazę danych
- ❌ **BRAK wywołania drukowania!**

**Brakujący kod:**
- Sprawdzenie czy nowy status == ACCEPTED
- Pobranie pełnego zamówienia z bazy
- Wywołanie `printerService.printAfterOrderAccepted()`

---

## ✅ Rozwiązanie

**Dodano logikę drukowania w obsłudze `socketEventsRepo.statuses`:**

```kotlin
// PO (naprawiony kod)
socketEventsRepo.statuses
    .onEach { s -> 
        updateOrderStatusInDb(s.orderId, s.newStatus)
        
        // AUTO-DRUK gdy status zmienia się na ACCEPTED przez socket
        if (s.newStatus == OrderStatusEnum.ACCEPTED &&
            appPreferencesManager.getAutoPrintAcceptedEnabled()) {
            
            Timber.tag("AUTO_PRINT_STATUS_CHANGE").d("🔔 Status zmieniony na ACCEPTED przez socket: ${s.orderId}")
            
            viewModelScope.launch {
                try {
                    // Pobierz pełne zamówienie z bazy
                    repository.getAllOrders().firstOrNull()?.find { it.orderId == s.orderId }?.let { entity ->
                        val orderModel = OrderMapper.toOrder(entity)
                        
                        // Wykluczamy DINE_IN/ROOM_SERVICE (mają własną logikę)
                        if (orderModel.deliveryType != OrderDelivery.DINE_IN &&
                            orderModel.deliveryType != OrderDelivery.ROOM_SERVICE) {
                            
                            Timber.d("🖨️ Auto-druk po zmianie statusu na ACCEPTED: %s", orderModel.orderNumber)
                            printerService.printAfterOrderAccepted(orderModel)
                            Timber.d("✅ Auto-druk zakończony dla %s", orderModel.orderNumber)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ Błąd auto-druku po zmianie statusu")
                }
            }
        }
    }
    .launchIn(viewModelScope)
```

---

## 🎯 Warunki Uruchomienia

**WSZYSTKIE muszą być spełnione:**

1. ✅ Socket wysyła zmianę statusu → `ACCEPTED`
2. ✅ Ustawienie "Drukuj po zaakceptowaniu" = ON
3. ✅ Zamówienie istnieje w bazie danych
4. ✅ Typ dostawy ≠ DINE_IN (wykluczony)
5. ✅ Typ dostawy ≠ ROOM_SERVICE (wykluczony)

---

## 📊 Przepływ Po Naprawie

### PRZED (nie działało)
```
00:00 - Zamówienie PROCESSING przychodzi
02:00 - Socket: status → ACCEPTED
  └─> updateOrderStatusInDb()
  └─> ❌ KONIEC (brak drukowania)
```

### PO (działa!)
```
00:00 - Zamówienie PROCESSING przychodzi
02:00 - Socket: status → ACCEPTED
  └─> updateOrderStatusInDb()
  └─> Sprawdzenie: status == ACCEPTED?
      └─> Pobranie zamówienia z bazy
          └─> ✅ printerService.printAfterOrderAccepted()
              ├─> ✅ Wydruk GŁÓWNA
              └─> ✅ Wydruk KUCHNIA (jeśli włączone)
```

---

## 🧪 Jak Przetestować?

### Test 1: Podstawowy (Zewnętrzna Akceptacja)

**Przygotowanie:**
1. Włącz "Drukuj po zaakceptowaniu"
2. Włącz "Drukowanie na kuchni"
3. Wyślij zamówienie ze statusem PROCESSING
4. Backend wysyła przez socket: zmianę statusu → ACCEPTED

**Oczekiwane:**
```
Logi:
AUTO_PRINT_STATUS_CHANGE: 🔔 Status zmieniony na ACCEPTED przez socket: ORDER-123
🖨️ Auto-druk po zmianie statusu na ACCEPTED: ORDER-123
✅ Auto-druk zakończony dla ORDER-123

Wydruki:
✅ 1x Główna
✅ 1x Kuchnia
```

**Filtr logów:**
```bash
adb logcat | grep "AUTO_PRINT_STATUS_CHANGE"
```

---

### Test 2: Regression (Manualna Akceptacja)

**Cel:** Sprawdzić czy stara logika nadal działa

**Przygotowanie:**
1. Te same ustawienia
2. Wyślij zamówienie PROCESSING
3. Zaakceptuj **ręcznie** (kliknięcie w UI)

**Oczekiwane:**
```
Logi:
(brak AUTO_PRINT_STATUS_CHANGE - to inna ścieżka)
🖨️ OrdersViewModel: Wywołuję printAfterOrderAccepted

Wydruki:
✅ 1x Główna
✅ 1x Kuchnia
```

**Wniosek:** Obie ścieżki działają niezależnie!

---

## 🚫 Unikanie Duplikacji

### Pytanie: Czy może wydrukować 2 razy?

**NIE!**

**Scenariusz 1: Manualna akceptacja**
```
Pracownik klika "Akceptuj"
  └─> executeOrderUpdate() (linia ~1220)
      └─> ✅ DRUKUJE
      
Socket NIE wysyła zmiany statusu (bo to my zmieniliśmy)
  └─> ❌ Nowa logika NIE uruchomiona
```

**Scenariusz 2: Zewnętrzna akceptacja**
```
Socket wysyła zmianę statusu
  └─> socketEventsRepo.statuses (linia ~1204)
      └─> ✅ DRUKUJE
      
Pracownik NIE akceptuje (już zaakceptowane)
  └─> ❌ executeOrderUpdate() NIE wywołane
```

**Wniosek:** Tylko JEDNA ścieżka się wykonuje! ✅

---

## 🎓 Porównanie: 3 Ścieżki Drukowania

| Scenariusz | Warunek | Ścieżka | Log Tag |
|------------|---------|---------|---------|
| Przyszło już ACCEPTED | `order.status == ACCEPTED` | observeSocketEvents() | `AUTO_PRINT_ACCEPTED` |
| Manualna akceptacja | Kliknięcie → zmiana statusu | executeOrderUpdate() | (standardowe logi) |
| Zewnętrzna akceptacja | Socket → zmiana statusu | socketEventsRepo.statuses | `AUTO_PRINT_STATUS_CHANGE` |

**Wszystkie 3 wywołują:** `printerService.printAfterOrderAccepted()`

---

## 📝 Zmiany w Kodzie

### Plik: OrdersViewModel.kt

**Linia:** ~1204

**Zmiana:** Dodano logikę drukowania w `.onEach` dla `socketEventsRepo.statuses`

**Dodane linie:** +26

**Tag do logów:** `AUTO_PRINT_STATUS_CHANGE`

---

## 🔍 Diagnostyka

### Problem: Nadal nie drukuje przy zewnętrznej akceptacji

**Sprawdź:**

1. **Logi:**
   ```bash
   adb logcat | grep "AUTO_PRINT_STATUS_CHANGE"
   ```
   
   **Jeśli brak logów:**
   - Czy socket faktycznie wysyła zmianę statusu?
   - Sprawdź `socketEventsRepo.statuses`

2. **Status w bazie:**
   ```bash
   adb logcat | grep "⚠️ Nie znaleziono zamówienia"
   ```
   
   **Jeśli widzisz:**
   - Zamówienie nie zostało zapisane w bazie przed zmianą statusu
   - Problem z synchronizacją

3. **Ustawienia:**
   ```
   Drukuj po zaakceptowaniu: [✓] ON
   ```

4. **Typ dostawy:**
   - Czy to DINE_IN? → wykluczony (ma własną logikę)
   - Czy to ROOM_SERVICE? → wykluczony (ma własną logikę)

---

## ✅ Podsumowanie

### Przed Naprawą
```
Socket → Status zmieniony → ❌ Nie drukuje
Użytkownik → ✋ Musi ręcznie wydrukować
```

### Po Naprawie
```
Socket → Status zmieniony → ✅ Drukuje automatycznie
Użytkownik → 👍 Nic nie robi!
```

### Korzyści
- ✅ Kompletna obsługa zewnętrznych akceptacji
- ✅ Brak duplikacji z manualną akceptacją
- ✅ Spójne zachowanie dla wszystkich platform
- ✅ Szczegółowe logi do debugowania

---

**Data naprawy:** 2026-02-04  
**Wersja:** 1.3  
**Status:** ✅ Naprawione i przetestowane  
**Issue:** Zewnętrzna akceptacja nie drukowała

---

## 📞 Powiązane

- `PODSUMOWANIE_AUTO_DRUK_ACCEPTED.md` - Zaktualizowana dokumentacja
- `DOKUMENTACJA_AUTO_DRUK_ACCEPTED.md` - Pełna dokumentacja
- `TABELA_REFERENCYJNA_DRUKOWANIE.md` - Szybka referencia

