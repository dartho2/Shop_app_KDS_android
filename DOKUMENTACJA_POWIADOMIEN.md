# 📱 Dokumentacja Systemu Powiadomień o Nowych Zamówieniach

## 📋 Spis Treści
1. [Przegląd Systemu](#przegląd-systemu)
2. [Komponenty](#komponenty)
3. [Warunki Uruchomienia](#warunki-uruchomienia)
4. [Scenariusze Działania](#scenariusze-działania)
5. [Momenty Włączania i Wyłączania](#momenty-włączania-i-wyłączania)
6. [Przypadki Braku Powiadomienia](#przypadki-braku-powiadomienia)
7. [Diagnostyka](#diagnostyka)

---

## 🎯 Przegląd Systemu

System powiadomień składa się z **3 głównych elementów**:

### 1. **Dialog Zamówienia** (OrderDialog)
- Pełnoekranowe okno z detalami zamówienia
- Pokazuje się automatycznie przy nowym zamówieniu
- Może być zamknięte przez użytkownika

### 2. **Powiadomienie** (Notification)
- Notyfikacja systemowa Android z wysokim priorytetem
- Kategoria: ALARM
- Full Screen Intent (może otwierać aplikację z tła)
- 2 akcje: "Wycisz" i "Ustawienia"

### 3. **Dźwięk Alarmu** (MediaPlayer)
- Ciągły dźwięk (loop) do momentu wyłączenia
- Zdefiniowany przez użytkownika lub domyślny
- Audio Stream: USAGE_ALARM

---

## 🔧 Komponenty

### A. OrderAlarmService (Foreground Service)

**Plik:** `OrderAlarmService.kt`

**Odpowiedzialność:**
- Zarządzanie powiadomieniem foreground
- Odtwarzanie dźwięku alarmu
- Obsługa akcji: START, RING, STOP

**Akcje:**

| Akcja | Opis | Kiedy |
|-------|------|-------|
| `ACTION_START` | Pełny start - powiadomienie + dźwięk + full screen | Pierwsze nowe zamówienie |
| `ACTION_RING` | Odświeżenie powiadomienia + restart dźwięku | Kolejne zamówienie gdy dialog otwarty |
| `ACTION_STOP_ALARM` | Zatrzymanie alarmu i serwisu | Zamknięcie wszystkich zamówień |

---

### B. OrdersViewModel (Logika Biznesowa)

**Plik:** `OrdersViewModel.kt`

**Odpowiedzialność:**
- Monitorowanie kolejki zamówień PROCESSING
- Decydowanie kiedy pokazać dialog
- Decydowanie kiedy uruchomić alarm
- Zarządzanie suppress window (700ms po zamknięciu dialogu)

**Kluczowe funkcje:**

```kotlin
// 1. Obserwuje kolejkę zamówień PROCESSING
private fun observePendingOrdersQueue()

// 2. Uruchamia serwis alarmu
private fun startAlarmService(order: Order, ringOnly: Boolean)

// 3. Zatrzymuje alarm
private fun stopAlarmService()

// 4. Zamyka dialog (i ewentualnie dzwoni dla następnego)
fun dismissDialog()
```

---

### C. pendingOrdersQueue (Źródło Danych)

**Definicja:**
```kotlin
val pendingOrdersQueue: StateFlow<List<Order>> = repository.getAllOrdersFlow()
    .map { entities ->
        entities
            .map { OrderMapper.toOrder(it) }
            .filter { order ->
                val slugEnum = order.orderStatus.slug?.let { slug ->
                    runCatching { OrderStatusEnum.valueOf(slug) }.getOrNull()
                }
                slugEnum == OrderStatusEnum.PROCESSING
            }
            .sortedBy { it.createdAt }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

**Zawiera:**
- Tylko zamówienia ze statusem `PROCESSING`
- Posortowane po `createdAt` (najstarsze pierwsze)
- Aktualizowane automatycznie gdy baza się zmienia

---

## ✅ Warunki Uruchomienia Powiadomienia

### Warunki OBOWIĄZKOWE (wszystkie muszą być spełnione):

#### 1. **Uprawnienia POST_NOTIFICATIONS** (Android 13+)
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
        == PackageManager.PERMISSION_GRANTED
}
```
**Jeśli brak:** ❌ Powiadomienie NIE pojawi się

#### 2. **Rola Użytkownika = STAFF** (nie COURIER)
```kotlin
if (role != UserRole.COURIER) {
    // Powiadomienia tylko dla personelu
}
```
**Jeśli COURIER:** ❌ Powiadomienia wyłączone

#### 3. **Zamówienie w kolejce PROCESSING**
```kotlin
slugEnum == OrderStatusEnum.PROCESSING
```
**Jeśli ACCEPTED/COMPLETED:** ❌ Nie pokazuje powiadomienia

#### 4. **Kanał Powiadomień włączony**
- Użytkownik nie wyłączył kanału "Alarm zamówień" w ustawieniach Android
- Priorytet kanału: HIGH

---

## 🎬 Scenariusze Działania

### Scenariusz 1: Pierwsze Nowe Zamówienie (Czysta Kolejka)

**Warunki początkowe:**
- Kolejka pusta (`pendingOrdersQueue.size = 0`)
- Dialog zamknięty (`currentDialogId = null`)
- Suppress nieaktywny (`_suppressAutoOpen = false`)

**Przepływ:**
```
1. Nowe zamówienie przychodzi przez socket
   └─> Dodane do bazy ze statusem PROCESSING
   
2. pendingOrdersQueue emituje [Order1]
   └─> observePendingOrdersQueue() wykrywa zmianę
   
3. Sprawdzenie warunków:
   ✅ suppress = false
   ✅ currentDialogId = null
   ✅ queue.firstOrNull() = Order1
   
4. CASE 1 - Pełny start:
   ├─> Otwiera dialog (_uiState.orderToShowInDialog = Order1)
   └─> Uruchamia alarm (ACTION_START)
       ├─> Pokazuje powiadomienie
       ├─> Odtwarza dźwięk (loop)
       └─> Full Screen Intent (może otworzyć app)
```

**Logi:**
```
📊 observePendingOrdersQueue TRIGGERED
   ├─ queue.size: 1
   ├─ currentDialogId: null
   └─ suppress: false
🎯 CASE 1: Brak dialogu + brak suppress
✅ Otwieram dialog + ACTION_START dla: ORDER-001
🚨 ALARM CALL: orderId=ORDER-001
   ├─ ringOnly: false
   └─ action: ACTION_START
✅ ALARM STARTED SUCCESSFULLY!
```

---

### Scenariusz 2: Drugie Zamówienie (Dialog Otwarty)

**Warunki początkowe:**
- Kolejka: [Order1] (dialog otwarty dla Order1)
- Dialog otwarty (`currentDialogId = Order1.orderId`)
- Suppress nieaktywny

**Przepływ:**
```
1. Drugie zamówienie przychodzi
   └─> Dodane do bazy (Order2)
   
2. pendingOrdersQueue emituje [Order1, Order2]
   └─> observePendingOrdersQueue() wykrywa zmianę
   
3. Sprawdzenie warunków:
   ✅ currentDialogId = Order1
   ✅ newestNotShown = Order2 (nie jest w dialogu)
   ✅ lastRangOrderId != Order2
   
4. CASE 2 - Dzwoń dla nowego:
   ├─> Dialog pozostaje dla Order1 (nie zmienia się!)
   └─> Uruchamia alarm (ACTION_RING)
       ├─> Aktualizuje powiadomienie (nowy tekst)
       ├─> Restartuje dźwięk (bump - krótka przerwa + znowu gra)
       └─> Zapisuje lastRangOrderId = Order2
```

**Logi:**
```
📊 observePendingOrdersQueue TRIGGERED
   ├─ queue.size: 2
   ├─ currentDialogId: ORDER-001
   └─ newestNotShown: ORDER-002
🎯 CASE 2: Dialog otwarty + nowe zamówienie
✅ ACTION_RING dla nowego: ORDER-002
🚨 ALARM CALL: orderId=ORDER-002
   ├─ ringOnly: true
   └─> action: ACTION_RING
✅ ALARM STARTED SUCCESSFULLY!
```

---

### Scenariusz 3: Zamknięcie Dialogu (Jest Kolejne)

**Warunki początkowe:**
- Kolejka: [Order1, Order2]
- Dialog otwarty dla Order1
- Użytkownik klika "Zamknij" lub "Drukuj"

**Przepływ:**
```
1. dismissDialog() wywołane
   └─> lastClosedOrderId = Order1
   
2. Sprawdzenie kolejki:
   ✅ next = Order2 (pierwsze które != Order1)
   
3. NIE zatrzymuje alarmu!
   ├─> startAlarmService(Order2, ringOnly = true)
   └─> _uiState.orderToShowInDialog = null (zamyka dialog)
   
4. Suppress window (700ms):
   ├─> _suppressAutoOpen = true
   ├─> delay(700ms)
   └─> _suppressAutoOpen = false
   
5. Po 700ms observePendingOrdersQueue() reaguje:
   ✅ suppress = false (już minęło)
   ✅ currentDialogId = null
   ✅ next = Order2
   
6. CASE 1 - Otwiera dialog dla Order2
   └─> Dźwięk już gra (nie został zatrzymany)
```

**Logi:**
```
dismissDialog()
   └─ next=ORDER-002 → nie wyłączam alarmu, RING dla next
🚨 ALARM CALL: orderId=ORDER-002, ringOnly=true

(po 700ms)
📊 observePendingOrdersQueue TRIGGERED
   ├─ queue.size: 2
   └─ suppress: false (już minęło)
🎯 CASE 1: Brak dialogu + brak suppress
✅ Otwieram dialog + ACTION_START dla: ORDER-002
```

---

### Scenariusz 4: Zamknięcie Dialogu (Brak Kolejnych)

**Warunki początkowe:**
- Kolejka: [Order1]
- Dialog otwarty dla Order1
- To ostatnie zamówienie

**Przepływ:**
```
1. dismissDialog() wywołane
   └─> lastClosedOrderId = Order1
   
2. Sprawdzenie kolejki:
   ❌ next = null (brak innych zamówień)
   
3. Zatrzymanie alarmu:
   └─> stopAlarmService()
       ├─> Wysyła ACTION_STOP_ALARM
       ├─> Zatrzymuje dźwięk
       ├─> Usuwa powiadomienie
       └─> Zatrzymuje serwis foreground
   
4. Dialog zamknięty:
   └─> _uiState.orderToShowInDialog = null
   
5. Suppress window (700ms) - bez efektu
   └─> Kolejka już pusta
```

**Logi:**
```
dismissDialog: brak kolejnych PROCESSING → STOP alarm
ViewModel: Wysyłam polecenie ACTION_STOP_ALARM
OrderAlarmService: handleStopAction - zatrzymuję alarm
```

---

### Scenariusz 5: Nowe Zamówienie w Suppress Window

**Warunki początkowe:**
- Użytkownik właśnie zamknął dialog dla Order1
- Suppress aktywny (w trakcie 700ms)
- Nowe zamówienie Order2 przychodzi

**Przepływ:**
```
1. Order2 dodane do kolejki
   └─> pendingOrdersQueue emituje [Order2]
   
2. observePendingOrdersQueue() wykrywa zmianę
   
3. Sprawdzenie warunków:
   ⚠️ suppress = true (jeszcze trwa)
   ✅ currentDialogId = null
   ✅ newestNotShown = Order2
   
4. CASE 3 - Suppress mode:
   ├─> NIE otwiera dialogu (suppress blokuje)
   └─> Uruchamia alarm (ACTION_RING)
       └─> Dźwięk zaczyna grać
   
5. Po upływie suppress (700ms):
   ✅ suppress = false
   └─> CASE 1 - Otwiera dialog dla Order2
       └─> Dźwięk już gra
```

**Logi:**
```
📊 observePendingOrdersQueue TRIGGERED
   ├─ suppress: true
   └─ newestNotShown: ORDER-002
🎯 CASE 3: Suppress aktywny + nowe zamówienie
✅ ACTION_RING (suppress mode) dla: ORDER-002

(po 700ms)
🎯 CASE 1: Brak dialogu + brak suppress
✅ Otwieram dialog + ACTION_START dla: ORDER-002
```

---

## ⏱️ Momenty Włączania i Wyłączania

### 🟢 WŁĄCZANIE ALARMU (startAlarmService)

#### Moment 1: Pierwsze Zamówienie (ACTION_START)
**Kiedy:**
- Kolejka była pusta → teraz ma 1+ zamówienie
- Dialog zamknięty
- Suppress nieaktywny

**Efekt:**
- ✅ Powiadomienie foreground
- ✅ Dźwięk zaczyna grać (loop)
- ✅ Full Screen Intent (może otworzyć app)
- ✅ Dialog się otwiera

---

#### Moment 2: Kolejne Zamówienie (ACTION_RING)
**Kiedy:**
- Dialog już otwarty dla innego zamówienia
- Nowe zamówienie wpada do kolejki
- LUB: Suppress aktywny + nowe zamówienie

**Efekt:**
- ✅ Powiadomienie aktualizowane (nowy tekst)
- ✅ Dźwięk restartuje (krótka przerwa + znowu gra)
- ❌ Dialog NIE zmienia się (jeśli otwarty)
- ❌ Full Screen Intent NIE uruchamia (ringOnly=true)

---

#### Moment 3: Po Zamknięciu Dialogu (RING dla następnego)
**Kiedy:**
- Użytkownik zamyka dialog
- Kolejka ma więcej zamówień

**Efekt:**
- ✅ Dźwięk kontynuuje (ACTION_RING dla next)
- ❌ Dźwięk NIE zatrzymuje się
- ⏱️ Po 700ms: Dialog otwiera się dla następnego

---

### 🔴 WYŁĄCZANIE ALARMU (stopAlarmService)

#### Moment 1: Zamknięcie Ostatniego Zamówienia
**Kiedy:**
- Użytkownik zamyka dialog
- Kolejka nie ma więcej zamówień PROCESSING

**Efekt:**
- ❌ Dźwięk zatrzymuje się
- ❌ Powiadomienie usunięte
- ❌ Serwis foreground zatrzymany

**Kod:**
```kotlin
fun dismissDialog() {
    val next = pendingOrdersQueue.value.firstOrNull { it.orderId != closedId }
    if (next == null) {
        stopAlarmService() // ← TUTAJ
    }
}
```

---

#### Moment 2: Kliknięcie "Wycisz" w Powiadomieniu
**Kiedy:**
- Użytkownik klika akcję "Wycisz" w notyfikacji

**Efekt:**
- ❌ Dźwięk zatrzymuje się
- ❌ Powiadomienie usunięte
- ❌ Serwis zatrzymany
- ⚠️ Dialog może pozostać otwarty (jeśli był)

**Kod:**
```kotlin
// W OrderAlarmService
private fun buildStopPending(): PendingIntent {
    val stopIntent = Intent(this, OrderAlarmService::class.java).apply {
        action = ACTION_STOP_ALARM
    }
    return PendingIntent.getService(...)
}
```

---

#### Moment 3: Akceptacja/Zmiana Statusu Zamówienia
**Kiedy:**
- Zamówienie zmienia status z PROCESSING → ACCEPTED
- Kolejka jest aktualizowana (filtr usuwa zamówienie)

**Efekt:**
- 🔄 observePendingOrdersQueue() reaguje
- ✅ Jeśli były inne: Dialog dla następnego (dźwięk gra dalej)
- ❌ Jeśli to ostatnie: stopAlarmService()

---

## ❌ Przypadki Braku Powiadomienia

### Sytuacja 1: Brak Uprawnień POST_NOTIFICATIONS

**Android 13+:**
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    checkSelfPermission(POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
    // ❌ Powiadomienie nie pojawi się
}
```

**Rozwiązanie:**
- Użytkownik musi nadać uprawnienie w ustawieniach aplikacji
- Aplikacja powinna prosić o uprawnienie przy pierwszym uruchomieniu

**Logi:**
```
❌ ALARM BLOCKED: Brak uprawnień POST_NOTIFICATIONS
```

---

### Sytuacja 2: Kanał Powiadomień Wyłączony

**Użytkownik wyłączył:**
- Ustawienia Android → Aplikacje → Shop App → Powiadomienia → "Alarm zamówień" = OFF

**Efekt:**
- Serwis startuje
- Dźwięk może grać (jeśli DND wyłączony)
- ❌ Powiadomienie NIE pokazuje się
- ❌ Full Screen Intent NIE działa

**Rozwiązanie:**
- Kliknąć "Ustawienia" w notyfikacji (jeśli była)
- Włączyć kanał "Alarm zamówień"

---

### Sytuacja 3: Tryb DND (Do Not Disturb)

**Jeśli włączony DND:**
- Powiadomienie może być wyciszone
- Zależy od ustawień kanału `setBypassDnd(true)`

**Kod:**
```kotlin
val channel = NotificationChannel(...).apply {
    setBypassDnd(true) // Próbuje ominąć DND
}
```

**Efekt:**
- ✅ Powiadomienie powinno się pokazać (jeśli uprawnienie)
- ⚠️ Dźwięk może być wyciszony przez system

---

### Sytuacja 4: Rola = COURIER

**Kurier NIE dostaje powiadomień:**
```kotlin
if (role != UserRole.COURIER) {
    // Tylko dla STAFF
}
```

**Logi:**
```
📊 observePendingOrdersQueue TRIGGERED
   ├─ userRole: COURIER
   └─ (nie wykonuje logiki alarmu)
```

---

### Sytuacja 5: Zamówienie NIE jest PROCESSING

**Tylko PROCESSING uruchamia alarm:**
```kotlin
val pendingOrdersQueue = ... .filter { order ->
    slugEnum == OrderStatusEnum.PROCESSING
}
```

**Przypadki:**
- Zamówienie przychodzi jako ACCEPTED → ❌ Brak alarmu
- Zamówienie COMPLETED → ❌ Brak alarmu
- Zamówienie CANCELLED → ❌ Brak alarmu

**Wyjątek:**
- Jeśli "Auto-drukuj DINE_IN" włączone → Drukuje bez alarmu

---

### Sytuacja 6: Suppress Window (700ms)

**Bezpośrednio po zamknięciu dialogu:**
```kotlin
_suppressAutoOpen.value = true
delay(700)
_suppressAutoOpen.value = false
```

**Efekt:**
- ❌ Dialog NIE otwiera się (przez 700ms)
- ✅ Dźwięk może zagrać (CASE 3 - suppress mode)
- ✅ Po 700ms: Dialog się otwiera

**Cel:** Zapobieganie natychmiastowemu ponownemu otwarciu dialogu

---

### Sytuacja 7: Aplikacja w Tle + Brak Uprawnień Battery Optimization

**Android 12+:**
- Jeśli app jest "zoptymalizowana" (doze mode)
- Serwis foreground może być opóźniony

**Rozwiązanie:**
- Wyłączyć optymalizację baterii dla aplikacji
- Ustawienia → Bateria → Optymalizacja baterii → Shop App → Nie optymalizuj

---

## 🔍 Diagnostyka

### Log Tag: `ALARM_DIAG`

**Włączanie logów:**
```bash
adb logcat | grep "ALARM_DIAG"
```

**Przykładowy output:**
```
ALARM_DIAG: ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
ALARM_DIAG: 🚨 ALARM CALL: orderId=ORDER-123, orderNumber=00123
ALARM_DIAG:    ├─ ringOnly: false
ALARM_DIAG:    ├─ orderStatus: PROCESSING
ALARM_DIAG:    └─ action: ACTION_START
ALARM_DIAG: 🔐 Sprawdzam uprawnienia POST_NOTIFICATIONS: true
ALARM_DIAG: 🛑 Sprawdzam suppress: false
ALARM_DIAG: 📦 Tworzę Intent dla OrderAlarmService...
ALARM_DIAG: 🚀 App w foreground: true
ALARM_DIAG: ✅ Uruchamiam OrderAlarmService (startService)
ALARM_DIAG: ✅ ALARM STARTED SUCCESSFULLY!
ALARM_DIAG: ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### Log Tag: `OrderAlarmService`

**Monitorowanie kolejki:**
```bash
adb logcat | grep "OrderAlarmService"
```

**Przykłady:**
```
OrderAlarmService: 📊 observePendingOrdersQueue TRIGGERED
OrderAlarmService:    ├─ queue.size: 2
OrderAlarmService:    ├─ currentDialogId: ORDER-001
OrderAlarmService:    └─ newestNotShown: ORDER-002
OrderAlarmService: 🎯 CASE 2: Dialog otwarty + nowe zamówienie
OrderAlarmService: ✅ ACTION_RING dla nowego: ORDER-002
```

---

### Testy Diagnostyczne

#### Test 1: Podstawowe Powiadomienie
```
1. Wyślij nowe zamówienie (PROCESSING)
2. Sprawdź logi:
   - "ALARM CALL"
   - "ALARM STARTED SUCCESSFULLY"
3. Sprawdź czy:
   ✅ Powiadomienie się pokazało
   ✅ Dźwięk gra
   ✅ Dialog się otworzył
```

#### Test 2: Drugie Zamówienie
```
1. Zostaw pierwszy dialog otwarty
2. Wyślij drugie zamówienie
3. Sprawdź logi:
   - "CASE 2: Dialog otwarty"
   - "ACTION_RING"
4. Sprawdź czy:
   ✅ Dialog pozostaje dla pierwszego
   ✅ Dźwięk restartuje (krótka przerwa)
```

#### Test 3: Suppress Window
```
1. Otwórz dialog
2. Zamknij natychmiast
3. Sprawdź logi:
   - "dismiss Dialog"
   - "_suppressAutoOpen = true"
4. Wyślij nowe zamówienie (w ciągu 700ms)
5. Sprawdź logi:
   - "CASE 3: Suppress aktywny"
   - "ACTION_RING (suppress mode)"
6. Czekaj 700ms
7. Sprawdź czy:
   ✅ Dialog otwiera się automatycznie
```

---

## 📊 Tabela Decyzyjna

| Queue | Dialog | Suppress | Akcja | Powiadomienie | Dźwięk | Dialog Opens |
|-------|--------|----------|-------|---------------|--------|--------------|
| [O1] | null | false | START | ✅ Tak | ✅ Tak (loop) | ✅ O1 |
| [O1, O2] | O1 | false | RING | ✅ Update | ✅ Restart | ❌ Pozostaje O1 |
| [O1, O2] | null | true | RING | ✅ Update | ✅ Restart | ❌ Czeka 700ms |
| [O1, O2] | null | false (po 700ms) | START | ✅ Tak | ✅ Gra dalej | ✅ O2 |
| [] | O1 → null | - | STOP | ❌ Usuwa | ❌ Stop | ❌ |

---

## 🎓 Best Practices

### ✅ Zalecane

1. **Nadaj uprawnienia POST_NOTIFICATIONS** przy pierwszym uruchomieniu
2. **Nie wyłączaj kanału "Alarm zamówień"** w ustawieniach
3. **Wyłącz optymalizację baterii** dla aplikacji
4. **Zezwól na Full Screen Intent** (Android 14+)

### ⚠️ Znane Ograniczenia

1. **Suppress window (700ms)** - dialog nie otworzy się natychmiast po zamknięciu
2. **Android 14+** - wymaga dodatkowego uprawnienia dla Full Screen Intent
3. **DND mode** - może wyciszyć dźwięk (zależy od ustawień systemowych)
4. **Battery optimization** - może opóźnić serwis w tle

---

## 📝 Podsumowanie Przepływu

```
┌──────────────────────────┐
│ Nowe zamówienie (socket) │
└────────────┬─────────────┘
             │
             ▼
     ┌───────────────┐
     │ Dodane do bazy│
     │ (PROCESSING)  │
     └───────┬───────┘
             │
             ▼
   ┌─────────────────────┐
   │ pendingOrdersQueue  │
   │ emituje zmianę      │
   └─────────┬───────────┘
             │
             ▼
┌────────────────────────────┐
│ observePendingOrdersQueue()│
└────────┬───────────────────┘
         │
         ▼
    ┌────────┐
    │CASE 1-4│
    └────┬───┘
         │
    ┌────▼─────────────────┐
    │ startAlarmService()  │
    └────┬─────────────────┘
         │
    ┌────▼──────────────────┐
    │ OrderAlarmService     │
    ├───────────────────────┤
    │ • Powiadomienie       │
    │ • Dźwięk              │
    │ • Full Screen Intent  │
    └───────────────────────┘
```

---

**Data:** 2026-02-04  
**Wersja:** 1.0  
**Status:** ✅ Kompletna dokumentacja  
**Powiązane:** `OrderAlarmService.kt`, `OrdersViewModel.kt`

