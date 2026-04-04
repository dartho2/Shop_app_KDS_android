# 📋 WDRAŻANIE SYSTEMU ZARZĄDZANIA WIELOMA DRUKARKAMI

## 🎯 Cel
Zastąpić stary system z wpisanym na sztywno **jedną drukarka** (Settings → Printer Settings) na **nowy, elastyczny system** pozwalający na:
- ✅ Nieograniczoną liczbę drukarek
- ✅ CRUD operacje (dodaj, edytuj, usuń, zmień kolejność)
- ✅ Predefiniowane profile drukarek (POS-8390 DUAL, Mobile SSP, Custom)
- ✅ Wybór szablonu wydruku na drukarkę
- ✅ Sekwencyjne drukowanie na wielu drukarkach
- ✅ Kolejkowanie zadań drukowania

---

## 📦 ETAP 1: Modele Danych

### Tworzone pliki:

#### 1. `Printer.kt` (Model główny)
**Ścieżka:** `data/model/Printer.kt`

Reprezentuje pojedynczą drukarkę w systemie:
```kotlin
@Serializable
data class Printer(
    val id: String,              // UUID
    val name: String,            // "Drukarka Główna", "Kuchnia"
    val deviceId: String,        // MAC BT (AB:0D:6F:E2:85:D7)
    val profileId: String,       // profile_pos_8390_dual, profile_mobile_ssp, profile_custom
    val templateId: String,      // template_standard, template_compact, template_kitchen_only
    val encoding: String,        // Cp852, UTF-8, Cp437
    val codepage: Int?,          // 13 (PC852), null (UTF-8)
    val autoCut: Boolean,        // automatyczne cięcie papieru
    val enabled: Boolean,        // drukarka aktywna
    val order: Int               // kolejność drukowania
)
```

**Metody:**
- `getProfileDisplayName()`: Zwraca czytelną nazwę profilu
- `getTemplateDisplayName()`: Zwraca czytelną nazwę szablonu
- `isDualMode()`: Czy drukarka jest DUAL mode
- `toPrinterSettings()`: Konwersja na legacy PrinterSettings (backward compat)

#### 2. `PrinterProfile.kt` (Predefiniowane profile)
**Ścieżka:** `data/model/PrinterProfile.kt`

Enum z domyślnymi ustawieniami dla znanych modeli:
```kotlin
enum class PrinterProfile(
    val id: String,
    val displayName: String,
    val encoding: String,
    val codepage: Int?,
    val autoCut: Boolean
) {
    POS_8390_DUAL("profile_pos_8390_dual", "POS-8390 (DUAL)", "Cp852", 13, true),
    MOBILE_SSP("profile_mobile_ssp", "Mobile (SSP)", "UTF-8", null, false),
    CUSTOM("profile_custom", "Niestandardowy", "UTF-8", null, false)
}
```

**Metody:**
- `fromId(id: String)`: Pobranie profilu po ID
- `getAllProfiles()`: Lista wszystkich profi profili

#### 3. `PrinterPreferences.kt` (Persistencja)
**Ścieżka:** `data/preferences/PrinterPreferences.kt`

Singleton obsługujący zapis/odczyt drukarek z SharedPreferences (JSON):
```kotlin
object PrinterPreferences {
    suspend fun getPrinters(context: Context): List<Printer>
    suspend fun addPrinter(context: Context, printer: Printer)
    suspend fun updatePrinter(context: Context, id: String, printer: Printer)
    suspend fun deletePrinter(context: Context, id: String)
    suspend fun reorderPrinters(context: Context, fromIndex: Int, toIndex: Int)
    suspend fun toggleEnabled(context: Context, id: String)
}
```

**Klucz w SharedPrefs:** `printers_list_json`

#### 4. `PrinterMigration.kt` (Migracja danych)
**Ścieżka:** `data/preferences/PrinterMigration.kt`

Singleton migrujący stare ustawienia do nowego systemu (tylko raz):
```kotlin
object PrinterMigration {
    fun migrateOldPrintersToNewSystem(context: Context)
}
```

**Co robi:**
1. Sprawdza czy migracja już wykonana (`printers_migrated_v1`)
2. Jeśli nie, pobiera stare ustawienia z `old_printer_id`, `old_encoding`, `old_codepage`
3. Tworzy `Printer` z ID 1000
4. Zapisuje w nowym formacie (`printers_list_json`)
5. Ustawia flagę `printers_migrated_v1 = true`

---

## 🎨 ETAP 2: UI - ViewModel i Compose

### Tworzone pliki:

#### 1. `PrintersViewModel.kt`
**Ścieżka:** `ui/settings/printer/PrintersViewModel.kt`

HiltViewModel obsługujący:
```kotlin
@HiltViewModel
class PrintersViewModel @Inject constructor(...) : ViewModel() {
    val printers: StateFlow<List<Printer>>
    val isLoading: StateFlow<Boolean>
    val errorMessage: StateFlow<String?>
    
    fun loadPrinters()
    fun addPrinter(printer: Printer)
    fun updatePrinter(id: String, printer: Printer)
    fun deletePrinter(id: String)
    fun toggleEnabled(id: String)
}
```

#### 2. `PrintersListScreen.kt`
**Ścieżka:** `ui/settings/printer/PrintersListScreen.kt`

Główny ekran zarządzania drukarkami:
- **TopAppBar**: Powrót, tytuł
- **FAB**: Dodaj drukarkę (+)
- **LazyColumn**: Lista drukarek z:
  - Numerem kolejności
  - Nazwą i profilem
  - Przyciskami: Edytuj, Usuń
  - Switchem: Aktywna/Nieaktywna

#### 3. `AddEditPrinterDialog.kt`
**Ścieżka:** `ui/settings/printer/AddEditPrinterDialog.kt`

Dialog dodawania/edycji drukarki z:
- **TextField**: Nazwa drukarki
- **Dropdown**: Wybór urządzenia BT (lista sparowanych)
- **Dropdown**: Wybór profilu drukarki
- **TextField** (Custom only): Custom encoding
- **TextField** (Custom only): Custom codepage
- **Dropdown**: Szablon wydruku
- **Switch**: Automatyczne cięcie papieru
- **Switch**: Drukarka aktywna

---

## 🔗 ETAP 3: Integracja w Nawigacji

### Zmiany w HomeActivity:

#### 1. AppDestinations
```kotlin
object AppDestinations {
    // ...existing...
    const val PRINTERS_LIST = "printers_list"  // Nowy ekran
}
```

#### 2. AppNavHost (NavGraph)
```kotlin
composable(AppDestinations.PRINTERS_LIST) {
    PrintersListScreen(navController = navController)
}
```

#### 3. SettingsMainScreen
```kotlin
fun SettingsMainScreen(
    onNavigateToPrinterSettings: () -> Unit,
    onNavigateToOpenHours: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToPrintersList: () -> Unit,  // Nowy callback
    modifier: Modifier = Modifier
)
```

**Nowa pozycja w liście:**
```
Zarządzaj drukarkami
Dodaj, edytuj i zarządzaj wieloma drukarkami
```

### Callback w HomeActivity
```kotlin
composable(AppDestinations.SETTINGS_MAIN) {
    SettingsMainScreen(
        // ...existing...
        onNavigateToPrintersList = { navController.navigate(AppDestinations.PRINTERS_LIST) }
    )
}
```

---

## 📋 Checklist Wdrażania

- [x] ETAP 1: Tworzyć modele danych (Printer, PrinterProfile, PrinterPreferences, PrinterMigration)
- [x] ETAP 2: Tworzyć UI (PrintersViewModel, PrintersListScreen, AddEditPrinterDialog)
- [x] ETAP 3: Integrować nawigację (AppDestinations, routing)
- [x] **ETAP 4**: Integrować z PrinterService do drukowania
- [ ] **ETAP 5**: Testy i walidacja
- [ ] **ETAP 6**: Dokumentacja dla użytkownika

---

## 🚀 ETAP 4: Integracja PrinterService (UKOŃCZONE)

### Dodana metoda `printOrderOnAllEnabledPrinters()`

**Lokalizacja:** `PrinterService.kt`

```kotlin
suspend fun printOrderOnAllEnabledPrinters(
    order: Order,
    useDeliveryInterval: Boolean = false
)
```

**Funkcjonalność:**
1. Pobiera wszystkie drukarki z `PrinterPreferences.getPrinters(context)`
2. Filtruje włączone drukarki i sortuje po `order`
3. Iteruje sekwencyjnie:
   - Drukuje na każdej drukarce
   - 2000ms timeout BT cleanup między drukárkami
   - Retry logic dla drukarek DUAL
   - Obsługa błędów - kontynuuje mimo błędu
4. Zwraca UI feedback via Toast

**Logowanie:**
```
🖨️  printOrderOnAllEnabledPrinters: START
📊 printOrderOnAllEnabledPrinters: Znaleziono X włączonych drukarek
🔄 printOrderOnAllEnabledPrinters: Drukuję [1/X] Drukarka1
✅ printOrderOnAllEnabledPrinters: [1/X] OK
⏳ printOrderOnAllEnabledPrinters: Czekam 2000ms...
🔄 printOrderOnAllEnabledPrinters: Drukuję [2/X] Drukarka2
🖨️  printOrderOnAllEnabledPrinters: END
```

---

## 🚀 Następne Kroki (ETAP 5 - Testy i Valadacja)

1. **Build i sprawdzenie błędów**:
   ```bash
   ./gradlew clean build
   ```

2. **Manualne testy**:
   - Przechodź do Settings → Zarządzaj drukarkami
   - Dodaj nową drukarkę (test z istniejącą drukárką BT)
   - Edytuj ustawienia
   - Usuń drukarkę
   - Zmień kolejność drag-drop

3. **Testy drukowania**:
   - Drukuj na jednej drukarce
   - Drukuj na dwóch drukarkach sekwencyjnie
   - Sprawdź logi w Logcat
   - Sprawdź timeouty między drukárkami

4. **Regression testy**:
   - Old PrinterService (`printAfterOrderAccepted`) powinien nadal działać
   - Backward compatibility z legacy systemem

---

## 📚 DOKUMENTACJA KOŃCOWA

**Wersja:** 1.0  
**Data:** 2026-01-22  
**Status:** ETAP 1-4 Ukończony ✅  
**Przygotowywane:** ETAP 5 (Testy)

