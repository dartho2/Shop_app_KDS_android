# 🖨️ System Zarządzania Wieloma Drukarkami - COMPLETE GUIDE

**Wersja:** 1.0-RC1  
**Status:** ✅ ETAP 1-4 UKOŃCZONY  
**Data:** 2026-01-22

---

## 🎯 Cel Systemu

Zastąpić stary, sztywny system jednej drukarki na **elastyczny system obsługujący nieograniczoną liczbę drukarek** z możliwością:

- ➕ Dodawania/usuwania drukarek
- ✏️ Edycji ustawień (profil, encoding, szablon)
- 🔄 Sekwencyjnego drukowania na wielu drukarkach
- 🎚️ Zmienia kolejności drukowania
- 🔌 Włączania/wyłączania drukarek bez usuwania

---

## 📦 Architektura

```
┌─────────────────────────────────────────────────────┐
│                   HomeActivity                       │
│              (Navigation Host)                       │
└─────────────────────────────────────────────────────┘
                         │
          ┌──────────────┼──────────────┐
          │              │              │
     ┌────▼────┐  ┌─────▼──────┐  ┌──▼─────────────┐
     │ Orders  │  │ Settings   │  │ Printers List  │
     │ (Stare) │  │ (Stare)    │  │ (NOWY)        │
     └─────────┘  └────────────┘  └────────┬──────┘
                                            │
                                   ┌────────▼──────────┐
                                   │ Add/Edit Dialog   │
                                   │ (NOWY)            │
                                   └───────────────────┘
```

---

## 📁 Struktura Plików

```
app/src/main/java/com/itsorderchat/
├─ data/
│  ├─ model/
│  │  ├─ Printer.kt (NOWY)           ⭐ Model główny
│  │  └─ PrinterProfile.kt (NOWY)    ⭐ Predefiniowane profile
│  └─ preferences/
│     ├─ PrinterPreferences.kt       ✅ Persystencja (istniejący)
│     └─ PrinterMigration.kt         ✅ Migracja (istniejący)
│
├─ ui/
│  ├─ settings/
│  │  ├─ printer/
│  │  │  ├─ PrintersViewModel.kt (NOWY)        ⭐ ViewModel
│  │  │  ├─ PrintersListScreen.kt (NOWY)      ⭐ Main Screen
│  │  │  └─ AddEditPrinterDialog.kt (NOWY)    ⭐ Dialog
│  │  └─ SettingsScreen.kt                     ✏️ Zmodyfikowany
│  │
│  └─ print/
│     └─ PrinterService.kt                     ✏️ Zmodyfikowany
│
└─ theme/home/
   └─ HomeActivity.kt                          ✏️ Zmodyfikowany
```

---

## 🚀 Quick Start

### 1. Dodanie Drukarki

**Ścieżka:** Settings → Zarządzaj drukarkami → (+)

```
┌─────────────────────────────────┐
│ Dodaj Drukarkę                  │
├─────────────────────────────────┤
│ Nazwa:           [Drukarka 1   ]│
│ Urządzenie BT:   [AA:BB:CC...]  │
│ Profil:          [POS-8390 DUAL]│
│ Szablon:         [Standardowy  ]│
│ Autoczęcie:      [✓]           │
├─────────────────────────────────┤
│ [Anuluj]           [Zapisz]    │
└─────────────────────────────────┘
```

### 2. Sekwencyjne Drukowanie

```
OrderAccepted Event
  └─→ printOrderOnAllEnabledPrinters()
      ├─→ Drukarka #1 (order=1)
      │   └─→ EscPosPrinter.printFormattedText()
      │
      ├─→ Timeout: 3000ms (BT cleanup)
      │
      └─→ Drukarka #2 (order=2)
          └─→ EscPosPrinter.printFormattedText()
```

---

## 🔧 Konfiguracja Drukarek

### Profil: POS-8390 DUAL

```
Urządzenie:      YHD-8390 (BLE/BT Dual Mode)
Encoding:        Cp852
Codepage:        13 (PC852)
Autoczęcie:      ✓ TAK
Szablon:         Standardowy
Timeout BT:      3000ms
```

### Profil: Mobile SSP

```
Urządzenie:      Bluetooth Classic (SPP)
Encoding:        UTF-8
Codepage:        null
Autoczęcie:      ✗ NIE
Szablon:         Kompaktowy
Timeout BT:      2000ms
```

### Profil: Custom

```
Encoding:        [Custom]
Codepage:        [Custom]
Autoczęcie:      [Wybór użytkownika]
Szablon:         [Wybór użytkownika]
```

---

## 📊 Data Model

### Printer

```kotlin
data class Printer(
    val id: String,              // UUID
    val name: String,            // "Drukarka Główna"
    val deviceId: String,        // "AB:0D:6F:E2:85:D7"
    val profileId: String,       // "profile_pos_8390_dual"
    val templateId: String,      // "template_standard"
    val encoding: String,        // "Cp852"
    val codepage: Int?,          // 13
    val autoCut: Boolean,        // true
    val enabled: Boolean,        // true
    val order: Int               // 1 (kolejność drukowania)
)
```

### PrinterProfile

```kotlin
enum class PrinterProfile(
    val id: String,
    val displayName: String,
    val encoding: String,
    val codepage: Int?,
    val autoCut: Boolean
) {
    POS_8390_DUAL(...),    // PC852
    MOBILE_SSP(...),       // UTF-8
    CUSTOM(...)            // Niestandardowy
}
```

---

## 🔄 Migracja z Systemu Starego

### Automatyczna Migracja

```
Pierwszy uruchomienie:
├─ Sprawdź: printers_migrated_v1 flag
├─ Jeśli brak:
│  ├─ Czytaj: AppPrefs.getPrinterSettings (STARE)
│  ├─ Konwertuj: → Printer model
│  ├─ Zapisz: PrinterPreferences (JSON)
│  └─ Ustaw: printers_migrated_v1 = true
└─ Gotowe! Oba systemy działają
```

### Backward Compatibility

✅ **Stary kod bez zmian:**
- `printAfterOrderAccepted()` - nadal używa AppPrefs
- `printTestPage()` - nie zmieniony
- `AppPrefs.getPrinterSettings()` - wciąż dostępny

---

## 📱 API Reference

### PrintersViewModel

```kotlin
@HiltViewModel
class PrintersViewModel @Inject constructor(
    private val printerPreferences: PrinterPreferences
) : ViewModel() {
    
    val printers: StateFlow<List<Printer>>
    val isLoading: StateFlow<Boolean>
    val errorMessage: StateFlow<String?>
    
    // CRUD
    fun loadPrinters()
    fun addPrinter(printer: Printer)
    fun updatePrinter(id: String, printer: Printer)
    fun deletePrinter(id: String)
    fun reorderPrinters(fromIndex: Int, toIndex: Int)
    fun toggleEnabled(id: String)
}
```

### PrinterService

```kotlin
@Singleton
class PrinterService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    // NOWY: Sekwencyjne drukowanie na wszystkich drukarach
    suspend fun printOrderOnAllEnabledPrinters(
        order: Order,
        useDeliveryInterval: Boolean = false
    )
    
    // STARY: Drukowanie na konkretnej drukarce
    suspend fun printOrder(
        order: Order,
        useDeliveryInterval: Boolean = false,
        target: PrinterType = PrinterType.STANDARD
    )
}
```

---

## 🧪 Testowanie

### Build

```bash
./gradlew clean assembleDebug
```

### Manual Tests

| Test | Oczekiwanie | Status |
|------|------------|--------|
| Dodaj drukarkę | ✅ Toast | 🟡 |
| Edytuj drukarkę | ✅ Update | 🟡 |
| Usuń drukarkę | ✅ Usunięta | 🟡 |
| Drukuj (1 drukarka) | ✅ Wydruk | 🟡 |
| Drukuj (2 drukarki) | ✅ Sekwencyjnie | 🟡 |
| Timeout BT cleanup | ✅ 3000ms | 🟡 |

### Logcat

```bash
# Szukaj wydruku
adb logcat | grep -i "printer\|druk"

# Pełne logowanie
adb logcat | grep "PrinterSTEP"
```

**Oczekiwany wynik:**
```
✅ PrinterSTEP: [SUCCESS] Drukowanie zakończone pomyślnie na STANDARD
⏳ PrinterSTEP: Czekam 3000ms przed przełączeniem...
✅ PrinterSTEP: [SUCCESS] Drukowanie zakończone pomyślnie na KITCHEN
```

---

## 📚 Dokumentacja Techniczna

| Dokument | Zawartość |
|----------|-----------|
| `PRINTER_SYSTEM_IMPLEMENTATION.md` | Pełna specyfikacja ETAP 1-4 |
| `PRINTER_SYSTEM_SUMMARY.md` | Podsumowanie systemu |
| `ETAP_5_TESTY_INSTRUKCJE.md` | Instrukcje QA i testów |
| `RAPORT_WDRAZANIA.md` | Raport statystyk i timeline |

---

## 🎓 Best Practices

### ✅ DO's

- ✅ Mapuj profile do znanych modeli drukarek
- ✅ Używaj predefiniowanych profili zamiast custom
- ✅ Dodaj timeouty BT cleanup między drukárkami
- ✅ Loguj każdy krok drukowania
- ✅ Obsługuj błędy gracefully

### ❌ DON'Ts

- ❌ Nie zmieniaj macierzy startowych profili
- ❌ Nie usuwaj backward compatibility kodu
- ❌ Nie skracaj timeout BT poniżej 2000ms
- ❌ Nie modyfikuj PrinterMigration bez konsultacji
- ❌ Nie mieszaj starego i nowego systemu w jednym miejscu

---

## 🐛 Troubleshooting

| Problem | Przyczyna | Rozwiązanie |
|---------|-----------|------------|
| "BT connect failed" | Timeout za krótki | Zwiększ do 3000ms |
| Drukarka nie drukuje | Profil niezgodny | Sprawdzić encoding |
| Duplikaty na liście | SharedPrefs bug | `adb shell pm clear` |
| Dialog się nie zamyka | ViewModel error | Sprawdzić logcat |

---

## 🚀 Co Dalej?

### ETAP 5: Testy ✅ (w trakcie)
- Build verification
- Manual QA tests
- Regression tests

### ETAP 6: Production 🟡 (następny tydzień)
- Beta release
- Final documentation
- Google Play deployment

---

## 📞 Support

- **Code Issues:** Sprawdzić logcat + dokumentacja
- **Build Issues:** `./gradlew clean build --stacktrace`
- **BT Issues:** Restart urządzenia + Bluetooth adapter

---

**Twórca:** GitHub Copilot  
**Status:** ✅ READY FOR QA  
**Wersja:** 1.0-RC1  
**Ostatnia Aktualizacja:** 2026-01-22

