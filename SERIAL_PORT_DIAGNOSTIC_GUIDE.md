tu r# 🔍 Diagnostyka Portów Szeregowych - Instrukcja

## Co to jest?

Ekran diagnostyki portów szeregowych pozwala sprawdzić czy urządzenie ma wbudowaną drukarkę (np. Sunmi H10).

## Jak z tego korzystać?

### 1. Przejdź do Diagnostyki

- Otwórz aplikację ItsOrderChat
- Przejdź do **Ustawienia** → **Drukarki**
- Kliknij ikonę debugowania 🐛 w prawym górnym rogu

### 2. Skanuj Porty

- Kliknij przycisk **"Skanuj porty"**
- Czekaj na wyniki (ok. 1-2 sekundy)

### 3. Odczytaj Wyniki

Ekran wyświetli:

#### Informacje o urządzeniu
```
Producent: Samsung
Model: SM-X700
Urządzenie: x1s
Sunmi: ❌ NIE
```

#### Wyniki diagnostyki

Jeśli urządzenie NIE ma wbudowanej drukarki:
```
❌ Brak dostępnych portów szeregowych
   To urządzenie prawdopodobnie NIE ma wbudowanej drukarki.
```

Jeśli urządzenie MA wbudowaną drukarkę:
```
✅ Znaleziono 1 portów:

📍 /dev/ttyS1
   Uprawnienia: crw-------
   Odczyt: ✅
   Zapis: ✅
   Typ: Character Device

🎯 Zalecany port: /dev/ttyS1
```

## Interpretacja Uprawień

| Uprawnienia | Znaczenie |
|-------------|-----------|
| crw------- | Tylko właściciel może czytać/pisać |
| crw-rw---- | Grupa może czytać/pisać |
| crw-rw-rw- | Wszyscy mogą czytać/pisać |

## Jeśli porty Istnieją Ale Nie Działają

Jeśli widzisz porty ale zapis = ❌:

1. **Aplikacja wymaga uprawnień root**
   - Urządzenie musi być zrootowane
   - Lub firmware musi mieć specjalne SELinux rules

2. **Próba przy użyciu biblioteki**
   - Dodaj do `build.gradle`:
   ```gradle
   implementation 'com.github.licheedev:Android-SerialPort-API:2.1.3'
   ```
   - Dodaj do `settings.gradle`:
   ```gradle
   maven { url 'https://jitpack.io' }
   ```

## Popularne Urządzenia z Wbudowaną Drukarką

| Urządzenie | Port | Producent |
|-----------|------|-----------|
| Sunmi H10 | /dev/ttyS1 | Sunmi |
| Sunmi T2 | /dev/ttyS1 | Sunmi |
| Urovo A70 | /dev/ttyS0 | Urovo |
| Easypos | /dev/ttyMT1 | Wincor Nixdorf |

## Przyczyny Braku Portu

- ✅ Standardowy Android na telefonie/tablecie = brak wbudowanej drukarki
- ❌ Nie posiadasz urządzenia POS (Sunmi, Urovo, itp.)
- ❌ ROM nie wspiera dostępu do portów szeregowych
- ❌ Drukarka jest wyłączona w BIOS/firmware

## Czym to NE jest

To nie jest narzędzie do:
- Debugowania Bluetooth
- Testowania połączeń sieciowych
- Sprawdzania innych portów USB

## Następne Kroki

### Jeśli urządzenie MA drukarkę
1. Zainstaluj bibliotekę `android-serialport-api`
2. W ustawieniach drukarek utwórz nową drukarkę typu "Wbudowana"
3. Wskaż port z raportu diagnostyki
4. Przetestuj wydruk

### Jeśli urządzenie NIE MA drukarki
1. Użyj drukarek Bluetooth
2. Lub drukarek sieciowych
3. Lub połącz drukarkę USB (jeśli urządzenie to wspiera)

