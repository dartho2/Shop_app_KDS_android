# 🔔 Przegląd Systemów Alarmowych w Aplikacji

## 📊 Dwa Niezależne Systemy Alarmów

Aplikacja wykorzystuje **DWA ODDZIELNE SERWISY** do odtwarzania dźwięków alarmowych, każdy z innym celem.

---

## 1️⃣ OrderAlarmService - Alarmy Zamówień

### 🎯 Przeznaczenie
Powiadomienie kelnerów/kuchni o **nowych zamówieniach** wymagających obsługi.

### 📂 Lokalizacja
`app/src/main/java/com/itsorderchat/service/OrderAlarmService.kt`

### 🔊 Typ Dźwięku
**Customowy** - wybrany przez użytkownika w ustawieniach aplikacji:
- Domyślnie: `R.raw.order_iphone`
- Można zmienić w Ustawieniach → Powiadomienia → Dźwięk alarmu zamówienia

### 🚀 Wyzwalacze

#### A) Nowe zamówienie ze statusem "PROCESSING"
```kotlin
// Źródło: OrdersViewModel.observePendingOrdersQueue()
// Warunki:
// - Nowe zamówienie weszło do kolejki
// - Dialog nie jest otwarty
// - Flaga suppress nieaktywna (700ms po zamknięciu dialogu)
startAlarmService(order, ringOnly = false) // ACTION_START
```

#### B) Dodatkowe zamówienie podczas otwartego dialogu
```kotlin
// Źródło: OrdersViewModel.observePendingOrdersQueue()
// Warunki:
// - Dialog już otwarty dla innego zamówienia
// - Wpada kolejne nowe zamówienie
startAlarmService(newestOrder, ringOnly = true) // ACTION_RING
```

#### C) Przypomnienie z serwera (reminder)
```kotlin
// Źródło: SocketStaffEventsHandler.handleOrderReminderAlarm()
// Warunki:
// - Serwer wysyła event "ORDER_REMINDER_ALARM"
// - Zamówienie nie zostało obsłużone w czasie X
startAlarmServiceSafely(orderId, orderJson)
```

### 🎵 Charakterystyka MediaPlayera
- **Usage:** `AudioAttributes.USAGE_ALARM`
- **Loop:** `true` (odtwarza w pętli do wyciszenia)
- **Volume:** Kanał ALARM (niezależny od głośności mediów)

### 🛑 Zatrzymanie Alarmu
- Kliknięcie przycisku "Wycisz" w powiadomieniu
- Zamknięcie dialogu zamówienia
- Zaakceptowanie/odrzucenie zamówienia
- Wywołanie `stopAlarmService()` w OrdersViewModel

### 📱 Powiadomienie
```
Tytuł: "Nowe zamówienie"
Treść: "Dotknij, aby obsłużyć"
Kanał: order_alarm_channel_v3
Priorytet: HIGH
Full-screen intent: TAK (otwiera dialog na zablokowanym ekranie)
```

### 🔒 Zabezpieczenia Przed Duplikacją
- **Flag `isAlarmActive`** - blokuje start dźwięku jeśli już gra
- **Debounce 500ms** - ignoruje powtórne wywołania dla tego samego orderId w ciągu 500ms
- **Guard `player?.isPlaying`** - sprawdza czy MediaPlayer już odtwarza

---

## 2️⃣ SocketService - Alarmy Połączenia/Sesji

### 🎯 Przeznaczenie
Ostrzeżenie o **problemach z połączeniem** WebSocket lub **wygaśnięciu sesji**.

### 📂 Lokalizacja
`app/src/main/java/com/itsorderchat/service/SocketService.kt`

### 🔊 Typ Dźwięku
**Systemowy alarm** - domyślny dźwięk alarmu z systemu Android:
```kotlin
RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
```
Nie można go zmienić w ustawieniach aplikacji.

### 🚀 Wyzwalacze

#### A) Utrata połączenia WebSocket > 30 sekund
```kotlin
// Źródło: SocketService.scheduleWsDisconnectAlarm()
// Warunki:
// - WebSocket disconnected
// - Przez 30 sekund brak reconnect
// - SocketManager.isConnected() == false

wsAlarmJob = serviceScope.launch {
    delay(30_000) // Debounce
    if (!SocketManager.isConnected()) {
        NotificationHelper.showWsDisconnectAlert()
        startAlarmSound()
    }
}
```

#### B) Wygaśnięcie sesji (401 Unauthorized)
```kotlin
// Źródło: SocketService.handleAuthExpiry()
// Warunki:
// - Token refresh zwrócił 401
// - RefreshResult.SessionExpired

when (result) {
    is RefreshResult.SessionExpired -> {
        startAlarmSound()
        NotificationHelper.showSessionExpiredAlert()
        sendBroadcast(ACTION_FORCE_LOGOUT)
    }
}
```

### 🎵 Charakterystyka MediaPlayera
- **Usage:** `AudioAttributes.USAGE_ALARM`
- **Loop:** `true` (odtwarza w pętli)
- **Volume:** Kanał ALARM (niezależny od głośności mediów)

### 🛑 Zatrzymanie Alarmu
- **Dla WebSocket disconnect:**
  - Automatycznie po reconnect (SocketManager.onConnect)
  - `cancelWsDisconnectAlarm()` → `stopAlarmSound()`

- **Dla session expired:**
  - Użytkownik musi się wylogować i zalogować ponownie
  - Alarm może grać aż do restartu aplikacji

### 📱 Powiadomienia

#### WebSocket Disconnect:
```
Tytuł: "Brak połączenia z serwerem"
Treść: "Sprawdź internet/Wi-Fi/zasilanie. Alarm wyłączy się po połączeniu."
Kanał: ws_alert_channel
Priorytet: HIGH
Ongoing: TAK (nie można odrzucić)
Flag: FLAG_INSISTENT
```

#### Session Expired:
```
Tytuł: "Sesja wygasła"
Treść: "Musisz zalogować się ponownie."
Kanał: alerts_channel
Priorytet: MAX
Full-screen intent: TAK (otwiera LoginActivity)
```

---

## 🆚 Porównanie

| Właściwość | OrderAlarmService | SocketService |
|------------|-------------------|---------------|
| **Cel** | Nowe zamówienia | Problemy z połączeniem/sesją |
| **Dźwięk** | Customowy (ustawienia) | Systemowy alarm |
| **Możliwość zmiany dźwięku** | ✅ TAK | ❌ NIE |
| **Trigger** | WebSocket event ORDER_PROCESSING | WebSocket disconnect / 401 |
| **Delay** | Natychmiast | 30s (dla disconnect) |
| **Auto-stop** | Po obsłużeniu zamówienia | Po reconnect (disconnect) / Nigdy (401) |
| **Full-screen** | TAK (dialog zamówienia) | TAK (LoginActivity dla 401) |
| **Zabezpieczenie duplikacji** | isAlarmActive + debounce | Brak (tylko wsAlarmJob.isActive) |

---

## ⚠️ Możliwe Konflikty

### Scenariusz 1: Utrata połączenia podczas nowego zamówienia
```
T+0s:   Nowe zamówienie → OrderAlarmService.startAlarmSound()
        ┌─ DŹWIĘK 1: order_iphone (loop)
        
T+1s:   WebSocket disconnect
        ┌─ scheduleWsDisconnectAlarm() rozpoczyna 30s countdown

T+31s:  Timeout → SocketService.startAlarmSound()
        ┌─ DŹWIĘK 2: systemowy alarm (loop)
        
WYNIK: DWA DŹWIĘKI grają jednocześnie!
```

**Rozwiązanie:**
- Jeśli słyszysz nakładające się dźwięki, sprawdź:
  1. Czy masz problem z siecią (WiFi, Router)
  2. Czy powiadomienie "Brak połączenia z serwerem" jest aktywne
  3. W logach z tagiem `ALARM START` szukaj:
     - `[OrderAlarmService] startAlarmSound()`
     - `[SocketService] startAlarmSound() - WS DISCONNECT ALARM`

### Scenariusz 2: Wygaśnięcie sesji podczas zamówienia
```
T+0s:   Nowe zamówienie → OrderAlarmService.startAlarmSound()
        ┌─ DŹWIĘK 1: order_iphone (loop)

T+2s:   Token expired (401) → SocketService.startAlarmSound()
        ┌─ DŹWIĘK 2: systemowy alarm (loop)
        
WYNIK: DWA DŹWIĘKI grają jednocześnie!
```

**Rozwiązanie:**
- Sprawdź czy nie ma problemu z czasem życia tokena
- Sprawdź logi autentykacji
- Powiadomienie "Sesja wygasła" będzie widoczne

---

## 🔍 Diagnostyka - Jak Rozpoznać Źródło Alarmu?

### Metoda 1: Filtr Logcat
```bash
# Tylko alarmy zamówień:
adb logcat -s "ALARM START:*" | grep "OrderAlarmService"

# Tylko alarmy WebSocket:
adb logcat -s "ALARM START:*" | grep "SocketService"
```

### Metoda 2: Sprawdź powiadomienia
- Jeśli widzisz **"Nowe zamówienie"** → OrderAlarmService
- Jeśli widzisz **"Brak połączenia"** → SocketService (disconnect)
- Jeśli widzisz **"Sesja wygasła"** → SocketService (401)

### Metoda 3: Posłuchaj dźwięku
- **Krótki, melodyjny (iPhone-like)** → OrderAlarmService (order_iphone.mp3)
- **Głośny, przenikliwy (systemowy)** → SocketService (system alarm)

### Metoda 4: Sprawdź czas odtwarzania
- **Natychmiast po zamówieniu** → OrderAlarmService
- **30s po utracie połączenia** → SocketService (disconnect)
- **Natychmiast po 401** → SocketService (session expired)

---

## 🛠️ Potencjalne Optymalizacje

### Opcja 1: Priorytet OrderAlarmService
Jeśli OrderAlarmService już gra, nie uruchamiaj SocketService alarm:
```kotlin
// W SocketService.startAlarmSound()
if (OrderAlarmService.isAlarmActive()) {
    Timber.d("OrderAlarmService już gra - pomijam SocketService alarm")
    return
}
```

### Opcja 2: Wyłącz alarm sesji (jeśli niepożądany)
Jeśli alarm przy wygaśnięciu sesji jest zbędny:
```kotlin
// W SocketService.handleAuthExpiry()
is RefreshResult.SessionExpired -> {
    // startAlarmSound() ← ZAKOMENTUJ
    NotificationHelper.showSessionExpiredAlert()
    sendBroadcast(ACTION_FORCE_LOGOUT)
}
```

### Opcja 3: Skróć timeout WebSocket alarm
Jeśli 30s to za długo:
```kotlin
// W SocketService.scheduleWsDisconnectAlarm()
delay(10_000) // zmień z 30_000 na 10_000 (10s)
```

---

## 📝 Podsumowanie

✅ **OrderAlarmService** = Główny system alarmów dla zamówień  
✅ **SocketService** = System awaryjny dla problemów z połączeniem  
⚠️ **Oba mogą grać jednocześnie** w przypadku problemów sieciowych podczas nowego zamówienia  
🔍 **Użyj logów z tagiem "ALARM START"** do zdiagnozowania źródła dźwięku  

---

**Data utworzenia:** 2026-02-12  
**Wersja aplikacji:** v1.7.x  
**Status:** ✅ Dokumentacja kompletna

