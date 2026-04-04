# 🧪 ETAP 6: MANUAL TESTING GUIDE

## Przygotowanie do Testów

### Wymagania
- ✅ Android Studio z emulator'em LUB urządzenie fizyczne (API 26+)
- ✅ Zainstalowana aplikacja debug APK
- ✅ Dwie druki bluetooth (jeśli testujemy drukowanie wielopiętrowe)
- ✅ Logcat dostępny dla monitoringu

### Instalacja APK
```powershell
# Build debug APK
.\gradlew.bat assembleDebug

# Zainstaluj APK
adb install app\build\outputs\apk\debug\app-debug.apk

# Lub zainstal na wszystkich podłączonych urządzeniach
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 🧪 TEST SUITES

### TEST 1: Navigation & UI
**Celu**: Sprawdzić czy nowy ekran drukarek jest dostępny

**Kroki**:
1. Otwórz aplikację
2. Przejdź do Settings → "Zarządzaj drukarkami"
3. Sprawdź czy pojawił się nowy ekran

**Expected**:
- ✅ Nowy ekran się otwiera
- ✅ Lista drukarek jest pusta (przy pierwszym uruchomieniu)
- ✅ Przycisk "Dodaj drukarkę" jest widoczny

**Logi**:
```
PrintersViewModel: Pobieranie listy drukarek...
PrintersViewModel: Załadowano 0 drukarek
```

---

### TEST 2: Add Printer (CRUD - CREATE)
**Celu**: Dodaj nową drukarkę

**Kroki**:
1. W ekranie drukarek kliknij "Dodaj drukarkę"
2. Wypełnij formularz:
   - Nazwa: "Kitchen Printer"
   - MAC: "AB:0D:6F:E2:85:D7" (lub Twoja drukarka)
   - Profil: "KITCHEN"
   - Encoding: "Cp852"
   - Codepage: "13"
3. Kliknij "Zapisz"

**Expected**:
- ✅ Drukarka pojawia się na liście
- ✅ Order = 1

**Logi**:
```
PrintersViewModel: Dodano drukarkę: Kitchen Printer
PrinterPreferences: Dodano drukarkę 'Kitchen Printer' (order=1)
```

---

### TEST 3: Add Second Printer
**Celu**: Dodaj drugą drukarkę dla sekwencyjnego drukowania

**Kroki**:
1. Kliknij "Dodaj drukarkę" ponownie
2. Wypełnij:
   - Nazwa: "Standard Printer"
   - MAC: "00:11:22:33:44:55" (lub Twoja drukarka)
   - Profil: "STANDARD"
   - Encoding: "UTF-8"
3. Zapisz

**Expected**:
- ✅ Dwie druki na liście
- ✅ Order: Kitchen=1, Standard=2
- ✅ Sortowanie prawidłowe

---

### TEST 4: Edit Printer (CRUD - UPDATE)
**Celu**: Edytuj drukarkę

**Kroki**:
1. Long-click na "Kitchen Printer"
2. Kliknij "Edytuj"
3. Zmień nazwę na "Kitchen Printer v2"
4. Zapisz

**Expected**:
- ✅ Nazwa zostaje zaktualizowana
- ✅ Order się nie zmienia
- ✅ MAC adres pozostaje

**Logi**:
```
PrintersViewModel: Zaktualizowano drukarkę: Kitchen Printer v2
```

---

### TEST 5: Delete Printer (CRUD - DELETE)
**Celu**: Usuń drukarkę

**Kroki**:
1. Long-click na drukarce
2. Kliknij "Usuń"
3. Potwierdź usunięcie

**Expected**:
- ✅ Drukarka zostaje usunięta
- ✅ List jest teraz pusty lub zawiera jedną drukarkę

**Logi**:
```
PrintersViewModel: Usunięto drukarkę
PrinterPreferences: Zapisano 1 drukarek
```

---

### TEST 6: Enable/Disable Printer
**Celu**: Toggle drukowania na drukarce

**Kroki**:
1. Dodaj dwie druki
2. Kliknij "Toggle" na "Kitchen Printer"
3. Zaobserwuj zmianę stanu

**Expected**:
- ✅ Przycisk zmienia stan (enabled/disabled)
- ✅ Wyłączona drukarka ma wyszarzony wygląd
- ✅ Stan jest zapisywany

**Logi**:
```
PrintersViewModel: Włączono drukarkę: Kitchen Printer
PrintersViewModel: Wyłączono drukarkę: Kitchen Printer
```

---

### TEST 7: Sequential Printing - Single Printer
**Celu**: Drukuj zamówienie na jednej drukarce

**Kroki**:
1. Zaakceptuj zamówienie
2. Sprawdź Logcat
3. Obserwuj wydruk

**Expected**:
- ✅ Drukarka się łączy
- ✅ Zamówienie jest drukowane
- ✅ Brak błędów połączenia
- ✅ Drukarka się rozłącza

**Logi**:
```
PrinterService: 📍 PrinterSTEP: STEP 1 - Weryfikacja konfiguracji dla STANDARD
PrinterService: ✅ PrinterSTEP: STEP 2 OK - Połączenie uzyskane
PrinterService: 📍 PrinterSTEP: STEP 4 - Drukowanie...
PrinterService: ✅ PrinterSTEP: [SUCCESS] Drukowanie zakończone pomyślnie
```

---

### TEST 8: Sequential Printing - TWO Printers
**Celu**: Drukuj sekwencyjnie na dwóch drukarkach

**Kroki**:
1. Upewnij się że obie druki są enabled
2. Zaakceptuj zamówienie
3. Obserwuj Logcat

**Expected**:
- ✅ Najpierw drukuje na STANDARD
- ✅ Czeka (delay) między drukutkami
- ✅ Potem drukuje na KITCHEN
- ✅ Obie druki wydrukują

**Logi**:
```
PrinterService: 🎯 PrinterSTEP: [printAfterOrderAccepted] START
PrinterService: 🧾 PrinterSTEP: Uruchamiam drukowanie na STANDARD
PrinterService: ✅ PrinterSTEP: [SUCCESS] Drukowanie zakończone pomyślnie na STANDARD
PrinterService: ⏳ PrinterSTEP: Czekam 2000ms przed przełączeniem...
PrinterService: 🍳 PrinterSTEP: Uruchamiam drukowanie na KITCHEN
PrinterService: ✅ PrinterSTEP: [SUCCESS] Drukowanie zakończone pomyślnie na KITCHEN
```

---

### TEST 9: Disabled Printer Skipped
**Celu**: Wyłączona drukarka nie powinna się drukować

**Kroki**:
1. Wyłącz "KITCHEN" drukarkę
2. Zaakceptuj zamówienie
3. Obserwuj Logcat

**Expected**:
- ✅ STANDARD druje
- ✅ KITCHEN nie ma w logach
- ✅ Brak błędów
- ✅ Proces trwa krótko (brak opóźnienia)

**Logi**:
```
PrinterService: 📍 PrinterSTEP: Konfiguracja - KITCHEN(enabled=false...)
PrinterService: 🧾 PrinterSTEP: Uruchamiam drukowanie na STANDARD
# Brak KITCHEN
```

---

### TEST 10: Stress Test - Rapid Orders
**Celu**: Testuj kolejkowanie drukowania

**Kroki**:
1. Zaakceptuj 3 zamówienia szybko pod rząd
2. Obserwuj czy druki się nie zacinają
3. Sprawdź logcat

**Expected**:
- ✅ Druki drukują się po kolei
- ✅ Brak zacinania się
- ✅ Każde zamówienie drukuje się kompletnie
- ✅ Brak duplikatów

---

### TEST 11: Connection Error Handling
**Celu**: Testuj obsługę błędu połączenia

**Kroki**:
1. Wyłącz drukarkę fizycznie (lub zmień MAC)
2. Zaakceptuj zamówienie
3. Obserwuj Logcat

**Expected**:
- ✅ Błąd jest wyłapany
- ✅ Komunikat o błędzie w logach
- ✅ Aplikacja nie pada
- ✅ Drukowanie na drugiej drukarce działa

**Logi**:
```
PrinterService: ❌ PrinterSTEP: BŁĄD podczas drukowania
PrinterService: 🔓 PrinterSTEP: [EXIT] target=KITCHEN
```

---

## 📊 Logcat Monitorowanie

```bash
# Filtruj logi PrinterService
adb logcat com.itsorderchat:V | grep PrinterService

# Lub użyj Android Studio Logcat i filtruj:
# Package: com.itsorderchat
# Log Level: Verbose
# Search: "PrinterService" lub "PrintersViewModel"
```

---

## ✅ Checklist Testów

- [ ] TEST 1: Navigation OK
- [ ] TEST 2: Add Printer OK
- [ ] TEST 3: Add Second Printer OK
- [ ] TEST 4: Edit Printer OK
- [ ] TEST 5: Delete Printer OK
- [ ] TEST 6: Enable/Disable OK
- [ ] TEST 7: Single Printer Printing OK
- [ ] TEST 8: Two Printers Printing OK
- [ ] TEST 9: Disabled Printer Skipped OK
- [ ] TEST 10: Stress Test OK
- [ ] TEST 11: Error Handling OK

---

## 🐛 Jeśli Coś Się Nie Zgadza

### Problem: "Printer list is empty after adding"
**Rozwiązanie**:
- Sprawdź SharedPreferences: `adb shell`
- `dumpsys meminfo com.itsorderchat | grep printer`
- Czyszczenie cache: Ustawienia → Aplikacja → Clear Cache

### Problem: "Printer won't print"
**Rozwiązanie**:
- Sprawdź Logcat dla błędów BT
- Upewnij się że drukarka jest włączona i w zasięgu
- Resetuj drukarkę

### Problem: "Two printers print at same time"
**Rozwiązanie**:
- Sprawdź czy delay (2000ms) jest ustawiony
- Sprawdź czy `printOrderOnAllEnabledPrinters` jest sekwencyjny
- Dodaj więcej opóźnienia w `PrinterService.kt` linii XXX

---

## 📝 Raportowanie

Po ukończeniu testów, stwórz raport:
```
Test Results Summary:
- Total Tests: 11
- Passed: X/11
- Failed: Y/11
- Critical Issues: Z

Critical Issues Found:
1. ...
2. ...

Minor Issues Found:
1. ...

Recommendations:
1. ...
```

---

## 🎯 Koniec ETAP 6

Po ukończeniu wszystkich testów i braku krytycznych błędów, jesteśmy gotowi do **ETAP 7: Production Release** 🚀

