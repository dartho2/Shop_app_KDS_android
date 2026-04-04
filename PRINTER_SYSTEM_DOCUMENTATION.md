# 📋 Dokumentacja Systemu Drukarek

## Przegląd

System drukarek w aplikacji ItsOrderChat obsługuje trzy typy połączeń:

1. **Bluetooth (BT)** - Tradycyjne drukarki POS na Bluetooth Classic (SPP)
2. **Sieć (Network)** - Drukarki sieciowe TCP/IP
3. **Wbudowana (Builtin)** - Drukarki wbudowane w urządzeniach POS (np. Sunmi H10)

## Struktura

### Modele Danych

#### `Printer` (data/model/Printer.kt)
```kotlin
data class Printer(
    val id: String,                    // Unikalny identyfikator
    val name: String,                  // Nazwa wyświetlana
    val deviceId: String,              // MAC/IP/port (zależy od typu)
    val profileId: String,             // Referencja do profilu
    val templateId: String,            // Template do wydruku
    val encoding: String,              // np. "UTF-8", "Cp852"
    val codepage: Int?,                // Numer strony kodowej ESC/POS
    val autoCut: Boolean,              // Automatyczne cięcie papieru
    val enabled: Boolean,              // Czy drukarka jest aktywna
    val order: Int,                    // Kolejność drukowania
    val printerType: PrinterType,      // STANDARD, KITCHEN
    val connectionType: PrinterConnectionType  // BT, NETWORK, BUILTIN
)
```

#### `PrinterType` (data/model/PrinterType.kt)
- `STANDARD` - Drukarka do paragonów/recept
- `KITCHEN` - Drukarka do biletów kuchennych

#### `PrinterConnectionType` (data/model/PrinterConnectionType.kt)
- `BLUETOOTH` - Połączenie Bluetooth
- `NETWORK` - Połączenie TCP/IP
- `BUILTIN` - Wbudowana drukarka urządzenia

### Managers

#### `PrinterService` (ui/settings/print/PrinterService.kt)
Główny serwis obsługujący drukowanie.

**Publiczne metody:**
```kotlin
suspend fun printOrder(order: Order, useDeliveryInterval: Boolean, docType: DocumentType)
suspend fun printKitchenTicket(order: Order, useDeliveryInterval: Boolean)
suspend fun printReceipt(order: Order, useDeliveryInterval: Boolean)
suspend fun printAfterOrderAccepted(order: Order)
suspend fun printTest(deviceId: String, profile: PrinterProfile, templateId: String, autoCut: Boolean)
```

**Dokumenty:**
- `KITCHEN_TICKET` - Bilet do kuchni
- `RECEIPT` - Paragon dla klienta
- `TEST` - Dokument testowy

#### `PrinterConnectionManager` (ui/settings/print/PrinterConnectionManager.kt)
Obsługa połączeń do drukarek (Bluetooth, TCP/IP).

```kotlin
suspend fun <R> withConnection(
    connection: DeviceConnection,
    printer: Printer,
    block: suspend (DeviceConnection) -> R
): R
```

#### `PrinterManager` (ui/settings/printer/PrinterManager.kt)
Zarządzanie drukarkami - inicjalizacja, skanowanie.

**Metody:**
```kotlin
fun getConnectionById(context: Context, deviceId: String): DeviceConnection?
fun scanBluetoothPrinters(context: Context): List<BluetoothPrinterInfo>
fun getConnection(context: Context): BluetoothConnection?
```

### Storage

#### `PrinterPreferences` (data/preferences/PrinterPreferences.kt)
Przechowywanie konfiguracji drukarek w DataStore.

```kotlin
suspend fun getPrinters(context: Context): List<Printer>
suspend fun addPrinter(context: Context, printer: Printer)
suspend fun updatePrinter(context: Context, id: String, printer: Printer)
suspend fun deletePrinter(context: Context, id: String)
```

#### `AppPreferencesManager` (data/preferences/AppPreferencesManager.kt)
Ustawienia drukowania (automatyczne drukowanie, etc).

```kotlin
suspend fun getAutoPrintAcceptedEnabled(): Boolean
suspend fun getAutoPrintKitchenEnabled(): Boolean
suspend fun setAutoPrintKitchenEnabled(value: Boolean)
```

### Szablony

#### `PrintTemplate` (ui/settings/print/PrintTemplate.kt)
Dostępne szablony wydruku:
- `TEMPLATE_STANDARD` - Standard A4
- `TEMPLATE_COMPACT` - Kompaktowy format

#### `PrintTemplateFactory` (ui/settings/print/PrintTemplateFactory.kt)
Budowanie zawartości wydruku na podstawie zamówienia i szablonu.

```kotlin
fun buildTicket(
    context: Context,
    order: Order,
    template: PrintTemplate,
    useDeliveryInterval: Boolean
): String
```

### Diagnostyka

#### `SerialPortHelper` (ui/settings/printer/SerialPortHelper.kt)
Skanowanie dostępnych portów szeregowych do wbudowanych drukarek.

```kotlin
fun scanSerialPorts(): List<SerialPortInfo>
fun getBestSerialPortCandidate(): String?
fun isSunmiDevice(): Boolean
fun getDiagnosticInfo(): String
```

## Przepływy

### 1. Drukowanie po zaakceptowaniu zamówienia

```
AcceptOrderSheetContent
    ↓
OrdersViewModel.acceptOrder()
    ↓
PrinterService.printAfterOrderAccepted(order)
    ↓
PrinterPreferences.getPrinters() → filtruj STANDARD + KITCHEN (jeśli enabled)
    ↓
PrinterConnectionManager.withConnection()
    ├─ PrinterManager.getConnectionById() → Bluetooth
    ├─ PrinterConnectionManager.createConnection() → Network
    └─ (null) → Builtin (TODO)
    ↓
EscPosPrinter.printFormattedText() lub printFormattedTextAndCut()
```

### 2. Testowanie drukarki

```
PrinterSettingsScreen
    ↓
Klik "Test Print"
    ↓
PrinterService.printTest(deviceId, profile, templateId, autoCut)
    ↓
Buforowanie do EscPosPrinter
    ↓
Wydruk testowy
```

### 3. Diagnostyka portów szeregowych

```
PrintersListScreen
    ↓
Klik na ikonę diagnostyki
    ↓
SerialPortDiagnosticScreen
    ↓
SerialPortHelper.scanSerialPorts()
    ���─ Sprawdzanie /dev/ttyS0, /dev/ttyS1, /dev/ttyUSB0, itp.
    ├─ Uprawnienia, typ pliku, dostępność
    └─ Wyświetlenie raport
```

## Ustawienia

### AppPreferencesManager Keys

```kotlin
KEY_AUTO_PRINT_ACCEPTED = "auto_print_accepted"      // Druk po zaakceptowaniu
KEY_AUTO_PRINT_KITCHEN = "auto_print_kitchen"        // Druk na kuchni
```

### PrinterPreferences Keys

```kotlin
PRINTERS_LIST_KEY = "printers_list"                   // Lista drukarek (JSON)
```

## Kodowanie i Codepage

Obsługiwane kodowania dla Bluetooth/Network:

| Encoding | Codepage | Opis |
|----------|----------|------|
| UTF-8 | null | Standardowe kodowanie Unicode |
| Cp852 | 13 | Znaki polskie (łódź, etc.) |
| GBK | - | Znaki chińskie |
| CP437 | 0 | ASCII |

## Błędy i Rozwiązania

### "BT connect failed after retry"
- Drukarka może być zajęta lub w innym urządzeniu
- Upewnij się że Bluetooth jest włączony
- Sprawdź uprawnienia aplikacji do Bluetooth

### "Unable to connect to network"
- Sprawdź IP i port drukarki
- Upewnij się że drukarka jest w sieci
- Sprawdź firewall

### "Brak skonfigurowanej drukarki"
- Przejdź do Ustawienia → Drukarki
- Dodaj drukarkę
- Włącz drukarkę (switch ON)

### Znaki polskie drukują się jako krzaki
- Sprawdź encoding w profilu drukarki
- Dla Bluetooth zmień na CP852 z codepage 13
- Dla sieci spróbuj UTF-8

## Testing

### Testowanie drukowania

1. Przejdź do Ustawienia → Drukarki
2. Klik na drukarkę
3. Klik przycisk "Test Print"
4. Sprawdź czy wydruk się pojawił

### Diagnostyka portu szeregowego (Sunmi, etc.)

1. Przejdź do Ustawienia → Drukarki
2. Klik na ikonę debugowania (🐛)
3. Klik "Skanuj porty"
4. Zobacz listę dostępnych portów

## TODO / Przyszłe funkcjonalności

- [ ] Obsługa drukarek wbudowanych (Builtin) przez Android Print Service
- [ ] UI dla dodawania drukarek sieciowych
- [ ] Automatyczne skanowanie drukarek Bluetooth
- [ ] Obsługa kasety papieru / ustawieniach temperatur
- [ ] Historia wydruków
- [ ] Kolejkowanie wydruków w bazie
- [ ] Retry logic z exponential backoff

