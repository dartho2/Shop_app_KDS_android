# System Dwóch Drukarek - Instrukcja Implementacji

## 📋 Przegląd

System obsługuje **dwie drukarki**:
1. **Drukarka Frontowa (FRONT/sala)** – paragon, potwierdzenia, druki ogólne
2. **Drukarka Kuchenna (KITCHEN)** – bileciki kuchenne

### Architektura

```
PrinterService
├── PrinterTarget { FRONT, KITCHEN }
├── DocumentType { KITCHEN_TICKET, RECEIPT, TEST }
├── resolveTargetForDocument() – routing na podstawie typu dokumentu
└── printAfterOrderAccepted() – druk po akceptacji (obie drukarki)

AppPrefs (z migracją v1→v2)
├── getFrontPrinterMac/Config/AutoCut
├── setKitchenPrinterEnabled/Mac/Config/AutoCut
└── performMigration() – jeśli stara drukarka, ustaw ją jako FRONT

PrinterSettingsScreen (UI)
└── Sekcja drukarki kuchennej (enable/select/test/auto-cut)
```

## 🔧 Co zostało zmienione

### 1. **AppPrefs.kt**
- Dodane klucze dla drukarki kuchennej w DeletablePrefKey
- Funkcja `performMigration()` – jeśli wcześniej była jedna drukarka, ustaw ją jako FRONT
- API dla dwóch drukarek:
  - `getFrontPrinterMac()`, `getFrontPrinterConfig()`, `setFrontPrinterAutoCut()`
  - `getKitchenPrinterMac()`, `getKitchenPrinterConfig()`, `setKitchenPrinterAutoCut()`
  - `isKitchenPrinterEnabled()`, `setKitchenPrinterEnabled()`

### 2. **PrinterService.kt**
- Enum `PrinterTarget { FRONT, KITCHEN }`
- Enum `DocumentType { KITCHEN_TICKET, RECEIPT, TEST }`
- Funkcja `resolveTargetForDocument()` – routing (KITCHEN_TICKET → kuchnia z fallback na front)
- Metody publiczne:
  - `printKitchenTicket(order, useDeliveryInterval)` – drukuje na kuchni
  - `printReceipt(order, useDeliveryInterval)` – drukuje na froncie
  - `printAfterOrderAccepted(order)` – drukuje na obu drukarkach po akceptacji
  - `printTestPage(target)` – test wydruku dla wybranego targetu

### 3. **PrinterSettingsScreen.kt**
- Nowa sekcja UI dla drukarki kuchennej
- Przełącznik włączenia/wyłączenia kuchni
- Możliwość wyboru innego MAC dla kuchni
- Test wydruku dla kuchni oddzielnie
- Checkbox auto-cut dla kuchni

## 🚀 Jak Podpiąć Druk Po Akceptacji Zamówienia

W miejscu gdzie obsługujesz akcję **ACCEPTED** zamówienia (np. OrdersViewModel, SocketHandler, Activity):

```kotlin
// 1. Wstrzyknij PrinterService
@Inject
private lateinit var printerService: PrinterService

// 2. Po pomyślnej akceptacji zamówienia:
scope.launch {
    try {
        // ... logika akceptacji zamówienia ...
        
        // Druk bileciku
        val shouldPrint = AppPrefs.getAutoPrintAcceptedEnabled() // lub Twoja flaga
        if (shouldPrint) {
            printerService.printAfterOrderAccepted(acceptedOrder)
        }
    } catch (e: Exception) {
        Timber.e(e, "Error during order acceptance")
    }
}
```

### Gdzie to podpiąć konkretnie?

Szukaj w kodzie:
- **OrdersViewModel.kt** – metoda obsługująca zmianę statusu na ACCEPTED
- **OrderActionsUseCase.kt** – funkcja `acceptOrder()`
- **Socket handler** – when("ORDER_ACCEPTED") { ... }
- **HomeActivity/MainActivity** – button akcji w UI

Logika powinna być analogiczna do `printAutomatically()` – jeśli już gdzieś drukujesz, tam dodaj drugą drukarkę.

## ⚙️ Domyślne Konfiguracje

**Drukarka Frontowa (FRONT)** – jeśli wcześniej była jedna:
- MAC: z poprzedniej konfiguracji
- Encoding: aktualny z AppPrefs
- Codepage: aktualny z AppPrefs
- Auto-cut: aktualny z AppPrefs

**Drukarka Kuchenna (KITCHEN)** – po migracj:
- Włączona: `false` (wyłączona)
- MAC: pusta
- Encoding: `UTF-8`
- Codepage: `null`
- Auto-cut: `false`

## 📝 Stringi Zasobów Do Dodania

Dodaj do `res/values/strings.xml` i `res/values-pl/strings.xml`:

```xml
<!-- Drukarka Kuchenna -->
<string name="kitchen_printer_settings">Ustawienia drukarki kuchennej</string>
<string name="enable_kitchen_printer">Włącz drukarkę kuchenną</string>
<string name="auto_cut_kitchen">Obcinaj papier po wydruku (kuchnia)</string>
<string name="select">Wybierz</string>
```

## 🔍 Testowanie

1. **Ustawienia → Drukarka**:
   - Włącz drukarkę kuchenną
   - Wybierz urządzenie
   - Kliknij "Test wydruku" pod sekcją kuchni

2. **Po akceptacji zamówienia**:
   - Jeśli `getAutoPrintAcceptedEnabled()` == true
   - Powinny drukować się bileciki na obu drukarkach (jeśli obie skonfigurowane)

3. **Fallback**:
   - Jeśli kuchnia wyłączona lub nie skonfigurowana, bilecik drukuje się na froncie

## 🛠️ Troubleshooting

| Problem | Rozwiązanie |
|---------|-------------|
| "Brak skonfigurowanej drukarki" | Sprawdź AppPrefs.getFrontPrinterMac() – powinna być MAC druk |
| Drukarka kuchenna nie drukuje | Sprawdź AppPrefs.isKitchenPrinterEnabled() i getKitchenPrinterMac() |
| Duplikaty druku | Zmniejsz liczbę wywołań printAfterOrderAccepted() – powinna być 1x po ACCEPTED |
| Polskie znaki – krzaki | Sprawdź encoding/codepage w Ustawieniach (ustawić CP852/13 dla YHD) |

## 📦 Migracja Danych

Po deployu nowej wersji aplikacja automatycznie:
1. Sprawdza wersję migracji w SharedPreferences
2. Jeśli < 2, kopiuje starą drukarkę do FRONT
3. Ustawia KITCHEN_ENABLED = false
4. Aktualizuje wersję na 2

**Nie tracisz poprzednich ustawień drukarki.**

## 🎯 Następne Kroki

1. Dodaj stringi zasobów (język PL i EN)
2. Podepnij `printerService.printAfterOrderAccepted(order)` w miejscu akceptacji zamówienia
3. Przetestuj na dwóch drukarkach Bluetooth
4. Ustaw domyślnie ustawienia koderów (YHD → CP852/13, Mobile → UTF-8)

