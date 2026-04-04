# 🏗️ Przewodnik po Strukturze Projektu - Jak Poprawić

**Data:** 2025-01-03  
**Cel:** Wizualizacja problemów i rozwiązań w strukturze projektu

---

## 📁 Obecna Struktura vs Docelowa Struktura

### ❌ PROBLEM: Obecna Struktura

```
com.itsorderchat/
├── 📱 Główne pliki (PROBLEM: za dużo w root)
│   ├── ItsChat.kt
│   ├── LoginActivity.kt
│   ├── MainActivity.kt
│   ├── AppPrefsWrapper.kt
│   ├── RegisterDevice.kt
│   └── ...8 innych plików
│
├── 📦 data/
│   ├── entity/
│   │   ├── datebase/  ❌ LITERÓWKA!
│   │   └── ...
│   ├── model/  ⚠️ Mieszane DTO i Domain Models
│   ├── network/
│   │   ├── preferences/  ⚠️ Dziwne miejsce
│   │   └── ...
│   ├── repository/  ✅ OK (ale niepełne)
│   └── util/  ⚠️ Duplikacja z util/ w root
│
├── 🎨 ui/
│   ├── product/
│   │   ├── ProductsRepository.kt  ❌ Repository w UI!
│   │   ├── ProductApi.kt  ❌ API w UI!
│   │   └── ...
│   ├── settings/
│   │   ├── SettingsRepository.kt  ❌ Repository w UI!
│   │   └── ...
│   ├── vehicle/
│   │   ├── VehicleRepository.kt  ❌ Repository w UI!
│   │   ├── VehicleApi.kt  ❌ API w UI!
│   │   └── ...
│   ├── utili/  ❌ LITERÓWKA! (powinno być util)
│   └── theme/
│       ├── MyForegroundService.kt  ❌ Service w UI!
│       └── ...
│
├── 🔌 service/  ✅ OK
├── 🔔 notification/  ✅ OK
└── 🛠️ util/  ⚠️ Duplikacja z data/util

```

### ✅ ROZWIĄZANIE: Docelowa Struktura

```
com.itsorderchat/
├── 📱 app/
│   ├── ItsOrderChatApp.kt  (zmieniona nazwa z ItsChat)
│   └── di/  (wszystkie moduły DI)
│       ├── AppModule.kt
│       ├── NetworkModule.kt
│       ├── DatabaseModule.kt
│       └── RepositoryModule.kt
│
├── 🧠 domain/  ⭐ NOWE!
│   ├── model/  (czyste domain models)
│   │   ├── Order.kt
│   │   ├── Product.kt
│   │   └── User.kt
│   ├── repository/  (interfejsy)
│   │   ├── OrderRepository.kt
│   │   ├── ProductRepository.kt
│   │   └── AuthRepository.kt
│   └── usecase/  (logika biznesowa)
│       ├── order/
│       │   ├── GetOrdersUseCase.kt
│       │   ├── SendToExternalCourierUseCase.kt
│       │   └── UpdateOrderStatusUseCase.kt
│       └── auth/
│           ├── LoginUseCase.kt
│           └── LogoutUseCase.kt
│
├── 📦 data/
│   ├── local/  ⭐ ZMIENIONE
│   │   ├── database/  (naprawiono literówkę)
│   │   │   ├── AppDatabase.kt
│   │   │   └── dao/
│   │   ├── entity/  (Room entities)
│   │   └── preferences/  (przeniesione z network)
│   │       └── AppPreferencesManager.kt  (skonsolidowane)
│   │
│   ├── remote/  ⭐ ZMIENIONE
│   │   ├── api/  (wszystkie API tutaj!)
│   │   │   ├── OrderApi.kt
│   │   │   ├── ProductApi.kt  ← przeniesione z ui/
│   │   │   ├── VehicleApi.kt  ← przeniesione z ui/
│   │   │   ├── AuthApi.kt
│   │   │   └── SettingsApi.kt
│   │   ├── dto/  (network DTOs)
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── interceptor/
│   │   └── socket/
│   │
│   ├── repository/  (wszystkie implementacje!)
│   │   ├── OrderRepositoryImpl.kt
│   │   ├── ProductRepositoryImpl.kt  ← przeniesione z ui/
│   │   ├── VehicleRepositoryImpl.kt  ← przeniesione z ui/
│   │   └── AuthRepositoryImpl.kt
│   │
│   └── mapper/  ⭐ NOWE
│       ├── OrderMapper.kt
│       └── ProductMapper.kt
│
├── 🎨 presentation/  (zmieniona nazwa z ui)
│   ├── navigation/
│   ├── theme/  (tylko theme!)
│   ├── components/  (reusable components)
│   └── feature/  (organizacja po features)
│       ├── auth/
│       │   └── login/
│       ├── orders/
│       │   ├── list/
│       │   └── detail/
│       ├── products/
│       └── settings/
│
├── 🏗️ infrastructure/  ⭐ NOWE
│   ├── service/
│   │   ├── SocketService.kt
│   │   └── OrderAlarmService.kt
│   ├── notification/
│   └── printer/
│
└── 🛠️ util/  (skonsolidowane)
    ├── extensions/
    │   ├── StringExtensions.kt
    │   └── FlowExtensions.kt
    ├── Constants.kt
    └── Logger.kt
```

---

## 🎯 Szczegółowy Plan Migracji

### Krok 1: Napraw Podstawowe Problemy (1 dzień)

#### A. Napraw Literówki
```bash
# W Android Studio:
1. Kliknij prawym na pakiet "utili"
2. Refactor → Rename → "util"
3. Kliknij prawym na pakiet "datebase"  
4. Refactor → Rename → "database"
```

**Rezultat:**
```
✅ ui/util/  (było: ui/utili/)
✅ data/entity/database/  (było: data/entity/datebase/)
```

---

#### B. Przenieś Repository (30 min)

**Zadanie:** Przenieś wszystkie Repository do `data/repository/`

```
PRZED:
ui/product/ProductsRepository.kt  ❌
ui/settings/SettingsRepository.kt  ❌
ui/vehicle/VehicleRepository.kt  ❌

PO:
data/repository/ProductsRepository.kt  ✅
data/repository/SettingsRepository.kt  ✅
data/repository/VehicleRepository.kt  ✅
```

**Jak to zrobić:**
1. Utwórz folder: `data/repository/`
2. Przeciągnij (drag&drop) każdy `*Repository.kt` do nowego folderu
3. Android Studio automatycznie zaktualizuje importy
4. Usuń puste foldery

---

#### C. Przenieś API (20 min)

**Zadanie:** Przenieś wszystkie API do `data/remote/api/`

```
PRZED:
ui/product/ProductApi.kt  ❌
ui/vehicle/VehicleApi.kt  ❌

PO:
data/remote/api/ProductApi.kt  ✅
data/remote/api/VehicleApi.kt  ✅
```

**Jak to zrobić:**
1. Utwórz folder: `data/remote/api/`
2. Przenieś wszystkie `*Api.kt`
3. Przenieś też istniejące API z `data/network/`:
   - `OrderApi.kt`
   - `AuthApi.kt`
   - `SettingsApi.kt`

---

### Krok 2: Uporządkuj Preferences (2 godziny)

**Problem:** 3 klasy robią to samo

```
PRZED:
AppPrefs.kt (SharedPreferences)
UserPreferences.kt (DataStore)
DataStoreTokenProvider.kt (DataStore)

PO:
data/local/preferences/AppPreferencesManager.kt  ⭐ JEDNA KLASA
```

**Jak migrować:**

1. **Utwórz nową klasę:**
```kotlin
// data/local/preferences/AppPreferencesManager.kt
@Singleton
class AppPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore by preferencesDataStore("app_prefs")
    
    // Wszystkie preferencje tutaj
}
```

2. **Migruj stopniowo:**
   - Dzień 1: Utwórz nową klasę
   - Dzień 2-3: Migruj 10 metod dziennie
   - Dzień 4: Usuń stare klasy

---

### Krok 3: Dodaj Extension Functions (1 godzina)

**Utwórz:** `util/extensions/StringExtensions.kt`

```kotlin
package com.itsorderchat.util.extensions

fun String?.orDash(): String = 
    if (this.isNullOrBlank()) "—" else this

fun String.maskPhone(): String {
    if (length < 4) return this
    return "*".repeat(length - 4) + takeLast(4)
}

fun String.isValidEmail(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
```

**Zastosuj w kodzie:**

```kotlin
// PRZED (20 razy w kodzie):
Text(order.consumer.phone ?: stringResource(R.string.common_dash))

// PO (z extension):
import com.itsorderchat.util.extensions.orDash
Text(order.consumer.phone.orDash())
```

**Pliki do zmiany:**
- `AcceptOrderSheetContent.kt` (~20 miejsc)
- `OrderCard.kt` (~10 miejsc)
- `OrderDetailScreen.kt` (~15 miejsc)

**Narzędzie:** Find & Replace w Android Studio
```
Find: ?: stringResource(R.string.common_dash)
Replace: .orDash()
```

---

### Krok 4: Dodaj Constants (30 min)

**Utwórz:** `util/Constants.kt`

```kotlin
package com.itsorderchat.util

object OrderConstants {
    const val MIN_PREPARATION_TIME = 15
    const val MAX_PREPARATION_TIME = 120
    const val DEFAULT_PREPARATION_TIME = 30
    
    val PREPARATION_TIME_OPTIONS = listOf(15, 30, 45, 60)
}

object NotificationIds {
    const val WS_DISCONNECT = 1997
    const val ORDER_ALARM = 2000
    const val ROUTE_UPDATE = 2
    const val EXTERNAL_DELIVERY_SUCCESS = 3000
}

object NetworkConstants {
    const val TIMEOUT_SECONDS = 30L
    const val MAX_RETRIES = 3
    const val CACHE_SIZE = 10L * 1024 * 1024 // 10 MB
}

object DatabaseConstants {
    const val DATABASE_NAME = "itsorderchat.db"
    const val DATABASE_VERSION = 1
}
```

**Zastosuj:**

```kotlin
// PRZED:
if (preparationTime in 15..120) { }

// PO:
import com.itsorderchat.util.OrderConstants
if (preparationTime in OrderConstants.MIN_PREPARATION_TIME..OrderConstants.MAX_PREPARATION_TIME) { }
```

---

## 📊 Wizualizacja Problemów

### Problem 1: Repository w Złych Miejscach

```
❌ ŹLE:
ui/
├── product/
│   ├── ProductScreen.kt  ✅ OK (UI)
│   └── ProductsRepository.kt  ❌ NIE! (to nie UI)

✅ DOBRZE:
ui/
└── product/
    └── ProductScreen.kt  ✅

data/
└── repository/
    └── ProductsRepository.kt  ✅
```

---

### Problem 2: Duplikacja util/

```
❌ ŹLE:
com.itsorderchat/
├── util/
│   ├── AppPrefs.kt
│   └── FileLoggingTree.kt
└── data/
    └── util/
        ├── FlexibleDoubleAdapter.kt
        └── OrderUtils.kt

✅ DOBRZE:
com.itsorderchat/
└── util/
    ├── AppPrefs.kt
    ├── FileLoggingTree.kt
    ├── FlexibleDoubleAdapter.kt
    ├── OrderUtils.kt
    └── extensions/
        ├── StringExtensions.kt
        └── FlowExtensions.kt
```

---

### Problem 3: Zbyt Duże ViewModels

```
❌ ŹLE:
OrdersViewModel.kt  (900 linii!)
├── Lista zamówień (150 linii)
├── Szczegóły zamówienia (200 linii)
├── Drukowanie (100 linii)
├── Kurier (150 linii)
├── Status restauracji (100 linii)
├── Synchronizacja (100 linii)
└── Nawigacja (100 linii)

✅ DOBRZE:
OrdersListViewModel.kt (200 linii)
├── Lista zamówień
├── Filtrowanie
└── Odświeżanie

OrderDetailViewModel.kt (200 linii)
├── Szczegóły
├── Aktualizacja statusu
└── Wysyłanie do kuriera

CourierViewModel.kt (150 linii)
├── Przypisywanie
└── Trasy

PrintViewModel.kt (100 linii)
└── Drukowanie
```

---

## 🎓 Najlepsze Praktyki

### 1. Nazewnictwo Pakietów

```
✅ DOBRZE:
com.itsorderchat.data.repository
com.itsorderchat.data.remote.api
com.itsorderchat.presentation.feature.orders

❌ ŹLE:
com.itsorderchat.data.repo  (skrót)
com.itsorderchat.ui.orders  (niejasne)
com.itsorderchat.helpers  (zbyt ogólne)
```

---

### 2. Nazewnictwo Plików

```
✅ DOBRZE:
OrdersViewModel.kt  (liczba mnoga dla kolekcji)
OrderRepository.kt  (liczba pojedyncza)
SendToExternalCourierUseCase.kt  (czasownik + UseCase)
StringExtensions.kt  (Extensions suffix)

❌ ŹLE:
OrdersVM.kt  (skrót)
OrderRepo.kt  (skrót)
SendToCourier.kt  (niejasne co to)
StringUtils.kt  (Utils to anti-pattern)
```

---

### 3. Organizacja Klas

```
✅ DOBRZE:
class OrdersViewModel @Inject constructor(
    private val getOrdersUseCase: GetOrdersUseCase,
    private val updateOrderUseCase: UpdateOrderStatusUseCase
) : ViewModel() {
    
    // 1. Properties (StateFlow, etc.)
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // 2. Init block
    init {
        loadOrders()
    }
    
    // 3. Public methods (alfabetycznie)
    fun loadOrders() { }
    fun updateOrder() { }
    
    // 4. Private methods (alfabetycznie)
    private fun handleError() { }
    private fun mapToUiState() { }
    
    // 5. Companion object
    companion object {
        private const val TAG = "OrdersViewModel"
    }
}
```

---

### 4. Imports Organizacja

```
✅ DOBRZE:
// 1. Android
import android.content.Context
import androidx.compose.runtime.*

// 2. Third-party
import com.google.gson.Gson
import javax.inject.Inject

// 3. Project
import com.itsorderchat.data.model.Order
import com.itsorderchat.util.extensions.orDash

❌ ŹLE:
// Wszystko pomieszane bez kolejności
```

---

## 🔍 Checklist Przed Commitem

### Każdy Plik Powinien:

- [ ] Mieć poprawną nazwę pakietu (bez literówek)
- [ ] Być w odpowiednim folderze (Repository w data/repository)
- [ ] Mieć zorganizowane importy (Optimize Imports)
- [ ] Nie mieć nieużywanych importów
- [ ] Mieć maksymalnie 300 linii (jeśli ViewModel)
- [ ] Używać extension functions zamiast duplikacji
- [ ] Używać Constants zamiast magic numbers
- [ ] Mieć KDoc dla public API
- [ ] Przechodzić Detekt bez critical issues
- [ ] Mieć testy (jeśli to logika biznesowa)

---

## 🚀 Quick Start - Co Zrobić Teraz?

### Dzień 1: Podstawy (2 godziny)
```bash
1. ✅ Napraw literówki (utili → util, datebase → database)
2. ✅ Usuń nieużywane importy (Optimize Imports)
3. ✅ Dodaj .editorconfig
4. ✅ Utwórz Constants.kt
```

### Dzień 2: Struktura (3 godziny)
```bash
5. ✅ Przenieś Repository do data/repository/
6. ✅ Przenieś API do data/remote/api/
7. ✅ Utwórz StringExtensions.kt
8. ✅ Zastosuj .orDash() w 5 plikach
```

### Dzień 3: Dokumentacja (2 godziny)
```bash
9. ✅ Dodaj KDoc do top 10 klas
10. ✅ Utwórz README.md
```

### Tydzień 2: Setup Narzędzi (1 dzień)
```bash
11. ✅ Setup Detekt
12. ✅ Setup JaCoCo
13. ✅ Napisz 5 pierwszych testów
```

---

## 📞 Potrzebujesz Pomocy?

### Dokumenty:
- 📋 `TODO.md` - Szczegółowa lista zadań
- 📚 `REFACTORING_PROPOSAL.md` - Pełny plan refaktoryzacji
- 🚀 `QUICK_WINS.md` - Szybkie poprawki
- 📊 `CODE_QUALITY_METRICS.md` - Metryki

### Polecenia:
```bash
# Sprawdź strukturę
tree app/src/main/java/com/itsorderchat -L 3

# Znajdź duplikacje
./gradlew detekt

# Zobacz coverage
./gradlew jacocoTestReport
```

---

**Ostatnia aktualizacja:** 2025-01-03  
**Następna aktualizacja:** Po Week 1 Quick Wins

