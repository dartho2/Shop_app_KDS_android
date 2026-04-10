# KDS — Dokumentacja Techniczna

> **ItsOrder Kitchen Display System (KDS)**  
> Wersja aplikacji: 2.068 | Android min SDK 26 | Target SDK 35  
> Pakiet: `com.itsorderkds`  
> Data dokumentu: 2026-04-06

---

## 1. Czym jest aplikacja

ItsOrder KDS to dedykowana aplikacja Android przeznaczona wyłącznie dla kuchni restauracyjnej. Działa jako **Kitchen Display System** — cyfrowy ekran zastępujący papierowe wydruki biletów kuchennych. Wyświetla zamówienia (tickety) w czasie rzeczywistym, pozwala kucharzom zarządzać ich statusem oraz sygnalizuje nowe zlecenia dźwiękiem i powiadomieniami systemowymi.

Aplikacja jest zaprojektowana do pracy **ciągłej, bezobsługowej** — na tablecie lub dedykowanym ekranie w kuchni, przez całą dobę, bez konieczności logowania się każdorazowo.

---

## 2. Architektura

### 2.1 Stos technologiczny

| Warstwa | Technologia |
|---|---|
| Język | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architektura | MVVM + Clean Architecture |
| DI | Hilt (Dagger) |
| Nawigacja | Jetpack Navigation Compose |
| Real-time | Socket.IO Client 2.0 |
| HTTP | Retrofit 2 + OkHttp |
| Preferencje | DataStore (Preferences) |
| Tło | Foreground Service (`SocketService`) |
| Powiadomienia | NotificationManager + MediaPlayer |
| Logi | Timber |
| Crashlytics | Firebase Crashlytics |

### 2.2 Warstwy aplikacji

```
┌─────────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose)                     │
│  KdsScreen · KdsTicketCard · KdsFilterRow        │
│  SettingsScreen · NotificationSettings           │
├─────────────────────────────────────────────────┤
│  ViewModel Layer (Hilt + StateFlow)             │
│  KdsViewModel · OrdersViewModel · SettingsVM    │
├─────────────────────────────────────────────────┤
│  Domain / Repository Layer                      │
│  KdsRepository · KdsSocketEventsRepository      │
├─────────────────────────────────────────────────┤
│  Data Layer                                     │
│  KdsApi (Retrofit) · SocketManager (Socket.IO)  │
│  AppPreferencesManager (DataStore)              │
├─────────────────────────────────────────────────┤
│  Services (Foreground)                          │
│  SocketService · OrderAlarmService              │
└─────────────────────────────────────────────────┘
```

---

## 3. Modele danych

### 3.1 KdsTicket — bilet kuchenny

```
KdsTicket
  ├── id             : String        — unikalny ID (MongoDB _id)
  ├── orderId        : String        — powiązane zamówienie
  ├── orderNumber    : String        — numer czytelny dla kucharza (np. "#0042")
  ├── source         : String?       — źródło: "checkout", "portal", itp.
  ├── priority       : String        — "normal" | "rush"
  ├── state          : String        — aktualny stan (patrz §4)
  ├── note           : String?       — notatka do zamówienia
  ├── scheduledFor   : String?       — ISO 8601 — zaplanowana godzina realizacji
  ├── slaTargetAt    : String?       — ISO 8601 — deadline SLA (createdAt + 15 min)
  ├── startedAt      : String?       — kiedy kuchnia zaczęła przygotowanie
  ├── readyAt        : String?       — kiedy zamówienie było gotowe
  ├── handedOffAt    : String?       — kiedy wydano
  ├── cancelledAt    : String?       — kiedy anulowano
  ├── createdAt      : String        — timestamp utworzenia
  └── updatedAt      : String        — timestamp ostatniej zmiany
```

### 3.2 KdsTicketItem — pozycja biletu

```
KdsTicketItem
  ├── id          : String        — unikalny ID pozycji
  ├── ticketId    : String        — ref do KdsTicket._id
  ├── displayName : String        — nazwa produktu do wyświetlenia
  ├── qty         : Int           — ilość sztuk
  ├── state       : String        — QUEUED | COOKING | READY | SERVED | VOID
  ├── notes       : List<String>  — alergeny, modyfikacje, życzenia klienta
  ├── stationId   : String?       — stanowisko kuchni (opcjonalne)
  ├── sequence    : Int           — kolejność wyświetlania
  ├── firedAt     : String?       — kiedy zaczęto gotować pozycję
  └── doneAt      : String?       — kiedy zakończono pozycję
```

### 3.3 KdsStation — stanowisko kuchni

```
KdsStation
  ├── id           : String   — unikalny ID
  ├── code         : String   — uppercase, np. "GRILL", "FRYER", "BAR"
  ├── name         : String   — nazwa wyświetlana
  ├── isActive     : Boolean
  └── displayOrder : Int      — kolejność sortowania
```

---

## 4. Cykl życia ticketu (State Machine)

```
                     ┌─────────────────────────────────────────────────────┐
                     │                     NEW                             │
                     │      (bilet właśnie przyszedł z zamówienia)         │
                     └──────────┬────────────────┬───────────────┬─────────┘
                                │ ACK             │ START         │ CANCEL
                                ▼                 ▼               ▼
                     ┌──────────────────┐  ┌────────────────┐  ┌──────────────┐
                     │     ACKED        │  │  IN_PROGRESS   │  │  CANCELLED   │
                     │  (przyjęto)      │  │  (gotowanie)   │  │              │
                     └──────────┬───────┘  └───────┬────────┘  └──────────────┘
                                │ START             │ READY
                                │                   ▼
                                └──────────► ┌──────────────┐
                                             │    READY     │
                                             │  (gotowe)    │
                                             └──────┬───────┘
                                                    │ HANDOFF
                                                    ▼
                                             ┌──────────────┐
                                             │  HANDED_OFF  │
                                             │  (wydane)    │
                                             └──────────────┘
```

**Stany ticketu:**

| Stan | Opis | Kolor w UI |
|---|---|---|
| `NEW` | Nowy bilet, czeka na potwierdzenie | Niebieski |
| `ACKED` | Kuchnia potwierdziła przyjęcie | Fioletowy |
| `IN_PROGRESS` | Trwa przygotowanie potraw | Żółty |
| `READY` | Zamówienie gotowe do wydania | Zielony |
| `HANDED_OFF` | Wydane kurierowi / obsłudze | Niebieski jasny |
| `CANCELLED` | Anulowane | Szary |

**Stany pozycji (KdsTicketItem):**

| Stan | Opis |
|---|---|
| `QUEUED` | Czeka na przygotowanie |
| `COOKING` | W trakcie gotowania |
| `READY` | Gotowe |
| `SERVED` | Wydane gościowi |
| `VOID` | Unieważnione |

---

## 5. Widok główny — KdsScreen

### 5.1 Układ ekranu

```
┌──────────────────────────────────────────────────────────────┐
│  TopAppBar: [☰ Menu]  KDS — ItsOrder  [WiFi] [⟳ Refresh]    │
├──────────────────────────────────────────────────────────────┤
│  FilterRow:                                                   │
│  [Aktywne] [Nowe] [Przyjęte] [W przygotowaniu] [Gotowe]      │
│           [📅 Zaplanowane (badge: N)]                         │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ◄── LazyRow (horizontal scroll) ──►                        │
│                                                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ KdsTicket   │  │ KdsTicket   │  │ KdsTicket   │  ...    │
│  │    Card     │  │    Card     │  │    Card     │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### 5.2 Filtry i zakładki

Ekran posiada wiersz filtrów chipowych (FilterChip) umożliwiający szybkie sortowanie widoku:

| Filtr | Co pokazuje |
|---|---|
| **Aktywne** (domyślny) | Wszystkie NEW/ACKED/IN_PROGRESS/READY + zaplanowane ≤ 60 min |
| **Nowe** | Tylko `NEW` |
| **Przyjęte** | Tylko `ACKED` |
| **W przygotowaniu** | Tylko `IN_PROGRESS` |
| **Gotowe** | Tylko `READY` |
| **📅 Zaplanowane** | Zamówienia z `scheduledFor` > 60 min w przyszłości |

Filtr "Zaplanowane" posiada **badge** z liczbą ukrytych zamówień zaplanowanych.

### 5.3 Sortowanie ticketów w widoku Aktywne

Tickety są sortowane wg:
1. **RUSH** na pierwszym miejscu (priorytet)
2. Następnie wg `scheduledFor` (zaplanowane) lub `createdAt` (reszta) — rosnąco

---

## 6. Karta ticketu — KdsTicketCard

Każde zamówienie wyświetlane jest jako karta (320 dp szerokości) zawierająca:

### 6.1 Elementy karty

```
┌──────────────────────────────────────────┐
│ [⏰ Zaplanowane na 14:30 — za 45 min]    │  ← baner (tylko jeśli scheduledFor)
├──────────────────────────────────────────┤
│ #0042                    ⚡ RUSH          │
│ W przygotowaniu          🕐 02:15         │  ← timer SLA
├──────────────────────────────────────────┤
│ 📝 Bez cebuli, alergia na gluten         │  ← notatka (jeśli jest)
├──────────────────────────────────────────┤
│ ×2  Burger Classic                 [▶]   │
│ ×1  Frytki Large                   [✓]   │
│ ×1  ~~Cola 0,5~~  (przekreślone)   [✓]   │
├──────────────────────────────────────────┤
│    [PRZYJMIJ]      [START]               │
│    [        ANULUJ       ]               │
└──────────────────────────────────────────┘
```

### 6.2 Timer SLA

Każda karta posiada odliczanie czasu do przekroczenia SLA (Service Level Agreement):
- **Zielony** — ponad 2 minuty do deadlinu
- **Żółty** — 0–2 minuty do deadlinu  
- **Czerwony** — SLA przekroczone (licznik z minusem)

Timer odświeżany co **1 sekundę**.

### 6.3 Baner zaplanowanego zamówienia

Gdy ticket ma pole `scheduledFor`, na górze karty pojawia się baner informujący o:
- Zaplanowanej godzinie realizacji (sformatowanej w lokalnej strefie czasowej)
- Odliczaniu do czasu gotowania

Kolory banera w zależności od czasu pozostałego:

| Czas | Kolor | Komunikat |
|---|---|---|
| > 120 min | Fioletowy | "za Xh Ymin" |
| 61–120 min | Pomarańczowy | "za Xh Ymin" |
| 1–60 min | Czerwony | "za Xmin — zacznij gotować!" |
| 0 – (-10) min | Czerwony | "CZAS ZACZĄĆ!" |
| < -10 min | Szary | "spóźnione Xmin" |

### 6.4 Przyciski akcji (per stan)

| Stan | Dostępne przyciski |
|---|---|
| `NEW` | PRZYJMIJ · START · ANULUJ |
| `ACKED` | ZACZNIJ PRZYGOTOWANIE · ANULUJ |
| `IN_PROGRESS` | GOTOWE · ANULUJ |
| `READY` | WYDAJ |
| `HANDED_OFF` | (tylko etykieta "Wydane") |
| `CANCELLED` | (tylko etykieta "Anulowane") |

**Zamówienia zaplanowane > 60 min** mają zablokowane przyciski START — można tylko PRZYJĄĆ lub ANULOWAĆ. Komunikat: *"Poczekaj na czas realizacji"*.

### 6.5 Akcje na poziomie pozycji

Każda pozycja `KdsTicketItem` ma własne przyciski:
- **▶ (QUEUED → COOKING)** — niebieska ikona play
- **✓ (COOKING → READY)** — zielona ikona check
- Pozycje gotowe są przekreślone i wyszarzone

---

## 7. Zamówienia zaplanowane (Scheduled Orders)

### 7.1 Logika widoczności

Ticket z polem `scheduledFor` (zaplanowana godzina realizacji) jest traktowany następująco:

```
scheduledFor - now > 60 min  →  Zakładka "Zaplanowane" (ukryty z Aktywnych)
scheduledFor - now ≤ 60 min  →  Wskakuje automatycznie do widoku "Aktywnych"
scheduledFor - now ≤ 0       →  Traktowany jak zwykłe zamówienie (spóźnione)
```

### 7.2 Automatyczne przejście do Aktywnych

Ticker minutowy (`_minuteTick` co 60 s) wymusza przeliczenie `filteredTickets`. Gdy zaplanowany ticket wchodzi w okno **≤ 60 minut**, automatycznie pojawia się w widoku "Aktywne" bez żadnej interakcji operatora.

### 7.3 Alert "Zaplanowane wkrótce"

Gdy `isScheduledSoon()` == true (0–60 min do planowanego czasu), aplikacja emituje zdarzenie `KdsEvent.ScheduledSoon`, które wyświetla snackbar:  
> *"⏰ Zaplanowane zamówienie #0042 — za 45 min!"*

---

## 8. Komunikacja real-time — Socket.IO

### 8.1 Przepływ danych

```
Serwer (Socket.IO)
       │
       │  namespace: /staff
       │  path: /socket.io/v3/
       │  auth: Bearer JWT
       ▼
SocketManager (singleton)
       │
       ├── KdsSocketEventsHandler
       │      ├── KDS_TICKET_CREATED  → fetch HTTP (ticket + items) → KdsSocketEventsRepository
       │      ├── KDS_TICKET_ACKED    → KdsSocketEventsRepository.emitTicketStateChanged
       │      ├── KDS_TICKET_STARTED  → KdsSocketEventsRepository.emitTicketStateChanged
       │      ├── KDS_TICKET_READY    → KdsSocketEventsRepository.emitTicketStateChanged
       │      ├── KDS_TICKET_HANDOFF  → KdsSocketEventsRepository.emitTicketStateChanged
       │      ├── KDS_TICKET_CANCEL   → KdsSocketEventsRepository.emitTicketStateChanged
       │      ├── KDS_ITEM_STARTED    → KdsSocketEventsRepository.emitItemStateChanged
       │      └── KDS_ITEM_READY      → KdsSocketEventsRepository.emitItemStateChanged
       │
       └── SocketStaffEventsHandler (inne eventy, nieistotne dla KDS)

KdsSocketEventsRepository (SharedFlow)
       │
       ▼
KdsViewModel (collectLatest)
       │
       ▼
_ticketsMap (MutableStateFlow<Map<String, KdsTicketEntry>>)
       │
       ▼
filteredTickets (StateFlow<List<KdsTicketEntry>>) → KdsScreen → KdsTicketCard
```

### 8.2 Obsługiwane zdarzenia Socket.IO

| Zdarzenie | Akcja |
|---|---|
| `KDS_TICKET_CREATED` | Fetch pełnego ticketu przez HTTP, dodanie do mapy, powiadomienie heads-up |
| `KDS_TICKET_ACKED` | Aktualizacja stanu → `ACKED` |
| `KDS_TICKET_STARTED` | Aktualizacja stanu → `IN_PROGRESS` |
| `KDS_TICKET_READY` | Aktualizacja stanu → `READY` |
| `KDS_TICKET_HANDOFF` | Aktualizacja stanu → `HANDED_OFF` |
| `KDS_TICKET_CANCEL` | Aktualizacja stanu → `CANCELLED` |
| `KDS_ITEM_STARTED` | Aktualizacja pozycji → `COOKING` |
| `KDS_ITEM_READY` | Aktualizacja pozycji → `READY` |

### 8.3 Reconnect

Socket.IO posiada wbudowany mechanizm `reconnection = true`. Po ponownym połączeniu (`EVENT_CONNECT`) `KdsViewModel` automatycznie odświeża wszystkie aktywne tickety przez HTTP (`loadActiveTickets()`), wyrównując ewentualne różnice.

### 8.4 Obsługa błędów autoryzacji

Gdy serwer zwróci `HTTP 401/403` podczas handshake lub wyśle event `unauthorized`, `SocketManager` blokuje auto-reconnect i wywołuje callback `onAuthExpired` → `SocketService` → `HomeActivity` → wylogowanie.

---

## 9. REST API — endpointy KDS

Bazowy URL konfigurowany przez DataStore (możliwość zmiany w ustawieniach).

### 9.1 Odczyt

| Metoda | Endpoint | Opis |
|---|---|---|
| `GET` | `/client/v3/api/staff/kds/tickets` | Lista ticketów (parametry: state, from, to, priority, limit, skip) |
| `GET` | `/client/v3/api/staff/kds/tickets/:id` | Pojedynczy ticket + wszystkie pozycje |
| `GET` | `/client/v3/api/staff/kds/stations` | Lista stanowisk kuchni |

### 9.2 Komendy ticketu

| Metoda | Endpoint | Przejście stanu | Nagłówek wymagany |
|---|---|---|---|
| `POST` | `/staff/kds/tickets/:id/ack` | NEW → ACKED | `Idempotency-Key` |
| `POST` | `/staff/kds/tickets/:id/start` | NEW\|ACKED → IN_PROGRESS | `Idempotency-Key` |
| `POST` | `/staff/kds/tickets/:id/ready` | * → READY | `Idempotency-Key` |
| `POST` | `/staff/kds/tickets/:id/handoff` | READY → HANDED_OFF | `Idempotency-Key` |
| `POST` | `/staff/kds/tickets/:id/cancel` | * → CANCELLED (wymaga `reason` w body) | `Idempotency-Key` |

### 9.3 Komendy pozycji

| Metoda | Endpoint | Przejście stanu |
|---|---|---|
| `POST` | `/staff/kds/items/:id/start` | QUEUED → COOKING |
| `POST` | `/staff/kds/items/:id/ready` | * → READY |

Każde żądanie mutujące wysyłane jest z nagłówkiem `Idempotency-Key` (UUID v4) zapobiegającym podwójnemu przetworzeniu przy retry.

---

## 10. Powiadomienia i alarmy dźwiękowe

### 10.1 Kanały powiadomień

| Kanał | ID | Priorytet | Opis |
|---|---|---|---|
| Nowe tickety KDS | `kds_new_ticket_v1` | HIGH | Heads-up po każdym nowym tickecie |
| Alarm zamówień | `order_alarm_channel_v4` | HIGH | Ciągły dźwięk alarmu (MediaPlayer) |
| Rozłączenie WS | `ws_disconnect_alert_v1` | HIGH | Alert braku połączenia |
| Usługa Socket | `socket_service_channel` | LOW | Stała notyfikacja foreground service |
| Alerty FGS | `fgs_alerts_channel` | HIGH | Problemy z działaniem w tle |

### 10.2 Nowe zamówienie — zachowanie

Po odebraniu eventu `KDS_TICKET_CREATED`:
1. `KdsSocketEventsHandler` pobiera pełny ticket przez HTTP
2. `KdsSocketEventsRepository.emitTicketCreated()` → `KdsViewModel` dodaje ticket do mapy
3. `NotificationHelper.showNewKdsTicket()` wyświetla **jednorazowe** powiadomienie heads-up (dźwięk systemowy raz)
4. `KdsViewModel` emituje `KdsEvent.NewTicket` → snackbar w UI: *"🆕 Nowe zamówienie: #0042"*

> **Ważne:** Powiadomienie o nowym tickecie jest **jednorazowe** — dźwięk odtwarzany raz i nie zapętla się. Alarm `OrderAlarmService` z zapętlonym dźwiękiem jest używany wyłącznie dla starszego mechanizmu zamówień (ORDER_NEW), nie dla ticketów KDS.

### 10.3 OrderAlarmService — charakterystyka

`OrderAlarmService` to `ForegroundService` zarządzający alarmem dźwiękowym:
- Odtwarza wybrany przez użytkownika dźwięk przez `MediaPlayer` w pętli (`isLooping = true`)
- Flaga `isAlarmActive` zapobiega podwójnemu uruchomieniu
- Akcja `ACTION_STOP_ALARM` wycisza alarm i zamyka serwis
- Dźwięk wybierany z `AppPreferencesManager` (ustawienia powiadomień)
- Uruchamiany jako Foreground Service z powiadomieniem na pasku (możliwość wyciszenia bez otwierania aplikacji)

### 10.4 Typy dźwięków

Użytkownik może wybrać w ustawieniach (`NotificationSettingsScreen`) oddzielny dźwięk dla każdego typu powiadomienia (`order_alarm`, lub globalnie). Dostępne dźwięki przechowywane jako `raw` zasoby aplikacji.

---

## 11. Inicjalizacja i ładowanie danych

### 11.1 Sekwencja startowa

```
HomeActivity.onCreate()
  → startService(SocketService)          — start Socket.IO w tle
  → setContent(ItsOrderChatTheme)
    → MainAppContainer
      → isHomeReady (OrdersViewModel)    — oczekiwanie na inicjalizację
        → AppLoadingScreen (splash)
          lub
        → MainScaffoldContent
          → KdsScreen
            → KdsViewModel.init()
              → loadActiveTickets()      — REST: pobierz NEW, ACKED, IN_PROGRESS, READY
              → observeSocketEvents()    — subskrybuj SharedFlow
```

### 11.2 loadActiveTickets()

Przy każdym starcie i po reconnect Socket.IO:
1. Iteruje przez stany: `NEW`, `ACKED`, `IN_PROGRESS`, `READY`
2. Dla każdego stanu wywołuje `GET /kds/tickets?state=X&limit=100`
3. Dla każdego ticketu pobiera szczegóły z pozycjami `GET /kds/tickets/:id`
4. Buduje mapę `ticketId → KdsTicketEntry`
5. Po załadowaniu sprawdza `checkScheduledAlerts()` — emituje alerty dla zaplanowanych "wkrótce"

---

## 12. Tryby pracy tabletu / ustawienia terminala

Aplikacja wspiera pracę jako **terminal kuchenny** z możliwością konfiguracji:

| Ustawienie | Opis |
|---|---|
| **Kiosk Mode** | Blokuje ekran w trybie kiosku (Android LockTask) — użytkownik nie może opuścić aplikacji |
| **Auto-restart** | Po zniszczeniu Activity aplikacja uruchamia się ponownie automatycznie |
| **Task Reopen** | Gdy aplikacja przejdzie w tło, automatycznie wraca na pierwszy plan po 200 ms |

Ustawienia konfigurowane przez `MainSettingsScreen` → zapisywane w `AppPreferencesManager` (DataStore).

---

## 13. Nawigacja

```
HOME (KdsScreen) ──── hamburger menu (DrawerContent) ────► SETTINGS_MAIN
                                                                │
                                               ┌────────────────┤
                                               │                │
                                    SETTINGS_MAIN_CONFIG   SETTINGS_PRINT
                                    (kiosk/restart)         (auto-print)
                                               │
                                    SETTINGS_NOTIFICATIONS
                                    (dźwięki alarmów)
                                               │
                                    SETTINGS_PERMISSIONS
                                    (uprawnienia Android)
                                               │
                                    PRINTERS_LIST
                                    (drukarki USB/BT)
                                               │
                                    SERIAL_DIAGNOSTIC
                                    (diagnostyka portu)
```

Back-press na HOME → `moveTaskToBack(true)` (aplikacja zostaje w pamięci, nie zamyka się).

---

## 14. Drukowanie

Opcjonalne automatyczne drukowanie biletów kuchennych po przyjęciu zamówienia:

- **Drukarki USB** (ESC/POS) — połączenie przez Android USB Host
- **Drukarki Bluetooth** (ESC/POS) — parowanie BT
- **Drukarki szeregowe** — przez port AIDL (np. wbudowane w tablety przemysłowe)
- Konfiguracja: `PrintSettingsScreen` — per drukarki (kuchenna / główna / obie)
- Ustawienia przechowywane w `AppPreferencesManager`

---

## 15. Bezpieczeństwo i autoryzacja

- Autoryzacja JWT (Bearer Token) — token przechowywany w DataStore
- `AuthInterceptor` — automatyczne dodawanie `Authorization: Bearer <token>` do każdego żądania HTTP
- `TokenAuthenticator` — obsługa `HTTP 401` → odświeżanie tokenu refresh
- Socket.IO — token przekazywany w polu `auth` podczas handshake
- Automatyczne wylogowanie przy wygaśnięciu sesji (`FORCE_LOGOUT` broadcast)
- Tryb Kiosk zapobiega opuszczeniu aplikacji przez nieupoważniony personel

---

## 16. Stabilność i odporność na błędy

| Scenariusz | Zachowanie |
|---|---|
| Brak internetu | Socket.IO auto-reconnect; UI pokazuje ikonę braku połączenia |
| Rozłączenie Socket.IO | Po reconnect: pełne odświeżenie ticketów przez HTTP |
| Błąd HTTP (5xx) | Snackbar z komunikatem błędu; ticket nie znika z UI |
| Idempotency | Każda mutacja wysyłana z `Idempotency-Key` (UUID v4) — bezpieczne retry |
| Podwójny alarm | Flaga `isAlarmActive` blokuje drugie wywołanie `startAlarmSound()` |
| Restart Activity | `OrdersViewModel.isHomeReady` blokuje UI aż inicjalizacja zakończy się |
| Aplikacja w tle | Foreground Service utrzymuje połączenie Socket.IO nieprzerwalnie |

---

## 17. Uproszczona mapa kluczowych plików

```
app/src/main/java/com/itsorderkds/
│
├── ui/kds/
│   ├── KdsScreen.kt          — główny ekran KDS (filtry + lista kart)
│   ├── KdsTicketCard.kt      — karta pojedynczego ticketu
│   └── KdsViewModel.kt       — logika UI, filtry, obsługa eventów
│
├── data/
│   ├── model/
│   │   ├── KdsTicket.kt      — model ticketu + metody isScheduled*, isRush*
│   │   ├── KdsTicketItem.kt  — model pozycji
│   │   ├── KdsStation.kt     — model stanowiska
│   │   └── KdsEnums.kt       — enumeracje stanów
│   ├── network/
│   │   └── KdsApi.kt         — interfejs Retrofit (REST)
│   └── repository/
│       └── KdsRepository.kt  — warstwa dostępu do danych HTTP
│
├── service/
│   ├── SocketManager.kt               — inicjalizacja i zarządzanie Socket.IO
│   ├── SocketService.kt               — Foreground Service utrzymujący połączenie
│   ├── KdsSocketEventsHandler.kt      — parsowanie eventów Socket.IO → repo
│   ├── KdsSocketEventsRepository.kt   — SharedFlow: ticketCreated, stateChanged
│   └── OrderAlarmService.kt           — Foreground Service dźwięku alarmowego
│
├── notification/
│   └── NotificationHelper.kt          — kanały, heads-up, powiadomienia KDS
│
└── ui/settings/
    ├── SettingsScreen.kt              — menu ustawień
    ├── MainSettingsScreen.kt          — kiosk mode, auto-restart
    ├── NotificationSettingsScreen.kt  — wybór dźwięków alarmów
    └── PrintSettingsScreen.kt         — konfiguracja auto-drukowania
```

---

## 18. Wymagania sprzętowe i systemowe

| Parametr | Minimum | Zalecane |
|---|---|---|
| Android | 8.0 (API 26) | 10+ (API 29+) |
| RAM | 2 GB | 4 GB |
| Ekran | 8" 1280×800 | 10" 1920×1200 |
| Sieć | WiFi 2.4 GHz | WiFi 5 GHz / Ethernet |
| Stałe zasilanie | Wymagane | Wymagane |

Aplikacja budowana dla architektur: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`.  
Wspiera strony pamięci 16 KB (`useLegacyPackaging = false`).

---

## 19. Konfiguracja połączenia z serwerem

1. Przy pierwszym uruchomieniu aplikacja wymaga zalogowania (`LoginActivity`)
2. Po zalogowaniu token JWT i URL serwera zapisywane w DataStore
3. URL serwera możliwy do zmiany w `MainSettingsScreen`
4. Socket.IO łączy się pod namespace `/staff`, ścieżka `/socket.io/v3/`

---

## 20. Słowniczek

| Termin | Znaczenie |
|---|---|
| **KDS** | Kitchen Display System — elektroniczny ekran kuchenny |
| **Ticket** | Bilet kuchenny odpowiadający jednemu zamówieniu |
| **Item** | Pojedyncza pozycja (produkt) w bilecie kuchennym |
| **SLA** | Service Level Agreement — deadline przygotowania zamówienia (domyślnie createdAt + 15 min) |
| **ACK** | Acknowledge — potwierdzenie przyjęcia biletu przez kuchnię |
| **Handoff** | Fizyczne wydanie gotowego zamówienia kurierowi/obsłudze |
| **Scheduled** | Zamówienie z ustaloną godziną realizacji (np. na konkretną godzinę) |
| **Rush** | Bilet o najwyższym priorytecie — zawsze pokazywany jako pierwszy |
| **Station** | Stanowisko kuchenne (Grill, Frytkownica, Bar itp.) |
| **Idempotency Key** | Unikalny klucz UUID v4 zapobiegający podwójnemu wykonaniu akcji |

---

*Dokument wygenerowany: 2026-04-06 | ItsOrder KDS v2.068*

