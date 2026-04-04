# ✅ Aktualizacja Dokumentacji - Base URL

## 🔧 Co Zostało Dodane?

### Nowa Sekcja: "Konfiguracja Base URL"

Dodano kompleksową dokumentację dotyczącą:

#### 1. **Skąd Bierze Się Base URL?**
```kotlin
// Przechowywane w DataStore przez AppPreferencesManager
suspend fun getBaseUrl(): String =
    dataStore.data.map { it[Keys.BASE_URL] ?: "https://localhost:8001/" }.first()
```

**Kluczowe:**
- Base URL jest przechowywany w **DataStore** (trwałe)
- Domyślna wartość: `"https://localhost:8001/"`
- Może być **dynamicznie zmieniony** przez użytkownika

---

#### 2. **Jak Jest Dołączany do Retrofit?**
```kotlin
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
        .baseUrl(baseUrl)  // ✅ Tutaj jest dołączany!
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
```

**Kluczowe:**
- `runBlocking` - Pobiera Base URL synchronicznie przy inicjalizacji
- Base URL + Endpoint = Pełny URL requestu
- Przykład: `"https://api.example.com/"` + `"client/v3/api/mobile/login"` = `"https://api.example.com/client/v3/api/mobile/login"`

---

#### 3. **Walidacja i Normalizacja**

AppPreferencesManager automatycznie:
- ✅ Dodaje `https://` jeśli brakuje
- ✅ Zachowuje `http://` jeśli użytkownik go podał
- ✅ Dodaje trailing slash `/` (wymagane przez Retrofit)
- ✅ Waliduje czy URL nie jest pusty

**Przykłady:**
```kotlin
"api.example.com" → "https://api.example.com/"
"http://192.168.1.100:8001" → "http://192.168.1.100:8001/"
"https://api.example.com" → "https://api.example.com/"
```

---

#### 4. **Zmiana Base URL w Runtime**

```kotlin
// SettingsViewModel.kt
fun updateBaseUrl(newUrl: String) {
    viewModelScope.launch {
        preferencesManager.setBaseUrl(newUrl)
        _event.emit(SettingsEvent.Success("URL zapisany"))
    }
}
```

**⚠️ Ważne:** Zmiana Base URL wymaga **restartu aplikacji**, ponieważ Retrofit jest inicjalizowany raz przy starcie.

---

#### 5. **⚠️ KLUCZOWE: Base URL PRZED Logowaniem!**

**Problem:**
Base URL jest potrzebne **PRZED pierwszym logowaniem**, ponieważ sam endpoint logowania wymaga Base URL!

**Flow Pierwszego Uruchomienia:**

```
1. Użytkownik instaluje aplikację
   ↓
2. Base URL = "https://localhost:8001/" (domyślny - NIE DZIAŁA!)
   ↓
3. Użytkownik próbuje się zalogować
   ↓
4. Request idzie do: https://localhost:8001/client/v3/api/mobile/login
   ↓
5. ❌ BŁĄD: Connection failed (localhost nie istnieje)
```

**Rozwiązanie 1: Ekran Konfiguracji Przed Logowaniem (ZALECANE)**

```kotlin
// App Flow
@Composable
fun AppNavigation() {
    val baseUrl by preferencesManager.baseUrlFlow.collectAsState(initial = "")
    
    NavHost(
        startDestination = when {
            baseUrl.isEmpty() || baseUrl.contains("localhost") -> "setup"
            else -> "login"
        }
    ) {
        // Ekran konfiguracji (pierwszy raz)
        composable("setup") {
            SetupScreen(
                onConfigured = { navController.navigate("login") }
            )
        }
        
        // Ekran logowania
        composable("login") {
            LoginScreen()
        }
    }
}

// SetupScreen.kt
@Composable
fun SetupScreen(
    onConfigured: () -> Unit
) {
    var serverUrl by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Konfiguracja serwera",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Adres serwera") },
            placeholder = { Text("api.myrestaurant.com") },
            isError = error != null,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Przykłady
        Text(
            text = "Przykłady:",
            style = MaterialTheme.typography.labelMedium
        )
        Text("• api.itsorderchat.com")
        Text("• 192.168.1.100:8001")
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                viewModel.saveBaseUrl(serverUrl) { success ->
                    if (success) {
                        onConfigured()
                    } else {
                        error = "Nieprawidłowy adres serwera"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kontynuuj")
        }
    }
}
```

**Rozwiązanie 2: Hardcoded Base URL (dla konkretnej restauracji)**

```kotlin
// AppPreferencesManager.kt
suspend fun getBaseUrl(): String =
    dataStore.data.map { 
        it[Keys.BASE_URL] ?: "https://api.itsorderchat.com/"  // ✅ Produkcyjny URL
    }.first()

// LUB w build.gradle
android {
    buildTypes {
        debug {
            buildConfigField "String", "BASE_URL", "\"https://staging-api.example.com/\""
        }
        release {
            buildConfigField "String", "BASE_URL", "\"https://api.example.com/\""
        }
    }
}

// NetworkModule.kt
@Provides
@Singleton
fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
    return Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)  // ✅ Z build config
        .client(okHttpClient)
        .build()
}
```

**Rozwiązanie 3: QR Code Scanner (profesjonalne)**

```kotlin
// SetupScreen.kt
Button(onClick = { 
    // Skanuj QR z konfiguracją
    qrScannerLauncher.launch()
}) {
    Icon(Icons.Default.QrCode, contentDescription = null)
    Text("Skanuj QR z serwera")
}

// QR Code zawiera JSON:
// {
//   "baseUrl": "https://api.myrestaurant.com/",
//   "tenantKey": "restaurant123"
// }
```

---

#### 6. **Przykłady Base URL**

**Produkcja:**
```kotlin
"https://api.itsorderchat.com/"
```

**Staging:**
```kotlin
"https://staging-api.itsorderchat.com/"
```

**Lokalny development:**
```kotlin
"http://192.168.1.100:8001/"
```

**Localhost (emulator Android):**
```kotlin
"http://10.0.2.2:8001/"  // 10.0.2.2 = localhost w emulatorze
```

---

#### 6. **Flow Zmiany Base URL**

```
User wprowadza: "api.myrestaurant.com"
  ↓
setBaseUrl()
  ├─> Normalizacja
  ├─> Dodaj https://
  ├─> Dodaj trailing /
  └─> Zapisz do DataStore: "https://api.myrestaurant.com/"
  ↓
⚠️ Retrofit NIE aktualizuje się automatycznie
  ↓
Rozwiązanie: Restart aplikacji
```

---

### Zaktualizowano Sekcje

#### Spis Treści
- Dodano punkt 7: "Konfiguracja Base URL"

#### AppPreferencesManager Keys
```kotlin
private object Keys {
    val ACCESS_TOKEN = stringPreferencesKey("access_token")
    val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    val REFRESH_TOKEN_ID = stringPreferencesKey("refresh_token_id")
    val TENANT_KEY = stringPreferencesKey("tenant_key")
    val USER_ID = stringPreferencesKey("user_id")
    val USER_ROLE = stringPreferencesKey("user_role")
    val BASE_URL = stringPreferencesKey("base_url")  // ✅ DODANO
}
```

#### Implementacja Krok Po Kroku
- Rozszerzono krok 4 (AppPreferencesManager) o implementację Base URL
- Dodano szczegóły do konfiguracji Retrofit

#### Troubleshooting
- Dodano Problem 6: "Base URL Się Nie Zmienia Po Zapisaniu"
- Rozwiązania: Restart aplikacji (zalecane) lub Dynamiczny Retrofit (zaawansowane)

---

## 📊 Podsumowanie

**Teraz dokumentacja wyjaśnia:**

✅ Skąd bierze się Base URL (DataStore)  
✅ Jak jest przechowywany (AppPreferencesManager)  
✅ Jak jest dołączany do Retrofit (NetworkModule)  
✅ Jak jest normalizowany (https://, trailing slash)  
✅ Jak go zmienić (setBaseUrl + restart)  
✅ Przykłady różnych środowisk (prod, staging, local)  
✅ Troubleshooting (problem ze zmianą URL)  
✅ **KLUCZOWE: Base URL MUSI być skonfigurowane PRZED logowaniem!** ⭐

**Typowe Flow:**
```
1. Pierwszy uruchomienie → Ekran konfiguracji serwera
2. Użytkownik wpisuje Base URL (lub skanuje QR)
3. Zapisz do DataStore
4. Restart lub reinicjalizacja Retrofit
5. Ekran logowania (teraz może działać!)
```

**Dokumentacja jest teraz kompletna i wyjaśnia wszystkie aspekty konfiguracji serwera, w tym kolejność inicjalizacji!** ✅

---

**Data aktualizacji:** 2026-02-06  
**Dodano sekcję:** Konfiguracja Base URL  
**Zaktualizowano:** Spis treści, Keys, Implementacja, Troubleshooting

