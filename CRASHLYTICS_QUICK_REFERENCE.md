# 📋 QUICK REFERENCE - GOOGLE CRASHLYTICS

## ✅ STATUS: PEŁNA IMPLEMENTACJA

---

## 1️⃣ KONFIGURACJA (GRADLE)

### ✅ build.gradle (root)
```groovy
dependencies {
    classpath 'com.google.gms:google-services:4.4.2'           // ✅
    classpath 'com.google.firebase:firebase-crashlytics-gradle:3.0.2'  // ✅
}
```

### ✅ build.gradle (app)
```groovy
plugins {
    id 'com.google.gms.google-services'      // ✅
    id 'com.google.firebase.crashlytics'     // ✅
}

dependencies {
    implementation platform('com.google.firebase:firebase-bom:33.2.0')
    implementation 'com.google.firebase:firebase-crashlytics'   // ✅
    implementation 'com.google.firebase:firebase-analytics'     // ✅
    implementation 'com.google.firebase:firebase-analytics-ktx'
    implementation 'com.google.firebase:firebase-auth-ktx'
    implementation 'com.google.firebase:firebase-firestore-ktx'
    implementation 'com.google.firebase:firebase-messaging-ktx'
}
```

---

## 2️⃣ FIREBASE (google-services.json)

### ✅ Lokacja
```
L:\SHOP APP\app\google-services.json
```

### ✅ Zawiera
```json
{
  "project_id": "fir-crashlytics-da7b7",
  "mobilesdk_app_id": "1:80201754190:android:473af0f0a0d4b076e0c51f",
  "package_name": "com.itsorderchat"
}
```

---

## 3️⃣ INICJALIZACJA (ItsChat.kt)

### ✅ onCreate()
```kotlin
override fun onCreate() {
    super.onCreate()
    
    // Inicjalizuj Firebase
    FirebaseApp.initializeApp(this)  // ✅
    
    // Ustawienia Crashlytics
    FirebaseCrashlytics.getInstance()
        .setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)  // ✅
    
    // Dodaj do Timber
    Timber.plant(CrashlyticsTree())  // ✅
    
    // Handler dla uncaught exceptions
    Thread.setDefaultUncaughtExceptionHandler(
        TimberUncaughtExceptionHandler(Thread.getDefaultUncaughtExceptionHandler())
    )  // ✅
}
```

---

## 4️⃣ OBSŁUGA BŁĘDÓW (3 poziomy)

### Poziom 1: Thread Exceptions
```kotlin
class TimberUncaughtExceptionHandler : UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        Timber.e(e, "UNCAUGHT on ${t.name}")                    // ✅
        FirebaseCrashlytics.getInstance().recordException(e)    // ✅
        defaultHandler?.uncaughtException(t, e)
    }
}
```

### Poziom 2: Coroutine Exceptions
```kotlin
val coroutineErrorHandler = CoroutineExceptionHandler { _, throwable ->
    Timber.e(throwable, "Uncaught coroutine")                  // ✅
    FirebaseCrashlytics.getInstance().recordException(throwable)  // ✅
}
```

### Poziom 3: Timber Integration
```kotlin
class CrashlyticsTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Breadcrumbs
        crashlytics.log("${priorityToText(priority)} [$tag] $message")  // ✅
        
        // Exceptions
        if (t != null && priority >= Log.WARN) {
            crashlytics.recordException(t)  // ✅
        }
    }
}
```

---

## 5️⃣ PERMISSIONS (AndroidManifest.xml)

### ✅ Wymagane
```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

### ✅ Application
```xml
<application android:name=".ItsChat" ...>
    <!-- All other components -->
</application>
```

---

## 6️⃣ CO JEST ZBIERANE

| Element | Zbierane | Format |
|---------|----------|--------|
| Crash | ✅ TAK | Stack trace + timestamp |
| ANR | ✅ TAK | Freeze detection |
| Wyjątki | ✅ TAK | Full exception info |
| Logi WARN+ | ✅ TAK | Breadcrumbs |
| Device info | ✅ TAK | Model, OS, app version |
| Thread name | ✅ TAK | Custom key |
| Metadane | ✅ TAK | Custom keys |

---

## 7️⃣ DEBUG vs RELEASE

```kotlin
FirebaseCrashlytics.getInstance()
    .setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

DEBUG Build (devevelopment):
  ├─ BuildConfig.DEBUG = true
  └─ Crashlytics enabled = FALSE ❌ (nie wysyła)

RELEASE Build (production):
  ├─ BuildConfig.DEBUG = false
  └─ Crashlytics enabled = TRUE ✅ (wysyła)
```

---

## 8️⃣ FIREBASE CONSOLE

### Adres
```
https://console.firebase.google.com/
Projekt: fir-crashlytics-da7b7
```

### Sekcje
```
Crashlytics
├── Issues (lista wszystkich crash'y)
├── Breadcrumbs (logi poprzedzające crash)
├── Graphs (statystyki, crash-free %)
├── Affected Users (ile osób dotkniętych)
└── ...
```

---

## 9️⃣ TESTING

### Test 1: Wysłanie Test Crash'u
```kotlin
FirebaseCrashlytics.getInstance().recordException(
    Exception("🧪 TEST CRASH")
)
// Pojawi się w Firebase w ciągu 5 minut
```

### Test 2: Sprawdzenie Timber Integracji
```kotlin
Timber.w("Warning message")  // Będzie w breadcrumbs
Timber.e("Error message")    // Będzie w breadcrumbs
// Pojawi się w Firebase Crashlytics > Breadcrumbs
```

### Test 3: Custom Key
```kotlin
FirebaseCrashlytics.getInstance()
    .setCustomKey("device", Build.MODEL)
// Pojawi się w crash details
```

---

## 🔟 TROUBLESHOOTING

| Problem | Przyczyna | Rozwiązanie |
|---------|-----------|------------|
| Crash nie pojawia się | DEBUG mode | Użyj RELEASE build |
| Firebase init failed | google-services.json | Sprawdź project_id |
| Breadcrumbs puste | Brak Timber logs | Dodaj Timber.w() lub Timber.e() |
| Wysyłanie trwa | Bez internetu | Sprawdź connection |

---

## 1️⃣1️⃣ CHECKLIST

```
Pre-Release:
  ☑️ Pluginy Gradle skonfigurowane
  ☑️ google-services.json w app/
  ☑️ ItsChat.kt inicjalizuje Firebase
  ☑️ CrashlyticsTree w Timber.forest()
  ☑️ build.gradle clean assembleRelease OK
  
Testing:
  ☑️ Test crash pojawia się w Firebase
  ☑️ Breadcrumbs są widoczne
  ☑️ Custom keys są widoczne
  ☑️ DEBUG build NIE wysyła
  
Monitoring:
  ☑️ Firebase Console accessible
  ☑️ Crashlytics tab widoczny
  ☑️ Alerting configured (optional)
```

---

## 1️⃣2️⃣ METODY API

```kotlin
// Wysłanie errora
FirebaseCrashlytics.getInstance()
    .recordException(throwable)

// Log (breadcrumb)
FirebaseCrashlytics.getInstance()
    .log("Message here")

// Custom key
FirebaseCrashlytics.getInstance()
    .setCustomKey("key_name", "value")

// User ID
FirebaseCrashlytics.getInstance()
    .setUserId("user_123")

// Wymuś wysłanie
FirebaseCrashlytics.getInstance()
    .sendUnsentReports()

// Włącz/Wyłącz
FirebaseCrashlytics.getInstance()
    .setCrashlyticsCollectionEnabled(true)
```

---

## 1️⃣3️⃣ FLOW ZBIERANIA BŁĘDÓW

```
Exception → UncaughtHandler → Timber → CrashlyticsTree 
→ FirebaseCrashlytics → Firebase Cloud → Console
```

---

## 1️⃣4️⃣ FILES

- `ItsChat.kt` - Inicjalizacja
- `build.gradle` (root) - Pluginy
- `build.gradle` (app) - Zależności
- `google-services.json` - Firebase config
- `AndroidManifest.xml` - Permissions

---

## 1️⃣5️⃣ DOKUMENTACJA

- 📖 https://firebase.google.com/docs/crashlytics
- 🎯 https://firebase.google.com/docs/crashlytics/get-started

---

## WNIOSEK

✅ **PEŁNA, PRACUJĄCA IMPLEMENTACJA GOOGLE CRASHLYTICS**

Aplikacja zbiera i raportuje:
- 🚨 Crash'y
- 🔴 ANR
- ⚠️ Logi
- 📊 Metryki

Wszystko widoczne w Firebase Console.

**STATUS: READY FOR PRODUCTION ✅**


