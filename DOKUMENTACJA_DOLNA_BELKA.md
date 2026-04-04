# Dokumentacja Dolnej Belki Nawigacyjnej

## Spis treści
1. [Przegląd](#przegląd)
2. [Architektura](#architektura)
3. [Konfiguracja zakładek](#konfiguracja-zakładek)
4. [Komponent StaffBottomBar](#komponent-staffbottombar)
5. [Integracja z głównym ekranem](#integracja-z-głównym-ekranem)
6. [Przepływ danych](#przepływ-danych)
7. [Synchronizacja stanu](#synchronizacja-stanu)
8. [Układ wizualny](#układ-wizualny)

---

## Przegląd

Dolna belka nawigacyjna (Bottom Navigation Bar) to kluczowy element interfejsu aplikacji dla pracowników restauracji (rola: Staff). Pozwala ona na szybkie przełączanie się między trzema głównymi widokami zamówień.

### Kluczowe cechy:
- **Widoczność**: Wyświetlana tylko na ekranie głównym (`HOME`) dla użytkowników niebędących kurierami
- **Ilość zakładek**: 3 zakładki (`staffTabs`)
- **Synchronizacja**: Dwukierunkowa synchronizacja między belką nawigacyjną a zawartością ekranu
- **Animacje**: Płynne przejścia między zakładkami z wykorzystaniem `HorizontalPager`

---

## Architektura

### Lokalizacja plików

```
app/src/main/java/com/itsorderchat/ui/theme/home/
├── HomeActivity.kt          # Główna aktywność z komponentem StaffBottomBar
├── MainScreen.kt            # Definicja zakładek (staffTabs)
└── view/
    └── StaffView.kt         # Logika wyświetlania zawartości zakładek
```

### Diagram przepływu

```
HomeActivity (Scaffold)
    └── bottomBar: { StaffBottomBar(...) }
            ├── selectedTabIndex ──────┐
            └── onTabSelected ─────────┤
                                       │
                                       ▼
                            StaffView (HorizontalPager)
                                ├── pageIndex = 0: Zamówienia (activeOrders)
                                ├── pageIndex = 1: Zakończone (completedOrders)
                                └── pageIndex = 2: Inne (groupedOrders)
```

---

## Konfiguracja zakładek

### Definicja w `MainScreen.kt`

```kotlin
internal data class OrderTab(
    @StringRes val titleRes: Int,      // ID zasobu tłumaczenia
    val icon: ImageVector,              // Ikona Material Icons
    val statuses: Set<OrderStatusEnum>  // Statusy zamówień dla filtrowania
)

internal val staffTabs = listOf(
    // Zakładka 0: Zamówienia
    OrderTab(
        R.string.order_newOrder,        // "Zamówienia"
        Icons.Default.PendingActions,
        setOf(
            OrderStatusEnum.PROCESSING, 
            OrderStatusEnum.OUT_FOR_DELIVERY,
            OrderStatusEnum.ACCEPTED
        )
    ),
    
    // Zakładka 1: Zakończone
    OrderTab(
        R.string.order_completed,       // "Zakończone"
        Icons.Default.Done,
        setOf(
            OrderStatusEnum.OUT_FOR_DELIVERY, 
            OrderStatusEnum.COMPLETED
        )
    ),
    
    // Zakładka 2: Inne
    OrderTab(
        R.string.status_other,          // "Inne"
        Icons.Default.MoreHoriz,
        emptySet()                      // Brak filtra statusu
    )
)
```

### Znaczenie pól:

- **titleRes**: Klucz do pliku `strings.xml` dla tłumaczeń
- **icon**: Ikona wyświetlana w dolnej belce
- **statuses**: Zestaw statusów używanych do filtrowania zamówień w danej zakładce

---

## Komponent StaffBottomBar

### Lokalizacja
`HomeActivity.kt`, linie 1024-1050

### Kod źródłowy

```kotlin
@Composable
private fun StaffBottomBar(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar {
        staffTabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = (selectedTabIndex == index),
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        tab.icon,
                        contentDescription = stringResource(tab.titleRes)
                    )
                },
                alwaysShowLabel = true,
                label = {
                    Text(
                        stringResource(tab.titleRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}
```

### Parametry

| Parametr | Typ | Opis |
|----------|-----|------|
| `selectedTabIndex` | `Int` | Indeks aktualnie wybranej zakładki (0-2) |
| `onTabSelected` | `(Int) -> Unit` | Callback wywoływany przy kliknięciu zakładki |

### Komponenty Material 3

- **NavigationBar**: Kontener dla dolnej belki nawigacyjnej
- **NavigationBarItem**: Pojedyncza zakładka z ikoną i etykietą

### Właściwości NavigationBarItem

- **selected**: Określa czy zakładka jest aktywna (zmienia kolor)
- **onClick**: Akcja wykonywana po kliknięciu
- **icon**: Ikona zakładki
- **label**: Etykieta tekstowa
- **alwaysShowLabel**: `true` = etykieta zawsze widoczna (nawet gdy nieaktywna)

---

## Integracja z głównym ekranem

### HomeActivity.kt - Scaffold

```kotlin
Scaffold(
    topBar = { /* ... */ },
    bottomBar = {
        // WARUNEK WIDOCZNOŚCI
        if (currentRoute == AppDestinations.HOME && 
            !isCourier && 
            showMainScaffoldLayout) {
            
            StaffBottomBar(
                selectedTabIndex = selectedStaffTabIndex,
                onTabSelected = { selectedStaffTabIndex = it }
            )
        }
    }
) { innerPadding ->
    AppNavHost(
        navController = navController,
        modifier = Modifier.padding(innerPadding),
        ordersViewModel = ordersViewModel,
        selectedStaffTabIndex = selectedStaffTabIndex,
        onStaffTabSelected = { selectedStaffTabIndex = it },
        isCourier = isCourier
    )
}
```

### Warunki wyświetlania dolnej belki

1. **currentRoute == AppDestinations.HOME**: Użytkownik jest na ekranie głównym
2. **!isCourier**: Użytkownik nie jest kurierem (tylko dla roli STAFF)
3. **showMainScaffoldLayout**: Flaga sterująca widocznością głównego układu

### Stan zakładki

```kotlin
var selectedStaffTabIndex by remember { mutableIntStateOf(0) }
```

Stan jest przechowywany w `HomeActivity` i przekazywany do:
- `StaffBottomBar` (wyświetlanie)
- `AppNavHost` → `StaffView` (zawartość)

---

## Przepływ danych

### 1. Kliknięcie zakładki w dolnej belce

```
Użytkownik klika zakładkę
    ↓
NavigationBarItem.onClick
    ↓
onTabSelected(index)
    ↓
selectedStaffTabIndex = index (HomeActivity)
    ↓
Rekomposycja StaffBottomBar + StaffView
    ↓
HorizontalPager przewija do strony 'index'
```

### 2. Przewijanie palcem (swipe gesture)

```
Użytkownik przesuwa palcem
    ↓
HorizontalPager zmienia currentPage
    ↓
LaunchedEffect(pagerState.currentPage)
    ↓
onTabSelected(pagerState.currentPage)
    ↓
selectedStaffTabIndex = pagerState.currentPage
    ↓
Rekomposycja StaffBottomBar (podświetlenie zakładki)
```

---

## Synchronizacja stanu

### StaffView.kt - Dwukierunkowa synchronizacja

```kotlin
@Composable
fun StaffView(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { staffTabs.size })
    
    // SYNCHRONIZACJA 1: HomeActivity → Pager
    LaunchedEffect(selectedTabIndex) {
        if (pagerState.currentPage != selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }
    }
    
    // SYNCHRONIZACJA 2: Pager → HomeActivity
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            onTabSelected(pagerState.currentPage)
        }
    }
    
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { pageIndex ->
        // Renderowanie zawartości dla pageIndex
    }
}
```

### Mechanizmy synchronizacji

| Kierunek | Trigger | Akcja | Cel |
|----------|---------|-------|-----|
| HomeActivity → Pager | `selectedTabIndex` zmienia się | `pagerState.animateScrollToPage()` | Przewinięcie do właściwej strony |
| Pager → HomeActivity | `pagerState.currentPage` zmienia się | `onTabSelected(currentPage)` | Aktualizacja stanu w HomeActivity |

### Warunek `isScrollInProgress`

```kotlin
if (!pagerState.isScrollInProgress) {
    onTabSelected(pagerState.currentPage)
}
```

**Dlaczego?** Zapobiega wielokrotnym wywołaniom `onTabSelected` podczas animacji przewijania.

---

## Układ wizualny

### Renderowanie zawartości zakładek

```kotlin
HorizontalPager(
    state = pagerState,
    modifier = modifier.fillMaxSize(),
    key = { staffTabs[it].titleRes }
) { pageIndex ->
    when (pageIndex) {
        // Zakładka 0: Zamówienia
        0 -> ActiveOrdersList(
            allOrders = activeOrders,
            isRefreshing = isRefreshing,
            onOrderClick = onOrderClick,
            onRefresh = onRefresh
        )
        
        // Zakładka 1: Zakończone
        1 -> StandardOrdersList(
            visibleOrders = completedOrders,
            isRefreshing = isRefreshing,
            onOrderClick = onOrderClick,
            pageIndex = pageIndex,
            selectedSubTabIndex = selectedSubTabIndex,
            isCompletedTabSelected = isCompletedTabSelected,
            onRefresh = onRefresh
        )
        
        // Zakładka 2: Inne
        2 -> GroupedOrdersList(
            groupedOrders = groupedOrders,
            isRefreshing = isRefreshing,
            onOrderClick = onOrderClick
        )
    }
}
```

### Widoki dla poszczególnych zakładek

#### **Zakładka 0: Zamówienia** (`ActiveOrdersList`)

**Cel**: Wyświetlenie aktywnych zamówień podzielonych na sekcje:

1. **PROCESSING** - Zamówienia do wykonania
2. **ACCEPTED** - Zaakceptowane zamówienia
3. **OUT_FOR_DELIVERY** - W dostawie (zwijana sekcja)

**Układ**:
```
┌─────────────────────────────┐
│ Do zrobienia                │
├─────────────────────────────┤
│ [Zamówienie #123]           │
│ [Zamówienie #124]           │
├─────────────────────────────┤
│ Zaakceptowane               │
├─────────────────────────────┤
│ [Zamówienie #125]           │
├─────────────────────────────┤
│ 🚚 W dostawie (3) ▼         │ ← Zwijany nagłówek
├─────────────────────────────┤
│ [Zamówienie #126]           │ ← Widoczne po rozwinięciu
│ [Zamówienie #127]           │
└─────────────────────────────┘
```

#### **Zakładka 1: Zakończone** (`StandardOrdersList`)

**Cel**: Wyświetlenie zakończonych zamówień z filtrami sub-zakładek

**Podzakładki** (PillSwitch):
- W dostawie (`OUT_FOR_DELIVERY`)
- Zakończone (`COMPLETED`)

**Układ**:
```
┌─────────────────────────────┐
│ [W dostawie] [Zakończone]   │ ← PillSwitch
├─────────────────────────────┤
│ [Zamówienie #130]           │
│ [Zamówienie #131]           │
│ [Zamówienie #132]           │
└─────────────────────────────┘
```

#### **Zakładka 2: Inne** (`GroupedOrdersList`)

**Cel**: Wyświetlenie zamówień pogrupowanych według portalu zamówień

**Grupy**:
- UBER, GLOVO, WOLT, BOLT, TAKEAWAY, WOOCOMMERCE, itd.

**Układ**:
```
┌─────────────────────────────┐
│ SUMA: 12 zamówień 450 PLN   │
│ Dostawy: 8 | Odbiory: 4     │
├─────────────────────────────┤
│ 🍔 UBER (5) 200 PLN ▼       │ ← Zwijany nagłówek
├─────────────────────────────┤
│ [Zamówienie #140]           │ ← Widoczne po rozwinięciu
│ [Zamówienie #141]           │
├─────────────────────────────┤
│ 🍕 GLOVO (3) 150 PLN ▼      │
├─────────────────────────────┤
│ 🚚 WOLT (4) 100 PLN ▼       │
└─────────────────────────────┘
```

---

## Stylizacja i motywy

### Material Design 3

Dolna belka wykorzystuje komponenty Material 3 z automatycznym wsparciem dla:
- **Trybu ciemnego/jasnego**
- **Kolorów motywu** (dynamiczne kolory z `MaterialTheme.colorScheme`)
- **Typografii** (`MaterialTheme.typography`)

### Kolory

```kotlin
NavigationBarItem(
    selected = true,
    // Automatyczne kolory:
    // - selected = primary color
    // - unselected = onSurfaceVariant (przyciemniony)
)
```

### Ikony

Wszystkie ikony pochodzą z `androidx.compose.material.icons.Icons`:
- `Icons.Default.PendingActions` - Oczekujące
- `Icons.Default.Done` - Zakończone
- `Icons.Default.MoreHoriz` - Inne

---

## Obsługa tłumaczeń

### Zasoby stringów

Wszystkie teksty są pobierane z `strings.xml`:

```kotlin
stringResource(tab.titleRes)
```

**Przykładowe klucze**:
```xml
<string name="order_newOrder">Zamówienia</string>
<string name="order_completed">Zakończone</string>
<string name="status_other">Inne</string>
```

### Wsparcie dla wielu języków

Aplikacja wspiera wielojęzyczność poprzez:
1. Pliki `res/values-{locale}/strings.xml`
2. Funkcję `stringResource()` w Jetpack Compose

---

## Wydajność i optymalizacja

### Kluczowe elementy wydajności

1. **remember**: Stan przechowywany lokalnie w komponentach
```kotlin
var selectedStaffTabIndex by remember { mutableIntStateOf(0) }
```

2. **derivedStateOf**: Obliczenia wykonywane tylko gdy zależności się zmieniają
```kotlin
val ordersToShow by remember(visibleOrders, isCompletedTabSelected, selectedSubTabIndex) {
    derivedStateOf { /* filtrowanie */ }
}
```

3. **LazyColumn + key**: Efektywne renderowanie list
```kotlin
items(orders, key = { "processing-${it.orderId}" }) { order ->
    OrderListItem(order, onClick = { onOrderClick(order) })
}
```

4. **Animacje**: Płynne przejścia z `tween` i `FastOutSlowInEasing`

---

## Testowanie

### Scenariusze testowe

1. **Kliknięcie zakładki**
   - Użytkownik klika zakładkę "Zakończone"
   - Oczekiwanie: Pager przewija do strony 1, dolna belka podświetla zakładkę 1

2. **Swipe gesture**
   - Użytkownik przesuwa palcem w lewo
   - Oczekiwanie: Pager przewija do następnej strony, dolna belka aktualizuje podświetlenie

3. **Rotacja ekranu**
   - Stan `selectedStaffTabIndex` powinien być zachowany
   - Oczekiwanie: Ta sama zakładka aktywna po rotacji

4. **Zmiana roli użytkownika**
   - Użytkownik zmienia rolę na COURIER
   - Oczekiwanie: Dolna belka znika, wyświetlany jest CourierView

---

## Rozszerzanie funkcjonalności

### Dodawanie nowej zakładki

**Krok 1**: Dodaj definicję w `MainScreen.kt`
```kotlin
internal val staffTabs = listOf(
    // ... istniejące zakładki ...
    OrderTab(
        R.string.nowa_zakladka,
        Icons.Default.NewIcon,
        setOf(OrderStatusEnum.NOWY_STATUS)
    )
)
```

**Krok 2**: Dodaj klucz tłumaczenia w `strings.xml`
```xml
<string name="nowa_zakladka">Nowa zakładka</string>
```

**Krok 3**: Dodaj obsługę w `StaffView.kt`
```kotlin
when (pageIndex) {
    0 -> ActiveOrdersList(...)
    1 -> StandardOrdersList(...)
    2 -> GroupedOrdersList(...)
    3 -> NowaZakladkaView(...) // NOWA
}
```

**Krok 4**: Zmodyfikuj `pagerState`
```kotlin
val pagerState = rememberPagerState(pageCount = { 4 }) // było: 3
```

---

## Diagram klas

```
┌─────────────────────────┐
│   HomeActivity          │
│  ├─ selectedStaffTabIndex│
│  └─ onTabSelected()     │
└─────────┬───────────────┘
          │ przekazuje stan
          ├────────────────────┐
          ▼                    ▼
┌─────────────────────┐  ┌─────────────────────┐
│  StaffBottomBar     │  │    StaffView        │
│  ├─ NavigationBar   │  │  ├─ HorizontalPager │
│  └─ NavigationBarItem│  │  ├─ LaunchedEffect │
└─────────────────────┘  │  └─ when(pageIndex) │
                         └─────────┬───────────┘
                                   │ renderuje
                ┌──────────────────┼──────────────────┐
                ▼                  ▼                  ▼
      ┌──────────────────┐ ┌──────────────┐ ┌──────────────┐
      │ActiveOrdersList  │ │StandardOrders│ │GroupedOrders │
      │                  │ │List          │ │List          │
      └──────────────────┘ └──────────────┘ └──────────────┘
```

---

## Najlepsze praktyki

1. **Jednokierunkowy przepływ danych**: Stan spływa w dół, zdarzenia płyną w górę
2. **Kompozycja zamiast dziedziczenia**: Wszystkie komponenty to funkcje @Composable
3. **Stan hoistowany**: `selectedStaffTabIndex` w `HomeActivity` (najwyższy wspólny przodek)
4. **Niemutowalność**: Używanie `val` zamiast `var` gdzie to możliwe
5. **Rozdzielenie odpowiedzialności**: StaffBottomBar tylko wyświetla, StaffView zarządza logiką

---

## Znane problemy i ograniczenia

1. **Brak animacji licznika**: Liczba zamówień w zakładce nie jest wyświetlana w belce
2. **Brak badge notifications**: Brak wizualnego wskaźnika nowych zamówień
3. **Twarda zależność od 3 zakładek**: Zmiana liczby zakładek wymaga modyfikacji wielu miejsc

---

## Historia zmian

| Data | Wersja | Zmiany |
|------|--------|--------|
| 2024-01 | 1.0 | Pierwotna implementacja z 3 zakładkami |
| 2024-02 | 1.1 | Dodanie synchronizacji dwukierunkowej |
| 2024-03 | 1.2 | Refaktoryzacja na Material 3 |

---

## FAQ

**P: Dlaczego dolna belka nie jest widoczna dla kurierów?**  
O: Kurierzy mają własny interfejs z różnymi zakładkami (Trasa, W dostawie, Zaakceptowane, Mapa).

**P: Jak zmienić domyślną zakładkę po uruchomieniu?**  
O: Zmodyfikuj inicjalizację stanu:
```kotlin
var selectedStaffTabIndex by remember { mutableIntStateOf(1) } // Start na zakładce 1
```

**P: Czy mogę ukryć etykiety i zostawić same ikony?**  
O: Tak, zmień `alwaysShowLabel = false` w `NavigationBarItem`.

**P: Jak dodać badge z liczbą nowych zamówień?**  
O: Użyj `badge = { Badge { Text("3") } }` w `NavigationBarItem`.

---

## Zasoby

- [Material Design 3 - Navigation Bar](https://m3.material.io/components/navigation-bar)
- [Jetpack Compose - HorizontalPager](https://developer.android.com/jetpack/compose/layouts/pager)
- [State Management in Compose](https://developer.android.com/jetpack/compose/state)

---

**Autor**: AI Documentation Generator  
**Ostatnia aktualizacja**: 2026-02-02  
**Wersja dokumentacji**: 1.0

