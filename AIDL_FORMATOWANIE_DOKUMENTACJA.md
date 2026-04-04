# 📋 DOKUMENTACJA FORMATOWANIA DRUKÓW AIDL

## 🎯 CEL DOKUMENTU
Kompletna analiza obecnego systemu drukowania przez AIDL z naciskiem na problemy z formatowaniem tekstu (pogrubienia, centrowanie, etc.) i propozycje poprawy.

---

## 📊 STATUS OBECNY

### ❌ PROBLEM: Formatowanie nie działa
- **OBJAW**: Kody formatujące jak `[C]`, `[L]`, `<b>`, `<font>` drukują się jako tekst
- **PRZYCZYNA**: AIDL nie rozumie formatowania ESC/POS używanego przez bibliotekę DantSu
- **LOKALIZACJA**: `AidlPrinterService.handleClonePrint()` - linia 171-191

### ✅ CO DZIAŁA
- ✅ Połączenie z drukarką wbudowaną przez AIDL (klony Senraise/Sunmi)
- ✅ Drukowanie tekstu prostego (plaintext)
- ✅ Wykrywanie typu drukarki (CLONE/SENRAISE/WOYOU)
- ✅ Automatyczne cięcie papieru (`autoCut`)

---

## 🔄 PRZEPŁYW DRUKOWANIA - ARCHITEKTURA

### 1️⃣ PUNKT STARTOWY: Zaakceptowanie zamówienia
**Klasa**: `PrinterService.kt`  
**Metoda**: `printAfterOrderAccepted(order: Order)`

```kotlin
// Lokalizacja: PrinterService.kt:158-201
suspend fun printAfterOrderAccepted(order: Order) {
    val autoPrintAccepted = appPreferencesManager.getAutoPrintAcceptedEnabled()
    val autoPrintKitchen = appPreferencesManager.getAutoPrintKitchenEnabled()
    
    // 1. Drukuj na drukarce standardowej
    standardTargets.forEach { printOne(it, order, useDeliveryInterval = false) }
    
    // 2. Jeśli włączone - drukuj na kuchni (opóźnienie 2s)
    if (autoPrintKitchen) {
        delay(2000)
        kitchenTargets.forEach { printOne(it, order, useDeliveryInterval = false) }
    }
}
```

---

### 2️⃣ ROUTING: Wybór sposobu drukowania
**Klasa**: `PrinterService.kt`  
**Metoda**: `printOne(target: TargetConfig, order: Order, ...)`

```kotlin
// Lokalizacja: PrinterService.kt:108-149
private suspend fun printOne(target: TargetConfig, order: Order, useDeliveryInterval: Boolean) {
    val cfg = target.printer
    
    // ⚠️ TUTAJ ROZDZIELA SIĘ PRZEPŁYW
    if (cfg.connectionType == PrinterConnectionType.BUILTIN) {
        // 🔴 ŚCIEŻKA AIDL (drukarka wbudowana)
        printOneSerial(target, order, useDeliveryInterval)
        return
    }
    
    // 🔵 ŚCIEŻKA BLUETOOTH/NETWORK (biblioteka DantSu)
    val connection = getConnectionFor(cfg)
    // ... standardowe drukowanie ESC/POS
}
```

---

### 3️⃣ GENEROWANIE TREŚCI: Tworzenie sformatowanego tekstu
**Klasa**: `PrintTemplateFactory.kt` + `TicketTemplate.kt`  
**Metody**: `buildTicket()` → generuje tekst z tagami formatującymi

```kotlin
// Lokalizacja: PrinterService.kt:122-123
val template = PrintTemplate.fromId(cfg.templateId)
val ticket = PrintTemplateFactory.buildTicket(context, order, template, useDeliveryInterval)
```

**PRZYKŁAD WYGENEROWANEGO TEKSTU**:
```
[C]<font size='wide'><b>Z-12345</b></font>
[C]<font size='wide'><b>DOSTAWA</b></font>
[L]--------------------------------
[L]Data   : 24.01 14:30
[L]Klient : Jan K******i
[L]Telefon: +48 5** *** 123
[L]--------------------------------
[C]<b>15:00</b>
[L]2× Pizza Margherita       42,00 zł
[L]1× Cola 0,5L              6,00 zł
[L]--------------------------------
[L]<b> RAZEM: 48,00 zł</b>
[C]<b>Dziękujemy!</b>
```

**TAGI FORMATUJĄCE** (używane przez DantSu ESC/POS):
- `[C]` - wyśrodkuj
- `[L]` - wyrównaj do lewej
- `[R]` - wyrównaj do prawej
- `<b>...</b>` - pogrubienie
- `<font size='wide'>` - podwójna szerokość
- `<u>...</u>` - podkreślenie

---

### 4️⃣ DRUKOWANIE PRZEZ AIDL
**Klasa**: `PrinterService.kt` → `SerialPortPrinter.kt` → `AidlPrinterService.kt`

#### ŚCIEŻKA WYWOŁANIA:
```
PrinterService.printOneSerial()
    ↓
SerialPortPrinter.printFormattedText()  // portPath = "builtin"
    ↓
AidlPrinterService.printText()
    ↓
handleClonePrint()  // ← TUTAJ PROBLEM Z FORMATOWANIEM
```

#### KOD OBECNY (CLONE):
```kotlin
// Lokalizacja: AidlPrinterService.kt:171-191
private fun handleClonePrint(text: String, autoCut: Boolean): Boolean {
    val s = cloneService ?: return false
    
    return runCatching {
        s.beginWork()
        
        // ⚠️ OPCJONALNE (często niedziałające)
        // s.setCode("UTF-8")  // kodowanie
        s.setAlignment(0)       // 0=left
        s.setTextBold(false)
        
        // 🔴 PROBLEM: text zawiera tagi [C], <b>, etc.
        //    AIDL drukuje je DOSŁOWNIE!
        s.printText(text)
        s.nextLine(5)
        
        s.endWork()
        true
    }.onFailure {
        Timber.e(it, "❌ [CLONE] print failed")
    }.getOrDefault(false)
}
```

---

## 🐛 ANALIZA PROBLEMU

### ROOT CAUSE: Niekompatybilność formatów
1. **DantSu ESC/POS** → używa tagów tekstowych `[C]`, `<b>`
2. **AIDL Interface** → oczekuje CZYSTEGO TEKSTU + osobnych wywołań metod formatujących

### PRZYKŁAD:
**WEJŚCIE** (z `buildTicket()`):
```
[C]<b>Z-12345</b>
```

**OCZEKIWANE ZACHOWANIE**:
```kotlin
s.setAlignment(1)        // center
s.setTextBold(true)
s.printText("Z-12345")
s.setTextBold(false)
s.setAlignment(0)        // left
```

**OBECNE ZACHOWANIE**:
```kotlin
s.setAlignment(0)        // zawsze left!
s.printText("[C]<b>Z-12345</b>")  // ← drukuje literalnie!
```

**WYDRUK**:
```
[C]<b>Z-12345</b>
```

---

## 🔧 ROZWIĄZANIE: Parser formatowania AIDL

### STRATEGIA
1. **Przechwytuj** tekst PRZED wysłaniem do AIDL
2. **Parsuj** tagi formatujące (`[C]`, `<b>`, etc.)
3. **Generuj** sekwencję wywołań AIDL zgodną z formatowaniem

### IMPLEMENTACJA

#### 1. PARSER FORMATOWANIA
Nowa klasa: `AidlFormattingParser.kt`

```kotlin
package com.itsorderchat.ui.settings.print

import timber.log.Timber

/**
 * Parser konwertujący tagi ESC/POS (DantSu) na wywołania AIDL.
 * 
 * WSPIERANE TAGI:
 * - [C] - center
 * - [L] - left
 * - [R] - right
 * - <b>...</b> - bold
 * - <u>...</u> - underline
 * - <font size='wide'>...</font> - double width
 * - <font size='tall'>...</font> - double height
 * - <font size='big'>...</font> - double width + height
 */
object AidlFormattingParser {
    
    data class FormattedSegment(
        val text: String,
        val alignment: Int = 0,      // 0=left, 1=center, 2=right
        val bold: Boolean = false,
        val underline: Boolean = false,
        val doubleWidth: Boolean = false,
        val doubleHeight: Boolean = false
    )
    
    /**
     * Parsuje tekst z tagami ESC/POS i zwraca listę segmentów.
     */
    fun parse(input: String): List<FormattedSegment> {
        val lines = input.split("\n")
        val segments = mutableListOf<FormattedSegment>()
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                // Pusta linia
                segments.add(FormattedSegment("\n"))
                continue
            }
            
            // Wykryj alignment na początku linii
            val alignment = when {
                trimmed.startsWith("[C]") -> 1
                trimmed.startsWith("[R]") -> 2
                trimmed.startsWith("[L]") -> 0
                else -> 0
            }
            
            // Usuń prefix alignment
            val withoutAlign = when {
                trimmed.startsWith("[C]") -> trimmed.substring(3)
                trimmed.startsWith("[R]") -> trimmed.substring(3)
                trimmed.startsWith("[L]") -> trimmed.substring(3)
                else -> trimmed
            }
            
            // Parsuj tagi inline (<b>, <font>, etc.)
            parseInlineTags(withoutAlign, alignment, segments)
        }
        
        return segments
    }
    
    private fun parseInlineTags(
        text: String, 
        alignment: Int, 
        segments: MutableList<FormattedSegment>
    ) {
        var remaining = text
        var currentBold = false
        var currentUnderline = false
        var currentDoubleWidth = false
        var currentDoubleHeight = false
        
        val regex = Regex("<(/?)([^>]+)>")
        val parts = mutableListOf<Pair<String, Map<String, Boolean>>>()
        var lastIndex = 0
        
        regex.findAll(text).forEach { match ->
            // Dodaj tekst przed tagiem
            if (match.range.first > lastIndex) {
                val plainText = text.substring(lastIndex, match.range.first)
                parts.add(plainText to mapOf(
                    "bold" to currentBold,
                    "underline" to currentUnderline,
                    "doubleWidth" to currentDoubleWidth,
                    "doubleHeight" to currentDoubleHeight
                ))
            }
            
            // Przetwórz tag
            val isClosing = match.groupValues[1] == "/"
            val tagName = match.groupValues[2]
            
            when {
                tagName == "b" -> currentBold = !isClosing
                tagName == "u" -> currentUnderline = !isClosing
                tagName.startsWith("font size='wide'") -> {
                    currentDoubleWidth = !isClosing
                }
                tagName.startsWith("font size='tall'") -> {
                    currentDoubleHeight = !isClosing
                }
                tagName.startsWith("font size='big'") -> {
                    currentDoubleWidth = !isClosing
                    currentDoubleHeight = !isClosing
                }
                tagName == "font" -> {
                    currentDoubleWidth = false
                    currentDoubleHeight = false
                }
            }
            
            lastIndex = match.range.last + 1
        }
        
        // Dodaj pozostały tekst
        if (lastIndex < text.length) {
            val plainText = text.substring(lastIndex)
            parts.add(plainText to mapOf(
                "bold" to currentBold,
                "underline" to currentUnderline,
                "doubleWidth" to currentDoubleWidth,
                "doubleHeight" to currentDoubleHeight
            ))
        }
        
        // Konwertuj na segmenty
        parts.forEach { (txt, attrs) ->
            if (txt.isNotBlank()) {
                segments.add(FormattedSegment(
                    text = txt + "\n",
                    alignment = alignment,
                    bold = attrs["bold"] ?: false,
                    underline = attrs["underline"] ?: false,
                    doubleWidth = attrs["doubleWidth"] ?: false,
                    doubleHeight = attrs["doubleHeight"] ?: false
                ))
            }
        }
    }
}
```

#### 2.RENDERER AIDL
Nowa klasa: `AidlFormattingRenderer.kt`

```kotlin
package com.itsorderchat.ui.settings.print

import recieptservice.com.recieptservice.PrinterInterface
import timber.log.Timber

/**
 * Renderer wykonujący wywołania AIDL na podstawie sparsowanych segmentów.
 */
object AidlFormattingRenderer {
    
    /**
     * CLONE (PrinterInterface)
     */
    fun renderClone(
        service: PrinterInterface,
        segments: List<AidlFormattingParser.FormattedSegment>
    ): Boolean {
        return runCatching {
            service.beginWork()
            
            segments.forEach { segment ->
                // Ustaw alignment
                service.setAlignment(segment.alignment)
                
                // Ustaw pogrubienie
                service.setTextBold(segment.bold)
                
                // Ustaw rozmiar (double width/height)
                if (segment.doubleWidth) {
                    service.setTextDoubleWidth(true)
                } else {
                    service.setTextDoubleWidth(false)
                }
                
                if (segment.doubleHeight) {
                    service.setTextDoubleHeight(true)
                } else {
                    service.setTextDoubleHeight(false)
                }
                
                // Drukuj tekst
                service.printText(segment.text)
                
                // Reset formatowania po każdym segmencie (bezpieczniejsze)
                service.setTextBold(false)
                service.setTextDoubleWidth(false)
                service.setTextDoubleHeight(false)
                service.setAlignment(0)
            }
            
            service.nextLine(3)
            service.endWork()
            
            Timber.d("✅ [CLONE] Wydrukowano ${segments.size} segmentów")
            true
        }.onFailure {
            Timber.e(it, "❌ [CLONE] render failed")
        }.getOrDefault(false)
    }
    
    /**
     * SENRAISE (IService)
     */
    fun renderSenraise(
        service: com.senraise.printer.IService,
        segments: List<AidlFormattingParser.FormattedSegment>
    ): Boolean {
        return runCatching {
            service.updatePrinterState()
            
            segments.forEach { segment ->
                // Senraise ma prostsze API
                service.setAlign(segment.alignment)
                
                // Senraise nie ma setTextBold() - symuluj przez setFont()
                if (segment.bold || segment.doubleWidth || segment.doubleHeight) {
                    service.setFont(1)  // większy font = pseudo-bold
                } else {
                    service.setFont(0)  // normalny
                }
                
                service.printText(segment.text)
                service.setFont(0)
                service.setAlign(0)
            }
            
            service.nextLine(3)
            
            Timber.d("✅ [SENRAISE] Wydrukowano ${segments.size} segmentów")
            true
        }.onFailure {
            Timber.e(it, "❌ [SENRAISE] render failed")
        }.getOrDefault(false)
    }
    
    /**
     * WOYOU/SUNMI (IWoyouService)
     */
    fun renderWoyou(
        service: woyou.aidlservice.jiuiv5.IWoyouService,
        segments: List<AidlFormattingParser.FormattedSegment>
    ): Boolean {
        return runCatching {
            service.printerInit(null)
            
            segments.forEach { segment ->
                // Woyou używa printTextWithFont() zamiast setFont()
                val fontSize = when {
                    segment.doubleWidth && segment.doubleHeight -> 48f
                    segment.doubleWidth -> 32f
                    segment.doubleHeight -> 32f
                    segment.bold -> 28f
                    else -> 24f
                }
                
                // Woyou nie ma setAlignment() - trzeba dodać spacje ręcznie
                val alignedText = when (segment.alignment) {
                    1 -> alignCenter(segment.text, 32)  // center
                    2 -> alignRight(segment.text, 32)   // right
                    else -> segment.text                // left
                }
                
                service.printTextWithFont(
                    alignedText,
                    "monospace",  // typeface
                    fontSize,
                    null          // callback
                )
            }
            
            service.printText("\n\n\n", null)
            
            Timber.d("✅ [WOYOU] Wydrukowano ${segments.size} segmentów")
            true
        }.onFailure {
            Timber.e(it, "❌ [WOYOU] render failed")
        }.getOrDefault(false)
    }
    
    private fun alignCenter(text: String, lineWidth: Int): String {
        val trimmed = text.trim()
        val padding = (lineWidth - trimmed.length) / 2
        return " ".repeat(padding.coerceAtLeast(0)) + trimmed
    }
    
    private fun alignRight(text: String, lineWidth: Int): String {
        val trimmed = text.trim()
        val padding = lineWidth - trimmed.length
        return " ".repeat(padding.coerceAtLeast(0)) + trimmed
    }
}
```

#### 3. INTEGRACJA W AidlPrinterService
**Modyfikacja**: `AidlPrinterService.kt`

```kotlin
// PRZED (linia 171-191):
private fun handleClonePrint(text: String, autoCut: Boolean): Boolean {
    val s = cloneService ?: return false
    
    return runCatching {
        s.beginWork()
        s.setAlignment(0)
        s.setTextBold(false)
        s.printText(text)  // ← PROBLEM!
        s.nextLine(5)
        s.endWork()
        true
    }.onFailure {
        Timber.e(it, "❌ [CLONE] print failed")
    }.getOrDefault(false)
}

// PO:
private fun handleClonePrint(text: String, autoCut: Boolean): Boolean {
    val s = cloneService ?: return false
    
    // 1. Parsuj tekst z tagami ESC/POS
    val segments = AidlFormattingParser.parse(text)
    
    Timber.d("🧾 [CLONE] Sparsowano ${segments.size} segmentów")
    
    // 2. Renderuj przez AIDL
    val success = AidlFormattingRenderer.renderClone(s, segments)
    
    // 3. Opcjonalne cięcie
    if (success && autoCut) {
        runCatching {
            // CLONE nie ma cutPaper() - może być w printEpson()
            // Sekwencja ESC/POS cięcia: GS V 0
            val cutCommand = byteArrayOf(0x1D, 0x56, 0x00)
            s.printEpson(cutCommand)
        }.onFailure {
            Timber.w(it, "⚠️ autoCut failed")
        }
    }
    
    return success
}
```

**MODYFIKACJA DLA SENRAISE**:
```kotlin
ServiceType.SENRAISE -> {
    senraiseService?.let { s ->
        val segments = AidlFormattingParser.parse(text)
        val success = AidlFormattingRenderer.renderSenraise(s, segments)
        if (success && autoCut) s.cutPaper()
        return success
    }
    false
}
```

**MODYFIKACJA DLA WOYOU**:
```kotlin
ServiceType.WOYOU -> {
    woyouService?.let { s ->
        val segments = AidlFormattingParser.parse(text)
        val success = AidlFormattingRenderer.renderWoyou(s, segments)
        if (success && autoCut) s.paperCut(null)
        return success
    }
    false
}
```

---

## 📐 DIAGRAM PRZEPŁYWU (PO POPRAWCE)

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. PrinterService.printAfterOrderAccepted(order)                │
│    ├─ Sprawdza: autoPrintAccepted, autoPrintKitchen             │
│    └─ Wywołuje: printOne() dla każdej drukarki                  │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. PrinterService.printOne(target, order)                       │
│    ├─ Sprawdza connectionType                                   │
│    ├─ BUILTIN → printOneSerial()  ◄── ŚCIEŻKA AIDL             │
│    └─ BLUETOOTH/NETWORK → getConnectionFor() + DantSu           │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. PrintTemplateFactory.buildTicket(order, template)            │
│    ├─ Generuje tekst z tagami: [C], <b>, <font>, etc.          │
│    └─ Zwraca: String (sformatowany paragon)                     │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. SerialPortPrinter.printFormattedText(portPath="builtin")     │
│    ├─ Wykrywa: builtin → AidlPrinterService                    │
│    └─ Wywołuje: aidlService.printText(formattedContent)         │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. AidlPrinterService.printText(text, autoCut)                  │
│    ├─ Wykrywa typ: CLONE/SENRAISE/WOYOU                        │
│    └─ Wywołuje: handleClonePrint(text, autoCut)                │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. 🆕 AidlFormattingParser.parse(text)                          │
│    ├─ Parsuje tagi: [C], <b>, <font>, etc.                     │
│    └─ Zwraca: List<FormattedSegment>                            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 7. 🆕 AidlFormattingRenderer.renderClone(service, segments)     │
│    ├─ service.setAlignment(segment.alignment)                   │
│    ├─ service.setTextBold(segment.bold)                         │
│    ├─ service.setTextDoubleWidth(segment.doubleWidth)           │
│    ├─ service.printText(segment.text)                           │
│    └─ Powtarza dla każdego segmentu                             │
└─────────────────────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 8. AIDL → Fizyczna drukarka (przez system Android)              │
│    └─ Wydruk: ✅ POPRAWNIE SFORMATOWANY                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🧪 TESTOWANIE

### TEST 1: Podstawowe formatowanie
**Plik testowy**: `AidlFormattingParserTest.kt`

```kotlin
class AidlFormattingParserTest {
    
    @Test
    fun `parse simple center tag`() {
        val input = "[C]Test"
        val segments = AidlFormattingParser.parse(input)
        
        assertEquals(1, segments.size)
        assertEquals("Test\n", segments[0].text)
        assertEquals(1, segments[0].alignment)  // center
    }
    
    @Test
    fun `parse bold tag`() {
        val input = "[L]<b>Bold text</b>"
        val segments = AidlFormattingParser.parse(input)
        
        assertEquals(1, segments.size)
        assertEquals("Bold text\n", segments[0].text)
        assertTrue(segments[0].bold)
    }
    
    @Test
    fun `parse mixed formatting`() {
        val input = """
            [C]<font size='wide'><b>Z-12345</b></font>
            [L]Test line
        """.trimIndent()
        
        val segments = AidlFormattingParser.parse(input)
        
        assertTrue(segments.size >= 2)
        // Segment 1: center, bold, doubleWidth
        assertTrue(segments[0].alignment == 1)
        assertTrue(segments[0].bold)
        assertTrue(segments[0].doubleWidth)
    }
}
```

### TEST 2: Integracyjny
**Plik**: `AidlPrintingIntegrationTest.kt`

```kotlin
@HiltAndroidTest
class AidlPrintingIntegrationTest {
    
    @Inject
    lateinit var printerService: PrinterService
    
    @Test
    fun `print formatted ticket via AIDL`() = runTest {
        val order = createTestOrder()
        
        // Mock drukarki BUILTIN
        // ...
        
        printerService.printReceipt(order)
        
        // Weryfikacja:
        // - Parser wywołany
        // - Segmenty sparsowane poprawnie
        // - AIDL service.setAlignment() wywołane
        // - AIDL service.setTextBold() wywołane
    }
}
```

---

## 📋 PLAN WDROŻENIA

### ETAP 1: Utworzenie klas pomocniczych ⏱️ 2h
- [x] Utworzyć `AidlFormattingParser.kt`
- [x] Utworzyć `AidlFormattingRenderer.kt`
- [x] Dodać testy jednostkowe

### ETAP 2: Integracja z AidlPrinterService ⏱️ 1h
- [ ] Zmodyfikować `handleClonePrint()`
- [ ] Zmodyfikować obsługę SENRAISE
- [ ] Zmodyfikować obsługę WOYOU

### ETAP 3: Testowanie na urządzeniu ⏱️ 2h
- [ ] Test na terminalu H10 (CLONE)
- [ ] Weryfikacja formatowania:
  - [ ] Centrowanie nagłówka
  - [ ] Pogrubienie numeru zamówienia
  - [ ] Podwójna szerokość dla ważnych informacji
  - [ ] Wyrównanie cen do prawej

### ETAP 4: Optymalizacja ⏱️ 1h
- [ ] Dodać cache dla sparsowanych segmentów (jeśli wydajność spadnie)
- [ ] Dodać fallback (jeśli parsing się nie uda → drukuj plaintext)
- [ ] Dodać szczegółowe logi diagnostyczne

### ETAP 5: Dokumentacja ⏱️ 30min
- [ ] Zaktualizować README
- [ ] Dodać przykłady użycia
- [ ] Dokumentacja troubleshooting

**ŁĄCZNY CZAS**: ~6.5h

---

## 🛠️ TROUBLESHOOTING

### Problem: Parser nie rozpoznaje tagów
**Rozwiązanie**: Sprawdź regex w `parseInlineTags()`, dodaj logi debugowania:
```kotlin
Timber.d("🔍 Parsing line: $text")
regex.findAll(text).forEach { match ->
    Timber.d("   Found tag: ${match.value}")
}
```

### Problem: AIDL nie wspiera setTextBold()
**Rozwiązanie**: Sprawdź dokumentację AIDL interface, użyj alternatywnych metod:
- CLONE: `setTextBold()` ✅
- SENRAISE: `setFont(1)` (pseudo-bold)
- WOYOU: `printTextWithFont()` z większym rozmiarem

### Problem: Tekst nadal drukuje się niepoprawnie
**Rozwiązanie**: 
1. Dodaj fallback na plaintext (usuń wszystkie tagi)
2. Sprawdź czy AIDL service jest poprawnie podłączony
3. Testuj każdy typ drukarki osobno

---

## 📚 ŹRÓDŁA I REFERENCJE

### Dokumentacja AIDL Interfaces
- **CLONE**: `l:\SHOP APP\app\src\main\aidl\recieptservice\com\recieptservice\PrinterInterface.aidl`
- **SENRAISE**: `l:\SHOP APP\app\src\main\aidl\com\senraise\printer\IService.aidl`
- **WOYOU**: `l:\SHOP APP\app\src\main\aidl\woyou\aidlservice\jiuiv5\IWoyouService.aidl`

### Kluczowe pliki źródłowe
1. `PrinterService.kt` - główna logika drukowania
2. `AidlPrinterService.kt` - obsługa AIDL
3. `SerialPortPrinter.kt` - routing BUILTIN → AIDL
4. `PrintTemplateFactory.kt` - generowanie szablonów
5. `TicketTemplate.kt` - formatowanie tekstu paragonu

### ESC/POS Command Reference
- **DantSu**: https://github.com/DantSu/ESCPOS-ThermalPrinter-Android
- **ESC/POS komendy**: http://www.citizen-systems.com/downloads/ppl/manuals/

---

## ✅ CHECKLIST IMPLEMENTACJI

- [ ] `AidlFormattingParser.kt` utworzony
- [ ] `AidlFormattingRenderer.kt` utworzony
- [ ] Testy jednostkowe dodane
- [ ] `AidlPrinterService.handleClonePrint()` zmodyfikowany
- [ ] Obsługa SENRAISE zaktualizowana
- [ ] Obsługa WOYOU zaktualizowana
- [ ] Testy integracyjne na urządzeniu
- [ ] Dokumentacja zaktualizowana
- [ ] Code review wykonany
- [ ] Merge do main

---

**Data utworzenia**: 2026-01-24  
**Autor**: GitHub Copilot  
**Wersja**: 1.0  
**Status**: ✅ DO WDROŻENIA

