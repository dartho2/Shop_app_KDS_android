# 🐛 DEBUG: Portale nie są przekazywane w request body

## Problem
Request do `PUT /client/v3/api/admin/openhours` nie zawiera pola `portals` w JSON body, mimo że powinien.

## Dodane logowanie
W `OpenHoursRepository.setClosed()` dodano logi Timber:
```kotlin
timber.log.Timber.d("🔄 setClosed REQUEST: closed=$closed, portals=$portals")
timber.log.Timber.d("🔄 Request body: isOpen=${requestBody.isOpen}, portals=${requestBody.portals}")
```

## Instrukcje testowania

### Krok 1: Zbuduj aplikację
```bash
cd "L:\SHOP APP"
.\gradlew assembleDebug
```

### Krok 2: Zainstaluj i uruchom
Zainstaluj APK na urządzeniu i uruchom aplikację.

### Krok 3: Test scenariusz
1. **Kliknij chip statusu restauracji** w top bar (zielony/czerwony przycisk)
2. **Czy pokazuje się bottom sheet** z opcjami? ✅
3. **Kliknij "Otwórz sklep"** (opcja z zielonym obramowaniem)
4. **❓ CZY POKAZUJE SIĘ DIALOG WYBORU PORTALI?**
   - **TAK** → Przejdź do kroku 5
   - **NIE** → Problem w `RestaurantStatusSheet` lub `OpenCloseStoreDialog`
5. **Wybierz portal** (np. UBER, GLOVO)
6. **Kliknij "Potwierdź"**
7. **Sprawdź logi** w Logcat (filtr: "setClosed")

### Krok 4: Analiza logów

#### Scenario A: Dialog się pokazuje i wybierasz portal
**Oczekiwane logi:**
```
D/OpenHoursRepository: 🔄 setClosed REQUEST: closed=false, portals=[UBER]
D/OpenHoursRepository: 🔄 Request body: isOpen=true, portals=[UBER]
I/okhttp.OkHttpClient: --> PUT /client/v3/api/admin/openhours
I/okhttp.OkHttpClient: {"is_open":true,"portals":["UBER"]}
```

**Jeśli logi pokazują `portals=null`:**
→ Problem w `TopBarConfigBuilder.kt` - portale nie są przekazywane z dialogu

**Jeśli logi pokazują `portals=[UBER]` ALE request body nie zawiera `portals`:**
→ Problem w Gson serializacji - Gson pomija pole (prawdopodobnie bo jest nullable)

#### Scenario B: Dialog się NIE pokazuje
**Możliwe przyczyny:**
1. `showOpenDialog` / `showCloseDialog` nie są ustawiane na `true`
2. `OpenCloseStoreDialog` nie jest renderowany
3. Import `OpenCloseStoreDialog` jest błędny

**Sprawdź:**
```kotlin
// W RestaurantStatusSheet.kt linijka ~85-90
onClick = { showOpenDialog = true }  // <-- Czy to jest?
```

## Możliwe rozwiązania

### Rozwiązanie 1: Gson nie serializuje null
Jeśli problem jest w Gson, możemy:
1. Skonfigurować Gson aby serializował `null` values
2. Lub zmienić API aby używało pustej listy `[]` zamiast `null`

```kotlin
// W NetworkModule.kt
@Provides
@Singleton
fun provideGson(): Gson = GsonBuilder()
    .serializeNulls()  // <-- Dodaj to
    .create()

@Provides
@Singleton
fun provideRetrofitBuilder(gson: Gson): Retrofit.Builder {
    return Retrofit.Builder()
        .baseUrl("http://placeholder.com/")
        .addConverterFactory(GsonConverterFactory.create(gson))  // <-- Użyj custom Gson
}
```

### Rozwiązanie 2: Dialog nie jest wywoływany
Sprawdź czy `OpenCloseStoreDialog` jest poprawnie zaimportowany i renderowany:

```kotlin
// W RestaurantStatusSheet.kt
import com.itsorderchat.ui.theme.status.OpenCloseStoreDialog  // <-- Import

// Na końcu funkcji, przed zamknięciem }
if (showOpenDialog) {
    OpenCloseStoreDialog(
        isOpenAction = true,
        onDismiss = { showOpenDialog = false },
        onConfirm = { portals ->
            onOpen(portals)  // <-- Czy to jest wywoływane?
            showOpenDialog = false
        }
    )
}
```

### Rozwiązanie 3: Backend nie obsługuje `portals`
Jeśli backend API nie obsługuje pola `portals` w `PUT /admin/openhours`, ale tylko w `POST /admin/openhours/pause`, to możemy:
1. Sprawdzić dokumentację API
2. Stworzyć nowy endpoint lub
3. Użyć pauzy jako workaround

## Następne kroki
1. Uruchom aplikację i wykonaj test
2. Sprawdź logi
3. Jeśli dialog się nie pokazuje → napraw `RestaurantStatusSheet`
4. Jeśli portale są `null` w logach → napraw przekazywanie z dialogu
5. Jeśli Gson pomija pole → dodaj `serializeNulls()` do Gson konfiguracji

