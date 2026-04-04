# 🔄 Problem: Drukowanie Po Restarcie Aplikacji

## 📋 Opis Problemu

**Zgłoszenie użytkownika:**
> "Za każdym razem jak wyłączę i włączę aplikację drukuje się zamówienie mimo że jest zaakceptowane i zostało wydrukowane po zaakceptowaniu automatycznie."

## 🐛 Analiza Przyczyny

### Timeline Problematyczny:

```
t=0ms:  Restart aplikacji
        └─ OrdersViewModel init {
             loadPrintedOrderIds()    // ← viewModelScope.launch (ASYNC!)
             observeSocketEvents()    // ← Startuje NATYCHMIAST!
           }

t=1ms:  Socket połączony → wysyła zamówienia ACCEPTED

t=2ms:  socketEventsRepo.orders otrzymuje zamówienie
        └─ wasPrintedSync(orderId)?
           └─ printedOrderIds.contains(orderId)
              ├─ printedOrderIds = [] ❌ (PUSTE - jeszcze nie załadowane!)
              └─ return false → DRUKUJE ❌

t=50ms: loadPrintedOrderIds zakończone
        └─ printedOrderIds = [id1, id2, id3...] ✅ (ZA PÓŹNO!)
```

### Problem:

**`loadPrintedOrderIds()` jest asynchroniczne** (`viewModelScope.launch`), ale **`observeSocketEvents()` startuje natychmiast**!

Socket może wysłać zamówienia **ZANIM** `printedOrderIds` zostaną załadowane z DataStore!

## ✅ ROZWIĄZANIE

### Mechanizm: Flaga Gotowości `printedOrdersLoaded`

**1. Dodano flagę volatile:**
```kotlin
// OrdersViewModel.kt linia ~157
// Flaga - czy printedOrderIds zostały załadowane z DataStore
@Volatile
private var printedOrdersLoaded = false
```

**2. Ustawienie flagi po załadowaniu:**
```kotlin
// OrdersViewModel.kt linia ~328
private fun loadPrintedOrderIds() {
    viewModelScope.launch {
        val stored = appPreferencesManager.getPrintedOrderIds()
        synchronized(printedOrderIds) {
            printedOrderIds.addAll(stored)
        }
        printedOrdersLoaded = true  // ✅ Oznacz jako załadowane!
        Timber.d("📂 Załadowano ${stored.size} wydrukowanych zamówień z DataStore")
    }
}
```

**3. Sprawdzenie flagi przed drukowaniem:**
```kotlin
// OrdersViewModel.kt linia ~397
private fun wasPrintedSync(orderId: String): Boolean {
    // ✅ CRITICAL: Jeśli dane jeszcze nie załadowane, uznaj że BYŁO wydrukowane
    // Zapobiega drukowaniu starych zamówień przy starcie aplikacji
    if (!printedOrdersLoaded) {
        Timber.tag("PRINT_GUARD").d("⏳ PrintedOrderIds nie załadowane - pomijam: $orderId")
        return true  // ← Blokuj drukowanie dopóki dane nie zostaną załadowane!
    }

    return synchronized(printedOrderIds) {
        printedOrderIds.contains(orderId)
    }
}
```

## 🎯 Nowa Sekwencja (PO FIXIE)

```
t=0ms:  Restart aplikacji
        printedOrdersLoaded = false ✅
        └─ init {
             loadPrintedOrderIds()    // Async (w tle)
             observeSocketEvents()    // Startuje natychmiast
           }

t=1ms:  Socket połączony → wysyła zamówienia ACCEPTED

t=2ms:  socketEventsRepo.orders otrzymuje zamówienie (11657)
        └─ wasPrintedSync(11657)?
           ├─ printedOrdersLoaded? NIE! ✅
           ├─ return true (BLOKADA!) ✅
           └─ Log: "⏳ PrintedOrderIds nie załadowane - pomijam: 11657"
           
        ⏭️ BRAK DRUKOWANIA! ✅

t=50ms: loadPrintedOrderIds zakończone
        └─ printedOrderIds = [11657, ...]
           printedOrdersLoaded = true ✅

--- Teraz dopiero można drukować nowe zamówienia ---

t=5min: Nowe zamówienie (12345) przychodzi
        └─ wasPrintedSync(12345)?
           ├─ printedOrdersLoaded? TAK ✅
           ├─ printedOrderIds.contains(12345)? NIE
           └─ return false → DRUKUJE ✅ (nowe zamówienie)
```

## 🛡️ Dodatkowe Zabezpieczenia

### Dlaczego `@Volatile`?

```kotlin
@Volatile
private var printedOrdersLoaded = false
```

**Powód:** Zapewnia widoczność zmiennej między wątkami:
- `loadPrintedOrderIds()` wykonuje się w `viewModelScope` (może być inny wątek)
- `wasPrintedSync()` wykonuje się na głównym wątku (socket event)
- `@Volatile` gwarantuje że zmiana flagi będzie natychmiast widoczna dla wszystkich wątków

### Dlaczego `synchronized` w `loadPrintedOrderIds`?

```kotlin
synchronized(printedOrderIds) {
    printedOrderIds.addAll(stored)
}
```

**Powód:** Thread-safety przy dodawaniu elementów:
- `printedOrderIds` jest `MutableSet` (nie thread-safe)
- `loadPrintedOrderIds()` może być wywoływane równolegle z `wasPrintedSync()`
- `synchronized` chroni przed race condition

## 📊 Przypadki Użycia

### Case 1: Restart aplikacji z zamówieniami ACCEPTED
```
Zamówienia w bazie: [11657 ACCEPTED, 9880 ACCEPTED]
printedOrderIds w DataStore: [11657, 9880]

Restart → loadPrintedOrderIds() (async)
Socket wysyła: 11657 ACCEPTED
wasPrintedSync(11657)? 
  └─ printedOrdersLoaded = false → return true (BLOKADA) ✅

REZULTAT: Brak drukowania ✅
```

### Case 2: Nowe zamówienie po załadowaniu
```
printedOrdersLoaded = true
Socket wysyła: 12345 ACCEPTED (NOWE)
wasPrintedSync(12345)?
  └─ printedOrdersLoaded = true
     └─ printedOrderIds.contains(12345) = false
        └─ return false → DRUKUJE ✅

REZULTAT: Drukuje nowe zamówienie ✅
```

### Case 3: Duplikat po załadowaniu
```
printedOrdersLoaded = true
printedOrderIds = [11657, 9880]
Socket wysyła: 11657 ACCEPTED (stare)
wasPrintedSync(11657)?
  └─ printedOrdersLoaded = true
     └─ printedOrderIds.contains(11657) = true
        └─ return true → BLOKUJE ✅

REZULTAT: Brak drukowania (duplikat) ✅
```

## ✅ Status

- ✅ **Kod:** Poprawiony (OrdersViewModel.kt linia ~157, ~328, ~397)
- ✅ **Kompilacja:** OK (tylko ostrzeżenia)
- ✅ **Fix:** Flaga printedOrdersLoaded blokuje drukowanie przed załadowaniem
- ✅ **Gotowe do testowania**

## 🎯 Rezultat

**PRZED FIX:**
```
Restart → Socket wysyła zamówienia → printedOrderIds = [] → DRUKUJE ❌
```

**PO FIX:**
```
Restart → Socket wysyła zamówienia → printedOrdersLoaded = false → BLOKUJE ✅
```

---

**Data rozwiązania:** 2026-02-13  
**Wersja:** v1.8.0  
**Typ:** Critical Bug Fix - Print After App Restart  
**Status:** ✅ ROZWIĄZANE

## 💡 Kluczowa Lekcja

**Problem:**  
Asynchroniczne ładowanie danych z DataStore w `init` może spowodować że dane nie są gotowe gdy socket zaczyna wysyłać eventy!

**Rozwiązanie:**  
Flaga `@Volatile` + sprawdzenie w synchronicznej funkcji przed drukowaniem. Blokuj wszystkie akcje dopóki dane nie zostaną załadowane!

