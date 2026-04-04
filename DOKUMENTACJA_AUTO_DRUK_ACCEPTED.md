# Automatyczne Drukowanie Zamówień Przychodzących Już Zaakceptowanych

## 🎯 Problem

Niektóre platformy (Uber Eats, Glovo, Wolt) wysyłają zamówienia **już ze statusem ACCEPTED**. Takie zamówienia:
- ❌ **NIE** przechodzą przez manualną akceptację w aplikacji
- ❌ **NIE** wywołują `executeOrderUpdate()` (bo status się nie zmienia)
- ❌ **NIE** drukują się automatycznie mimo włączonej opcji "Drukuj po zaakceptowaniu"

## ✅ Rozwiązanie

Dodano nową logikę w `OrdersViewModel.observeSocketEvents()` która:
1. Wykrywa zamówienia przychodzące już jako **ACCEPTED**
2. Automatycznie je drukuje (jeśli włączone "Drukuj po zaakceptowaniu")
3. **NIE** koliduje z manualną akceptacją

---

## 🔧 Implementacja

### Kod: OrdersViewModel.kt (linia ~1170)

```kotlin
// AUTO-DRUK DLA ZAMÓWIEŃ PRZYCHODZĄCYCH JUŻ ZAAKCEPTOWANYCH
// (np. z platform Uber Eats, Glovo - przychodzą już ze statusem ACCEPTED)
val orderStatus = order.orderStatus.slug?.let { slug ->
    runCatching { OrderStatusEnum.valueOf(slug) }.getOrNull()
}

if (orderStatus == OrderStatusEnum.ACCEPTED &&
    order.deliveryType != OrderDelivery.DINE_IN &&
    order.deliveryType != OrderDelivery.ROOM_SERVICE &&
    appPreferencesManager.getAutoPrintAcceptedEnabled()) {
    
    Timber.tag("AUTO_PRINT_ACCEPTED").d("🔔 Zamówienie przyszło już ACCEPTED: ${order.orderNumber}")
    
    viewModelScope.launch {
        try {
            Timber.d("🖨️ Auto-druk zamówienia już zaakceptowanego: %s", order.orderNumber)
            printerService.printAfterOrderAccepted(order)
            Timber.d("✅ Auto-druk zakończony dla %s", order.orderNumber)
        } catch (e: Exception) {
            Timber.e(e, "❌ Błąd auto-druku dla %s", order.orderNumber)
        }
    }
}
```

---

## 📊 Warunki Uruchomienia

### ✅ WSZYSTKIE muszą być spełnione:

1. **Status zamówienia** = `ACCEPTED`
2. **Typ dostawy** ≠ `DINE_IN` (wykluczony - ma własną logikę)
3. **Typ dostawy** ≠ `ROOM_SERVICE` (wykluczony - ma własną logikę)
4. **Ustawienie** "Drukuj po zaakceptowaniu" = `ON`

### Przykład

**Zamówienie z Uber Eats:**
```json
{
  "orderNumber": "UBER-12345",
  "orderStatus": { "slug": "ACCEPTED" },
  "deliveryType": "DELIVERY"
}
```

**Przy ustawieniach:**
- ✅ Drukuj po zaakceptowaniu: **ON**
- ✅ Drukowanie na kuchni: **ON**

**Rezultat:**
```
00:00 - Zamówienie przychodzi (już ACCEPTED)
  ├─► 00:01 ✅ Wydruk na GŁÓWNEJ
  └─► 00:02 ✅ Wydruk na KUCHENNEJ
```

---

## 🔄 Porównanie: Przed vs Po

### PRZED (bez tej funkcjonalności)

**Scenariusz:** Zamówienie Uber Eats przychodzi jako ACCEPTED

| Akcja | Rezultat |
|-------|----------|
| Zamówienie przychodzi | ❌ Nie drukuje się |
| Opcja "Drukuj po zaakceptowaniu" | ❌ Nie działa (bo status się nie zmienia) |
| Pracownik musi | ✋ Ręcznie wydrukować z menu |

### PO (z nową funkcjonalnością)

**Scenariusz:** To samo zamówienie

| Akcja | Rezultat |
|-------|----------|
| Zamówienie przychodzi | ✅ Drukuje się automatycznie |
| Opcja "Drukuj po zaakceptowaniu" | ✅ Działa! |
| Pracownik musi | 👍 Nic - wszystko automatyczne |

---

## 🎬 Flowchart

```
┌─────────────────────────┐
│ Nowe zamówienie socket  │
└──────────┬──────────────┘
           │
           ▼
    ┌──────────────┐
    │ orderStatus? │
    └──┬───────┬───┘
       │       │
   ACCEPTED  PROCESSING
       │       │
       │       └──> (inna logika)
       │
       ▼
  ┌──────────────────┐
  │ deliveryType?    │
  └────┬──────┬──────┘
       │      │
   DINE_IN  DELIVERY/
   ROOM_    PICKUP
   SERVICE    │
       │      │
       │      ▼
       │  ┌─────────────────────┐
       │  │Auto-print accepted? │
       │  └────┬──────┬─────────┘
       │      ON     OFF
       │       │      │
       │       │      └──> ❌ KONIEC
       │       │
       │       ▼
       │  ┌─────────────────────┐
       │  │ DRUKUJ GŁÓWNA       │
       │  └──────┬──────────────┘
       │         │
       │         ▼
       │  ┌─────────────────────┐
       │  │ Drukowanie kuchni?  │
       │  └────┬──────┬─────────┘
       │      ON     OFF
       │       │      │
       │       │      └──> ✅ KONIEC
       │       ▼
       │  ┌─────────────────────┐
       │  │ DRUKUJ KUCHNIA      │
       │  └─────────────────────┘
       │
       └──> (logika DINE_IN)
```

---

## 🚫 Unikanie Kolizji

### Problem: Czy zamówienie może wydrukować się 2 razy?

**Scenariusz 1: Zamówienie przychodzi PROCESSING → pracownik akceptuje**

```
1. Nowe zamówienie (PROCESSING)
   └─> ❌ NIE spełnia warunku (status != ACCEPTED)
   
2. Pracownik akceptuje (PROCESSING → ACCEPTED)
   └─> executeOrderUpdate()
       └─> ✅ DRUKUJE (manualna akceptacja)
```

**Rezultat:** 1 wydruk ✅

---

**Scenariusz 2: Zamówienie przychodzi już ACCEPTED**

```
1. Nowe zamówienie (ACCEPTED)
   └─> observeSocketEvents()
       └─> ✅ DRUKUJE (automatyczna detekcja)
   
2. Pracownik NIE akceptuje (bo już jest ACCEPTED)
   └─> executeOrderUpdate() NIE jest wywołane
```

**Rezultat:** 1 wydruk ✅

---

**Scenariusz 3: Teoretyczna duplikacja (NIE MOŻE SIĘ ZDARZYĆ)**

```
1. Zamówienie przychodzi ACCEPTED
   └─> observeSocketEvents() - DRUKUJE
   
2. Status zmienia się ACCEPTED → ACCEPTED (???)
   └─> executeOrderUpdate() - teoretycznie DRUKUJE
```

**Ale:** Status **NIE ZMIENIA SIĘ** z ACCEPTED na ACCEPTED, więc `executeOrderUpdate()` **NIE** zostanie wywołane.

**Rezultat:** Brak duplikacji ✅

---

## 🧪 Scenariusze Testowe

### Test 1: Zamówienie Uber Eats (ACCEPTED)

**Przygotowanie:**
- ✅ Drukuj po zaakceptowaniu: ON
- ✅ Drukowanie na kuchni: ON
- Wyślij zamówienie z API ze statusem `ACCEPTED`

**Oczekiwany rezultat:**
```
🔔 Zamówienie przyszło już ACCEPTED: UBER-12345
🖨️ Auto-druk zamówienia już zaakceptowanego: UBER-12345
✅ Auto-druk zakończony dla UBER-12345

Wydruki:
- 1x Główna
- 1x Kuchnia
```

**Sprawdź logi:** `AUTO_PRINT_ACCEPTED`

---

### Test 2: Normalne Zamówienie (PROCESSING → ACCEPTED)

**Przygotowanie:**
- ✅ Drukuj po zaakceptowaniu: ON
- ✅ Drukowanie na kuchni: ON
- Wyślij zamówienie z API ze statusem `PROCESSING`
- Zaakceptuj ręcznie

**Oczekiwany rezultat:**
```
Przy nowym: Brak drukowania (status = PROCESSING)
Po akceptacji: executeOrderUpdate() wywołane
✅ Drukuje normalnie (stara logika)

Wydruki:
- 1x Główna
- 1x Kuchnia
```

**Sprawdź:** Brak logów `AUTO_PRINT_ACCEPTED` (bo inna ścieżka)

---

### Test 3: DINE_IN Przychodzi ACCEPTED (wykluczony)

**Przygotowanie:**
- ✅ Auto DINE_IN: ON
- ✅ Drukuj po zaakceptowaniu: ON
- Wyślij DINE_IN ze statusem `ACCEPTED`

**Oczekiwany rezultat:**
```
❌ NIE drukuje przez nową logikę (deliveryType = DINE_IN)
✅ Drukuje przez logikę DINE_IN (inna ścieżka)

Wydruki:
- 1x (z logiki DINE_IN)
```

**Sprawdź:** Warunek `order.deliveryType != OrderDelivery.DINE_IN` blokuje duplikację

---

## 📝 Logi Debugowe

### Pozytywne (zamówienie drukowane)

```
AUTO_PRINT_ACCEPTED: 🔔 Zamówienie przyszło już ACCEPTED: ORDER-12345
OrdersViewModel: 🖨️ Auto-druk zamówienia już zaakceptowanego: ORDER-12345
PrinterService: 📋 printAfterOrderAccepted: order=ORDER-12345, autoAccepted=true, autoKitchen=true
PrinterService: 🖨️ Drukuję na standardowej: Główna
PrinterService: ✅ Drukowanie standardowe zakończone
PrinterService: 🍳 Drukuję na kuchni: Kuchnia
PrinterService: ✅ Drukowanie kuchenne zakończone
OrdersViewModel: ✅ Auto-druk zakończony dla ORDER-12345
```

### Negatywne (warunek nie spełniony)

```
OrdersViewModel: 📥 Received order: orderId=ORDER-12345
OrdersViewModel: 💾 Order saved to database: orderId=ORDER-12345
(brak logów AUTO_PRINT_ACCEPTED - warunek nie spełniony)
```

---

## ⚙️ Konfiguracja

### Włączenie Funkcjonalności

**Ustawienia → Drukowanie:**
- ✅ **Drukuj po zaakceptowaniu**: ON

To wystarczy! Funkcjonalność jest automatycznie aktywna.

### Dodatkowe Opcje

- ✅ **Drukowanie na kuchni**: ON → Dodatkowo drukuje na kuchennej
- ❌ **Drukowanie na kuchni**: OFF → Tylko główna

---

## 🔍 Diagnostyka

### Problem: Zamówienie ACCEPTED nie drukuje się

**Sprawdź:**

1. **Ustawienie włączone?**
   ```
   Ustawienia → Drukowanie → Drukuj po zaakceptowaniu [✓]
   ```

2. **Logi:**
   ```bash
   adb logcat | grep "AUTO_PRINT_ACCEPTED"
   ```
   
   **Powinno być:**
   ```
   🔔 Zamówienie przyszło już ACCEPTED: ...
   ```

3. **Typ dostawy:**
   - Czy to DINE_IN? (ma własną logikę)
   - Czy to ROOM_SERVICE? (ma własną logikę)

4. **Status zamówienia:**
   - Sprawdź API response
   - Czy `orderStatus.slug` = `"ACCEPTED"`?

---

## 📊 Statystyki Użycia

### Platformy Wysyłające ACCEPTED

| Platforma | Status przy nowym zamówieniu | Obsługiwane? |
|-----------|------------------------------|--------------|
| Uber Eats | ✅ ACCEPTED | ✅ TAK |
| Glovo | ✅ ACCEPTED | ✅ TAK |
| Wolt | ✅ ACCEPTED | ✅ TAK |
| Bolt Food | ⚠️ Różnie (sprawdź) | ✅ TAK |
| GOPOS | ❌ PROCESSING | ❌ Nie dotyczy |
| Manual Order | ❌ PROCESSING | ❌ Nie dotyczy |

---

## 🎓 Best Practices

### ✅ Zalecane

**Konfiguracja A: Wszystkie zamówienia drukuj po "akceptacji"**
```
❌ Automatyczne drukowanie: OFF
✅ Drukuj po zaakceptowaniu: ON
✅ Drukowanie na kuchni: ON
```

**Rezultat:**
- Zamówienia PROCESSING → pracownik akceptuje → drukuje
- Zamówienia ACCEPTED → drukuje automatycznie
- **Spójne zachowanie!**

---

### ⚠️ Niezalecane

**Konflikt: Wszystko włączone**
```
✅ Automatyczne drukowanie: ON
✅ Drukuj po zaakceptowaniu: ON
```

**Problem:**
- Zamówienia PROCESSING → drukuje 2x (nowe + akceptacja)
- Zamówienia ACCEPTED → drukuje 1x (tylko nowa logika)
- **Niespójne!**

---

## 🔄 Historia Zmian

### v1.2 (2026-02-04)
- ✅ Dodano automatyczne drukowanie dla zamówień przychodzących jako ACCEPTED
- ✅ Wykluczono DINE_IN/ROOM_SERVICE żeby uniknąć duplikacji
- ✅ Dodano logi `AUTO_PRINT_ACCEPTED`
- ✅ Zaktualizowano dokumentację

### v1.1 (2026-02-03)
- Naprawiono duplikację DINE_IN
- Dodano wykluczenie DINE_IN z SocketStaffEventsHandler

---

## 📚 Powiązana Dokumentacja

- `DOKUMENTACJA_AUTOMATYCZNEGO_DRUKOWANIA.md` - Główna dokumentacja
- `TABELA_REFERENCYJNA_DRUKOWANIE.md` - Szybka referencia
- `ANALIZA_WIELOKROTNE_DRUKOWANIE_DINE_IN.md` - Analiza problemów

---

**Data:** 2026-02-04  
**Wersja:** 1.2  
**Status:** ✅ Zaimplementowane i przetestowane  
**Autor:** AI Assistant

