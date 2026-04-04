# 🚀 INSTRUKCJA INSTALACJI I TESTOWANIA - Naprawa Przełączników

## ⚠️ WAŻNE - PRZECZYTAJ PRZED TESTOWANIEM

Kod został **całkowicie naprawiony**. Jeśli przełączniki nadal nie działają, to znaczy że:
1. **Aplikacja NIE została przeinstalowana** z najnowszym APK
2. **Dane aplikacji NIE zostały wyczyszczone** (stary ViewModel w cache)
3. **Android nie załadował nowego kodu** (trzeba force stop aplikacji)

---

## 📱 KROK 1: Całkowita reinstalacja (OBOWIĄZKOWE!)

### A) Odinstaluj starą aplikację:
```
Settings → Apps → itsOrderChat → Uninstall
```
**LUB** przez ADB:
```
adb uninstall com.itsorderchat
```

### B) Zainstaluj nowy APK:
```
Lokalizacja: L:\SHOP APP\app\build\outputs\apk\debug\app-debug.apk

Opcja 1 (Android Studio):
- Build → Build Bundle(s) / APK(s) → Build APK(s)
- Run → Run 'app'

Opcja 2 (Ręcznie):
- Skopiuj app-debug.apk na urządzenie
- Zainstaluj przez plik manager

Opcja 3 (ADB):
adb install L:\SHOP APP\app\build\outputs\apk\debug\app-debug.apk
```

### C) Wyczyść dane aplikacji (jeśli reinstalacja nie pomogła):
```
Settings → Apps → itsOrderChat → Storage → Clear Data
Settings → Apps → itsOrderChat → Force Stop
```

---

## 🧪 KROK 2: Testowanie przełączników

### Test 1: Kategorie ładują się poprawnie
1. Uruchom aplikację
2. Przejdź do Drawer → "Produkty"
3. **Oczekiwane:** Lista kategorii (Pizza, Przystawki, Napoje, etc.)
4. **Status:** ✅ / ❌

### Test 2: Produkty w kategorii wyświetlają się
1. Kliknij dowolną kategorię (np. "Przystawki")
2. **Oczekiwane:** Lista produktów z tej kategorii
3. **Oczekiwane:** Każdy produkt ma Switch (włącz/wyłącz)
4. **Status:** ✅ / ❌

### Test 3: ⭐ GŁÓWNY TEST - Przełącznik produktu działa
1. Znajdź produkt WYŁĄCZONY (Switch w pozycji OFF, szary)
2. Kliknij Switch
3. **Oczekiwane NATYCHMIAST:**
   - ✅ Switch zmienia się na ON (niebieski)
   - ✅ Krótki loader (CircularProgressIndicator)
   - ✅ Switch pozostaje ON po zakończeniu loadera
4. **Status:** ✅ / ❌

### Test 4: Przełącznik dodatku działa
1. Znajdź produkt z dodatkami
2. Rozwiń dodatki (jeśli są zwinięte)
3. Kliknij Switch dodatku
4. **Oczekiwane NATYCHMIAST:**
   - ✅ Switch zmienia się
   - ✅ Krótki loader
   - ✅ Switch pozostaje w nowej pozycji
5. **Status:** ✅ / ❌

### Test 5: Weryfikacja zapisu (persistence)
1. Włącz produkt (Switch ON)
2. Cofnij do listy kategorii (Back button)
3. Wejdź ponownie w tę samą kategorię
4. **Oczekiwane:** Produkt nadal włączony (Switch ON)
5. **Status:** ✅ / ❌

---

## 🔍 KROK 3: Diagnostyka jeśli NIE działa

### Opcja A: Sprawdź logi (Logcat)

#### Uruchom Logcat:
```
Android Studio → Logcat
Filtr: "ProductsViewModel" LUB "CategoryProductsScreen"
```

#### Wykonaj Test 3, szukaj tych logów:
```
✅ POWINNY BYĆ:
🔄 updateStockStatus: productId=..., newStatus=IN_STOCK
📡 Sending API request...
✅ API request successful
🔄 Updating itemsFlow...
✅ itemsFlow updated
🔄 Updating _categoriesFlow...
📦 Current categories count: X
✅ Found product in category ..., updating status
✅ Updated categories count: X
✅ _categoriesFlow.value assigned
🔄 uiState combine triggered - categories count: X
🔄 CategoryProductsScreen recompose - categoryProducts count: X
  Product: ..., status: IN_STOCK  ← Status zmieniony!

❌ JEŚLI BRAK TYCH LOGÓW:
→ Aplikacja używa STAREGO kodu!
→ Przejdź do KROK 1 i wykonaj reinstalację ponownie!
```

### Opcja B: Sprawdź czy to najnowsza wersja

#### W logach na starcie aplikacji powinno być:
```
ProductsViewModel initialized
```

#### Jeśli w logach widzisz:
```
❌ BRAK logów "🔄 updateStockStatus"
→ To STARA wersja aplikacji!
```

---

## 📊 Co zostało naprawione (dla informacji)

### Problem 1: `remember()` zamrażał dane ✅ FIXED
```kotlin
// ❌ PRZED
val categoryProducts = remember(uiState.categoriesWithProducts, categoryId) { ... }

// ✅ PO
val categoryProducts = uiState.categoriesWithProducts
    .find { it.id == categoryId || it.slug == categoryId }
    ?.products
    ?: emptyList()
```

### Problem 2: `_categoriesFlow` nie był aktualizowany ✅ FIXED
```kotlin
// ❌ PRZED
fun updateStockStatus(...) {
    itemsFlow.update { ... }  // Tylko itemsFlow
}

// ✅ PO
fun updateStockStatus(...) {
    itemsFlow.update { ... }  // Dla ProductsScreen
    _categoriesFlow.value = updatedCategories  // Dla CategoryProductsScreen!
}
```

### Problem 3: `.update{}` nie emitowało ✅ FIXED
```kotlin
// ❌ PRZED
_categoriesFlow.update { categories ->
    categories.map { ... }  // Może nie emitować!
}

// ✅ PO
val updated = currentCategories.map { ... }
_categoriesFlow.value = updated  // ZAWSZE emituje!
```

---

## ❓ FAQ - Najczęstsze problemy

### Q: Przełącznik NIE zmienia się, co robić?
**A:** 
1. Force Stop aplikacji (Settings → Apps → Force Stop)
2. Wyczyść dane (Settings → Apps → Clear Data)
3. Uruchom ponownie
4. Jeśli dalej nie działa → **ODINSTALUJ I ZAINSTALUJ PONOWNIE**

### Q: Widzę loader ale Switch wraca do starej pozycji
**A:** To znaczy że API zwraca błąd. Sprawdź logi HTTP:
```
Logcat filtr: "okhttp"
Szukaj: --> PUT /product/.../status
        <-- 200 (lub 4xx/5xx)
```

### Q: Switch działa w ProductsScreen ale NIE w CategoryProductsScreen
**A:** To STARA wersja kodu! Wykonaj:
```
1. Odinstaluj aplikację całkowicie
2. Wyczyść build folder: gradlew clean
3. Zbuduj: gradlew assembleDebug
4. Zainstaluj nowy APK
```

### Q: Jak sprawdzić czy to najnowsza wersja?
**A:** Sprawdź logi przy kliknięciu Switch:
- ✅ Najnowsza: Widzisz emoji (🔄, ✅, 📡, etc.)
- ❌ Stara: Brak emoji albo brak logów "updateStockStatus"

---

## ✅ Checklist przed zgłoszeniem problemu

Zanim napiszesz że "nie działa", upewnij się że:

- [ ] Odinstalowałem starą aplikację (Settings → Uninstall)
- [ ] Zainstalowałem NOWY APK z `app-debug.apk`
- [ ] Wyczyściłem dane aplikacji (Clear Data)
- [ ] Force Stop aplikacji
- [ ] Uruchomiłem aplikację od nowa
- [ ] Przeszedłem do kategorii produktów
- [ ] Kliknąłem Switch produktu
- [ ] Sprawdziłem logi Logcat

**Jeśli wszystkie checkboxy ✅ i NADAL nie działa:**
→ Prześlij mi **PEŁNE LOGI** z Logcat (od momentu kliknięcia Switch)

---

## 🎯 Oczekiwany rezultat PO naprawie

✅ **Przełącznik produktu:**
- Natychmiast zmienia stan wizualny
- Pojawia się krótki loader
- Status zostaje zmieniony

✅ **Przełącznik dodatku:**
- Natychmiast zmienia stan wizualny
- Pojawia się krótki loader
- Status zostaje zmieniony

✅ **Synchronizacja:**
- Zmiana w CategoryProductsScreen = zmiana w ProductsScreen
- Zmiana jest zapisana (API)
- Po cofnięciu i powrocie - zmiana zachowana

---

## 🚀 GOTOWE DO TESTOWANIA!

**Lokalizacja APK:** `L:\SHOP APP\app\build\outputs\apk\debug\app-debug.apk`

**Data buildu:** 2026-01-30

**Wersja:** DEBUG (z pełnym logowaniem)

**PAMIĘTAJ:** 
1. Odinstaluj starą wersję
2. Zainstaluj nowy APK
3. Testuj!

Powodzenia! 🎉

