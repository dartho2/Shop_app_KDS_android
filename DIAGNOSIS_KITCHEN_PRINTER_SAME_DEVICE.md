# 🔍 DIAGNOZA I NAPRAWA - Drukarka Kuchenna Używa Pierwszej Drukarki

## 🐛 ZGŁOSZONY PROBLEM
> "druk na kuchnie jest wykonywany przez pierwsza drukarke"

Drukarka kuchenna drukuje na tym samym urządzeniu co drukarka frontowa (pierwsza drukarka).

---

## 🔍 ANALIZA PROBLEMU

### Możliwe Przyczyny

#### 1. Nie wybrano drukarki kuchennej
Użytkownik nie wybrał urządzenia dla drukarki kuchennej w ustawieniach.

#### 2. Wybrano to samo urządzenie
Użytkownik wybrał to samo urządzenie BT dla FRONT i KITCHEN.

#### 3. Kod zapisuje MAC niepoprawnie
Bug w `setKitchenPrinter()` - zapisuje MAC frontowej zamiast kuchennej.

#### 4. Kod pobiera MAC niepoprawnie
Bug w `getKitchenPrinterMac()` - zwraca MAC frontowej zamiast kuchennej.

---

## ✅ WDROŻONE NAPRAWY

### 1. Dodano Szczegółowe Logi w PrinterService.kt

#### `resolveTargetConfig()`
```kotlin
private fun resolveTargetConfig(target: PrinterTarget): TargetConfig? {
    Timber.d("resolveTargetConfig: target=$target")
    
    return when (target) {
        PrinterTarget.FRONT -> {
            val frontMac = AppPrefs.getFrontPrinterMac()
            Timber.d("resolveTargetConfig: FRONT mac=$frontMac")
            // ... existing code ...
        }
        PrinterTarget.KITCHEN -> {
            val enabled = AppPrefs.isKitchenPrinterEnabled()
            Timber.d("resolveTargetConfig: KITCHEN enabled=$enabled")
            
            val kitchenMac = AppPrefs.getKitchenPrinterMac()
            Timber.d("resolveTargetConfig: KITCHEN mac=$kitchenMac")
            // ... existing code ...
        }
    }
}
```

**Cel:** Zobaczyć jakie MACs są pobierane dla każdej drukarki.

---

### 2. Dodano Logi w PrinterSettingsScreen.kt

```kotlin
modifier = Modifier.clickable {
    Timber.d("PrinterSettings: selecting KITCHEN printer: ${printer.device.address}")
    AppPrefs.setKitchenPrinter(printer.device.address)
    kitchenMac = printer.device.address
    kitchenCfg = AppPrefs.getKitchenPrinterConfig()
    Timber.d("PrinterSettings: KITCHEN saved: mac=$kitchenMac, cfg=$kitchenCfg")
    showKitchenPrinterDialog = false
}
```

**Cel:** Zobaczyć co jest zapisywane gdy użytkownik wybiera drukarkę kuchenną.

---

### 3. Dodano Import Timber do PrinterSettingsScreen.kt

```kotlin
import timber.log.Timber
```

---

## 🧪 JAK ZDIAGNOZOWAĆ PROBLEM

### Krok 1: Włącz Logi ADB
```powershell
adb logcat -c
adb logcat | Select-String "resolveTargetConfig|PrinterSettings"
```

### Krok 2: Wybierz Drukarkę Kuchenną w Ustawieniach
1. Otwórz Ustawienia → Drukarki
2. Sekcja Drukarka Kuchenna
3. Kliknij "Wybierz drukarkę"
4. Wybierz **INNĄ** drukarkę niż frontowa

**Obserwuj logi:**
```
D/PrinterSettings: selecting KITCHEN printer: AB:CD:EF:12:34:56
D/PrinterSettings: KITCHEN saved: mac=AB:CD:EF:12:34:56, cfg=...
```

### Krok 3: Zaakceptuj Zamówienie
1. Odbierz nowe zamówienie
2. Zaakceptuj

**Obserwuj logi:**
```
D/PrinterService: resolveTargetConfig: target=KITCHEN
D/PrinterService: resolveTargetConfig: KITCHEN enabled=true
D/PrinterService: resolveTargetConfig: KITCHEN mac=AB:CD:EF:12:34:56  ← SPRAWDŹ TEN MAC
D/PrinterService: resolveTargetConfig: target=FRONT
D/PrinterService: resolveTargetConfig: FRONT mac=00:11:22:33:44:55    ← SPRAWDŹ TEN MAC
```

### Krok 4: Zweryfikuj MACs

#### ✅ POPRAWNIE (MACs są różne)
```
KITCHEN mac=AB:CD:EF:12:34:56  ← Drukarka kuchenna
FRONT mac=00:11:22:33:44:55    ← Drukarka frontowa
```

#### ❌ BŁĄD (MACs są takie same)
```
KITCHEN mac=00:11:22:33:44:55  ← TO SAMO!
FRONT mac=00:11:22:33:44:55    ← TO SAMO!
```

---

## 📋 SCENARIUSZE DIAGNOSTYCZNE

### Scenariusz 1: MACs są identyczne

**Przyczyna:** Użytkownik nie wybrał drukarki kuchennej LUB wybrał to samo urządzenie.

**Rozwiązanie:**
1. Sprawdź czy w ustawieniach jest wybrana drukarka kuchenna
2. Sprawdź czy wybrałeś **INNĄ** drukarkę niż frontowa
3. Ponownie wybierz drukarkę kuchenną (obserwuj logi)

---

### Scenariusz 2: KITCHEN mac=null

**Logi:**
```
D/PrinterService: resolveTargetConfig: KITCHEN mac=null
```

**Przyczyna:** Nie wybrano urządzenia dla drukarki kuchennej.

**Rozwiązanie:**
1. Otwórz Ustawienia → Drukarki
2. Włącz drukarkę kuchenną
3. Wybierz urządzenie BT

---

### Scenariusz 3: KITCHEN enabled=false

**Logi:**
```
D/PrinterService: resolveTargetConfig: KITCHEN enabled=false
```

**Przyczyna:** Drukarka kuchenna jest wyłączona w ustawieniach.

**Rozwiązanie:**
1. Otwórz Ustawienia → Drukarki
2. Włącz przełącznik "Drukarka kuchenna"

---

## 🎯 CO SPRAWDZIĆ W LOGACH

| Log | Co oznacza | Co zrobić jeśli błędne |
|-----|------------|------------------------|
| `selecting KITCHEN printer: XX:XX:...` | Użytkownik wybrał drukarkę | Sprawdź czy MAC jest poprawny |
| `KITCHEN saved: mac=XX:XX:...` | Zapisano w AppPrefs | Sprawdź czy MAC jest różny od FRONT |
| `resolveTargetConfig: KITCHEN mac=XX:XX:...` | Pobrano z AppPrefs | Jeśli taki sam jak FRONT → problem |
| `resolveTargetConfig: FRONT mac=YY:YY:...` | Pobrano FRONT | Sprawdź czy różny od KITCHEN |

---

## 📊 KRYTERIA SUKCESU

✅ **Drukarka kuchenna działa poprawnie jeśli:**
1. Logi pokazują **różne MACs** dla KITCHEN i FRONT
2. Fizycznie drukuje na **dwóch różnych urządzeniach**
3. Każda drukarka używa **swojego szablonu**

---

## 📝 PLIKI ZMIENIONE

### PrinterService.kt
- ✅ Dodano logi do `resolveTargetConfig()`
- Liczba zmian: ~15 linii

### PrinterSettingsScreen.kt
- ✅ Dodano logi do wyboru drukarki kuchennej
- ✅ Dodano import Timber
- Liczba zmian: ~5 linii

---

## 🚀 NASTĘPNE KROKI

### 1. Skompiluj i Zainstaluj
```powershell
cd "L:\SHOP APP"
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 2. Uruchom Logi
```powershell
adb logcat -c
adb logcat | Select-String "resolveTargetConfig|PrinterSettings"
```

### 3. Skonfiguruj Drukarki
- Upewnij się że KITCHEN i FRONT mają **różne MACs**

### 4. Testuj
- Zaakceptuj zamówienie
- Sprawdź logi
- Zweryfikuj wydruki

---

**Data:** 2026-01-21  
**Status:** ✅ DODANO LOGI DIAGNOSTYCZNE  
**Cel:** Zidentyfikować dlaczego KITCHEN używa tego samego MAC co FRONT

