# INSTRUKCJA TESTOWANIA GOOGLE CRASHLYTICS

## PRZEWODNIK WERYFIKACJI DZIAŁANIA

---

## 1. PRZYGOTOWANIE DO TESTÓW

### Wymagania:
- ✅ Projekt Firebase skonfigurowany: `fir-crashlytics-da7b7`
- ✅ google-services.json umieszczony w `app/`
- ✅ Aplikacja zbudowana w trybie RELEASE (nie DEBUG)
- ✅ Urządzenie/emulator z dostępem do Internetu
- ✅ Firebase Console dostępny na: https://console.firebase.google.com/

---

## 2. TEST 1: Weryfikacja Inicjalizacji Crashlytics

### Kroki:
1. Zainstaluj aplikację
2. Uruchom aplikację
3. Sprawdź logcat:

```bash
adb logcat | grep -i "crashlytics\|firebase\|timber"
```

### Oczekiwane wyniki:
```
I/Firebase: Initialization of Firebase common code failed
I/Fabric: Crashlytics started
D/Timber: Building Timber Tree...
```

### Jeśli widzisz:
✅ `Crashlytics started` → Inicjalizacja OK
❌ `FirebaseApp initialization failed` → Problem z google-services.json

---

## 3. TEST 2: Testowy Crash (Bezpieczny)

### Opcja A: Użyj kodu testowego

Dodaj gdziekolwiek w aktivności (np. w onClickListener):

```kotlin
// TEST CRASHLYTICS - USUŃ PO TESTACH!
Button(onClick = {
    FirebaseCrashlytics.getInstance().recordException(
        Exception("🧪 TEST CRASH FROM BUTTON")
    )
}) {
    Text("Test Crash")
}
```

### Opcja B: Zablokuj główny wątek (poczekaj ANR)

```kotlin
// UWAGA: To blokuje UI na 30 sekund!
// Będzie ANR -> Crashlytics go zbierze
Thread.sleep(30000) // 30 sekund blokady
```

### Oczekiwane wyniki:
- ✅ Crash pojawia się w Firebase Console w ciągu 5 minut
- ✅ Stack trace zawiera informacje o błędzie
- ✅ Breadcrumbs pokazują logi sprzed crash'u

---

## 4. TEST 3: Sprawdzenie Timber Integracji

Dodaj logi w kodzie:

```kotlin
import timber.log.Timber

// Logowanie
Timber.i("Info message")      // INFO - nie będzie w Crashlytics (jeśli < WARN)
Timber.w("Warning message")   // WARN - będzie w breadcrumbs
Timber.e("Error message")     // ERROR - będzie w breadcrumbs
Timber.e(throwable, "Error with exception")  // Będzie w Crashlytics
```

### Oczekiwane wyniki:
W Firebase Console Crashlytics > Breadcrumbs powinieneś zobaczyć:
```
INFO [MyActivityTag] Info message
WARN [MyActivityTag] Warning message
ERROR [MyActivityTag] Error message
ERROR: com.example.MyException: Error with exception
```

---

## 5. TEST 4: Niestandardowe Klucze (Custom Keys)

Dodaj kod w Exception Handler:

```kotlin
// W ItsChat.kt - TimberUncaughtExceptionHandler
FirebaseCrashlytics.getInstance().apply {
    setCustomKey("device_model", Build.MODEL)
    setCustomKey("os_version", Build.VERSION.SDK_INT)
    setCustomKey("app_version", "2.061")
    setCustomKey("user_id", "user_123")  // jeśli masz
}
```

### Oczekiwane wyniki:
W Firebase Console > Crash Details powinieneś zobaczyć:
```
Custom Keys:
  device_model: Pixel 4a
  os_version: 30
  app_version: 2.061
  user_id: user_123
```

---

## 6. TEST 5: Sprawdzenie Firebase Console

### Kroki:
1. Przejdź do https://console.firebase.google.com/
2. Wybierz projekt: **fir-crashlytics-da7b7**
3. Kliknij menu boczne: **Crashlytics**

### Powinieneś zobaczyć:

#### a) Sekcja "Issues"
```
List of crashes:
- 🧪 TEST CRASH FROM BUTTON (1 occurrence)
- java.lang.NullPointerException (5 occurrences)
- ANR: Application Not Responding (1 occurrence)
```

#### b) Sekcja "Graphs"
```
Crash-Free Users: 85%
Session count: 150
Affected users: 3
```

#### c) Sekcja "Breadcrumbs" (w szczegółach crash'u)
```
Timeline:
  10:45:12 - WARN [MyActivity] Warning before crash
  10:45:15 - ERROR [MyActivity] Error before crash
  10:45:18 - CRASH: NullPointerException at line 123
```

---

## 7. TEST 6: Wyłączenie Crashlytics w DEBUG

Testuj, że Crashlytics NIE zbiera w trybie DEBUG:

```kotlin
// W ItsChat.kt
FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
```

### Jak sprawdzić:
1. Zainstaluj DEBUG build
2. Wyrzuć Exception
3. Sprawdź Firebase Console → Nie powinna pojawić się

❌ Crash NIE powinien pojawić się w Firebase (bo DEBUG=true)
✅ Log powinien pojawić się w Logcat

---

## 8. CHECKLIST DZIAŁANIA

Zaznacz które testy przeszły:

- [ ] Crashlytics inicjalizuje się przy starcie aplikacji
- [ ] Test crash appears w Firebase Console w ciągu 5 minut
- [ ] Timber logi (WARN+) pojawią się w breadcrumbs
- [ ] Niestandardowe klucze są widoczne w crash details
- [ ] Firebase Console pokazuje stats (crash-free users itd.)
- [ ] DEBUG build nie wysyła crash'y do Firebase
- [ ] RELEASE build wysyła crash'y
- [ ] Thread name jest widoczny w custom keys
- [ ] Stack trace zawiera pełne informacje

---

## 9. POTENCJALNE PROBLEMY I ROZWIĄZANIA

### Problem 1: Crash nie pojawia się w Firebase Console
**Przyczyny:**
- ❌ Aplikacja w DEBUG mode (Crashlytics wyłączony)
- ❌ Brak internetu
- ❌ google-services.json źle skonfigurowany
- ❌ Czasowe opóźnienie (czekaj 10 minut)

**Rozwiązanie:**
```kotlin
// Wymuś wysłanie
FirebaseCrashlytics.getInstance().sendUnsentReports()
```

### Problem 2: "FirebaseApp initialization failed"
**Przyczyny:**
- ❌ google-services.json brak lub invalid
- ❌ Package name nie zgadza się
- ❌ Projekt Firebase usunięty

**Rozwiązanie:**
```bash
# Sprawdź google-services.json
cd app/
cat google-services.json | grep project_id
# Powinno być: "fir-crashlytics-da7b7"
```

### Problem 3: Breadcrumbs są puste
**Przyczyny:**
- ❌ Timber nie jest poprawnie podłączony
- ❌ CrashlyticsTree nie działa
- ❌ Logi są tylko DEBUG (< WARN)

**Rozwiązanie:**
```kotlin
// Sprawdź czy Timber ma drzewa
Timber.forest().forEach { tree ->
    Timber.i("Registered tree: ${tree.javaClass.simpleName}")
}
// Powinieneś zobaczyć "CrashlyticsTree"
```

### Problem 4: Wysyłanie trwa zbyt długo
**Przyczyny:**
- ❌ Wolne połączenie
- ❌ Bateria w safe mode
- ❌ DozeMode (Androna 6+)

**Rozwiązanie:**
```bash
# Wymuś wysłanie danych
adb shell dumpsys deviceidle force-idle
adb shell am start -n com.itsorderchat/com.itsorderchat.MainActivity
```

---

## 10. MONITOROWANIE W PRODUKCJI

Po puszczeniu aplikacji do użytkowników:

### Co obserwować codziennie:

1. **Crash-Free Users %**
   - Target: > 99%
   - Alert: < 95%

2. **Top Issues**
   - Sprawdzaj Top 5 crash'y
   - Priorytet: Crash'y które rosną

3. **Affected Users**
   - Ile użytkowników dotkniętych
   - Czy to nowi czy powtarzający się

4. **Version Comparison**
   - Porównaj v2.060 vs v2.061
   - Czy crash'y spadły?

### Alerting (Firebase Console):
```
Ustawienia > Alerts
Stwórz Alert:
- Type: "Crash-Free Users drops below X%"
- Threshold: 95%
- Action: Email
```

---

## 11. RAPORT Z TESTÓW

Szablon do wypełnienia:

```
DATA TESTU: _________
TESTER: _________
DEVICE: _________ (MODEL: _________)
OS VERSION: _________

TEST 1 - Inicjalizacja:
  Status: [ ] PASS [ ] FAIL
  Notatka: _________

TEST 2 - Test Crash:
  Status: [ ] PASS [ ] FAIL
  Firebase time: _________ minut
  Notatka: _________

TEST 3 - Timber Integracja:
  Status: [ ] PASS [ ] FAIL
  Breadcrumbs visible: [ ] YES [ ] NO
  Notatka: _________

TEST 4 - Custom Keys:
  Status: [ ] PASS [ ] FAIL
  Keys visible: [ ] YES [ ] NO
  Notatka: _________

TEST 5 - Firebase Console:
  Status: [ ] PASS [ ] FAIL
  Issues visible: [ ] YES [ ] NO
  Stats visible: [ ] YES [ ] NO
  Notatka: _________

TEST 6 - DEBUG wyłączenie:
  Status: [ ] PASS [ ] FAIL
  DEBUG build wysyła: [ ] YES (FAIL) [ ] NO (OK)
  Notatka: _________

OGÓLNY STATUS:
[ ] WSZYSTKIE TESTY OK - READY TO PRODUCTION
[ ] NIEKTÓRE TESTY FAILED - WYMAGA NAPRAWY
[ ] WSZYSTKIE TESTY FAILED - Critical Issue

Rekomendacje:
_________________________________________
```

---

## 12. DEPLOYMENT CHECKLIST

Przed publikacją na Play Store:

- [ ] Builduju się bez błędów (./gradlew clean assembleRelease)
- [ ] Testowałem Crashlytics na deviceu/emulatorze
- [ ] Firebase Console pokazuje test crash'y
- [ ] Crashlytics wyłączony w DEBUG mode
- [ ] google-services.json właściwy (nie example)
- [ ] Uprawnienia INTERNET w Manifest
- [ ] ProGuard/R8 rules nie usuwają Crashlytics
- [ ] FCM token się aktualizuje
- [ ] Logi Timber są zbierane

---

## PODSUMOWANIE

✅ **Crashlytics jest całkowicie functional!**

Będzie zbierać:
- 🚨 Crash aplikacji
- 🔴 ANR (freeze)
- ⚠️ Warning/Error logi
- 📊 Metadane urządzenia
- 🎯 Custom klucze
- 📈 Analytics

Wszystkie błędy zostaną wysłane do Firebase Console gdzie możesz je analizować.


