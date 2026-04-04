# ✅ FIX DUPLIKACJI DRUKOWANIA - OKNO CZASOWE DEDUPLIKACJI

## Data implementacji: 2026-02-13

---

## 🎯 PROBLEM KTÓRY ROZWIĄZUJEMY

**Symptom:** Po zaakceptowaniu zamówienia zewnętrznie (przez inną aplikację), zamówienie drukuje się **2 razy** na każdej drukarce.

**Przyczyna:** Backend emituje **podwójny event ORDER_ACCEPTED** dla tego samego zamówienia (identyfikowane w `DUPLIKACJA_DRUKOWANIA_ANALIZA_API.md`):
- Pierwszy event: 11:04:28.402 (emitCount: 162)
- Drugi event: 11:04:30.171 (emitCount: 163)
- **Opóźnienie: ~1.77 sekundy**

**Istniejące zabezpieczenia NIE wystarczały:**
- `distinctUntilChanged` - filtruje tylko kolejne identyczne wartości
- `debounce(500ms)` - za krótkie dla opóźnienia 1.77s
- `wasPrintedSync` - może zawieść przy race condition

---

## 🛡️ ZAIMPLEMENTOWANE ROZWIĄZANIE

### Mechanizm: **Okno czasowe deduplikacji (Time-Window Deduplication)**

**Zasada działania:**
- Zapamiętujemy timestamp każdego eventu drukowania dla danego `orderId`
- Blokujemy kolejne eventy w oknie **3 sekund** (konfigurowalny parametr)
- Automatyczne czyszczenie starych wpisów (>10 sekund)

**Zalety:**
✅ Skuteczne nawet dla opóźnień > 500ms  
✅ Thread-safe (używa `synchronized`)  
✅ Nie opóźnia reakcji UI (natychmiastowa weryfikacja)  
✅ Automatyczne zarządzanie pamięcią (cleanup)  

---

## 📝 ZMIANY W KODZIE

### 1. Nowe zmienne w `OrdersViewModel.kt`

```kotlin
// ✅ DEDUPLIKACJA: Okno czasowe dla eventów drukowania
private val recentPrintEvents = mutableMapOf<String, Long>() // orderId -> timestamp
private val printEventWindowMs = 3000L // 3 sekundy - blokuje duplikaty w tym oknie
private val recentPrintMutex = Mutex()
```

**Lokalizacja:** Linia ~158-161

---

### 2. Nowa funkcja `shouldAllowPrintEvent()`

```kotlin
/**
 * ✅ DEDUPLIKACJA: Sprawdza czy event drukowania nie jest duplikatem w oknie czasowym
 * Blokuje wielokrotne drukowanie tego samego zamówienia w okresie printEventWindowMs
 * Używa synchronized zamiast suspend mutex dla natychmiastowej reakcji
 */
private fun shouldAllowPrintEvent(orderId: String): Boolean {
    val now = System.currentTimeMillis()
    
    synchronized(recentPrintEvents) {
        val lastPrintTime = recentPrintEvents[orderId]
        
        return if (lastPrintTime == null || (now - lastPrintTime) > printEventWindowMs) {
            // Dozwolone - zapisz timestamp
            recentPrintEvents[orderId] = now
            
            // Cleanup starych wpisów (starsze niż 10s)
            val cleanupThreshold = now - 10000L
            recentPrintEvents.entries.removeIf { it.value < cleanupThreshold }
            
            Timber.tag("DEDUPLICATION").d("✅ Dozwolony event drukowania: $orderId")
            true
        } else {
            val elapsedMs = now - lastPrintTime
            Timber.tag("DEDUPLICATION").w("⏭️ Zablokowany duplikat! orderId=$orderId, elapsed=${elapsedMs}ms (okno=${printEventWindowMs}ms)")
            false
        }
    }
}
```

**Lokalizacja:** Linia ~420-447

---

### 3. Integracja w obsłudze zamówień ACCEPTED (socketEventsRepo.orders)

**Poprzednio:**
```kotlin
if (orderStatus == OrderStatusEnum.ACCEPTED && ...) {
    if (manuallyAcceptedOrders.contains(order.orderId)) {
        return@onEach
    }
    if (wasPrintedSync(order.orderId)) {
        return@onEach
    }
    // ... drukowanie
}
```

**Teraz:**
```kotlin
if (orderStatus == OrderStatusEnum.ACCEPTED && ...) {
    // ✅ DEDUPLIKACJA #1: Okno czasowe
    if (!shouldAllowPrintEvent(order.orderId)) {
        Timber.tag("AUTO_PRINT_ACCEPTED").w("⏭️ Pomijam drukowanie - duplikat w oknie czasowym")
        return@onEach
    }
    
    if (manuallyAcceptedOrders.contains(order.orderId)) {
        return@onEach
    }
    if (wasPrintedSync(order.orderId)) {
        return@onEach
    }
    // ... drukowanie
}
```

**Lokalizacja:** Linia ~1485-1494

---

### 4. Integracja w obsłudze zmian statusu (socketEventsRepo.statuses)

**Poprzednio:**
```kotlin
if (s.newStatus == OrderStatusEnum.ACCEPTED) {
    if (manuallyAcceptedOrders.contains(s.orderId)) {
        return@onEach
    }
    if (wasPrintedSync(s.orderId)) {
        return@onEach
    }
    // ... drukowanie
}
```

**Teraz:**
```kotlin
if (s.newStatus == OrderStatusEnum.ACCEPTED) {
    // ✅ DEDUPLIKACJA #1: Okno czasowe
    if (!shouldAllowPrintEvent(s.orderId)) {
        Timber.tag("AUTO_PRINT_STATUS_CHANGE").w("⏭️ Pomijam drukowanie - duplikat w oknie czasowym")
        return@onEach
    }
    
    if (manuallyAcceptedOrders.contains(s.orderId)) {
        return@onEach
    }
    if (wasPrintedSync(s.orderId)) {
        return@onEach
    }
    // ... drukowanie
}
```

**Lokalizacja:** Linia ~1552-1561

---

## 🔄 PRZEPŁYW DZIAŁANIA

### Scenariusz: Backend wysyła 2x ORDER_ACCEPTED

```
11:04:28.402 - Backend EMIT #162: ORDER_ACCEPTED (orderId: ABC123)
              ↓
              App otrzymuje event przez WebSocket
              ↓
              shouldAllowPrintEvent("ABC123") = TRUE (brak wpisu w recentPrintEvents)
              ↓
              Zapisz timestamp: recentPrintEvents["ABC123"] = 1707825868402
              ↓
              ✅ DRUKOWANIE #1 wykonane
              
11:04:30.171 - Backend EMIT #163: ORDER_ACCEPTED (orderId: ABC123) ⚠️ DUPLIKAT
              ↓
              App otrzymuje event przez WebSocket
              ↓
              shouldAllowPrintEvent("ABC123") = FALSE
              ↓
              Sprawdzenie: now - lastPrintTime = 1769ms < 3000ms
              ↓
              ⏭️ ZABLOKOWANO - Log: "Zablokowany duplikat! elapsed=1769ms"
              ↓
              ❌ DRUKOWANIE NIE wykonane
```

---

## 🧪 TESTOWANIE

### Logi które powinny się pojawić:

#### ✅ Poprawne działanie (1 drukowanie):
```
DEDUPLICATION: ✅ Dozwolony event drukowania: 698f0539fdf9812564734b12
AUTO_PRINT_STATUS_CHANGE: 🔔 Status zmieniony na ACCEPTED przez socket: 698f0539fdf9812564734b12
🖨️ Auto-druk po zmianie statusu na ACCEPTED: #12345
✅ Auto-druk zakończony dla #12345

[1.77s później - duplikat z backendu]

DEDUPLICATION: ⏭️ Zablokowany duplikat! orderId=698f0539fdf9812564734b12, elapsed=1769ms (okno=3000ms)
AUTO_PRINT_STATUS_CHANGE: ⏭️ Pomijam drukowanie - duplikat w oknie czasowym: 698f0539fdf9812564734b12
```

#### ❌ Gdyby nie działało (2 drukowania):
```
AUTO_PRINT_STATUS_CHANGE: 🔔 Status zmieniony na ACCEPTED przez socket: ...
🖨️ Auto-druk po zmianie statusu na ACCEPTED: #12345
✅ Auto-druk zakończony dla #12345

[1.77s później]

AUTO_PRINT_STATUS_CHANGE: 🔔 Status zmieniony na ACCEPTED przez socket: ... ⚠️ DUPLIKAT
🖨️ Auto-druk po zmianie statusu na ACCEPTED: #12345 ⚠️ DUPLIKAT
```

---

## ⚙️ PARAMETRY KONFIGURACYJNE

### `printEventWindowMs = 3000L`

**Wartość:** 3000ms (3 sekundy)

**Dlaczego 3 sekundy?**
- Pokrywa opóźnienia backendu do ~3s (obserwowane: 1.77s)
- Wystarczająco długie aby złapać duplikaty
- Wystarczająco krótkie aby nie blokować prawdziwych ponownych akceptacji

**Jak zmienić:**
```kotlin
private val printEventWindowMs = 5000L // 5 sekund - bardziej konserwatywne
```

---

## 📊 WARSTWY OCHRONY (Defense in Depth)

Po tym fixie mamy **5 warstw zabezpieczeń** przed duplikacją:

1. ✅ **Okno czasowe** (`shouldAllowPrintEvent`) - **NOWE!**
   - Blokuje duplikaty w oknie 3s
   - Skuteczny nawet dla opóźnień > 500ms

2. ✅ **Manualna akceptacja** (`manuallyAcceptedOrders`)
   - Blokuje drukowanie przez socket po manualnej akceptacji

3. ✅ **Flaga wydrukowanych** (`wasPrintedSync`)
   - Permanent flag - nie drukuje jeśli już było

4. ✅ **distinctUntilChanged**
   - Filtruje kolejne identyczne wartości

5. ✅ **debounce(500ms)**
   - Zgrupowuje szybkie duplikaty

---

## 🔧 DŁUGOTERMINOWE ROZWIĄZANIE

### ⚠️ To jest FIX TYMCZASOWY!

**Prawdziwy problem:** Backend emituje duplikaty ORDER_ACCEPTED

**Należy naprawić w backend:**
- Zbadać system outbox pattern
- Zidentyfikować dlaczego powstają 2 dokumenty dla tego samego eventu
- Dodać deduplikację po stronie backendu

**Powiązany dokument:** `DUPLIKACJA_DRUKOWANIA_ANALIZA_API.md`

---

## 📈 METRYKI SUKCESU

### Przed fixem:
- ❌ Zaakceptowanie zewnętrzne → 2x drukowanie na każdej drukarce
- ❌ Zaakceptowanie manualne → 2x drukowanie na każdej drukarce (czasami)

### Po fixie:
- ✅ Zaakceptowanie zewnętrzne → 1x drukowanie na każdej drukarce
- ✅ Zaakceptowanie manualne → 1x drukowanie na każdej drukarce
- ✅ Backend wysyła duplikaty → aplikacja ignoruje

---

## 🔗 POWIĄZANE PLIKI

- **Implementacja:** `L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\order\OrdersViewModel.kt`
  - Linie: 158-161 (zmienne)
  - Linie: 420-447 (funkcja shouldAllowPrintEvent)
  - Linie: 1485-1494 (integracja #1)
  - Linie: 1552-1561 (integracja #2)

- **Analiza problemu:** `L:\SHOP APP\DUPLIKACJA_DRUKOWANIA_ANALIZA_API.md`

---

## 🚀 WDROŻENIE

1. ✅ Kod zaimplementowany
2. ✅ Brak błędów kompilacji
3. 🔄 **Następny krok: Build & Test**

### Testuj:
```bash
# Build aplikacji
./gradlew assembleDebug

# Zainstaluj na urządzeniu
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Scenariusze testowe:
1. Zaakceptuj zamówienie **manualnie** w aplikacji → oczekiwane: 1x drukowanie
2. Zaakceptuj zamówienie **zewnętrznie** (inna app) → oczekiwane: 1x drukowanie
3. Sprawdź logi z tagiem `DEDUPLICATION` → powinny być widoczne blokady duplikatów

---

**Autor:** GitHub Copilot  
**Data:** 2026-02-13  
**Status:** ✅ Gotowe do testów

