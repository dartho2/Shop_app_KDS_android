# 🔧 FIX: Persystencja Wydrukowanych Zamówień (DataStore)

## 🐛 Problem

**Zgłoszenie:** Przy starcie aplikacji, zamówienia które są już w bazie (PROCESSING/ACCEPTED) drukują się ponownie, mimo że mogły być już wcześniej wydrukowane.

**Dodatkowo:** Zamówienia zaakceptowane drukują się **4x na każdej drukarce** zamiast 1x.

---

## 🔍 Przyczyna

### Problem 1: Brak Persystencji

```kotlin
// PRZED
private val printedOrderIds = mutableSetOf<String>()
```

**Problem:**
- `printedOrderIds` jest tylko w pamięci RAM
- Przy restarcie aplikacji → `printedOrderIds` jest pusty
- Zamówienia w bazie → sprawdzenie `contains(orderId)` → FALSE → **drukuje ponownie!**

### Problem 2: Czterokrotne Drukowanie

Przy zaakceptowaniu zamówienia **zewnętrznie** (zmiana statusu przez socket) drukuje się 4x, ponieważ:

**Główny problem:** `socketEventsRepo.statuses` NIE sprawdzał czy zamówienie było już wydrukowane!

1. **socketEventsRepo.statuses** - socket wysyła zmianę → DRUKUJE #1 (❌ brak sprawdzenia!)
2. **socketEventsRepo.orders** - socket wysyła całe zamówienie → DRUKUJE #2
3. **Ponowne wywołanie statuses** → DRUKUJE #3
4. **Ponowne wywołanie orders** → DRUKUJE #4

**Przyczyna:** Brak `if (printedOrderIds.contains(s.orderId))` w `socketEventsRepo.statuses`

---

## ✅ Rozwiązanie

### 1. Dodano Persystencję w DataStore

**AppPreferencesManager.kt:**

```kotlin
// Nowy klucz
val PRINTED_ORDER_IDS = stringPreferencesKey("printed_order_ids")

// Metody
suspend fun getPrintedOrderIds(): Set<String> {
    val stored = dataStore.data.map { it[Keys.PRINTED_ORDER_IDS] }.first()
    return stored?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
}

suspend fun addPrintedOrderId(orderId: String) {
    val current = getPrintedOrderIds().toMutableSet()
    current.add(orderId)
    
    // Limit 1000 ostatnich
    val limited = if (current.size > 1000) {
        current.toList().takeLast(1000).toSet()
    } else {
        current
    }
    
    savePrintedOrderIds(limited)
}
```

---

### 2. Ładowanie przy Starcie

**OrdersViewModel.kt - init:**

```kotlin
init {
    initUserSession()
    loadPrintedOrderIds() // ✅ Ładuj z DataStore
    observeSocketEvents()
    // ...
}

private fun loadPrintedOrderIds() {
    viewModelScope.launch {
        val stored = appPreferencesManager.getPrintedOrderIds()
        printedOrderIds.addAll(stored)
        Timber.d("📂 Załadowano ${stored.size} wydrukowanych zamówień")
    }
}
```

---

### 3. Zapisywanie przy Drukowaniu

**Funkcja pomocnicza:**

```kotlin
private fun markAsPrinted(orderId: String) {
    printedOrderIds.add(orderId) // Pamięć (szybkie sprawdzanie)
    
    // DataStore (persystencja)
    viewModelScope.launch {
        try {
            appPreferencesManager.addPrintedOrderId(orderId)
            Timber.d("💾 Zapisano do DataStore: $orderId")
        } catch (e: Exception) {
            Timber.e(e, "❌ Błąd zapisu: $orderId")
        }
    }
}
```

**Zastosowanie we wszystkich 4 miejscach + SPRAWDZENIE PRZED:**

1. ✅ **DINE_IN** - sprawdza `contains()` → `markAsPrinted(order.orderId)`
2. ✅ **ACCEPTED** - sprawdza `contains()` → `markAsPrinted(order.orderId)`
3. ✅ **Socket statuses** - sprawdza `contains()` → `markAsPrinted(s.orderId)` ⭐ **NAPRAWIONE v1.7.1**
4. ✅ **Manualna akceptacja** - `markAsPrinted(res.value.orderId)`

**KLUCZOWA NAPRAWA (v1.7.1):**
```kotlin
// socketEventsRepo.statuses - DODANO sprawdzenie
if (printedOrderIds.contains(s.orderId)) {
    Timber.d("⏭️ Pomijam drukowanie - zamówienie już było wydrukowane")
    return@onEach
}

markAsPrinted(s.orderId) // Teraz dopiero oznacza
```

---

## 📊 Przepływ Po Naprawie

### Start Aplikacji

```
1. OrdersViewModel.init()
   └─> loadPrintedOrderIds()
       └─> Ładuje z DataStore: ["ORDER-001", "ORDER-002", ...]
           └─> printedOrderIds = mutableSetOf("ORDER-001", "ORDER-002", ...)

2. Socket reconnect → Backend wysyła ostatnie zamówienia
   └─> observeSocketEvents()
       ├─> Zamówienie ORDER-001 (ACCEPTED)
       ├─> Sprawdzenie: printedOrderIds.contains("ORDER-001")? → TAK! ✅
       └─> ⏭️ POMIJA drukowanie

REZULTAT: Brak duplikacji przy restarcie! ✅
```

---

### Nowe Zamówienie → Akceptacja

```
1. Nowe zamówienie (PROCESSING) przychodzi
   └─> observeSocketEvents()
       └─> Status PROCESSING (nie ACCEPTED) → ❌ Nie drukuje

2. Pracownik akceptuje ręcznie
   └─> executeOrderUpdate()
       ├─> markAsPrinted(orderId) ✅ (dodaje do pamięci + DataStore)
       └─> DRUKUJE #1

3. Socket wysyła status change (PROCESSING → ACCEPTED)
   └─> socketEventsRepo.statuses
       ├─> Sprawdzenie: printedOrderIds.contains(orderId)? → TAK! ✅
       └─> ⏭️ POMIJA drukowanie

4. Socket wysyła całe zamówienie (status ACCEPTED)
   └─> socketEventsRepo.orders
       ├─> Sprawdzenie: printedOrderIds.contains(orderId)? → TAK! ✅
       └─> ⏭️ POMIJA drukowanie

REZULTAT: Drukuje TYLKO RAZ! ✅
```

---

## 🔧 Zmiany w Kodzie

### AppPreferencesManager.kt

**Dodane:**
- ✅ Klucz `PRINTED_ORDER_IDS`
- ✅ `getPrintedOrderIds()` - odczyt z DataStore
- ✅ `savePrintedOrderIds()` - zapis do DataStore
- ✅ `addPrintedOrderId()` - dodanie pojedynczego ID (z limitem 1000)
- ✅ `wasPrinted()` - sprawdzenie czy było wydrukowane
- ✅ `clearPrintedOrderIds()` - czyszczenie (maintenance)

---

### OrdersViewModel.kt

**Dodane:**

1. **loadPrintedOrderIds()** (linia ~311)
   - Ładuje przy starcie z DataStore
   - Dodaje do `printedOrderIds` Set

2. **markAsPrinted(orderId)** (linia ~322)
   - Dodaje do pamięci (synchronicznie)
   - Zapisuje do DataStore (asynchronicznie)

**Zmodyfikowane (4 miejsca):**

1. **observeSocketEvents() - DINE_IN** → `markAsPrinted(order.orderId)`
2. **observeSocketEvents() - ACCEPTED** → `markAsPrinted(order.orderId)`
3. **socketEventsRepo.statuses** → `markAsPrinted(s.orderId)`
4. **executeOrderUpdate()** → `markAsPrinted(res.value.orderId)`

---

## 🎯 Korzyści

### ✅ Przed vs Po

**PRZED:**
- Restart → Drukuje wszystkie zamówienia z bazy ponownie
- Akceptacja → Drukuje 4x (wszystkie ścieżki)
- Dane tracone przy zamknięciu aplikacji

**PO:**
- Restart → Pomija już wydrukowane (z DataStore)
- Akceptacja → Drukuje 1x (inne ścieżki pomijane)
- Dane przetrwają restart, force kill, reboot

---

## 🧪 Testy

### Test 1: Restart Aplikacji

```
1. Wyślij zamówienie PROCESSING
2. Zaakceptuj ręcznie → DRUKUJE (1x)
3. Zamknij aplikację
4. Otwórz aplikację ponownie
5. Sprawdź logi:

Oczekiwane:
📂 Załadowano 1 wydrukowanych zamówień z DataStore
⏭️ Pomijam drukowanie - zamówienie już było wydrukowane

6. Sprawdź wydruki: 0 nowych ✅
```

---

### Test 2: Force Kill

```
1. Wyślij zamówienie
2. Zaakceptuj → DRUKUJE
3. Force kill:
   adb shell am force-stop com.itsorderchat
4. Otwórz aplikację
5. Sprawdź logi:

Oczekiwane:
📂 Załadowano 1 wydrukowanych zamówień z DataStore
⏭️ Pomijam drukowanie - zamówienie już było wydrukowane

6. Rezultat: Nie drukuje ponownie ✅
```

---

### Test 3: Manualna Akceptacja (Brak 4x Duplikacji)

```
1. Wyślij PROCESSING
2. Zaakceptuj ręcznie
3. Sprawdź logi:

Oczekiwane:
🖨️ Wywołuję printAfterOrderAccepted dla ORDER-123
💾 Zapisano do DataStore: ORDER-123
✅ Drukowanie zakończone

(socket confirmation #1)
⏭️ Pomijam drukowanie - zamówienie było manualnie zaakceptowane

(socket confirmation #2 - statuses)
⏭️ Pomijam drukowanie - zamówienie już było wydrukowane

(socket orders)
⏭️ Pomijam drukowanie - zamówienie już było wydrukowane

4. Sprawdź wydruki: 1x Główna + 1x Kuchnia ✅
```

---

## 🔍 Diagnostyka

### Logi do Monitorowania

```bash
# Ładowanie z DataStore
adb logcat | grep "📂 Załadowano"

# Zapisywanie do DataStore
adb logcat | grep "💾 Zapisano"

# Pomijanie duplikacji
adb logcat | grep "⏭️ Pomijam drukowanie"
```

### Oczekiwane Logi (Sukces)

**Przy starcie:**
```
📂 Załadowano 15 wydrukowanych zamówień z DataStore
```

**Przy drukowaniu:**
```
🖨️ Auto-druk DINE_IN dla ORDER-456
💾 Zapisano do DataStore: ORDER-456
```

**Przy duplikacji:**
```
⏭️ Pomijam drukowanie - zamówienie już było wydrukowane: ORDER-456
```

---

## 💾 Przechowywanie Danych

### Format w DataStore

```
PRINTED_ORDER_IDS = "ORDER-001,ORDER-002,ORDER-003,..."
```

**Limit:** 1000 ostatnich zamówień

**Czemu 1000?**
- Balans między rozmiarem a użytecznością
- ~50KB danych (50 znaków * 1000)
- Automatyczne przycinanie starszych

---

## 🛠️ Maintenance

### Czyszczenie Listy (Opcjonalne)

```kotlin
// W przyszłości, jeśli potrzebne
viewModelScope.launch {
    appPreferencesManager.clearPrintedOrderIds()
}
```

**Kiedy?**
- Reset ustawień drukarki
- Factory reset aplikacji
- Manual cleanup przez admina

---

## ⚠️ Ważne Notatki

### 1. Limit 1000 Zamówień

Po przekroczeniu → automatyczne usuwanie **najstarszych**

```kotlin
val limited = if (current.size > 1000) {
    current.toList().takeLast(1000).toSet() // Ostatnie 1000
} else {
    current
}
```

### 2. Thread Safety

- `printedOrderIds.add()` - synchroniczne (główny wątek)
- `appPreferencesManager.addPrintedOrderId()` - asynchroniczne (viewModelScope)
- Brak race condition (dodajemy do pamięci PRZED launch)

### 3. Error Handling

```kotlin
try {
    appPreferencesManager.addPrintedOrderId(orderId)
} catch (e: Exception) {
    Timber.e(e, "❌ Błąd zapisu")
    // Zamówienie jest w pamięci, więc nie drukuje się ponownie w tej sesji
    // Po restarcie może się wydrukować (akceptowalne)
}
```

---

## ✅ Podsumowanie

### Przed Naprawą
```
Start app → Drukuje wszystkie z bazy
Akceptacja → Drukuje 4x (każda ścieżka)
Force kill → Traci dane
```

### Po Naprawie
```
Start app → Ładuje z DataStore → Pomija wydrukowane
Akceptacja → Drukuje 1x → Zapisuje → Pomija resztę
Force kill → Dane przetrwają
```

### Korzyści
- ✅ Brak duplikacji przy restarcie
- ✅ Brak 4x drukowania przy akceptacji
- ✅ Persystencja (DataStore)
- ✅ Automatyczne czyszczenie (limit 1000)
- ✅ Thread-safe
- ✅ Error handling

---

**Data naprawy:** 2026-02-04  
**Wersja:** 1.7.1 (CRITICAL FIX)  
**Status:** ✅ Naprawione - 4x Duplikacja Rozwiązana  
**Issue:** Drukowanie przy restarcie + 4x duplikacja przy zewnętrznej akceptacji

**CRITICAL FIX v1.7.1:**
- ✅ Dodano brakujące sprawdzenie `printedOrderIds.contains()` w `socketEventsRepo.statuses`
- ✅ Naprawiono 4x drukowanie przy zewnętrznej akceptacji przez socket

---

## 📞 Powiązane

- `FIX_RESTART_PRINTING.md` - Pierwsza wersja (in-memory)
- `FIX_SOCKET_STATUS_DUPLICATION.md` - Fix duplikacji socket
- `FIX_DOUBLE_PRINTING_MANUAL_ACCEPTANCE.md` - Fix duplikacji manualnej
- `DOKUMENTACJA_AUTOMATYCZNEGO_DRUKOWANIA.md` - Główna dokumentacja

