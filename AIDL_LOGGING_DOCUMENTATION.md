# AIDL Printer Service - Dokumentacja Logowania Połączenia

## Przegląd zmian

Dodano szczegółowe logowanie do `AidlPrinterService.kt`, które pozwala śledzić pełny przepływ połączenia i rozłączenia z drukärką AIDL.

## Przepływ logów

### 1. START PROCESU POŁĄCZENIA (`connect()`)

```
🔄 ===== START PROCESU POŁĄCZENIA =====
⏱️  Timestamp: 1674158123456
📊 isConnected: false
🔌 currentServiceType: NONE
```

#### Próby połączenia (w kolejności):

**Próba 1: Klon (recieptservice)**
```
📍 Próba 1: Klon (recieptservice)
   🔗 Binding Klon: recieptservice.com.recieptservice
   ✅ bindService zwrócił: true
   📊 Wynik bind: true
✅ Próba 1 POWIODŁA SIĘ!
⏱️  Czas: 234ms
🔄 ===== KONIEC PROCESU POŁĄCZENIA (POMYŚLNIE) =====
```

**Próba 2: Senraise**
```
📍 Próba 2: Senraise
   🔗 Binding Senraise: com.senraise.printer
   ✅ bindService zwrócił: true
   📊 Wynik bind: true
✅ Próba 2 POWIODŁA SIĘ!
⏱️  Czas: 456ms
🔄 ===== KONIEC PROCESU POŁĄCZENIA (POMYŚLNIE) =====
```

**Próba 3: Sunmi/Woyou**
```
📍 Próba 3: Sunmi/Woyou
   🔗 Binding Sunmi/Woyou: woyou.aidlservice.jiuiv5
   ✅ bindService zwrócił: true
   📊 Wynik bind: true
✅ Próba 3 POWIODŁA SIĘ!
⏱️  Czas: 678ms
🔄 ===== KONIEC PROCESU POŁĄCZENIA (POMYŚLNIE) =====
```

### 2. CALLBACK POŁĄCZENIA (`onServiceConnected`)

Po pomyślnym bindService system wywołuje `onServiceConnected`:

```
🔗 ===== POŁĄCZENIE Z DRUKÄRKĄ AIDL =====
⏱️  Timestamp: 1674158123690
📦 Pakiet: recieptservice.com.recieptservice

✅ Typ: KLON (recieptservice)
✅ Service Interface: PrinterInterface
✅ Status: POŁĄCZONO POMYŚLNIE
🔗 ===== KONIEC LOGOWANIA POŁĄCZENIA =====
```

### 3. DRUKOWANIE TEKSTU (`printText()`)

```
🖨️ ===== START DRUKOWANIA TEKSTU =====
⏱️  Timestamp: 1674158123800
📝 Tekst: TEST PRINT\n... (100 znaków)
✂️  AutoCut: false
📊 isConnected: true
🔌 currentServiceType: CLONE

🔌 Typ usługi: CLONE
🖨️ Wysyłam do KLONA...
   🖨️ [KLON] START DIAGNOSTYKI (RAW MODE)...
   📦 DANE: 1B 74 00 1B 21 00 54 45 53 54...
   🚀 METODA A: printImage(byte[])
   ✅ METODA A zakończona
✅ Drukowanie zakończone pomyślnie
🖨️ ===== KONIEC DRUKOWANIA =====
```

### 4. START PROCESU ROZŁĄCZENIA (`disconnect()`)

```
🔌 ===== START PROCESU ROZŁĄCZENIA =====
⏱️  Timestamp: 1674158124000
🔌 Typ usługi: CLONE
📊 isConnected: true

✅ unbindService wykonany pomyślnie
✅ Status: ROZŁĄCZONO (zmienne wyzerowane)
🔌 ===== KONIEC PROCESU ROZŁĄCZENIA =====
```

### 5. CALLBACK ROZŁĄCZENIA (`onServiceDisconnected`)

```
❌ ===== ROZŁĄCZENIE Z DRUKÄRKĄ AIDL =====
⏱️  Timestamp: 1674158124100
📦 Pakiet: recieptservice.com.recieptservice
🔌 Typ usługi: CLONE
❌ Status: ROZŁĄCZONO
❌ ===== KONIEC LOGOWANIA ROZŁĄCZENIA =====
```

## Scenariusze logowania

### Scenariusz A: Pomyślne połączenie i drukowanie

```
connect() → bindToCloneExplicit() → onServiceConnected() → printText() → zakończenie
```

Logi będą zawierać:
- ✅ START PROCESU POŁĄCZENIA
- ✅ Próba 1 POWIODŁA SIĘ!
- 🔗 POŁĄCZENIE Z DRUKÄRKĄ AIDL
- ✅ POŁĄCZONO POMYŚLNIE
- 🖨️ START DRUKOWANIA TEKSTU
- ✅ Drukowanie zakończone pomyślnie

### Scenariusz B: Nieudane poł��czenie

```
connect() → bindToCloneExplicit() → bindToSenraise() → bindToSunmi() → wszystko nieudane
```

Logi będą zawierać:
- ⏳ START PROCESU POŁĄCZENIA
- ❌ Próba 1 NIEUDANA
- ❌ Próba 2 NIEUDANA
- ❌ Próba 3 NIEUDANA
- ❌ WSZYSTKIE PRÓBY NIEUDANE!
- ❌ Status: BRAK POŁĄCZENIA

### Scenariusz C: Rozłączenie podczas drukowania

```
printText() → disconnect() → onServiceDisconnected()
```

Logi będą zawierać:
- 🔌 START PROCESU ROZŁĄCZENIA
- ✅ unbindService wykonany pomyślnie
- ❌ ROZŁĄCZENIE Z DRUKÄRKĄ AIDL

## Symbole w logach

| Symbol | Znaczenie |
|--------|-----------|
| 🔗 | Połączenie |
| 🔌 | Rozłączenie |
| 🖨️ | Drukowanie |
| ✅ | Sukces |
| ❌ | Błąd/Nieudane |
| ⚠️  | Ostrzeżenie |
| ⏱️ | Timestamp/Czas |
| 📝 | Tekst |
| ✂️ | Cięcie papieru |
| 📊 | Status/Dane |
| 📦 | Pakiet/Dane |
| 📍 | Krok |
| 🚀 | Uruchomienie |
| 🔄 | Proces/Iteracja |

## Timestampy

Każdy główny punkt w przepływie zawiera timestamp w milisekundach od uruchamiania systemu.
Pozwala to na:
- Śledzenie czasu trwania operacji
- Identyfikowanie bottlenecks
- Korespondowanie logów z innymi zdarzeniami w systemie

## Jak czytać logi

1. Wpisz w Logcat filtr: `AidlPrinterService`
2. Szukaj linii z `=====` - to początek/koniec głównych operacji
3. Czytaj od góry do dołu, zwracając uwagę na symbole ✅ vs ❌
4. Porównaj timestampy, aby zobaczyć czas trwania operacji

## Przykład pełnego przepływu w Logcat

```
08:45:23.456  AidlPrinterService  🔄 ===== START PROCESU POŁĄCZENIA =====
08:45:23.457  AidlPrinterService  ⏱️  Timestamp: 1674158123456
08:45:23.458  AidlPrinterService  📍 Próba 1: Klon (recieptservice)
08:45:23.459  AidlPrinterService     🔗 Binding Klon: recieptservice.com.recieptservice
08:45:23.690  AidlPrinterService  🔗 ===== POŁĄCZENIE Z DRUKÄRKĄ AIDL =====
08:45:23.691  AidlPrinterService  ✅ Typ: KLON (recieptservice)
08:45:23.692  AidlPrinterService  ✅ Status: POŁĄCZONO POMYŚLNIE
08:45:23.693  AidlPrinterService  ✅ Próba 1 POWIODŁA SIĘ!
08:45:23.694  AidlPrinterService  🔄 ===== KONIEC PROCESU POŁĄCZENIA (POMYŚLNIE) =====
08:45:24.000  AidlPrinterService  🖨️ ===== START DRUKOWANIA TEKSTU =====
08:45:24.001  AidlPrinterService  📊 isConnected: true
08:45:24.500  AidlPrinterService  ✅ Drukowanie zakończone pomyślnie
08:45:24.501  AidlPrinterService  🖨️ ===== KONIEC DRUKOWANIA =====
08:45:25.000  AidlPrinterService  🔌 ===== START PROCESU ROZŁĄCZENIA =====
08:45:25.100  AidlPrinterService  ✅ unbindService wykonany pomyślnie
08:45:25.101  AidlPrinterService  🔌 ===== KONIEC PROCESU ROZŁĄCZENIA =====
08:45:25.200  AidlPrinterService  ❌ ===== ROZŁĄCZENIE Z DRUKÄRKĄ AIDL =====
08:45:25.201  AidlPrinterService  ❌ Status: ROZŁĄCZONO
08:45:25.202  AidlPrinterService  ❌ ===== KONIEC LOGOWANIA ROZŁĄCZENIA =====
```

## Informacje debugowania

Każdy log zawiera:
- **Timestamp** - moment w miliśek
- **Service Type** - typ drukärki (CLONE, SENRAISE, WOYOU, NONE)
- **isConnected** - stan połączenia
- **Intent details** - parametry bindService
- **Duration** - czas trwania operacji (w ms)

To pozwala na szybkie zidentyfikowanie problemów z połączeniami.

