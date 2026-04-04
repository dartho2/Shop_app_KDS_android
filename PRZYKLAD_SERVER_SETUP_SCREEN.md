# 🔧 Przykład Implementacji: Ekran Konfiguracji Serwera

## Problem

Base URL **MUSI** być skonfigurowane **PRZED** pierwszym logowaniem, ponieważ endpoint `/client/v3/api/mobile/login` wymaga Base URL żeby w ogóle móc wysłać request.

---

## Rozwiązanie: ServerSetupScreen

### 1. ViewModel

```kotlin
// ServerSetupViewModel.kt
@HiltViewModel
class ServerSetupViewModel @Inject constructor(
    private val preferencesManager: AppPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<SetupUiState>(SetupUiState.Idle)
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    /**
     * Waliduj i zapisz Base URL
     */
    fun saveBaseUrl(url: String) {
        viewModelScope.launch {
            _uiState.value = SetupUiState.Loading

            try {
                // Walidacja podstawowa
                if (url.isBlank()) {
                    _uiState.value = SetupUiState.Error("Adres serwera nie może być pusty")
                    return@launch
                }

                // Zapisz (AppPreferencesManager zwaliduje i znormalizuje)
                preferencesManager.setBaseUrl(url)

                _uiState.value = SetupUiState.Success
            } catch (e: Exception) {
                _uiState.value = SetupUiState.Error(
                    e.message ?: "Błąd zapisywania adresu serwera"
                )
            }
        }
    }

    /**
     * Załaduj konfigurację z QR Code
     */
    fun loadFromQrCode(qrData: String) {
        viewModelScope.launch {
            try {
                // Parsuj JSON z QR
                val config = Gson().fromJson(qrData, ServerConfig::class.java)
                
                // Zapisz Base URL
                preferencesManager.setBaseUrl(config.baseUrl)
                
                // Opcjonalnie zapisz tenant key
                config.tenantKey?.let {
                    // Możesz zapisać do DataStore jeśli używasz
                }
                
                _uiState.value = SetupUiState.Success
            } catch (e: Exception) {
                _uiState.value = SetupUiState.Error("Nieprawidłowy QR code")
            }
        }
    }
}

sealed class SetupUiState {
    object Idle : SetupUiState()
    object Loading : SetupUiState()
    object Success : SetupUiState()
    data class Error(val message: String) : SetupUiState()
}

data class ServerConfig(
    val baseUrl: String,
    val tenantKey: String? = null,
    val restaurantName: String? = null
)
```

---

### 2. Screen (Jetpack Compose)

```kotlin
// ServerSetupScreen.kt
@Composable
fun ServerSetupScreen(
    viewModel: ServerSetupViewModel = hiltViewModel(),
    onConfigured: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var serverUrl by remember { mutableStateOf("") }
    
    // QR Scanner launcher
    val qrScannerLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        result.contents?.let { qrData ->
            viewModel.loadFromQrCode(qrData)
        }
    }

    // Obsługa sukcesu
    LaunchedEffect(uiState) {
        if (uiState is SetupUiState.Success) {
            onConfigured()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Tytuł
        Text(
            text = "Konfiguracja serwera",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Wprowadź adres serwera restauracji",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Input
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Adres serwera") },
            placeholder = { Text("api.myrestaurant.com") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null
                )
            },
            isError = uiState is SetupUiState.Error,
            enabled = uiState !is SetupUiState.Loading,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Komunikat błędu
        if (uiState is SetupUiState.Error) {
            Text(
                text = (uiState as SetupUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Przykłady
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Przykłady:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• api.itsorderchat.com", style = MaterialTheme.typography.bodySmall)
                Text("• 192.168.1.100:8001", style = MaterialTheme.typography.bodySmall)
                Text("• myserver.com", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Przycisk zapisu
        Button(
            onClick = { viewModel.saveBaseUrl(serverUrl) },
            enabled = serverUrl.isNotBlank() && uiState !is SetupUiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState is SetupUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Kontynuuj")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Przycisk QR
        OutlinedButton(
            onClick = {
                // Uruchom skaner QR
                val options = ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setPrompt("Zeskanuj QR code z serwera")
                    setBeepEnabled(true)
                    setOrientationLocked(true)
                }
                qrScannerLauncher.launch(options)
            },
            enabled = uiState !is SetupUiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Skanuj QR code")
        }
    }
}
```

---

### 3. Nawigacja (NavGraph)

```kotlin
// AppNavGraph.kt
@Composable
fun AppNavGraph(
    navController: NavHostController,
    preferencesManager: AppPreferencesManager
) {
    // Sprawdź czy Base URL jest skonfigurowany
    val baseUrl by preferencesManager.baseUrlFlow.collectAsState(initial = "")
    
    val startDestination = remember(baseUrl) {
        when {
            baseUrl.isEmpty() || 
            baseUrl.contains("localhost") || 
            baseUrl.contains("8001") -> "setup"  // Domyślny = nie skonfigurowany
            else -> "login"
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Ekran konfiguracji serwera
        composable("setup") {
            ServerSetupScreen(
                onConfigured = {
                    // Po zapisaniu Base URL → restart lub przejdź do logowania
                    navController.navigate("login") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }

        // Ekran logowania
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // Inne ekrany...
        composable("home") {
            HomeScreen()
        }
    }
}
```

---

### 4. MainActivity

```kotlin
// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: AppPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MyAppTheme {
                val navController = rememberNavController()
                
                AppNavGraph(
                    navController = navController,
                    preferencesManager = preferencesManager
                )
            }
        }
    }
}
```

---

### 5. QR Code Format

Jeśli używasz QR code scanner, QR powinien zawierać JSON:

```json
{
  "baseUrl": "https://api.myrestaurant.com/",
  "tenantKey": "restaurant123",
  "restaurantName": "Pizza Roma"
}
```

**Generator QR (backend/admin panel):**
```kotlin
// Backend - generuj QR dla restauracji
fun generateSetupQr(restaurant: Restaurant): String {
    val config = ServerConfig(
        baseUrl = "https://api.itsorderchat.com/",
        tenantKey = restaurant.tenantKey,
        restaurantName = restaurant.name
    )
    return Gson().toJson(config)
}
```

---

### 6. Dependencies (build.gradle)

```gradle
dependencies {
    // QR Code Scanner
    implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
    
    // Jetpack Compose
    implementation "androidx.compose.ui:ui:1.5.0"
    implementation "androidx.compose.material3:material3:1.1.0"
    implementation "androidx.navigation:navigation-compose:2.7.0"
    
    // Hilt
    implementation "com.google.dagger:hilt-android:2.48"
    kapt "com.google.dagger:hilt-compiler:2.48"
    implementation "androidx.hilt:hilt-navigation-compose:1.0.0"
}
```

---

## Flow Użycia

```
1. Użytkownik instaluje aplikację
   ↓
2. MainActivity startuje → sprawdza baseUrlFlow
   ↓
3. baseUrl = "localhost" → Navigate("setup")
   ↓
4. ServerSetupScreen wyświetla się
   ↓
5. Użytkownik:
   - Wpisuje ręcznie: "api.myrestaurant.com"
   - LUB skanuje QR code
   ↓
6. ViewModel.saveBaseUrl()
   ├─> AppPreferencesManager.setBaseUrl()
   ├─> Normalizacja: "https://api.myrestaurant.com/"
   └─> Zapis do DataStore
   ↓
7. SetupUiState.Success → Navigate("login")
   ↓
8. LoginScreen wyświetla się
   ↓
9. Retrofit używa Base URL: "https://api.myrestaurant.com/"
   ↓
10. Login request: POST https://api.myrestaurant.com/client/v3/api/mobile/login
    ✅ DZIAŁA!
```

---

## Alternatywy

### Opcja 1: Deep Link

```kotlin
// AndroidManifest.xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data
        android:scheme="myapp"
        android:host="setup" />
</intent-filter>

// Link: myapp://setup?baseUrl=https://api.myrestaurant.com/&tenantKey=rest123
```

### Opcja 2: NFC Tag

```kotlin
// Czytanie NFC
override fun onNewIntent(intent: Intent) {
    if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
        val ndefMessage = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        // Parse i zapisz Base URL
    }
}
```

---

## Podsumowanie

✅ **Base URL MUSI być skonfigurowane PRZED logowaniem**  
✅ Ekran setup jako `startDestination` jeśli Base URL to localhost  
✅ Walidacja i normalizacja przez AppPreferencesManager  
✅ Opcje: ręczne wpisanie, QR code, deep link, NFC  
✅ Po zapisie → restart lub przejdź do ekranu logowania  

**Ten pattern zapewnia że użytkownik zawsze ma poprawnie skonfigurowany Base URL przed pierwszą próbą logowania!** 🎯

