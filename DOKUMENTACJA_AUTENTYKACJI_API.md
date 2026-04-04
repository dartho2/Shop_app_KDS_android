# 📚 Dokumentacja Systemu Autentykacji i Tokenów

## 📋 Spis Treści

1. [Przegląd Systemu](#przegląd-systemu)
2. [Architektura](#architektura)
3. [API Endpoints](#api-endpoints)
4. [Request/Response Models](#requestresponse-models)
5. [Flow Logowania](#flow-logowania)
6. [Automatyczne Odświeżanie Tokena](#automatyczne-odświeżanie-tokena)
7. [Konfiguracja Base URL](#konfiguracja-base-url)
8. [Przechowywanie Tokenów](#przechowywanie-tokenów)
9. [Dołączanie Tokenów do Zapytań](#dołączanie-tokenów-do-zapytań)
10. [Obsługa 401 Unauthorized](#obsługa-401-unauthorized)
11. [Implementacja Krok Po Kroku](#implementacja-krok-po-kroku)
12. [Best Practices](#best-practices)
13. [Troubleshooting](#troubleshooting)

---

## 🎯 Przegląd Systemu

System autentykacji wykorzystuje **JWT (JSON Web Tokens)** z mechanizmem **refresh token** dla bezpiecznego i wygodnego zarządzania sesją użytkownika.

### Kluczowe Komponenty

```
┌─────────────────┐
│   LoginScreen   │ → UI dla logowania
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ AuthRepository  │ → Logika biznesowa
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│    AuthApi      │ → Komunikacja z API
└────────┬────────┘
         │
         ▼
┌─────────────────────────┐
│  TokenProvider/         │ → Przechowywanie tokenów
│  AppPreferencesManager  │
└─────────────────────────┘
         │
         ▼
┌──────────────────────────────┐
│  AuthInterceptor +           │ → Automatyczne dołączanie
│  TokenAuthenticator          │   i odświeżanie tokenów
└──────────────────────────────┘
```

---

## 🏗️ Architektura

### Warstwy Systemu

#### 1. **API Layer** (Retrofit)
- `AuthApi` - Definicje endpointów
- `AuthInterceptor` - Dołączanie tokena do requestów
- `TokenAuthenticator` - Automatyczne odświeżanie tokena

#### 2. **Repository Layer**
- `AuthRepository` - Logika biznesowa logowania
- `TokenRefreshHelper` - Pomocnik do odświeżania tokenów

#### 3. **Data Layer**
- `AppPreferencesManager` - Przechowywanie tokenów (DataStore)
- `TokenProvider` - Interface dla dostępu do tokenów

#### 4. **Models**
- `LoginRequest` - Model żądania logowania
- `RefreshTokenRequest` - Model żądania odświeżenia tokena
- `LoginResponse` - Model odpowiedzi z tokenami

---

## 🌐 API Endpoints

### 1. Login

```http
POST /client/v3/api/mobile/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token_id": "550e8400-e29b-41d4-a716-446655440000",
  "tenantKey": "restaurant123",
  "role": "STAFF",
  "sub": "user-id-12345"
}
```

---

### 2. Refresh Token

```http
POST /client/v3/api/mobile/refresh
Content-Type: application/json

{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:** (identyczna jak przy logowaniu)
```json
{
  "access_token": "NEW_ACCESS_TOKEN...",
  "refresh_token": "NEW_REFRESH_TOKEN...",
  "refresh_token_id": "NEW_REFRESH_TOKEN_ID...",
  "tenantKey": "restaurant123",
  "role": "STAFF",
  "sub": "user-id-12345"
}
```

---

## 📦 Request/Response Models

### LoginRequest

```kotlin
data class LoginRequest(
    val email: String,
    val password: String
)
```

**Przykład użycia:**
```kotlin
val request = LoginRequest(
    email = "staff@restaurant.com",
    password = "password123"
)
```

---

### RefreshTokenRequest

```kotlin
data class RefreshTokenRequest(
    val refresh_token: String,
    val refresh_token_id: String
)
```

**Przykład użycia:**
```kotlin
val refreshRequest = RefreshTokenRequest(
    refresh_token = storedRefreshToken,
    refresh_token_id = storedRefreshTokenId
)
```

---

### LoginResponse

```kotlin
data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("refresh_token")
    val refreshToken: String,

    @SerializedName("refresh_token_id")
    val refreshTokenId: String,

    @SerializedName("tenantKey")
    val tenantKey: String,

    @SerializedName("role")
    val role: UserRole,

    @SerializedName("sub")
    val sub: String  // User ID
)

enum class UserRole {
    ADMIN,
    USER,
    MANAGER,
    STAFF,
    COURIER
}
```

**Opis pól:**
- `accessToken` - Token JWT dla autoryzacji requestów (krótkotrwały, np. 15 min)
- `refreshToken` - Token do odświeżania access token (długotrwały, np. 7 dni)
- `refreshTokenId` - Unikalny ID refresh tokena (dla bezpieczeństwa)
- `tenantKey` - Klucz restauracji/firmy
- `role` - Rola użytkownika w systemie
- `sub` - Unikalny identyfikator użytkownika

---

## 🔐 Flow Logowania

### Diagram Sekwencji

```
User             UI              Repository        API            DataStore
 │               │                    │             │                 │
 │──Click Login─>│                    │             │                 │
 │               │                    │             │                 │
 │               │──login()──────────>│             │                 │
 │               │                    │             │                 │
 │               │                    │──POST────>  │                 │
 │               │                    │   /login    │                 │
 │               │                    │             │                 │
 │               │                    │<─Response───│                 │
 │               │                    │   (tokens)  │                 │
 │               │                    │             │                 │
 │               │                    │──saveTokens()──────────────> │
 │               │                    │             │                 │
 │               │<──Success──────────│             │                 │
 │               │                    │             │                 │
 │<─Navigate─────│                    │             │                 │
 │   to Home     │                    │             │                 │
```

---

### Implementacja Krok Po Kroku

#### Krok 1: Definicja API

```kotlin
// AuthApi.kt
interface AuthApi {
    @POST("client/v3/api/mobile/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>

    @POST("client/v3/api/mobile/refresh")
    fun refreshSync(
        @Body refreshTokenRequest: RefreshTokenRequest
    ): Call<LoginResponse>
}
```

**Kluczowe różnice:**
- `login()` - **suspend** function (asynchroniczna, dla coroutines)
- `refreshSync()` - **Call** (synchroniczna, dla Authenticator)

---

#### Krok 2: Repository

```kotlin
// AuthRepository.kt
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenProvider: TokenProvider,
    @ApplicationContext private val context: Context,
    messageManager: GlobalMessageManager
) : BaseRepository(messageManager) {

    /**
     * Logowanie użytkownika
     */
    suspend fun login(email: String, password: String): Resource<LoginResponse> {
        return safeApiCall {
            api.login(LoginRequest(email, password))
        }
    }

    /**
     * Zapisanie tokenów po logowaniu
     */
    suspend fun saveAuthToken(
        access: String,
        refresh: String,
        refreshId: String,
        tenantKey: String,
        role: UserRole?,
        userId: String?
    ) {
        tokenProvider.saveTokens(
            accessToken = access,
            refreshToken = refresh,
            refreshTokenId = refreshId,
            tenantKey = tenantKey,
            role = role,
            userId = userId
        )
    }
}
```

---

#### Krok 3: ViewModel (przykład użycia)

```kotlin
// LoginViewModel.kt
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            when (val result = authRepository.login(email, password)) {
                is Resource.Success -> {
                    val response = result.value

                    // Zapisz tokeny
                    authRepository.saveAuthToken(
                        access = response.accessToken,
                        refresh = response.refreshToken,
                        refreshId = response.refreshTokenId,
                        tenantKey = response.tenantKey,
                        role = response.role,
                        userId = response.sub
                    )

                    _uiState.value = LoginUiState.Success
                }

                is Resource.Failure -> {
                    _uiState.value = LoginUiState.Error(
                        message = result.errorBody ?: "Błąd logowania"
                    )
                }
            }
        }
    }
}
```

---

## 🔄 Automatyczne Odświeżanie Tokena

### Jak To Działa?

Gdy access token wygasa (401 Unauthorized), system **automatycznie** próbuje odświeżyć token używając refresh tokena.

### Komponenty

#### 1. TokenAuthenticator

```kotlin
// TokenAuthenticator.kt
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val authApi: AuthApi
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {

        // ① Unikaj nieskończonej pętli
        if (response.wasRetried()) return null

        // ② Sprawdź czy ktoś już odświeżył token
        val cachedToken = tokenProvider.getAccessToken()
        val requestHeader = response.request.header("Authorization")
        
        if (!cachedToken.isNullOrBlank() && 
            requestHeader != "Bearer $cachedToken") {
            // Token już został odświeżony przez inny request
            return response.request.withAuth(cachedToken)
        }

        // ③ Odśwież token
        return when (val result = TokenRefreshHelper.refreshBlocking(
            tokenProvider, 
            authApi
        )) {
            is TokenRefreshHelper.RefreshResult.Success -> {
                // Użyj nowego tokena
                response.request.withAuth(result.newAccessToken)
            }
            else -> {
                // Refresh nieudany → przekaż 401 wyżej
                null
            }
        }
    }

    // Helpers
    private fun Response.wasRetried(): Boolean = retryCount() > 0
    
    private fun Response.retryCount(): Int {
        var count = 0
        var p = priorResponse
        while (p != null) {
            count++
            p = p.priorResponse
        }
        return count
    }

    private fun Request.withAuth(token: String): Request =
        newBuilder().header("Authorization", "Bearer $token").build()
}
```

**Kluczowe zabezpieczenia:**
1. **Retry limit** - Maksymalnie 1 próba (sprawdzanie `priorResponse`)
2. **Token reuse** - Jeśli inny request już odświeżył, użyj go
3. **Synchronizacja** - `TokenRefreshLock` zapobiega równoczesnym odświeżeniom

---

#### 2. TokenRefreshHelper

```kotlin
// TokenRefreshHelper.kt
object TokenRefreshLock {
    val lock = Any()
}

object TokenRefreshHelper {
    @Volatile
    private var refreshInProgress = false

    fun refreshBlocking(
        tokenProvider: TokenProvider, 
        authApi: AuthApi
    ): RefreshResult {
        synchronized(TokenRefreshLock.lock) {
            
            // Jeśli refresh jest w toku, czekaj
            if (refreshInProgress) {
                repeat(10) {
                    Thread.sleep(100)
                    val nowToken = runBlocking { 
                        tokenProvider.getAccessToken() 
                    }
                    if (!nowToken.isNullOrBlank()) {
                        return RefreshResult.Success(nowToken)
                    }
                }
                return RefreshResult.Failed
            }

            refreshInProgress = true
            try {
                val refreshToken = runBlocking { 
                    tokenProvider.getRefreshToken() 
                }
                val refreshTokenId = runBlocking { 
                    tokenProvider.getRefreshTokenId() 
                }

                if (refreshToken.isNullOrBlank() || 
                    refreshTokenId.isNullOrBlank()) {
                    return RefreshResult.Failed
                }

                // Wykonaj request refresh
                val refreshResp = try {
                    authApi.refreshSync(
                        RefreshTokenRequest(refreshToken, refreshTokenId)
                    ).execute()
                } catch (_: IOException) {
                    return RefreshResult.Failed
                }

                // Obsługa wygasłej sesji
                if (refreshResp.code() == 401) {
                    return RefreshResult.SessionExpired
                }

                if (!refreshResp.isSuccessful) {
                    return RefreshResult.Failed
                }

                val body = refreshResp.body() ?: return RefreshResult.Failed

                // Zapisz nowe tokeny
                runBlocking {
                    tokenProvider.saveTokens(
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        refreshTokenId = body.refreshTokenId,
                        tenantKey = body.tenantKey,
                        role = body.role,
                        userId = body.sub
                    )
                }

                return RefreshResult.Success(body.accessToken)
            } finally {
                refreshInProgress = false
            }
        }
    }

    sealed class RefreshResult {
        data class Success(val newAccessToken: String) : RefreshResult()
        object Failed : RefreshResult()
        object SessionExpired : RefreshResult()
    }
}
```

**Kluczowe mechanizmy:**
1. **Synchronized block** - Tylko jeden refresh naraz
2. **Volatile flag** - Thread-safe sprawdzanie stanu
3. **Retry logic** - Czekanie na token z innego wątku
4. **Session expired** - Rozróżnienie 401 (wyloguj) vs błąd sieci

---

### Diagram Flow Odświeżania

```
Request 1 (401)         Request 2 (401)         TokenRefreshHelper
     │                       │                          │
     ├──authenticate()──────>│                          │
     │                       ├──authenticate()─────────>│
     │                       │                          │
     │                       │                  ┌───────┴────────┐
     │                       │                  │ synchronized { │
     │                       │                  │                │
     │                       │                  │  refreshing?   │
     │                       │                  │     NO         │
     │                       │                  │                │
     │                       │                  │ refreshing=true│
     │                       │                  │                │
     │                       │                  │  POST /refresh │
     │                       │ <─Wait (100ms)───┤                │
     │                       │                  │  Save tokens   │
     │                       │ <─Wait (100ms)───┤                │
     │                       │                  │ refreshing=false│
     │ <─New token───────────┤                  │                │
     │                       │ <─New token──────┤ }              │
     │                       │                  └────────────────┘
     │                       │                          │
     │──Retry with token────>│                          │
     │                       │──Retry with token───────>│
     │                       │                          │
     ▼ Success               ▼ Success                  ▼
```

---

## 🌐 Konfiguracja Base URL

### Skąd Bierze Się Base URL?

Base URL (adres serwera) jest **przechowywany w DataStore** i może być **dynamicznie zmieniany** przez użytkownika (np. w ustawieniach aplikacji).

#### Przechowywanie w AppPreferencesManager

```kotlin
// AppPreferencesManager.kt
private object Keys {
    val BASE_URL = stringPreferencesKey("base_url")
    // ...inne klucze
}

/**
 * Pobierz Base URL z DataStore
 * Domyślnie: https://localhost:8001/
 */
suspend fun getBaseUrl(): String =
    dataStore.data.map { it[Keys.BASE_URL] ?: "https://localhost:8001/" }.first()

/**
 * Zapisz nowy Base URL
 * Automatycznie dodaje https:// i trailing slash
 */
suspend fun setBaseUrl(url: String) {
    val trimmed = url.trim()
    require(trimmed.isNotEmpty()) { "Adres serwera jest pusty" }

    // Dodaj schemat jeśli brakuje
    val withScheme = when {
        trimmed.startsWith("http://", ignoreCase = true) -> trimmed
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        else -> "https://$trimmed"  // Domyślnie https
    }

    // Dodaj trailing slash (wymagane przez Retrofit)
    val normalized = if (withScheme.endsWith("/")) withScheme else "$withScheme/"

    dataStore.edit { it[Keys.BASE_URL] = normalized }
}

/**
 * Flow dla reaktywnych zmian Base URL
 */
val baseUrlFlow: Flow<String> = dataStore.data.map { prefs ->
    prefs[Keys.BASE_URL] ?: "https://localhost:8001/"
}
```

---

### Jak Jest Dołączany do Retrofit?

#### Konfiguracja w NetworkModule

```kotlin
// NetworkModule.kt
@Provides
@Singleton
fun provideRetrofit(
    okHttpClient: OkHttpClient,
    preferencesManager: AppPreferencesManager
): Retrofit {
    // Pobierz Base URL z DataStore
    val baseUrl = runBlocking { 
        preferencesManager.getBaseUrl() 
    }
    
    return Retrofit.Builder()
        .baseUrl(baseUrl)  // ✅ Dołącza Base URL
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
```

**Kluczowe:**
- `runBlocking` - Pobiera Base URL synchronicznie przy inicjalizacji
- Retrofit używa tego URL jako prefix dla wszystkich endpointów
- Przykład: `baseUrl = "https://api.example.com/"` + endpoint `"client/v3/api/mobile/login"` = `"https://api.example.com/client/v3/api/mobile/login"`

---

### Przykład Ustawienia Base URL w UI

```kotlin
// SettingsViewModel.kt
class SettingsViewModel @Inject constructor(
    private val preferencesManager: AppPreferencesManager
) : ViewModel() {

    // Aktualny Base URL (reaktywny)
    val baseUrl: StateFlow<String> = preferencesManager.baseUrlFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    /**
     * Zmień Base URL
     */
    fun updateBaseUrl(newUrl: String) {
        viewModelScope.launch {
            try {
                preferencesManager.setBaseUrl(newUrl)
                _event.emit(SettingsEvent.Success("URL zapisany"))
            } catch (e: Exception) {
                _event.emit(SettingsEvent.Error(e.message ?: "Błąd"))
            }
        }
    }
}
```

---

### Flow Zmiany Base URL

```
User wprowadza nowy URL w ustawieniach
  ↓
ViewModel.updateBaseUrl("api.myrestaurant.com")
  ↓
AppPreferencesManager.setBaseUrl()
  ├─> Normalizacja: "api.myrestaurant.com"
  ├─> Dodaj https://: "https://api.myrestaurant.com"
  ├─> Dodaj /: "https://api.myrestaurant.com/"
  └─> Zapisz do DataStore
  ↓
⚠️ UWAGA: Retrofit NIE aktualizuje się automatycznie!
  ↓
Rozwiązanie: Restart aplikacji lub reinicjalizacja Retrofit
```

---

### Dynamiczna Zmiana Base URL (Zaawansowane)

Jeśli chcesz zmienić Base URL bez restartu aplikacji:

```kotlin
// DynamicRetrofitManager.kt (opcjonalne)
@Singleton
class DynamicRetrofitManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val preferencesManager: AppPreferencesManager
) {
    private var currentRetrofit: Retrofit? = null

    suspend fun getRetrofit(): Retrofit {
        val baseUrl = preferencesManager.getBaseUrl()
        
        // Jeśli URL się zmienił, stwórz nowy Retrofit
        if (currentRetrofit?.baseUrl()?.toString() != baseUrl) {
            currentRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        
        return currentRetrofit!!
    }

    fun <T> createApi(apiClass: Class<T>): T {
        return runBlocking { getRetrofit().create(apiClass) }
    }
}
```

**Ale zazwyczaj wystarczy restart aplikacji po zmianie Base URL.**

---

### Przykłady Base URL

**Produkcja:**
```kotlin
baseUrl = "https://api.itsorderchat.com/"
```

**Staging:**
```kotlin
baseUrl = "https://staging-api.itsorderchat.com/"
```

**Lokalny development:**
```kotlin
baseUrl = "http://192.168.1.100:8001/"
```

**Localhost (emulator Android):**
```kotlin
baseUrl = "http://10.0.2.2:8001/"  // 10.0.2.2 = localhost w emulatorze
```

---

### ⚠️ KLUCZOWE: Base URL PRZED Logowaniem!

**Bardzo ważne:** Base URL jest potrzebne **PRZED pierwszym logowaniem**, ponieważ sam endpoint logowania wymaga Base URL!

#### Problem przy Pierwszym Uruchomieniu

```
1. Użytkownik instaluje aplikację
   ↓
2. Base URL = "https://localhost:8001/" (domyślny - NIE DZIAŁA!)
   ↓
3. Użytkownik próbuje się zalogować
   ↓
4. Request idzie do: https://localhost:8001/client/v3/api/mobile/login
   ↓
5. ❌ BŁĄD: Connection failed (localhost nie istnieje na urządzeniu)
```

#### Rozwiązania

**Rozwiązanie 1: Ekran Konfiguracji Przed Logowaniem (ZALECANE)**

```kotlin
// MainActivity.kt / NavGraph
@Composable
fun AppNavigation(preferencesManager: AppPreferencesManager) {
    val baseUrl by preferencesManager.baseUrlFlow.collectAsState(initial = "")
    
    NavHost(
        startDestination = when {
            // Jeśli Base URL to localhost lub pusty → ekran setup
            baseUrl.isEmpty() || baseUrl.contains("localhost") -> "setup"
            else -> "login"
        }
    ) {
        composable("setup") {
            ServerSetupScreen(
                onConfigured = { navController.navigate("login") }
            )
        }
        
        composable("login") {
            LoginScreen()
        }
    }
}
```

**Rozwiązanie 2: Hardcoded Base URL w BuildConfig**

Jeśli aplikacja jest dedykowana dla **jednej restauracji**:

```gradle
// build.gradle (app)
android {
    buildTypes {
        debug {
            buildConfigField "String", "BASE_URL", "\"https://staging-api.myrestaurant.com/\""
        }
        release {
            buildConfigField "String", "BASE_URL", "\"https://api.myrestaurant.com/\""
        }
    }
}
```

```kotlin
// NetworkModule.kt
@Provides
@Singleton
fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
    return Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)  // ✅ Hardcoded, bez DataStore
        .client(okHttpClient)
        .build()
}
```

**Rozwiązanie 3: QR Code Scanner**

Profesjonalne rozwiązanie dla wielu restauracji:

```kotlin
// ServerSetupScreen.kt
@Composable
fun ServerSetupScreen(onConfigured: () -> Unit) {
    Column {
        Text("Konfiguracja serwera")
        
        // Opcja 1: Ręczne wpisanie
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Adres serwera") }
        )
        
        // Opcja 2: Skan QR
        Button(onClick = { scanQrCode() }) {
            Icon(Icons.Default.QrCode, contentDescription = null)
            Text("Skanuj QR z serwera")
        }
    }
}

// QR Code zawiera JSON:
// {
//   "baseUrl": "https://api.restaurant123.com/",
//   "tenantKey": "restaurant123",
//   "restaurantName": "Pizza Roma"
// }
```

#### Typowy Flow Pierwszego Uruchomienia

```
┌──────────────────────────────────┐
│ 1. Instalacja aplikacji          │
└────────────┬─────────────────────┘
             │
             ▼
┌──────────────────────────────────┐
│ 2. Sprawdź Base URL w DataStore  │
│    └─> "localhost" lub pusty?    │
└────────────┬─────────────────────┘
             │
             ▼ TAK
┌──────────────────────────────────┐
│ 3. EKRAN KONFIGURACJI SERWERA    │
│    - Wpisz adres ręcznie         │
│    - Lub skanuj QR code          │
└────────────┬─────────────────────┘
             │
             ▼
┌──────────────────────────────────┐
│ 4. Zapisz Base URL do DataStore  │
│    + walidacja + normalizacja    │
└────────────┬─────────────────────┘
             │
             ▼
┌──────────────────────────────────┐
│ 5. Reinicjalizuj Retrofit        │
│    (restart app lub dynamic)     │
└────────────┬─────────────────────┘
             │
             ▼
┌──────────────────────────────────┐
│ 6. EKRAN LOGOWANIA               │
│    (teraz endpoint działa!)      │
└──────────────────────────────────┘
```

---

### Walidacja i Normalizacja

**AppPreferencesManager automatycznie:**

1. **Dodaje https://** jeśli brakuje schematu
   ```kotlin
   "api.example.com" → "https://api.example.com/"
   ```

2. **Zachowuje http://** jeśli użytkownik go podał
   ```kotlin
   "http://192.168.1.100:8001" → "http://192.168.1.100:8001/"
   ```

3. **Dodaje trailing slash** (wymagane przez Retrofit)
   ```kotlin
   "https://api.example.com" → "https://api.example.com/"
   ```

4. **Validuje** czy URL nie jest pusty
   ```kotlin
   require(trimmed.isNotEmpty()) { "Adres serwera jest pusty" }
   ```

---

## 💾 Przechowywanie Tokenów

### AppPreferencesManager (DataStore)

```kotlin
@Singleton
class AppPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = "itsorderchat_preferences"
    )

    private val dataStore get() = context.dataStore

    // ═══ Keys ═══
    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val REFRESH_TOKEN_ID = stringPreferencesKey("refresh_token_id")
        val TENANT_KEY = stringPreferencesKey("tenant_key")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_ROLE = stringPreferencesKey("user_role")
        val BASE_URL = stringPreferencesKey("base_url")  // ✅ Adres serwera
    }

    // ═══ Cache dla synchronicznego dostępu ═══
    @Volatile
    private var cachedAccessToken: String? = null

    /**
     * Synchroniczny dostęp (dla interceptors)
     */
    fun getAccessToken(): String? = cachedAccessToken

    /**
     * Flow z auto-cache
     */
    val accessTokenFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.ACCESS_TOKEN].also { cachedAccessToken = it }
    }

    /**
     * Zapisz tokeny
     */
    suspend fun saveAuthTokens(
        accessToken: String,
        refreshToken: String,
        refreshTokenId: String,
        tenantKey: String,
        role: UserRole?,
        userId: String?
    ) {
        require(accessToken.isNotBlank()) { "Access token nie może być pusty" }
        require(refreshToken.isNotBlank()) { "Refresh token nie może być pusty" }

        dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            prefs[Keys.REFRESH_TOKEN] = refreshToken
            prefs[Keys.REFRESH_TOKEN_ID] = refreshTokenId
            prefs[Keys.TENANT_KEY] = tenantKey

            if (role != null) {
                prefs[Keys.USER_ROLE] = role.name
            } else {
                prefs.remove(Keys.USER_ROLE)
            }

            if (userId != null && userId.isNotBlank()) {
                prefs[Keys.USER_ID] = userId
            } else {
                prefs.remove(Keys.USER_ID)
            }
        }
        
        // Zaktualizuj cache
        cachedAccessToken = accessToken
    }

    /**
     * Wyczyść tokeny (logout)
     */
    suspend fun clearAuthTokens() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN_ID)
            prefs.remove(Keys.TENANT_KEY)
            prefs.remove(Keys.USER_ROLE)
            prefs.remove(Keys.USER_ID)
        }
        cachedAccessToken = null
    }

    // ═══ Asynchroniczne gettery ═══
    
    suspend fun getRefreshToken(): String? =
        dataStore.data.map { it[Keys.REFRESH_TOKEN] }.first()

    suspend fun getRefreshTokenId(): String? =
        dataStore.data.map { it[Keys.REFRESH_TOKEN_ID] }.first()

    suspend fun getTenantKey(): String? =
        dataStore.data.map { it[Keys.TENANT_KEY] }.first()

    suspend fun getUserId(): String? =
        dataStore.data.map { it[Keys.USER_ID] }.first()

    suspend fun getUserRole(): UserRole? =
        dataStore.data.map { prefs ->
            prefs[Keys.USER_ROLE]?.let { UserRole.valueOf(it) }
        }.first()
}
```

**Kluczowe cechy:**
1. **Volatile cache** - Szybki synchroniczny dostęp dla interceptors
2. **DataStore** - Trwałe przechowywanie (przetrwa restart)
3. **Flow API** - Reaktywne powiadomienia o zmianach
4. **Validation** - Sprawdzanie czy tokeny nie są puste

---

## 🔌 Dołączanie Tokenów do Zapytań

### AuthInterceptor

```kotlin
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // ① Pobierz token
        val token = tokenProvider.getAccessToken()

        // ② Dodaj nagłówek Authorization
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original  // Brak tokena → nie modyfikuj
        }

        // ③ Wyślij request
        val response = chain.proceed(request)

        // ④ Opcjonalnie: Obsłuż 401
        if (response.code == 401) {
            Log.e("AuthInterceptor", "Unauthorized - token expired")
            // TokenAuthenticator automatycznie spróbuje odświeżyć
        }

        return response
    }
}
```

**Działanie:**
1. Pobiera access token z TokenProvider (cache, szybkie)
2. Dodaje nagłówek `Authorization: Bearer <token>`
3. Przepuszcza request dalej
4. Loguje 401 (TokenAuthenticator go obsłuży)

---

### Konfiguracja OkHttpClient

```kotlin
// NetworkModule.kt (Hilt/Dagger)
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenProvider: TokenProvider
    ): AuthInterceptor = AuthInterceptor(tokenProvider)

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokenProvider: TokenProvider,
        authApi: AuthApi
    ): TokenAuthenticator = TokenAuthenticator(tokenProvider, authApi)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)        // Dołącza token
            .authenticator(tokenAuthenticator)      // Odświeża token
            .addInterceptor(loggingInterceptor)     // Logi (opcjonalnie)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        preferencesManager: AppPreferencesManager
    ): Retrofit {
        // ✅ Pobierz Base URL z DataStore
        // runBlocking bo Hilt wymaga synchronicznego providera
        val baseUrl = runBlocking { 
            preferencesManager.getBaseUrl() 
        }
        
        return Retrofit.Builder()
            .baseUrl(baseUrl)  // np. "https://api.itsorderchat.com/"
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }
    
    // Przykład: Jak działa Base URL z endpointami
    // Base URL: "https://api.example.com/"
    // Endpoint: "client/v3/api/mobile/login"
    // Pełny URL: "https://api.example.com/client/v3/api/mobile/login"
}
```

**Kluczowe punkty:**
- `runBlocking` - Pobiera Base URL synchronicznie (wymagane przez Hilt)
- Base URL jest pobierany **raz przy inicjalizacji** Retrofit
- Zmiana Base URL wymaga **restartu aplikacji** lub reinicjalizacji Retrofit
- Base URL **musi kończyć się slashem** `/` (AppPreferencesManager dodaje automatycznie)

**Kolejność jest ważna:**
1. **AuthInterceptor** - Dodaje token do każdego requestu
2. **TokenAuthenticator** - Odświeża token gdy dostanie 401
3. **LoggingInterceptor** - Loguje requesty (po modyfikacji)

---

## ⚠️ Obsługa 401 Unauthorized

### 3 Scenariusze

#### 1. Access Token Wygasł (Refresh OK)

```
Request → 401 → TokenAuthenticator
  ↓
refreshBlocking()
  ↓
POST /refresh → 200 OK
  ↓
Save new tokens
  ↓
Retry request with new token
  ↓
Success ✅
```

---

#### 2. Refresh Token Wygasł (Session Expired)

```
Request → 401 → TokenAuthenticator
  ↓
refreshBlocking()
  ↓
POST /refresh → 401 Unauthorized
  ↓
RefreshResult.SessionExpired
  ↓
Return null (propaguj 401)
  ↓
App: Wyloguj użytkownika ❌
```

---

#### 3. Błąd Sieci

```
Request → 401 → TokenAuthenticator
  ↓
refreshBlocking()
  ↓
POST /refresh → IOException (brak sieci)
  ↓
RefreshResult.Failed
  ↓
Return null
  ↓
App: Pokaż błąd, nie wylogowuj ⚠️
```

---

### Obsługa w UI

```kotlin
// OrdersViewModel.kt (przykład)
private fun handleApiError(error: Resource.Failure) {
    when (error.errorCode) {
        401 -> {
            // Unauthorized - sesja wygasła
            viewModelScope.launch {
                tokenProvider.clearAccessTokens()
                _navigationEvent.emit(NavigationEvent.Logout)
            }
        }
        else -> {
            // Inny błąd
            _uiState.value = UiState.Error(error.errorBody ?: "Unknown error")
        }
    }
}
```

---

## 🛠️ Implementacja Krok Po Kroku

### 1. Dependencies (build.gradle)

```gradle
dependencies {
    // Retrofit
    implementation "com.squareup.retrofit2:retrofit:2.9.0"
    implementation "com.squareup.retrofit2:converter-gson:2.9.0"
    
    // OkHttp
    implementation "com.squareup.okhttp3:okhttp:4.11.0"
    implementation "com.squareup.okhttp3:logging-interceptor:4.11.0"
    
    // DataStore
    implementation "androidx.datastore:datastore-preferences:1.0.0"
    
    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1"
    
    // Hilt (Dependency Injection)
    implementation "com.google.dagger:hilt-android:2.48"
    kapt "com.google.dagger:hilt-compiler:2.48"
}
```

---

### 2. Stwórz Models

```kotlin
// LoginRequest.kt
data class LoginRequest(
    val email: String,
    val password: String
)

// RefreshTokenRequest.kt
data class RefreshTokenRequest(
    val refresh_token: String,
    val refresh_token_id: String
)

// LoginResponse.kt
data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("refresh_token_id") val refreshTokenId: String,
    @SerializedName("tenantKey") val tenantKey: String,
    @SerializedName("role") val role: UserRole,
    @SerializedName("sub") val sub: String
)

enum class UserRole {
    ADMIN, USER, MANAGER, STAFF, COURIER
}
```

---

### 3. Stwórz API Interface

```kotlin
// AuthApi.kt
interface AuthApi {
    @POST("client/v3/api/mobile/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>

    @POST("client/v3/api/mobile/refresh")
    fun refreshSync(
        @Body refreshTokenRequest: RefreshTokenRequest
    ): Call<LoginResponse>
}
```

---

### 4. Stwórz AppPreferencesManager

```kotlin
// AppPreferencesManager.kt
@Singleton
class AppPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = "app_preferences"
    )

    private val dataStore get() = context.dataStore

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val REFRESH_TOKEN_ID = stringPreferencesKey("refresh_token_id")
        val TENANT_KEY = stringPreferencesKey("tenant_key")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_ROLE = stringPreferencesKey("user_role")
        val BASE_URL = stringPreferencesKey("base_url")  // ✅ Dodaj Base URL
    }

    @Volatile
    private var cachedAccessToken: String? = null

    // ═══ Base URL ═══
    suspend fun getBaseUrl(): String =
        dataStore.data.map { it[Keys.BASE_URL] ?: "https://localhost:8001/" }.first()

    suspend fun setBaseUrl(url: String) {
        val trimmed = url.trim()
        require(trimmed.isNotEmpty()) { "Adres serwera jest pusty" }

        val withScheme = when {
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }

        val normalized = if (withScheme.endsWith("/")) withScheme else "$withScheme/"

        dataStore.edit { it[Keys.BASE_URL] = normalized }
    }

    val baseUrlFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.BASE_URL] ?: "https://localhost:8001/"
    }

    // Implementacja pozostałych metod jak w sekcji "Przechowywanie Tokenów"
    // ...
}
```

---

### 5. Stwórz TokenProvider Interface

```kotlin
// TokenProvider.kt
interface TokenProvider {
    fun getAccessToken(): String?
    val accessTokenFlow: Flow<String?>
    suspend fun getRefreshToken(): String?
    suspend fun getRefreshTokenId(): String?
    suspend fun getTenantKey(): String?
    suspend fun getRole(): UserRole?
    suspend fun getUserID(): String?
    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        refreshTokenId: String,
        tenantKey: String,
        role: UserRole?,
        userId: String?
    )
    suspend fun clearAccessTokens()
}

// DataStoreTokenProvider.kt
@Singleton
class DataStoreTokenProvider @Inject constructor(
    private val preferencesManager: AppPreferencesManager
) : TokenProvider {
    override fun getAccessToken(): String? = 
        preferencesManager.getAccessToken()

    override val accessTokenFlow: Flow<String?> = 
        preferencesManager.accessTokenFlow

    override suspend fun getRefreshToken(): String? = 
        preferencesManager.getRefreshToken()

    // ...pozostałe metody
}
```

---

### 6. Stwórz TokenRefreshHelper

```kotlin
// TokenRefreshHelper.kt
object TokenRefreshLock {
    val lock = Any()
}

object TokenRefreshHelper {
    // Implementacja jak w sekcji "Automatyczne Odświeżanie"
    // ...
}
```

---

### 7. Stwórz AuthInterceptor

```kotlin
// AuthInterceptor.kt
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {
    // Implementacja jak w sekcji "Dołączanie Tokenów"
    // ...
}
```

---

### 8. Stwórz TokenAuthenticator

```kotlin
// TokenAuthenticator.kt
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val authApi: AuthApi
) : Authenticator {
    // Implementacja jak w sekcji "Automatyczne Odświeżanie"
    // ...
}
```

---

### 9. Stwórz Repository

```kotlin
// AuthRepository.kt
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenProvider: TokenProvider
) {
    suspend fun login(email: String, password: String) = safeApiCall {
        api.login(LoginRequest(email, password))
    }

    suspend fun saveAuthToken(
        access: String,
        refresh: String,
        refreshId: String,
        tenantKey: String,
        role: UserRole?,
        userId: String?
    ) {
        tokenProvider.saveTokens(access, refresh, refreshId, tenantKey, role, userId)
    }
}
```

---

### 10. Konfiguracja Hilt Module

```kotlin
// NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // Implementacja jak w sekcji "Dołączanie Tokenów"
    // ...
}

// TokenProviderModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class TokenProviderModule {
    @Binds
    @Singleton
    abstract fun bindTokenProvider(
        impl: DataStoreTokenProvider
    ): TokenProvider
}
```

---

## ✅ Best Practices

### 1. Bezpieczeństwo Tokenów

```kotlin
// ✅ DOBRZE: Przechowuj w DataStore (szyfrowane)
preferencesManager.saveAuthTokens(...)

// ❌ ŹLE: Przechowuj w SharedPreferences (niezaszyfrowane)
// ❌ ŹLE: Przechowuj w zmiennych statycznych
// ❌ ŹLE: Przechowuj w logach
```

---

### 2. Token Expiry Handling

```kotlin
// ✅ DOBRZE: Automatyczne odświeżanie przez Authenticator
authenticator(tokenAuthenticator)

// ❌ ŹLE: Ręczne sprawdzanie czasu wygaśnięcia
// (Server decyduje, 401 = wygasł)
```

---

### 3. Synchronizacja Refresh

```kotlin
// ✅ DOBRZE: Synchronized block + volatile flag
synchronized(TokenRefreshLock.lock) {
    if (refreshInProgress) { /* wait */ }
    refreshInProgress = true
    // ...
}

// ❌ ŹLE: Brak synchronizacji (race conditions)
```

---

### 4. Rozróżnienie Błędów

```kotlin
// ✅ DOBRZE: Różne resulaty
sealed class RefreshResult {
    Success,        // Odświeżono
    Failed,         // Błąd sieci (retry)
    SessionExpired  // 401 (wyloguj)
}

// ❌ ŹLE: Jeden wynik (nie wiadomo co zrobić)
```

---

### 5. Cache dla Performance

```kotlin
// ✅ DOBRZE: Volatile cache + DataStore
@Volatile private var cachedAccessToken: String?
fun getAccessToken() = cachedAccessToken

// ❌ ŹLE: Każde wywołanie czyta z DataStore (wolne)
fun getAccessToken() = runBlocking { dataStore.get() }
```

---

### 6. Walidacja Inputów

```kotlin
// ✅ DOBRZE: Sprawdź przed zapisem
require(accessToken.isNotBlank()) { "Token pusty" }

// ❌ ŹLE: Brak walidacji
prefs[TOKEN] = accessToken  // Może być pusty!
```

---

## 🔧 Troubleshooting

### Problem 1: Token Nie Dołącza Się do Requestów

**Symptomy:**
- Requesty dostają 401 mimo że token jest zapisany
- Brak nagłówka `Authorization` w logach

**Rozwiązanie:**
```kotlin
// Sprawdź czy AuthInterceptor jest dodany
OkHttpClient.Builder()
    .addInterceptor(authInterceptor)  // ✅ Musi być!
    .build()

// Sprawdź czy token jest w cache
val token = tokenProvider.getAccessToken()
Log.d("TOKEN", "Token: $token")  // Powinien być wartość, nie null
```

---

### Problem 2: Nieskończona Pętla Refresh

**Symptomy:**
- App się zawiesza
- Logi pokazują wielokrotne `/refresh` requesty

**Rozwiązanie:**
```kotlin
// ✅ Dodaj retry limit
private fun Response.wasRetried(): Boolean = retryCount() > 0

// ✅ Sprawdź czy już odświeżono
if (cachedToken != requestHeader) {
    return request.withAuth(cachedToken)
}
```

---

### Problem 3: Race Condition (Multiple Refreshes)

**Symptomy:**
- 2+ równoczesne `/refresh` requesty
- Token zmienia się chaotycznie

**Rozwiązanie:**
```kotlin
// ✅ Synchronizuj
synchronized(TokenRefreshLock.lock) {
    if (refreshInProgress) {
        // Czekaj na wynik
        repeat(10) {
            Thread.sleep(100)
            val token = getAccessToken()
            if (token != null) return Success(token)
        }
    }
    refreshInProgress = true
    // ...
}
```

---

### Problem 4: Logout Nie Działa

**Symptomy:**
- Po wylogowaniu nadal wysyła requesty z tokenem
- Token widoczny po ponownym logowaniu

**Rozwiązanie:**
```kotlin
// ✅ Wyczyść cache + DataStore
suspend fun clearAuthTokens() {
    dataStore.edit { prefs ->
        prefs.remove(Keys.ACCESS_TOKEN)
        prefs.remove(Keys.REFRESH_TOKEN)
        // ...
    }
    cachedAccessToken = null  // ✅ Nie zapomnij!
}
```

---

### Problem 5: Session Expired Nie Wylogowuje

**Symptomy:**
- Refresh dostaje 401, ale user nie jest wylogowany
- App próbuje w kółko

**Rozwiązanie:**
```kotlin
// ✅ Rozróżnij 401 na /refresh
if (refreshResp.code() == 401) {
    return RefreshResult.SessionExpired  // Nie Failed!
}

// ✅ W UI wyloguj
when (result) {
    is RefreshResult.SessionExpired -> logout()
    is RefreshResult.Failed -> showRetry()
}
```

---

### Problem 6: Base URL Się Nie Zmienia Po Zapisaniu

**Symptomy:**
- Zapisujesz nowy Base URL w ustawieniach
- Requesty nadal idą do starego URL
- Logi pokazują stary adres

**Przyczyna:**
Retrofit jest inicjalizowany **raz** przy starcie aplikacji i nie reaguje na zmiany Base URL w DataStore.

**Rozwiązanie:**

**Opcja 1: Restart aplikacji (zalecane)**
```kotlin
// Po zapisaniu Base URL
fun updateBaseUrl(newUrl: String) {
    viewModelScope.launch {
        preferencesManager.setBaseUrl(newUrl)
        
        // Pokaż komunikat o restarcie
        _event.emit(SettingsEvent.RequiresRestart(
            "Zmiana URL wymaga restartu aplikacji"
        ))
    }
}

// W UI
Button(onClick = { 
    // Restart aplikacji
    val intent = context.packageManager
        .getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    context.startActivity(intent)
    exitProcess(0)
}) {
    Text("Restartuj aplikację")
}
```

**Opcja 2: Dynamiczny Retrofit (zaawansowane)**
```kotlin
@Singleton
class RetrofitProvider @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val preferencesManager: AppPreferencesManager
) {
    private var cachedRetrofit: Retrofit? = null
    private var cachedBaseUrl: String? = null

    suspend fun getRetrofit(): Retrofit {
        val currentBaseUrl = preferencesManager.getBaseUrl()
        
        // Reinicjalizuj jeśli URL się zmienił
        if (cachedBaseUrl != currentBaseUrl) {
            cachedRetrofit = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            cachedBaseUrl = currentBaseUrl
        }
        
        return cachedRetrofit!!
    }
}

// Użycie w Repository
class AuthRepository @Inject constructor(
    private val retrofitProvider: RetrofitProvider
) {
    suspend fun login(email: String, password: String): Resource<LoginResponse> {
        val api = retrofitProvider.getRetrofit().create(AuthApi::class.java)
        return safeApiCall { api.login(LoginRequest(email, password)) }
    }
}
```

**Zalecenie:** Opcja 1 (restart) jest prostsza i bezpieczniejsza.

---

## 📊 Diagram Pełnego Flow

```
┌──────────────────────────────────────────────────────────────┐
│                         USER LOGIN                            │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         ▼
            ┌────────────────────────┐
            │  POST /login           │
            │  { email, password }   │
            └────────┬───────────────┘
                     │
                     ▼
            ┌────────────────────────┐
            │  LoginResponse         │
            │  - access_token        │
            │  - refresh_token       │
            │  - refresh_token_id    │
            │  - tenantKey           │
            │  - role                │
            │  - sub (userId)        │
            └────────┬───────────────┘
                     │
                     ▼
       ┌─────────────────────────────┐
       │  Save to DataStore          │
       │  + Update cache             │
       └────────┬────────────────────┘
                │
                ▼
┌───────────────────────────────────────────────────────────────┐
│                    AUTHENTICATED REQUESTS                      │
└────────────────────────┬──────────────────────────────────────┘
                         │
                         ▼
          ┌──────────────────────────┐
          │  AuthInterceptor         │
          │  Add: Authorization      │
          │  Bearer <access_token>   │
          └────────┬─────────────────┘
                   │
                   ▼
          ┌──────────────────────────┐
          │  API Request             │
          └────────┬─────────────────┘
                   │
          ┌────────┴────────┐
          │                 │
          ▼                 ▼
    ┌─────────┐       ┌─────────┐
    │ 200 OK  │       │ 401     │
    └─────────┘       └────┬────┘
                           │
                           ▼
              ┌────────────────────────────┐
              │  TokenAuthenticator        │
              │  Detect: Token expired     │
              └────────┬───────────────────┘
                       │
                       ▼
         ┌─────────────────────────────────┐
         │  TokenRefreshHelper             │
         │  synchronized(lock) {           │
         │    refreshInProgress?           │
         │      YES → wait for token       │
         │      NO  → POST /refresh        │
         │  }                              │
         └─────────┬───────────────────────┘
                   │
          ┌────────┴────────┐
          │                 │
          ▼                 ▼
    ┌──────────┐      ┌───────────┐
    │ 200 OK   │      │ 401       │
    │ New      │      │ Session   │
    │ Tokens   │      │ Expired   │
    └────┬─────┘      └─────┬─────┘
         │                  │
         ▼                  ▼
  ┌─────────────┐    ┌─────────────┐
  │ Save Tokens │    │ LOGOUT      │
  │ Retry Req.  │    │ Clear Data  │
  └─────────────┘    └─────────────┘
```

---

## 📝 Podsumowanie

### Kluczowe Punkty

1. **JWT Tokens** - Access (krótkotrwały) + Refresh (długotrwały)
2. **DataStore** - Bezpieczne przechowywanie z cache
3. **Interceptor** - Automatyczne dołączanie tokena
4. **Authenticator** - Automatyczne odświeżanie przy 401
5. **Synchronizacja** - Synchronized + Volatile dla thread-safety
6. **Session Expired** - Rozróżnienie 401 (refresh fail) vs błąd sieci

---

### Quick Start Checklist

- [ ] Dodaj dependencies (Retrofit, DataStore, OkHttp)
- [ ] Stwórz models (LoginRequest, LoginResponse, RefreshTokenRequest)
- [ ] Stwórz AuthApi interface
- [ ] Zaimplementuj AppPreferencesManager
- [ ] Stwórz TokenProvider interface + implementation
- [ ] Zaimplementuj TokenRefreshHelper
- [ ] Stwórz AuthInterceptor
- [ ] Stwórz TokenAuthenticator
- [ ] Skonfiguruj OkHttpClient z interceptorami
- [ ] Stwórz AuthRepository
- [ ] Zaimplementuj login flow w ViewModel
- [ ] Obsłuż 401/SessionExpired w UI

---

**Data:** 2026-02-06  
**Wersja:** 1.0  
**Status:** ✅ Kompletna dokumentacja systemu autentykacji

**Powiązane API:**
- Endpoint: `/client/v3/api/mobile/login`
- Endpoint: `/client/v3/api/mobile/refresh`
- DataStore: `itsorderchat_preferences`

