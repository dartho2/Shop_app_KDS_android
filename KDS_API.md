# KDS API — Dokumentacja dla aplikacji mobilnej

> Base URL: `http://<host>/api`  
> Autoryzacja: `Authorization: Bearer <token>` (JWT staff/admin)  
> Wszystkie daty: ISO 8601 UTC (`2026-04-10T12:00:00.000Z`)

---

## Spis treści

1. [Modele danych](#1-modele-danych)
2. [Enumy](#2-enumy)
3. [Socket.IO — eventy real-time](#3-socketio--eventy-real-time)
4. [Endpointy — Tickety (odczyt)](#4-endpointy--tickety-odczyt)
5. [Endpointy — Komendy na tickecie](#5-endpointy--komendy-na-tickecie)
6. [Endpointy — Komendy na pozycji (item)](#6-endpointy--komendy-na-pozycji-item)
7. [Endpointy — Stacje (odczyt)](#7-endpointy--stacje-odczyt)
8. [Endpointy — Stacje (admin CRUD)](#8-endpointy--stacje-admin-crud)
9. [Endpointy — Audit log](#9-endpointy--audit-log)
10. [Obsługa błędów](#10-obsługa-błędów)
11. [Przepływ stanów](#11-przepływ-stanów)
12. [Przykładowe sesje](#12-przykładowe-sesje)

---

## 1. Modele danych

### 1.1 KdsTicket

Główny bilet produkcyjny — tworzony automatycznie po złożeniu zamówienia.

```ts
{
  _id: string;                    // ObjectId (string)
  orderId: string;                // ID zamówienia
  orderNumber: string;            // Numer zamówienia (czytelny)
  kdsTicketNumber?: string;       // Dzienny numer KDS np. "12" lub "D-07"
  source?: string;                // Źródło: "checkout" | "pos" | np. "wolt"
  priority: "normal" | "rush";   // Priorytet
  state: KdsTicketState;          // Aktualny stan (enum poniżej)
  note?: string;                  // Notatka do zamówienia
  startedAt?: string;             // ISO — kiedy kuchnia zaczęła
  readyAt?: string;               // ISO — kiedy gotowe
  handedOffAt?: string;           // ISO — kiedy wydane
  cancelledAt?: string;           // ISO — kiedy anulowane
  slaTargetAt?: string;           // ISO — deadline SLA (domyślnie +15 min)
  scheduledFor?: string;          // ISO — czas odbioru (null = ASAP)
  orderType?: string;             // Typ dostawy: "delivery" | "pickup" | ...
  hasMultipleStations: boolean;   // Czy pozycje są na różnych stacjach KDS
  stationCount: number;           // Liczba różnych stacji
  stations: StationEnum[];        // Lista stacji w tym tickecie
  createdAt: string;              // ISO
  updatedAt: string;              // ISO

  // Pole wirtualne — dołączane przez listTickets i getTicket:
  items: KdsTicketItem[];
}
```

**TTL:** Tickety w stanie `HANDED_OFF` lub `CANCELLED` są automatycznie usuwane po **30 dniach**.

---

### 1.2 KdsTicketItem

Pojedyncza pozycja produkcyjna w ramach ticketu. Jeden produkt może generować wiele pozycji (gdy ma `production[]` z wieloma taskami).

```ts
{
  _id: string;                    // ObjectId (string)
  ticketId: string;               // ObjectId — ref do KdsTicket
  orderId: string;                // ID zamówienia (denorm.)
  productId?: string;             // ID produktu (z katalogu)
  sku?: string;                   // SKU produktu
  posId?: string;                 // POS ID produktu
  displayName: string;            // Nazwa do wyświetlenia na ekranie KDS
                                  //   = task.label ?? product.name
  qty: number;                    // Ilość do wyprodukowania
                                  //   = zamówiona_ilość × task.quantity
  station: StationEnum;           // Stacja KDS (domyślnie MAIN) — który tablet WYŚWIETLA pozycję
  stationId?: string;             // Opcjonalnie: ObjectId stacji z KdsStation
  printer?: PrinterEnum | null;   // Drukarka fizyczna — która drukarka DRUKUJE label
                                  //   niezależna od station! (np. station=MAIN, printer=KITCHEN)
  state: KdsItemState;            // Stan pozycji (enum poniżej)
  notes: string[];                // Tablice notatek do pozycji
  firedAt?: string;               // ISO — kiedy kuchnia zaczęła tę pozycję
  doneAt?: string;                // ISO — kiedy pozycja gotowa
  sequence: number;               // Kolejność wyświetlania (rosnąco)
  createdAt: string;              // ISO
  updatedAt: string;              // ISO
}
```

**TTL:** Pozycje w stanie `SERVED` lub `VOID` są automatycznie usuwane po **30 dniach**.

---

### 1.3 KdsStation

Konfigurowalny ekran/tablet KDS. Definiowany przez admina w panelu.

```ts
{
  _id: string;           // ObjectId (string)
  code: string;          // Unikalny kod np. "SUSHI", "KITCHEN" (uppercase)
  name: string;          // Nazwa wyświetlana np. "Sushi bar"
  isActive: boolean;     // Czy stacja jest aktywna
  displayOrder: number;  // Kolejność na liście
  createdAt: string;     // ISO
  updatedAt: string;     // ISO
}
```

---

### 1.4 KdsAudit

Historia zmian stanów ticketu.

```ts
{
  _id: string;               // ObjectId (string)
  ticketId: string;          // ObjectId — ref do KdsTicket
  orderId: string;           // ID zamówienia
  actionType: KdsAuditAction; // Typ akcji (enum poniżej)
  actorId?: string;          // ID użytkownika który wykonał akcję
  actorRole?: string;        // Rola użytkownika
  before?: object;           // Stan przed zmianą np. { state: "NEW" }
  after?: object;            // Stan po zmianie np. { state: "ACKED" }
  idempotencyKey?: string;   // Klucz idempotencji żądania
  createdAt: string;         // ISO
}
```

**TTL:** Rekordy audytu są automatycznie usuwane po **5 dniach**.

---

## 2. Enumy

### KdsTicketState

```
NEW          → Nowy ticket, czeka na potwierdzenie
ACKED        → Potwierdzony przez kuchnię
IN_PROGRESS  → W trakcie produkcji
READY        → Gotowe do wydania
HANDED_OFF   → Wydane klientowi (terminal)
CANCELLED    → Anulowane
```

**Stan wirtualny ACTIVE** (używany w filtrze `?state=ACTIVE`):
= `NEW` + `ACKED` + `IN_PROGRESS`

---

### KdsItemState

```
QUEUED   → Czeka w kolejce
COOKING  → W trakcie produkcji
READY    → Gotowe
SERVED   → Wydane (terminal)
VOID     → Unieważnione
```

---

### StationEnum

```
MAIN      → Tablet główny (domyślny fallback)
KITCHEN   → Kuchnia
SUSHI     → Sushi bar
BAR       → Bar
DESSERT   → Desery
```

---

### PrinterEnum

```
MAIN      → Drukarka główna (tablet MAIN)
KITCHEN   → Drukarka kuchenna
SUSHI     → Drukarka sushi
BAR       → Drukarka bar
DESSERT   → Drukarka deserów
```

> `printer` na `KdsTicketItem` jest kopiowane z `production[].printer` lub `product.printer`. Jeśli nie ustawione — `null` (brak drukowania lub drukuj domyślnie).

---

### KdsAuditAction

```
TICKET_CREATED
TICKET_ACKED
TICKET_STARTED
TICKET_READY
TICKET_HANDOFF
TICKET_CANCEL
ITEM_STARTED
ITEM_READY
```

---

## 3. Socket.IO — eventy real-time

Aplikacja mobilna powinna nasłuchiwać tych eventów zamiast pollować API.

### Połączenie

```
ws://<host>
Auth: przekaż token w handshake lub nagłówku
```

### Eventy przychodzące (nasłuchuj)

| Event | Kiedy emitowany | Payload |
|---|---|---|
| `kds:ticket:created` | Nowe zamówienie → nowy ticket | `{ ticketId, orderId, orderNumber, kdsTicketNumber, state, itemCount, slaTargetAt, scheduledFor, orderType }` |
| `kds:ticket:acked` | Ticket potwierdzony | `{ ticketId, state, actorId }` |
| `kds:ticket:started` | Kuchnia zaczęła | `{ ticketId, state, startedAt, actorId }` |
| `kds:ticket:ready` | Gotowe | `{ ticketId, state, readyAt, actorId }` |
| `kds:ticket:handoff` | Wydane | `{ ticketId, state, handedOffAt, actorId }` |
| `kds:ticket:cancel` | Anulowane | `{ ticketId, state, reason, actorId }` |
| `kds:item:started` | Pozycja zaczęta | `{ itemId, ticketId, state, firedAt, actorId }` |
| `kds:item:ready` | Pozycja gotowa | `{ itemId, ticketId, state, doneAt, actorId }` |

> Po otrzymaniu eventu pobierz świeże dane przez `GET /staff/kds/tickets/:ticketId` lub zaktualizuj lokalny stan.

---

## 4. Endpointy — Tickety (odczyt)

### `GET /api/staff/kds/tickets`

Lista ticketów z pozycjami (items). Wymaga uprawnienia `kds.index`.

**Query params:**

| Param | Typ | Opis | Przykład |
|---|---|---|---|
| `state` | string | Stan lub `ACTIVE` | `?state=ACTIVE` |
| `station` | string | Filtruj items po stacji (która wyświetla) | `?station=SUSHI` |
| `printer` | string | Filtruj items po drukarce (która drukuje) | `?printer=KITCHEN` |
| `from` | ISO date | Tickety od daty | `?from=2026-04-10T00:00:00Z` |
| `to` | ISO date | Tickety do daty | `?to=2026-04-10T23:59:59Z` |
| `priority` | string | `normal` lub `rush` | `?priority=rush` |
| `scheduledOnly` | boolean | Tylko z `scheduledFor` | `?scheduledOnly=true` |
| `limit` | number | Max wyników (max 500) | `?limit=50` |
| `skip` | number | Paginacja | `?skip=0` |

**Odpowiedź `200`:**

```json
{
  "data": [
    {
      "_id": "664e1a2b3f4c5d6e7f8a9b0c",
      "orderId": "664e1a2b3f4c5d6e7f8a9b01",
      "orderNumber": "ORD-1042",
      "kdsTicketNumber": "42",
      "source": "checkout",
      "priority": "normal",
      "state": "NEW",
      "note": "bez cebuli",
      "slaTargetAt": "2026-04-10T12:15:00.000Z",
      "scheduledFor": null,
      "orderType": "pickup",
      "hasMultipleStations": true,
      "stationCount": 2,
      "stations": ["SUSHI", "KITCHEN"],
      "createdAt": "2026-04-10T12:00:00.000Z",
      "updatedAt": "2026-04-10T12:00:00.000Z",
      "items": [
        {
          "_id": "664e1a2b3f4c5d6e7f8a9b10",
          "ticketId": "664e1a2b3f4c5d6e7f8a9b0c",
          "orderId": "664e1a2b3f4c5d6e7f8a9b01",
          "productId": "663abc...",
          "sku": "205",
          "posId": null,
          "displayName": "Hosomaki",
          "qty": 12,
          "station": "SUSHI",
          "printer": "KITCHEN",
          "state": "QUEUED",
          "notes": ["bez sezamu"],
          "sequence": 0,
          "createdAt": "2026-04-10T12:00:00.000Z",
          "updatedAt": "2026-04-10T12:00:00.000Z"
        },
        {
          "_id": "664e1a2b3f4c5d6e7f8a9b11",
          "ticketId": "664e1a2b3f4c5d6e7f8a9b0c",
          "orderId": "664e1a2b3f4c5d6e7f8a9b01",
          "displayName": "Miso Soup",
          "qty": 2,
          "station": "KITCHEN",
          "state": "QUEUED",
          "notes": [],
          "sequence": 1,
          "createdAt": "2026-04-10T12:00:00.000Z",
          "updatedAt": "2026-04-10T12:00:00.000Z"
        }
      ]
    }
  ],
  "count": 1
}
```

> **Uwaga filtrowanie po stacji:** `?station=SUSHI` filtruje tylko `items[]` — ticket pojawi się na liście ale będzie zawierał tylko items z danej stacji. Aplikacja mobilna powinna używać tego do wyświetlania ticketów specyficznych dla danego tabletu.

> **Filtrowanie po drukarce:** `?printer=KITCHEN` filtruje items po polu `printer` — niezależnie od `station`. Tablet MAIN z podłączoną drukarką kuchenną powinien używać `?printer=KITCHEN` żeby dostać wszystkie pozycje do wydruku, nawet jeśli ich `station` to `KITCHEN` (inny tablet). To pozwala oddzielić logikę **wyświetlania na ekranie** od **drukowania labela**.

---

### `GET /api/staff/kds/tickets/:ticketId`

Pojedynczy ticket z pozycjami.

**Odpowiedź `200`:**

```json
{
  "ticket": { /* KdsTicket bez items */ },
  "items": [ /* KdsTicketItem[] */ ]
}
```

**Odpowiedź `404`:**
```json
{ "message": "Ticket KDS nie znaleziony" }
```

---

## 5. Endpointy — Komendy na tickecie

Wszystkie komendy wymagają uprawnienia `kds.edit`.

### Nagłówek idempotencji (ważne!)

```
Idempotency-Key: <uuid-v4>
```

Wysyłaj unikalny UUID przy każdej komendzie. Jeśli żądanie zostanie wysłane ponownie z tym samym kluczem, serwer zwróci poprzedni wynik bez duplikowania akcji.

---

### `POST /api/staff/kds/tickets/:ticketId/ack`

Potwierdź ticket (kuchnia go widziała).

**Dozwolone stany:** `NEW` → `ACKED`

**Body:**
```json
{ "note": "opcjonalna notatka" }
```
(ciało może być puste `{}`)

**Odpowiedź `200`:** obiekt `KdsTicket` (bez items) z `state: "ACKED"`

**Błędy:**
```json
// 409 — niedozwolona zmiana stanu
{ "message": "Niedozwolona zmiana stanu: IN_PROGRESS -> ACKED", "codeStatus": "invalid_transition" }

// 404 — nie znaleziono
{ "message": "Ticket KDS nie znaleziony", "codeStatus": "not_found" }
```

---

### `POST /api/staff/kds/tickets/:ticketId/start`

Rozpocznij produkcję.

**Dozwolone stany:** `NEW`, `ACKED` → `IN_PROGRESS`

**Body:** `{}` (opcjonalna notatka)

**Odpowiedź `200`:** `KdsTicket` z `state: "IN_PROGRESS"`, `startedAt: "<ISO>"`

---

### `POST /api/staff/kds/tickets/:ticketId/ready`

Oznacz ticket jako gotowy do wydania.

**Dozwolone stany:** `NEW`, `ACKED`, `IN_PROGRESS` → `READY`

**Body:** `{}` (opcjonalna notatka)

**Odpowiedź `200`:** `KdsTicket` z `state: "READY"`, `readyAt: "<ISO>"`

---

### `POST /api/staff/kds/tickets/:ticketId/handoff`

Wydaj zamówienie klientowi (terminal).

**Dozwolone stany:** `READY` → `HANDED_OFF`

**Body:** `{}` (opcjonalna notatka)

**Odpowiedź `200`:** `KdsTicket` z `state: "HANDED_OFF"`, `handedOffAt: "<ISO>"`

---

### `POST /api/staff/kds/tickets/:ticketId/cancel`

Anuluj ticket.

**Dozwolone stany:** `NEW`, `ACKED`, `IN_PROGRESS` → `CANCELLED`

**Body (wymagane):**
```json
{ "reason": "Błędne zamówienie" }
```

**Odpowiedź `200`:** `KdsTicket` z `state: "CANCELLED"`, `cancelledAt: "<ISO>"`

---

## 6. Endpointy — Komendy na pozycji (item)

Komendy na pojedynczej pozycji. Wymagają `kds.edit`.

### `POST /api/staff/kds/items/:itemId/start`

Rozpocznij produkcję tej pozycji.

**Dozwolone stany:** `QUEUED` → `COOKING`

**Body:** `{}` (opcjonalna notatka)

**Odpowiedź `200`:** `KdsTicketItem` z `state: "COOKING"`, `firedAt: "<ISO>"`

---

### `POST /api/staff/kds/items/:itemId/ready`

Oznacz pozycję jako gotową.

**Dozwolone stany:** `QUEUED`, `COOKING` → `READY`

**Body:** `{}` (opcjonalna notatka)

**Odpowiedź `200`:** `KdsTicketItem` z `state: "READY"`, `doneAt: "<ISO>"`

---

## 7. Endpointy — Stacje (odczyt)

### `GET /api/staff/kds/stations`

Lista aktywnych stacji KDS (tablet pobiera swoją listę).

**Query params:**

| Param | Typ | Opis |
|---|---|---|
| `all` | `"true"` | Zwróć też nieaktywne stacje |

**Odpowiedź `200`:**

```json
{
  "data": [
    {
      "_id": "664e1a2b3f4c5d6e7f8a0001",
      "code": "SUSHI",
      "name": "Sushi bar",
      "isActive": true,
      "displayOrder": 1,
      "createdAt": "2026-01-01T00:00:00.000Z",
      "updatedAt": "2026-01-01T00:00:00.000Z"
    },
    {
      "_id": "664e1a2b3f4c5d6e7f8a0002",
      "code": "KITCHEN",
      "name": "Kuchnia",
      "isActive": true,
      "displayOrder": 2,
      "createdAt": "2026-01-01T00:00:00.000Z",
      "updatedAt": "2026-01-01T00:00:00.000Z"
    }
  ]
}
```

---

## 8. Endpointy — Stacje (admin CRUD)

Wymagają roli admin (`AuthAdmin`) oraz uprawnienia.

### `GET /api/admin/kds/stations`

Zwraca wszystkie stacje (aktywne i nieaktywne). Wymaga `kds.index`.

Odpowiedź: taka sama jak wyżej.

---

### `POST /api/admin/kds/stations`

Utwórz nową stację KDS. Wymaga `kds.create`.

**Body:**
```json
{
  "code": "BAR",
  "name": "Bar",
  "displayOrder": 3
}
```

| Pole | Typ | Wymagane | Opis |
|---|---|---|---|
| `code` | string | TAK | Unikalny kod, only `[A-Z0-9_]`, max 20 znaków |
| `name` | string | TAK | Nazwa wyświetlana, max 100 znaków |
| `displayOrder` | number | NIE | Kolejność sortowania (domyślnie 0) |

**Odpowiedź `201`:** `KdsStation`

---

### `PUT /api/admin/kds/stations/:stationId`

Edytuj stację. Wymaga `kds.edit`.

**Body (wszystkie pola opcjonalne):**
```json
{
  "name": "Bar — nowa nazwa",
  "isActive": false,
  "displayOrder": 5
}
```

**Odpowiedź `200`:** `KdsStation`

**Odpowiedź `404`:**
```json
{ "message": "Stacja KDS nie znaleziona" }
```

---

### `DELETE /api/admin/kds/stations/:stationId`

Usuń stację. Wymaga `kds.destroy`.

**Odpowiedź `204`:** brak ciała

**Odpowiedź `404`:**
```json
{ "message": "Stacja KDS nie znaleziona" }
```

---

## 9. Endpointy — Audit log

### `GET /api/staff/kds/tickets/:ticketId/audit`

Historia zmian ticketu. Wymaga `kds.index`.

**Query params:**

| Param | Typ | Opis |
|---|---|---|
| `limit` | number | Max wyników (max 200, domyślnie 50) |

**Odpowiedź `200`:**

```json
{
  "data": [
    {
      "_id": "664e1a2b...",
      "ticketId": "664e1a2b3f4c5d6e7f8a9b0c",
      "orderId": "664e1a2b3f4c5d6e7f8a9b01",
      "actionType": "TICKET_STARTED",
      "actorId": "user-123",
      "actorRole": "STAFF",
      "before": { "state": "ACKED" },
      "after": { "state": "IN_PROGRESS" },
      "createdAt": "2026-04-10T12:03:00.000Z"
    },
    {
      "_id": "664e1a2b...",
      "ticketId": "664e1a2b3f4c5d6e7f8a9b0c",
      "orderId": "664e1a2b3f4c5d6e7f8a9b01",
      "actionType": "TICKET_CREATED",
      "actorId": null,
      "actorRole": null,
      "before": null,
      "after": { "state": "NEW", "itemCount": 2 },
      "createdAt": "2026-04-10T12:00:00.000Z"
    }
  ]
}
```

---

## 10. Obsługa błędów

| HTTP | `codeStatus` | Opis |
|---|---|---|
| `400` | `invalid_id` | Nieprawidłowy format ObjectId |
| `400` | `validation_error` | Błąd walidacji body (Zod) |
| `404` | `not_found` | Ticket / item / stacja nie znaleziona |
| `409` | `invalid_transition` | Niedozwolona zmiana stanu |
| `401` | — | Brak/nieważny token |
| `403` | — | Brak uprawnienia |

**Format błędu walidacji:**
```json
{
  "message": "Validation error",
  "errors": [
    { "field": "reason", "message": "Powód jest wymagany" }
  ]
}
```

---

## 11. Przepływ stanów

### Ticket

```
                       ┌─────────────────────────────────────┐
                       ▼                                     │ idempotencja
[Zamówienie złożone] → NEW → ACKED → IN_PROGRESS → READY → HANDED_OFF
                        │       │         │
                        └───────┴─────────┴──→ CANCELLED
```

Szczegółowe reguły:

| Komenda | Dozwolone stany | Następny stan |
|---|---|---|
| `ack` | `NEW` | `ACKED` |
| `start` | `NEW`, `ACKED` | `IN_PROGRESS` |
| `ready` | `NEW`, `ACKED`, `IN_PROGRESS` | `READY` |
| `handoff` | `READY` | `HANDED_OFF` |
| `cancel` | `NEW`, `ACKED`, `IN_PROGRESS` | `CANCELLED` |

### Item

```
QUEUED → COOKING → READY
  │         │
  └─────────└──→ (brak komendy cofania — VOID tylko systemowo)
```

| Komenda | Dozwolone stany | Następny stan |
|---|---|---|
| `start` | `QUEUED` | `COOKING` |
| `ready` | `QUEUED`, `COOKING` | `READY` |

---

## 12. Przykładowe sesje

### Typowy flow dla tabletu SUSHI

```
1. Startup — pobierz aktywne tickety:
   GET /api/staff/kds/tickets?state=ACTIVE&station=SUSHI&limit=50

2. Podłącz Socket.IO — nasłuchuj kds:ticket:created

3. Nowe zamówienie przychodzi przez socket:
   event: kds:ticket:created { ticketId: "abc123", ... }

4. Pobierz szczegóły:
   GET /api/staff/kds/tickets/abc123
   → zwraca ticket + items[] (tylko items z station=SUSHI jeśli użyjesz ?station=SUSHI)

5. Potwierdź widoczność:
   POST /api/staff/kds/tickets/abc123/ack
   Headers: Idempotency-Key: <uuid>
   Body: {}

6. Zacznij produkcję pozycji:
   POST /api/staff/kds/items/<itemId>/start
   Headers: Idempotency-Key: <uuid>
   Body: {}

7. Pozycja gotowa:
   POST /api/staff/kds/items/<itemId>/ready
   Headers: Idempotency-Key: <uuid>
   Body: {}

8. Cały ticket gotowy:
   POST /api/staff/kds/tickets/abc123/ready
   Headers: Idempotency-Key: <uuid>
   Body: {}

9. Wydanie (np. terminal przy kasie):
   POST /api/staff/kds/tickets/abc123/handoff
   Headers: Idempotency-Key: <uuid>
   Body: {}
```

---

### Multi-stanowiskowy produkt (np. Hosomaki Set)

Zamówiono 2× Hosomaki Set (`production: [{station:SUSHI, qty:6}, {station:KITCHEN, qty:1}]`):

```
Ticket → items:
  [0] displayName: "Hosomaki", station: "SUSHI",   qty: 12, state: QUEUED
  [1] displayName: "Miso Soup", station: "KITCHEN", qty: 2,  state: QUEUED

Tablet SUSHI widzi:  GET ?station=SUSHI  → item [0]
Tablet KITCHEN widzi: GET ?station=KITCHEN → item [1]

Każdy tablet zarządza swoimi items niezależnie.
Ticket dostaje state=READY dopiero gdy ktoś wywoła /ready na tickecie.
```

---

### Inicjalizacja aplikacji mobilnej (startup)

```
1. GET /api/staff/kds/stations
   → Pobierz listę stacji, wybierz/zapamiętaj swoją stację (np. SUSHI)

2. GET /api/staff/kds/tickets?state=ACTIVE&station=SUSHI&limit=100
   → Załaduj bieżące aktywne tickety

3. Podłącz Socket.IO
   → Nasłuchuj na kds:ticket:created i pozostałe eventy
   → Po każdym evencie odśwież lokalny stan lub re-fetch ticketu
```
