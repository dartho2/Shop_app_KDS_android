# 🔍 DIAGNOSTYKA PROBLEMU DRUKOWANIA - INSTRUKCJA

## Data: 2026-02-13

---

## 📋 ZGŁOSZONY PROBLEM

**Opis użytkownika:**
- Ustawienie: **Drukowanie po zaakceptowaniu** → `Kuchnia`
- Ustawienie: **Auto-drukuj zamówienia na miejscu (DINE_IN)** → `Kuchnia`
- **Problem:** Zamiast drukować na kuchni, wydrukował się **2x na głównej drukarce**

---

## 🔧 CO ZOSTAŁO ZROBIONE

### 1. **Dodano rozszerzone logi diagnostyczne**

Do kodu dodano szczegółowe logi z tagiem `PRINT_DEBUG`, które pokażą dokładnie:
- Które ustawienie jest aktywne (`"main"`, `"kitchen"`, `"both"`)
- Ile drukarek zostało znalezionych
- Na której drukarce faktycznie drukuje
- Pełny przepływ drukowania

### 2. **Przebudowano aplikację**

Nowa wersja APK zawiera rozszerzone logi diagnostyczne.

---

## 📱 INSTRUKCJA TESTOWANIA

### Krok 1: Zainstaluj nową wersję aplikacji

```powershell
adb install -r "L:\SHOP APP\app\build\outputs\apk\debug\app-debug.apk"
```

### Krok 2: Uruchom monitorowanie logów

Otwórz **dwa okna PowerShell** i uruchom:

**Okno 1 - Logi drukowania po zaakceptowaniu:**
```powershell
adb logcat -s PRINT_DEBUG:* | Select-String "printAfterOrderAccepted"
```

**Okno 2 - Wszystkie logi drukowania:**
```powershell
adb logcat -s PRINT_DEBUG:*
```

### Krok 3: Sprawdź ustawienia w aplikacji

1. Otwórz **Ustawienia** → **Drukowanie**
2. Sprawdź sekcję **"Drukowanie po zaakceptowaniu"**:
   - [x] Włącz automatyczne drukowanie
   - Drukuj na: **Kuchnia** (zaznacz chip "Kuchnia")
3. Sprawdź sekcję **"Auto-drukuj zamówienia na miejscu"**:
   - [x] Włącz dla DINE_IN/ROOM_SERVICE
   - Drukuj na: **Kuchnia** (zaznacz chip "Kuchnia")

### Krok 4: Testuj drukowanie

#### Test A: Zaakceptowanie zamówienia zwykłego
1. Utwórz nowe zamówienie (DELIVERY lub PICKUP)
2. Zaakceptuj zamówienie
3. **Obserwuj logi** - powinny pokazać:
   ```
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   📋 printAfterOrderAccepted START
      ├─ order: #12345
      ├─ enabled: true
      ├─ printers: kitchen    ← WAŻNE! Sprawdź tę wartość
      └─ Thread: ...
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   🍳 WYBRANO: Drukowanie tylko na drukarce kuchennej
      → Znaleziono drukarek kuchennych: 1
      → 🍳 Drukuję paragon kuchenny: Drukarka Kuchnia
   ✅ Drukowanie standardowe zakończone
   ```

#### Test B: Auto-druk DINE_IN
1. Utwórz nowe zamówienie typu **DINE_IN**
2. **Obserwuj logi** - powinny pokazać:
   ```
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   🔔 AUTO-DRUK DINE_IN
      ├─ order: #12345
      ├─ deliveryType: DINE_IN
      └─ printers: kitchen    ← WAŻNE! Sprawdź tę wartość
   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   🍳 WYBRANO: Tylko kuchnia
   ✅ Wydrukowano na drukarce kuchennej
   ```

---

## 🐛 MOŻLIWE PRZYCZYNY PROBLEMU

### Przyczyna 1: **Wartość "both" zamiast "kitchen"**

**Objaw w logach:**
```
printers: both    ← Zamiast "kitchen"!
📄🍳 WYBRANO: Obie drukarki
```

**Rozwiązanie:**
1. Sprawdź czy w ustawieniach jest zaznaczony chip **"Kuchnia"** (powinien być niebieski/aktywny)
2. Jeśli nie - kliknij ponownie chip "Kuchnia"
3. Zrestartuj aplikację
4. Przetestuj ponownie

---

### Przyczyna 2: **Brak drukarki kuchennej**

**Objaw w logach:**
```
🍳 WYBRANO: Drukowanie tylko na drukarce kuchennej
   → Znaleziono drukarek kuchennych: 0    ← PROBLEM!
⚠️ Brak skonfigurowanej drukarki kuchennej
```

**Rozwiązanie:**
1. Idź do **Ustawienia** → **Drukarki**
2. Sprawdź czy masz skonfigurowaną drukarkę typu **KITCHEN**
3. Jeśli nie - dodaj drukarkę i ustaw typ na "Kuchnia"
4. Upewnij się że drukarka jest **włączona** (enabled)

---

### Przyczyna 3: **Drukarka kuchenna ustawiona jako STANDARD**

**Objaw w logach:**
```
🖨️ WYBRANO: Drukowanie tylko na drukarce głównej    ← Dlaczego główna?!
   → Znaleziono drukarek głównych: 2    ← Za dużo!
   → 🖨️ Drukuję na standardowej: Drukarka Bar
   → 🖨️ Drukuję na standardowej: Drukarka Kuchnia    ← To powinna być KITCHEN, nie STANDARD!
```

**Rozwiązanie:**
1. Idź do **Ustawienia** → **Drukarki**
2. Znajdź drukarkę która powinna być kuchenna
3. Edytuj ją i zmień typ z **"Standard"** na **"Kuchnia"**
4. Zapisz i przetestuj ponownie

---

### Przyczyna 4: **Dwa wywołania drukowania**

**Objaw w logach:**
```
📋 printAfterOrderAccepted START    ← Pierwsze wywołanie
   printers: kitchen
🍳 WYBRANO: Tylko kuchnia
✅ Wydrukowano na drukarce kuchennej

📋 printAfterOrderAccepted START    ← Drugie wywołanie!?
   printers: main
🖨️ WYBRANO: Tylko główna
✅ Wydrukowano na drukarce głównej
```

**Przyczyna:** Podwójne wywołanie funkcji (bug w kodzie lub duplikacja eventów)

**Rozwiązanie:** Prześlij pełne logi do developera

---

## 📊 SZABLON RAPORTU BŁĘDU

Jeśli problem nadal występuje, wyślij raport z następującymi informacjami:

### 1. Ustawienia aplikacji
```
Drukowanie po zaakceptowaniu:
- Włączone: [TAK/NIE]
- Wybrana opcja: [Główna / Kuchnia / Obie]

Auto-drukuj zamówienia na miejscu:
- Włączone: [TAK/NIE]
- Wybrana opcja: [Główna / Kuchnia / Obie]
```

### 2. Konfiguracja drukarek
```
Drukarka #1:
- Nazwa: ...
- Typ: [STANDARD / KITCHEN]
- Włączona: [TAK/NIE]

Drukarka #2:
- Nazwa: ...
- Typ: [STANDARD / KITCHEN]
- Włączona: [TAK/NIE]
```

### 3. Logi z tagiem PRINT_DEBUG

Skopiuj **pełne logi** z momentu problemu:
```powershell
adb logcat -s PRINT_DEBUG:* -d > logi_drukowania.txt
```

Wyślij plik `logi_drukowania.txt`

---

## ✅ OCZEKIWANE LOGI DLA POPRAWNEGO DZIAŁANIA

### Drukowanie po zaakceptowaniu (wybrano "Kuchnia"):

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 printAfterOrderAccepted START
   ├─ order: #12345
   ├─ enabled: true
   ├─ printers: kitchen    ✅ Poprawna wartość
   └─ Thread: DefaultDispatcher-worker-1
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🍳 WYBRANO: Drukowanie tylko na drukarce kuchennej
   → Znaleziono drukarek kuchennych: 1    ✅ Jest drukarka
   → 🍳 Drukuję paragon kuchenny: Drukarka Kuchnia
✅ Drukowanie standardowe zakończone
```

### Auto-druk DINE_IN (wybrano "Kuchnia"):

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔔 AUTO-DRUK DINE_IN
   ├─ order: #12345
   ├─ deliveryType: DINE_IN
   └─ printers: kitchen    ✅ Poprawna wartość
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🍳 WYBRANO: Tylko kuchnia
✅ Wydrukowano na drukarce kuchennej
✅ Auto-druk DINE_IN/ROOM_SERVICE zakończony dla #12345
```

---

## 🎯 NASTĘPNE KROKI

1. ✅ Zainstaluj nową wersję APK
2. ✅ Uruchom monitorowanie logów
3. ✅ Sprawdź ustawienia w aplikacji
4. ✅ Przetestuj oba scenariusze
5. ✅ Sprawdź logi - porównaj z oczekiwanymi
6. ❓ Jeśli problem nadal występuje - wyślij raport z logami

---

## 📞 KONTAKT

Jeśli problem nadal występuje po wykonaniu powyższych kroków, skontaktuj się z developerem i dołącz:
- Plik `logi_drukowania.txt`
- Screenshot ustawień drukowania
- Screenshot listy drukarek

---

**Autor:** GitHub Copilot  
**Data:** 2026-02-13  
**Wersja:** 1.0 (z rozszerzonymi logami diagnostycznymi)

