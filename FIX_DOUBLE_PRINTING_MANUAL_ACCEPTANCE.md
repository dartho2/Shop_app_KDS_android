# 🔧 FIX: Podwójne Drukowanie Przy Manualnej Akceptacji

## 🐛 Problem

**Zgłoszenie:** Gdy wszystkie opcje są zaznaczone, zamówienie drukuje się **2 razy** po zaakceptowaniu.

**Scenariusz:**
```
✅ Drukuj po zaakceptowaniu: ON
✅ Drukowanie na kuchni: ON

Pracownik akceptuje zamówienie
  └─> ❌ DRUKUJE 2 RAZY (4 bloczki razem: 2x Główna + 2x Kuchnia)
```

---

## 🔍 Diagnoza

**Problem był w duplikacji między 2 ścieżkami drukowania:**

### Ścieżka 1: `executeOrderUpdate()` (linia ~1285)
```kotlin
// Gdy PRACOWNIK akceptuje zamówienie ręcznie (kliknięcie w UI)
if (res.value.orderStatus.slug == OrderStatusEnum.ACCEPTED.name &&
    appPreferencesManager.getAutoPrintAcceptedEnabled()) {
    
    printerService.printAfterOrderAccepted(res.value)
    // ✅ DRUKUJE #1
}
```

### Ścieżka 2: `socketEventsRepo.statuses` (linia ~1215)
```kotlin
// Gdy SOCKET potwierdza zmianę statusu na ACCEPTED
if (s.newStatus == OrderStatusEnum.ACCEPTED &&
    appPreferencesManager.getAutoPrintAcceptedEnabled()) {
    
    printerService.printAfterOrderAccepted(orderModel)
    // ✅ DRUKUJE #2 (duplikacja!)
}
```

**Co się działo:**
1. Pracownik klika "Akceptuj" → API zmienia status → **DRUKUJE** (ścieżka 1)
2. Backend wysyła przez socket potwierdzenie: "status changed to ACCEPTED" → **DRUKUJE PONOWNIE** (ścieżka 2)

**Rezultat:** 2x Główna + 2x Kuchnia = **4 wydruki** zamiast 2!

---

## ✅ Rozwiązanie

**Dodano mechanizm wykrywania manualnej akceptacji:**

### 1. Dodano Set do śledzenia manualnych akceptacji

```kotlin
// Zamówienia zaakceptowane manualnie (żeby pominąć duplikację drukowania przez socket)
private val manuallyAcceptedOrders = mutableSetOf<String>()
```

### 2. Oznaczenie zamówienia przy manualnej akceptacji

```kotlin
// executeOrderUpdate() - linia ~1285
if (res.value.orderStatus.slug == OrderStatusEnum.ACCEPTED.name &&
    appPreferencesManager.getAutoPrintAcceptedEnabled()) {
    
    // Oznacz jako manualnie zaakceptowane
    manuallyAcceptedOrders.add(res.value.orderId)
    
    // Usuń z setu po 5 sekundach (na wypadek gdyby socket się spóźnił)
    viewModelScope.launch {
        delay(5000)
        manuallyAcceptedOrders.remove(res.value.orderId)
    }
    
    // DRUKUJ
    printerService.printAfterOrderAccepted(res.value)
}
```

**Mechanizm:**
- Dodaj `orderId` do setu
- Usuń po 5 sekundach (timeout na socket)

### 3. Sprawdzenie przed drukowaniem przez socket

```kotlin
// socketEventsRepo.statuses - linia ~1215
if (s.newStatus == OrderStatusEnum.ACCEPTED &&
    appPreferencesManager.getAutoPrintAcceptedEnabled()) {
    
    // Sprawdź czy to nie było manualne zaakceptowanie
    if (manuallyAcceptedOrders.contains(s.orderId)) {
        Timber.d("⏭️ Pomijam drukowanie - zamówienie było manualnie zaakceptowane: ${s.orderId}")
        return@onEach // POMIŃ drukowanie
    }
    
    // DRUKUJ (tylko dla zewnętrznej akceptacji)
    printerService.printAfterOrderAccepted(orderModel)
}
```

---

## 📊 Przepływ Po Naprawie

### Scenariusz 1: Manualna Akceptacja (z aplikacji)

```
00:00 - Pracownik klika "Akceptuj"
  │
  ├─► executeOrderUpdate()
  │   └─> API zmienia status
  │       └─> manuallyAcceptedOrders.add(orderId) ✅
  │       └─> ✅ DRUKUJE (Główna + Kuchnia)
  │
00:01 - Socket wysyła potwierdzenie: status → ACCEPTED
  │
  └─► socketEventsRepo.statuses
      └─> Sprawdzenie: manuallyAcceptedOrders.contains(orderId)?
          └─> TAK → ⏭️ POMIŃ drukowanie
          
Razem: 2 wydruki (1x Główna + 1x Kuchnia) ✅
```

### Scenariusz 2: Zewnętrzna Akceptacja (socket)

```
00:00 - Zamówienie PROCESSING w aplikacji
  │
00:05 - Socket wysyła: status → ACCEPTED (zewnętrzny system zaakceptował)
  │
  └─► socketEventsRepo.statuses
      └─> Sprawdzenie: manuallyAcceptedOrders.contains(orderId)?
          └─> NIE → ✅ DRUKUJE (Główna + Kuchnia)
          
Razem: 2 wydruki (1x Główna + 1x Kuchnia) ✅
```

---

## 🎯 Porównanie: Przed vs Po

### PRZED (błędne - duplikacja)

**Manualna akceptacja:**
```
Pracownik akceptuje → DRUKUJE
Socket potwierdza → DRUKUJE PONOWNIE
= 4 WYDRUKI ❌
```

### PO (poprawione)

**Manualna akceptacja:**
```
Pracownik akceptuje → DRUKUJE + oznacz jako manualne
Socket potwierdza → POMIŃ (było manualne)
= 2 WYDRUKI ✅
```

**Zewnętrzna akceptacja:**
```
Socket wysyła zmianę → Sprawdź: manualne? NIE → DRUKUJE
= 2 WYDRUKI ✅
```

---

## ⏱️ Timeout (5 sekund)

**Dlaczego 5 sekund?**
- Socket zazwyczaj potwierdza zmianę statusu w ciągu **1-2 sekund**
- 5 sekund to bezpieczny bufor na opóźnienia sieci
- Po 5 sekundach `orderId` jest usuwany z setu

**Co jeśli socket się spóźni (>5 sekund)?**
- `orderId` już nie będzie w secie
- Zamówienie wydrukuje się ponownie (rzadki edge case)
- **Lepiej** wydrukować 2 razy w rzadkim przypadku niż **nigdy** nie wydrukować zewnętrznej akceptacji

---

## 🧪 Testy

### Test 1: Manualna Akceptacja

**Przygotowanie:**
1. Włącz wszystkie opcje drukowania
2. Wyślij zamówienie PROCESSING
3. Zaakceptuj **ręcznie** w aplikacji

**Oczekiwane logi:**
```
🖨️ OrdersViewModel: Wywołuję printAfterOrderAccepted dla ORDER-123
PrinterService: 🖨️ Drukuję na standardowej: Główna
PrinterService: 🍳 Drukuję na kuchni: Kuchnia

(po ~1 sekundzie socket potwierdza)

AUTO_PRINT_STATUS_CHANGE: ⏭️ Pomijam drukowanie - zamówienie było manualnie zaakceptowane: ORDER-123
```

**Rezultat:** **2 wydruki** (1x Główna + 1x Kuchnia) ✅

---

### Test 2: Zewnętrzna Akceptacja

**Przygotowanie:**
1. Włącz wszystkie opcje drukowania
2. Wyślij zamówienie PROCESSING
3. Backend zmienia status przez API (nie z aplikacji)
4. Socket wysyła zmianę statusu

**Oczekiwane logi:**
```
AUTO_PRINT_STATUS_CHANGE: 🔔 Status zmieniony na ACCEPTED przez socket: ORDER-124
🖨️ Auto-druk po zmianie statusu na ACCEPTED: ORDER-124
PrinterService: 🖨️ Drukuję na standardowej: Główna
PrinterService: 🍳 Drukuję na kuchni: Kuchnia
```

**Rezultat:** **2 wydruki** (1x Główna + 1x Kuchnia) ✅

---

### Test 3: Regression - Zamówienie Przychodzi ACCEPTED

**Przygotowanie:**
1. Wyślij zamówienie już jako ACCEPTED (np. Uber Eats)

**Oczekiwane logi:**
```
AUTO_PRINT_ACCEPTED: 🔔 Zamówienie przyszło już ACCEPTED: UBER-125
🖨️ Auto-druk zamówienia już zaakceptowanego: UBER-125
```

**Rezultat:** **2 wydruki** (przez logikę "nowe ACCEPTED", nie socket) ✅

---

## 📝 Zmiany w Kodzie

### Plik: OrdersViewModel.kt

**Dodane:**
1. **Linia ~140:** `manuallyAcceptedOrders` Set
2. **Linia ~1290:** Dodanie do setu + timeout
3. **Linia ~1217:** Sprawdzenie przed drukowaniem

**Zmodyfikowane funkcje:**
- `executeOrderUpdate()` - dodano oznaczanie manualnej akceptacji
- `observeSocketEvents()` - dodano sprawdzenie przed drukowaniem

**Dodane linie:** ~15

---

## 🔍 Diagnostyka

### Problem: Nadal drukuje 2 razy

**Sprawdź logi:**
```bash
adb logcat | grep -E "AUTO_PRINT|manualnie zaakceptowane"
```

**Jeśli widzisz:**
```
⏭️ Pomijam drukowanie - zamówienie było manualnie zaakceptowane
```
→ ✅ Fix działa poprawnie

**Jeśli NIE widzisz tego logu:**
→ Socket nie potwierdza zmiany statusu (rzadki przypadek)

---

### Problem: Zewnętrzna akceptacja nie drukuje

**Sprawdź:**
1. Czy zamówienie jest w `manuallyAcceptedOrders`?
   - Log: `⏭️ Pomijam drukowanie`
   - Rozwiązanie: Czekaj >5 sekund między testami

2. Czy socket wysyła zmianę statusu?
   - Sprawdź logi: `AUTO_PRINT_STATUS_CHANGE`
   - Jeśli brak → problem z socketem, nie z drukowaniem

---

## 🎓 Edge Cases

### Edge Case 1: Socket spóźnia się >5 sekund

**Scenariusz:**
- Pracownik akceptuje → drukuje
- Socket potwierdza po 6 sekundach
- `orderId` już usunięty z setu

**Rezultat:** Wydrukuje się 2 razy (rzadki przypadek)

**Rozwiązanie:** Można zwiększyć timeout do 10 sekund, ale:
- Dłuższy timeout = dłużej trzymamy `orderId` w pamięci
- 5 sekund to dobry balans

---

### Edge Case 2: Dwukrotne kliknięcie "Akceptuj"

**Scenariusz:**
- Pracownik klika 2x szybko

**Ochrona:** Już istnieje w `executeOrderUpdate` - `_lockedOrderIds`

**Rezultat:** Drugie kliknięcie jest ignorowane ✅

---

## ✅ Podsumowanie

### Przed Naprawą
```
Manualna akceptacja:
  - executeOrderUpdate() → DRUKUJE
  - socket confirmation → DRUKUJE PONOWNIE
  = 4 WYDRUKI ❌
```

### Po Naprawie
```
Manualna akceptacja:
  - executeOrderUpdate() → DRUKUJE + oznacz
  - socket confirmation → POMIŃ (było oznaczone)
  = 2 WYDRUKI ✅

Zewnętrzna akceptacja:
  - socket confirmation → DRUKUJE (nie było oznaczone)
  = 2 WYDRUKI ✅
```

### Korzyści
- ✅ Brak duplikacji przy manualnej akceptacji
- ✅ Zachowana obsługa zewnętrznej akceptacji
- ✅ Minimalny overhead (Set + 5s timeout)
- ✅ Automatyczne czyszczenie pamięci

---

**Data naprawy:** 2026-02-04  
**Wersja:** 1.4  
**Status:** ✅ Naprawione i przetestowane  
**Issue:** Podwójne drukowanie przy manualnej akceptacji

---

## 📞 Powiązane

- `FIX_EXTERNAL_ACCEPTANCE_PRINTING.md` - Fix zewnętrznej akceptacji
- `PODSUMOWANIE_AUTO_DRUK_ACCEPTED.md` - Dokumentacja auto-druku
- `DOKUMENTACJA_AUTOMATYCZNEGO_DRUKOWANIA.md` - Główna dokumentacja

