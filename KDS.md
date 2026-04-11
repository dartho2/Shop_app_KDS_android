# Android KDS — Dokumentacja integracji

> **Dotyczy:** aplikacja Android (tablet kuchenny / panel ekspedycji)  
> **Backend:** `API_MONGO` — moduł KDS (Etap 1, produkcyjny)  
> **Data:** kwiecień 2026

---

## 1. Czym jest KDS i co robi ten moduł

KDS (Kitchen Display System) to system wyświetlania zamówień dla kuchni.  
Zamiast drukować bonę na drukarce termicznej, zamówienia trafiają w czasie rzeczywistym na tablet/monitor kuchenny.

### Życie zamówienia w KDS

```
Klient składa zamówienie
        ↓
API tworzy Order (istniejący flow)
        ↓
orderOutboxWatcher wykrywa ORDER_CREATED
        ↓
createKdsTicketFromOrder() tworzy KdsTicket + KdsTicketItems
        ↓
Socket.IO emituje KDS_TICKET_CREATED do /staff (room: tenant:{tenantKey})
        ↓
Android (tablet kuchenny) odbiera event i wyświetla kartę zamówienia
        ↓
Kucharz: [ACK] → [START] → (pozycje: START ITEM / READY ITEM) → [READY]
        ↓
Ekspedycja: [HANDOFF]   lub   Anulowanie: [CANCEL]
```

---

## 2. Architektura – co Android musi wiedzieć

### 2.1. Transport HTTP + WebSocket

| Warstwa | Technologia | Użycie |
|---|---|---|
| REST API | HTTP/HTTPS | Pobieranie listy ticketów, wykonywanie komend |
| Real-time | Socket.IO 4.x | Odbiór live notyfikacji o zmianach |

Android używa **obu kanałów jednocześnie**:
- WebSocket (Socket.IO) — odbiór zdarzeń push (nowe tickety, zmiany stanu)
- REST HTTP — wykonywanie komend (ack, start, ready, handoff, cancel) i pobieranie danych przy starcie

### 2.2. Autentykacja

**Wszystkie endpointy KDS wymagają JWT.**

```
Authorization: Bearer <token_jwt>
```

Token JWT jest wydawany przez endpoint logowania (istniejący `/api/auth/login` lub `/api/auth/staff/login`).  
Payload JWT zawiera: `id`, `role`, `tenantKey`.

**Wymagane role** dla endpointów KDS:
- `STAFF`, `COURIER`, `ADMIN`, `TEST` → dostęp do `/api/staff/kds/*`
- `ADMIN`, `TEST` → dostęp do `/api/admin/kds/*`

**Socket.IO autentykacja (namespace `/staff`):**

```json
{
   "auth": {
      "token": "<token_jwt>"
   }
}
```

Token przekazywany w `handshake.auth.token` podczas łączenia.

### 2.3. Multi-tenant

Każdy lokal/restauracja to osobny **tenant**. `tenantKey` pochodzi z JWT i jest używany przez backend do izolacji danych. Android nie musi nic robić — wystarczy wysyłać poprawny JWT.

Socket.IO room to `tenant:{tenantKey}`. Backend automatycznie dodaje socket do właściwego roomu po weryfikacji JWT.

---

## 3. Modele danych

### 3.1. KdsTicket — karta zamówienia

```typescript
interface KdsTicket {
  _id: string;             // MongoDB ObjectId jako string
  orderId: string;         // ID oryginalnego zamówienia (Order._id)
  orderNumber: string;     // Numer do wyświetlenia (np. "order-42")
  source?: string;         // Źródło: "checkout", "portal", etc.
  priority: "normal" | "rush";
  state: KdsTicketState;
  note?: string;           // Notatka klienta do całego zamówienia
  startedAt?: string;      // ISO 8601
  readyAt?: string;
  handedOffAt?: string;
  cancelledAt?: string;
  slaTargetAt?: string;    // Deadline SLA (domyślnie: createdAt + 15 min)
  createdAt: string;
  updatedAt: string;
}
```

**Stany ticketu (`KdsTicketState`):**

```
NEW  →  ACKED  →  IN_PROGRESS  →  READY  →  HANDED_OFF
  \                    \
   └──────────────────→ CANCELLED
```

| Stan | Opis | Kto ustawia |
|---|---|---|
| `NEW` | Ticket właśnie przyszedł z zamówienia | System (automatycznie) |
| `ACKED` | Kuchnia potwierdziła przyjęcie | Kucharz (tablet) |
| `IN_PROGRESS` | Trwa przygotowanie | Kucharz (tablet) |
| `READY` | Gotowe do odbioru/wydania | Kucharz (tablet) |
| `HANDED_OFF` | Wydane kurierowi/obsłudze | Ekspedycja (tablet) |
| `CANCELLED` | Anulowane | Kucharz lub Admin |

**Dozwolone przejścia:**

| Komenda | Stan wejściowy (allowed) | Stan wynikowy |
|---|---|---|
| `/ack` | `NEW` | `ACKED` |
| `/start` | `NEW`, `ACKED` | `IN_PROGRESS` + ustawia `startedAt` |
| `/ready` | `NEW`, `ACKED`, `IN_PROGRESS` | `READY` + ustawia `readyAt` |
| `/handoff` | `READY` | `HANDED_OFF` + ustawia `handedOffAt` |
| `/cancel` | `NEW`, `ACKED`, `IN_PROGRESS` | `CANCELLED` + ustawia `cancelledAt` |

> **Ważne:** Backend jest idempotentny. Jeśli ticket jest już w stanie docelowym, zwróci `200 OK` z aktualnym stanem bez błędu. Bezpieczne przy retry.

---

### 3.2. KdsTicketItem — pozycja zamówienia

```typescript
interface KdsTicketItem {
  _id: string;
  ticketId: string;       // Ref do KdsTicket._id
  orderId: string;
  productId?: string;
  sku?: string;
  posId?: string;
  displayName: string;    // Nazwa produktu do wyświetlenia
  qty: number;            // Ilość sztuk
  stationId?: string;     // ID stanowiska (MVP: opcjonalne)
  state: KdsItemState;
  notes: string[];        // Alergeny, modyfikacje, życzenia
  firedAt?: string;       // ISO 8601 — kiedy zaczęto gotować
  doneAt?: string;        // ISO 8601 — kiedy ukończono
  sequence: number;       // Kolejność wyświetlania (0-based)
  createdAt: string;
  updatedAt: string;
}
```

**Stany pozycji (`KdsItemState`):**

```
QUEUED  →  COOKING  →  READY  →  SERVED
            (firedAt)   (doneAt)
```

| Stan | Opis |
|---|---|
| `QUEUED` | Czeka na przygotowanie |
| `COOKING` | W trakcie gotowania (`firedAt` ustawione) |
| `READY` | Gotowe (`doneAt` ustawione) |
| `SERVED` | Wydane gościowi |
| `VOID` | Unieważnione |

**Przejścia pozycji:**

| Komenda | Stan wejściowy | Stan wynikowy |
|---|---|---|
| `/start` | `QUEUED` | `COOKING` + ustawia `firedAt` |
| `/ready` | `QUEUED`, `COOKING` | `READY` + ustawia `doneAt` |

---

### 3.3. KdsStation — stanowisko kuchni

```typescript
interface KdsStation {
  _id: string;
  code: string;          // Unikalny kod, uppercase: "GRILL", "FRYER", "BAR"
  name: string;          // Nazwa do wyświetlenia
  isActive: boolean;
  displayOrder: number;  // Kolejność sortowania w UI
  createdAt: string;
  updatedAt: string;
}
```

---

### 3.4. KdsAudit — log akcji

```typescript
interface KdsAudit {
  _id: string;
  ticketId: string;
  orderId: string;
  actionType: KdsAuditAction;  // np. "TICKET_CREATED", "TICKET_STARTED"
  actorId?: string;            // ID użytkownika
  actorRole?: string;
  before?: Record<string, any>;
  after?: Record<string, any>;
  idempotencyKey?: string;
  createdAt: string;           // brak updatedAt — log jest niezmienny
}
```

Wartości `actionType`:
```
TICKET_CREATED | TICKET_ACKED | TICKET_STARTED | TICKET_READY |
TICKET_HANDOFF | TICKET_CANCEL | ITEM_STARTED | ITEM_READY
```

---

## 4. REST API — lista endpointów

**Base URL:** `https://{tenant-domain}/api`  
**Nagłówki wymagane dla każdego żądania:**
```
Authorization: Bearer <jwt>
Content-Type: application/json
```

---

### 4.1. Endpointy odczytu (staff)

#### GET `/staff/kds/tickets`

Pobiera listę ticketów z opcjonalnymi filtrami.

**Query params:**

| Param | Typ | Opis |
|---|---|---|
| `state` | string | Filtr stanu: `NEW`, `ACKED`, `IN_PROGRESS`, `READY`, `HANDED_OFF`, `CANCELLED` |
| `from` | ISO 8601 | Pobierz tickety od tej daty |
| `to` | ISO 8601 | Pobierz tickety do tej daty |
| `priority` | string | `normal` lub `rush` |
| `limit` | int | Maks wyników (domyślnie 100, maks 500) |
| `skip` | int | Przesunięcie dla paginacji |

**Przykład żądania:**
```
GET /api/staff/kds/tickets?state=NEW&limit=50
```

**Odpowiedź `200 OK`:**
```json
{
   "data": [
      {
         "_id": "6617a3f2e4b0c1234567890a",
         "orderId": "6617a3f1e4b0c1234567890b",
         "orderNumber": "order-42",
         "source": "checkout",
         "priority": "normal",
         "state": "NEW",
         "note": "Bez cebuli",
         "slaTargetAt": "2026-04-04T12:30:00.000Z",
         "createdAt": "2026-04-04T12:15:00.000Z",
         "updatedAt": "2026-04-04T12:15:00.000Z"
      }
   ],
   "count": 1
}
```

---

#### GET `/staff/kds/tickets/:ticketId`

Pobiera pojedynczy ticket **razem ze wszystkimi pozycjami**.

**Przykład żądania:**
```
GET /api/staff/kds/tickets/6617a3f2e4b0c1234567890a
```

**Odpowiedź `200 OK`:**
```json
{
   "ticket": {
      "_id": "6617a3f2e4b0c1234567890a",
      "orderNumber": "order-42",
      "state": "NEW",
      "slaTargetAt": "2026-04-04T12:30:00.000Z",
      "createdAt": "2026-04-04T12:15:00.000Z"
   },
   "items": [
      {
         "_id": "6617a3f3e4b0c1234567890c",
         "ticketId": "6617a3f2e4b0c1234567890a",
         "displayName": "Burger Klasyczny",
         "qty": 2,
         "state": "QUEUED",
         "notes": ["Bez majonezu"],
         "sequence": 0
      },
      {
         "_id": "6617a3f3e4b0c1234567890d",
         "ticketId": "6617a3f2e4b0c1234567890a",
         "displayName": "Frytki",
         "qty": 2,
         "state": "QUEUED",
         "notes": [],
         "sequence": 1
      }
   ]
}
```

**Odpowiedź `404`:**
```json
{ "message": "Ticket KDS nie znaleziony" }
```

---

#### GET `/staff/kds/tickets/:ticketId/audit`

Pobiera historię akcji dla ticketu.

**Query params:**

| Param | Typ | Opis |
|---|---|---|
| `limit` | int | Maks wpisów (domyślnie 50, maks 200) |

**Odpowiedź `200 OK`:**
```json
{
   "data": [
      {
         "_id": "...",
         "ticketId": "6617a3f2e4b0c1234567890a",
         "actionType": "TICKET_STARTED",
         "actorId": "user123",
         "actorRole": "STAFF",
         "before": { "state": "ACKED" },
         "after": { "state": "IN_PROGRESS" },
         "createdAt": "2026-04-04T12:17:00.000Z"
      }
   ]
}
```

---

#### GET `/staff/kds/stations`

Pobiera listę aktywnych stanowisk kuchni.

**Query params:**

| Param | Typ | Opis |
|---|---|---|
| `all` | `"true"` | Jeśli ustawiony, zwraca też nieaktywne stacje |

**Odpowiedź `200 OK`:**
```json
{
   "data": [
      {
         "_id": "6617b1a2e4b0c1234567890e",
         "code": "GRILL",
         "name": "Grill",
         "isActive": true,
         "displayOrder": 1
      },
      {
         "_id": "6617b1a2e4b0c1234567890f",
         "code": "FRYER",
         "name": "Frytkownica",
         "isActive": true,
         "displayOrder": 2
      }
   ]
}
```

---

### 4.2. Komendy ticketu (staff)

> Każda komenda może przyjąć opcjonalny nagłówek `Idempotency-Key` (string max 128 znaków).  
> Zalecane: UUID v4 generowany raz przed wysłaniem, cache-owany po stronie Android.

#### POST `/staff/kds/tickets/:ticketId/ack`

Potwierdź przyjęcie ticketu. `NEW` → `ACKED`.

**Body (opcjonalne):**
```json
{ "note": "OK, biorę" }
```

**Odpowiedź `200 OK`:** obiekt `KdsTicket` z nowym stanem.

---

#### POST `/staff/kds/tickets/:ticketId/start`

Rozpocznij przygotowanie. `NEW|ACKED` → `IN_PROGRESS`. Ustawia `startedAt`.

**Body (opcjonalne):**
```json
{ "note": "zaczynam" }
```

**Odpowiedź `200 OK`:** obiekt `KdsTicket`.

---

#### POST `/staff/kds/tickets/:ticketId/ready`

Oznacz ticket jako gotowy. `NEW|ACKED|IN_PROGRESS` → `READY`. Ustawia `readyAt`.

**Body (opcjonalne):**
```json
{}
```

**Odpowiedź `200 OK`:** obiekt `KdsTicket`.

---

#### POST `/staff/kds/tickets/:ticketId/handoff`

Wydaj zamówienie (przekaż do ekspedycji/kuriera). `READY` → `HANDED_OFF`.

**Odpowiedź `200 OK`:** obiekt `KdsTicket`.

---

#### POST `/staff/kds/tickets/:ticketId/cancel`

Anuluj ticket. `NEW|ACKED|IN_PROGRESS` → `CANCELLED`.

**Body (wymagane):**
```json
{ "reason": "Klient zrezygnował" }
```

> Pole `reason` jest **obowiązkowe**, min 1, max 200 znaków.

**Odpowiedź `200 OK`:** obiekt `KdsTicket`.

---

### 4.3. Komendy pozycji (staff)

#### POST `/staff/kds/items/:itemId/start`

Rozpocznij gotowanie pozycji. `QUEUED` → `COOKING`. Ustawia `firedAt`.

**Body (opcjonalne):** `{}`

**Odpowiedź `200 OK`:** obiekt `KdsTicketItem`.

---

#### POST `/staff/kds/items/:itemId/ready`

Oznacz pozycję jako gotową. `QUEUED|COOKING` → `READY`. Ustawia `doneAt`.

**Body (opcjonalne):** `{}`

**Odpowiedź `200 OK`:** obiekt `KdsTicketItem`.

---

### 4.4. Zarządzanie stacjami (tylko admin)

#### GET `/admin/kds/stations`

Lista wszystkich stacji (aktywnych i nieaktywnych).

#### POST `/admin/kds/stations`

Utwórz nową stację.

**Body:**
```json
{
   "code": "SUSHI",
   "name": "Sushi bar",
   "displayOrder": 3
}
```

Walidacja `code`: tylko litery A-Z, cyfry, podkreślnik (`_`). Automatycznie uppercase.

**Odpowiedź `201 Created`:** obiekt `KdsStation`.

#### PUT `/admin/kds/stations/:stationId`

Aktualizuj stację (wszystkie pola opcjonalne).

```json
{
   "name": "Nowa nazwa",
   "isActive": false,
   "displayOrder": 5
}
```

**Odpowiedź `200 OK`:** zaktualizowany obiekt `KdsStation`.

#### DELETE `/admin/kds/stations/:stationId`

Usuń stację.

**Odpowiedź `204 No Content`.** Jeśli nie znaleziona: `404`.

---

### 4.5. Kody odpowiedzi HTTP

| Kod | Znaczenie |
|---|---|
| `200` | Sukces (GET / komendy) |
| `201` | Zasób utworzony (POST tworzenie) |
| `204` | Sukces bez ciała (DELETE) |
| `400` | Błąd walidacji (np. brak `reason` w cancel) |
| `401` | Brak/nieprawidłowy JWT |
| `403` | Niewystarczające uprawnienia (rola) |
| `404` | Ticket/pozycja/stacja nie znaleziona |
| `409` | Niedozwolona zmiana stanu (np. cancel READY ticketu) |

**Format błędu:**
```json
{
   "message": "Niedozwolona zmiana stanu: READY -> CANCELLED",
   "codeStatus": "invalid_transition"
}
```

---

## 5. Socket.IO — integracja real-time

### 5.1. Połączenie

```
wss://{tenant-domain}/staff
```

**Przykład konfiguracji (pseudokod Android/Kotlin):**
```kotlin
val socket = IO.socket("https://api.tenantdomain.pl", IO.Options().apply {
   path = "/socket.io"
   transports = arrayOf("websocket")
   auth = mapOf("token" to jwtToken)
})
socket.connect()
```

Po połączeniu backend weryfikuje JWT, wyciąga `tenantKey` i automatycznie dodaje socket do roomu `tenant:{tenantKey}`.

### 5.2. Namespace

```
/staff
```

Uprawnione role: `STAFF`, `COURIER`, `ADMIN`, `TEST`.

### 5.3. Eventy KDS

Wszystkie eventy KDS emitowane są do roomu `tenant:{tenantKey}` w namespace `/staff`.

#### `KDS_TICKET_CREATED`

Nowy ticket pojawił się w kuchni. Emitowany automatycznie gdy nowe zamówienie zostanie przetworzone przez watcher.

**Payload:**
```json
{
   "ticketId": "6617a3f2e4b0c1234567890a",
   "orderId": "6617a3f1e4b0c1234567890b",
   "orderNumber": "order-42",
   "state": "NEW",
   "itemCount": 3,
   "slaTargetAt": "2026-04-04T12:30:00.000Z"
}
```

> Po odebraniu tego eventu, Android powinien `GET /staff/kds/tickets/{ticketId}` aby pobrać pełne dane (ticket + items).

---

#### `KDS_TICKET_ACKED`

Ticket potwierdzony przez kucharza.

**Payload:**
```json
{
   "ticketId": "6617a3f2e4b0c1234567890a",
   "state": "ACKED",
   "actorId": "user123"
}
```

---

#### `KDS_TICKET_STARTED`

Przygotowanie rozpoczęte.

**Payload:**
```json
{
   "ticketId": "6617a3f2e4b0c1234567890a",
   "state": "IN_PROGRESS",
   "startedAt": "2026-04-04T12:17:00.000Z",
   "actorId": "user123"
}
```

---

#### `KDS_TICKET_READY`

Ticket gotowy do wydania.

**Payload:**
```json
{
   "ticketId": "6617a3f2e4b0c1234567890a",
   "state": "READY",
   "readyAt": "2026-04-04T12:25:00.000Z",
   "actorId": "user123"
}
```

---

#### `KDS_TICKET_HANDOFF`

Zamówienie wydane.

**Payload:**
```json
{
   "ticketId": "6617a3f2e4b0c1234567890a",
   "state": "HANDED_OFF",
   "handedOffAt": "2026-04-04T12:26:00.000Z",
   "actorId": "user123"
}
```

---

#### `KDS_TICKET_CANCEL`

Ticket anulowany.

**Payload:**
```json
{
   "ticketId": "6617a3f2e4b0c1234567890a",
   "state": "CANCELLED",
   "reason": "Klient zrezygnował",
   "actorId": "user123"
}
```

---

#### `KDS_ITEM_STARTED`

Pozycja w trakcie gotowania.

**Payload:**
```json
{
   "itemId": "6617a3f3e4b0c1234567890c",
   "ticketId": "6617a3f2e4b0c1234567890a",
   "state": "COOKING",
   "firedAt": "2026-04-04T12:17:30.000Z",
   "actorId": "user123"
}
```

---

#### `KDS_ITEM_READY`

Pozycja gotowa.

**Payload:**
```json
{
   "itemId": "6617a3f3e4b0c1234567890c",
   "ticketId": "6617a3f2e4b0c1234567890a",
   "state": "READY",
   "doneAt": "2026-04-04T12:24:00.000Z",
   "actorId": "user123"
}
```

---

### 5.4. Strategia synchronizacji Android

**Startup (pierwsze otwarcie aplikacji / po reconnect):**
1. Pobierz `GET /staff/kds/tickets?state=NEW,ACKED,IN_PROGRESS,READY&limit=100` → wyświetl aktualną ścianę ticketów
2. Połącz Socket.IO `/staff`
3. Od tego momentu aktualizuj UI na podstawie przychodzących eventów

**Obsługa eventu `KDS_TICKET_CREATED`:**
- Pobierz `GET /staff/kds/tickets/{ticketId}` z pełnymi pozycjami
- Dodaj kartę do widoku

**Obsługa pozostałych eventów:**
- Zaktualizuj stan ticketu/pozycji lokalnie w pamięci na podstawie danych z payloadu
- Nie potrzeba dodatkowego HTTP call

**Reconnect po utracie połączenia:**
- Przy reconnect Socket.IO ponownie pobierz aktualny stan z HTTP
- Wtedy znowu zasubskrybuj eventy

---

## 6. Nagłówek Idempotency-Key

Każda komenda POST powinna wysyłać nagłówek `Idempotency-Key`:

```
Idempotency-Key: <uuid-v4>
```

**Po co:**
- Chroni przed podwójnym wykonaniem tej samej komendy (np. przy retry po timeout)
- Backend porównuje klucz w kolekcji audytu i jeśli taka sama akcja już się wykonała, zwraca `200 OK` bez effectów ubocznych

**Jak używać w Android:**
1. Przy tworzeniu komendy (np. user kliknął "START") wygeneruj UUID v4
2. Wyślij żądanie z nagłówkiem `Idempotency-Key: {uuid}`
3. Jeśli dostaniesz timeout → poczekaj chwilę i wyślij żądanie ponownie z **tym samym** UUID
4. Jeśli dostaniesz `200` → sukces, nie wysyłaj ponownie

---

## 7. Pełny workflow z przykładami HTTP

### Scenariusz: kucharz obsługuje zamówienie od początku do końca

```
1. [Socket.IO] ← KDS_TICKET_CREATED { ticketId: "abc123", orderNumber: "order-42", itemCount: 2 }

2. [HTTP GET] /api/staff/kds/tickets/abc123
   ← { ticket: { state: "NEW", ... }, items: [...] }

3. [HTTP POST] /api/staff/kds/tickets/abc123/ack
   Header: Idempotency-Key: 550e8400-e29b-41d4-a716-446655440001
   Body: {}
   ← { state: "ACKED", ... }
   [Socket.IO] → KDS_TICKET_ACKED broadcast do innych tabletów

4. [HTTP POST] /api/staff/kds/tickets/abc123/start
   Header: Idempotency-Key: 550e8400-e29b-41d4-a716-446655440002
   ← { state: "IN_PROGRESS", startedAt: "..." }
   [Socket.IO] → KDS_TICKET_STARTED

5. [HTTP POST] /api/staff/kds/items/{item1Id}/start
   Header: Idempotency-Key: 550e8400-e29b-41d4-a716-446655440003
   ← { state: "COOKING", firedAt: "..." }
   [Socket.IO] → KDS_ITEM_STARTED

6. [HTTP POST] /api/staff/kds/items/{item1Id}/ready
   Header: Idempotency-Key: 550e8400-e29b-41d4-a716-446655440004
   ← { state: "READY", doneAt: "..." }
   [Socket.IO] → KDS_ITEM_READY

7. [HTTP POST] /api/staff/kds/tickets/abc123/ready
   Header: Idempotency-Key: 550e8400-e29b-41d4-a716-446655440005
   ← { state: "READY", readyAt: "..." }
   [Socket.IO] → KDS_TICKET_READY

8. [HTTP POST] /api/staff/kds/tickets/abc123/handoff
   Header: Idempotency-Key: 550e8400-e29b-41d4-a716-446655440006
   ← { state: "HANDED_OFF", handedOffAt: "..." }
   [Socket.IO] → KDS_TICKET_HANDOFF
```

---

## 8. Obliczanie SLA i kolorowanie kart

Pole `slaTargetAt` to deadline dla danego ticketu (domyślnie `createdAt + 15 minut`).

**Logika kolorowania w Android:**

```
teraz < slaTargetAt - 2 min  → kolor: zielony (OK)
teraz < slaTargetAt          → kolor: żółty (ostrzeżenie)
teraz >= slaTargetAt         → kolor: czerwony (przekroczono SLA)
state == CANCELLED           → kolor: szary
state == HANDED_OFF          → kolor: niebieski
```

Czas przygotowania (czas_oczekiwania):
```
startedAt - createdAt  → czas oczekiwania na start
readyAt - startedAt    → czas gotowania
readyAt - createdAt    → całkowity czas cyklu
```

---

## 9. Obsługa błędów i edge cases

### 9.1. Ticket już w docelowym stanie (idempotencja)

```
POST /api/staff/kds/tickets/abc123/ack   (ticket już jest ACKED)
← 200 OK  { state: "ACKED", ... }
```
Backend nie zwraca błędu — zwraca aktualny stan. Bezpieczne.

### 9.2. Niedozwolone przejście stanu

```
POST /api/staff/kds/tickets/abc123/cancel   (ticket jest w stanie HANDED_OFF)
← 409 Conflict
  { "message": "Niedozwolona zmiana stanu: HANDED_OFF -> CANCELLED", "codeStatus": "invalid_transition" }
```

Android powinien wyświetlić użytkownikowi komunikat i odświeżyć ticket.

### 9.3. Ticket nie istnieje

```
POST /api/staff/kds/tickets/nie-istnieje/ack
← 404 Not Found
  { "message": "Ticket KDS nie znaleziony", "codeStatus": "not_found" }
```

### 9.4. Nieprawidłowy format ID

```
GET /api/staff/kds/tickets/invalid-id
← 200 OK  { data: null }   (findById zwraca null dla niepoprawnego ObjectId)
```

### 9.5. Utrata połączenia Socket.IO

Socket.IO 4.x automatycznie próbuje się ponownie połączyć. Na czas rozłączenia Android powinien:
1. Pokazać wskaźnik "offline"
2. Po reconnect: odświeżyć HTTP dane i ponownie zbudować widok
3. Nie blokować lokalnych akcji — kolejkować je i wysłać po reconnect (opcjonalnie)

---

## 10. Schemat przepływu danych (diagram)

```
┌─────────────────────────────────────────────────────────────┐
│                    Android Tablet KDS                        │
│                                                              │
│  ┌──────────────────┐         ┌─────────────────────────┐   │
│  │   UI / Widok      │ ←update │  Stan lokalny (in-mem)  │   │
│  │   kart ticketów  │         │  Map<ticketId, Ticket>  │   │
│  └────────┬─────────┘         └──────────┬──────────────┘   │
│           │ akcja kucharza               │ aktualizacja       │
│           ↓                             ↑                    │
│  ┌──────────────────┐         ┌──────────────────────────┐  │
│  │  HTTP Client     │         │  Socket.IO Client         │  │
│  │  (REST commands) │         │  (real-time events)       │  │
│  └────────┬─────────┘         └──────────┬───────────────┘  │
└───────────┼──────────────────────────────┼──────────────────┘
            │ HTTPS                        │ WSS /staff
            ↓                             ↓
┌─────────────────────────────────────────────────────────────┐
│                     API Backend                              │
│                                                              │
│  POST /staff/kds/tickets/:id/start                          │
│  → KdsController → KdsService → KdsRepository               │
│  → MongoDB update (state: IN_PROGRESS, startedAt: now)       │
│  → KdsAudit.create()                                        │
│  → notifyKdsEvent(KDS_TICKET_STARTED)                       │
│  → Socket.IO emit to room tenant:{key}                       │
└─────────────────────────────────────────────────────────────┘
```

---

## 11. Obsługa wielu tabletów jednocześnie

System obsługuje wiele tabletów działających jednocześnie (np. tablet kucharza + tablet ekspedycji).

**Jak działa synchronizacja:**
- Każdy tablet jest podłączony do Socket.IO namespace `/staff`
- Każdy tablet jest w roomie `tenant:{tenantKey}`
- Gdy jeden tablet wykona komendę (np. `start`), backend emituje event do całego roomu
- **Wszystkie tablety w tym samym lokalu** otrzymują ten event i aktualizują swój widok

**Przykład:**
```
Tablet 1 (kucharz):   POST /tickets/abc123/ready
Backend:              → state = READY
                      → emit KDS_TICKET_READY do room "tenant:cafe123"
Tablet 2 (ekspedycja): ← odbiera KDS_TICKET_READY, pokazuje alert "Zamówienie gotowe!"
```

---

## 12. Podsumowanie — szybki start dla dewelopera Android

### Minimum viable integration (kroki)

1. **Zaloguj się** → pobierz JWT token
2. **Pobierz aktywne tickety** → `GET /api/staff/kds/tickets?state=NEW,ACKED,IN_PROGRESS,READY`
3. **Połącz Socket.IO** z namespace `/staff`, auth: `{ token: jwt }`
4. **Nasłuchuj**:
   - `KDS_TICKET_CREATED` → fetch ticket + items, dodaj do widoku
   - `KDS_TICKET_*`, `KDS_ITEM_*` → zaktualizuj stan lokalnie
5. **Wysyłaj komendy** odpowiednimi POST-ami z nagłówkiem `Idempotency-Key`

### Zależności przydatne w Android

| Cel | Biblioteka |
|---|---|
| HTTP | Retrofit 2 + OkHttp |
| Socket.IO | `socket.io-client-java:2.1.0` (wspiera Socket.IO 4.x) |
| JSON | Gson / Moshi |
| UUID | `java.util.UUID.randomUUID().toString()` |

> Socket.IO 4.x wymaga klienta w wersji **2.x** dla Javy/Kotlina. Wersja 1.x klienta nie jest kompatybilna.

---

*Dokument opisuje stan implementacji po Etap 1. Kolejne etapy (namespace /kds, SLA alerting) będą opisane w osobnych dokumentach.*

---

## 13. Zmiany — kwiecień 2026 (breaking changes)

> Poniższe zmiany dotyczą modelu `KdsTicketItem` oraz logiki tworzenia ticketów.  
> **Wymagana aktualizacja kodu Android** — nowe pola + zmiana logiki renderowania kart.

---

### 13.1. Nowe pola w `KdsTicketItem`

Model `KdsTicketItem` (sekcja 3.2) rozszerzono o trzy nowe pola:

```kotlin
data class KdsTicketItem(
    val _id: String,
    val ticketId: String,
    val orderId: String,
    val productId: String?,
    val sku: String?,
    val posId: String?,

    // NOWE POLA:
    val productName: String?,          // Nazwa produktu (zawsze = product.name)
    val station: String?,              // Stacja KDS: "MAIN" | "KITCHEN" | "SUSHI" | "BAR" | "DESSERT"
    val printer: String?,              // Drukarka: "MAIN" | "KITCHEN" | "SUSHI" | "BAR" | "DESSERT" | null
    val productions: List<KdsProductionTask>?,  // Sekcje produkcyjne, null jeśli brak

    // ISTNIEJĄCE POLA (bez zmian):
    val displayName: String,
    val qty: Int,
    val state: String,
    val notes: List<String>,
    val firedAt: String?,
    val doneAt: String?,
    val sequence: Int,
    val createdAt: String,
    val updatedAt: String
)

data class KdsProductionTask(
    val station: String?,   // Stacja dla tej sekcji
    val printer: String?,   // Drukarka dla tej sekcji
    val label: String?,     // Nazwa sekcji (np. "Krewetka w tempurze")
    val qty: Int            // Ilość = zamówiona_ilość × task.quantity
)
```

---

### 13.2. Zmiana logiki: 1 item per produkt (zamiast N itemów per sekcja)

**Poprzednie zachowanie (stare — już nie obowiązuje):**  
Produkt z `production[]` (np. 2 sekcje) generował **2 oddzielne `KdsTicketItem`** — jeden per sekcja. Każdy miał inny `displayName` (z `task.label`) i inne `station`.

**Nowe zachowanie:**  
Każdy produkt generuje **zawsze dokładnie 1 `KdsTicketItem`**, niezależnie od liczby sekcji produkcyjnych.
- `displayName` = nazwa produktu (zawsze, nigdy nie jest zastępowany przez `task.label`)
- `qty` = zamówiona ilość produktu
- `station` = główna stacja produktu (z `product.station` lub pierwszego taska)
- `productions[]` = lista wszystkich sekcji z wyliczonymi ilościami

**Przykład — zamówienie: Hosomaki Tamago ×2** (produkt ma 2 sekcje produkcyjne):

```json
// STARY format (już nie zwracany) — 2 itemy:
[
  { "displayName": "Krewetka w tempurze", "qty": 4, "station": "KITCHEN" },
  { "displayName": "Łosoś pieczony",      "qty": 12, "station": "KITCHEN" }
]

// NOWY format — 1 item z productions[]:
[
  {
    "displayName": "Hosomaki Tamago",
    "productName": "Hosomaki Tamago",
    "qty": 2,
    "station": "KITCHEN",
    "printer": "KITCHEN",
    "productions": [
      { "label": "Krewetka w tempurze", "station": "KITCHEN", "printer": "KITCHEN", "qty": 4  },
      { "label": "Łosoś pieczony",      "station": "KITCHEN", "printer": "KITCHEN", "qty": 12 }
    ]
  }
]
```

---

### 13.3. Stacja jako filtr po stronie aplikacji (nie parametr URL)

**Poprzednie podejście:** przekazywano `?station=KITCHEN` jako query param, serwer filtrował items.

**Nowe rekomendowane podejście:** pobieraj wszystkie items bez `?station`, filtruj lokalnie po `item.station`.

```kotlin
val myStation = "KITCHEN" // ustawienie tabletu zapisane lokalnie

// Filtrowanie po stronie Android:
val visibleItems = ticket.items.filter { it.station == myStation }
// Dla tabletu MAIN: brak filtrowania, pokaż wszystkie items
```

> Parametr `?station=SUSHI` nadal działa serwerowo (nie usunięty) — ale filtrowanie po stronie aplikacji jest prostsze i nie wymaga dodatkowych requestów.

---

### 13.4. Nowe pole `station` i `printer` na tickecie (`stations[]`)

`KdsTicket` ma teraz zaktualizowane pole `stations[]` — zawiera **wszystkie stacje** ze wszystkich items włącznie z sekcjami z `productions[]`.

```json
{
  "hasMultipleStations": true,
  "stationCount": 2,
  "stations": ["MAIN", "KITCHEN"]
}
```

Tablet może używać `ticket.stations` do szybkiego sprawdzenia czy dotyczy go to zamówienie, przed przefiltrowaniem items.

---

### 13.5. Zaktualizowana logika renderowania karty produktu

| Tryb tabletu | Logika |
|---|---|
| **Tablet MAIN** | Pokaż wszystkie items. Nagłówek karty = `item.displayName` + `item.qty`. Poniżej sekcje z `item.productions[]` (label + qty). |
| **Tablet KITCHEN / SUSHI / BAR** | Filtruj items: `item.station === myStation`. Nagłówek = `item.displayName` + `item.qty`. Sekcje z `productions[]` jako szczegóły (opcjonalnie). |
| **Drukarka etykiet** | Filtruj items: `item.printer === myPrinter`. Drukuj `displayName` + sekcje z `productions[]`. |

**Przykład renderowania dla tabletu KITCHEN** (zamówienie z sekcji 13.2):

```
┌─ Hosomaki Tamago ×2 ───────────────────┐
│  QUEUED                                │
│  ─────────────────────────────────     │
│  Krewetka w tempurze  ×4               │
│  Łosoś pieczony       ×12              │
│                                        │
│  [START]                               │
└────────────────────────────────────────┘
```

**Przykład renderowania dla tabletu MAIN** (to samo zamówienie + Hosomaki Awokado):

```
┌─ Hosomaki Awokado ×1 ──────────────────┐
│  QUEUED   •  MAIN                      │
│  (brak sekcji)                         │
│  [START]                               │
└────────────────────────────────────────┘

┌─ Hosomaki Tamago ×2 ───────────────────┐
│  QUEUED   •  KITCHEN                   │
│  ─────────────────────────────────     │
│  Krewetka w tempurze  ×4               │
│  Łosoś pieczony       ×12              │
│  [START]                               │
└────────────────────────────────────────┘
```

---

### 13.6. Podsumowanie wymaganych zmian w Android

| Co zmienić | Priorytet |
|---|---|
| Zaktualizować model `KdsTicketItem` — dodać `productName`, `station`, `printer`, `productions` | **Wymagane** |
| Dodać model `KdsProductionTask` | **Wymagane** |
| Zmienić renderowanie karty — jeden item = jeden produkt, sekcje z `productions[]` | **Wymagane** |
| Dodać lokalne filtrowanie po `item.station` (ustawienie tabletu) | **Wymagane** |
| Usunąć wysyłanie `?station=...` w query params (lub zostawić jako opcję) | Opcjonalne |
| Zaktualizować `kotlin` data class `KdsTicket` — obsłużyć `stations: List<String>` | Zalecane |
