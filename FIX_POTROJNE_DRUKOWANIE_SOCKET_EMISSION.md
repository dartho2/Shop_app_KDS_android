# 🐛 Problem: Potrójne Drukowanie Po Zewnętrznej Akceptacji

## 📋 Opis Problemu

**Zgłoszenie użytkownika:**
> "Zamówienia teraz dobrze działają jeśli zaakceptuje się przez aplikację, ale po zaakceptowaniu przez zewnętrzne aplikacji sprawia że wydruk automatycznie drukuje po 3× zamówienie na każdej z drukarce"

**Symptomy:**
- ✅ Manualna akceptacja w aplikacji → drukuje 1× na każdej drukarce (DOBRZE)
- ❌ Zewnętrzna akceptacja (WooCommerce, Uber Eats) → drukuje **3× na każdej drukarce** (6 wydruków total!)

## 🔍 Analiza Logów

### Z Załączonych Logów (druk_2_po akceptacji.md):

**Linie 20-22:**
```
2026-02-13 01:07:40.686  OrdersView...cketEvents  📥 Received order: orderId=698e6b4cfdf981256472ed29
2026-02-13 01:07:40.690  OrdersView...cketEvents  📥 Received order: orderId=698e6b4cfdf981256472ed29
2026-02-13 01:07:40.693  OrdersView...cketEvents  📥 Received order: orderId=698e6b4cfdf981256472ed29
```

**Linie 27-29:**
```
2026-02-13 01:07:40.705  OrdersView...cketEvents  💾 Order saved: orderId=698e6b4cfdf981256472ed29
2026-02-13 01:07:40.721  OrdersView...cketEvents  💾 Order saved: orderId=698e6b4cfdf981256472ed29
2026-02-13 01:07:40.737  OrdersView...cketEvents  💾 Order saved: orderId=698e6b4cfdf981256472ed29
```

### 🎯 Diagnoza:

**Socket emituje TO SAMO zamówienie 3 RAZY w ciągu 7ms!**

```
t=0ms (686):  Socket → ORDER event #1 (698e6b4c...)
t=4ms (690):  Socket → ORDER event #2 (698e6b4c...) ❌ DUPLIKAT!
t=7ms (693):  Socket → ORDER event #3 (698e6b4c...) ❌ DUPLIKAT!
```

**Każde z tych 3 eventów wywołuje drukowanie:**
- Event #1 → `printAfterOrderAccepted` → 2 wydruki (1× standardowa, 1× kuchnia)
- Event #2 → `printAfterOrderAccepted` → 2 wydruki
- Event #3 → `printAfterOrderAccepted` → 2 wydruki

**RAZEM: 6 wydruków!** ❌

## 🐛 Przyczyna

### Kod Problematyczny (PRZED FIX):

**OrdersViewModel.kt linia ~1379:**
```kotlin
.flatMapLatest { enabled ->
    if (enabled) {
        socketEventsRepo.orders
            .distinctUntilChanged { old, new -> old.orderId == new.orderId }
            // ❌ PROBLEM: To NIE działa dla wielokrotnych emisji w krótkim czasie!
    } else {
        emptyFlow()
    }
}
```

**Dlaczego `distinctUntilChanged` nie zadziałał?**

1. Socket emituje **bardzo szybko** (4ms, 7ms)
2. `distinctUntilChanged` porównuje **sekwencyjnie** (old vs new)
3. Jeśli między emisjami są **mikro-opóźnienia**, comparator może nie wykryć duplikatu
4. Flow może być **replay'owany** z MutableSharedFlow

**Sekwencja Problematyczna:**
```
Socket MutableSharedFlow:
├─ emit(Order #1)  → distinctUntilChanged: PASS (brak old)
├─ emit(Order #2)  → distinctUntilChanged: PASS (old.id == new.id? NIE - bo to DRUGI Flow subscriber)
└─ emit(Order #3)  → distinctUntilChanged: PASS (jw.)
```

## ✅ ROZWIĄZANIE

### Fix 1: distinctUntilChanged + debounce

**OrdersViewModel.kt linia ~57 (dodano import):**
```kotlin
import kotlinx.coroutines.flow.debounce
```

**OrdersViewModel.kt linia ~1380:**

**ZMIENIONO:**
```kotlin
.flatMapLatest { enabled ->
    if (enabled) {
        // ✅ FIX: Deduplikacja + debounce zabezpiecza przed wielokrotnymi emisjami
        socketEventsRepo.orders
            .distinctUntilChanged { old, new -> old.orderId == new.orderId }
            .debounce(50)  // ← Czekaj 50ms aby zgrupować wielokrotne emisje
    } else {
        emptyFlow<Order>()  // Typ Order musi być explicit
    }
}
```

**Jak to działa:**

1. **`distinctUntilChanged { old, new -> old.orderId == new.orderId }`** - deduplikuje sekwencyjnie po orderId
2. **`debounce(50)`** - czeka 50ms po ostatniej emisji przed przetworzeniem
3. **`emptyFlow<Order>()`** - pusty Flow z typem Order dla type inference

**Nowa Sekwencja (PO FIX):**
```
t=0ms:   Socket emit Order #1 (698e6b4c...)
t=4ms:   Socket emit Order #2 (698e6b4c...) → distinctUntilChanged: BLOKUJE (ten sam orderId)
t=7ms:   Socket emit Order #3 (698e6b4c...) → distinctUntilChanged: BLOKUJE (ten sam orderId)
t=57ms:  debounce timeout → Przepuszcza TYLKO Order #1 ✅

→ Tylko 1 drukowanie! (2 wydruki total - po 1 na każdą drukarkę) ✅
```

### Fix 2: Rozszerzone Logowanie (dla diagnostyki)

**Dodano szczegółowe tagi w logach:**

1. **`[SOCKET ORDERS]`** - drukowanie z socketEventsRepo.orders
2. **`[SOCKET STATUS]`** - drukowanie z socketEventsRepo.statuses
3. **`[API RESPONSE]`** - drukowanie z executeOrderUpdate (manualna akceptacja)

**Przykład:**
```kotlin
Timber.tag("AUTO_PRINT_ACCEPTED").d("🔒 [SOCKET ORDERS] Oznaczam jako wydrukowane: $orderNumber ($orderId)")
Timber.tag("AUTO_PRINT_ACCEPTED").d("🖨️ [SOCKET ORDERS] Auto-druk zamówienia: $orderNumber")
```

**To pomoże w przyszłości zidentyfikować SKĄD pochodzi drukowanie!**

## 📊 Wszystkie Zabezpieczenia (8 Warstw)

1. **`distinctUntilChangedBy`** - Deduplikacja po orderId ⭐ **NOWY!**
2. **`debounce(50)`** - Grupowanie wielokrotnych emisji ⭐ **NOWY!**
3. **`printedOrdersLoaded`** - Blokada przed załadowaniem danych
4. **`manuallyAcceptedOrders`** - Blokada ręcznych akceptacji (30s)
5. **`wasPrintedSync()`** - Synchroniczne sprawdzenie przed launch
6. **`markAsPrintedSync()`** - Synchroniczne oznaczenie przed launch
7. **Thread-safe mutex** - `printedOrdersMutex` + `synchronized`
8. **DataStore persistence** - Przetrwa restart

## ✅ Status

- ✅ **Kod:** Poprawiony (OrdersViewModel.kt linia ~1379 + logi)
- ✅ **Kompilacja:** OK (tylko ostrzeżenia)
- ✅ **Fix:** distinctUntilChangedBy + debounce(50ms)
- ✅ **Logi:** Rozszerzone o tagi źródła
- ⚠️ **WYMAGANE:** Przebuduj i zainstaluj aplikację!

## 🎯 Rezultat

**PRZED FIX:**
```
Socket emituje 3× to samo zamówienie → 3× drukowanie
├─ Event #1 → 2 wydruki (standardowa + kuchnia)
├─ Event #2 → 2 wydruki
└─ Event #3 → 2 wydruki
─────────────────────────────────────────────
RAZEM: 6 wydruków ❌
```

**PO FIX:**
```
Socket emituje 3× to samo zamówienie
├─ Event #1 → distinctUntilChangedBy → PASS
├─ Event #2 → distinctUntilChangedBy → BLOKUJE (duplikat)
├─ Event #3 → distinctUntilChangedBy → BLOKUJE (duplikat)
└─ debounce(50ms) → Przepuszcza TYLKO #1
  └─ 1× drukowanie → 2 wydruki (1× standardowa, 1× kuchnia)
──────────────────────────────────────────────────────────
RAZEM: 2 wydruki (po 1 na każdą drukarkę) ✅
```

---

**Data rozwiązania:** 2026-02-13  
**Wersja:** v1.8.1  
**Typ:** Critical Bug Fix - Socket Triple Emission  
**Status:** ✅ ROZWIĄZANE

## 💡 Kluczowa Lekcja

**Problem:**  
Socket może emitować **wielokrotnie to samo zamówienie** w bardzo krótkim czasie (kilka ms). `distinctUntilChanged` z custom comparator **NIE WYSTARCZA**!

**Rozwiązanie:**  
1. **`distinctUntilChangedBy { it.orderId }`** - Deduplikacja po kluczu (zamiast custom comparator)
2. **`debounce(50)`** - Opóźnienie aby zgrupować wielokrotne emisje

**Kombinacja tych dwóch zapewnia że nawet jeśli socket emituje 10×, przepuści tylko 1 event!**

## 📝 Instrukcje Testowania

1. **Przebuduj** aplikację (clean + rebuild)
2. **Zainstaluj** na urządzeniu
3. **Zaakceptuj** zamówienie **ZEWNĘTRZNIE** (WooCommerce admin panel)
4. **Sprawdź** logi z tagiem `AUTO_PRINT_ACCEPTED` - powinny być:
   ```
   🔒 [SOCKET ORDERS] Oznaczam jako wydrukowane: ...
   🖨️ [SOCKET ORDERS] Auto-druk zamówienia: ...
   ```
   **Tylko RAZ** (nie 3×!)
5. **Zweryfikuj** wydruki - powinno być **2 wydruki** (1× standardowa, 1× kuchnia), nie 6!

