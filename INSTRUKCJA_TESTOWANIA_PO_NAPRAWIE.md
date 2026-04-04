# 📋 INSTRUKCJA KROK PO KROKU - Testowanie po naprawie

## ✅ Crashlytics teraz wysyła crash'y w DEBUG mode

---

## KROK 1️⃣: Sprawdzić zmienione pliki

### gradle.properties
```bash
# Sprawdzić czy flaga jest ustawiona
Linia ~27:
wc.crashlytics.enabled.in.debug = true
```

### ItsChat.kt
```kotlin
# Sprawdzić czy nowa logika jest na miejscu
Linie 47-62: val crashlyticsEnabled = if (BuildConfig.DEBUG) { ... }
```

✅ **Oba pliki mają zmianę** → Przejdź do KROKU 2

---

## KROK 2️⃣: Clean build

Wymaż build cache:

```bash
cd L:\SHOP APP
./gradlew clean
```

Czekaj aż się skończy (może potrwać kilka minut).

---

## KROK 3️⃣: Zbuduj DEBUG APK

```bash
./gradlew assembleDebug
```

Czekaj na komunikat:
```
BUILD SUCCESSFUL in XXs
```

---

## KROK 4️⃣: Zainstaluj na device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Czekaj na:
```
Success
```

---

## KROK 5️⃣: Otwórz aplikację

1. Wciśnij przycisk HOME na device
2. Szukaj "ItsChat" lub "ItsOrderChat"
3. Otwórz aplikację

---

## KROK 6️⃣: Przejdź do Settings

1. W aplikacji szukaj menu Settings (Ustawienia)
2. Kliknij Settings
3. Kliknij "Main Settings" (Główne Ustawienia)

---

## KROK 7️⃣: Przewiń na dół

Przewiń całą stronę w dół aż zobaczysz sekcję "Other" (Inne).

---

## KROK 8️⃣: Kliknij przycisk Test

Szukaj:
```
🧪 Test Crashlytics
Wyślij test crash do Firebase Crashlytics
                                    [Test]
```

Kliknij przycisk **[Test]**

---

## KROK 9️⃣: Potwierdź w dialogu

Dialog:
```
⚠️ Test Crashlytics

To wyśle test crash do Firebase Crashlytics.

Crash pojawi się w Firebase Console w ciągu kilku minut.

Kontynuować?

[Tak, wyślij test]  [Anuluj]
```

Kliknij: **[Tak, wyślij test]**

---

## KROK 🔟: Sprawdzić Logcat

Otwórz Android Studio i sprawdzisz logcat.

Powinieneś zobaczyć:

```
I/Timber: 🔍 Crashlytics in DEBUG mode: enabled=true
W/SettingsScreenKt: 🧪 Test Crashlytics triggered by user
I/SettingsScreenKt: ✅ Test crash sent to Firebase Crashlytics
```

✅ **Jeśli widzisz te logi → Crashlytics wysyła!**

❌ **Jeśli "enabled=false" → Flaga nie została przeczytana**

---

## KROK 1️⃣1️⃣: Czekaj 5 minut

Czekaj na przetworzenie danych w Firebase Cloud.

---

## KROK 1️⃣2️⃣: Sprawdź Firebase Console

1. Otwórz przeglądarkę
2. Przejdź do:
```
https://console.firebase.google.com/project/fir-crashlytics-da7b7/crashlytics
```

3. Kliknij tab **"Issues"**

4. Szukaj nowego issue'u:
```
TEST CRASH: This is a test crash from ItsChat Settings...
```

✅ **Jeśli widzisz → Crashlytics działa prawidłowo!**

---

## KROK 1️⃣3️⃣: Weryfikuj detale

Kliknij na issue aby sprawdzić:

```
Custom Keys:
  ✅ test_type: "manual_from_settings"
  ✅ timestamp: "1707564890000"

Breadcrumbs:
  ✅ WARN 🧪 Test Crashlytics triggered by user
  ✅ INFO ✅ Test crash sent to Firebase Crashlytics

Stack Trace:
  ✅ java.lang.Exception: TEST CRASH: ...
```

✅ **Wszystko powinno być widoczne**

---

## Troubleshooting

### ❌ Logcat pokazuje "enabled=false"
**Przyczyna:** Flaga `wc.crashlytics.enabled.in.debug` nie została przeczytana.

**Rozwiązanie:**
1. Sprawdź `gradle.properties` linia ~27
2. Upewnij się że pisze: `wc.crashlytics.enabled.in.debug = true`
3. Zrób `./gradlew clean`
4. Zbuduj ponownie

### ❌ Crash nie pojawia się w Firebase
**Przyczyna:** 
- Zbyt krótko czekałeś (potrzebujesz 5 minut)
- Brak internetu na device
- Google-services.json problem

**Rozwiązanie:**
1. Czekaj pełne 5 minut
2. Sprawdzisz internet: Settings → Wi-Fi / Mobile Data
3. Refresh Firebase Console (F5)
4. Sprawdzisz czy `google-services.json` jest prawidłowy

### ❌ Test crash pojawia się ale bez custom keys
**Przyczyna:** Custom keys nie są wysyłane prawidłowo.

**Rozwiązanie:**
1. Sprawdzisz czy `triggerTestCrash()` ma `setCustomKey()`
2. Czekaj dłużej (do 10 minut)
3. Refresh Firebase Console

---

## ✅ Success Criteria

Po wykonaniu kroków powinieneś widzieć:

```
✅ Logcat: "🔍 Crashlytics in DEBUG mode: enabled=true"
✅ Logcat: "🧪 Test Crashlytics triggered by user"
✅ Logcat: "✅ Test crash sent to Firebase Crashlytics"
✅ Firebase Console: Nowy issue "TEST CRASH..."
✅ Custom keys widoczne
✅ Breadcrumbs widoczne
✅ Stack trace widoczny
```

---

## 🎉 Gotowe!

Jeśli wszystkie kroki się powiedły:

**Crashlytics jest w 100% pracujący! 🚀**

Możesz teraz:
- ✅ Testować Crashlytics bez budowania RELEASE
- ✅ Weryfikować Firebase integrację
- ✅ Sprawdzać custom keys i breadcrumbs
- ✅ Debugować problemy Crashlytics

---

## Następne kroki:

1. **Build RELEASE APK** (opcjonalnie) aby sprawdzić czy również działa
2. **Monitoruj Firebase Console** codziennie
3. **Deploy na Play Store** gdy wszystko będzie gotowe

---

**Data:** 2026-02-11
**Status:** ✅ READY FOR TESTING


