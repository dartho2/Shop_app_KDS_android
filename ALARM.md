# 📱 Dokumentacja Systemu Alarmów dla Zamówień

## 📋 Spis treści
1. [Przegląd systemu](#przegląd-systemu)
2. [Architektura](#architektura)
3. [Przepływ danych](#przepływ-danych)
4. [Wyzwalacze alarmu](#wyzwalacze-alarmu)
5. [Warunki zatrzymania alarmu](#warunki-zatrzymania-alarmu)
6. [Typy akcji alarmu](#typy-akcji-alarmu)
7. [Zarządzanie powiadomieniami](#zarządzanie-powiadomieniami)
8. [Obsługa wielu zamówień](#obsługa-wielu-zamówień)
9. [Dźwięk alarmu](#dźwięk-alarmu)
10. [Debugowanie](#debugowanie)

---

## 🎯 Przegląd systemu

System alarmów w aplikacji SHOP APP jest odpowiedzialny za:
- **Wykrywanie nowych zamówień** ze statusem `PROCESSING`
- **Odtwarzanie dźwięku alarmu** w pętli
- **Wyświetlanie powiadomień** typu fullscreen
- **Zarządzanie kolejką zamówień** oczekujących na akceptację
- **Automatyczne zatrzymywanie** po akcjach użytkownika

### Główne komponenty:
- **`OrderAlarmService`** - Foreground Service odtwarzający alarm
- **`OrdersViewModel`** - Logika biznesowa i monitorowanie kolejki
- **`SocketStaffEventsHandler`** - Obsługa zdarzeń WebSocket
- **`AcceptOrderSheetContent`** - UI dialogu akceptacji z zatrzymywaniem alarmu

---

## 🏗️ Architektura

```
┌─────────────────────────────────────────────────────────────┐
│                      WebSocket Server                        │
└──────────────────────┬──────────────────────────────────────┘
                       │ ORDER_CREATED event
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              SocketStaffEventsHandler                        │
│  • Parsuje event ORDER_CREATED                              │
│  • Sprawdza status: PROCESSING?                             │
│  • Zapisuje do lokalnej bazy (Room)                         │
│  • Emituje do SocketEventsRepository                        │
└──────────────────────┬──────────────────────────────────────┘
                       │ emitOrder(order)
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   OrdersViewModel                            │
│  • Monitoruje pendingOrdersQueue (Flow)                     │
│  • Wykrywa nowe zamówienia PROCESSING                       │
│  • Decyduje: pełny alarm vs dzwonek                         │
│  • Wywołuje startAlarmService()                             │
└──────────────────────┬──────────────────────────────────────┘
                       │ startService(ACTION_START/ACTION_RING)
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                  OrderAlarmService                           │
│  • Uruchamia Foreground Service                             │
│  • Odtwarza dźwięk w pętli (MediaPlayer)                   │
│  • Wyświetla powiadomienie fullscreen                       │
│  • Czeka na ACTION_STOP_ALARM                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 Przepływ danych

### 1. **Otrzymanie nowego zamówienia przez WebSocket**

```kotlin
// SocketStaffEventsHandler.kt: handleNewOrProcessingOrder()

1. WebSocket event: ORDER_CREATED
   ↓
2. Parse JSON → OrderWrapper
   ↓
3. Sprawdź status: order.orderStatus.slug == "PROCESSING"
   ↓
4. Zapisz do Room DB: ordersRepository.insertOrUpdateOrder()
   ↓
5. Emituj do Flow: socketEventsRepo.emitOrder(order)
   ↓
6. Jeśli app w tle → openHomeWithPayload()
```

### 2. **Monitorowanie kolejki w OrdersViewModel**

```kotlin
// OrdersViewModel.kt: observePendingOrdersQueue()

pendingOrdersQueue (Flow<List<Order>>) ← filtruje zamówienia PROCESSING
   ↓
combine(queue, userRole, suppressAutoOpen)
   ↓
DECYZJA:
┌────────────────────────────────────────────────────────┐
│ Case 1: Brak dialogu + brak suppress                  │
│   → _uiState.orderToShowInDialog = next               │
│   → startAlarmService(next, ringOnly = FALSE)         │
│   → Pełny alarm + fullscreen                          │
├────────────────────────────────────────────────────────┤
│ Case 2: Dialog otwarty + nowe zamówienie              │
│   → startAlarmService(newest, ringOnly = TRUE)        │
│   → Tylko dzwonek, dialog się nie zmienia             │
├────────────────────────────────────────────────────────┤
│ Case 3: suppress + nowe zamówienie                    │
│   → startAlarmService(newest, ringOnly = TRUE)        │
│   → Dzwonek bez otwierania dialogu                    │
└────────────────────────────────────────────────────────┘
```

### 3. **Uruchomienie OrderAlarmService**

```kotlin
// OrdersViewModel.kt: startAlarmService()

val intent = Intent(context, OrderAlarmService::class.java).apply {
    action = if (ringOnly) ACTION_RING else ACTION_START
    putExtra(EXTRA_ORDER_ID, order.orderId)
    putExtra(EXTRA_ORDER_JSON, jsonPayload)
}

if (AppStateManager.isAppInForeground) {
    context.startService(intent)
} else {
    ContextCompat.startForegroundService(context, intent)
}
```

---

## ⚡ Wyzwalacze alarmu

### ✅ Alarm zostaje uruchomiony gdy:

| # | Warunek | Typ akcji | Opis |
|---|---------|-----------|------|
| 1 | **Nowe zamówienie PROCESSING** | `ACTION_START` | Pierwsze zamówienie w kolejce, brak aktywnego dialogu |
| 2 | **Kolejne zamówienie przy otwartym dialogu** | `ACTION_RING` | Dialog jest już wyświetlony, nowe zamówienie dzwoni |
| 3 | **Nowe zamówienie w oknie suppress** | `ACTION_RING` | W ciągu 700ms po zamknięciu dialogu |
| 4 | **WebSocket: ORDER_CREATED + status PROCESSING** | `ACTION_START` | Socket event bezpośrednio z serwera |
| 5 | **App w tle + nowe PROCESSING** | `ACTION_START` | Service uruchamia HomeActivity z fullscreen |
| 6 | **Restart po STICKY** | `ACTION_START` | System restartuje service po zabójstwie procesu |

### 🚫 Alarm NIE zostaje uruchomiony gdy:

| # | Warunek | Powód |
|---|---------|-------|
| 1 | Brak uprawnień `POST_NOTIFICATIONS` | Android 13+, brak zgody użytkownika |
| 2 | `_suppressAutoOpen.value == true` | 700ms "dead zone" po zamknięciu dialogu |
| 3 | `userRole == UserRole.COURIER` | Kurierzy nie widzą alarmu PROCESSING |
| 4 | Zamówienie ma inny status niż PROCESSING | Np. PENDING, ACCEPTED, CANCELLED |
| 5 | `currentNotificationId != null` przy ACTION_START | Alarm już aktywny dla innego zamówienia |

---

## 🛑 Warunki zatrzymania alarmu

### 1. **Akcje użytkownika w AcceptOrderSheetContent.kt**

Alarm jest zatrzymywany **PRZED** wszystkimi akcjami (aby uniknąć restartu):

```kotlin
context.startService(Intent(context, OrderAlarmService::class.java).apply {
    action = OrderAlarmService.ACTION_STOP_ALARM
})
```

| # | Akcja użytkownika | Lokalizacja w kodzie | Opis |
|---|-------------------|---------------------|------|
| 1 | **Wybór czasu przygotowania** | `onConfirm` w `PreparationTimeDialog` | Akceptacja z czasem 15/30/45/60/90 min |
| 2 | **Wybór daty/czasu dostawy** | Przycisk "Potwierdź czas" | Ręczne ustawienie terminu |
| 3 | **Wysłanie do kuriera zewnętrznego** | `onSendExternalCourier` | Stava/Stuart/Wolt |
| 4 | **Anulowanie kuriera zewnętrznego** | `onCancelExternalCourier` | Przycisk "Anuluj" |
| 5 | **Kurier bierze zamówienie** | `onCourierChange(true)` | Przycisk "Weź zamówienie" |
| 6 | **Kurier rezygnuje** | `onCourierChange(false)` | Przycisk "Rezygnuj" |
| 7 | **Zmiana statusu: W drodze** | `onStatusChange(OUT_FOR_DELIVERY)` | Dla kuriera lub personelu |
| 8 | **Zmiana statusu: Ukończone** | `onStatusChange(COMPLETED)` | Dla kuriera lub personelu |
| 9 | **Zmiana statusu: Dostarczone** | `onStatusChange(COMPLETED)` | Kurier kończy dostawę |

### 2. **Automatyczne zatrzymanie w OrdersViewModel**

```kotlin
// OrdersViewModel.kt: dismissDialog()

val next = pendingOrdersQueue.value.firstOrNull { it.orderId != closedId }

if (next == null) {
    // Brak kolejnych zamówień → STOP
    stopAlarmService()
} else {
    // Jest kolejne → RING (restart dźwięku bez zatrzymania)
    startAlarmService(next, ringOnly = true)
}
```

**Zatrzymuje alarm gdy:**
- Kolejka zamówień PROCESSING jest pusta
- Wszystkie zamówienia zostały zaakceptowane/odrzucone

### 3. **Zatrzymanie przez notyfikację**

Użytkownik może kliknąć przycisk **"Wycisz"** w powiadomieniu:

```kotlin
// OrderAlarmService.kt: buildStopPending()

val stopIntent = Intent(this, OrderAlarmService::class.java).apply {
    action = ACTION_STOP_ALARM
}
// Dodane jako akcja w NotificationCompat.Builder
```

### 4. **Zatrzymanie przy zamknięciu Service**

```kotlin
// OrderAlarmService.kt: onDestroy()

override fun onDestroy() {
    stopAlarmSound()
    currentNotificationId?.let {
        NotificationManagerCompat.from(this).cancel(it)
    }
    currentNotificationId = null
}
```

---

## 🎬 Typy akcji alarmu

### `ACTION_START` - Pełny alarm

**Kiedy:** Pierwsze zamówienie w kolejce, brak aktywnego alarmu

**Zachowanie:**
- ✅ Uruchamia Foreground Service
- ✅ Wyświetla powiadomienie **fullscreen** (otwiera app automatycznie)
- ✅ Rozpoczyna odtwarzanie dźwięku w pętli
- ✅ Ustawia `currentNotificationId` (blokuje kolejne ACTION_START)

```kotlin
when (action) {
    ACTION_START, null -> {
        if (currentNotificationId != null) {
            Timber.w("Alarm już aktywny. Ignoruję nowe.")
            stopSelf(startId)
            return START_STICKY
        }
        currentNotificationId = notificationId
        val notif = buildAlarmNotification(orderJson, useFullScreen = true)
        updateForeground(notificationId, notif)
        startAlarmSound()
        return START_STICKY
    }
}
```

### `ACTION_RING` - Dzwonek

**Kiedy:** Kolejne zamówienie przy aktywnym alarmie/dialogu

**Zachowanie:**
- ✅ Aktualizuje powiadomienie (bez fullscreen)
- ✅ Restartuje dźwięk (`seekTo(0)` + `start()`)
- ❌ NIE otwiera dialogu automatycznie
- ❌ NIE zmienia `currentNotificationId`

```kotlin
when (action) {
    ACTION_RING -> {
        val notif = buildAlarmNotification(orderJson, useFullScreen = false)
        if (currentNotificationId == null) {
            currentNotificationId = notificationId
            updateForeground(notificationId, notif)
        } else {
            NotificationManagerCompat.from(this).notify(currentNotificationId!!, notif)
        }
        restartAlarmSound()
        return START_STICKY
    }
}
```

### `ACTION_STOP_ALARM` - Zatrzymanie

**Kiedy:** Użytkownik akceptuje zamówienie lub klika "Wycisz"

**Zachowanie:**
- ✅ Zatrzymuje MediaPlayer (`player?.stop()` + `release()`)
- ✅ Usuwa powiadomienie
- ✅ Wyłącza Foreground Service
- ✅ Czyści `currentNotificationId`

```kotlin
when (action) {
    ACTION_STOP_ALARM -> {
        tryEnsurePlaceholderForeground()
        handleStopAction(startId)
        return START_NOT_STICKY
    }
}
```

---

## 🔔 Zarządzanie powiadomieniami

### Kanał notyfikacji

```kotlin
// OrderAlarmService.kt: createAlarmNotificationChannel()

NotificationChannel(
    id = "order_alarm_channel_v3",
    name = getString(R.string.order_alarm_channel_name),
    importance = NotificationManager.IMPORTANCE_HIGH
).apply {
    enableVibration(true)
    setBypassDnd(true)  // Pomija tryb "Nie przeszkadzać"
    setSound(null, attrs) // Dźwięk przez MediaPlayer, nie kanał
}
```

### Budowanie powiadomienia

```kotlin
NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
    .setSmallIcon(R.drawable.ic_log)
    .setContentTitle(getString(R.string.order_alarm_title))
    .setContentText(getString(R.string.order_alarm_body))
    .setPriority(NotificationCompat.PRIORITY_HIGH)
    .setCategory(NotificationCompat.CATEGORY_ALARM)
    .setFullScreenIntent(fullPending, useFullScreen)  // ← KEY: fullscreen
    .setOngoing(true)  // Nie można ręcznie usunąć
    .setAutoCancel(false)
    .addAction(R.drawable.ic_log, "Wycisz", stopPending)
    .addAction(R.drawable.ic_log, "Ustawienia", channelSettingsPending)
```

### Placeholder Foreground

**Dlaczego?** Android wymaga wejścia w Foreground w ciągu 5s od `startForegroundService()`.

```kotlin
// OrderAlarmService.kt: ensurePlaceholderForeground()

val placeholder = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
    .setSmallIcon(R.drawable.ic_log)
    .setContentTitle(getString(R.string.order_alarm_boot_title))
    .setPriority(NotificationCompat.PRIORITY_MIN)
    .setOngoing(true)
    .build()

startForeground(PLACEHOLDER_ID, placeholder, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
// Potem zamieniane na właściwe powiadomienie
```

---

## 📦 Obsługa wielu zamówień

### Kolejka zamówień

```kotlin
// OrdersViewModel.kt

val pendingOrdersQueue: StateFlow<List<Order>> = allOrdersFlow
    .map { orders ->
        orders.filter { order ->
            val slug = order.orderStatus.slug?.let {
                runCatching { OrderStatusEnum.valueOf(it) }.getOrNull()
            }
            slug == OrderStatusEnum.PROCESSING
        }.sortedBy { it.createdAt }
    }
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
```

### Strategie wyświetlania

| Sytuacja | Akcja | Typ alarmu |
|----------|-------|-----------|
| **Kolejka pusta** | Nic | - |
| **1 zamówienie, brak dialogu** | Otwórz dialog | `ACTION_START` (fullscreen) |
| **2+ zamówień, brak dialogu** | Otwórz dialog dla 1-go | `ACTION_START` |
| **Dialog otwarty, nowe wpada** | Dialog bez zmian | `ACTION_RING` (dzwonek) |
| **Zamknięto dialog, jest kolejne** | Okno suppress 700ms | `ACTION_RING` |
| **Zaakceptowano wszystkie** | Zatrzymaj alarm | `ACTION_STOP` |

### Suppress Auto-Open (700ms)

**Po co?** Blokuje natychmiastowe ponowne otwarcie dialogu po zamknięciu.

```kotlin
// OrdersViewModel.kt: dismissDialog()

autoOpenGuardJob = viewModelScope.launch {
    _suppressAutoOpen.value = true
    delay(700)  // "Dead zone"
    _suppressAutoOpen.value = false
}
```

**W tym czasie:**
- ❌ NIE otwiera się nowy dialog
- ✅ DZWONI dla nowych zamówień (`ACTION_RING`)
- ✅ Użytkownik może ręcznie otworzyć listę

---

## 🔊 Dźwięk alarmu

### MediaPlayer - Odtwarzanie w pętli

```kotlin
// OrderAlarmService.kt: startAlarmSound()

val selected = runBlocking {
    prefs.getNotificationSoundUri("order_alarm")  // Per-typ
        ?: prefs.getNotificationSoundUri()        // Globalny
        ?: "android.resource://$packageName/${R.raw.order_iphone}"  // Domyślny
}

player = MediaPlayer().apply {
    setDataSource(applicationContext, selected.toUri())
    setAudioAttributes(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    )
    isLooping = true  // ← Pętla nieskończona
    prepare()
    start()
}
```

### Restart dźwięku (`ACTION_RING`)

```kotlin
// OrderAlarmService.kt: restartAlarmSound()

if (player == null) {
    startAlarmSound()
} else {
    player?.seekTo(0)  // Cofnij na początek
    if (player?.isPlaying != true) {
        player?.start()
    }
}
```

### Zatrzymanie dźwięku

```kotlin
// OrderAlarmService.kt: stopAlarmSound()

if (player?.isPlaying == true) {
    player?.stop()
}
player?.release()
player = null
```

---

## 🐛 Debugowanie

### Logi przed wywołaniem alarmu

**WSZYSTKIE** wywołania `startAlarmService()` są poprzedzone szczegółowymi logami diagnostycznymi:

```kotlin
// Przykładowy output w Logcat:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🚨 ALARM CALL: orderId=67890abcdef, orderNumber=#12345
   ├─ ringOnly: false
   ├─ orderStatus: processing
   ├─ createdAt: 2026-01-13T10:30:00Z
   └─ action: ACTION_START
🔐 Sprawdzam uprawnienia POST_NOTIFICATIONS: true
🛑 Sprawdzam suppress: false
📦 Tworzę Intent dla OrderAlarmService...
🚀 App w foreground: true
✅ Uruchamiam OrderAlarmService (startService)
✅ ALARM STARTED SUCCESSFULLY!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Logi w observePendingOrdersQueue():**

```kotlin
// Każda zmiana w kolejce zamówień jest logowana:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 observePendingOrdersQueue TRIGGERED
   ├─ queue.size: 2
   ├─ userRole: STAFF
   └─ suppress: false
   ├─ currentDialogId: null
   └─ lastClosedOrderId: abc123
   └─ newestNotShown: def456
🎯 CASE 1: Brak dialogu + brak suppress
   └─ next: def456
✅ Otwieram dialog + ACTION_START dla: def456
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Możliwe przypadki (CASE):**

| Case | Warunek | Log |
|------|---------|-----|
| **CASE 1** | Brak dialogu + brak suppress | `✅ Otwieram dialog + ACTION_START` |
| **CASE 2** | Dialog otwarty + nowe zamówienie | `✅ ACTION_RING dla nowego` |
| **CASE 3** | Suppress aktywny + nowe zamówienie | `✅ ACTION_RING (suppress mode)` |
| **CASE 4** | Żadne warunki nie spełnione | `Przyczyna: [opis]` |

### Logi Timber

Wszystkie operacje alarmu są logowane z tagami:

```kotlin
// Użycie:
Timber.tag("ALARM_DIAG").d("startAlarmService: order=${order.orderId}, ringOnly=$ringOnly")
Timber.tag("OrderAlarmService").i("Aktywuję nowy alarm dla orderId: $orderId")
```

**Filtrowanie w Logcat:**
```
tag:ALARM_DIAG OR tag:OrderAlarmService
```

### Kluczowe punkty do sprawdzenia

| Problem | Sprawdź w logach |
|---------|------------------|
| **Alarm nie uruchamia się** | Szukaj `❌ ALARM BLOCKED` → sprawdź powód (uprawnienia/suppress) |
| **Dźwięk nie gra** | `prefs.getNotificationSoundUri()` - czy URI jest poprawne? |
| **Dialog nie otwiera się** | Szukaj `🎯 CASE 4` → sprawdź przyczynę blokady |
| **Alarm gra w kółko po akceptacji** | `AcceptOrderSheetContent` - czy `ACTION_STOP_ALARM` jest wywoływane? |
| **Fullscreen nie działa** | `setFullScreenIntent(pending, true)` + uprawnienia systemowe |
| **Niewłaściwy typ alarmu** | Sprawdź `ringOnly: true/false` i `action: ACTION_START/ACTION_RING` |

### Śledzenie przepływu alarmu

**1. Sprawdź czy zamówienie wpada do kolejki:**
```
tag:OrderAlarmService | grep "queue.size"
```

**2. Sprawdź decyzję o uruchomieniu:**
```
tag:OrderAlarmService | grep "CASE"
```

**3. Sprawdź czy alarm faktycznie wystartował:**
```
tag:ALARM_DIAG | grep "ALARM CALL"
```

**4. Sprawdź blokady:**
```
tag:ALARM_DIAG | grep "BLOCKED"
```

### Sprawdzanie stanu alarmu

```kotlin
// W OrderAlarmService.kt
Timber.d("currentNotificationId: $currentNotificationId")
Timber.d("player?.isPlaying: ${player?.isPlaying}")

// W OrdersViewModel.kt
Timber.d("pendingOrdersQueue.size: ${pendingOrdersQueue.value.size}")
Timber.d("_suppressAutoOpen: ${_suppressAutoOpen.value}")
Timber.d("orderToShowInDialog: ${_uiState.value.orderToShowInDialog?.orderId}")
```

---

## 📊 Diagram sekwencji - Pełny przepływ

```
┌─────────┐    ┌──────────────┐    ┌───────────────┐    ┌──────────────┐    ┌────────────┐
│ WebSocket│    │SocketHandler │    │OrdersViewModel│    │AlarmService  │    │ User       │
└────┬────┘    └──────┬───────┘    └───────┬───────┘    └──────┬───────┘    └─────┬──────┘
     │                │                     │                   │                   │
     │ ORDER_CREATED  │                     │                   │                   │
     │───────────────>│                     │                   │                   │
     │                │ parsePayload()      │                   │                   │
     │                │ status==PROCESSING? │                   │                   │
     │                │ insertOrUpdateOrder()│                   │                   │
     │                │ emitOrder()         │                   │                   │
     │                │────────────────────>│                   │                   │
     │                │                     │ observePendingQueue()                 │
     │                │                     │ NOWE PROCESSING!  │                   │
     │                │                     │ startAlarmService(ACTION_START)       │
     │                │                     │──────────────────>│                   │
     │                │                     │                   │ startForeground() │
     │                │                     │                   │ startAlarmSound() │
     │                │                     │                   │ 🔊 DZWONEK GRA   │
     │                │                     │ triggerOpenDialog()                   │
     │                │                     │ ─ ─ ─ ─ ─ ─ ─ ─ ─│─ ─ ─ ─ ─ ─ ─ ─ >│
     │                │                     │                   │                   │ DIALOG
     │                │                     │                   │                   │ OTWIERA
     │                │                     │                   │                   │
     │                │                     │                   │                   │ Klik "15 min"
     │                │                     │                   │<─ ─ ─ ─ ─ ─ ─ ─ ─│
     │                │                     │                   │ ACTION_STOP_ALARM │
     │                │                     │                   │ stopAlarmSound()  │
     │                │                     │                   │ 🔇 CISZA          │
     │                │                     │<──────────────────│                   │
     │                │                     │ dismissDialog()   │                   │
     │                │                     │ Kolejne w queue?  │                   │
     │                │                     │ TAK → ACTION_RING │                   │
     │                │                     │──────────────────>│                   │
     │                │                     │                   │ restartAlarmSound()│
     │                │                     │                   │ 🔊 DZWONEK ZNOWU  │
```

---

## ⚙️ Konfiguracja

### Preferencje użytkownika

| Preferencja | Klucz | Domyślna wartość | Wpływ na alarm |
|-------------|-------|------------------|----------------|
| Dźwięk powiadomienia | `notification_sound_uri` | `order_iphone.mp3` | URI dźwięku alarmu |
| Dźwięk per-typ | `notification_sound_order_alarm` | `null` | Nadpisuje globalny |
| Auto-drukowanie | `auto_print_enabled` | `false` | Drukuje po otrzymaniu PROCESSING |
| Auto-druk po akceptacji | `auto_print_accepted_enabled` | `false` | Drukuje po wybraniu czasu |

### Wymagane uprawnienia

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
<uses-permission android:name="android.permission.VIBRATE" />
```

---

## 📝 Podsumowanie

### ✅ Alarm uruchamia się gdy:
1. Otrzymano zamówienie ze statusem **PROCESSING** przez WebSocket
2. Aplikacja ma uprawnienia `POST_NOTIFICATIONS`
3. Nie ma aktywnego alarmu dla innego zamówienia (lub używany jest `ACTION_RING`)
4. Użytkownik nie jest kurierem (`UserRole != COURIER`)
5. Nie jesteśmy w 700ms "dead zone" po zamknięciu dialogu (dla `ACTION_START`)

### 🛑 Alarm zatrzymuje się gdy:
1. Użytkownik **akceptuje zamówienie** (wybiera czas przygotowania)
2. Użytkownik **zmienia status** zamówienia (W drodze, Ukończone)
3. Użytkownik **przypisuje kuriera** (wewnętrznego lub zewnętrznego)
4. Użytkownik klika **"Wycisz"** w powiadomieniu
5. Kolejka zamówień **PROCESSING jest pusta**
6. System zabija proces i nie ma zamówień do kontynuacji

### 🔄 Alarm restartuje dźwięk (ACTION_RING) gdy:
1. Dialog jest otwarty, a **wpada nowe zamówienie**
2. Jesteśmy w oknie **suppress** (700ms), a wpada nowe zamówienie
3. Użytkownik zamknął dialog, a **są kolejne zamówienia w kolejce**

---

**Ostatnia aktualizacja:** 2026-01-13  
**Wersja dokumentu:** 1.0  
**Autor:** System Documentation Generator

