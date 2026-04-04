# 🧪 INSTRUKCJA TESTOWANIA - Drukarka Kuchenna

## 📋 PRZYGOTOWANIE

### 1. Włącz Logi ADB
```powershell
adb logcat -c
adb logcat | Select-String "printAfterOrderAccepted|printOrder|KITCHEN|FRONT|resolveTargetConfig|PrinterSettings"
```

### 2. Skonfiguruj Drukarki w Aplikacji

#### Drukarka Frontowa (Sala)
1. Otwórz **Ustawienia** → **Drukarki**
2. Sekcja **Drukarka Frontowa**:
   - Wybierz drukarkę BT (np. `InnerPrinter`)
   - **WAŻNE:** Zapisz adres MAC drukarki frontowej
   - Profil: `YHD-8390` lub `Mobile`
   - Szablon: `STANDARD`
   - Encoding: `CP852` (lub UTF-8 dla Mobile)
   - ✅ Auto-cut (opcjonalnie)

#### Drukarka Kuchenna
1. W sekcji **Drukarka Kuchenna**:
   - ✅ **Włącz drukarkę kuchenną**
   - Wybierz **INNĄ** drukarkę BT niż frontowa (np. `POS-8390_BLE`)
   - **SPRAWDŹ LOGI:**
     ```
     D/PrinterSettings: selecting KITCHEN printer: AB:CD:EF:12:34:56
     D/PrinterSettings: KITCHEN saved: mac=AB:CD:EF:12:34:56, cfg=...
     ```
   - Profil: `YHD-8390` lub `Mobile`
   - Szablon: `COMPACT` (lub `MINIMAL`)
   - Encoding: `CP852` (lub UTF-8 dla Mobile)
   - ✅ Auto-cut (opcjonalnie)

### 3. Włącz Auto-Druk
1. Ustawienia → **Automatyczny wydruk po zaakceptowaniu**
2. Zaznacz ✅

---

## 🧪 TEST 1: Zaakceptuj Zamówienie

### Krok 1: Odbierz Zamówienie
1. Poczekaj na nowe zamówienie (PENDING)
2. Otwórz szczegóły zamówienia

### Krok 2: Zaakceptuj
1. Kliknij **ACCEPT ORDER**
2. Potwierdź

### Krok 3: Obserwuj Logi
```
D/PrinterService: printAfterOrderAccepted: order=KR-1234
D/PrinterService: printAfterOrderAccepted: kitchenEnabled=true, kitchenMac=AB:CD:EF:12:34:56
D/PrinterService: printAfterOrderAccepted: printing to KITCHEN
D/PrinterService: printOrder: target=KITCHEN, order=KR-1234
D/PrinterService: resolveTargetConfig: target=KITCHEN
D/PrinterService: resolveTargetConfig: KITCHEN enabled=true
D/PrinterService: resolveTargetConfig: KITCHEN mac=AB:CD:EF:12:34:56
D/PrinterService: resolveTargetConfig: KITCHEN cfg encoding=CP852, codepage=13
D/PrinterService: printOrder: cfg.deviceId=AB:CD:EF:12:34:56, encoding=CP852, codepage=13
D/PrinterService: printOrder: templateId=template_compact for target=KITCHEN
D/PrinterService: printOrder: autoCut=true for target=KITCHEN
D/PrinterService: printOrder: SUCCESS for target=KITCHEN
D/PrinterService: printAfterOrderAccepted: frontMac=00:11:22:33:44:55
D/PrinterService: printAfterOrderAccepted: printing to FRONT
D/PrinterService: printOrder: target=FRONT, order=KR-1234
D/PrinterService: resolveTargetConfig: target=FRONT
D/PrinterService: resolveTargetConfig: FRONT mac=00:11:22:33:44:55
D/PrinterService: resolveTargetConfig: FRONT cfg encoding=UTF-8, codepage=null
D/PrinterService: printOrder: cfg.deviceId=00:11:22:33:44:55, encoding=UTF-8, codepage=null
D/PrinterService: printOrder: templateId=template_standard for target=FRONT
D/PrinterService: printOrder: autoCut=false for target=FRONT
D/PrinterService: printOrder: SUCCESS for target=FRONT
```

**KLUCZOWE:** Sprawdź czy MACs są różne:
- `KITCHEN mac=AB:CD:EF:12:34:56` (drukarka kuchenna)
- `FRONT mac=00:11:22:33:44:55` (drukarka frontowa)

❌ **JEŚLI oba MACs są takie same** → Problem: obie drukarki używają tego samego urządzenia!

### Krok 4: Weryfikuj Wydruki

#### ✅ Drukarka KUCHENNA powinny wydrukować:
- Szablon: `COMPACT` (zwięzły)
- Polskie znaki: ✅ Poprawne
- Auto-cut: ✅ Cięcie (jeśli włączone)
- Zawartość: Produkty, adres, uwagi

#### ✅ Drukarka FRONT powinna wydrukować:
- Szablon: `STANDARD` (pełny)
- Polskie znaki: ✅ Poprawne
- Auto-cut: zgodnie z ustawieniami
- Zawartość: Pełny paragon

---

## �� TEST 2: Tylko Drukarka Frontowa

### Krok 1: Wyłącz Drukarkę Kuchenną
1. Ustawienia → Drukarka Kuchenna
2. ❌ Wyłącz

### Krok 2: Zaakceptuj Zamówienie
1. Odbierz nowe zamówienie
2. Zaakceptuj

### Krok 3: Obserwuj Logi
```
D/PrinterService: printAfterOrderAccepted: order=KR-5678
D/PrinterService: printAfterOrderAccepted: kitchenEnabled=false, kitchenMac=AB:CD:EF:12:34:56
D/PrinterService: printAfterOrderAccepted: KITCHEN skipped (enabled=false, mac=AB:CD:EF:12:34:56)
D/PrinterService: printAfterOrderAccepted: frontMac=00:11:22:33:44:55
D/PrinterService: printAfterOrderAccepted: printing to FRONT
D/PrinterService: printOrder: target=FRONT, order=KR-5678
D/PrinterService: printOrder: SUCCESS for target=FRONT
```

### Krok 4: Weryfikuj
- ❌ Drukarka KUCHENNA nie drukuje
- ✅ Drukarka FRONT drukuje

---

## 🧪 TEST 3: Różne Szablony

### Krok 1: Ustaw Różne Szablony
- Drukarka FRONT: `DETAILED`
- Drukarka KITCHEN: `MINIMAL`

### Krok 2: Zaakceptuj Zamówienie

### Krok 3: Weryfikuj Logi
```
D/PrinterService: printOrder: templateId=template_minimal for target=KITCHEN
D/PrinterService: printOrder: templateId=template_detailed for target=FRONT
```

### Krok 4: Weryfikuj Wydruki
- KITCHEN: Minimalny szablon (tylko najważniejsze)
- FRONT: Szczegółowy szablon (wszystko)

---

## 🐛 DIAGNOSTYKA PROBLEMÓW

### ⚠️ Problem: Drukarka kuchenna używa tego samego urządzenia co frontowa

#### Sprawdź Logi
```
D/PrinterService: resolveTargetConfig: KITCHEN mac=00:11:22:33:44:55
D/PrinterService: resolveTargetConfig: FRONT mac=00:11:22:33:44:55
```
❌ **Oba MACs są identyczne!**

**Przyczyna:** Drukarka kuchenna nie została poprawnie skonfigurowana - używa tego samego urządzenia co drukarka frontowa.

**Rozwiązanie:**
1. Otwórz **Ustawienia** → **Drukarki**
2. Sekcja **Drukarka Kuchenna**
3. Kliknij "Wybierz drukarkę"
4. Wybierz **INNĄ** drukarkę niż ta która jest wybrana dla FRONT
5. **Obserwuj logi:**
   ```
   D/PrinterSettings: selecting KITCHEN printer: AB:CD:EF:12:34:56
   D/PrinterSettings: KITCHEN saved: mac=AB:CD:EF:12:34:56, cfg=...
   ```
6. Sprawdź czy teraz MACs są różne

---

### Problem: Nie drukuje na kuchni

#### Sprawdź Logi
```
D/PrinterService: KITCHEN skipped (enabled=false, mac=...)
```
**Rozwiązanie:** Włącz drukarkę kuchenną w ustawieniach

#### Sprawdź Konfigurację
```
D/PrinterService: kitchenEnabled=true, kitchenMac=null
```
**Rozwiązanie:** Wybierz urządzenie BT dla drukarki kuchennej

---

### Problem: Błąd połączenia

#### Sprawdź Logi
```
W/PrinterService: printOrder: connection=null for deviceId=AB:CD:EF:12:34:56
```

**Możliwe przyczyny:**
1. Drukarka wyłączona
2. Drukarka rozparowana
3. Brak uprawnień Bluetooth
4. Drukarka jest BLE-only (nie SPP)

**Rozwiązanie:**
1. Włącz drukarkę
2. Sparuj ponownie w ustawieniach Androida
3. Sprawdź uprawnienia w aplikacji
4. Sprawdź czy drukarka ma SPP:
   ```
   D/PrinterManager: device.type=2, uuids=00001101-...
   ```
   - `type=2` = DUAL (OK)
   - `type=0` = BLE only (NIE OBSŁUGIWANE)

---

### Problem: Krzaki zamiast polskich znaków

#### Sprawdź Logi
```
D/PrinterService: printOrder: encoding=UTF-8, codepage=null
```

**Dla drukarki YHD-8390:**
- Encoding powinno być: `CP852`
- Codepage powinien być: `13`

**Rozwiązanie:**
1. Ustaw profil: `YHD-8390`
2. Lub encoding niestandardowy: `CP852`, codepage `13`

---

### Problem: Nie cina papieru

#### Sprawdź Logi
```
D/PrinterService: printOrder: autoCut=false for target=KITCHEN
```

**Rozwiązanie:**
1. Ustawienia → Drukarka Kuchenna
2. ✅ Zaznacz "Obcinaj papier po wydruku"

---

## ✅ KRYTERIA SUKCESU

| Test | Oczekiwany Rezultat | Status |
|------|---------------------|--------|
| Auto-druk włączony | Drukuje na KITCHEN i FRONT | ⬜ |
| KITCHEN wyłączona | Drukuje tylko na FRONT | ⬜ |
| Różne szablony | Każda drukarka używa swojego | ⬜ |
| Polskie znaki | Poprawne na obu drukarkach | ⬜ |
| Auto-cut KITCHEN | Cina papier jeśli włączone | ⬜ |
| Auto-cut FRONT | Cina papier jeśli włączone | ⬜ |
| Logi debugowania | Wszystkie etapy zalogowane | ⬜ |

---

## 📊 RAPORT Z TESTÓW

### Data: ___________
### Tester: ___________

#### Test 1: Obie drukarki
- [ ] ✅ KITCHEN drukuje
- [ ] ✅ FRONT drukuje
- [ ] ✅ Różne szablony
- [ ] ✅ Polskie znaki OK
- [ ] Notatki: _______________________

#### Test 2: Tylko FRONT
- [ ] ✅ KITCHEN nie drukuje
- [ ] ✅ FRONT drukuje
- [ ] Notatki: _______________________

#### Test 3: Auto-cut
- [ ] ✅ KITCHEN cina papier
- [ ] ✅ FRONT cina papier
- [ ] Notatki: _______________________

#### Problemy znalezione:
1. _______________________________
2. _______________________________
3. _______________________________

---

**Status Końcowy:** ✅ PASS / ❌ FAIL

**Uwagi:** _________________________________

