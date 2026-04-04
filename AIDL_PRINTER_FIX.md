# NAPRAWA DRUKOWANIA AIDL (H10/Senraise/KLON)

## 🔴 PROBLEM
Drukarka H10/Senraise nie drukowała tekstu. Logika wysyłała ESC/POS surowe bajty do metody `printImage()`, ale ta metoda **oczekuje danych obrazu (bitmapy)**, a nie ESC/POS.

### Objaw:
```
📤 [KLON] Wysyłam 17 bajtów ESC/POS przez printImage()...
✅ [KLON] printImage() zwróciła (bez exception)
```
Drukarka: **cisza, minimalny feed, brak tekstu**

---

## 🟢 ROZWIĄZANIE

### Zmiana w `AidlPrinterService.kt` - metoda `handleClonePrint()`

**STARE (BŁĘDNE):**
```kotlin
// Wysyłanie surowych ESC/POS do printImage() - NIE DZIAŁA!
val esc = buildEscPosBytes(cleanText, autoCut)
s.printImage(esc)  // ❌ printImage() to nie dla ESC/POS!
```

**NOWE (PRAWIDŁOWE):**
```kotlin
// Używamy właściwych metod AIDL interfejsu
s.updatePrinterState()              // Inicjalizacja
s.printText(cleanText)              // ✅ PRAWIDŁOWA metoda do druku tekstu
s.nextLine(3)                       // Posunięcie papieru
if (autoCut) s.cutPaper()          // Opcjonalne cięcie
```

### Interfejs PrinterInterface (z AIDL)
```aidl
interface PrinterInterface {
    void updatePrinterState();      // Inicjalizacja
    void printText(String text);    // ✅ DRUK TEKSTU - TO JE ST WŁAŚCIWA METODA!
    void printImage(in byte[] data); // Dla bitmapy, NIE dla ESC/POS!
    void nextLine(int line);         // Feed papieru
    void cutPaper();                 // Cięcie
    int getPrinterStatus();
}
```

---

## ✅ INSTRUKCJA TESTOWANIA

### Scenariusz 1: Test "HELLO 123"
1. Otwórz ekran Ustawienia > Drukarki
2. Kliknij przycisk "Test AIDL (drukarka wbudowana)" (ikona Science)
3. Spodziewany wynik w logach:
```
🧪 AIDL test print (HELLO 123), serviceType=CLONE
📍 KROK 1: Inicjalizacja drukarki...
✅ printText() wysłano
📍 KROK 2: Wysyłam tekst przez printText()...
📍 KROK 3: Feed (posunięcie papieru)...
✅ [KLON] Drukowanie zakończone pomyślnie
```
4. **Drukarka powinna wydrukować:**
```
HELLO 123
```

### Scenariusz 2: Druk zamówienia
1. Otwórz listę zamówień
2. Zaakceptuj zamówienie (lub kliknij "Drukuj")
3. Spodziewany wynik: **pełny paragon z tekstem**

---

## 📊 DIAGNOSTYKA W LOGACH

Szukaj linii:
```
✅ [KLON] Drukowanie zakończone pomyślnie
```

Jeśli widzisz:
```
❌ [KLON] Błąd drukowania (printText): ...
```
→ Drukarka zgłasza błąd (status, permissions)

Jeśli widzisz:
```
⚠️ [KLON] Fallback: próbuję raw socket port 9100...
```
→ printText() zawiódł, teraz próbujemy port 9100 (mniej wiarygodne)

---

## 🔧 USUNIĘTE ELEMENTY

- ❌ Metoda `buildEscPosBytes()` - była do printImage(), teraz niepotrzebna
- ❌ `ByteArrayOutputStream` import - niepotrzebny
- ✅ Import `Charset` - pozostawiony na potrzeby raw socket fallback

---

## 📝 NOTATKA TECHNICZNA

AIDL interfejs Klona (recieptservice.com.recieptservice) definiuje:
- `printText(String)` - druk tekstu (właściwa metoda)
- `printImage(byte[])` - druk bitmapy (NIE dla ESC/POS)
- `nextLine(int)` - posunięcie papieru
- `cutPaper()` - cięcie

**Błąd polegał na tym, że konwertowaliśmy tekst do ESC/POS bajtów i wysyłaliśmy do printImage().** 
Interfejs AIDL już obsługuje wszystko poprzez `printText()` + `nextLine()` + `cutPaper()`.

---

## 🚀 WYNIK

Po naprawie drukarka powinna:
1. ✅ Drukować tekst prawidłowo
2. ✅ Obsługiwać polskie znaki
3. ✅ Posuwać papier
4. ✅ Ciąć papier (jeśli włączone)

---

**Data naprawy:** 2026-01-23  
**Plik:** `AidlPrinterService.kt`  
**Metoda:** `handleClonePrint()`

