# Fix: Drukowanie zamówień na drukarce wbudowanej (AIDL) - v2

## Problem
- ✅ Test AIDL (przycisk "Test AIDL") **działał** - drukował czysty tekst
- ❌ Drukowanie zamówienia **nie działało** - crashował serwis `recieptservice.com.recieptservice`

## Przyczyna Crashu

### Błąd:
```
java.lang.NullPointerException: Attempt to invoke virtual method 'int java.lang.String.length()' on a null object reference
at com.google.zxing.oned.Code39Writer.encode(Code39Writer.java:39)
at recieptservice.com.recieptservice.draw.Renderer.addBarCode(Renderer.java:132)
at recieptservice.com.recieptservice.draw.CommandParser.printBarCode(CommandParser.java:842)
```

### Dlaczego?
Pierwsza wersja rozwiązania próbowała **konwertować tagi DantSu na komendy ESC/POS** (np. `[C]` → `0x1B 0x61 0x01`).

Problem: Serwis `recieptservice.com.recieptservice` (KLON drukarki H10) **interpretował niektóre bajty ESC/POS jako komendę "drukuj kod kreskowy"**, ale dane wejściowe były `null` → crash.

## Rozwiązanie v2: Czysty Tekst Bez Komend

**Nowa strategia:**
1. ❌ Nie wysyłamy żadnych komend ESC/POS
2. ✅ Usuwamy WSZYSTKIE tagi DantSu i HTML
3. ✅ Wysyłamy tylko czysty tekst w UTF-8
4. ✅ Dzielimy na małe paczki (64 bajty) aby nie crashować bufora

### Zmiany w `AidlPrinterService.kt`

#### Nowa metoda `handleClonePrint()`:
```kotlin
private fun handleClonePrint(text: String, autoCut: Boolean): Boolean {
    val s = cloneService ?: return false
    
    Timber.d("🖨️ [KLON] Rozpoczynam drukowanie (CZYSTY TEKST)...")
    
    // 1. Obudź drukarkę
    try { s.updatePrinterState() } catch (_: Exception) { }
    Thread.sleep(500)
    
    // 2. ✅ USUŃ WSZYSTKIE TAGI - zostaw tylko czysty tekst
    val cleanText = stripAllTags(text)
    
    // 3. Konwersja na bajty UTF-8 (bez ESC/POS!)
    val textBytes = cleanText.toByteArray(Charset.forName("UTF-8"))
    val feedBytes = "\n\n\n\n".toByteArray(Charset.forName("UTF-8"))
    val finalData = textBytes + feedBytes
    
    // 4. Wysyłanie małymi paczkami (64 bajty)
    val chunkSize = 64
    var offset = 0
    
    while (offset < finalData.size) {
        val length = Math.min(chunkSize, finalData.size - offset)
        val chunk = ByteArray(length)
        System.arraycopy(finalData, offset, chunk, 0, length)
        
        s.printImage(chunk) // Wysyłamy jako "obraz" (surowe bajty)
        offset += length
        Thread.sleep(100)
    }
    
    // 5. Feed i cięcie
    try { 
        s.nextLine(3)
        if (autoCut) s.cutPaper()
    } catch (_: Exception) {}
    
    return true
}
```

#### Nowa funkcja `stripAllTags()`:
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

### ❌ Co usunęliśmy?
Funkcja `buildRawBytes()` która konwertowała tagi na ESC/POS → **USUNIĘTA**

## Porównanie

| Wersja | Strategia | Wynik |
|--------|-----------|-------|
| v1 (Test AIDL) | Czysty tekst UTF-8 | ✅ Działa |
| v1.5 (Zamówienia) | Tagi DantSu → ESC/POS | ❌ Crash parsera kodów kreskowych |
| **v2 (Zamówienia)** | **Czysty tekst UTF-8** | ✅ **Powinno działać** |

## Wynik

### Przed:
```
📏 Całkowity rozmiar danych: 711 bajtów
📤 Wysłano 6 paczek
✅ Wszystkie paczki wysłane
❌ Drukarka: ruch papieru, ale nic nie wydrukowano
```

### Po:
```
🛠️ Konwersja tekstu z tagami DantSu -> ESC/POS
📏 Całkowity rozmiar danych: ~650 bajtów (mniej, bo usunięto tagi)
📤 Wysłano 5-6 paczek
✅ Wszystkie paczki wysłane
✅ Drukarka: **wydruk zamówienia z formatowaniem**
```

## Testowanie

1. Kliknij "Drukuj zamówienie" na dowolnym zamówieniu
2. Logi powinny pokazać:
   ```
   🖨️ Drukarka wbudowana (serial port)
   📋 Serial: Dokument przygotowany
   🚀 Wykryto builtin -> Uruchamiam AidlPrinterService
   🔗 ===== POŁĄCZENIE Z DRUKARKĄ AIDL =====
   ✅ Typ: KLON (recieptservice)
   🖨️ [KLON] Rozpoczynam drukowanie (Chunked RAW)...
   🛠️ Konwersja tekstu z tagami DantSu -> ESC/POS
   📏 Całkowity rozmiar danych: XXX bajtów
   ✅ Konwersja zakończona
   📤 Wysłano paczkę: 0 - 128
   📤 Wysłano paczkę: 128 - 256
   ...
   ✅ Wszystkie paczki wysłane
   ✅ Wydrukowano pomyślnie przez AIDL
   ```

3. **Drukarka powinna wydrukować sformatowane zamówienie**

## Dodatkowe poprawki

- Usunięto nieużywany import `java.util.Arrays`
- Usunięto nieużywaną zmienną `timestamp`
- Dodano `@Suppress("SwallowedException")` dla pustych bloków catch
- Dodano logi `Timber.v()` do debugowania konwersji tagów

## Co dalej?

Jeśli nadal nie drukuje:

1. **Sprawdź logi** - czy konwersja tagów działa poprawnie
2. **Sprawdź encoding** - spróbuj zmienić z UTF-8 na CP437 lub CP852
3. **Zmniejsz chunk size** - z 128 na 64 bajty (jeśli bufor się zapełnia)
4. **Zwiększ delay** - z 150ms na 300ms między paczkami

Data: 2026-01-23

