# KDS — Dzienny numer ticketu (`kdsTicketNumber`)

## Problem

Aktualnie ticket KDS zawiera pole `orderNumber` (np. `KR-24`) — jest to zewnętrzny numer zamówienia z systemu e-commerce. Na wyświetlaczu kuchennym (KDS) jest nieczytelny:

- kucharz widzi `KR-24`, `KR-25`, `KR-513` — numery bez kontekstu
- numery nie resetują się każdego dnia
- kucharz nie wie od razu czy to wynos, dostawa czy na miejscu

## Propozycja

Dodać nowe pole `kdsTicketNumber` — czytelny identyfikator z prefiksem typu zamówienia i **jednym wspólnym** licznikiem dziennym resetowanym o północy.

---

## Format

```
{PREFIX}-{NNN}
```

| Typ zamówienia | Prefix | Przykład |
|----------------|--------|----------|
| `PICKUP`       | `W`    | `W-001`  |
| `DELIVERY`     | `D`    | `D-002`  |
| `DINE_IN`      | `M`    | `M-003`  |
| nieznany       | `X`    | `X-004`  |

- Numer jest **trójcyfrowy z zerowaniem** (`001`, `002`, ... `999`)
- Licznik jest **jeden wspólny dla wszystkich typów** w danym dniu
- Prefix tylko informuje o typie zamówienia — numer jest globalny
- Reset o północy (strefa czasu tenanta)

### Przykład kolejności w ciągu dnia

```
Zamówienie 1 (PICKUP)   → W-001
Zamówienie 2 (DELIVERY) → D-002
Zamówienie 3 (PICKUP)   → W-003
Zamówienie 4 (DINE_IN)  → M-004
Zamówienie 5 (DELIVERY) → D-005
```

---

## Specyfikacja pola

| Pole              | Typ      | Opis |
|-------------------|----------|------|
| `kdsTicketNumber` | `String` | Czytelny numer KDS z prefiksem typu. Np. `W-001`, `D-002`, `M-003`. Jeden wspólny licznik dzienny. Reset o północy. |

### Przykład odpowiedzi API

```json
GET /client/v3/api/staff/kds/tickets/:ticketId

{
  "ticket": {
    "_id": "69d432878b3b7519e95f94e4",
    "orderNumber": "KR-24",
    "kdsTicketNumber": "W-003",
    "orderType": "PICKUP",
    ...
  }
}
```

```json
GET /client/v3/api/staff/kds/tickets?state=NEW

{
  "data": [
    { "kdsTicketNumber": "W-001", "orderType": "PICKUP",   ... },
    { "kdsTicketNumber": "D-002", "orderType": "DELIVERY", ... },
    { "kdsTicketNumber": "W-003", "orderType": "PICKUP",   ... },
    { "kdsTicketNumber": "M-004", "orderType": "DINE_IN",  ... }
  ]
}
```

---

## Logika backendu

```
Prefixes:
  PICKUP   → "W"
  DELIVERY → "D"
  DINE_IN  → "M"
  other    → "X"

Przy tworzeniu ticketu:
  1. Pobierz prefix na podstawie orderType
  2. Pobierz aktualną datę w strefie czasu tenanta (np. Europe/Warsaw)
  3. Atomowo zwiększ JEDEN wspólny licznik dla klucza: kds:{tenantKey}:{YYYY-MM-DD}
     - Redis: INCR → zwraca nowy globalny numer dnia
     - TTL = 48h
  4. kdsTicketNumber = "{prefix}-{numer padded to 3 digits}"
     np. prefix="D", numer=2 → "D-002"
```

---

## Co KDS (Android) wyświetli

```
┌─────────────────────────────┐
│  W-003           🏠 Wynos   │
│  KR-24                      │
│  NEW · 14:23                │
│ ─────────────────────────── │
│  1× Hosomaki Tuna           │
└─────────────────────────────┘
```

- `W-003` — duże, czytelne z odległości 3m, prefix mówi od razu "wynos"
- `KR-24` — mały podpis, do weryfikacji z kasą

---

## Priorytety

- **Wymagane:** `kdsTicketNumber` w `GET /tickets` (lista) oraz `GET /tickets/:id`
- **Wymagane:** `kdsTicketNumber` w socket event `KDS_TICKET_CREATED`
- **Opcjonalne:** w pozostałych socket eventach

---

## Fallback (do czasu wdrożenia API)

KDS wyświetla `orderNumber` (`KR-24`) jak dotychczas.
Po dodaniu pola przez API — automatycznie przełącza się na `W-003` bez żadnej dodatkowej zmiany kodu.
