# 🧪 Test Crash & Auto-Restart - Dokumentacja

## 📋 Opis Funkcjonalności

Dodano przycisk testowy w Ustawieniach Głównych, który symuluje crash aplikacji i weryfikuje mechanizm automatycznego restartu.

## 🎯 Cel

Umożliwienie łatwego przetestowania czy aplikacja poprawnie restartuje się po nieoczekiwanym błędzie (crash).

## 📍 Lokalizacja

**Ścieżka w UI:**
```
Ustawienia → Ustawienia główne → (scroll w dół) → Test Crash & Auto-Restart
```

**Widoczność:**
- ✅ Widoczny TYLKO w trybie DEBUG (`BuildConfig.DEBUG == true`)
- ❌ Ukryty w wersji RELEASE (dla bezpieczeństwa)

## 🔧 Implementacja

### 1. Zmiana w `SettingsScreen.kt`

**Dodane importy:**
```kotlin
import androidx.compose.material.icons.filled.BugReport
```

**Dodany przycisk (linia ~377):**
```kotlin
// ✅ NOWY: Przycisk Test Crash (tylko w DEBUG)
if (com.itsorderchat.BuildConfig.DEBUG) {
    item {
        SettingsItem(
            icon = Icons.Default.BugReport,
            title = "Test Crash & Auto-Restart",
            subtitle = "Symuluje crash aplikacji i sprawdza auto-restart (tylko DEBUG)",
            onClick = {
                // Celowy crash po 1 sekundzie (żeby UI zdążyło się odświeżyć)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    timber.log.Timber.tag("TEST_CRASH").w("🧪 Symulowany crash - test auto-restart")
                    throw RuntimeException("🧪 TEST CRASH: Sprawdzenie mechanizmu auto-restart")
                }, 1000)
            },
            iconBackgroundColor = Color(0xFFFF5252).copy(alpha = 0.15f),
            iconTint = Color(0xFFFF5252)
        )
    }
}
```

### 2. Istniejący Mechanizm Auto-Restart

**Plik:** `ItsChat.kt` (Application class)

**Handler:** `AutoRestartExceptionHandler`
- Przechwytuje WSZYSTKIE nieobsłużone wyjątki w aplikacji
- Loguje błąd do Timber i Crashlytics
- Czeka 2 sekundy (żeby logi zostały zapisane)
- Planuje restart aplikacji za 1 sekundę przez AlarmManager
- Pozwala systemowi dokończyć crash

**Kod:**
```kotlin
private class AutoRestartExceptionHandler(
    private val context: Application,
    private val defaultHandler: UncaughtExceptionHandler?
) : UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            // 1) Log do Timber i Crashlytics
            Timber.e(e, "🔥 CRASH na wątku: ${t.name}")
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("thread", t.name)
                setCustomKey("crash_handled_at", System.currentTimeMillis().toString())
            }

            // 2) Czekaj 2 sekundy
            Thread.sleep(2000)

            // 3) Zaplanuj restart
            Timber.i("🔄 Planując restart aplikacji za 1 sekundę...")
            scheduleAppRestart()

        } catch (ex: Throwable) {
            Timber.e(ex, "❌ Błąd w AutoRestartExceptionHandler")
        } finally {
            // Pozwól systemowi dokończyć
            defaultHandler?.uncaughtException(t, e)
        }
    }
}
```

## 🚀 Jak Używać

### Krok 1: Uruchom aplikację w trybie DEBUG
```bash
./gradlew assembleDebug
# lub przez Android Studio: Run 'app'
```

### Krok 2: Przejdź do Ustawień
```
Hamburger Menu → Ustawienia → Ustawienia główne
```

### Krok 3: Scroll w dół do sekcji "Inne"
Zobaczysz przycisk z czerwoną ikoną błędu:
```
🐛 Test Crash & Auto-Restart
Symuluje crash aplikacji i sprawdza auto-restart (tylko DEBUG)
```

### Krok 4: Kliknij przycisk
- Aplikacja wyświetli toast/UI przez 1 sekundę
- Po 1 sekundzie: **CRASH!** (celowy RuntimeException)
- Aplikacja się zamknie

### Krok 5: Obserwuj auto-restart
Po ~3 sekundach (2s wait + 1s alarm delay):
- ✅ Aplikacja automatycznie się **RESTARTUJE**
- ✅ Otwiera się MainActivity
- ✅ Jesteś zalogowany (sesja zachowana)
- ✅ Wszystko działa normalnie

## 📊 Sekwencja Zdarzeń

```
1. Kliknięcie "Test Crash"
   └─ Handler.postDelayed(1000ms)

2. Po 1 sekundzie: RuntimeException
   └─ AutoRestartExceptionHandler.uncaughtException()
       ├─ Log do Timber: "🔥 CRASH na wątku: main"
       ├─ Log do Crashlytics (custom keys: thread, crash_handled_at)
       ├─ Thread.sleep(2000ms) - czekaj na zapis logów
       ├─ scheduleAppRestart() - AlarmManager za 1s
       └─ defaultHandler.uncaughtException() - system kończy proces

3. System zabija proces aplikacji
   └─ Aplikacja się zamyka

4. Po ~1 sekundzie: AlarmManager wyzwala PendingIntent
   └─ MainActivity startuje z flagą Intent.FLAG_ACTIVITY_NEW_TASK
       └─ Aplikacja wraca do życia ✅
```

## 🔍 Weryfikacja Działania

### Sprawdź Logi (Logcat)

**Przed crashem:**
```
TEST_CRASH: 🧪 Symulowany crash - test auto-restart
ItsChat: 🔥 CRASH na wątku: main
ItsChat: 🔄 Planując restart aplikacji za 1 sekundę...
ItsChat: ✅ Restart zaplanowany na za 1s
```

**Po restarcie:**
```
MainActivity: onCreate() - RESTART_ACTION
ItsChat: onCreate() - inicjalizacja aplikacji
```

### Sprawdź Firebase Crashlytics

Po zsynchronizowaniu z Firebase:
```
Crashlytics Dashboard:
  ├─ Crash: RuntimeException
  ├─ Message: "🧪 TEST CRASH: Sprawdzenie mechanizmu auto-restart"
  ├─ Thread: main
  ├─ Custom Keys:
  │   ├─ thread: "main"
  │   └─ crash_handled_at: "1739318689033"
  └─ Stack trace: [...]
```

### Sprawdź Zachowanie Użytkownika

- ✅ Aplikacja restartuje się automatycznie (bez interakcji)
- ✅ Sesja logowania zachowana (nie trzeba logować się ponownie)
- ✅ Stan aplikacji przywrócony
- ✅ Wszystkie serwisy działają (WebSocket, Foreground Service, etc.)

## ⚠️ Uwagi Bezpieczeństwa

### Dlaczego tylko w DEBUG?

```kotlin
if (com.itsorderchat.BuildConfig.DEBUG) {
    // Przycisk widoczny
}
```

**Powody:**
1. ❌ **Nie chcemy** żeby użytkownicy mogli celowo crashować aplikację w produkcji
2. ❌ **Nie chcemy** wysyłać fałszywych crash raportów do Crashlytics
3. ❌ **Nie chcemy** ryzykować utraty danych w środowisku produkcyjnym
4. ✅ **Chcemy** tylko testować mechanizm podczas developmentu

### Flaga RELEASE

W wersji RELEASE:
- Przycisk jest całkowicie UKRYTY
- Kod nie jest kompilowany do APK (dzięki `if (DEBUG)`)
- Brak ryzyka przypadkowego kliknięcia przez użytkownika

## 🎨 Wygląd UI

**Ikona:** 🐛 BugReport (czerwona)
**Tło ikony:** Czerwone (#FF5252) z 15% przezroczystością
**Tytuł:** "Test Crash & Auto-Restart"
**Podtytuł:** "Symuluje crash aplikacji i sprawdza auto-restart (tylko DEBUG)"
**Kolor:** Czerwony (ostrzegawczy)

## 📝 Alternatywne Użycie

### Test 1: Crash na wątku UI
```kotlin
// Już zaimplementowane - domyślne zachowanie
throw RuntimeException("TEST")
```

### Test 2: Crash na wątku w tle (opcjonalnie)
```kotlin
// Można dodać drugi przycisk dla testowania crashy w coroutines:
viewModel.viewModelScope.launch {
    throw RuntimeException("TEST CRASH - Background Thread")
}
```

### Test 3: Test bez auto-restartu (opcjonalnie)
```kotlin
// Można dodać trzeci przycisk który crashuje BEZ restartu:
// (wyłączając handler tymczasowo)
```

## 🔧 Rozwiązywanie Problemów

### Problem: Aplikacja nie restartuje się

**Możliwe przyczyny:**
1. AlarmManager został zablokowany przez system
2. Brak uprawnień SCHEDULE_EXACT_ALARM (Android 12+)
3. Battery optimization zabił proces

**Rozwiązanie:**
```kotlin
// W AndroidManifest.xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />

// Wyłącz battery optimization dla aplikacji
```

### Problem: Crashlytics nie loguje

**Możliwe przyczyny:**
1. Crashlytics wyłączony w DEBUG
2. Brak połączenia z internetem
3. Crashlytics nie zsynchronizował się

**Rozwiązanie:**
```kotlin
// W gradle.properties
wc.crashlytics.enabled.in.debug=true

// Wymuś synchronizację
FirebaseCrashlytics.getInstance().sendUnsentReports()
```

### Problem: Przycisk niewidoczny

**Możliwa przyczyna:**
Aplikacja uruchomiona w trybie RELEASE

**Rozwiązanie:**
```bash
# Upewnij się że budujesz DEBUG variant
./gradlew assembleDebug

# Lub w Android Studio:
Build Variants → app → debug
```

## ✅ Testy Akceptacyjne

### Test 1: Widoczność przycisku
- [ ] Przycisk widoczny w DEBUG
- [ ] Przycisk ukryty w RELEASE

### Test 2: Crash i restart
- [ ] Kliknięcie → crash po 1s
- [ ] Log w Timber widoczny
- [ ] Aplikacja restartuje się po ~3s
- [ ] MainActivity otwiera się poprawnie

### Test 3: Crashlytics
- [ ] Crash raport wysłany do Firebase
- [ ] Custom keys zapisane (thread, crash_handled_at)
- [ ] Stack trace kompletny

### Test 4: Zachowanie sesji
- [ ] Użytkownik nadal zalogowany
- [ ] Stan aplikacji przywrócony
- [ ] WebSocket reconnect działa

## 📊 Metryki

**Oczekiwany czas restartu:**
- Crash: 0s
- Wait dla logów: 2s
- Alarm delay: 1s
- System startup: ~1-2s
- **Total: ~4-5 sekund**

**Zużycie zasobów:**
- Brak dodatkowego zużycia (tylko test)
- Handler w pamięci: ~100 bytes
- Crash raport: ~5-10 KB

## 🎯 Podsumowanie

✅ **Dodano:** Przycisk "Test Crash & Auto-Restart" w Ustawieniach Głównych  
✅ **Widoczność:** Tylko DEBUG (bezpieczeństwo)  
✅ **Funkcja:** Symuluje crash i weryfikuje auto-restart  
✅ **Logi:** Pełne w Timber i Crashlytics  
✅ **Bezpieczeństwo:** Ukryty w RELEASE  
✅ **UI:** Czerwona ikona ostrzegawcza  

**Status:** ✅ Gotowe do testowania

---

**Data implementacji:** 2026-02-12  
**Wersja:** v1.8.0  
**Typ:** Narzędzie deweloperskie (DEBUG only)

