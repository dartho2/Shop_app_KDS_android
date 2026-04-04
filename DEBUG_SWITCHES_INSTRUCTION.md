# 🔍 INSTRUKCJA TESTOWANIA - Debug Przełączników Produktów

## ⚠️ NAJNOWSZA POPRAWKA (2026-01-30)

**Problem:** `_categoriesFlow.update { }` nie emitował nowej wartości pomimo zwracania nowej listy.

**Rozwiązanie:** Zmieniono na bezpośrednie przypisanie `_categoriesFlow.value = updatedCategories`

**Co to zmienia:** 
- `MutableStateFlow` ZAWSZE emituje gdy `.value` jest przypisywane (nawet jeśli struktura jest taka sama)
- `.update { }` może nie emitować jeśli Kotlin optymalizuje referencje

---

## 📱 Instalacja aplikacji

1. **Zainstaluj świeżo zbudowaną aplikację:**
   ```
   Znajdź plik: L:\SHOP APP\app\build\outputs\apk\debug\app-debug.apk
   Zainstaluj na urządzeniu (przez Android Studio lub ręcznie)
   ```

2. **Wyczyść dane aplikacji** (WAŻNE!):
   ```
   Settings → Apps → itsOrderChat → Storage → Clear Data
   ```
   - To wymusi przeładowanie ViewModelu i kategorii

---

## 🧪 Scenariusz testowy z logami

### Test 1: Sprawdź czy dane są ładowane
1. Uruchom aplikację
2. Otwórz Logcat i filtruj po: `ProductsViewModel`
3. Przejdź do ekranu Produkty (drawer → "Produkty")

**Oczekiwane logi:**
```
🌐 Loading categories from API...
✅ Categories loaded: X categories
🔄 uiState combine triggered - categories count: X
📦 categoriesWithProducts assigned, count: X
```

---

### Test 2: Sprawdź co się dzieje przy kliknięciu kategorii
1. W Logcat filtruj po: `CategoryProductsScreen`
2. Kliknij dowolną kategorię (np. "Przystawki")

**Oczekiwane logi:**
```
🔄 CategoryProductsScreen recompose - categoryProducts count: X
  Product: Ebi Tempura 6 szt.., status: OUT_OF_STOCK
  Product: ..., status: ...
```

---

### Test 3: KLUCZOWY - Sprawdź co się dzieje przy kliknięciu Switch
1. W Logcat filtruj po: `updateStockStatus` LUB wyczyść filtr
2. Kliknij Switch produktu (np. "Ebi Tempura 6 szt..")

**Oczekiwane logi (kolejność!):**
```
🔄 updateStockStatus: productId=69261af5d716f1624a59fe0f, newStatus=IN_STOCK
📡 Sending API request...
✅ API request successful
🔄 Updating itemsFlow...
✅ itemsFlow updated
🔄 Updating _categoriesFlow...
✅ Found product in category Przystawki, updating status
✅ _categoriesFlow updated, categories count: X
🔄 uiState combine triggered - categories count: X  ← WAŻNE!
📦 categoriesWithProducts assigned, count: X
🔄 CategoryProductsScreen recompose - categoryProducts count: X  ← WAŻNE!
  Product: Ebi Tempura 6 szt.., status: IN_STOCK  ← WAŻNE! Status zmieniony!
```

---

## 🔍 Analiza wyników

### ✅ JEŚLI WIDZISZ wszystkie logi powyżej:
- Kod działa poprawnie
- Problem może być w UI (Switch component)
- Sprawdź czy `product.stockStatus` jest używany poprawnie w Switch

### ❌ JEŚLI NIE WIDZISZ `🔄 uiState combine triggered` PO aktualizacji:
- `_categoriesFlow.update` NIE wywołuje combine
- Problem z Flow - może trzeba użyć `MutableSharedFlow` zamiast `MutableStateFlow`

### ❌ JEŚLI NIE WIDZISZ `🔄 CategoryProductsScreen recompose` PO aktualizacji:
- UI nie reaguje na zmiany `uiState`
- Problem z `collectAsStateWithLifecycle()` lub rekomponowaniem

### ❌ JEŚLI Status się NIE ZMIENIA w logu produktu:
- Aktualizacja `_categoriesFlow` nie działa
- Problem z `copy()` lub mapowaniem

---

## 🐛 Możliwe problemy i rozwiązania

### Problem 1: Brak logów `updateStockStatus`
**Przyczyna:** Aplikacja nie została przebudowana/przeinstalowana  
**Rozwiązanie:** Wyczyść dane aplikacji i zainstaluj ponownie APK

### Problem 2: Logi pokazują aktualizację ale Switch się nie zmienia
**Przyczyna:** Problem w komponencie Switch - może używa `product.status` zamiast `product.stockStatus`  
**Rozwiązanie:** Sprawdź kod `ProductListItem` w `CategoryProductsScreen.kt`

### Problem 3: `_categoriesFlow.update` nie wywołuje `combine`
**Przyczyna:** `MutableStateFlow` nie emituje jeśli referencja listy jest ta sama  
**Rozwiązanie:** Upewnij się że `categories.map { ... }` tworzy NOWĄ listę

---

## 📋 Checklist debugowania

- [ ] Aplikacja przebudowana (`gradlew clean assembleDebug`)
- [ ] APK zainstalowany na urządzeniu
- [ ] Dane aplikacji wyczyszczone
- [ ] Logcat otwarty i filtrowany
- [ ] Test 1: Kategorie ładują się ✅
- [ ] Test 2: Produkty wyświetlają się ✅
- [ ] Test 3: Logi przy kliknięciu Switch
- [ ] Analiza: Czy `uiState combine` jest wywoływany?
- [ ] Analiza: Czy `CategoryProductsScreen` się rekomponuje?
- [ ] Analiza: Czy status produktu się zmienia w logu?

---

## 📊 Wyślij mi logi

Po wykonaniu Test 3, skopiuj **WSZYSTKIE logi** od momentu kliknięcia Switch i prześlij mi.

Szukam szczególnie tych linii:
```
🔄 updateStockStatus
✅ _categoriesFlow updated
🔄 uiState combine triggered  ← CZY TO SIĘ POJAWIA?
🔄 CategoryProductsScreen recompose  ← CZY TO SIĘ POJAWIA?
  Product: ..., status: ...  ← JAKI STATUS?
```

To pozwoli mi dokładnie zdiagnozować gdzie jest problem!

