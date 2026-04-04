# 📋 Instrukcja testowania drukarki H10 (KLON/Senraise)

## ✅ Zaimplementowane zmiany

### 1. Pliki AIDL
Utworzono poprawne definicje AIDL zgodne ze zdekompilowanym interfejsem:
- `app/src/main/aidl/recieptservice/com/recieptservice/PrinterInterface.aidl`
- `app/src/main/aidl/recieptservice/com/recieptservice/PSAMCallback.aidl`

### 2. Poprawiony flow drukowania
`AidlPrinterService` teraz używa **beginWork/endWork**:
```kotlin
beginWork()       // rozpocznij sesję
printText(...)    // drukuj tekst
nextLine(5)       // przesuń papier
endWork()         // zakończ sesję
```

### 3. Nowe metody testowe
- **`feedOnlyTest(lines)`** - Test przesuwu papieru (bez tekstu)
- **`printEpsonHello()`** - Test RAW ESC/POS (HELLO przez printEpson)
- **`handleClonePrint()`** - Poprawiony flow z beginWork/endWork

---

## 🧪 Jak testować?

### Krok 1: Wejdź w ekran "Drukarki"
W aplikacji: **Ustawienia → Drukarki**

### Krok 2: Użyj przycisków testowych w TopBar

#### 🧻 Test FEED ONLY (ikona Article)
**Co robi:** Wysyła tylko `nextLine(5)` w sesji beginWork/endWork  
**Oczekiwany wynik:** Drukarka powinna **przesunąć papier** (ok. 5 linii)  
**Jeśli NIE przesunie papieru:**
- Problem NIE jest w kodzie drukowania
- Sprawdź: drukarka włączona? Papier załadowany? Klapka zamknięta?
- Sprawdź logi (tag: `AidlPrinterService`)

#### 🖨️ Test ESC/POS RAW (ikona Code)
**Co robi:** Wysyła RAW bajty ESC/POS: `ESC @ + "HELLO\n" + feed`  
**Oczekiwany wynik:** Wydruk napisu **"HELLO"** + przesuw papieru  
**Jeśli drukuje krzaki/nic:**
- Drukarka nie rozumie ESC/POS w obecnym kodowaniu
- Zmień w kodzie `setCode("UTF-8")` na `setCode("GBK")` w `handleClonePrint()`

#### 📢 Test HELLO (ikona Campaign)
**Co robi:** Drukuje "HELLO 123" przez `handleClonePrint()` (pełny flow z beginWork)  
**Oczekiwany wynik:** Wydruk napisu "HELLO 123"

---

## 📊 Diagnostyka (gdy nic nie drukuje)

### 🔍 Przycisk "Security" (ikona tarcza)
Uruchamia **pełną diagnostykę systemową**:
- Wypisuje wszystkie serwisy w pakiecie `recieptservice.com.recieptservice`
- Sprawdza wymagane uprawnienia
- Wynik w **logach** (tag: `AidlPrinterService`, poziom ERROR)

**Szukaj w logach:**
```
📦 service=recieptservice.com.recieptservice.service.PrinterService
   exported=true
   permission=BRAK (public)
```

**Jeśli `exported=false` lub `permission=signature`:**
→ Twoja apka jest **ZABLOKOWANA** przez system (tylko systemowe apki mogą drukować)

---

## ⚠️ Znane problemy i rozwiązania

### Problem: "MCU power off 2" w logach SRPrinter
**Przyczyna:** Drukarka fizycznie nie jest gotowa (brak papieru, klapka otwarta, sprzęt wyłączony)  
**Rozwiązanie:** Sprawdź fizyczny stan drukarki + uruchom self-test z aplikacji SRPrinter

### Problem: beginWork/printText/endWork wywołują się, ale nic nie drukuje
**Przyczyna 1:** Biblioteka wysyła komendy do **innego serwisu** (nie do prawdziwego sterownika)  
**Rozwiązanie:** Sprawdź w logach którą usługę połączyliśmy:
```
✅ Typ: KLON (recieptservice)
```

**Przyczyna 2:** Serwis wymaga **uprawnień signature** (tylko systemowe apki)  
**Rozwiązanie:** Zobacz diagnostykę (przycisk 🔍) i sprawdź `permission=...`

### Problem: Drukarka przesunie papier przy FEED TEST, ale nie drukuje tekstu
**Przyczyna:** Kodowanie tekstu jest złe (drukarka oczekuje GBK, a dostaje UTF-8)  
**Rozwiązanie:** W `AidlPrinterService.handleClonePrint()` zmień:
```kotlin
s.setCode("GBK")  // zamiast "UTF-8"
```

---

## 📖 Następne kroki (jeśli testy nie działają)

1. **Sprawdź czy systemowa apka SRPrinter potrafi drukować**
   - Uruchom aplikację `Printer` (lub `Settings` na urządzeniu H10)
   - Spróbuj wydrukować self-test / demo page
   - Jeśli systemowa apka NIE drukuje → **problem sprzętowy** (nie kod)

2. **Sprawdź uprawnienia (diagnostyka 🔍)**
   - Jeśli serwis wymaga `signature` → potrzebujesz:
     - Podpisać apkę kluczem platform **LUB**
     - Zainstalować apkę jako `/system/priv-app/` **LUB**
     - Użyć helperowej apki systemowej (AIDL wrapper)

3. **Alternatywne podejście: PrintManager**
   - Przycisk 🖨️ (Print) uruchamia systemowy dialog wydruku
   - Jeśli to działa → drukuj przez PrintManager zamiast AIDL

---

## 🔧 Szybkie poprawki w kodzie

### Zmiana kodowania na GBK
**Plik:** `AidlPrinterService.kt`, metoda `handleClonePrint()`  
**Linia:** `s.setCode("UTF-8")`  
**Zmień na:** `s.setCode("GBK")`

### Zwiększenie timeoutów (jeśli drukarka jest wolna)
**Plik:** `AidlPrinterService.kt`, metoda `handleClonePrint()`  
**Linia:** `Thread.sleep(1500)`  
**Zmień na:** `Thread.sleep(3000)` (3 sekundy)

### Testowanie bez rozłączania (debug)
**Plik:** `AidlPrinterService.kt`, metoda `printText()`  
**Zakomentuj:** `disconnect()` na końcu testów  
**Dlaczego:** Serwis PrinterSender może ginąć za szybko przy disconnect

---

## 📞 Support

Jeśli po wszystkich testach:
- ✅ FEED TEST działa (papier się przesuwa)
- ❌ Ale drukowanie tekstu NIE działa

**To oznacza:** Problem nie jest w kodzie beginWork/endWork, tylko w:
- Kodowaniu tekstu (GBK vs UTF-8)
- Uprawnieniach systemowych (diagnostyka 🔍)
- Sposobie wysyłania danych (printText vs printEpson)

**Kolejny krok:** Przetestuj `printEpsonHello()` (ESC/POS RAW) - jeśli to zadziała, to problem jest w `printText()` i trzeba używać tylko `printEpson()`.

---

**Autor:** GitHub Copilot  
**Data:** 2026-01-23  
**Terminal:** H10/Senraise (KLON)

