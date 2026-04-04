# 🚀 Quick Wins - Szybkie Poprawki

**Zmiany do wprowadzenia NATYCHMIAST (1-2 dni)**

---

## 1. Napraw Literówki w Nazwach Pakietów

### ❌ Przed:
```
ui/utili/
data/entity/datebase/
```

### ✅ Po:
```
ui/util/
data/entity/database/
```

**Kroki:**
1. W Android Studio: kliknij prawym na pakiet `utili` → Refactor → Rename → `util`
2. Podobnie dla `datebase` → `database`
3. Android Studio automatycznie zaktualizuje wszystkie importy

---

## 2. Dodaj Extension Functions dla Częstych Operacji

### ❌ Przed (duplikacja w wielu miejscach):
```kotlin
Text(order.consumer.phone ?: stringResource(R.string.common_dash))
Text(order.consumer.email ?: stringResource(R.string.common_dash))
Text(order.notes ?: stringResource(R.string.common_dash))
```

### ✅ Po:
```kotlin
// Utwórz plik: core/util/extensions/StringExtensions.kt
fun String?.orDash(): String = this ?: "—"

// Użycie:
Text(order.consumer.phone.orDash())
Text(order.consumer.email.orDash())
Text(order.notes.orDash())
```

**Korzyści:**
- Mniej kodu
- Łatwiejsze w utrzymaniu
- Spójność w całej aplikacji

---

## 3. Usuń Nieużywane Importy i Zmienne

### W ItsChat.kt:
```kotlin
// ❌ USUŃ (nieużywane):
lateinit var tokenProvider: DataStoreTokenProvider
lateinit var authApi: AuthApi
lateinit var okHttpClient: OkHttpClient

// Te zmienne są dostępne przez Hilt DI, nie trzeba ich tu trzymać
```

**Jak znaleźć nieużywane:**
1. Analyze → Run Inspection by Name → "Unused declaration"
2. Usuń wszystkie nieużywane deklaracje

---

## 4. Skonsoliduj Zarządzanie Preferencjami

### ❌ Problem: 3 różne klasy do tego samego
```
AppPrefs.kt (SharedPreferences)
UserPreferences.kt (DataStore)
DataStoreTokenProvider.kt (DataStore)
```

### ✅ Rozwiązanie: Jedna klasa
```kotlin
// data/local/preferences/AppPreferencesManager.kt
@Singleton
class AppPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.preferencesDataStore
    
    // Wszystkie preferencje w jednym miejscu
}
```

---

## 5. Dodaj KDoc do Publicznych API

### ❌ Przed:
```kotlin
suspend fun sendToExternalCourier(orderId: String, body: DispatchCourier)
```

### ✅ Po:
```kotlin
/**
 * Wysyła zamówienie do zewnętrznego kuriera
 * 
 * @param orderId Unikalny identyfikator zamówienia
 * @param body Dane kuriera wraz z czasem przygotowania
 * @return Result z potwierdzeniem wysłania lub błędem
 */
suspend fun sendToExternalCourier(
    orderId: String,
    body: DispatchCourier
): Result<DispatchResponse>
```

---

## 6. Napraw Ostrzeżenia Kompilatora

### ❌ Obecne ostrzeżenia:
```kotlin
// WARNING: Unnecessary safe call
order.orderStatus?.slug  // orderStatus jest non-null!

// WARNING: Elvis operator always returns left operand
val rawJson = args.firstOrNull()?.toString() ?: return  // toString() zawsze zwraca String
```

### ✅ Poprawione:
```kotlin
order.orderStatus.slug

val rawJson = args.firstOrNull()?.toString() ?: return
// LUB lepiej:
val rawJson = args.firstOrNull()?.toString()
if (rawJson.isNullOrBlank()) return
```

**Jak naprawić:**
1. Build → Analyze → Inspect Code
2. Przejrzyj wszystkie WARNING
3. Napraw trywialne przypadki

---

## 7. Dodaj Constants dla Magic Numbers/Strings

### ❌ Przed (rozproszone po kodzie):
```kotlin
if (preparationTime in 15..120) { }
val NOTIFICATION_ID = 1997
val TAG = "SocketStaffEventsHandler"
```

### ✅ Po:
```kotlin
// core/util/Constants.kt
object OrderConstants {
    const val MIN_PREPARATION_TIME = 15
    const val MAX_PREPARATION_TIME = 120
}

object NotificationIds {
    const val WS_DISCONNECT = 1997
    const val ORDER_ALARM = 2000
}

// Użycie:
if (preparationTime in OrderConstants.MIN_PREPARATION_TIME..OrderConstants.MAX_PREPARATION_TIME)
```

---

## 8. Wprowadź Sealed Interface dla Resource

### ❌ Przed (Resource.kt):
```kotlin
sealed class Resource<out T> {
    data class Success<out T>(val value: T) : Resource<T>()
    data class Failure(val errorCode: Int?, val errorBody: Any?) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
```

### ✅ Po:
```kotlin
sealed interface Resource<out T> {
    data class Success<out T>(val value: T) : Resource<T>
    data class Failure(
        val errorCode: Int?,
        val errorBody: Any?,
        val exception: Throwable? = null
    ) : Resource<Nothing>
    data object Loading : Resource<Nothing>
}
```

**Korzyści:**
- `interface` zamiast `class` = mniej pamięci
- `data object` zamiast `object` = lepsze toString()
- Dodatkowe `exception` field dla debugowania

---

## 9. Dodaj Logging Helper

### ❌ Przed (duplikacja):
```kotlin
Timber.tag("SocketStaffEventsHandler").d("...")
Timber.tag("OrdersViewModel").d("...")
Timber.tag("HomeScreen").d("...")
```

### ✅ Po:
```kotlin
// core/util/Logger.kt
object Logger {
    inline fun <reified T> T.logDebug(message: String) {
        Timber.tag(T::class.java.simpleName).d(message)
    }
    
    inline fun <reified T> T.logError(error: Throwable, message: String) {
        Timber.tag(T::class.java.simpleName).e(error, message)
    }
}

// Użycie:
class OrdersViewModel {
    fun something() {
        logDebug("This is a debug message")  // Auto tag: "OrdersViewModel"
    }
}
```

---

## 10. Ujednolicenie Nazewnictwa Repository

### ❌ Przed (niespójne):
```
OrdersRepository  ✓ (liczba mnoga)
ProductsRepository  ✓ (liczba mnoga)
VehicleRepository  ✗ (liczba pojedyncza!)
```

### ✅ Po (konsekwentnie liczba mnoga):
```
OrdersRepository
ProductsRepository
VehiclesRepository  ← Zmienione
```

**Albo** (konsekwentnie liczba pojedyncza):
```
OrderRepository
ProductRepository
VehicleRepository
```

**Wybierz jeden styl i trzymaj się go!**

---

## 11. Dodaj @VisibleForTesting

### ❌ Przed:
```kotlin
class OrdersViewModel {
    fun syncOrders() { }  // Internal, ale używane w testach
}
```

### ✅ Po:
```kotlin
class OrdersViewModel {
    @VisibleForTesting
    internal fun syncOrders() { }
}
```

**Korzyści:**
- Jasne oznaczenie co jest testowane
- Linter ostrzeże jeśli użyjemy tego w produkcji

---

## 12. Dodaj TODOs dla Technicznego Długu

### Format:
```kotlin
// TODO(nazwisko, 2025-01-03): Opisz co trzeba zrobić i dlaczego
// TODO(jankowalski, 2025-01-03): Refaktoryzacja - przenieść do UseCase
```

**Znajdź wszystkie TODOs:**
1. View → Tool Windows → TODO
2. Przejrzyj i zaktualizuj stare TODOs

---

## 13. Dodaj .editorconfig

Utwórz w rootcie projektu `.editorconfig`:

```ini
[*.kt]
indent_size = 4
insert_final_newline = true
max_line_length = 120

[*.xml]
indent_size = 4
insert_final_newline = true

[*.gradle]
indent_size = 4
```

**Korzyści:**
- Spójne formatowanie w całym zespole
- Automatyczne stosowanie przez IDE

---

## 14. Dodaj Proguard Rules dla Production

W `proguard-rules.pro` dodaj:

```proguard
# Timber - usuń logi w release
-assumenosideeffects class timber.log.Timber* {
    public static *** d(...);
    public static *** v(...);
}

# Zachowaj nazwy klas dla Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep class com.itsorderchat.** { *; }
```

---

## 15. Code Cleanup Script

Utwórz skrypt `cleanup.sh`:

```bash
#!/bin/bash
echo "🧹 Running code cleanup..."

# Format code
./gradlew spotlessApply

# Remove unused imports
./gradlew removeUnusedImports

# Lint check
./gradlew lint

echo "✅ Cleanup complete!"
```

---

## ✅ Checklist Quick Wins

- [ ] Naprawić literówki w nazwach pakietów (`utili` → `util`)
- [ ] Dodać extension functions (`.orDash()`)
- [ ] Usunąć nieużywane importy i zmienne
- [ ] Dodać KDoc do publicznych API
- [ ] Naprawić wszystkie compiler warnings
- [ ] Utworzyć `Constants.kt`
- [ ] Ulepszyć `Resource` sealed class
- [ ] Dodać logging helper
- [ ] Ujednolicić nazewnictwo Repository
- [ ] Dodać `@VisibleForTesting` annotations
- [ ] Zaktualizować TODOs
- [ ] Dodać `.editorconfig`
- [ ] Zaktualizować Proguard rules
- [ ] Utworzyć cleanup script

---

**Czas wykonania: 1-2 dni**  
**Wymagane umiejętności: Basic Kotlin/Android**  
**Wpływ: Średni (poprawa czytelności, mniej błędów)**

