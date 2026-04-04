# 🎯 ETAP 5: TESTY I WALIDACJA - INSTRUKCJE

## ✅ Status Bieżący

**Data:** 2026-01-22  
**Ukończone Etapy:** ETAP 1-4 ✅  
**Obecny Etap:** ETAP 5 - Testy i Walidacja  

---

## 📋 Checklist Testów

### 1️⃣ Build i Kompilacja

- [ ] Uruchom build w IDE:
  ```bash
  ./gradlew clean assembleDebug
  ```

- [ ] Sprawdź brak błędów kompilacji (tylko warningi są OK)

- [ ] Uruchom Lint:
  ```bash
  ./gradlew lint
  ```

---

### 2️⃣ Unit Testy (Jeśli czasowo)

#### PrintersViewModel Tests
```kotlin
// test/PrintersViewModelTest.kt
@get:Rule
val instantExecutorRule = InstantTaskExecutorRule()

@Test
fun testLoadPrinters() {
    // Arrange
    val printers = listOf(
        Printer(name = "Drukarka1", deviceId = "AA:BB:CC:DD:EE:FF", profileId = "profile_pos_8390_dual", templateId = "template_standard", order = 1, enabled = true),
        Printer(name = "Drukarka2", deviceId = "11:22:33:44:55:66", profileId = "profile_mobile_ssp", templateId = "template_compact", order = 2, enabled = true)
    )
    
    // Act
    viewModel.loadPrinters()
    
    // Assert
    assert(viewModel.printers.value == printers)
}

@Test
fun testAddPrinter() {
    val newPrinter = Printer(name = "Nowa", deviceId = "XX:YY:ZZ", profileId = "profile_custom", templateId = "template_standard", order = 1, enabled = true)
    viewModel.addPrinter(newPrinter)
    assert(viewModel.printers.value.contains(newPrinter))
}
```

---

### 3️⃣ Testy Manualne (Najważniejsze)

#### Scenariusz A: Dodawanie Drukarki

**Kroki:**
1. Otwórz aplikację
2. Idź do Settings → Zarządzaj drukarkami
3. Kliknij FAB (+)
4. Wypełnij formularz:
   - Nazwa: "Drukarka Testowa"
   - Urządzenie: Wybierz istniejące urządzenie BT
   - Profil: "POS-8390 (DUAL)"
   - Szablon: "Standardowy"
   - Autoczęcie: ✓ ON
5. Kliknij "Zapisz"

**Oczekiwany wynik:**
- ✅ Drukarka pojawia się na liście
- ✅ Toast: "Drukarka dodana"
- ✅ Dane zapisane w SharedPreferences

---

#### Scenariusz B: Edycja Drukarki

**Kroki:**
1. Z listy drukarek kliknij istniejącą
2. Zmień profil na "Mobile (SSP)"
3. Zmień szablon na "Kompaktowy"
4. Kliknij "Zapisz"

**Oczekiwany wynik:**
- ✅ Profil zmieniony w liście
- ✅ Encoding zmieniony (UTF-8)
- ✅ Toast: "Drukarka zaktualizowana"

---

#### Scenariusz C: Drukowanie na Jednej Drukarce

**Kroki:**
1. Przejdź do ekranu zamówień
2. Zaakceptuj zamówienie (przycisk)
3. Obserwuj logi w Logcat

**Oczekiwany wynik:**
```
✅ PrinterSTEP: [SUCCESS] Drukowanie zakończone pomyślnie na STANDARD
```

---

#### Scenariusz D: Drukowanie na Dwóch Drukarkach (KRYTYCZNE)

**Kroki:**
1. Konfiguruj 2 drukarki w ustawieniach
2. Zaakceptuj zamówienie
3. Obserwuj logi Logcat

**Oczekiwany wynik:**
```
🧾 PrinterSTEP: Uruchamiam drukowanie na STANDARD (MAC1)
✅ PrinterSTEP: [SUCCESS] Drukowanie zakończone pomyślnie na STANDARD

⏳ PrinterSTEP: Czekam 3000ms przed przełączeniem na KITCHEN (BT stack cleanup)...

🍳 PrinterSTEP: Uruchamiam drukowanie na KITCHEN (MAC2)
✅ PrinterSTEP: [SUCCESS] Drukowanie zakończone pomyślnie na KITCHEN
```

**Czas:** Około 8-10 sekund (timeout entre drukárkami)

---

#### Scenariusz E: Test Druku (Print Test Page)

**Kroki:**
1. Settings → Zarządzaj drukarkami
2. Kliknij przycisk "Test" na drukarce
3. Sprawdź fizycznie wydruk na drukarce
4. Obserwuj Toast

**Oczekiwany wynik:**
- ✅ "Wydruk testowy wysłany (STANDARD)"
- ✅ Papier wydrukuje się

---

### 4️⃣ Regression Testy (Stary System)

#### Test: printAfterOrderAccepted (Stare API)

**Kroki:**
1. Nie dodawaj nowych drukarek
2. Skonfiguruj Settings → Printer Settings (stary sposób)
3. Zaakceptuj zamówienie
4. Sprawdź wydruk

**Oczekiwany wynik:**
- ✅ Druk działa na STANDARD i KITCHEN (jeśli skonfigurowane)
- ✅ Stary kod bez zmian

---

### 5️⃣ Logcat Analysis

**Szukaj w logach:**

```
// Poprawne logowanie:
✅ printOrderOnAllEnabledPrinters: START
✅ Znaleziono X włączonych drukarek
✅ PrinterSTEP: [SUCCESS]
✅ Drukowanie zakończone na X drukarkach

// Błędy do unikania:
❌ BT connect failed after retry
❌ Unable to connect to bluetooth device
❌ Unresolved reference (ERROR)
```

**Filtry Logcat:**
```bash
# Tylko drukowanie
adb logfilter "PrinterService|PrinterSTEP|Druk"

# Bluetooth
adb logfilter "BluetoothConnection|BT"

# Błędy
adb logfilter "ERROR|FATAL"
```

---

## 🐛 Znane Problemy i Rozwiązania

### Problem 1: "BT connect failed after retry"
**Przyczyna:** Timeout między drukárkami za krótki (szczególnie DUAL)
**Rozwiązanie:** Zwiększ delay z 2000ms do 3000ms

```kotlin
// Zmień w printOrderOnAllEnabledPrinters()
delay(3000)  // było 2000
```

### Problem 2: "Drukarka pojawia się 2 razy w liście"
**Przyczyna:** Duplikaty w SharedPreferences
**Rozwiązanie:** Wyczyść dane applikacji i dodaj ponownie

```bash
adb shell pm clear com.itsorderchat
```

### Problem 3: Dialog nie zamyka się po zapisaniu
**Przyczyna:** ViewModel nie emituje wyniku
**Rozwiązanie:** Sprawdź czy `onSave` lambda jest wołana poprawnie

---

## 📊 Metryki Sukcesu

| Metryka | Oczekiwanie | Rzeczywistość |
|---------|-------------|----------------|
| Build SUCCESS | ✅ | ___ |
| Brak błędów kompilacji | ✅ | ___ |
| Druk na 1 drukarce | ✅ | ___ |
| Druk na 2 drukarkach | ✅ | ___ |
| Timeout BT cleanup | 3000ms | ___ |
| Backward compatibility | ✅ | ___ |
| UI responsywny | ✅ | ___ |

---

## 🚀 Instrukcja Wdrażania

### Jeśli Wszystkie Testy OK ✅

1. **Merge do main/production branch**
   ```bash
   git add .
   git commit -m "feat: new printer management system (ETAP 1-4)"
   git push origin main
   ```

2. **Utwórz Release Notes**
   - Nowy ekran: Settings → Zarządzaj drukarkami
   - Nowa funkcja: Wielodrukarkowe drukowanie
   - Backward compatibility: Stary system nadal działa

3. **Deploy na Production**
   ```bash
   ./gradlew bundleRelease  # Dla Google Play
   ```

### Jeśli Znalezione Błędy ❌

1. **Udokumentuj błąd:**
   ```
   - Co się stało (krok po kroku)
   - Logi z Logcat
   - Liczba drukarek zaangażowanych
   ```

2. **Utwórz issue:**
   ```
   Title: [BUG] Drukowanie na drukarce X failuje
   Logs: [paste logcat]
   ```

3. **Napraw i ponownie testuj**

---

## 📞 Kontakt i Wsparcie

- **Dokumentacja:** `PRINTER_SYSTEM_IMPLEMENTATION.md`
- **Kod:** Sprawdzaj klasy w katalogach:
  - `ui/settings/printer/`
  - `data/preferences/`
  - `data/model/`

---

**Status:** 🟡 ETAP 5 W TRAKCIE  
**Data:** 2026-01-22  
**Wersja:** 1.0-RC1  
**Następny:** ETAP 6 (Production Release)

