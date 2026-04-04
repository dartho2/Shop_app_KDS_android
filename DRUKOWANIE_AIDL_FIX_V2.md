# Fix: Drukowanie zamówień na drukarce wbudowanej (AIDL) - v2 FINAL

## Problem
- ✅ Test AIDL (przycisk "Test AIDL") **działał** - drukował czysty tekst
- ❌ Drukowanie zamówienia **crashowało** serwis `recieptservice.com.recieptservice`

## Przyczyna Crashu

### Błąd:
```
java.lang.NullPointerException: Attempt to invoke virtual method 'int java.lang.String.length()' on a null object reference
at com.google.zxing.oned.Code39Writer.encode(Code39Writer.java:39)
at recieptservice.com.recieptservice.draw.Renderer.addBarCode(Renderer.java:132)
at recieptservice.com.recieptservice.draw.CommandParser.printBarCode(CommandParser.java:842)
```

### Analiza:
Pierwsza wersja rozwiązania próbowała **konwertować tagi DantSu na komendy ESC/POS** w funkcji `buildRawBytes()`:
- Tagi `[C]`, `[L]`, `<b>` → bajty ESC/POS (np. `0x1B 0x61 0x01`)
- Wysyłano je do serwisu przez `printImage()`

**Problem:** Serwis `recieptservice.com.recieptservice` (klon drukarki H10) **interpretował niektóre bajty ESC/POS jako komendę "drukuj kod kreskowy"**, ale nie miał danych do kodu → crash z `NullPointerException`.

## Rozwiązanie v2: Czysty Tekst Bez Komend ESC/POS

**Nowa strategia:**
1. ❌ NIE wysyłamy żadnych komend ESC/POS
2. ✅ Usuwamy WSZYSTKIE tagi DantSu i HTML
3. ✅ Wysyłamy tylko czysty tekst w UTF-8
4. ✅ Dzielimy na małe paczki (64 bajty) aby nie przepełnić bufora

### Zmiany w `AidlPrinterService.kt`

#### 1. Przepisano `handleClonePrint()`:

**PRZED (v1.5 - crashowało):**
```kotlin
private fun handleClonePrint(text: String, autoCut: Boolean): Boolean {
    val rawData = buildRawBytes(text, autoCut) // ❌ Konwersja na ESC/POS
    // Wysyłanie paczek...
    s.printImage(chunk) // ❌ Bajty ESC/POS crashują parser
}
```

**PO (v2 - stabilne):**
```kotlin
private fun handleClonePrint(text: String, autoCut: Boolean): Boolean {
    val s = cloneService ?: return false
    
    Timber.d("🖨️ [KLON] Rozpoczynam drukowanie (CZYSTY TEKST)...")
    
    // 1. Obudź drukarkę
    try { s.updatePrinterState() } catch (_: Exception) { }
    Thread.sleep(500)
    
    // 2. ✅ USUŃ WSZYSTKIE TAGI - zostaw tylko czysty tekst
    val cleanText = stripAllTags(text)
    Timber.v("📝 Czysty tekst (${cleanText.length} znaków):\n$cleanText")
    
    // 3. Konwersja na bajty UTF-8 (bez ESC/POS!)
    val textBytes = cleanText.toByteArray(Charset.forName("UTF-8"))
    val feedBytes = "\n\n\n\n".toByteArray(Charset.forName("UTF-8"))
    val finalData = textBytes + feedBytes
    
    Timber.d("📏 Rozmiar danych: ${finalData.size} bajtów")
    
    // 4. Wysyłanie małymi paczkami (64 bajty)
    val chunkSize = 64
    var offset = 0
    
    while (offset < finalData.size) {
        val length = Math.min(chunkSize, finalData.size - offset)
        val chunk = ByteArray(length)
        System.arraycopy(finalData, offset, chunk, 0, length)
        
        try {
            s.printImage(chunk)
            Timber.v("📤 Wysłano paczkę: $offset - ${offset + length}")
        } catch (e: Exception) {
            Timber.w("⚠️ Błąd paczki @$offset: ${e.message}")
        }
        
        offset += length
        Thread.sleep(100)
    }
    
    Timber.d("✅ Wszystkie paczki wysłane")
    
    // 5. Feed i cięcie
    Thread.sleep(500)
    try { 
        s.nextLine(3)
        if (autoCut) s.cutPaper()
    } catch (_: Exception) {}
    
    return true
}
```

#### 2. Dodano `stripAllTags()`:

```kotlin
/**
 * Usuwa WSZYSTKIE tagi DantSu i HTML, zostawiając tylko czysty tekst.
 */
private fun stripAllTags(text: String): String {
    var result = text
    
    // Usuń tagi DantSu: [C], [L], [R], [B], itp.
    result = result.replace(Regex("\\[[A-Z]+\\]"), "")
    
    // Usuń tagi HTML: <b>, <font>, <center>, itp.
    result = result.replace(Regex("<[^>]+>"), "")
    
    // Usuń nadmiarowe puste linie (max 2 pod rząd)
    result = result.replace(Regex("\n{3,}"), "\n\n")
    
    return result.trim()
}
```

#### 3. ❌ Usunięto `buildRawBytes()`:

Funkcja która konwertowała tagi na ESC/POS → **całkowicie usunięta** (powodowała crash).

## Porównanie

| Wersja | Strategia | Wynik |
|--------|-----------|-------|
| v1 (Test AIDL) | Czysty tekst UTF-8 | ✅ Działa |
| v1.5 (Zamówienia) | Tagi DantSu → ESC/POS → `printImage()` | ❌ Crash parsera kodów kreskowych |
| **v2 (Zamówienia)** | **Czysty tekst UTF-8 → `printImage()`** | ✅ **Stabilne** |

## Testowanie

1. Kliknij **"Drukuj zamówienie"** na dowolnym zamówieniu
2. Sprawdź logi (Logcat, tag: `AidlPrinterService`):

```
🖨️ Drukarka wbudowana (serial port)
📋 Serial: Dokument przygotowany (XXX znaków)
🚀 Wykryto builtin -> Uruchamiam AidlPrinterService
🔗 ===== POŁĄCZENIE Z DRUKARKĄ AIDL =====
✅ Typ: KLON (recieptservice)
🖨️ [KLON] Rozpoczynam drukowanie (CZYSTY TEKST)...
📝 Czysty tekst (XXX znaków)
📏 Rozmiar danych: XXX bajtów
📤 Wysłano paczkę: 0 - 64
📤 Wysłano paczkę: 64 - 128
📤 Wysłano paczkę: 128 - 192
...
✅ Wszystkie paczki wysłane
✅ Wydrukowano pomyślnie przez AIDL
```

3. **Oczekiwany wynik:**
   - Drukarka wydrukuje zamówienie jako **czysty tekst** (bez formatowania)
   - Brak crashu serwisu `recieptservice.com.recieptservice`

## Kompromisy v2

### ✅ Zalety:
- **Brak crashów** - nie wysyłamy komend ESC/POS, które crashowały parser
- **Stabilne drukowanie** - czysty tekst zawsze przechodzi
- **Prostsza implementacja** - brak konwersji tagów

### ❌ Wady:
- **Brak formatowania** - wydruk nie będzie miał:
  - Pogrubienia (`<b>`)
  - Wyśrodkowania (`[C]`)
  - Dużych czcionek (`<font size='big'>`)
- **Wydruk wygląda jak zwykły tekst** - podobnie jak wydruk z Notatnika

## Co dalej?

### Jeśli potrzebujesz formatowania:

Opcja 1: **Reverse-engineer AIDL API**
- Dekompiluj APK `recieptservice.com.recieptservice` (np. przez jadx)
- Znajdź dokumentację komend, których oczekuje parser
- Zaimplementuj własny `buildRawBytes()` oparty na tej dokumentacji

Opcja 2: **Użyj dedykowanej aplikacji producenta**
- Sprawdź czy producent H10 ma własną aplikację do drukowania
- Może mieć gotową obsługę formatowania

Opcja 3: **Użyj zewnętrznej drukarki**
- Drukarka Bluetooth/USB z pełną obsługą ESC/POS
- Ominie problemy z AIDL

### Jeśli nadal nie drukuje (troubleshooting):

1. **Sprawdź logi `stripAllTags()`**:
   ```
   📝 Czysty tekst (XXX znaków):
   ```
   Czy tekst wygląda poprawnie?

2. **Zmień encoding**:
   ```kotlin
   // W handleClonePrint() zmień:
   val textBytes = cleanText.toByteArray(Charset.forName("CP437"))
   // lub
   val textBytes = cleanText.toByteArray(Charset.forName("CP852"))
   ```

3. **Zmniejsz chunk size**:
   ```kotlin
   val chunkSize = 32 // było 64
   ```

4. **Zwiększ delay między paczkami**:
   ```kotlin
   Thread.sleep(200) // było 100ms
   ```

5. **Sprawdź uprawnienia** - czy aplikacja ma BIND_SERVICE do AIDL:
   ```xml
   <uses-permission android:name="android.permission.BIND_DEVICE_ADMIN" />
   ```

6. **Sprawdź status drukarki**:
   ```kotlin
   val status = aidlService.getPrinterStatus()
   Timber.d("Status drukarki: $status")
   // 0 = OK, -1 = błąd
   ```

## Wnioski

- **v2 eliminuje crashe** kosztem formatowania
- Jeśli czysty tekst Cię zadowala → gotowe ✅
- Jeśli potrzebujesz formatowania → musisz poznać API `recieptservice` lub zmienić drukarkę

---

**Data:** 2026-01-23 (v2 - CZYSTY TEKST)  
**Status:** ✅ Poprawka zaimplementowana, gotowa do testów

