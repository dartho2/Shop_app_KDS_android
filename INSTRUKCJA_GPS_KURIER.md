# 📍 Instrukcja: Jak włączyć GPS dla kuriera

## Problem
Gdy kurier próbuje rozpocząć zmianę lub zaktualizować status zamówienia, pojawia się komunikat:
```
"Nie można pobrać lokalizacji. Sprawdź ustawienia GPS."
```

## ✅ Rozwiązanie (ZAKTUALIZOWANE - Aplikacja pyta automatycznie!)

### Automatyczne pytanie o uprawnienie
**OD TERAZ**: Gdy kurier pierwszy raz kliknie "Rozpocznij zmianę" lub spróbuje zaktualizować status zamówienia, **aplikacja automatycznie pokaże dialog systemowy** z prośbą o przyznanie uprawnień lokalizacji.

1. **Kliknij "Rozpocznij zmianę"** lub zmień status zamówienia
2. System Android wyświetli dialog: **"Zezwolić aplikacji ItsOrderChat na dostęp do lokalizacji urządzenia?"**
3. Wybierz jedną z opcji:
   - ✅ **"Zawsze zezwalaj"** (zalecane dla kurierów) - GPS działa w tle
   - ✅ **"Tylko podczas korzystania z aplikacji"** - GPS działa tylko gdy aplikacja jest otwarta
   - ❌ **"Nie zezwalaj"** - aplikacja nie będzie działać poprawnie
4. Po wybraniu "Zawsze zezwalaj" lub "Tylko podczas korzystania", **spróbuj ponownie** kliknąć "Rozpocznij zmianę"

### Alternatywna metoda (przez Ustawienia aplikacji)

Jeśli przypadkowo odrzuciłeś uprawnienie lub chcesz je zmienić:

1. W aplikacji **ItsOrderChat** przejdź do ekranu **Ustawień** (ikona zębatki ⚙️)
2. Przewiń na dół do sekcji **"Uprawnienia"**
3. Znajdź pozycję **"Lokalizacja"** (z opisem: "Wymagana do funkcji dostawy i map")
4. Jeśli obok jest **czerwona ikona** lub status "Wyłączone", **kliknij** na tę pozycję
5. System przeniesie Cię do ekranu z ustawieniami uprawnień
6. Wybierz **"Zawsze zezwalaj"** lub **"Zezwalaj tylko podczas korzystania z aplikacji"**

### Krok 3: Sprawdź ustawienia GPS w telefonie/tablecie
Dodatkowo upewnij się, że GPS jest włączony globalnie w urządzeniu:
1. Przejdź do **Ustawień systemowych** telefonu/tabletu
2. Znajdź **"Lokalizacja"** lub **"GPS"**
3. Upewnij się, że jest **WŁĄCZONA**
4. Sprawdź, czy dokładność jest ustawiona na **"Wysoka"** (High accuracy)

### Krok 4: Zrestartuj aplikację (opcjonalnie)
Po przyznaniu uprawnień możesz:
1. Po prostu **spróbować ponownie** - aplikacja powinna już działać
2. Lub zamknij całkowicie aplikację i uruchom ponownie

---

## Techniczne informacje (dla deweloperów)

### ✨ CO NOWEGO (v2.0)
- ✅ **Automatyczne pytanie o uprawnienia** - aplikacja sama pyta o GPS gdy funkcja tego wymaga
- ✅ **Dodano `ACCESS_BACKGROUND_LOCATION`** - teraz dostępna opcja "Zawsze zezwalaj" w dialogu systemowym
- ✅ **Event `RequestLocationPermission`** - dedykowany event w OrdersViewModel
- ✅ **Launcher w HomeActivity** - `rememberLauncherForActivityResult` obsługuje dialog uprawnień
- ✅ **Lepsze komunikaty** - po przyznaniu/odrzuceniu uprawnień użytkownik widzi Snackbar

### Implementacja uprawnień
- **AndroidManifest.xml**: 
  - Linie 18-19: `ACCESS_FINE_LOCATION` i `ACCESS_COARSE_LOCATION` (podstawowe)
  - Linia 21: `ACCESS_BACKGROUND_LOCATION` (nowe! dla "Zawsze zezwalaj")
- **SettingsScreen.kt**: Linie 550-649 - UI dla zarządzania uprawnieniami lokalizacji
- **LocationProvider.kt**: Obsługuje pobieranie aktualnej lokalizacji z Google Play Services
  - Rzuca `LocationException` gdy brakuje uprawnień
- **OrdersViewModel.kt**: 
  - Linia 117: Nowy event `OrderEvent.RequestLocationPermission`
  - Linia 960-963: `checkIn()` - rzuca event gdy brakuje uprawnień (zamiast błędu)
  - Linia 1025-1028: `updateOrdersStatusWithLocation()` - rzuca event gdy brakuje uprawnień
- **HomeActivity.kt**:
  - Linia 435-448: Launcher `locationPermissionLauncher` do pytania o uprawnienia
  - Linia 461-471: Obsługa eventu `RequestLocationPermission` w LaunchedEffect

### Kiedy wymagana jest lokalizacja?
Lokalizacja GPS jest niezbędna dla kurierów w następujących sytuacjach:
1. **Check-in** (rozpoczęcie zmiany) - zapisuje pozycję startu
2. **Aktualizacja statusu zamówień** (np. "odebrano", "w drodze", "dostarczone") - śledzi trasę kuriera
3. **Check-out** (zakończenie zmiany) - zapisuje pozycję końcową

### Flow obsługi uprawnień (NOWY)
```
1. Kurier klika "Rozpocznij zmianę" lub zmienia status zamówienia
2. OrdersViewModel wywołuje locationProvider.getCurrentLocation()
3. LocationProvider sprawdza uprawnienia (ACCESS_FINE_LOCATION)
4. Jeśli BRAK uprawnień:
   ├─ LocationProvider rzuca LocationException
   ├─ OrdersViewModel łapie wyjątek w catch { }
   ├─ Emituje event: OrderEvent.RequestLocationPermission
   ├─ HomeActivity odbiera event w LaunchedEffect
   ├─ Wywołuje locationPermissionLauncher.launch()
   └─ System Android pokazuje dialog z pytaniem o uprawnienia
5. Po przyznaniu uprawnień:
   ├─ Pokazuje Snackbar: "Uprawnienia lokalizacji przyznane. Spróbuj ponownie."
   └─ Kurier klika ponownie "Rozpocznij zmianę" → teraz działa!
6. Po odrzuceniu uprawnień:
   └─ Pokazuje Snackbar: "Brak uprawnień do lokalizacji. Włącz w ustawieniach aplikacji."
```

### Testowanie
```kotlin
// W LocationProvider.kt można dodać logi do debugowania:
Timber.d("🧪 Checking location permission: $hasPermission")
Timber.d("📍 Location retrieved: lat=${location?.latitude}, lng=${location?.longitude}")
```

---

## FAQ

**Q: Dlaczego muszę włączyć GPS?**  
A: Aplikacja wymaga lokalizacji GPS do śledzenia trasy kuriera, co jest niezbędne dla funkcji dostawy.

**Q: Czy GPS będzie działać cały czas?**  
A: GPS jest pobierany tylko wtedy, gdy kurier rozpoczyna/kończy zmianę lub aktualizuje status zamówienia. Nawet z opcją "Zawsze zezwalaj", aplikacja nie śledzi lokalizacji ciągle w tle - tylko gdy jest to potrzebne.

**Q: Co jeśli tablet/telefon nie ma GPS?**  
A: Wszystkie nowoczesne telefony i tablety mają GPS. Upewnij się, że funkcja lokalizacji nie jest wyłączona w ustawieniach systemowych.

**Q: Aplikacja nadal pokazuje błąd pomimo włączonego GPS**  
A: 
1. Upewnij się, że przyznałeś uprawnienie w dialogu systemowym (kliknij "Zawsze zezwalaj" lub "Podczas korzystania")
2. Sprawdź czy GPS w tablecie jest włączony (nie tylko WiFi/sieć komórkowa)
3. Wyjdź na zewnątrz - w budynkach GPS może nie działać dobrze
4. Zrestartuj tablet
5. Zreinstaluj aplikację (ostateczność)

**Q: Co oznacza "Zawsze zezwalaj" vs "Tylko podczas korzystania"?**  
A: 
- **"Zawsze zezwalaj"** - aplikacja może pobrać lokalizację nawet gdy jest w tle (zalecane dla kurierów)
- **"Tylko podczas korzystania"** - aplikacja może pobrać lokalizację tylko gdy jest otwarta na pierwszym planie

**Q: Nie pojawił mi się dialog z pytaniem o uprawnienia**  
A: 
- Sprawdź czy przypadkowo nie odrzuciłeś go wcześniej
- Przejdź do Ustawień → Aplikacje → ItsOrderChat → Uprawnienia → Lokalizacja i ustaw ręcznie
- Lub odinstaluj i zainstaluj aplikację ponownie

---

**Data utworzenia:** 2026-04-04  
**Wersja:** 2.0 (z automatycznym pytaniem o uprawnienia)


