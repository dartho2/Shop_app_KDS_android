# Analiza: duplikacja alarmu/powiadomienia dla nowego zamówienia

Data analizy: 2026-02-12
Źródło: `alarm_problem_.md`

## TL;DR
Problem nie wynika z pojedynczego dźwięku odtwarzanego “dwa razy” przez jeden MediaPlayer, tylko z **dwukrotnego uruchomienia mechanizmu alarmu** w krótkim oknie czasu oraz z **późniejszych reminderów**, które ponownie startują alarm. Logi pokazują, że **event nowego zamówienia jest emitowany podwójnie** (socket) i że **`observePendingOrdersQueue()` uruchamia ACTION_START dwa razy** dla tego samego orderId. Następnie po ~1m30s uruchamia się mechanizm “REMINDER”, który ponownie startuje alarm.

Najbardziej prawdopodobna przyczyna: **podwójna subskrypcja na strumień socketów** lub **dwa źródła “nowe zamówienie” (socket + intent/reminder) powodują równoległe wywołanie `startAlarmService()`**, bez blokady per-order.

---

## 1) Gdzie w logach widać podwójne wywołanie

### 1.1. Podwójny event z socketów (ten sam orderId)
```
00:02:16.906  OrdersView...cketEvents  📥 Received order from socket: orderId=698d0a79fdf9812564720b42
00:02:16.908  OrdersView...cketEvents  📥 Received order from socket: orderId=698d0a79fdf9812564720b42
```
oraz później:
```
00:03:47.427  OrdersView...cketEvents  📥 Received order from socket: orderId=698d0a79fdf9812564720b42
00:03:47.430  OrdersView...cketEvents  📥 Received order from socket: orderId=698d0a79fdf9812564720b42
```
**Wniosek:** to wygląda na **podwójne wyzwolenie** zdarzenia “nowe zamówienie” z socketów (ten sam orderId w odstępie 2 ms).

### 1.2. Podwójny start alarmu dla tego samego orderId
Pierwsze uruchomienie:
```
00:02:17.215  OrderAlarmService  ✅ Otwieram dialog + ACTION_START dla: 698d0a79fdf9812564720b42
00:02:17.261  ALARM_DIAG         ✅ Uruchamiam OrderAlarmService (startForegroundService)
00:02:17.268  ALARM_DIAG         ✅ ALARM STARTED SUCCESSFULLY!
```
Drugie uruchomienie w tym samym “oknie”:
```
00:02:17.792  OrderAlarmService  ✅ Otwieram dialog + ACTION_START dla: 698d0a79fdf9812564720b42
00:02:17.824  ALARM_DIAG         ✅ Uruchamiam OrderAlarmService (startForegroundService)
00:02:17.829  ALARM_DIAG         ✅ ALARM STARTED SUCCESSFULLY!
```
To sugeruje, że **`observePendingOrdersQueue()` uruchomiło się ponownie** i ponownie wywołało ACTION_START dla tego samego orderId.

### 1.3. Alarm wyłączany tuż po drugim starcie
```
00:02:18.007  OrderAlarmService: onStartCommand, action=ACTION_START_ALARM, startId=2
00:02:18.017  OrderAlarmService  Alarm już aktywny. Ignoruję nowe ... (startId=2)
00:02:18.031  OrderAlarmService  onDestroy – cleanup.
00:02:18.031  OrderAlarmService  stopAlarmSound: Zatrzymuję i zwalniam MediaPlayer.
```
**Wniosek:** Drugi start powoduje “kolizję” z już aktywnym alarmem, po czym serwis jest niszczony i dźwięk się kończy. W praktyce daje to “start–stop–start”.

### 1.4. Reminder po czasie uruchamia alarm ponownie
```
00:03:47.147  SocketStaff...ntsHandler  🔔 REMINDER ALARM RECEIVED! Order: 698d0a79fdf9812564720b42, Attempt: 1
00:03:47.152  SocketHandler: Uruchamiam OrderAlarmService ...
00:03:47.194  OrderAlarmService  Aktywuję nowy alarm ...
00:03:47.244  OrderAlarmService  Odtwarzanie alarmu (loop) rozpoczęte.
```
To jest **drugi, niezależny** start alarmu (reminder), który powoduje wrażenie “podwójnego alarmu”.

---

## 2) Potencjalne przyczyny

1) **Podwójna subskrypcja socketów** w `observeSocketEvents()` (OrdersViewModel):
- Ten sam `orderId` przychodzi dwa razy z `socketEventsRepo.orders`.
- Może wynikać z wielokrotnego `launchIn(viewModelScope)` przy re-inicjalizacji.

2) **Brak blokady per orderId** przed uruchomieniem ACTION_START w `observePendingOrdersQueue()`:
- Aktualnie brak twardego „guardu” typu `activeAlarmOrderId`/`lastStartOrderId`.

3) **Konflikt dwóch źródeł:**
- Socket -> zapis do DB -> `observePendingOrdersQueue()` startuje alarm.
- Reminder -> bezpośredni start alarmu.

4) **Race condition podczas zmian state** (foreground/background):
- `HomeActivity` jest uruchamiane w tle (intent), co może powodować wielokrotne ponowne wystartowanie `observePendingOrdersQueue()`.

---

## 3) Konkretna propozycja poprawki

### Opcja A (najmniej inwazyjna): Blokada per orderId dla ACTION_START
Dodaj blokadę w `OrdersViewModel`:
- `lastAlarmStartedOrderId: String?`
- w CASE 1 i CASE 2 sprawdź czy `lastAlarmStartedOrderId == next.orderId` i jeśli tak, pomiń.

**Pseudo-fix:**
```kotlin
if (lastAlarmStartedOrderId == next.orderId) {
    Timber.d("⏭️ Pomijam duplicate ACTION_START dla ${next.orderId}")
} else {
    startAlarmService(next, ringOnly = false)
    lastAlarmStartedOrderId = next.orderId
}
```

### Opcja B (stabilniejsza): Debounce / deduplikacja socket eventów
W `observeSocketEvents()` dodaj `distinctUntilChangedBy { it.orderId }` lub własną mapę `lastSeenOrderId + timestamp`, np. ignoruj ten sam `orderId` w oknie 1–2s.

**Pseudo-fix:**
```kotlin
socketEventsRepo.orders
  .filterNot { it.orderId == lastSocketOrderId && System.currentTimeMillis() - lastSocketTs < 2000 }
```

### Opcja C (najlepsza): Jedno źródło prawdy + idempotentny alarm
- Tylko `observePendingOrdersQueue()` uruchamia alarm.
- Reminder nie startuje alarmu, tylko zwiększa flagę “ringOnly” lub “reminderScheduled”.
- AlarmService sprawdza “active alarm for orderId” i ignoruje każde kolejne ACTION_START dla tego samego orderId.

---

## 4) Rekomendowane miejsce do poprawki

**Plik:** `L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\order\OrdersViewModel.kt`
- Funkcje: `observePendingOrdersQueue()`, `observeSocketEvents()`
- Tam dodać deduplikację i blokadę per orderId.

**Plik:** `L:\SHOP APP\app\src\main\java\com\itsorderchat\service\OrderAlarmService.kt`
- Dodać stricte idempotentny guard: jeśli `currentAlarmOrderId == incomingOrderId` → ignore.

---

## 5) Podsumowanie diagnostyki

**Logi potwierdzają:**
- Zdarzenie nowego zamówienia przychodzi dwukrotnie z socketów (duplikacja).
- `observePendingOrdersQueue()` uruchamia ACTION_START **dwa razy** dla tego samego orderId.
- Po ~1–2 min następuje Reminder, który uruchamia alarm ponownie.

**Przyczyna:**
- Brak pełnej deduplikacji eventów i brak blokady per orderId w ACTION_START.

**Rozwiązanie:**
- Deduplikacja socket events + guard per orderId w ACTION_START (najlepiej oba).
- Opcjonalnie: przenieść przypomnienia do jednego kanału, który nie startuje alarmu jeśli już jest aktywny.

---

## 6) Działania do wykonania

1. Dodać blokadę per-order w `observePendingOrdersQueue()`.
2. Deduplikować socket events w `observeSocketEvents()`.
3. Upewnić się, że Reminder nie odpala alarmu, jeśli jest aktywny.
4. Dodać logi z `source` (SOCKET/REMINDER/INTENT) dla łatwiejszej diagnostyki.

---

## 7) Potwierdzenie naprawy

Po zmianach w logach powinno być:
- Tylko **1x** `✅ Otwieram dialog + ACTION_START` dla danego orderId.
- Brak kolejnego `startForegroundService` dla tego samego orderId w ciągu 1–2 sekund.
- Reminder uruchamia się tylko, jeśli alarm nie był aktywny.

---

## 8) Zastosowana poprawka (wdrożona w kodzie)

### 8.1. Deduplikacja eventów socket (2s debounce)
**Plik:** `L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\order\OrdersViewModel.kt`
- Dodane pola:
  - `lastSocketOrderId`, `lastSocketOrderAtMs`
- Nowy guard w `observeSocketEvents()`:
  - ignoruje ten sam `orderId` w oknie 2s

### 8.2. Guard na ACTION_START (2s debounce per orderId)
**Plik:** `L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\order\OrdersViewModel.kt`
- Dodane pola:
  - `lastAlarmStartOrderId`, `lastAlarmStartAtMs`
- Guard w `observePendingOrdersQueue()` w CASE 1:
  - blokuje ponowny `ACTION_START` dla tego samego `orderId` w 2s

### 8.3. Serwis alarmu nie gasi się przy duplikacie
**Plik:** `L:\SHOP APP\app\src\main\java\com\itsorderchat\service\OrderAlarmService.kt`
- Zmiana w `ACTION_START`:
  - duplikat nie wywołuje `stopSelf()`
  - alarm pozostaje aktywny

---

## 9) Co powinno się zmienić w logach

Po poprawkach spodziewane logi:
- brak drugiego `✅ Otwieram dialog + ACTION_START` w <2s
- brak `onDestroy – cleanup` tuż po starcie
- reminder nie przerywa aktywnego alarmu

---

## 10) Nowy log (00:39:36) – dlaczego alarm się zatrzymuje po starcie

W nowych logach widać **start alarmu**, a zaraz potem **ACTION_STOP_ALARM**:
```
00:39:36.828 OrderAlarmService  onStartCommand, action=ACTION_START_ALARM, startId=1
00:39:36.841 OrderAlarmService  Aktywuję nowy alarm dla orderId=698d1338fdf9812564720e52
00:39:36.901 OrderAlarmService  onStartCommand, action=ACTION_STOP_ALARM, startId=2
00:39:36.908 OrderAlarmService  handleStopAction: wyciszam i zamykam alarm
```

**Źródło STOP:** chwilę wcześniej w logu:
```
00:39:36.181 OrdersViewModel  observeCurrentlyOpenDialogStatus: Zamówienie 698d12e... nie jest już PROCESSING → ... ZATRZYMUJĘ alarm.
00:39:36.183 OrdersViewModel  ViewModel: Wysyłam polecenie ACTION_STOP_ALARM
```

**Wniosek:** alarm dla nowego ordera jest zatrzymywany przez logikę zamykania poprzedniego dialogu (inny `orderId`). To generuje efekt „start → stop → ring”.

**Naprawa wdrożona:** `ACTION_STOP_ALARM` jest teraz ignorowany, jeśli `orderId` w STOP nie pasuje do aktywnego alarmu. Dodatkowo `stopAlarmService()` przekazuje `orderId` zamykanego dialogu.

---

## 11) Linia z NotificationService (Cannot find enqueued record)

Linia:
```
00:39:37.176 NotificationService  Cannot find enqueued record for key: 0|com.itsorderchat|-687983058|null|10133
```

To ostrzeżenie systemu o powiadomieniu, które zostało już usunięte lub zastąpione. **Nie jest źródłem duplikacji dźwięku** – jest skutkiem wcześniejszego `stopForeground/cancel`.

---

## 12) Nowe logi 00:47:47 – co dokładnie widać

### 12.1. Ten sam event socket przychodzi 3x
```
00:47:47.661 OrdersView...cketEvents  📥 Received order from socket: orderId=698d1523fdf981256472110a
00:47:47.664 OrdersView...cketEvents  📥 Received order from socket: orderId=698d1523fdf981256472110a
00:47:47.667 OrdersView...cketEvents  📥 Received order from socket: orderId=698d1523fdf981256472110a
```
**Wniosek:** źródło socketów emituje zdarzenie wielokrotnie (lub subskrypcja jest wielokrotna).

### 12.2. `observePendingOrdersQueue()` uruchamia CASE 1 więcej niż raz
```
00:47:47.760 OrderAlarmService  🎯 CASE 1: Brak dialogu + brak suppress
00:47:47.761 OrderAlarmService  ✅ Otwieram dialog + ACTION_START dla: 698d1523fdf981256472110a
...
00:47:47.842 OrderAlarmService  🎯 CASE 1: Brak dialogu + brak suppress
00:47:47.842 OrderAlarmService  ✅ Otwieram dialog + ACTION_START dla: 698d1523fdf981256472110a
...
00:47:47.897 OrderAlarmService  🎯 CASE 1: Brak dialogu + brak suppress
00:47:47.897 OrderAlarmService  ✅ Otwieram dialog + ACTION_START dla: 698d1523fdf981256472110a
```
**Wniosek:** stan `currentDialogId` wraca do `null` w krótkich odstępach, więc logika uznaje, że „nie ma dialogu” i odpala `ACTION_START` ponownie.

### 12.3. Serwis dostaje wielokrotne ACTION_START
```
00:47:47.957 OrderAlarmService  onStartCommand, action=ACTION_START_ALARM, startId=1
00:47:48.036 OrderAlarmService  onStartCommand, action=ACTION_START_ALARM, startId=2
00:47:48.048 OrderAlarmService  onStartCommand, action=ACTION_START_ALARM, startId=3
```
Serwis **ignoruje** duplikaty (alarm aktywny), ale **samo wielokrotne odpalenie** jest objawem problemu po stronie ViewModel/źródła eventów.

### 12.4. Dlaczego `currentDialogId` wraca do null?
Najbardziej prawdopodobne scenariusze:
- **nowa instancja `OrdersViewModel`** (np. HomeActivity zre-startowana przez `SocketStaff...Handler`),
- **reset stanu** w wyniku ponownego zainicjalizowania UI,
- **równoległe ścieżki** ustawiające `orderToShowInDialog = null` (np. inne observery lub kod w Activity/Fragment).

---

## 13) Co zrobić, jeśli dalej widzisz duplikację

1. **Zablokować reset `orderToShowInDialog` przy reentry**
   - przechowywać `currentDialogId` w `SavedStateHandle` lub `AppPrefs` i przywracać po re-kreacji ViewModelu.

2. **Wzmocnić deduplikację w `observePendingOrdersQueue()`**
   - dodać guard na `lastAlarmStartOrderId` + `lastAlarmStartAtMs` (już wdrożone)
   - jeśli log nadal pokazuje wielokrotne CASE1, to znaczy, że guard resetuje się (nowa instancja VM).

3. **Zablokować wielokrotne tworzenie HomeActivity**
   - upewnić się, że `SocketStaff...Handler` nie odpala Activity, jeśli już jest widoczna
   - dodać prosty guard np. `if (AppStateManager.isAppInForeground) skip startActivity`.

---

## 14) TEST FINAŁOWY - WYNIK: ✅ SUKCES

### Data testów: 2026-02-12 01:29:43

**Zamówienie testowe:** `698d1c2afdf98125647215e5` (Order #19461, PROCESSING)

#### Co obserwujemy w logach:

**1. EVENT SOCKET - Tylko 1x emitowany (wcześniej 3x)**
```
01:29:43.584   📥 Received order from socket: orderId=698d1c2afdf98125647215e5
01:29:43.584   💾 Order saved to database: orderId=698d1c2afdf98125647215e5
```
✅ Deduplikacja działaa!

**2. ACTION_START - Uruchamia się raz**
```
01:29:43.584   ✅ Otwieram dialog + ACTION_START dla: 698d1c2afdf98125647215e5
01:29:43.745   OrderAlarmService: onStartCommand, action=ACTION_START_ALARM, startId=1
01:29:43.771   I  Aktywuję nowy alarm dla orderId: 698d1c2afdf98125647215e5
01:29:43.891   D  Odtwarzanie alarmu (loop) rozpoczęte.
```
✅ Nie ma wielokrotnych startów!

**3. DIALOG - Pozostaje stabilny**
```
01:29:43.899   🎯 CASE 4: Żadne warunki nie spełnione
01:29:43.900   └─ Przyczyna: Dialog otwarty (698d1c2afdf98125647215e5), brak nowych
```
✅ `currentDialogId` jest persystowany, dialog nie resetuje się!

**4. ALARM - Nie przerywany**
- ✅ Brak `onDestroy` między startami
- ✅ Dźwięk gra bez przeszkód
- ✅ Brak "start–stop–start" efektu

---

## 15) PORÓWNANIE: WCZEŚNIEJ vs TERAZ

| Aspekt | Wcześniej | Teraz |
|--------|----------|-------|
| Socket events | 3x `📥 Received` | 1x `📥 Received` |
| ACTION_START | 3x `onStartCommand` | 1x `onStartCommand` |
| Dialog resetuje | TAK (`null`) | NIE (`SavedStateHandle`) |
| Alarm przerywany | TAK (stop–start) | NIE |
| Dźwięk | Duplikuje się | Gra bez przerw |

---

## ✅ KONKLUZJA

**Wszystkie trzy poprawki zadziałały:**

1. ✅ **Deduplikacja socketów** - `distinctUntilChanged { orderId }`
2. ✅ **Persystencja dialogu** - `SavedStateHandle["currentDialogId"]`
3. ✅ **Guard ACTION_START** - Duplikat nie zamyka serwisu

**Rezultat:** Alarm uruchamia się **raz** i gra płynnie bez duplikacji.

---

