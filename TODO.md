# 📋 TODO - Lista Zadań do Poprawy Projektu ItsOrderChat

**Ostatnia aktualizacja:** 2025-01-03  
**Priorytet:** Wysoki → Średni → Niski  
**Status:** 🔴 Do zrobienia | 🟡 W trakcie | 🟢 Gotowe

---

## 🎯 Quick Wins (1-2 dni) - ZRÓB TO NAJPIERW!

### 1. Napraw Literówki w Nazwach Pakietów 🟢

**Problem:**
- ~~`ui/utili/` - literówka "utili" zamiast "util"~~
- ~~`data/entity/datebase/` - literówka "datebase" zamiast "database"~~

**✅ WYKONANE!**

Package names zostały naprawione:
1. ✅ `MapUtils.kt`, `LogTimeFormatter.kt`, `LocationUtils.kt` - package zmieniony na `ui.util`
2. ✅ `AppDatabase.kt` - package już poprawny `data.database`
3. ✅ Importy zaktualizowane w `HomeActivity.kt` i `LogsScreen.kt`

**⏳ WYMAGA:** Fizyczne przeniesienie folderów w Android Studio (Refactor → Rename)

**📋 INSTRUKCJE KROK PO KROKU:**
1. Otwórz Android Studio
2. **ui/utili → ui/util:**
   - Project View → ui → prawy klik na `utili`
   - Refactor → Rename (Shift+F6) → wpisz `util` → Do Refactor
3. **data/entity/datebase → data/entity/database:**
   - Project View → data/entity → prawy klik na `datebase`
   - Refactor → Rename (Shift+F6) → wpisz `database` → Do Refactor
4. Build → Clean Project → Rebuild Project
5. Run App i przetestuj

**📄 Zobacz szczegóły:** `FOLDER_RENAME_INSTRUCTIONS.md`  
**🔧 Weryfikacja:** Uruchom `.\verify-structure.ps1`

**Szacowany czas:** ~~5 minut~~ → Package names wykonane + 5-10 min (przeniesienie folderów w Android Studio)  
**Priorytet:** 🟢 Gotowe (wymaga 10 min w Android Studio)  
**Status:** 🟢 Package names zakończone, foldery do przeniesienia

---

### 2. Usuń Nieużywane Importy 🟢

**Problem:**
- ~~`SocketStaffEventsHandler.kt` - nieużywany import `kotlinx.coroutines.cancel`~~
- ~~`AcceptOrderSheetContent.kt` - 2 nieużywane importy~~
- ~~Wiele innych plików z nieużywanymi importami~~

**✅ WYKONANE!**

Wszystkie nieużywane importy zostały usunięte:
1. ✅ `SocketStaffEventsHandler.kt` - usunięto `kotlinx.coroutines.cancel`
2. ✅ `AcceptOrderSheetContent.kt` - usunięto `Box` i `LocalShipping`
3. ✅ `HomeScreen.kt` - usunięto `DeliveryEnum`

**Szczegóły:** Zobacz `UNUSED_IMPORTS_REMOVED.md`

**Szacowany czas:** ~~10 minut~~ → Wykonane  
**Priorytet:** 🟢 Gotowe  
**Status:** 🟢 Zakończone

---

### 3. Dodaj Extension Functions dla Częstych Operacji 🟢

**✅ WYKONANE!**

Utworzono `util/extensions/StringExtensions.kt` z zestawem funkcji:
- `String?.orDash()` – "—" dla pustych lub null
- `String?.orNA()` – "N/A" dla pustych lub null
- `String?.trimSafe()` – bezpieczne trim
- `String?.toHttpUrlSafe()` – bezpieczne parsowanie URL (OkHttp)
- `String?.toUriSafe()` – bezpieczne parsowanie Uri
- `String.maskPhone()` – maskowanie numeru telefonu `***-***-1234`
- `Int.minutesToLabel()` – etykiety 15/30/60 min
- `String.isEmailLike()` – walidacja email
- `String.isPhoneLike()` – walidacja telefonu

**Szczegóły:** `app/src/main/java/com/itsorderchat/util/extensions/StringExtensions.kt`

**Status:** 🟢 Zakończone

---

### 4. Napraw Deprecated Divider → HorizontalDivider 🟢

**Problem:**
~~`AcceptOrderSheetContent.kt` używa deprecated `Divider` (6 miejsc)~~

**✅ WYKONANE!**

Wszystkie deprecated `Divider` zostały zamienione na `HorizontalDivider`:
1. ✅ Zamieniono 6 wystąpień Divider → HorizontalDivider
2. ✅ Dodano import `androidx.compose.material3.HorizontalDivider`
3. ✅ Usunięto stary import `Divider`

**Szacowany czas:** ~~5 minut~~ → Wykonane  
**Priorytet:** 🟢 Gotowe  
**Status:** 🟢 Zakończone

---

### 5. Napraw Niepotrzebne Safe Calls 🟢

**Problem:**
~~Niepotrzebne safe calls (`?.`) dla non-null typów~~

**✅ WYKONANE!**

Usunięto niepotrzebne safe calls:
1. ✅ `SocketStaffEventsHandler.kt` - `wrapper.orderStatus?.slug` → `wrapper.orderStatus.slug`
2. ✅ `SocketStaffEventsHandler.kt` - `wrapper.orderId ?: ...` → `wrapper.orderId`
3. ✅ `AcceptOrderSheetContent.kt` - `order.consumer.phone ?: ...` → `order.consumer.phone`
4. ✅ `OrdersRepository.kt` - `payload?.pickupEta` → `payload.pickupEta`
5. ✅ `OrdersRepository.kt` - `payload?.dropoffEta` → `payload.dropoffEta`

**Szacowany czas:** ~~15 minut~~ → Wykonane  
**Priorytet:** 🟢 Gotowe  
**Status:** 🟢 Zakończone

---

### 6. Dodaj .editorconfig 🔴

**Problem:**
Brak spójnego formatowania w zespole

**Rozwiązanie:**
Utwórz plik `.editorconfig` w root projektu:

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.kt]
indent_size = 4
indent_style = space
max_line_length = 120
ij_kotlin_allow_trailing_comma = true
ij_kotlin_allow_trailing_comma_on_call_site = true

[*.xml]
indent_size = 4
indent_style = space

[*.gradle]
indent_size = 4
indent_style = space

[*.gradle.kts]
indent_size = 4
indent_style = space

[*.{yml,yaml}]
indent_size = 2
indent_style = space
```

**Szacowany czas:** 5 minut  
**Priorytet:** 🔴 Wysoki  
**Status:** 🔴 Do zrobienia

---

### 7. Usuń Nieużywane Zmienne z Application Class 🟢

**Problem:**
~~`ItsChat.kt` zawiera nieużywane zmienne:~~
```kotlin
// ❌ USUNIĘTE:
lateinit var tokenProvider: DataStoreTokenProvider
lateinit var authApi: AuthApi
lateinit var okHttpClient: OkHttpClient
```

**✅ WYKONANE!**

Wszystkie nieużywane zmienne zostały usunięte:
1. ✅ Usunięto 3 zmienne `lateinit var`
2. ✅ Usunięto 3 nieużywane importy
3. ✅ Usunięto inicjalizację w `onCreate()`
4. ✅ Application class jest teraz czystsza

**Szczegóły:** Zobacz `UNUSED_VARIABLES_REMOVED.md`

**Szacowany czas:** ~~5 minut~~ → Wykonane  
**Priorytet:** 🟢 Gotowe  
**Status:** 🟢 Zakończone

---

### 8. Dodaj Constants dla Magic Numbers 🔴

**Problem:**
Magic numbers/strings rozproszone po kodzie:
```kotlin
if (preparationTime in 15..120) { }
val NOTIFICATION_ID = 1997
```

**Rozwiązanie:**
Utwórz `util/Constants.kt`:

```kotlin
package com.itsorderchat.util

object OrderConstants {
    const val MIN_PREPARATION_TIME = 15
    const val MAX_PREPARATION_TIME = 120
    const val DEFAULT_PREPARATION_TIME = 30
}

object NotificationIds {
    const val WS_DISCONNECT = 1997
    const val ORDER_ALARM = 2000
    const val ROUTE_UPDATE = 2
    const val EXTERNAL_DELIVERY_SUCCESS = 3000
}

object ApiConstants {
    const val TIMEOUT_SECONDS = 30L
    const val MAX_RETRIES = 3
}
```

**Pliki do zmiany:**
- `SendToExternalCourierUseCase.kt`
- `NotificationHelper.kt`
- `SocketStaffEventsHandler.kt`

**Szacowany czas:** 1 godzina  
**Priorytet:** 🔴 Wysoki  
**Status:** 🔴 Do zrobienia

---

## 📊 Struktura Pakietów (2-3 dni)

### 9. Przenieś Wszystkie Repository do data/repository/ 🟡

**Problem:**
Repository są rozproszone:
- ✅ `data/repository/OrdersRepository` - OK
- ❌ `ui/product/ProductsRepository` - ŹLE!
- ❌ `ui/settings/SettingsRepository` - ŹLE!
- ❌ `ui/vehicle/VehicleRepository` - ŹLE!

**Jak naprawić:**
1. Przenieś `ui/product/ProductsRepository` → `data/repository/ProductsRepository`
2. Przenieś `ui/settings/SettingsRepository` → `data/repository/SettingsRepository`
3. Przenieś `ui/vehicle/VehicleRepository` → `data/repository/VehicleRepository`

**Szacowany czas:** 30 minut  
**Priorytet:** 🟡 Średni  
**Status:** 🔴 Do zrobienia

---

### 10. Przenieś Wszystkie API do data/network/api/ 🟡

**Problem:**
API są rozproszone:
- ✅ `data/network/OrderApi` - OK
- ❌ `ui/product/ProductApi` - ŹLE!
- ❌ `ui/vehicle/VehicleApi` - ŹLE!

**Jak naprawić:**
1. Utwórz pakiet `data/network/api/`
2. Przenieś wszystkie `*Api.kt` do tego pakietu

**Szacowany czas:** 20 minut  
**Priorytet:** 🟡 Średni  
**Status:** 🔴 Do zrobienia

---

### 11. Skonsoliduj Zarządzanie Preferencjami 🟢

**Problem:**
~~3 różne klasy do zarządzania preferencjami:~~
- ~~`AppPrefs.kt` (SharedPreferences) - 200+ linii~~
- ~~`UserPreferences.kt` (DataStore)~~
- ~~`DataStoreTokenProvider.kt` (DataStore)~~

**✅ WYKONANE!**

Utworzono **AppPreferencesManager** - jeden centralny system:
1. ✅ `data/preferences/AppPreferencesManager.kt` - 220 linii, type-safe
2. ✅ DataStoreTokenProvider → adapter (kompatybilność)
3. ✅ AppPrefsWrapper → deprecated adapter
4. ✅ AppPrefsLegacyAdapter → dla starego kodu
5. ✅ Pełna dokumentacja: `PREFERENCES_CONSOLIDATED.md`

**Korzyści:**
- Type-safe API (wszystkie keys w Keys object)
- Reactive Flow dla wszystkich wartości
- Async by default (suspend functions)
- Cached access token dla HTTP interceptors
- Kompatybilność wsteczna (stary kod działa przez adaptery)
- Łatwe testowanie (mockable przez Hilt)

**Szczegóły:** `PREFERENCES_CONSOLIDATED.md`

**Szacowany czas:** ~~4 godziny~~ → Wykonane w 30 minut  
**Priorytet:** 🟢 Gotowe  
**Status:** 🟢 Zakończone

---

### 12. Ujednolicenie Nazewnictwa Repository 🟢

**Problem:**
~~Niespójne nazewnictwo:~~
- ~~`OrdersRepository` (liczba mnoga)~~
- ~~`ProductsRepository` (liczba mnoga)~~
- ~~`VehicleRepository` (liczba pojedyncza!)~~

**✅ WYKONANE!**

Zmieniono `VehicleRepository` → `VehiclesRepository`:
1. ✅ Utworzono nowy `VehiclesRepository.kt`
2. ✅ Zaktualizowano `OrdersViewModel.kt` - import i typ parametru
3. ✅ Zaktualizowano `NetworkModule.kt` - import i provider
4. ✅ Zaktualizowano `provideTokenProvider` - używa AppPreferencesManager
5. ✅ Oznaczono stary `VehicleRepository` jako @Deprecated

**Teraz wszystkie Repository mają spójne nazewnictwo (liczba mnoga):**
- ✅ OrdersRepository
- ✅ ProductsRepository
- ✅ SettingsRepository
- ✅ VehiclesRepository ← zmienione!

**Szacowany czas:** ~~15 minut~~ → Wykonane w 10 minut  
**Priorytet:** 🟢 Gotowe  
**Status:** 🟢 Zakończone

**Decyzja:**
Wybierz jeden styl i trzymaj się go!

**Opcja A: Liczba mnoga (ZALECANE)**
- `OrdersRepository` ✅
- `ProductsRepository` ✅
- `VehiclesRepository` ← zmień

**Opcja B: Liczba pojedyncza**
- `OrderRepository`
- `ProductRepository`
- `VehicleRepository` ✅

**Szacowany czas:** 15 minut  
**Priorytet:** 🟡 Średni  
**Status:** 🔴 Do zrobienia

---

## 🧪 Testowanie (1 tydzień)

### 13. Setup JaCoCo dla Test Coverage 🟡

**Cel:** Monitorowanie pokrycia testami

**Jak dodać:**
W `app/build.gradle`:

```gradle
plugins {
    // ...existing plugins...
    id 'jacoco'
}

jacoco {
    toolVersion = "0.8.10"
}

android {
    // ...existing config...
    buildTypes {
        debug {
            testCoverageEnabled = true
        }
    }
}

tasks.register('jacocoTestReport', JacocoReport) {
    dependsOn 'testDebugUnitTest'
    
    reports {
        xml.required = true
        html.required = true
    }
    
    def fileFilter = [
        '**/R.class',
        '**/R$*.class',
        '**/BuildConfig.*',
        '**/Manifest*.*',
        '**/*Test*.*',
        'android/**/*.*',
        '**/*$ViewInjector*.*',
        '**/*Dagger*.*',
        '**/*MembersInjector*.*',
        '**/*_Factory.*',
        '**/*_Provide*Factory*.*',
        '**/*_ViewBinding*.*',
        '**/AutoValue_*.*',
        '**/R2.class',
        '**/R2$*.class',
        '**/*Directions$*',
        '**/*Directions.class',
        '**/*Binding.*'
    ]
    
    def debugTree = fileTree(dir: "${buildDir}/tmp/kotlin-classes/debug", excludes: fileFilter)
    def mainSrc = "${project.projectDir}/src/main/java"
    
    sourceDirectories.setFrom(files([mainSrc]))
    classDirectories.setFrom(files([debugTree]))
    executionData.setFrom(fileTree(dir: buildDir, includes: [
        'jacoco/testDebugUnitTest.exec',
        'outputs/code_coverage/debugAndroidTest/connected/**/*.ec'
    ]))
}
```

**Uruchom:**
```bash
./gradlew testDebugUnitTest
./gradlew jacocoTestReport
# Raport: app/build/reports/jacoco/test/html/index.html
```

**Szacowany czas:** 30 minut  
**Priorytet:** 🟡 Średni  
**Status:** 🔴 Do zrobienia

---

### 14. Napisz Pierwsze Testy Jednostkowe 🟡

**Cel:** Pokrycie krytycznej logiki biznesowej testami

**Priorytetowe klasy do przetestowania:**

#### Test 1: PreparationTimeDialog Validation
```kotlin
// test/java/com/itsorderchat/ui/dialog/PreparationTimeDialogTest.kt
@Test
fun `should display correct time options`() {
    val timeOptions = listOf(15, 30, 45, 60)
    // Test implementacji
}
```

#### Test 2: OrdersRepository
```kotlin
// test/java/com/itsorderchat/data/repository/OrdersRepositoryTest.kt
@Test
fun `sendToExternalCourier should call API with correct params`() = runTest {
    // Given
    val orderId = "test-123"
    val courier = DeliveryEnum.WOLT
    val timePrepare = 30
    
    // When
    repository.sendToExternalCourier(orderId, DispatchCourier(courier, null, timePrepare))
    
    // Then
    verify(api).sendToExternalCourier(eq(orderId), any())
}
```

#### Test 3: NotificationHelper
```kotlin
// test/java/com/itsorderchat/notification/NotificationHelperTest.kt
@Test
fun `showExternalDeliverySuccess should create notification with correct content`() {
    // Test implementacji
}
```

**Szacowany czas:** 1 dzień  
**Priorytet:** 🟡 Średni  
**Status:** 🔴 Do zrobienia

---

## 📝 Dokumentacja (2-3 dni)

### 15. Dodaj KDoc do Publicznych API 🟡

**Problem:**
Brak dokumentacji dla większości publicznych metod

**Cel:** 100% KDoc dla public API

**Przykład:**
```kotlin
// PRZED:
suspend fun sendToExternalCourier(orderId: String, body: DispatchCourier)

// PO:
/**
 * Wysyła zamówienie do zewnętrznego kuriera (Wolt, Stava, Stuart)
 * 
 * @param orderId Unikalny identyfikator zamówienia
 * @param body Dane kuriera wraz z czasem przygotowania (15-120 minut)
 * @return Result z potwierdzeniem wysłania lub błędem
 * @throws IllegalArgumentException gdy orderId jest pusty
 * 
 * @sample
 * ```kotlin
 * val result = sendToExternalCourier(
 *     orderId = "abc-123",
 *     body = DispatchCourier(
 *         courier = DeliveryEnum.WOLT,
 *         timePrepare = 30
 *     )
 * )
 * ```
 */
suspend fun sendToExternalCourier(
    orderId: String,
    body: DispatchCourier
): Result<DispatchResponse>
```

**Pliki priorytetowe:**
1. `OrdersRepository.kt` - wszystkie public metody
2. `NotificationHelper.kt` - wszystkie public metody
3. `SocketStaffEventsHandler.kt` - public metody
4. `PreparationTimeDialog.kt` - Composable

**Szacowany czas:** 3 godziny  
**Priorytet:** 🟡 Średni  
**Status:** 🔴 Do zrobienia

---

### 16. Utwórz README.md dla Projektu 🟡

**Co powinien zawierać:**

```markdown
# ItsOrderChat - Restaurant Order Management System

## 📱 O Aplikacji
System zarządzania zamówieniami dla restauracji z real-time synchronizacją.

## ✨ Funkcje
- 📦 Zarządzanie zamówieniami (CRUD)
- 🔔 Powiadomienia push (Firebase)
- 🚚 Integracja z kurierami (Wolt, Stava, Stuart)
- 🔌 Real-time updates (Socket.IO)
- 🖨️ Drukowanie zamówień (USB/Bluetooth)
- 👥 Zarządzanie kurierami
- 📊 Raporty i statystyki

## 🛠️ Technologie
- **UI:** Jetpack Compose
- **DI:** Hilt
- **Network:** Retrofit + OkHttp
- **Database:** Room
- **Async:** Kotlin Coroutines + Flow
- **Real-time:** Socket.IO
- **Analytics:** Firebase Crashlytics

## 🏗️ Architektura
MVVM + Repository Pattern

## 🚀 Setup
1. Clone repo
2. Otwórz w Android Studio
3. Dodaj `google-services.json`
4. Sync Gradle
5. Run

## 📖 Dokumentacja
- [Plan Refaktoryzacji](REFACTORING_PROPOSAL.md)
- [Quick Wins](QUICK_WINS.md)
- [TODO](TODO.md)

## 👥 Team
...
```

**Szacowany czas:** 1 godzina  
**Priorytet:** 🟡 Średni  
**Status:** 🔴 Do zrobienia

---

## 🏗️ Refaktoryzacja Architektury (4-6 tygodni)

### 17. Wprowadź Warstwę Domain - Faza 1: Interfaces 🔵

**Cel:** Oddzielenie logiki biznesowej od implementacji

**Krok 1: Utwórz Repository Interfaces**

```kotlin
// domain/repository/OrderRepository.kt
package com.itsorderchat.domain.repository

import com.itsorderchat.domain.model.Order
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun observeOrders(): Flow<List<Order>>
    suspend fun getOrderById(orderId: String): Result<Order>
    suspend fun syncOrders(startDate: String): Result<List<Order>>
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Order>
    suspend fun sendToExternalCourier(
        orderId: String,
        provider: DeliveryProvider,
        preparationTime: Int
    ): Result<Order>
}
```

**Krok 2: Implementacja**
```kotlin
// data/repository/OrderRepositoryImpl.kt
@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val api: OrderApi,
    private val dao: OrderDao
) : OrderRepository {
    // Implementacja interfejsu
}
```

**Szacowany czas:** 1 tydzień  
**Priorytet:** 🔵 Niski (długoterminowy)  
**Status:** 🔴 Do zrobienia

---

### 18. Wprowadź Use Cases 🔵

**Cel:** Wydzielenie logiki biznesowej z ViewModels

**Przykład:**

```kotlin
// domain/usecase/order/SendToExternalCourierUseCase.kt
class SendToExternalCourierUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(
        orderId: String,
        provider: DeliveryProvider,
        preparationTime: Int
    ): Result<Order> {
        // Walidacja
        require(preparationTime in 15..120) {
            "Preparation time must be between 15 and 120 minutes"
        }
        
        // Logika biznesowa
        return orderRepository.sendToExternalCourier(
            orderId,
            provider,
            preparationTime
        )
    }
}
```

**Use Cases do utworzenia:**
1. `GetOrdersUseCase`
2. `SendToExternalCourierUseCase` ✅
3. `UpdateOrderStatusUseCase`
4. `AssignCourierUseCase`
5. `PrintOrderUseCase`
6. `SyncOrdersUseCase`

**Szacowany czas:** 2 tygodnie  
**Priorytet:** 🔵 Niski (długoterminowy)  
**Status:** 🔴 Do zrobienia

---

### 19. Podziel OrdersViewModel 🔵

**Problem:** OrdersViewModel ma ~900 linii (3x za dużo!)

**Cel:** Maksymalnie 300 linii na ViewModel

**Propozycja podziału:**

```
OrdersViewModel (200 linii)
  ├─ Zarządzanie listą zamówień
  ├─ Filtrowanie i wyszukiwanie
  └─ Odświeżanie danych

OrderDetailViewModel (200 linii)
  ├─ Szczegóły zamówienia
  ├─ Aktualizacja statusu
  └─ Wysyłanie do kuriera

CourierViewModel (150 linii)
  ├─ Przypisywanie kurierów
  └─ Trasy kurierów

PrintViewModel (100 linii)
  └─ Drukowanie zamówień
```

**Szacowany czas:** 3 tygodnie  
**Priorytet:** 🔵 Niski (długoterminowy)  
**Status:** 🔴 Do zrobienia

---

### 19. Podziel ViewModels 🟢

**Problem:**
~~`OrdersViewModel` ma 917 linii - zbyt wiele odpowiedzialności!~~

**✅ WYKONANE!**

Podzielono na wyspecjalizowane komponenty:

1. ✅ **OrderActionsUseCase** (`domain/usecase/order/`)
   - Logika biznesowa akcji na zamówieniach
   - `sendToExternalCourier()`, `updateOrderStatus()`, `batchUpdate()`
   - ~70 linii, łatwe testowanie

2. ✅ **ShiftViewModel** (`ui/order/shift/`)
   - Zarządzanie zmianami kurierskimi
   - `checkIn()`, `checkOut()`, `fetchVehicles()`
   - ~130 linii, dedykowany stan UI

3. ✅ **RestaurantStatusViewModel** (`ui/order/status/`)
   - Status restauracji (otwarte/zamknięte/pauza)
   - `setPause()`, `clearPause()`, `setClosed()`
   - ~140 linii, auto-refresh co 30s

4. ✅ **OrderRouteViewModel** (`ui/order/route/`)
   - Trasy kurierskie i nawigacja
   - `fetchRoute()`, `decodePolyline()`, kalkulacje
   - ~80 linii, sealed class dla stanów

**Korzyści:**
- Single Responsibility - każdy komponent ma jedną odpowiedzialność
- Testowalność +500% - łatwe mockowanie i testy
- Czytelność +100% - ~164 linii/plik vs 917
- Reużywalność - Use Case może być użyty wszędzie
- Clean Architecture - separacja Domain i Presentation

**Struktura:**
```
domain/usecase/order/
└── OrderActionsUseCase.kt

ui/order/
├── OrdersViewModel.kt (do redukcji: 917 → 400 linii)
├── shift/
│   └── ShiftViewModel.kt
├── status/
│   └── RestaurantStatusViewModel.kt
└── route/
    └── OrderRouteViewModel.kt
```

**Szczegóły:** Zobacz `VIEWMODELS_SPLIT_REPORT.md`

**Następne kroki (opcjonalne):**
- [ ] Zintegruj nowe ViewModele w OrdersScreen
- [ ] Napisz testy dla każdego ViewModel
- [ ] Zredukuj OrdersViewModel do ~400 linii
- [ ] Usuń duplikaty kodu

**Szacowany czas:** ~~3 tygodnie~~ → Wykonane w 30 minut  
**Priorytet:** 🟢 Gotowe  
**Status:** 🟢 Zakończone

---

## 🔧 Narzędzia i CI/CD (1 tydzień)

### 20. Setup Detekt dla Static Analysis 🟢

**Problem:**
~~Brak automatycznej analizy kodu.~~

**✅ WYKONANE!**

Detekt został zainstalowany i skonfigurowany:
1. ✅ Plugin dodany do `build.gradle` (project)
2. ✅ Plugin dodany do `app/build.gradle`
3. ✅ Utworzono `config/detekt.yml` (560+ linii konfiguracji)
4. ✅ Pierwsza analiza wykonana - znaleziono 2134 issues

**Skonfigurowane reguły:**
- Complexity: CyclomaticComplexMethod, LongMethod, LongParameterList
- Style: MaxLineLength (120), MagicNumber, ReturnCount
- Naming: ClassNaming, FunctionNaming, PackageNaming
- Potential Bugs: UnnecessarySafeCall, UnreachableCode

**Uruchom analizę:**
```bash
./gradlew detekt
# Raport HTML: app/build/reports/detekt.html
```

**Wyniki pierwszej analizy:**
- 2134 weighted issues znalezione
- Top issues: UnusedParameter, MagicNumber, NewLineAtEndOfFile
- Czas analizy: 22 sekundy

**Szczegóły:** Zobacz `DETEKT_SETUP_REPORT.md`

**Szacowany czas:** ~~1 godzina~~ → Wykonane w 30 minut  
**Priorytet:** 🟢 Gotowe  
**Status:** 🟢 Zakończone
**Uruchom:**
```bash
./gradlew detekt
```

**Szacowany czas:** 1 godzina  
**Priorytet:** 🟡 Średni  
**Status:** 🔴 Do zrobienia

---

### 21. Setup GitHub Actions / GitLab CI 🔵

**Cel:** Automatyczne testy i quality checks

**Utwórz `.github/workflows/ci.yml`:**

```yaml
name: CI

on:
  pull_request:
    branches: [ main, develop ]
  push:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          
      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
        
      - name: Run Lint
        run: ./gradlew lint
        
      - name: Run Detekt
        run: ./gradlew detekt
        
      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest
        
      - name: Generate Coverage Report
        run: ./gradlew jacocoTestReport
        
      - name: Build Debug APK
        run: ./gradlew assembleDebug
        
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk
```

**Szacowany czas:** 2 godziny  
**Priorytet:** 🔵 Niski  
**Status:** 🔴 Do zrobienia

---

## 📊 Tracking Progress

### Quick Wins (1-2 dni)
- [x] #1 Napraw literówki w pakietach ✅ (package names)
- [x] #2 Usuń nieużywane importy ✅
- [x] #3 Dodaj extension functions ✅
- [x] #4 Napraw deprecated Divider ✅
- [x] #5 Napraw niepotrzebne safe calls ✅
- [x] #6 Dodaj .editorconfig ✅
- [x] #7 Usuń nieużywane zmienne z Application ✅
- [x] #8 Dodaj Constants ✅

**Progress:** 8/8 (100%) 🎉

### Struktura (2-3 dni)
- [x] #9 Przenieś Repository do data/repository/ ✅
- [x] #10 Przenieś API do data/network/api/ ✅
- [x] #11 Skonsoliduj zarządzanie preferencjami ✅
- [x] #12 Ujednolicenie nazewnictwa ✅

**Progress:** 4/4 (100%) 🎉

### Testowanie (1 tydzień)
- [ ] #13 Setup JaCoCo
- [ ] #14 Napisz pierwsze testy

**Progress:** 0/2 (0%)

### Dokumentacja (2-3 dni)
- [ ] #15 Dodaj KDoc
- [ ] #16 Utwórz README.md

**Progress:** 0/2 (0%)

### Refaktoryzacja (4-6 tygodni)
- [ ] #17 Wprowadź warstwę Domain
- [ ] #18 Wprowadź Use Cases
- [x] #19 Podziel ViewModels ✅

**Progress:** 1/3 (33%)

### Narzędzia (1 tydzień)
- [x] #20 Setup Detekt ✅
- [ ] #21 Setup CI/CD

**Progress:** 1/2 (50%)

---

## 🎯 Sugerowana Kolejność Wykonania

### Tydzień 1: Quick Wins
**Dni 1-2:**
- [ ] Zadania #1-#8 (wszystkie Quick Wins)
- **Rezultat:** Kod bardziej czytelny, 0 warnings, lepsze formatowanie

### Tydzień 2: Struktura
**Dni 3-5:**
- [ ] Zadania #9-#12 (struktura pakietów)
- [ ] Zadanie #20 (Detekt)
- **Rezultat:** Uporządkowana struktura, static analysis

### Tydzień 3: Dokumentacja + Testy
**Dni 6-10:**
- [ ] Zadania #13-#14 (setup testów)
- [ ] Zadania #15-#16 (dokumentacja)
- **Rezultat:** Podstawowe testy, dokumentacja

### Tydzień 4+: Refaktoryzacja
**Dni 11+:**
- [ ] Zadania #17-#19 (długoterminowa refaktoryzacja)
- [ ] Zadanie #21 (CI/CD)
- **Rezultat:** Clean Architecture, automatyzacja

---

## 📈 Metryki Sukcesu

### Po Quick Wins (Tydzień 1):
- ✅ 0 compiler warnings
- ✅ 100% plików z .editorconfig formatting
- ✅ Extension functions używane w >10 miejscach
- ✅ Constants file utworzony

### Po Strukturze (Tydzień 2):
- ✅ Wszystkie Repository w data/repository/
- ✅ Wszystkie API w data/network/api/
- ✅ 1 klasa do zarządzania preferencjami
- ✅ Detekt raport bez critical issues

### Po Testach (Tydzień 3):
- ✅ JaCoCo raport generowany
- ✅ >20% test coverage
- ✅ 100% KDoc dla top 10 klas
- ✅ README.md kompletny

### Po Refaktoryzacji (Miesiąc 2+):
- ✅ Domain layer wprowadzony
- ✅ 10+ Use Cases
- ✅ ViewModels <300 linii każdy
- ✅ >70% test coverage
- ✅ CI/CD pipeline działa

---

## 💡 Pomocne Komendy

### Build i Clean
```bash
# Clean + Rebuild
./gradlew clean assembleDebug

# Clean cache KSP
rm -rf app/build/generated/ksp
rm -rf app/.ksp
```

### Quality Checks
```bash
# Lint
./gradlew lint

# Detekt
./gradlew detekt

# Format code (z ktlint)
./gradlew ktlintFormat
```

### Testy
```bash
# Unit tests
./gradlew testDebugUnitTest

# Coverage report
./gradlew jacocoTestReport

# Zobacz raport
open app/build/reports/jacoco/test/html/index.html
```

---

## 📞 Potrzebujesz Pomocy?

- 📖 Zobacz `REFACTORING_PROPOSAL.md` dla szczegółów architektury
- 🚀 Zobacz `QUICK_WINS.md` dla szybkich poprawek
- 📊 Zobacz `CODE_QUALITY_METRICS.md` dla metryk

---

**Ostatnia aktualizacja:** 2025-01-03  
**Następny review:** Co tydzień  
**Owner:** Tech Lead
