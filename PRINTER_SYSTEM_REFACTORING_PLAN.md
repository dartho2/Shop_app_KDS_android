# Plan Refaktoryzacji Systemu Drukarek

## 🎯 Cel
Zmiana z systemu **dwóch predefiniowanych drukarek (STANDARD/KITCHEN)** na **elastyczną listę drukarek z profilami**.

---

## 📋 Obecny Model vs. Nowy Model

### ❌ Obecny Model (do usunięcia)
```kotlin
enum class PrinterType { STANDARD, KITCHEN }

AppPrefs.getPrinterSettings(PrinterType.STANDARD) → PrinterSettings
AppPrefs.getPrinterSettings(PrinterType.KITCHEN) → PrinterSettings
```
- Tylko 2 drukarki (hardcoded)
- Każda ma osobne klucze w SharedPreferences
- Brak możliwości dodania 3. drukarki

### ✅ Nowy Model (docelowy)
```kotlin
data class Printer(
    val id: String,              // UUID
    val name: String,            // np. "Drukarka Główna", "Kuchnia Gorące"
    val deviceId: String,        // MAC address BT lub USB path
    val profileId: String,       // "profile_pos_8390_dual", "profile_mobile_ssp"
    val templateId: String,      // "template_standard", "template_compact"
    val encoding: String,        // "Cp852", "UTF-8"
    val codepage: Int?,          // 13 (PC852) lub null
    val autoCut: Boolean,        // true/false
    val enabled: Boolean,        // aktywna czy nie
    val order: Int               // kolejność drukowania (1, 2, 3...)
)

AppPrefs.getPrinters() → List<Printer>
```
- **Nieograniczona liczba drukarek**
- **Profile** (gotowe zestawy ustawień)
- **Szablony** (layout wydruku)
- **Kolejność drukowania** (order)

---

## 🗂️ Etapy Wdrożenia

### **ETAP 1: Nowe Modele Danych** ✅
**Pliki do stworzenia:**
1. `app/src/main/java/com/itsorderchat/data/model/Printer.kt`
   - Data class `Printer` (jak wyżej)
   - `fun toPrinterSettings(): PrinterSettings` (backward compat)

2. `app/src/main/java/com/itsorderchat/data/model/PrinterProfile.kt`
   - `enum class PrinterProfile` (gotowe profile)
   - Przykłady:
     - `PROFILE_POS_8390_DUAL` (Cp852, codepage=13, autoCut=true)
     - `PROFILE_MOBILE_SSP` (UTF-8, codepage=null, autoCut=false)
     - `PROFILE_CUSTOM` (użytkownik sam ustawi wszystko)

3. `app/src/main/java/com/itsorderchat/data/preferences/PrinterPreferences.kt`
   - `fun getPrinters(): List<Printer>` (deserializacja z JSON w SharedPreferences)
   - `fun savePrinters(printers: List<Printer>)` (serializacja do JSON)
   - `fun addPrinter(printer: Printer)`
   - `fun updatePrinter(id: String, printer: Printer)`
   - `fun deletePrinter(id: String)`
   - `fun reorderPrinters(fromIndex: Int, toIndex: Int)`

**Klucz w SharedPreferences:**
```kotlin
"printers_list_json" → JSON array List<Printer>
```

---

### **ETAP 2: Migracja Danych (Backward Compatibility)** ✅
**Plik do stworzenia:**
1. `app/src/main/java/com/itsorderchat/data/preferences/PrinterMigration.kt`
   - `fun migrateOldPrintersToNewSystem()`
   - Logika:
     1. Sprawdź czy istnieje `"printers_list_json"` → jeśli TAK, skip migracji
     2. Jeśli NIE:
        - Wczytaj stare ustawienia STANDARD → utwórz Printer(id=UUID, name="Drukarka Główna", ...)
        - Wczytaj stare ustawienia KITCHEN → utwórz Printer(id=UUID, name="Drukarka Kuchenna", ...)
        - Zapisz jako `List<Printer>` w nowym formacie
        - Usuń stare klucze (opcjonalnie)

**Wywołanie migracji:**
- W `MainActivity.onCreate()` lub `Application.onCreate()`
- Tylko raz, przy pierwszym uruchomieniu po update

---

### **ETAP 3: UI - Lista Drukarek** ✅
**Pliki do stworzenia/modyfikacji:**

1. **Nowy screen: `PrintersListScreen.kt`**
   - `@Composable fun PrintersListScreen(navController, viewModel)`
   - Lista drukarek (LazyColumn):
     - Nazwa drukarki
     - Status (aktywna/nieaktywna)
     - Przycisk "Edytuj"
     - Przycisk "Usuń"
     - Drag-and-drop do zmiany kolejności (order)
   - FloatingActionButton: "Dodaj drukarkę" → otwiera modal

2. **Nowy modal: `AddEditPrinterDialog.kt`**
   - `@Composable fun AddEditPrinterDialog(printer: Printer?, onSave, onDismiss)`
   - Pola:
     - **Nazwa drukarki** (TextField)
     - **Wybór urządzenia BT** (Dropdown z listy sparowanych)
     - **Profil drukarki** (Dropdown: POS-8390 DUAL, Mobile SSP, Custom)
       - Jeśli Custom → pokaż pola: encoding, codepage, autoCut
       - Jeśli gotowy profil → auto-fill z PrinterProfile enum
     - **Szablon wydruku** (Dropdown: Standard, Compact, Kitchen Only)
     - **Cięcie papieru** (Switch: TAK/NIE)
     - **Aktywna** (Switch: TAK/NIE)
   - Przyciski:
     - "Zapisz" → `viewModel.savePrinter(printer)`
     - "Anuluj" → dismiss

3. **ViewModel: `PrintersViewModel.kt`**
   - `val printers = MutableStateFlow<List<Printer>>(emptyList())`
   - `fun loadPrinters()` → wczytaj z PrinterPreferences
   - `fun addPrinter(printer: Printer)` → dodaj + zapisz
   - `fun updatePrinter(id: String, printer: Printer)` → zaktualizuj + zapisz
   - `fun deletePrinter(id: String)` → usuń + zapisz
   - `fun reorderPrinters(fromIndex: Int, toIndex: Int)` → zmień order + zapisz
   - `fun toggleEnabled(id: String)` → aktywuj/dezaktywuj

4. **Nawigacja:**
   - W `AppDestinations.kt`: dodaj `const val PRINTERS_LIST = "printers_list"`
   - W `HomeActivity.kt` → `AppNavHost`: dodaj route
     ```kotlin
     composable(AppDestinations.PRINTERS_LIST) {
         PrintersListScreen(navController, viewModel)
     }
     ```
   - W `SettingsScreen.kt`: dodaj pozycję "Zarządzaj drukarkami" → navigate do PRINTERS_LIST

---

### **ETAP 4: Refaktoryzacja PrinterService** ✅
**Plik do modyfikacji:**
1. `PrinterService.kt`
   - **Usunąć:** `enum class PrinterType { STANDARD, KITCHEN }`
   - **Zmienić:**
     ```kotlin
     // Stare:
     fun printOrder(order: Order, target: PrinterType)
     
     // Nowe:
     fun printOrder(order: Order, printer: Printer)
     ```
   - **Nowa funkcja:**
     ```kotlin
     suspend fun printAfterOrderAccepted(order: Order) {
         val enabledPrinters = PrinterPreferences.getPrinters()
             .filter { it.enabled }
             .sortedBy { it.order }
         
         for (printer in enabledPrinters) {
             try {
                 printOrder(order, printer)
                 // delay między drukarkami (jak teraz)
                 if (printer != enabledPrinters.last()) {
                     delay(2300) // inter-printer delay
                 }
             } catch (e: Exception) {
                 Timber.e(e, "Błąd druku na ${printer.name}")
                 // retry dla DUAL (jak teraz dla KITCHEN)
             }
         }
     }
     ```

2. **Nowa funkcja:** `fun printTestPage(printer: Printer)`
   - Zamiast `printTestPage(target: PrinterType)`

---

### **ETAP 5: Usunięcie Starych Plików/Kodu** ✅
**Pliki do usunięcia/uproszczenia:**

1. ❌ Usunąć: `PrinterType` enum (jeśli był jako osobny plik)
2. ✏️ Zmodyfikować: `AppPrefs.kt`
   - Usunąć funkcje:
     - `fun getPrinterSettings(type: PrinterType): PrinterSettings`
     - Wszystkie stare klucze SharedPreferences dla STANDARD/KITCHEN
3. ✏️ Zmodyfikować: `PrinterSettingsScreen.kt`
   - Przekierować do nowego `PrintersListScreen`
   - LUB usunąć całkowicie (jeśli stary screen nie jest już potrzebny)

---

### **ETAP 6: Testy i Walidacja** ✅
**Co przetestować:**

1. **Migracja:**
   - Zainstaluj nową wersję na urządzeniu ze starymi ustawieniami
   - Sprawdź czy stare drukarki STANDARD/KITCHEN zostały zmigowane do nowej listy

2. **Dodawanie/Edycja:**
   - Dodaj nową drukarkę przez modal
   - Edytuj istniejącą
   - Usuń drukarkę
   - Zmień kolejność (drag-and-drop)

3. **Drukowanie:**
   - Test print na każdej drukarce z listy
   - Akceptacja zamówienia → drukuje na wszystkich aktywnych drukarkach w kolejności

4. **Profile:**
   - Wybierz profil "POS-8390 DUAL" → sprawdź czy auto-fill działa
   - Wybierz "Custom" → sprawdź czy możesz ręcznie ustawić encoding/codepage

---

## 📁 Nowe Pliki - Podsumowanie

### Data Models:
- `data/model/Printer.kt` ✅
- `data/model/PrinterProfile.kt` ✅

### Preferences:
- `data/preferences/PrinterPreferences.kt` ✅
- `data/preferences/PrinterMigration.kt` ✅

### UI Screens:
- `ui/settings/printer/PrintersListScreen.kt` ✅
- `ui/settings/printer/PrintersViewModel.kt` ✅
- `ui/settings/printer/AddEditPrinterDialog.kt` ✅

### Modyfikacje:
- `ui/settings/print/PrinterService.kt` (refactor)
- `util/AppPrefs.kt` (cleanup starych kluczy)
- `ui/theme/home/HomeActivity.kt` (dodanie route)
- `ui/settings/SettingsScreen.kt` (link do zarządzania drukarkami)
- `MainActivity.kt` (wywołanie migracji)

---

## 🚀 Kolejność Implementacji

### Sprint 1: Fundament
1. Stwórz `Printer.kt` ✅
2. Stwórz `PrinterProfile.kt` ✅
3. Stwórz `PrinterPreferences.kt` ✅
4. Stwórz `PrinterMigration.kt` ✅

### Sprint 2: UI Basics
5. Stwórz `PrintersViewModel.kt` ✅
6. Stwórz `PrintersListScreen.kt` (bez drag-and-drop na razie) ✅
7. Stwórz `AddEditPrinterDialog.kt` ✅
8. Dodaj routing w HomeActivity ✅

### Sprint 3: Integracja
9. Refaktoryzuj `PrinterService.printAfterOrderAccepted()` ✅
10. Refaktoryzuj `PrinterService.printTestPage()` ✅
11. Dodaj wywołanie migracji w MainActivity ✅

### Sprint 4: Polish
12. Dodaj drag-and-drop do zmiany kolejności ✅
13. Dodaj walidację (np. czy nazwa nie jest pusta) ✅
14. Cleanup starych plików (PrinterType, stare AppPrefs keys) ✅
15. Testy end-to-end ✅

---

## 📊 Przewidywany Czas Implementacji

- **ETAP 1-2:** ~2-3h (modele + migracja)
- **ETAP 3:** ~4-5h (UI lista + modal)
- **ETAP 4:** ~2-3h (refaktor PrinterService)
- **ETAP 5:** ~1h (cleanup)
- **ETAP 6:** ~2h (testy)

**RAZEM: ~11-14h**

---

## ✅ Gotowe do Rozpoczęcia

Po zatwierdzeniu planu, zacznę implementację od **ETAPU 1** (modele danych).

Czy masz jakieś uwagi do planu lub chcesz coś zmienić przed rozpoczęciem?

