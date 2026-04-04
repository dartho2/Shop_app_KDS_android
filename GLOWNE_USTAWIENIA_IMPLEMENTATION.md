# ✅ IMPLEMENTACJA: Główne Ustawienia w Settings

## 🎯 CO ZOSTAŁO ZROBIONE?

Dodana nowa sekcja **"Główne ustawienia"** w ekranie Settings z własnym ekranem konfiguracyjnym. Zgodnie ze strukturą i wyglądem reszty ustawień aplikacji.

---

## 🔧 ZMIANY W KODZIE

### 1. Dodanie nowego parametru w SettingsMainScreen

**Plik**: `l:\SHOP APP\app\src\main\java\com\itsorderchat\ui\settings\SettingsScreen.kt` (linie 65-75)

```kotlin
@Composable
fun SettingsMainScreen(
    onNavigateToOpenHours: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToPrintersList: () -> Unit,
    onNavigateToPrintSettings: () -> Unit,
    onNavigateToMainSettings: () -> Unit,  // ← NOWE
    modifier: Modifier = Modifier
)
```

### 2. Dodanie pozycji "Główne ustawienia" na górze listy

**Plik**: `SettingsScreen.kt` (linie 85-93)

```kotlin
// 🎯 NOWE: Główne ustawienia na górze
item {
    SettingsItem(
        icon = Icons.Default.Settings,
        title = "Główne ustawienia",
        subtitle = "Podstawowe ustawienia aplikacji i terminala",
        onClick = onNavigateToMainSettings
    )
}
```

### 3. Nowy ekran MainSettingsScreen

**Plik**: `SettingsScreen.kt` (linie 289-373)

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Główne ustawienia") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, ...)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(...) {
            item {
                Text("Konfiguracja Terminala", ...)
            }
            
            item {
                SettingsItemWithSwitch(
                    icon = Icons.Default.Notifications,
                    title = "Tryb Kiosk",
                    subtitle = "Blokada aplikacji na terminalu restauracyjnym",
                    isEnabled = true,
                    onToggle = { }
                )
            }
            
            item {
                SettingsItemWithSwitch(
                    icon = Icons.Default.Settings,
                    title = "Auto-restart",
                    subtitle = "Automatyczne uruchomienie aplikacji",
                    isEnabled = true,
                    onToggle = { }
                )
            }
            
            item {
                Text("Inne", ...)
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Settings,
                    title = "O aplikacji",
                    subtitle = "Wersja i informacje",
                    onClick = { }
                )
            }
        }
    }
}
```

### 4. Komponent SettingsItemWithSwitch

**Plik**: `SettingsScreen.kt` (linie 375-408)

```kotlin
@Composable
fun SettingsItemWithSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(...)
        Spacer(...)
        Column(...) {
            Text(title, ...)
            Text(subtitle, ...)
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            ...
        )
    }
    HorizontalDivider(...)
}
```

### 5. Dodanie destination w AppDestinations

**Plik**: `HomeActivity.kt` (linia 137)

```kotlin
const val SETTINGS_MAIN_CONFIG = "settings_main_config"  // ← NOWE
```

### 6. Konfiguracja nawigacji w HomeActivity

**Plik**: `HomeActivity.kt` (linie 733-743)

```kotlin
composable(AppDestinations.SETTINGS_MAIN) {
    SettingsMainScreen(
        ...
        onNavigateToMainSettings = { navController.navigate(AppDestinations.SETTINGS_MAIN_CONFIG) }
    )
}

// 🎯 NOWE: Główne ustawienia
composable(AppDestinations.SETTINGS_MAIN_CONFIG) {
    MainSettingsScreen(onNavigateBack = { navController.navigateUp() })
}
```

### 7. Import w HomeActivity

**Plik**: `HomeActivity.kt` (linia 104)

```kotlin
import com.itsorderchat.ui.settings.MainSettingsScreen  // ← NOWE
```

---

## 📱 WYGLĄD EKRANU

### Główny ekran Settings (lista):
```
┌─────────────────────────────────┐
│  Główne ustawienia       →       │  ← NOWE (na górze)
├─────────────────────────────────┤
│  Ustawienia drukowania   →       │
├─────────────────────────────────┤
│  Zarządzaj drukarkami    →       │
├─────────────────────────────────┤
│  Powiadomienia           →       │
├─────────────────────────────────┤
│  Godziny otwarcia        →       │
└─────────────────────────────────┘
```

### Ekran "Główne ustawienia":
```
┌─────────────────────────────────┐
│  < Główne ustawienia            │
├─────────────────────────────────┤
│                                 │
│  Konfiguracja Terminala         │
│                                 │
│  🔔 Tryb Kiosk          [ON]    │
│     Blokada aplikacji...        │
├─────────────────────────────────┤
│  ⚙️  Auto-restart        [ON]   │
│     Automatyczne uruchom...    │
├─────────────────────────────────┤
│                                 │
│  Inne                           │
│                                 │
│  ⚙️  O aplikacji         →      │
│     Wersja i informacje         │
├─────────────────────────────────┤
└─────────────────────────────────┘
```

---

## ✅ STRUKTURA I KONWENCJE

### Zgodne z resztą Settings:

✅ **Komponent SettingsItem** - dla pozycji do klikania  
✅ **Komponent SettingsItemWithSwitch** - dla pozycji z toggle'em  
✅ **TopAppBar** z przyciskiem Back  
✅ **LazyColumn** dla listy  
✅ **HorizontalDivider** między pozycjami  
✅ **Ikony Material3** (Icons.Default.*)  
✅ **Styling** (MaterialTheme.colorScheme, typography)

### Gotowe do rozbudowy:

- `onToggle` callbacks w SettingsItemWithSwitch (na razie: `{ /* TODO */ }`)
- Obsługa stanu (preferences, ViewModel)
- Dodawanie nowych opcji

---

## 🚀 NASTĘPNE KROKI

### Aby rozbudować Główne ustawienia:

1. **Dodaj ViewModel** dla Głównych ustawień:
```kotlin
@HiltViewModel
class MainSettingsViewModel @Inject constructor(
    private val preferencesManager: AppPreferencesManager
) : ViewModel() {
    // Stan dla ustawień
}
```

2. **Powiąż toggle'i z preferencjami**:
```kotlin
var kioskModeEnabled by remember { mutableStateOf(prefs.isKioskModeEnabled()) }

SettingsItemWithSwitch(
    ...
    isEnabled = kioskModeEnabled,
    onToggle = { 
        kioskModeEnabled = it
        viewModel.setKioskMode(it)
    }
)
```

3. **Dodaj nowe opcje** w LazyColumn:
```kotlin
item {
    SettingsItemWithSwitch(
        icon = Icons.Default.Security,
        title = "Nowa opcja",
        subtitle = "Opis opcji",
        isEnabled = state,
        onToggle = { viewModel.setOption(it) }
    )
}
```

---

## 🎯 PODSUMOWANIE

| Element | Status |
|---------|--------|
| Pozycja w Settings | ✅ DODANE (górze listy) |
| Ekran MainSettingsScreen | ✅ GOTOWY |
| SettingsItemWithSwitch | ✅ GOTOWY |
| Nawigacja | ✅ SKONFIGUROWANA |
| Wygląd/Styl | ✅ ZGODNY |
| TODO: ViewModel | ⏳ DO DODANIA |
| TODO: Preferencje | ⏳ DO DODANIA |

---

**Data implementacji**: 2026-01-29  
**Status**: ✅ GOTOWE - STRUKTURA I WYGLĄD SKOŃCZONE  
**Pliki zmienione**: 2 (SettingsScreen.kt, HomeActivity.kt)  
**Następny krok**: Powiązanie z ViewModelem i preferencjami

