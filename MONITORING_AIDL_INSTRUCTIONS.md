# Instrukcja Monitorowania Połączeń AIDL z Drukarkami

## 📱 Gdzie zobaczyć logi?

### W Android Studio:
1. Otwórz **Logcat** (u dołu ekranu lub `View → Tool Windows → Logcat`)
2. W polu filtrowania wpisz: `AidlPrinterService`
3. Uruchom aplikację i spróbuj się połączyć z drukärką

## 🔍 Pełny przepływ logowania

### 1️⃣ Połączenie (BINDING)

```
🔄 ===== START PROCESU POŁĄCZENIA =====
⏱️  Timestamp: 1674158123456
📊 isConnected: false
🔌 currentServiceType: NONE
📍 Próba 1: Klon (recieptservice)
   🔗 Binding Klon: recieptservice.com.recieptservice
   ✅ bindService zwrócił: true
   📊 Wynik bind: true
✅ Próba 1 POWIODŁA SIĘ!
⏱️  Czas: 234ms
🔄 ===== KONIEC PROCESU POŁĄCZENIA (POMYŚLNIE) =====
```

### 2️⃣ Callback Połączenia

```
🔗 ===== POŁĄCZENIE Z DRUKÄRKĄ AIDL =====
⏱️  Timestamp: 1674158123690
📦 Pakiet: recieptservice.com.recieptservice
✅ Typ: KLON (recieptservice)
✅ Service Interface: PrinterInterface
✅ Status: POŁĄCZONO POMYŚLNIE
🔗 ===== KONIEC LOGOWANIA POŁĄCZENIA =====
```

### 3️⃣ Drukowanie

```
🖨️ ===== START DRUKOWANIA TEKSTU =====
⏱️  Timestamp: 1674158123800
📝 Tekst: TEST PRINT... (50 znaków)
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

### 4️⃣ Rozłączenie (UNBINDING)

```
🔌 ===== START PROCESU ROZŁĄCZENIA =====
⏱️  Timestamp: 1674158124000
🔌 Typ usługi: CLONE
📊 isConnected: true
✅ unbindService wykonany pomyślnie
✅ Status: ROZŁĄCZONO (zmienne wyzerowane)
🔌 ===== KONIEC PROCESU ROZŁĄCZENIA =====
```

### 5️⃣ Callback Rozłączenia

```
❌ ===== ROZŁĄCZENIE Z DRUKÄRKĄ AIDL =====
⏱️  Timestamp: 1674158124100
📦 Pakiet: recieptservice.com.recieptservice
🔌 Typ usługi: CLONE
❌ Status: ROZŁĄCZONO
❌ ===== KONIEC LOGOWANIA ROZŁĄCZENIA =====
```

## 🎯 Praktyczne zastosowanie

### Scenariusz: Chcę wiedzieć, czy drukarka się łączy

1. Uruchom aplikację
2. Otwórz **Logcat** i filtruj `AidlPrinterService`
3. Spróbuj wydrukować coś
4. Czekaj na logi w formie:

```
🔄 ===== START PROCESU POŁĄCZENIA =====
```

Jeśli widzisz:
- ✅ `POŁĄCZONO POMYŚLNIE` → drukarka działa
- ❌ `WSZYSTKIE PRÓBY NIEUDANE` → drukarka nie znaleziona

### Scenariusz: Chcę mierzyć czas drukowania

1. Szukaj: `🖨️ ===== START DRUKOWANIA TEKSTU =====`
2. Szukaj: `🖨️ ===== KONIEC DRUKOWANIA =====`
3. Porównaj timestampy w obu liniach

Czas drukowania = timestamp_koniec - timestamp_start

### Scenariusz: Drukarka nie drukuje, ale się łączy

Szukaj w logach:

```
✅ Drukowanie zakończone pomyślnie  ← mówi, że wysłano
```

Ale papier się nie porusza? Może to problem:
- Papieru w drukarce
- Ustawień encoding
- Kodepage'u
- Połączenia sprzętowego

## 📊 Znaczenie symboli

| Symbol | Znaczenie |
|--------|-----------|
| 🔗 | Połączenie się odbyło |
| 🔌 | Rozłączenie |
| 🖨️ | Drukowanie |
| ✅ | Sukces/OK |
| ❌ | Błąd/Nieudane/Rozłączone |
| ⚠️ | Ostrzeżenie |
| ⏱️ | Czas (timestamp) |
| 📝 | Tekst |
| ✂️ | Cięcie papieru |
| 📊 | Status/Statystyka |
| 📦 | Pakiet systemowy |
| 📍 | Kolejny krok |
| 🚀 | Uruchomienie metody |
| 🔄 | Proces/Cykl |

## 🐛 Debugowanie problemów

### Problem: "Nie widzę żadnych logów AIDL"

1. Sprawdź filtr w Logcat
   - Powinno być: `AidlPrinterService`
2. Wyczyść Logcat: `Ctrl + A` → Delete
3. Uruchom operację na nowo
4. Poczekaj kilka sekund

### Problem: "Widać POŁĄCZENIE, ale nie DRUKOWANIE"

```
✅ POŁĄCZONO POMYŚLNIE
❌ ===== START DRUKOWANIA TEKSTU =====
❌ Błąd druku AIDL
```

Przyczyny:
1. Zła drukarka połączona (czytaj Typ usługi)
2. Brak papieru
3. Problem z kodowaniem tekstu

### Problem: "Tylko NIEUDANE PRÓBY"

```
❌ Próba 1 NIEUDANA
❌ Próba 2 NIEUDANA
❌ Próba 3 NIEUDANA
❌ WSZYSTKIE PRÓBY NIEUDANE!
```

Przyczyny:
1. Drukarka jest wyłączona
2. Drukarka nie jest sparowana/połączona
3. Nie masz uprawnień Bluetooth
4. Zła drukarka sparowana

## 🔧 Ustawienia drukarki do monitorowania

### Jeśli chcesz monitorować określoną drukarkę:

1. W `AidlPrinterService.kt` szukaj metody `bindToCloneExplicit()`
2. Tam jest nazwa pakietu: `recieptservice.com.recieptservice`
3. Logi pokażą jaki pakiet faktycznie się podłączył

### Jeśli drukarka to Senraise:
- Będą logi z: `com.senraise.printer`

### Jeśli drukarka to Sunmi/Woyou:
- Będą logi z: `woyou.aidlservice.jiuiv5`

## 📝 Czytanie Timestampu

Każdy log zawiera `⏱️ Timestamp: 1674158123456`

To liczba milisekund od uruchamiania systemu Android.

Różnica między dwoma timestampami = czas trwania operacji

Przykład:
```
🔄 START: Timestamp: 1674158123456
🔄 KONIEC: Timestamp: 1674158123690

Czas = 1674158123690 - 1674158123456 = 234ms
```

## 💡 Pro Tips

1. **Eksportuj logi** - kliknij `Edit Filter Configuration` → `Save As` → wybierz nazwę
2. **Szukaj błędów** - szukaj ❌ zamiast czytania wszystkiego
3. **Mierz czasy** - jeśli drukowanie trwa +5s, coś nie tak
4. **Porównaj drukarki** - uruchom drugą drukarkę i porównaj logi

## 📞 Kontakt/Pomoc

Jeśli potrzebujesz więcej informacji, zobacz:
- `AIDL_LOGGING_DOCUMENTATION.md` - pełna dokumentacja
- `AIDL_LOGGING_SUMMARY.md` - podsumowanie zmian

