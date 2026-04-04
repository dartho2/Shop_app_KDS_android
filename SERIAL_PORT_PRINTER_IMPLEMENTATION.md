# Implementacja Drukowania na Portach Szeregowych i Drukarkach Wbudowanych

## Przegląd

System obsługuje teraz drukowanie na:
1. **Drukarkach Bluetooth** (klasyczne ESC/POS)
2. **Drukarkach Sieciowych** (TCP/IP)
3. **Drukarkach Wbudowanych** (Sunmi H10, Urovo, itp.)
4. **Portach Szeregowych** (rzeczywiste urządzenia /dev/ttyXX)

## Architektura

### Główne Komponenty

#### 1. `SerialPortPrinter` (SerialPortPrinter.kt)
- Obsługuje rzeczywiste porty szeregowe
- Wysyła komendy ESC/POS bezpośrednio do /dev/ttyXX
- Wspiera różne kodowania (UTF-8, CP852, itp.)
- Automatycznie dobiera kodepage

#### 2. `SunmiPrinterAdapter` (SunmiPrinterAdapter.kt)
- Adapter dla drukarek Sunmi H10
- Komunikuje się poprzez serwis systemowy Sunmi
- Obsługuje drukowanie i cięcie papieru
- Bezpiecznie zarządza połączeniami

#### 3. `PrinterService` (PrinterService.kt)
- Główny serwis drukowania
- Obsługuje wszystkie typy drukarek
- Zarządza sekwencyjnym drukowaniem
- Obsługuje drukowanie na kuchni i standardowe

## Konfiguracja Drukarek

### Dodawanie Drukraki Wbudowanej (Sunmi H10)

1. Przejdź do **Ustawienia → Drukarki**
2. Kliknij **Dodaj drukarkę**
3. Wybierz:
   - **Typ połączenia**: Wbudowana (Builtin)
   - **Nazwa**: np. "Sunmi H10"
   - **ID urządzenia**: "builtin"
   - **Kodowanie**: UTF-8 (domyślnie)
   - **Szablon**: Wybierz szablon wydruku
   - **Typ drukarki**: Standardowa / Kuchnia

### Dodawanie Drukarki na Porcie Szeregowym

1. Przejdź do **Ustawienia → Drukarki**
2. Kliknij **Dodaj drukarkę**
3. Wybierz:
   - **Typ połączenia**: Port Szeregowy
   - **Nazwa**: np. "Drukarka Termiczna"
   - **ID urządzenia**: np. "/dev/ttyS1"
   - **Kodowanie**: CP852 (dla polskich znaków) lub UTF-8
   - **Codepage**: 13 (dla CP852), null (dla UTF-8)
   - **Szablon**: Wybierz szablon wydruku

## Fluxowe Drukowania

### Drukowanie na Zamówienie (po zaakceptowaniu)

```
1. PrinterService.printAfterOrderAccepted() 
   ↓
2. Pobierz konfiguracje drukarek (standard, kuchnia)
   ↓
3. Dla każdej drukarki:
   a. Sprawdź typ połączenia
   b. Jeśli BUILTIN → printOneSerial() → printSunmiBuiltin()
   c. Jeśli SERIAL → printOneSerial() → SerialPortPrinter.printEscPosToSerial()
   d. Jeśli BT/NETWORK → printOne() → Driverowe API
   ↓
4. Wyślij, czekaj, odłącz
```

### Sekwencja dla Wielu Drukarek

```
Drukarka 1: connect → print → disconnect → WAIT 2000ms
                                              ↓
Drukarka 2: connect → print → disconnect
```

## Obsługiwane Kodowania

### UTF-8
- **Zastosowanie**: Drukanie znaków międzynarodowych
- **Codepage**: null/brak
- **Kodowanie**: UTF-8

### CP852 (Środkowa Europa)
- **Zastosowanie**: Polskie znaki (ąćęłńóśżź)
- **Codepage**: 13
- **Kodowanie**: Cp852
- **ESC/POS**: `ESC t 13` przed tekstem

### CP1250
- **Zastosowanie**: Windows-1250
- **Codepage**: 255 (zmienia się na systemie)
- **Kodowanie**: windows-1250

## Diagnostyka

### Skan Portów Szeregowych

```
1. Ustawienia → Drukarki
2. Kliknij "Skanuj Porty"
3. System wyświetli dostępne porty
```

### Logowanie

Wszystkie operacje drukowania są logowane z oznaczeniami:
- 🖨️ - Drukarka
- 📄 - Zawartość
- ✅ - Sukces
- ❌ - Błąd
- ✂️ - Cięcie
- 🔌 - Połączenie

## Rozwiązywanie Problemów

### Problem: "Read-only file system" na porcie
**Przyczyna**: System plików portu jest read-only  
**Rozwiązanie**: Użyj drukaki wbudowanej (Sunmi) zamiast portu szeregowego

### Problem: Sunmi nie drukuje
**Przyczyna**: Serwis Sunmi nie jest zainstalowany  
**Rozwiązanie**: 
1. Zainstaluj aplikację Sunmi Assistant
2. Zrestartuj aplikację
3. Spróbuj ponownie

### Problem: Polskie znaki drukują się jako "?"
**Przyczyna**: Zła strona kodowa  
**Rozwiązanie**:
1. Ustaw kodowanie na CP852
2. Ustaw codepage na 13
3. Test drukowania

## API dla Deweloperów

### Drukowanie Zamówienia

```kotlin
// Automatyczne drukowanie po zaakceptowaniu
printerService.printAfterOrderAccepted(order)

// Ręczne drukowanie
printerService.printOrder(
    order = order,
    printKitchen = true,  // czy drukować na kuchni
    useDeliveryInterval = false  // czy używać delivery_interval
)
```

### Drukowanie na Konkretnej Drukarce

```kotlin
val printer = appPreferencesManager.getStandardPrinter()
printer?.let { p ->
    printerService.printOrder(
        order = order,
        printerOverride = p,
        printKitchen = false
    )
}
```

## Moduły Zależności

- **SerialPortPrinter**: Obsługa rzeczywistych portów
- **SunmiPrinterAdapter**: Integracja z Sunmi H10
- **PrinterConnectionManager**: Zarządzanie połączeniami BT/Network
- **EscPosPrinter** (Dantsu): ESC/POS dla drukaren BT/Network

## Przyszłe Rozszerzenia

1. Obsługa drukarek Star Micronics
2. Obsługa drukarek Epson
3. Obsługa drukarek Zebra (etykiety)
4. Interfejs konfiguracji per-drukarka
5. Historia drukowania

## Notatki Techniczne

- Maksymalny timeout połączenia BT: 10 sekund
- Timeout między drukarkami: 2 sekundy
- Maksymalny rozmiar biletu: 64KB
- Kodowanie domyślne: UTF-8

