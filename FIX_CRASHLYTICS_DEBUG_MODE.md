# 🔧 NAPRAWA: Crashlytics nie pojawia się w Firebase (DEBUG Mode)

## Problem: Test crash nie pojawia się w Firebase Console

### Przyczyna:
```kotlin
FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
```

W DEBUG mode (`BuildConfig.DEBUG = true`), Crashlytics jest **wyłączony** (`!true = false`).

To jest celowe dla czystości testów, ale uniemożliwia testowanie funkcjonalności w DEV.

---

## Rozwiązanie: Dodana flaga kontrolna

### 1. gradle.properties
```ini
# Crashlytics Configuration
# true = zbiera crash'y także w DEBUG mode (dla testowania)
# false = wyłącza w DEBUG mode (dla czystości testów)
wc.crashlytics.enabled.in.debug = true
```

**Domyślnie ustawiona na `true`** aby umożliwić testowanie w DEBUG.

### 2. ItsChat.kt - nowa logika

```kotlin
val crashlyticsEnabled = if (BuildConfig.DEBUG) {
    // W DEBUG: czytaj flagę z gradle.properties
    try {
        BuildConfig.CRASHLYTICS_ENABLED_IN_DEBUG.toBoolean()
    } catch (e: Exception) {
        false  // fallback
    }
} else {
    true  // W RELEASE: zawsze włącz
}

FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(crashlyticsEnabled)
```

---

## Jak to działa:

### RELEASE build:
```
BuildConfig.DEBUG = false
  ↓
crashlyticsEnabled = true (zawsze)
  ↓
Crashlytics zbiera i wysyła crash'y
```

### DEBUG build (z nową flagą):
```
BuildConfig.DEBUG = true
  ↓
Czytaj: BuildConfig.CRASHLYTICS_ENABLED_IN_DEBUG = "true"
  ↓
crashlyticsEnabled = true (z flagi)
  ↓
Crashlytics zbiera i wysyła crash'y ✅
```

---

## Co się zmienia:

| Scenariusz | Przed | Po |
|-----------|-------|-----|
| RELEASE build | Wysyła | Wysyła ✅ |
| DEBUG build (def.) | NIE wysyła ❌ | Wysyła ✅ |
| DEBUG build (disabled) | NIE wysyła | NIE wysyła (jeśli zmienisz flagę) |

---

## Kontrola flagi

### Aby WŁĄCZYĆ Crashlytics w DEBUG (DEFAULT):
```ini
# gradle.properties
wc.crashlytics.enabled.in.debug = true
```

### Aby WYŁĄCZYĆ Crashlytics w DEBUG:
```ini
# gradle.properties
wc.crashlytics.enabled.in.debug = false
```

Zmień wartość i rebuild aplikacji.

---

## Logowanie statusu

Po zmianie, w Logcat będziesz widzieć:

```
I/Timber: 🔍 Crashlytics in DEBUG mode: enabled=true
```

lub

```
I/Timber: 🔍 Crashlytics in DEBUG mode: enabled=false
```

---

## System czytania gradle.properties

Gradle już posiada system do czytania properties z prefiksem `wc.`:

```groovy
// app/build.gradle
android.buildTypes.all { buildType ->
    def properties = loadPropertiesFromFile(inputFile)
    properties.any { property ->
        if (property.key.toLowerCase().startsWith("wc.")) {
            buildType.buildConfigField "String", property.key.replace("wc.", "").replace(".", "_").toUpperCase(),
                    "\"${property.value}\""
        }
    }
}
```

**Magiczne przekształcenie:**
```
gradle.properties:  wc.crashlytics.enabled.in.debug = true
                    ↓
BuildConfig field:  CRASHLYTICS_ENABLED_IN_DEBUG = "true"
```

---

## Zmienione pliki:

### 1. gradle.properties
```
Lokalizacja: L:\SHOP APP\gradle.properties
Linie: 20-27
Dodano: 4 linii (komentarz + flaga)
```

### 2. ItsChat.kt
```
Lokalizacja: L:\SHOP APP\app\src\main\java\com\itsorderchat\ItsChat.kt
Linie: 47-62
Zmieniono: 3 linii → 18 linii (nowa logika + logging)
Dodano: 15 linii
```

---

## Testing:

### Aby testować Test Crash button:

1. **Rebuild aplikacji:**
   ```bash
   ./gradlew clean assembleDebug
   ```

2. **Zainstaluj na device:**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Przejdź do Settings → Main Settings**

4. **Kliknij [Test] przy "🧪 Test Crashlytics"**

5. **Potwierdź: "Tak, wyślij test"**

6. **Sprawdzisz logcat:**
   ```
   I/Timber: 🔍 Crashlytics in DEBUG mode: enabled=true
   W/SettingsScreenKt: 🧪 Test Crashlytics triggered by user
   I/SettingsScreenKt: ✅ Test crash sent to Firebase Crashlytics
   ```

7. **Czekaj ~5 minut**

8. **Sprawdź Firebase Console:**
   ```
   https://console.firebase.google.com/project/fir-crashlytics-da7b7/crashlytics
   ```

---

## ⚠️ Ważne notatki:

### 1. Build system
Flaga `wc.crashlytics.enabled.in.debug` jest przetwarzana podczas budowania. Musisz **rebuild** aplikacji aby zmiana zadziałała.

### 2. Fallback
Jeśli będzie błąd czytania flagi, domyślnie wyłącza Crashlytics w DEBUG (`false`).

### 3. RELEASE zawsze
W RELEASE build Crashlytics jest **zawsze włączony** niezależnie od flagi.

### 4. Logowanie
Po starcie aplikacji będziesz widzieć w logcat czy Crashlytics jest enabled czy nie.

---

## FAQ

### P: Dlaczego domyślnie Crashlytics jest wyłączony w DEBUG?
A: Aby uniknąć szumu z testami. Teraz możesz go włączyć poprzez flagę.

### P: Czy to wpłynie na RELEASE build?
A: Nie. RELEASE zawsze ma Crashlytics enabled (`true`).

### P: Gdzie zmienić flagę?
A: W `gradle.properties` na linii ~27: `wc.crashlytics.enabled.in.debug = true/false`

### P: Czy muszę usuwać aplikację?
A: Tak, rebuild z `clean` jest zalecany: `./gradlew clean assembleDebug`

### P: A jeśli chcę szybko wyłączyć?
A: Zmień `wc.crashlytics.enabled.in.debug = false` w gradle.properties i rebuild.

---

## Podsumowanie

✅ **Problem rozwiązany**
- Crashlytics w DEBUG teraz wysyła crash'y (domyślnie)
- Kontrolowana przez flagę gradle.properties
- RELEASE zawsze zbiera
- Logowanie statusu w Logcat

**Możesz teraz testować Test Crash button i będziesz widzieć crash'y w Firebase! 🎉**


