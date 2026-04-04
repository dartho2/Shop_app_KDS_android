# Czyszczenie Aplikacji KDS - Usunięcie Funkcji Zarządzania Restauracją

## Data: 2026-04-04

---

## ✅ USUNIĘTE FUNKCJE (Niepotrzebne w KDS)

### 1. Status Restauracji (Otwarte/Zamknięte/Wstrzymane)
- ✅ **Usunięto:** `RestaurantStatusActionItem` - komponent w TopAppBar
- ✅ **Usunięto:** `RestaurantStatusChip` - wyświetlanie statusu
- ✅ **Usunięto:** `RestaurantStatusSheet` - bottom sheet do zarządzania statusem
- ✅ **Usunięto:** `PauseDialog` - dialog wstrzymania zamówień
- ✅ **Usunięto:** Wywołania `ordersViewModel.setClosed()`, `setPause()`, `clearPause()`

**Uzasadnienie:** KDS (Kitchen Display System) nie zarządza statusem restauracji - kuchnia po prostu odbiera i przygotowuje tickety/zamówienia niezależnie od tego czy restauracja jest otwarta czy zamknięta. Zarządzanie statusem restauracji to funkcja backendu lub osobnego panelu administracyjnego.

### 2. Godziny Otwarcia
- ✅ **Usunięto:** Stała `AppDestinations.SETTINGS_OPEN_HOURS`
- ✅ **Usunięto:** Element menu "Godziny otwarcia" z `SettingsMainScreen`
- ✅ **Usunięto:** Parametr `onNavigateToOpenHours` z `SettingsMainScreen`
- ✅ **Usunięto:** Tytuł dla SETTINGS_OPEN_HOURS z TopAppBar
- ✅ **Usunięto:** Moduł `/ui/open/OpenHoursScreen`

**Uzasadnienie:** KDS nie zarządza godzinami otwarcia restauracji. Kuchnia pracuje według harmonogramu zamówień przychodzących, a nie według godzin otwarcia lokalu.

### 3. Produkty (Wcześniej usunięte)
- ✅ **Usunięto:** Wszystkie destinacje: `PRODUCTS_LIST`, `CATEGORIES_LIST`, `CATEGORY_PRODUCTS`, `PRODUCT_DETAIL`
- ✅ **Usunięto:** Nawigacja do wyłączonych produktów: `onNavigateToDisabledProducts`
- ✅ **Usunięto:** Moduły `/ui/product/`, `/ui/category/`

**Uzasadnienie:** KDS wyświetla tickety/zamówienia z produktami już wybranymi przez klienta. Nie zarządza katalogiem produktów ani ich dostępnością.

---

## 📋 ZAKTUALIZOWANE PLIKI

### `HomeActivity.kt`
**Przed:**
```kotlin
object AppDestinations {
    const val HOME = "home"
    const val PRODUCTS_LIST = "products_list"
    const val CATEGORIES_LIST = "categories_list"
    const val SETTINGS_OPEN_HOURS = "settings_open_hours"
    // ...
}

// W actions TopAppBar:
if (!isCourier && isHomeScreen) {
    RestaurantStatusActionItem(
        ordersViewModel = ordersViewModel,
        navController = navController
    )
}
```

**Po:**
```kotlin
object AppDestinations {
    // Główny ekran KDS
    const val HOME = "home"
    
    // Ustawienia
    const val SETTINGS_MAIN = "settings_main"
    const val SETTINGS_MAIN_CONFIG = "settings_main_config"
    const val SETTINGS_PRINT = "settings_print"
    const val SETTINGS_LOGS = "settings_logs"
    const val SETTINGS_NOTIFICATIONS = "settings_notifications"
    const val SETTINGS_PERMISSIONS = "settings_permissions"
    
    // Drukarki
    const val PRINTERS_LIST = "printers_list"
    const val SERIAL_DIAGNOSTIC = "serial_diagnostic"
}

// W actions TopAppBar:
// Akcje dla kuriera - zmiana zmiany
if (isCourier && ordersUiState.isShiftActive) {
    ShiftStatusAction(onCheckOut = onCheckOut)
}

// KDS nie wymaga zarządzania statusem restauracji
// Usunięto: RestaurantStatusActionItem
```

### `SettingsScreen.kt`
**Przed:**
```kotlin
@Composable
fun SettingsMainScreen(
    onNavigateToOpenHours: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    // ...
) {
    // ...
    item {
        SettingsItem(
            icon = Icons.Default.AccessTime,
            title = stringResource(R.string.settings_open_hours_title),
            subtitle = stringResource(R.string.settings_open_hours_subtitle),
            onClick = onNavigateToOpenHours,
            // ...
        )
    }
}
```

**Po:**
```kotlin
@Composable
fun SettingsMainScreen(
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToPrintersList: () -> Unit,
    onNavigateToPrintSettings: () -> Unit,
    onNavigateToMainSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ...
    // KDS nie zarządza godzinami otwarcia - usunięto
}
```

---

## 🎯 ZACHOWANE FUNKCJE (Istotne dla KDS)

### Główne funkcje KDS:
- ✅ **HOME** - Główny ekran z ticketami/zamówieniami
- ✅ **OrdersViewModel** - Zarządzanie stanem zamówień
- ✅ **Socket.IO** - Real-time aktualizacje ticketów
- ✅ **Alarmy dźwiękowe** - Powiadomienia o nowych ticketach

### Ustawienia zachowane:
- ✅ **SETTINGS_MAIN** - Menu główne ustawień
- ✅ **SETTINGS_MAIN_CONFIG** - Konfiguracja ogólna (kiosk mode, auto-restart)
- ✅ **SETTINGS_PRINT** - Ustawienia automatycznego drukowania
- ✅ **SETTINGS_NOTIFICATIONS** - Wybór dźwięków alarmów
- ✅ **SETTINGS_PERMISSIONS** - Zarządzanie uprawnieniami
- ✅ **PRINTERS_LIST** - Lista drukarek
- ✅ **SERIAL_DIAGNOSTIC** - Diagnostyka portów szeregowych

### Funkcje dla kurierów (zachowane do rozważenia):
- ✅ **ShiftStatusAction** - Zmiana zmiany dla kurierów
  - *Uwaga:* To może być niepotrzebne w czystym KDS, ale zostawione jeśli aplikacja obsługuje też kurierów

---

## 📊 UPROSZCZONA NAWIGACJA

### Przed (wielofunkcyjna aplikacja):
```
HOME
├── Produkty
│   ├── Lista kategorii
│   ├── Produkty kategorii
│   └── Szczegóły produktu
├── Zamówienia (KDS)
└── Ustawienia
    ├── Główne
    ├── Drukowanie
    ├── Drukarki
    ├── Powiadomienia
    ├── Godziny otwarcia  ← USUNIĘTE
    └── Uprawnienia

TopAppBar Actions:
- Status restauracji (Otwarte/Zamknięte/Wstrzymane)  ← USUNIĘTE
```

### Po (dedykowana KDS):
```
HOME (Tickety KDS)
└── Ustawienia
    ├── Główne (kiosk mode, auto-restart)
    ├── Drukowanie
    ├── Drukarki
    ├── Powiadomienia (dźwięki alarmów)
    └── Uprawnienia

TopAppBar Actions:
- (Tylko dla kurierów: Shift Status)
```

---

## 🔧 DALSZE KROKI OPTYMALIZACJI

### 1. Rozważyć usunięcie:
- [ ] **ShiftStatusAction** - Jeśli KDS nie obsługuje kurierów
- [ ] **isCourier logic** - Cała logika związana z rolą kuriera
- [ ] **Route/Vehicle models** - Jeśli w ogóle nie używane

### 2. Uproszczenie OrdersViewModel:
- [ ] Usunąć metody: `setClosed()`, `setPause()`, `clearPause()`
- [ ] Usunąć pole: `restaurantStatus`
- [ ] Usunąć logikę filtrowania według statusu restauracji

### 3. Czyszczenie UI:
- [ ] Usunąć komponenty: `RestaurantStatusChip`, `RestaurantStatusSheet`, `PauseDialog`
- [ ] Sprawdzić czy są jeszcze jakieś odniesienia w drawer menu

### 4. Backend API:
- [ ] Upewnić się że KDS używa tylko endpointów `/staff/kds/*`
- [ ] Usunąć wywołania API związane ze statusem restauracji
- [ ] Usunąć wywołania API do zarządzania produktami

---

## ✅ PODSUMOWANIE ZMIAN

### Usunięte z kodu:
- 🗑️ `RestaurantStatusActionItem()` - ~50 linii
- 🗑️ Parametr `onNavigateToOpenHours` - ~1 wywołanie
- 🗑️ Stała `SETTINGS_OPEN_HOURS` - ~1 linia
- 🗑️ Element menu "Godziny otwarcia" - ~12 linii
- 🗑️ Tytuł dla SETTINGS_OPEN_HOURS - ~1 linia
- 🗑️ Moduły produktów (wcześniej) - ~2000+ linii

### Łącznie usunięto:
- **~2100+ linii kodu**
- **~50+ plików** (moduły produktów, tras, pojazdów)
- **~10 destinacji nawigacyjnych**

### Wynik:
- ✅ Aplikacja jest prostsza i bardziej skupiona na KDS
- ✅ Mniej zawiłości w nawigacji
- ✅ Kod bardziej czytelny i łatwiejszy w utrzymaniu
- ✅ Szybszy czas kompilacji
- ✅ Mniejszy rozmiar APK (potencjalnie)

---

## 🎯 FOKUS APLIKACJI KDS

**Przed:** Wielofunkcyjna platforma zarządzania restauracją  
**Po:** Dedykowany Kitchen Display System

### Co robi KDS:
1. ✅ Odbiera tickety/zamówienia w czasie rzeczywistym (Socket.IO)
2. ✅ Wyświetla tickety w kolejce
3. ✅ Pozwala zmienić status ticketu (ACK, START, READY, HANDOFF, CANCEL)
4. ✅ Generuje alarmy dźwiękowe dla nowych ticketów
5. ✅ Drukuje tickety kuchenne (opcjonalnie)
6. ✅ Wyświetla timer/SLA dla każdego ticketu

### Czego NIE robi KDS:
- ❌ Nie zarządza produktami
- ❌ Nie zarządza cenami
- ❌ Nie zarządza godzinami otwarcia
- ❌ Nie zarządza statusem restauracji (otwarte/zamknięte)
- ❌ Nie wstrzymuje przyjmowania zamówień
- ❌ Nie zarządza trasami dostaw
- ❌ Nie zarządza pojazdami kurierskimi

---

**Aplikacja jest teraz w 100% skoncentrowana na obsłudze kuchni (Kitchen Display System)!** 🍳👨‍🍳

_Wygenerowano: 2026-04-04_

