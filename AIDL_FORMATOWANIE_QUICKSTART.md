# 🚀 QUICK START - Wdrożenie poprawy formatowania AIDL

## 📋 PODSUMOWANIE WYKONAWCZE

**Problem**: Drukarka wbudowana (AIDL) drukuje tagi formatujące jako tekst (`[C]<b>Z-12345</b>` zamiast wyśrodkowanego i pogrubionego numeru).

**Rozwiązanie**: Parser + Renderer konwertujący tagi ESC/POS na wywołania AIDL.

**Status**: ✅ **KOD GOTOWY - WYMAGA TESTÓW NA URZĄDZENIU**

**Zmienione pliki**:
- ✅ `AidlFormattingParser.kt` - UTWORZONY
- ✅ `AidlFormattingRenderer.kt` - UTWORZONY
- ✅ `AidlPrinterService.kt` - ZMODYFIKOWANY

---

## ⚡ SZYBKI START (5 minut)

### KROK 1: Kompilacja projektu
```powershell
cd "L:\SHOP APP"
.\gradlew assembleDebug
```

**Oczekiwany rezultat**: `BUILD SUCCESSFUL`

### KROK 2: Instalacja na terminalu H10
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### KROK 3: Test drukowania
1. Otwórz aplikację na terminalu H10
2. Przejdź do ekranu zamówień
3. Zaakceptuj dowolne zamówienie (automatyczne drukowanie)
4. **Sprawdź wydruk**:
   - ✅ Numer zamówienia wyśrodkowany i pogrubiony
   - ✅ Typ dostawy wyśrodkowany
   - ✅ Ceny wyrównane poprawnie

### KROK 4: Weryfikacja logów
```powershell
adb logcat -s "AidlFormattingParser:D" "AidlFormattingRenderer:D" "AidlPrinterService:D"
```

**Szukaj**:
```
🔍 Parser: Przetwarzam 15 linii
✅ Parser: Wygenerowano 23 segmentów
🖨️ [CLONE RENDERER] Start: 23 segmentów, autoCut=true
   [0] text='Z-12345...', align=1, bold=true, dw=true, dh=false
   [1] text='DOSTAWA...', align=1, bold=true, dw=true, dh=false
✅ [CLONE RENDERER] Sukces: 23 segmentów wydrukowanych
```

---

## 🔍 CO ZOSTAŁO ZMIENIONE?

### PRZED (problematyczny kod):
```kotlin
// AidlPrinterService.kt - handleClonePrint()
s.setAlignment(0)       // ❌ zawsze left
s.setTextBold(false)    // ❌ nigdy nie pogrubia
s.printText(text)       // ❌ drukuje "[C]<b>Z-12345</b>" dosłownie
```

### PO (nowy kod):
```kotlin
// AidlPrinterService.kt - handleClonePrint()
val segments = AidlFormattingParser.parse(text)  // 1. Parsuj tagi
val success = AidlFormattingRenderer.renderClone(s, segments, autoCut)  // 2. Renderuj

// AidlFormattingRenderer.kt - renderClone()
segments.forEach { segment ->
    service.setAlignment(segment.alignment)     // ✅ wyśrodkuj jeśli [C]
    service.setTextBold(segment.bold)           // ✅ pogrub jeśli <b>
    service.setTextDoubleWidth(segment.doubleWidth)  // ✅ podwójna szerokość jeśli <font size='wide'>
    service.printText(segment.text)             // ✅ drukuj czysty tekst
}
```

---

## 📂 NOWE PLIKI

### 1. `AidlFormattingParser.kt`
**Lokalizacja**: `l:\SHOP APP\app\src\main\java\com\itsorderchat\ui\settings\print\AidlFormattingParser.kt`

**Rozmiar**: ~220 linii

**Funkcja**: Parsuje tekst z tagami ESC/POS na listę segmentów.

**API**:
```kotlin
data class FormattedSegment(
    val text: String,
    val alignment: Int = 0,      // 0=left, 1=center, 2=right
    val bold: Boolean = false,
    val doubleWidth: Boolean = false,
    val doubleHeight: Boolean = false
)

fun parse(input: String): List<FormattedSegment>
fun stripAllTags(input: String): String  // fallback
```

**Wspierane tagi**:
- `[C]` / `[L]` / `[R]` - alignment
- `<b>...</b>` - bold
- `<u>...</u>` - underline
- `<font size='wide'>` - double width
- `<font size='tall'>` - double height
- `<font size='big'>` - double width + height

---

### 2. `AidlFormattingRenderer.kt`
**Lokalizacja**: `l:\SHOP APP\app\src\main\java\com\itsorderchat\ui\settings\print\AidlFormattingRenderer.kt`

**Rozmiar**: ~330 linii

**Funkcja**: Renderuje sparsowane segmenty przez AIDL interface.

**API**:
```kotlin
fun renderClone(
    service: PrinterInterface, 
    segments: List<FormattedSegment>,
    autoCut: Boolean = false
): Boolean

fun renderSenraise(
    service: com.senraise.printer.IService,
    segments: List<FormattedSegment>,
    autoCut: Boolean = false
): Boolean

fun renderWoyou(
    service: woyou.aidlservice.jiuiv5.IWoyouService,
    segments: List<FormattedSegment>,
    autoCut: Boolean = false
): Boolean
```

**Wspierane drukarki**:
- ✅ CLONE (PrinterInterface) - terminal H10 i podobne
- ✅ SENRAISE (IService) - oryginalna drukarka Senraise
- ✅ WOYOU (IWoyouService) - Sunmi i kompatybilne

---

## 🧪 TESTY

### Test 1: Podstawowe formatowanie
**Wejście**:
```
[C]<b>Z-12345</b>
[L]Test line
```

**Oczekiwane segmenty**:
```kotlin
[
    FormattedSegment("Z-12345\n", alignment=1, bold=true),
    FormattedSegment("Test line\n", alignment=0, bold=false)
]
```

**Oczekiwany wydruk**:
```
       Z-12345        ← WYŚRODKOWANE, POGRUBIONE
Test line             ← LEWOSTRONNE, NORMALNE
```

---

### Test 2: Podwójna szerokość
**Wejście**:
```
[C]<font size='wide'><b>DOSTAWA</b></font>
```

**Oczekiwane wywołania AIDL**:
```kotlin
service.setAlignment(1)           // center
service.setTextBold(true)         // bold
service.setTextDoubleWidth(true)  // wide
service.printText("DOSTAWA\n")
```

**Oczekiwany wydruk**:
```
      D O S T A W A      ← WYŚRODKOWANE, POGRUBIONE, PODWÓJNA SZEROKOŚĆ
```

---

### Test 3: Kompletny paragon
**Scenariusz**: Zaakceptuj zamówienie i sprawdź wydruk

**Checklist**:
- [ ] Numer zamówienia (np. "Z-12345") - wyśrodkowany, pogrubiony, podwójna szerokość
- [ ] Typ dostawy ("DOSTAWA"/"ODBIÓR") - wyśrodkowany, pogrubiony, podwójna szerokość
- [ ] Separator "---..." - lewostronne, normalne
- [ ] Data i godzina - lewostronne, normalne
- [ ] Dane klienta - lewostronne, normalne
- [ ] Czas dostawy - wyśrodkowany, pogrubiony (jeśli klient czeka)
- [ ] Pozycje zamówienia - lewostronne, ceny dosunięte do prawej
- [ ] Suma - lewostronne, pogrubiona
- [ ] Stopka "Dziękujemy!" - wyśrodkowana, pogrubiona

---

## 🐛 TROUBLESHOOTING

### Problem: Wydruk nadal pokazuje tagi `[C]<b>...`
**Diagnoza**: Parser nie działa lub nie został wywołany

**Rozwiązanie**:
1. Sprawdź logi:
   ```
   adb logcat | findstr /i "Parser"
   ```
2. Szukaj linii: `🔍 Parser: Przetwarzam X linii`
3. Jeśli NIE MA → sprawdź czy `AidlPrinterService.handleClonePrint()` został poprawnie zmodyfikowany

**Kod diagnostyczny** (dodaj do `handleClonePrint()`):
```kotlin
Timber.e("🔴 DEBUG: text = ${text.take(100)}")
val segments = AidlFormattingParser.parse(text)
Timber.e("🔴 DEBUG: segments.size = ${segments.size}")
segments.take(3).forEach { 
    Timber.e("🔴 DEBUG: segment = $it") 
}
```

---

### Problem: Wydruk jest pusty / nic się nie drukuje
**Diagnoza**: AIDL service nie połączony lub błąd renderowania

**Rozwiązanie**:
1. Sprawdź czy AIDL service jest połączony:
   ```
   adb logcat | findstr /i "CLONE"
   ```
2. Szukaj: `✅ Typ: KLON (recieptservice)` lub `❌ ROZŁĄCZENIE`
3. Jeśli rozłączony → restartuj aplikację
4. Jeśli nadal problem → sprawdź czy pakiet `recieptservice.com.recieptservice` jest zainstalowany:
   ```powershell
   adb shell pm list packages | findstr recieptservice
   ```

**Oczekiwany output**: `package:recieptservice.com.recieptservice`

---

### Problem: Formatowanie częściowo działa (np. bold OK, ale center nie)
**Diagnoza**: AIDL interface może nie wspierać niektórych metod

**Rozwiązanie**:
1. Sprawdź logi renderer:
   ```
   adb logcat | findstr /i "RENDERER"
   ```
2. Szukaj ostrzeżeń: `⚠️ setAlignment failed`
3. Jeśli metoda nie działa → dodaj fallback w `AidlFormattingRenderer.kt`:
   ```kotlin
   try {
       service.setAlignment(segment.alignment)
   } catch (e: Exception) {
       Timber.w("⚠️ setAlignment not supported, using manual padding")
       // Symuluj center przez spacje (jak w renderWoyou)
       val paddedText = alignCenter(segment.text, 32)
       service.printText(paddedText)
       return@forEach  // pomiń standardowe printText()
   }
   ```

---

### Problem: Wydruk działa, ale wydajność spadła
**Diagnoza**: Parsing może być kosztowny dla dużych dokumentów

**Rozwiązanie**:
1. Zmierz czas parsowania:
   ```kotlin
   val startTime = System.currentTimeMillis()
   val segments = AidlFormattingParser.parse(text)
   val parseTime = System.currentTimeMillis() - startTime
   Timber.d("⏱️ Parse time: ${parseTime}ms")
   ```
2. Jeśli > 100ms → dodaj cache:
   ```kotlin
   private val parseCache = mutableMapOf<String, List<FormattedSegment>>()
   
   fun parseWithCache(text: String): List<FormattedSegment> {
       return parseCache.getOrPut(text) { parse(text) }
   }
   ```

---

## 📊 MONITORING PRODUKCYJNY

### Kluczowe metryki do monitorowania:

1. **Sukces drukowania**:
   ```kotlin
   // W AidlFormattingRenderer.kt
   Timber.d("✅ [CLONE RENDERER] Sukces: ${segments.size} segmentów")
   ```

2. **Czas renderowania**:
   ```kotlin
   val startTime = System.currentTimeMillis()
   val success = renderClone(...)
   val renderTime = System.currentTimeMillis() - startTime
   Timber.d("⏱️ Render time: ${renderTime}ms, success=$success")
   ```

3. **Błędy AIDL**:
   ```kotlin
   Timber.e("❌ [CLONE RENDERER] Błąd drukowania")
   // Alert jeśli > 5% drukowań kończy się błędem
   ```

---

## ✅ CHECKLIST WDROŻENIA

### Przed wdrożeniem:
- [x] Utworzono `AidlFormattingParser.kt`
- [x] Utworzono `AidlFormattingRenderer.kt`
- [x] Zmodyfikowano `AidlPrinterService.kt`
- [x] Brak błędów kompilacji
- [ ] Testy jednostkowe dodane (OPCJONALNIE)
- [ ] Code review wykonany

### Po wdrożeniu na DEV:
- [ ] Zainstalowano na terminalu testowym H10
- [ ] Test 1: Podstawowe formatowanie ✅
- [ ] Test 2: Podwójna szerokość ✅
- [ ] Test 3: Kompletny paragon ✅
- [ ] Weryfikacja logów - brak błędów
- [ ] Wydajność OK (parsing < 50ms)

### Przed wdrożeniem na PROD:
- [ ] Testy na 3 różnych urządzeniach
- [ ] Testy z 10+ zamówieniami
- [ ] Monitoring logów przez 1 dzień
- [ ] Brak zgłoszeń problemów od testerów
- [ ] Dokumentacja zaktualizowana
- [ ] Rollback plan przygotowany

---

## 🔄 ROLLBACK (jeśli coś pójdzie nie tak)

### Szybki rollback - powrót do starego kodu:

**KROK 1**: Przywróć starą wersję `AidlPrinterService.kt`:
```kotlin
// Stara wersja handleClonePrint()
private fun handleClonePrint(text: String, autoCut: Boolean): Boolean {
    val s = cloneService ?: return false
    
    return runCatching {
        s.beginWork()
        s.setAlignment(0)
        s.setTextBold(false)
        s.printText(text)
        s.nextLine(5)
        s.endWork()
        true
    }.onFailure {
        Timber.e(it, "❌ [CLONE] print failed")
    }.getOrDefault(false)
}
```

**KROK 2**: Usuń wywołania parsera z `printText()`:
```kotlin
ServiceType.SENRAISE -> {
    senraiseService?.let {
        it.updatePrinterState()
        it.printText(text + "\n\n\n")
        if (autoCut) it.cutPaper()
    }
    true
}

ServiceType.WOYOU -> {
    woyouService?.let {
        it.printerInit(null)
        it.printText(text + "\n\n\n", null)
        if (autoCut) it.paperCut(null)
    }
    true
}
```

**KROK 3**: Rekompiluj i zainstaluj:
```powershell
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 📞 WSPARCIE

### Logi do przesłania w razie problemów:
```powershell
adb logcat -d > logcat_full.txt
adb logcat -d -s "AidlFormattingParser:*" "AidlFormattingRenderer:*" "AidlPrinterService:*" > logcat_aidl.txt
```

### Informacje diagnostyczne:
```powershell
# Wersja aplikacji
adb shell dumpsys package com.itsorderchat | findstr versionName

# Zainstalowane drukarki
adb shell pm list packages | findstr -i "print"

# Model urządzenia
adb shell getprop ro.product.model
```

---

**Data utworzenia**: 2026-01-24  
**Wersja dokumentu**: 1.0  
**Status**: ✅ GOTOWY DO TESTÓW

