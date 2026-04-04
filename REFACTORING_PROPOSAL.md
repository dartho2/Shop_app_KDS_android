# 📋 Propozycja Refaktoryzacji Aplikacji ItsOrderChat

**Data analizy:** 2025-01-03  
**Wersja:** 1.0

---

## 🔍 Analiza Obecnej Struktury

### ✅ Mocne Strony

1. **Wykorzystanie nowoczesnych technologii:**
   - Jetpack Compose dla UI
   - Hilt dla Dependency Injection
   - Room dla lokalnej bazy danych
   - Retrofit + OkHttp dla komunikacji sieciowej
   - Kotlin Coroutines i Flow
   - Socket.IO dla real-time komunikacji

2. **Architektura warstwowa:**
   - Separacja data / domain / presentation
   - Repository pattern
   - MVVM pattern dla ViewModels

3. **Dodatkowe funkcjonalności:**
   - Firebase Crashlytics
   - Timber dla logowania
   - WorkManager dla zadań w tle

---

## ⚠️ Zidentyfikowane Problemy

### 1. **Niespójna Struktura Pakietów**

#### ❌ Obecna struktura:
```
com.itsorderchat/
├── data/
│   ├── model/          # Mieszane DTO i Domain Models
│   ├── entity/         # Room entities
│   ├── network/        # API + Interceptory + Preferences
│   ├── repository/
│   ├── socket/
│   └── util/
├── ui/
│   ├── product/        # Ma własne Repository i API (!)
│   ├── settings/       # Ma własne Repository (!)
│   ├── vehicle/        # Ma własne Repository i API (!)
│   ├── order/          # Tylko UI, Repository jest w data/
│   ├── theme/          # Zawiera Activity i Services (!)
│   └── utili/          # Literówka: "utili" zamiast "util"
├── service/            # Socket services
├── notification/
└── util/              # Duplikacja z data/util
```

**Problemy:**
- ❌ Repository i API są rozproszone (niektóre w `ui/`, inne w `data/`)
- ❌ Brak warstwy Domain (mieszanie DTO z Domain Models)
- ❌ `ui/theme/` zawiera nie tylko UI (Activity, Services)
- ❌ Duplikacja folderów `util`
- ❌ Literówka: `utili` zamiast `util`
- ❌ Pliki aplikacji na poziomie root (AppPrefs, LoginActivity, MainActivity)

---

### 2. **Problemy z Zarządzaniem Stanem**

#### ❌ Obecne:
```kotlin
// AppPrefs.kt - obiekt singleton z lateinit
@SuppressLint("StaticFieldLeak")
object AppPrefs {
    private lateinit var context: Context  // ⚠️ Wyciek pamięci!
    // Mieszane klucze dla SharedPreferences i DataStore
}
```

**Problemy:**
- ❌ Używanie `object` z `lateinit var context` = potencjalny wyciek pamięci
- ❌ Mieszanie SharedPreferences i DataStore w jednym miejscu
- ❌ Brak type-safe dostępu do preferencji
- ❌ Duplikacja: `AppPrefs` i `UserPreferences` i `DataStoreTokenProvider`

---

### 3. **Nadmierne Odpowiedzialności w ViewModels**

```kotlin
// OrdersViewModel.kt - ~900 linii kodu!
class OrdersViewModel @Inject constructor(
    private val repository: OrdersRepository,
    private val socketEventsRepo: SocketEventsRepository,
    private val printerService: PrinterService,
    private val openHoursRepository: OpenHoursRepository,
    // ... 10+ zależności
) : ViewModel() {
    // Obsługuje:
    // - Zarządzanie zamówieniami
    // - Drukowanie
    // - Socket events
    // - Alarmy
    // - Kurierów
    // - Status restauracji
    // - Synchronizację
    // - Nawigację
}
```

**Problemy:**
- ❌ Zbyt wiele odpowiedzialności (naruszenie Single Responsibility Principle)
- ❌ Trudne w testowaniu
- ❌ Trudne w utrzymaniu

---

### 4. **Mieszanie Logiki Biznesowej w Service**

```kotlin
// SocketStaffEventsHandler.kt
@Singleton
class SocketStaffEventsHandler @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val printerService: PrinterService,
    // ...
) {
    private fun handleNewOrProcessingOrder(args: Array<Any>) {
        // Logika biznesowa bezpośrednio w handlerze socketa
        if (isProcessing && AppPrefs.getAutoPrintEnabled()) {
            printerService.printOrder(order)  // ⚠️
        }
    }
}
```

**Problemy:**
- ❌ Logika biznesowa w warstwie infrastruktury
- ❌ Bezpośrednie wywołania UI (NotificationHelper, startActivity)
- ❌ Trudne w testowaniu

---

### 5. **Brak Use Cases / Interactors**

Brak dedykowanej warstwy dla logiki biznesowej:
- Cała logika jest w ViewModels lub Repository
- Trudne do reużycia między różnymi ekranami
- Trudne w testowaniu jednostkowym

---

### 6. **Niespójne Nazewnictwo**

- ✅ `OrdersRepository` (liczba mnoga)
- ❌ `ProductsRepository` (liczba mnoga)
- ❌ `SettingsRepository` (liczba mnoga)
- ❌ `VehicleRepository` (liczba pojedyncza!)
- ❌ `OpenHoursRepository` (liczba mnoga!)

**Pakiety:**
- ❌ `utili` zamiast `util`
- ❌ `datebase` zamiast `database`

---

### 7. **Problemy z Dependency Injection**

```kotlin
// ItsChat.kt - Application class
class ItsChat : Application() {
    lateinit var tokenProvider: DataStoreTokenProvider
    lateinit var authApi: AuthApi
    lateinit var okHttpClient: OkHttpClient
    // ⚠️ Dlaczego to nie jest w Hilt?
}
```

**Problemy:**
- ❌ Mieszanie DI przez Hilt i ręczne `lateinit var`
- ❌ Globalne zmienne w Application class
- ❌ Nieużywane zmienne

---

### 8. **Duplikacja Kodu**

- Multiple `stringResource(R.string.common_dash)` w UI
- Powtarzające się wzorce obsługi błędów
- Duplikacja walidacji
- Brak Extension Functions dla częstych operacji

---

## 🎯 Propozycja Nowej Struktury

### Struktura Pakietów (Clean Architecture)

```
com.itsorderchat/
├── app/
│   ├── ItsOrderChatApp.kt           # Application class
│   └── di/                           # Hilt modules
│       ├── AppModule.kt
│       ├── NetworkModule.kt
│       ├── DatabaseModule.kt
│       └── RepositoryModule.kt
│
├── core/
│   ├── util/
│   │   ├── extensions/               # Extension functions
│   │   ├── Constants.kt
│   │   └── Logger.kt
│   ├── error/
│   │   ├── ErrorHandler.kt
│   │   └── AppException.kt
│   └── base/
│       ├── BaseViewModel.kt
│       ├── BaseRepository.kt
│       └── BaseUseCase.kt
│
├── data/
│   ├── local/
│   │   ├── database/
│   │   │   ├── AppDatabase.kt
│   │   │   └── dao/
│   │   │       ├── OrderDao.kt
│   │   │       └── ProductDao.kt
│   │   ├── entity/                   # Room entities
│   │   │   ├── OrderEntity.kt
│   │   │   └── ProductEntity.kt
│   │   └── preferences/
│   │       ├── UserPreferencesDataStore.kt
│   │       └── AppPreferencesDataStore.kt
│   │
│   ├── remote/
│   │   ├── api/
│   │   │   ├── OrderApi.kt
│   │   │   ├── ProductApi.kt
│   │   │   ├── AuthApi.kt
│   │   │   └── SettingsApi.kt
│   │   ├── dto/                      # Network DTOs
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── interceptor/
│   │   │   ├── AuthInterceptor.kt
│   │   │   └── LanguageInterceptor.kt
│   │   ├── socket/
│   │   │   ├── SocketManager.kt
│   │   │   └── SocketEventHandler.kt
│   │   └── websocket/
│   │       └── WebSocketProvider.kt
│   │
│   ├── repository/                   # Implementacje Repository
│   │   ├── OrderRepositoryImpl.kt
│   │   ├── ProductRepositoryImpl.kt
│   │   └── AuthRepositoryImpl.kt
│   │
│   └── mapper/                       # DTO <-> Domain mappers
│       ├── OrderMapper.kt
│       └── ProductMapper.kt
│
├── domain/
│   ├── model/                        # Domain models (czyste!)
│   │   ├── Order.kt
│   │   ├── Product.kt
│   │   └── User.kt
│   │
│   ├── repository/                   # Repository interfaces
│   │   ├── OrderRepository.kt
│   │   ├── ProductRepository.kt
│   │   └── AuthRepository.kt
│   │
│   └── usecase/                      # Use cases (logika biznesowa)
│       ├── order/
│       │   ├── GetOrdersUseCase.kt
│       │   ├── UpdateOrderStatusUseCase.kt
│       │   ├── SendToExternalCourierUseCase.kt
│       │   └── PrintOrderUseCase.kt
│       ├── auth/
│       │   ├── LoginUseCase.kt
│       │   └── LogoutUseCase.kt
│       └── product/
│           ├── GetProductsUseCase.kt
│           └── UpdateProductUseCase.kt
│
├── presentation/                     # UI Layer
│   ├── navigation/
│   │   ├── AppNavigation.kt
│   │   └── NavigationRoutes.kt
│   │
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   │
│   ├── components/                   # Reusable components
│   │   ├── LoadingIndicator.kt
│   │   ├── ErrorView.kt
│   │   └── EmptyState.kt
│   │
│   └── feature/                      # Features
│       ├── auth/
│       │   ├── login/
│       │   │   ├── LoginScreen.kt
│       │   │   ├── LoginViewModel.kt
│       │   │   └── LoginState.kt
│       │   └── register/
│       │
│       ├── home/
│       │   ├── HomeScreen.kt
│       │   ├── HomeViewModel.kt
│       │   └── HomeState.kt
│       │
│       ├── orders/
│       │   ├── list/
│       │   │   ├── OrdersScreen.kt
│       │   │   ├── OrdersViewModel.kt
│       │   │   └── OrdersState.kt
│       │   ├── detail/
│       │   │   ├── OrderDetailScreen.kt
│       │   │   └── OrderDetailViewModel.kt
│       │   └── components/
│       │       ├── OrderCard.kt
│       │       └── OrderStatusChip.kt
│       │
│       ├── products/
│       │   ├── list/
│       │   ├── detail/
│       │   └── components/
│       │
│       └── settings/
│           ├── SettingsScreen.kt
│           └── SettingsViewModel.kt
│
└── infrastructure/                   # Platform-specific
    ├── service/
    │   ├── SocketService.kt
    │   ├── OrderAlarmService.kt
    │   └── NotificationService.kt
    │
    ├── notification/
    │   ├── NotificationHelper.kt
    │   └── NotificationChannels.kt
    │
    ├── printer/
    │   ├── PrinterService.kt
    │   └── PrinterManager.kt
    │
    └── location/
        └── LocationProvider.kt
```

---

## 🔧 Konkretne Zmiany do Wdrożenia

### Priorytet 1: WYSOKI (Krytyczne dla jakości kodu)

#### 1.1 Wprowadzenie warstwy Domain

**Cel:** Separacja logiki biznesowej od implementacji

**Kroki:**
1. Utworzyć pakiet `domain/model/` dla czystych modeli domenowych
2. Utworzyć pakiet `domain/repository/` dla interfejsów
3. Utworzyć pakiet `domain/usecase/` dla logiki biznesowej
4. Przenieść logikę biznesową z ViewModels do UseCases

**Przykład:**

```kotlin
// domain/usecase/order/SendToExternalCourierUseCase.kt
class SendToExternalCourierUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val notificationManager: NotificationManager
) {
    suspend operator fun invoke(
        orderId: String,
        courier: DeliveryProvider,
        preparationTime: Int
    ): Result<Unit> = runCatching {
        // Walidacja
        require(preparationTime in 15..120) {
            "Preparation time must be between 15 and 120 minutes"
        }
        
        // Wysłanie do API
        val result = orderRepository.sendToExternalCourier(
            orderId = orderId,
            courier = courier,
            preparationTime = preparationTime
        )
        
        // Powiadomienie
        notificationManager.showOrderSentNotification(orderId, courier.name)
        
        result
    }
}

// presentation/feature/orders/OrdersViewModel.kt
class OrdersViewModel @Inject constructor(
    private val sendToExternalCourierUseCase: SendToExternalCourierUseCase
) : ViewModel() {
    
    fun sendToExternalCourier(order: Order, courier: DeliveryProvider, time: Int) {
        viewModelScope.launch {
            sendToExternalCourierUseCase(order.id, courier, time)
                .onSuccess { /* Update UI */ }
                .onFailure { /* Handle error */ }
        }
    }
}
```

---

#### 1.2 Refaktoryzacja AppPrefs

**Cel:** Type-safe, nowoczesne zarządzanie preferencjami

```kotlin
// data/local/preferences/AppPreferencesDataStore.kt
@Singleton
class AppPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore by preferencesDataStore(name = "app_preferences")
    
    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val CURRENCY = stringPreferencesKey("currency")
        val AUTO_PRINT = booleanPreferencesKey("auto_print")
        val PRINTER_TYPE = stringPreferencesKey("printer_type")
        val PRINTER_ID = stringPreferencesKey("printer_id")
    }
    
    val baseUrl: Flow<String?> = context.dataStore.data
        .map { it[Keys.BASE_URL] }
    
    suspend fun setBaseUrl(url: String) {
        context.dataStore.edit { it[Keys.BASE_URL] = url }
    }
    
    // ... pozostałe metody
}

// domain/repository/PreferencesRepository.kt
interface PreferencesRepository {
    val baseUrl: Flow<String?>
    suspend fun setBaseUrl(url: String)
    // ...
}
```

---

#### 1.3 Podział OrdersViewModel

**Cel:** Pojedyncza odpowiedzialność, łatwiejsze testowanie

```kotlin
// presentation/feature/orders/list/OrdersViewModel.kt
@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val getOrdersUseCase: GetOrdersUseCase,
    private val observeOrderUpdatesUseCase: ObserveOrderUpdatesUseCase
) : ViewModel() {
    // Tylko lista zamówień
}

// presentation/feature/orders/detail/OrderDetailViewModel.kt
@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val updateOrderStatusUseCase: UpdateOrderStatusUseCase,
    private val sendToExternalCourierUseCase: SendToExternalCourierUseCase,
    private val printOrderUseCase: PrintOrderUseCase
) : ViewModel() {
    // Tylko szczegóły i akcje na zamówieniu
}

// presentation/feature/courier/CourierViewModel.kt
@HiltViewModel
class CourierViewModel @Inject constructor(
    private val assignCourierUseCase: AssignCourierUseCase,
    private val getCourierRouteUseCase: GetCourierRouteUseCase
) : ViewModel() {
    // Tylko logika kurierów
}
```

---

### Priorytet 2: ŚREDNI (Poprawa struktury)

#### 2.1 Uporządkowanie struktury pakietów

**Kroki:**
1. Przenieść wszystkie Repository do `data/repository/`
2. Przenieść wszystkie API do `data/remote/api/`
3. Naprawić literówkę: `utili` → `util`
4. Naprawić literówkę: `datebase` → `database`
5. Skonsolidować `util/` w jednym miejscu (`core/util/`)

---

#### 2.2 Wprowadzenie Extension Functions

```kotlin
// core/util/extensions/StringExtensions.kt
fun String?.orDash(): String = this ?: "—"

fun String.toHttpUrlSafe(): HttpUrl? = 
    this.toHttpUrlOrNull()

// core/util/extensions/FlowExtensions.kt
fun <T> Flow<Resource<T>>.onSuccess(action: suspend (T) -> Unit): Flow<Resource<T>> =
    onEach { if (it is Resource.Success) action(it.value) }

fun <T> Flow<Resource<T>>.onError(action: suspend (Int?, Any?) -> Unit): Flow<Resource<T>> =
    onEach { if (it is Resource.Failure) action(it.errorCode, it.errorBody) }

// Użycie:
Text(order.consumer.phone.orDash())  // Zamiast: order.consumer.phone ?: stringResource(R.string.common_dash)
```

---

#### 2.3 Wprowadzenie sealed class dla stanów UI

```kotlin
// presentation/feature/orders/OrdersUiState.kt
sealed interface OrdersUiState {
    data object Loading : OrdersUiState
    
    data class Success(
        val orders: List<Order>,
        val selectedOrder: Order? = null,
        val filters: OrderFilters = OrderFilters()
    ) : OrdersUiState
    
    data class Error(
        val message: String,
        val retryAction: (() -> Unit)? = null
    ) : OrdersUiState
}

// presentation/feature/orders/OrdersViewModel.kt
class OrdersViewModel @Inject constructor(...) : ViewModel() {
    
    private val _uiState = MutableStateFlow<OrdersUiState>(OrdersUiState.Loading)
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()
    
    init {
        loadOrders()
    }
}
```

---

### Priorytet 3: NISKI (Nice to have)

#### 3.1 Wprowadzenie Navigation Component

```kotlin
// presentation/navigation/AppNavGraph.kt
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavigationRoute.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavigationRoute.Home.route) {
                        popUpTo(NavigationRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavigationRoute.Home.route) {
            HomeScreen(
                onOrderClick = { orderId ->
                    navController.navigate(NavigationRoute.OrderDetail.createRoute(orderId))
                }
            )
        }
        
        composable(
            route = NavigationRoute.OrderDetail.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")
            OrderDetailScreen(orderId = orderId.orEmpty())
        }
    }
}
```

---

#### 3.2 Wprowadzenie Result wrapper

```kotlin
// core/util/Result.kt
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: AppException) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

// core/error/AppException.kt
sealed class AppException(message: String) : Exception(message) {
    data class Network(val code: Int, override val message: String) : AppException(message)
    data class Unauthorized(override val message: String) : AppException(message)
    data class Validation(override val message: String) : AppException(message)
    data class Unknown(override val message: String) : AppException(message)
}
```

---

## 📊 Plan Migracji (Krok po kroku)

### Faza 1: Przygotowanie (1 tydzień)
- [ ] Utworzyć nową strukturę pakietów
- [ ] Skonfigurować Gradle dla nowej struktury
- [ ] Utworzyć bazowe klasy (BaseViewModel, BaseUseCase, etc.)

### Faza 2: Warstwa Domain (2 tygodnie)
- [ ] Utworzyć modele domenowe
- [ ] Utworzyć interfejsy Repository
- [ ] Utworzyć pierwsze Use Cases (Order, Auth)
- [ ] Testy jednostkowe dla Use Cases

### Faza 3: Warstwa Data (2 tygodnie)
- [ ] Przenieść Repository do nowej struktury
- [ ] Przenieść API do nowej struktury
- [ ] Utworzyć Mappery (DTO ↔ Domain)
- [ ] Refaktoryzacja DataStore

### Faza 4: Warstwa Presentation (3 tygodnie)
- [ ] Podzielić ViewModels
- [ ] Wprowadzić sealed class dla stanów
- [ ] Przenieść ekrany do feature-based structure
- [ ] Wydzielić reusable components

### Faza 5: Infrastructure (1 tydzień)
- [ ] Przenieść Services
- [ ] Refaktoryzacja SocketEventHandler
- [ ] Oddzielić logikę biznesową od infrastruktury

### Faza 6: Cleanup (1 tydzień)
- [ ] Usunąć stary kod
- [ ] Naprawić wszystkie warningi
- [ ] Code review
- [ ] Dokumentacja

---

## 🧪 Testowanie

### Unit Tests
```kotlin
// domain/usecase/order/SendToExternalCourierUseCaseTest.kt
class SendToExternalCourierUseCaseTest {
    
    private lateinit var useCase: SendToExternalCourierUseCase
    private lateinit var orderRepository: OrderRepository
    private lateinit var notificationManager: NotificationManager
    
    @Before
    fun setup() {
        orderRepository = mockk()
        notificationManager = mockk(relaxed = true)
        useCase = SendToExternalCourierUseCase(orderRepository, notificationManager)
    }
    
    @Test
    fun `should validate preparation time`() = runTest {
        // Given
        val invalidTime = 200
        
        // When
        val result = useCase("orderId", DeliveryProvider.WOLT, invalidTime)
        
        // Then
        assertTrue(result.isFailure)
    }
}
```

---

## 📈 Oczekiwane Korzyści

### Krótkoterminowe (1-2 miesiące)
- ✅ Łatwiejsze onboarding nowych developerów
- ✅ Szybsze znajdowanie kodu
- ✅ Mniej błędów przy zmianach
- ✅ Lepsza testowalność

### Długoterminowe (6+ miesięcy)
- ✅ Łatwiejsze dodawanie nowych funkcji
- ✅ Możliwość reużycia logiki biznesowej
- ✅ Łatwiejsze skalowanie zespołu
- ✅ Lepsza wydajność (dzięki lepszej separacji)
- ✅ Możliwość modularyzacji aplikacji

---

## 🎓 Rekomendacje dla Zespołu

### Konwencje Kodowania
1. **Nazewnictwo:**
   - Repository: liczba pojedyncza lub mnoga KONSEKWENTNIE
   - UseCase: czasownik + rzeczownik + "UseCase" (np. `GetOrdersUseCase`)
   - ViewModel: rzeczownik + "ViewModel" (np. `OrdersViewModel`)

2. **Komentarze:**
   - KDoc dla public API
   - TODO z datą i autorem
   - Wyjaśnienia dla złożonej logiki

3. **Formatowanie:**
   - Używać formatera z Android Studio (Ctrl+Alt+L)
   - Max 120 znaków w linii
   - Konsekwentne wcięcia

### Code Review
- Każdy PR wymaga review od min. 1 osoby
- Sprawdzać testy jednostkowe
- Sprawdzać zgodność z architekturą

### CI/CD
- Automatyczne testy przy każdym PR
- Lint checks
- Detekt dla static analysis

---

## 📚 Przydatne Zasoby

- [Clean Architecture Guide](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Android Architecture Components](https://developer.android.com/topic/architecture)
- [Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- [Testing Guide](https://developer.android.com/training/testing)

---

## ✅ Checklist dla Pull Requestów

- [ ] Kod jest w odpowiednim pakiecie zgodnie z nową strukturą
- [ ] Dodano/zaktualizowano testy jednostkowe
- [ ] Dodano KDoc dla public API
- [ ] Brak warnings w Lint
- [ ] Przetestowano manualnie na urządzeniu
- [ ] Updated documentation (jeśli dotyczy)

---

**Koniec dokumentu**

