# 🔊 Diagnostyczne Logi "ALARM START" - Implementacja

## 📋 Podsumowanie

Dodano szczegółowe logi diagnostyczne z tagiem **"ALARM START"** we wszystkich kluczowych punktach odpowiedzialnych za uruchamianie alarmu powiadomienia o nowym zamówieniu.

## ⚠️ WAŻNE: Dwa Niezależne Systemy Alarmów

Aplikacja posiada **DWA NIEZALEŻNE SERWISY** odtwarzające dźwięki alarmowe:

### 1. **OrderAlarmService** - Alarm dla NOWYCH ZAMÓWIEŃ
- **Plik:** `service/OrderAlarmService.kt`
- **Cel:** Powiadomienie o nowych zamówieniach
- **Dźwięk:** Customowy (z ustawień użytkownika)
- **Zarządzanie:** `OrdersViewModel`, `SocketStaffEventsHandler`

### 2. **SocketService** - Alarm dla UTRATY POŁĄCZENIA
- **Plik:** `service/SocketService.kt`  
- **Cel:** Powiadomienie o utracie połączenia z WebSocket lub wygaśnięciu sesji
- **Dźwięk:** Domyślny alarm systemowy (RingtoneManager.TYPE_ALARM)
- **Wyzwalacze:**
  - Utrata połączenia WebSocket > 30 sekund
  - Wygaśnięcie sesji (401 Unauthorized)

## 🎯 Cel

Umożliwienie zdiagnozowania problemu z **podwójnym odtwarzaniem dźwięku alarmu** poprzez:
- Śledzenie każdego wywołania alarmu
- Identyfikację źródła duplikacji
- Wykrycie wielokrotnej rejestracji listenerów
- Sprawdzenie race conditions

## 📍 Miejsca Implementacji

### 1. **OrdersViewModel.startAlarmService()**
**Plik:** `ui/order/OrdersViewModel.kt`

**Co loguje:**
- ✅ Wszystkie szczegóły zamówienia (ID, numer, status, data utworzenia)
- ✅ Typ akcji (ACTION_START vs ACTION_RING)
- ✅ Stan flagi `ringOnly`
- ✅ Stan flagi `suppress` (okno suppress po zamknięciu dialogu)
- ✅ Uprawnienia POST_NOTIFICATIONS
- ✅ Stan aplikacji (foreground vs background)
- ✅ Stack trace wywołania
- ✅ Timestamp w milisekundach
- ✅ Nazwa wątku

**Przykładowy log:**
```
🚨 [OrdersViewModel] ALARM CALL
   ├─ orderId: 698d1885fdf98125647214a7
   ├─ orderNumber: #1234
   ├─ ringOnly: false
   ├─ orderStatus: PROCESSING
   ├─ createdAt: 2026-02-12T00:04:49.032Z
   ├─ action: ACTION_START
   ├─ Thread: main
   ├─ Stack trace:
   │  0: dalvik.system.VMStack.getThreadStackTrace
   │  1: java.lang.Thread.getStackTrace
   │  2: com.itsorderchat.ui.order.OrdersViewModel.startAlarmService
   └─ Timestamp: 1739318689033
```

### 2. **SocketStaffEventsHandler.startAlarmServiceSafely()**
**Plik:** `service/SocketStaffEventsHandler.kt`

**Co loguje:**
- ✅ Order ID
- ✅ Metoda uruchomienia (startService vs startForegroundService)
- ✅ Stack trace wywołania (pokazuje skąd pochodzi wywołanie)
- ✅ Timestamp w milisekundach
- ✅ Nazwa wątku
- ✅ Status powodzenia/błędu

**Przykładowy log:**
```
🚨 [SocketStaffEventsHandler] Uruchamiam OrderAlarmService
   ├─ orderId: 698d1885fdf98125647214a7
   ├─ Thread: DefaultDispatcher-worker-3
   ├─ Stack trace:
   │  0: dalvik.system.VMStack.getThreadStackTrace
   │  1: java.lang.Thread.getStackTrace
   │  2: com.itsorderchat.service.SocketStaffEventsHandler.startAlarmServiceSafely
   │  3: com.itsorderchat.service.SocketStaffEventsHandler.handleOrderReminderAlarm
   └─ Timestamp: 1739318689033
   → startForegroundService()
✅ OrderAlarmService uruchomiony pomyślnie!
```

### 3. **SocketStaffEventsHandler.handleOrderReminderAlarm()**
**Plik:** `service/SocketStaffEventsHandler.kt`

**Co loguje:**
- ✅ Order ID
- ✅ Numer przypomnienia
- ✅ Timestamp
- ✅ Nazwa wątku

**Przykładowy log:**
```
🔔 [SocketStaffEventsHandler] REMINDER ALARM RECEIVED!
   ├─ orderId: 698d1885fdf98125647214a7
   ├─ reminderNumber: 1
   ├─ Thread: DefaultDispatcher-worker-3
   └─ Timestamp: 1739318689033
```

### 4. **OrderAlarmService.onStartCommand()**
**Plik:** `service/OrderAlarmService.kt`

**Co loguje:**
- ✅ Akcja intencji (ACTION_START, ACTION_RING, ACTION_STOP)
- ✅ Start ID
- ✅ Bieżące ID notyfikacji
- ✅ Bieżące Order ID
- ✅ Stan `isAlarmActive`
- ✅ Stan `player?.isPlaying`
- ✅ Timestamp
- ✅ Nazwa wątku

**Przykładowy log:**
```
🎯 [OrderAlarmService] onStartCommand
   ├─ action: ACTION_START
   ├─ startId: 1
   ├─ currentNotificationId: null
   ├─ currentOrderId: null
   ├─ isAlarmActive: false
   ├─ player?.isPlaying: null
   ├─ Thread: main
   └─ Timestamp: 1739318689033
```

### 5. **OrderAlarmService.startAlarmSound()**
**Plik:** `service/OrderAlarmService.kt`

**Co loguje:**
- ✅ Stan `isAlarmActive`
- ✅ Stan `player?.isPlaying`
- ✅ Bieżące Order ID
- ✅ Wybrany plik dźwiękowy
- ✅ Timestamp
- ✅ Nazwa wątku
- ✅ Status blokady (jeśli alarm już gra)

**Przykładowy log:**
```
🔊 [OrderAlarmService] startAlarmSound()
   ├─ isAlarmActive: false
   ├─ player?.isPlaying: null
   ├─ currentOrderId: 698d1885fdf98125647214a7
   ├─ Thread: main
   └─ Timestamp: 1739318689033
🎵 Wybrany dźwięk: android.resource://com.itsorderchat/2131886080
✅ Odtwarzanie alarmu rozpoczęte (loop)!
```

**Lub w przypadku blokady:**
```
🔊 [OrderAlarmService] startAlarmSound()
   ├─ isAlarmActive: true
   ├─ player?.isPlaying: true
   ├─ currentOrderId: 698d1885fdf98125647214a7
   ├─ Thread: main
   └─ Timestamp: 1739318689033
❌ ALARM BLOCKED: Alarm już aktywny i gra!
```

### 6. **OrderAlarmService.restartAlarmSound()**
**Plik:** `service/OrderAlarmService.kt`

**Co loguje:**
- ✅ Stan playera (null vs exists)
- ✅ Stan `player?.isPlaying`
- ✅ Bieżące Order ID
- ✅ Akcja wykonana (seekTo, start, fallback)
- ✅ Timestamp
- ✅ Nazwa wątku

**Przykładowy log:**
```
🔄 [OrderAlarmService] restartAlarmSound()
   ├─ player: exists
   ├─ player?.isPlaying: true
   ├─ currentOrderId: 698d1885fdf98125647214a7
   ├─ Thread: main
   └─ Timestamp: 1739318689033
   → Player już gra, tylko seekTo(0)
✅ restartAlarmSound zakończony
```

### 7. **SocketService.startAlarmSound()** ⚠️ DRUGI ALARM!
**Plik:** `service/SocketService.kt`

**Co loguje:**
- ✅ Stan `alarmPlayer`
- ✅ Stan połączenia WebSocket (`isConnected`)
- ✅ Stack trace wywołania
- ✅ Timestamp
- ✅ Nazwa wątku

**Przykładowy log:**
```
🔊 [SocketService] startAlarmSound() - WS DISCONNECT ALARM
   ├─ alarmPlayer: null
   ├─ isConnected: false
   ├─ Thread: DefaultDispatcher-worker-2
   ├─ Stack trace:
   │  0: dalvik.system.VMStack.getThreadStackTrace
   │  1: java.lang.Thread.getStackTrace
   │  2: com.itsorderchat.service.SocketService.startAlarmSound
   │  3: com.itsorderchat.service.SocketService$scheduleWsDisconnectAlarm$1.invokeSuspend
   └─ Timestamp: 1739318689033
✅ [SocketService] Alarm WS DISCONNECT uruchomiony!
```

### 8. **SocketService.scheduleWsDisconnectAlarm()**
**Plik:** `service/SocketService.kt`

**Co loguje:**
- ✅ Start 30-sekundowego odliczania
- ✅ Informacja o uruchomieniu alarmu po timeout

**Przykładowy log:**
```
📡 [SocketService] scheduleWsDisconnectAlarm() - Rozpoczynam 30s countdown
⚠️ [SocketService] 30s minęło i brak połączenia - uruchamiam alarm WS!
```

### 9. **SocketService.handleAuthExpiry()**
**Plik:** `service/SocketService.kt`

**Przykładowy log:**
```
⚠️ [SocketService] SESJA WYGASŁA - uruchamiam alarm!
```

## 🔍 Jak Użyć Logów Do Diagnozy

### 1. **Filtrowanie logów**
W Logcat ustaw filtr na tag: `ALARM START`

### 2. **Sprawdzenie sekwencji wywołań**
Obserwuj kolejność wywołań i timestampy:
```
1. [OrdersViewModel] ALARM CALL (Timestamp: 1000)
2. [OrderAlarmService] onStartCommand (Timestamp: 1001)
3. [OrderAlarmService] startAlarmSound() (Timestamp: 1002)
✅ Odtwarzanie alarmu rozpoczęte
```

### 3. **Identyfikacja duplikacji**
Jeśli zobaczysz dwa wywołania w krótkim czasie:
```
1. [OrdersViewModel] ALARM CALL (Timestamp: 1000)
2. [OrdersViewModel] ALARM CALL (Timestamp: 1050)  ← DUPLIKACJA!
```

Sprawdź **Stack trace** - pokaże skąd pochodzi wywołanie:
```
Stack trace:
│  2: com.itsorderchat.ui.order.OrdersViewModel.startAlarmService
│  3: com.itsorderchat.ui.order.OrdersViewModel.observePendingOrdersQueue$lambda$42
│  4: kotlinx.coroutines.flow.FlowKt__TransformKt$onEach$1.invokeSuspend
```

### 4. **Sprawdzenie blokady**
Jeśli zobaczysz:
```
❌ ALARM BLOCKED: Alarm już aktywny i gra!
```
To znaczy, że mechanizm zabezpieczający działa poprawnie.

### 5. **Analiza wątków**
Sprawdź czy wywołania pochodzą z różnych wątków:
```
Thread: main
Thread: DefaultDispatcher-worker-3
```

## 🛠️ Potencjalne Przyczyny Duplikacji

Na podstawie logów możesz zidentyfikować:

### 1. **Wielokrotna emisja z serwera**
```
[SocketStaffEventsHandler] REMINDER ALARM RECEIVED!
   └─ Timestamp: 1000
[SocketStaffEventsHandler] REMINDER ALARM RECEIVED!
   └─ Timestamp: 1050
```
**Rozwiązanie:** Problem po stronie backendu

### 2. **Wielokrotna rejestracja listenera WebSocket**
```
[SocketStaffEventsHandler] REMINDER ALARM RECEIVED! (Thread: worker-1)
[SocketStaffEventsHandler] REMINDER ALARM RECEIVED! (Thread: worker-2)
```
**Rozwiązanie:** Sprawdź czy listener nie jest rejestrowany przy każdej rekompozycji

### 3. **Race condition w observePendingOrdersQueue**
```
[OrdersViewModel] ALARM CALL (Timestamp: 1000)
[OrdersViewModel] ALARM CALL (Timestamp: 1001)
```
**Rozwiązanie:** Dodanie debounce w observePendingOrdersQueue (już istnieje!)

### 4. **Przejście aplikacji background/foreground**
```
[OrderAlarmService] onStartCommand (action: null, startId: 2)
```
**Rozwiązanie:** Android restartuje serwis - należy zignorować puste intenty

### 5. **Konflikt między OrderAlarmService a SocketService** ⚠️ NOWY!
```
[OrderAlarmService] startAlarmSound() (Timestamp: 1000)
[SocketService] startAlarmSound() - WS DISCONNECT ALARM (Timestamp: 1005)
```
**Przyczyna:** Jeśli jednocześnie:
- Przychodzi nowe zamówienie (OrderAlarmService)
- Utracone połączenie z WebSocket > 30s (SocketService)

**Rozwiązanie:** 
- Sprawdź status połączenia WebSocket w logach
- Sprawdź czy nie ma problemu z siecią
- SocketService uruchamia alarm dopiero po 30s bez połączenia
- Możliwe że podczas testów tracisz połączenie i otrzymujesz nowe zamówienie jednocześnie

### 6. **Wygaśnięcie sesji podczas zamówienia**
```
[OrderAlarmService] startAlarmSound() (Timestamp: 1000)
[SocketService] SESJA WYGASŁA - uruchamiam alarm! (Timestamp: 1002)
```
**Przyczyna:** Token wygasł (401) podczas przetwarzania zamówienia

**Rozwiązanie:** 
- Sprawdź logi autentykacji
- Sprawdź czy token nie jest przedwcześnie wygasany
- Możliwa dezaktywacja alarmu w SocketService dla sesji (jeśli niepożądane)

## ✅ Status Implementacji

- ✅ Logi dodane w **OrdersViewModel.startAlarmService()**
- ✅ Logi dodane w **SocketStaffEventsHandler.startAlarmServiceSafely()**
- ✅ Logi dodane w **SocketStaffEventsHandler.handleOrderReminderAlarm()**
- ✅ Logi dodane w **OrderAlarmService.onStartCommand()**
- ✅ Logi dodane w **OrderAlarmService.startAlarmSound()**
- ✅ Logi dodane w **OrderAlarmService.restartAlarmSound()**
- ✅ Logi dodane w **SocketService.startAlarmSound()** ⚠️ DRUGI ALARM!
- ✅ Logi dodane w **SocketService.scheduleWsDisconnectAlarm()**
- ✅ Logi dodane w **SocketService.handleAuthExpiry()**
- ✅ Stack trace dodany dla identyfikacji źródła wywołania
- ✅ Timestamp dodany dla analizy czasowej
- ✅ Nazwa wątku dodana dla wykrycia wielowątkowych problemów

## 🔎 Jak Zdiagnozować Źródło Alarmu

### Metoda 1: Filtruj po źródle
```
# Tylko alarmy zamówień:
Logcat filter: ALARM START & OrderAlarmService

# Tylko alarmy WebSocket:
Logcat filter: ALARM START & SocketService
```

### Metoda 2: Sprawdź typ dźwięku w logach
```
# OrderAlarmService (customowy dźwięk):
🎵 Wybrany dźwięk: android.resource://com.itsorderchat/2131886080

# SocketService (systemowy alarm):
🔊 [SocketService] startAlarmSound() - WS DISCONNECT ALARM
```

### Metoda 3: Sprawdź powiadomienia
- **OrderAlarmService:** Powiadomienie "Nowe zamówienie"
- **SocketService:** Powiadomienie "Brak połączenia z serwerem" LUB "Sesja wygasła"

## 📊 Następne Kroki

1. **Uruchom aplikację** z nowymi logami
2. **Poczekaj na nowe zamówienie**
3. **Zbierz logi z Logcat** (filtr: `ALARM START`)
4. **Przeanalizuj sekwencję wywołań** i timestampy
5. **Zidentyfikuj przyczynę duplikacji** na podstawie stack trace i wątków

## 🔧 Dodatkowe Zabezpieczenia (Już Istniejące)

- ✅ **Debounce w observePendingOrdersQueue** (500ms)
- ✅ **Flag `isAlarmActive`** w OrderAlarmService
- ✅ **Sprawdzenie `player?.isPlaying`** przed startem dźwięku
- ✅ **Guard przed podwójnym startem** w startAlarmSound()

---

**Data implementacji:** 2026-02-12
**Wersja aplikacji:** v1.7.x
**Status:** ✅ Gotowe do testowania

