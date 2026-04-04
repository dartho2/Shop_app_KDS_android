# 📋 Dokumentacja: Automatyczne Drukowanie - Analiza i Plan Implementacji

**Data utworzenia:** 2026-01-22  
**Status:** ⚠️ CZĘŚCIOWO ZAIMPLEMENTOWANE - WYMAGA POPRAWEK

---

## 🎯 Przegląd Funkcjonalności

Aplikacja powinna wspierać 3 tryby automatycznego drukowania:

1. **AUTO_PRINT** - Drukowanie przy nowym zamówieniu (status PROCESSING)
2. **AUTO_PRINT_ACCEPTED** - Drukowanie po zaakceptowaniu zamówienia
3. **AUTO_PRINT_KITCHEN** - Dodatkowe drukowanie na drukarce kuchennej

---

## 📊 Stan Aktualny (Co Jest Zrobione)

### ✅ 1. Struktura Danych - KOMPLETNA

#### AppPreferencesManager.kt
```kotlin
// Klucze DataStore
val AUTO_PRINT = booleanPreferencesKey("auto_print")
val AUTO_PRINT_ACCEPTED = booleanPreferencesKey("auto_print_accepted")
val AUTO_PRINT_KITCHEN = booleanPreferencesKey("auto_print_kitchen")

// Funkcje odczytu/zapisu
suspend fun getAutoPrintEnabled(): Boolean
suspend fun setAutoPrintEnabled(enabled: Boolean)
suspend fun getAutoPrintAcceptedEnabled(): Boolean
suspend fun setAutoPrintAcceptedEnabled(enabled: Boolean)
suspend fun getAutoPrintKitchenEnabled(): Boolean
suspend fun setAutoPrintKitchenEnabled(enabled: Boolean)
```

**Status:** ✅ Kompletne

---

### ✅ 2. UI - Ekran Ustawień - KOMPLETNY

#### PrintSettingsScreen.kt
- ✅ ViewModel `PrintSettingsViewModel` z państwem dla 3 ustawień
- ✅ UI z 3 switchami:
  - Automatyczne drukowanie (nowe zamówienie)
  - Drukowanie po zaakceptowaniu
  - Drukowanie na kuchni (widoczne gdy jest drukarka KITCHEN)
- ✅ Nawigacja z głównego ekranu ustawień
- ✅ Zapisywanie ustawień w DataStore

**Lokalizacja:** `L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\settings\PrintSettingsScreen.kt`

**Status:** ✅ Kompletne

---

### ✅ 3. PrinterService - Funkcje Drukowania - KOMPLETNE

#### PrinterService.kt

```kotlin
// 1. Drukowanie standardowe
suspend fun printOrder(
    order: Order, 
    useDeliveryInterval: Boolean = false, 
    docType: DocumentType = DocumentType.RECEIPT
)

// 2. Drukowanie po zaakceptowaniu (używa AUTO_PRINT_ACCEPTED i AUTO_PRINT_KITCHEN)
suspend fun printAfterOrderAccepted(order: Order) {
    val autoPrintAccepted = appPreferencesManager.getAutoPrintAcceptedEnabled()
    val autoPrintKitchen = appPreferencesManager.getAutoPrintKitchenEnabled()
    
    if (!autoPrintAccepted) return
    
    // Drukuj na standardowej
    printOrder(order, useDeliveryInterval = false, DocumentType.RECEIPT)
    
    // Drukuj na kuchni jeśli włączone
    if (autoPrintKitchen) {
        delay(2000)
        printOrder(order, useDeliveryInterval = false, DocumentType.KITCHEN_TICKET)
    }
}
```

**Status:** ✅ Kompletne - funkcja `printAfterOrderAccepted` poprawnie sprawdza oba ustawienia

---

## ❌ Problemy do Naprawienia

### 🔴 PROBLEM #1: AUTO_PRINT Używa Starego AppPrefs

**Lokalizacja:** `SocketStaffEventsHandler.kt:305-310`

**Aktualny kod (BŁĘDNY):**
```kotlin
if (isProcessing && AppPrefs.getAutoPrintEnabled()) {  // ❌ STARY SYSTEM
    try {
        printerService.printOrder(order)
    } catch (e: Exception) {
        Timber.tag(TAG).e(e, "Auto-print failed for order: ${order.orderId}")
    }
}
```

**Problem:**
- Używa `AppPrefs.getAutoPrintEnabled()` (SharedPreferences) ❌
- Powinien używać `AppPreferencesManager.getAutoPrintEnabled()` (DataStore) ✅
- Nie używa `suspend` funkcji

**Rozwiązanie:**
```kotlin
if (isProcessing) {
    ioScope.launch {
        val autoPrintEnabled = appPreferencesManager.getAutoPrintEnabled()
        if (autoPrintEnabled) {
            try {
                printerService.printOrder(order, useDeliveryInterval = true)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Auto-print failed for order: ${order.orderId}")
            }
        }
    }
}
```

**Plik do edycji:**
- `L:\SHOP APP\app\src\main\java\com\itsorderchat\service\SocketStaffEventsHandler.kt`
- Linia: ~305-310

---

### 🔴 PROBLEM #2: printAfterOrderAccepted Jest Wywołane, Ale Może Nie Działać

**Lokalizacja:** `OrdersViewModel.kt:1164`

**Aktualny kod:**
```kotlin
printerService.printAfterOrderAccepted(res.value)
```

**Status:** ✅ Wywołanie jest poprawne

**Ale wymaga weryfikacji:**
1. Czy `res.value` to poprawny obiekt Order?
2. Czy funkcja jest wywoływana w coroutine scope?
3. Czy błędy są logowane?

**Zalecana poprawa (dodać try-catch):**
```kotlin
viewModelScope.launch {
    try {
        printerService.printAfterOrderAccepted(res.value)
    } catch (e: Exception) {
        Timber.e(e, "Błąd drukowania po zaakceptowaniu: ${res.value.orderNumber}")
    }
}
```

---

### ⚠️ PROBLEM #3: Brak Dependency Injection dla AppPreferencesManager w SocketStaffEventsHandler

**Aktualny stan:**
```kotlin
class SocketStaffEventsHandler @Inject constructor(
    private val context: Context,
    private val ordersRepository: OrdersRepository,
    private val socketEventsRepo: SocketEventsRepository,
    private val tokenProvider: TokenProvider,
    private val printerService: PrinterService,  // ✅ MA
    private val openHoursRepository: OpenHoursRepository
)
```

**Brakuje:**
```kotlin
private val appPreferencesManager: AppPreferencesManager  // ❌ BRAK!
```

**Rozwiązanie:**
Dodać `AppPreferencesManager` do konstruktora.

---

## 🛠️ Plan Implementacji (Krok po Kroku)

### Krok 1: Napraw SocketStaffEventsHandler

**Plik:** `SocketStaffEventsHandler.kt`

**Zmiany:**

1. **Dodaj dependency:**
```kotlin
@Inject constructor(
    // ...existing...
    private val printerService: PrinterService,
    private val appPreferencesManager: AppPreferencesManager,  // NOWE
    private val openHoursRepository: OpenHoursRepository
)
```

2. **Popraw handleNewOrProcessingOrder:**
```kotlin
private suspend fun handleNewOrProcessingOrder(args: Array<Any>) {
    val orderWrapper = parsePayload(args, OrderWrapper::class.java) ?: return
    val order = orderWrapper.order
    val json = gson.toJson(orderWrapper)
    Timber.tag("EVENT").i("ORDER: ${order.orderNumber}, Action: ORDER_CREATED")

    val slugEnum = order.orderStatus.slug?.let {
        runCatching { OrderStatusEnum.valueOf(it) }.getOrNull()
    } ?: OrderStatusEnum.UNKNOWN

    val isProcessing = slugEnum == OrderStatusEnum.PROCESSING

    if (isProcessing) {
        Timber.tag(TAG).d("Handling new order: ${order.orderId} (Status: PROCESSING) -> CHECKING AUTO-PRINT")
    } else {
        Timber.tag(TAG).d("Handling new order: ${order.orderId} (Status: ${order.orderStatus.slug}) -> SILENTLY SAVING")
    }

    if (tokenProvider.getRole() != UserRole.COURIER) {
        runCatching {
            ordersRepository.insertOrUpdateOrder(OrderMapper.fromOrder(order))
            socketEventsRepo.emitOrder(order)
            
            // ✅ NOWA IMPLEMENTACJA AUTO-PRINT
            if (isProcessing) {
                val autoPrintEnabled = appPreferencesManager.getAutoPrintEnabled()
                Timber.tag(TAG).d("Auto-print setting: $autoPrintEnabled")
                
                if (autoPrintEnabled) {
                    try {
                        Timber.tag(TAG).i("🖨️ AUTO-PRINT: Rozpoczynam drukowanie zamówienia ${order.orderNumber}")
                        printerService.printOrder(order, useDeliveryInterval = true)
                        Timber.tag(TAG).i("✅ AUTO-PRINT: Pomyślnie wydrukowano ${order.orderNumber}")
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "❌ AUTO-PRINT: Błąd drukowania zamówienia ${order.orderNumber}")
                    }
                } else {
                    Timber.tag(TAG).d("⏭️ AUTO-PRINT: Wyłączone - pomijam drukowanie")
                }
            }
        }.onFailure { e ->
            Timber.tag(TAG).e(e, "Error handling new order")
        }
    }
}
```

---

### Krok 2: Dodaj Logi w PrinterService.printAfterOrderAccepted

**Plik:** `PrinterService.kt`

**Zmiany:**
```kotlin
suspend fun printAfterOrderAccepted(order: Order) {
    val autoPrintAccepted = appPreferencesManager.getAutoPrintAcceptedEnabled()
    val autoPrintKitchen = appPreferencesManager.getAutoPrintKitchenEnabled()

    Timber.d("📋 printAfterOrderAccepted: order=${order.orderNumber}")
    Timber.d("  ├─ autoPrintAccepted=$autoPrintAccepted")
    Timber.d("  └─ autoPrintKitchen=$autoPrintKitchen")

    if (!autoPrintAccepted) {
        Timber.d("⏭️ Automatyczne drukowanie po zaakceptowaniu WYŁĄCZONE - pomijam")
        return
    }

    printMutex.lock()
    try {
        Timber.d("🖨️ Drukuję na drukarce STANDARDOWEJ...")
        
        // Drukuj na drukarce standardowej
        val standardTargets = resolveTargetsFor(DocumentType.RECEIPT)
        if (standardTargets.isEmpty()) {
            Timber.w("⚠️ Brak skonfigurowanej drukarki standardowej")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Brak skonfigurowanej drukarki standardowej", Toast.LENGTH_SHORT).show()
            }
        } else {
            for (t in standardTargets) {
                Timber.d("  ├─ Drukuję na: ${t.printer.name}")
                printOne(t, order, useDeliveryInterval = false)
                delay(500)
            }
            Timber.d("✅ Drukowanie STANDARDOWE zakończone")
        }

        // Drukuj na kuchni jeśli włączone
        if (autoPrintKitchen) {
            val kitchenTargets = resolveTargetsFor(DocumentType.KITCHEN_TICKET)
            if (kitchenTargets.isNotEmpty()) {
                Timber.d("🍳 Drukuję również na KUCHNI (autoPrintKitchen=true)")
                delay(2000) // Opóźnienie przed przełączeniem na inną drukarkę
                for (t in kitchenTargets) {
                    Timber.d("  ├─ Drukuję na: ${t.printer.name}")
                    printOne(t, order, useDeliveryInterval = false)
                    delay(500)
                }
                Timber.d("✅ Drukowanie KUCHNIA zakończone")
            } else {
                Timber.w("⚠️ autoPrintKitchen=true, ale brak skonfigurowanej drukarki kuchennej")
            }
        } else {
            Timber.d("⏭️ Drukowanie na KUCHNI wyłączone")
        }
    } finally {
        printMutex.unlock()
    }
}
```

---

### Krok 3: Dodaj Try-Catch w OrdersViewModel

**Plik:** `OrdersViewModel.kt`

**Lokalizacja:** ~linia 1164

**Zamień:**
```kotlin
printerService.printAfterOrderAccepted(res.value)
```

**Na:**
```kotlin
viewModelScope.launch {
    try {
        Timber.d("🖨️ OrdersViewModel: Wywołuję printAfterOrderAccepted dla ${res.value.orderNumber}")
        printerService.printAfterOrderAccepted(res.value)
    } catch (e: Exception) {
        Timber.e(e, "❌ OrdersViewModel: Błąd drukowania po zaakceptowaniu: ${res.value.orderNumber}")
        // Opcjonalnie: pokaż Toast użytkownikowi
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Błąd drukowania: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
```

---

## 🧪 Plan Testowania

### Test 1: AUTO_PRINT (Nowe Zamówienie)

**Kroki:**
1. Włącz "Automatyczne drukowanie" w ustawieniach
2. Wyślij testowe zamówienie przez Socket (status PROCESSING)
3. Sprawdź logi:
   ```
   AUTO-PRINT: Rozpoczynam drukowanie zamówienia KR-XXXX
   ✅ AUTO-PRINT: Pomyślnie wydrukowano KR-XXXX
   ```
4. Zweryfikuj fizyczny wydruk

**Oczekiwany rezultat:**
- ✅ Zamówienie się wydrukuje automatycznie
- ✅ W logach widać wszystkie kroki

---

### Test 2: AUTO_PRINT_ACCEPTED (Po Zaakceptowaniu)

**Kroki:**
1. Włącz "Drukowanie po zaakceptowaniu" w ustawieniach
2. **Wyłącz** "Drukowanie na kuchni"
3. Zaakceptuj zamówienie
4. Sprawdź logi:
   ```
   🖨️ OrdersViewModel: Wywołuję printAfterOrderAccepted dla KR-XXXX
   🖨️ Drukuję na drukarce STANDARDOWEJ...
   ✅ Drukowanie STANDARDOWE zakończone
   ⏭️ Drukowanie na KUCHNI wyłączone
   ```
5. Zweryfikuj fizyczny wydruk (tylko 1 drukarka)

**Oczekiwany rezultat:**
- ✅ Zamówienie się wydrukuje po zaakceptowaniu
- ✅ Tylko na drukarce standardowej

---

### Test 3: AUTO_PRINT_KITCHEN (Drukowanie na Kuchni)

**Kroki:**
1. Włącz "Drukowanie po zaakceptowaniu"
2. **Włącz** "Drukowanie na kuchni"
3. Upewnij się że jest skonfigurowana drukarka typu KITCHEN
4. Zaakceptuj zamówienie
5. Sprawdź logi:
   ```
   🖨️ Drukuję na drukarce STANDARDOWEJ...
   ✅ Drukowanie STANDARDOWE zakończone
   🍳 Drukuję również na KUCHNI (autoPrintKitchen=true)
   ✅ Drukowanie KUCHNIA zakończone
   ```
6. Zweryfikuj fizyczne wydruki (2 drukarki)

**Oczekiwany rezultat:**
- ✅ Wydruk na drukarce standardowej
- ✅ Wydruk na drukarce kuchennej (po 2s opóźnieniu)

---

### Test 4: Wszystkie Wyłączone

**Kroki:**
1. Wyłącz wszystkie 3 opcje
2. Wyślij nowe zamówienie
3. Zaakceptuj zamówienie
4. Sprawdź logi:
   ```
   ⏭️ AUTO-PRINT: Wyłączone - pomijam drukowanie
   ⏭️ Automatyczne drukowanie po zaakceptowaniu WYŁĄCZONE - pomijam
   ```

**Oczekiwany rezultat:**
- ✅ Brak wydruku
- ✅ W logach widać że ustawienia są wyłączone

---

## 📁 Pliki do Edycji

| Plik | Zmiany | Priorytet |
|------|--------|-----------|
| `SocketStaffEventsHandler.kt` | Dodać `AppPreferencesManager` DI + zmienić logikę AUTO_PRINT | 🔴 KRYTYCZNY |
| `PrinterService.kt` | Dodać szczegółowe logi w `printAfterOrderAccepted` | 🟡 WYSOKI |
| `OrdersViewModel.kt` | Dodać try-catch w wywołaniu `printAfterOrderAccepted` | 🟡 WYSOKI |

---

## ✅ Checklist Implementacji

- [ ] **Krok 1:** Dodać `AppPreferencesManager` do `SocketStaffEventsHandler` (DI)
- [ ] **Krok 2:** Zmienić `AppPrefs.getAutoPrintEnabled()` na `appPreferencesManager.getAutoPrintEnabled()`
- [ ] **Krok 3:** Dodać logi w `handleNewOrProcessingOrder`
- [ ] **Krok 4:** Dodać logi w `printAfterOrderAccepted`
- [ ] **Krok 5:** Dodać try-catch w `OrdersViewModel`
- [ ] **Krok 6:** Przeprowadzić Test 1 (AUTO_PRINT)
- [ ] **Krok 7:** Przeprowadzić Test 2 (AUTO_PRINT_ACCEPTED)
- [ ] **Krok 8:** Przeprowadzić Test 3 (AUTO_PRINT_KITCHEN)
- [ ] **Krok 9:** Przeprowadzić Test 4 (Wszystkie wyłączone)
- [ ] **Krok 10:** Weryfikacja produkcyjna

---

## 🐛 Możliwe Problemy i Rozwiązania

### Problem: "Brak skonfigurowanej drukarki"

**Przyczyna:** Funkcja `resolveTargetsFor()` nie znajduje drukarek

**Rozwiązanie:**
1. Sprawdź w logach: `resolveTargetsFor(RECEIPT)` → wynik
2. Zweryfikuj czy w bazie danych są drukarki z `enabled=true`
3. Sprawdź czy `printerType` jest poprawnie ustawiony (STANDARD/KITCHEN)

---

### Problem: "Drukowanie się nie rozpoczyna"

**Przyczyna:** Mutex jest zablokowany lub timeout

**Rozwiązanie:**
1. Sprawdź logi: czy mutex.lock() się udaje
2. Zwiększ timeout w `printMutex.withLock`
3. Sprawdź czy poprzednie drukowanie się zakończyło

---

### Problem: "Drukuje 2x na tej samej drukarce"

**Przyczyna:** Drukarka ma źle ustawiony `printerType`

**Rozwiązanie:**
1. Sprawdź w bazie: `SELECT * FROM printers`
2. Upewnij się że jedna ma `printerType='STANDARD'` a druga `KITCHEN`
3. Sprawdź w logach: jakie drukarki zwraca `resolveTargetsFor()`

---

## 📝 Notatki Developerskie

- Wszystkie funkcje drukowania są `suspend` - **zawsze wywołuj w coroutine**
- `printMutex` zapobiega równoczesnemu drukowaniu - **nie usuwaj**
- Opóźnienie 2s między drukarkami jest **krytyczne dla BT dual-mode**
- `useDeliveryInterval=true` dla nowych zamówień, `false` po zaakceptowaniu

---

**Koniec dokumentacji**

