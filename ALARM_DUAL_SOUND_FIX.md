# 🔊 ROZWIĄZANIE: Podwójny Dźwięk Alarmu - Kanał Powiadomień

## 🎯 Problem

**Opis:**
- Alarm dla nowego zamówienia odtwarzał **DWA DŹWIĘKI jednocześnie**
- **Dźwięk #1:** Odtwarzany w pętli (loop) - **TO BYŁO OK** ✅
- **Dźwięk #2:** Odtwarzany JEDEN RAZ - **TO BYŁ PROBLEM** ❌

## 🔍 Przyczyna

### Konflikt Kanałów Powiadomień

Android ma system **kanałów powiadomień** (Notification Channels). Każdy kanał może mieć:
- Własny dźwięk
- Własne wibracje
- Własny poziom ważności

**Problem:** Aplikacja miała **DWA MIEJSCA**, które tworzyły TEN SAM kanał z **RÓŻNYMI ustawieniami**:

### 1. NotificationHelper.kt (tworzony PIERWSZY w SocketService.onCreate())
```kotlin
private fun createOrderAlarmChannel(context: Context, manager: NotificationManager) {
    val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.order_iphone}")
    val channel = NotificationChannel(
        "order_alarm_channel_v3",  // ← ID kanału
        "Alarmy zamówień",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        setSound(soundUri, attrs)  // ❌ KANAŁ Z DŹWIĘKIEM!
    }
    manager.createNotificationChannel(channel)
}
```

### 2. OrderAlarmService.kt (tworzony PÓŹNIEJ w OrderAlarmService.onCreate())
```kotlin
private fun createAlarmNotificationChannel() {
    val channel = NotificationChannel(
        "order_alarm_channel_v3",  // ← TO SAMO ID!
        "Alarmy zamówień",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        setSound(null, attrs)  // ✅ KANAŁ BEZ DŹWIĘKU
    }
    manager.createNotificationChannel(channel)
}
```

### ⚠️ Zasada Androida

**Android ignoruje próbę ponownego utworzenia kanału o tym samym ID!**

**Kolejność:**
1. Aplikacja startuje → SocketService.onCreate() → NotificationHelper.createChannels()
2. NotificationHelper tworzy kanał `"order_alarm_channel_v3"` **Z DŹWIĘKIEM**
3. Przychodzi zamówienie → OrderAlarmService startuje
4. OrderAlarmService próbuje utworzyć kanał `"order_alarm_channel_v3"` **BEZ DŹWIĘKU**
5. **Android ignoruje** - kanał już istnieje!

### Rezultat

Gdy OrderAlarmService wysyła powiadomienie przez `notify()`:

```kotlin
NotificationManagerCompat.from(this).notify(currentNotificationId!!, notif)
```

**Android odtwarza dźwięk kanału (order_iphone.mp3) JEDEN RAZ**, ponieważ:
- Kanał ma ustawiony dźwięk (z NotificationHelper)
- `.setOnlyAlertOnce(true)` dotyczy tylko WIELOKROTNEGO notify() dla TEGO SAMEGO powiadomienia

**Dodatkowo** OrderAlarmService odtwarza dźwięk przez MediaPlayer w pętli:
```kotlin
player = MediaPlayer().apply {
    setDataSource(applicationContext, customUri)
    isLooping = true  // ← Pętla
    prepare()
    start()
}
```

## ✅ Rozwiązanie

### Usunięcie dźwięku z kanału w NotificationHelper.kt

**Plik:** `app/src/main/java/com/itsorderchat/notification/NotificationHelper.kt`

**Zmiana w linii ~234:**

**PRZED:**
```kotlin
private fun createOrderAlarmChannel(context: Context, manager: NotificationManager) {
    val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.order_iphone}")
    val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    val channel = NotificationChannel(
        ORDER_ALARM_CHANNEL_ID,
        ORDER_ALARM_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = ORDER_ALARM_CHANNEL_DESC
        enableVibration(true)
        setSound(soundUri, attrs)  // ❌ PROBLEM!
    }
    manager.createNotificationChannel(channel)
}
```

**PO:**
```kotlin
private fun createOrderAlarmChannel(context: Context, manager: NotificationManager) {
    // ❌ USUNIĘTO DŹWIĘK - odtwarzany przez MediaPlayer w OrderAlarmService
    // val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.order_iphone}")
    val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    val channel = NotificationChannel(
        ORDER_ALARM_CHANNEL_ID,
        ORDER_ALARM_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = ORDER_ALARM_CHANNEL_DESC
        enableVibration(true)
        setSound(null, attrs)  // ✅ BRAK DŹWIĘKU - odtwarzany przez MediaPlayer
    }
    manager.createNotificationChannel(channel)
}
```

## 📊 Jak to działa teraz?

### 1. Start aplikacji
```
SocketService.onCreate()
  └─ NotificationHelper.createChannels()
      └─ createOrderAlarmChannel()
          └─ Tworzy kanał "order_alarm_channel_v3" BEZ DŹWIĘKU ✅
```

### 2. Nowe zamówienie
```
OrderAlarmService.onStartCommand()
  ├─ createAlarmNotificationChannel()
  │   └─ Próbuje utworzyć kanał (ignorowane - już istnieje)
  │
  ├─ startForeground(notificationId, notification)
  │   └─ Android wyświetla powiadomienie
  │   └─ BRAK DŹWIĘKU z kanału ✅
  │
  └─ startAlarmSound()
      └─ MediaPlayer odtwarza dźwięk w pętli ✅
```

### 3. Rezultat
- **Jeden dźwięk** (MediaPlayer) odtwarzany w pętli ✅
- **Brak dźwięku** z kanału powiadomień ✅
- **Wibracje** nadal działają (ustawione w kanale) ✅

## ⚠️ Uwaga dla użytkowników

### Jeśli użytkownik już ma zainstalowaną aplikację

Android **zapamiętuje** ustawienia kanałów! Jeśli kanał `"order_alarm_channel_v3"` został już utworzony z dźwiękiem, to zmiana w kodzie **NIE ZADZIAŁA** do momentu:

**Opcja 1: Pełna reinstalacja aplikacji**
```
1. Odinstaluj aplikację
2. Zainstaluj nową wersję
```

**Opcja 2: Wyczyść dane aplikacji**
```
Ustawienia → Aplikacje → Twoja Aplikacja → Pamięć → Wyczyść dane
```

**Opcja 3: Zmień ID kanału (zalecane dla update)**
```kotlin
const val ORDER_ALARM_CHANNEL_ID = "order_alarm_channel_v4"  // ← Nowa wersja
```

## 🎯 Weryfikacja Poprawki

### Jak sprawdzić czy problem został rozwiązany?

1. **Reinstaluj aplikację** (lub wyczyść dane)
2. **Zaloguj się i poczekaj na zamówienie**
3. **Słuchaj dźwięku:**
   - ✅ **POWINIEN BYĆ:** Jeden dźwięk w pętli
   - ❌ **NIE POWINNO BYĆ:** Dwóch niezależnych dźwięków

4. **Sprawdź logi** z tagiem "ALARM START":
```
Logcat filter: ALARM START
```

**Powinieneś zobaczyć:**
```
🔊 [OrderAlarmService] startAlarmSound()
   ├─ isAlarmActive: false
   ├─ player?.isPlaying: null
   ...
✅ Odtwarzanie alarmu rozpoczęte (loop)!
```

**NIE powinieneś zobaczyć:**
- Wielokrotnego wywołania `startAlarmSound()`
- Wielu instancji MediaPlayera

## 📝 Podsumowanie Zmian

### Zmodyfikowane pliki:
1. ✅ `NotificationHelper.kt` - usunięcie dźwięku z kanału ORDER_ALARM_CHANNEL_ID

### Dodane logi diagnostyczne (wcześniej):
1. ✅ `OrdersViewModel.kt` - tag "ALARM START"
2. ✅ `SocketStaffEventsHandler.kt` - tag "ALARM START"  
3. ✅ `OrderAlarmService.kt` - tag "ALARM START"
4. ✅ `SocketService.kt` - tag "ALARM START"

### Utworzone dokumenty:
1. ✅ `ALARM_START_DIAGNOSTIC_LOGS.md` - Przewodnik po logach diagnostycznych
2. ✅ `ALARM_SYSTEMS_OVERVIEW.md` - Przegląd systemów alarmowych
3. ✅ `ALARM_SOURCES_DIAGNOSIS.md` - Diagnoza źródeł alarmów
4. ✅ `ALARM_DUAL_SOUND_FIX.md` - **TEN DOKUMENT** - Rozwiązanie podwójnego dźwięku

## 🔧 Techniczne Szczegóły

### Dlaczego MediaPlayer zamiast dźwięku kanału?

**Zalety MediaPlayer:**
- ✅ Pełna kontrola nad odtwarzaniem (start, stop, pause, volume)
- ✅ Możliwość odtwarzania w pętli (isLooping = true)
- ✅ Natychmiastowe zatrzymanie bez opóźnień
- ✅ Brak zależności od ustawień systemowych użytkownika
- ✅ Jeden dźwięk dla wielu powiadomień (nie trzeba tworzyć nowych kanałów)

**Wady dźwięku kanału:**
- ❌ Odtwarzany ZAWSZE gdy wysyłane jest powiadomienie przez notify()
- ❌ Brak kontroli nad zatrzymaniem (trzeba usunąć powiadomienie)
- ❌ Odtwarzany tylko JEDEN RAZ (chyba że `.setOnlyAlertOnce(false)`)
- ❌ Użytkownik może wyłączyć dźwięk w ustawieniach systemowych
- ❌ Trudno zsynchronizować z logiką aplikacji

### Dlaczego `.setOnlyAlertOnce(true)` nie pomogło?

`.setOnlyAlertOnce(true)` dotyczy tylko **WIELOKROTNEGO** wywołania `notify()` dla **TEGO SAMEGO** powiadomienia (tego samego ID).

**Przykład:**
```kotlin
// PIERWSZE wywołanie - dźwięk ZAGRANY
notify(100, notification)

// DRUGIE wywołanie - dźwięk POMINIĘTY (dzięki setOnlyAlertOnce)
notify(100, notification)
```

**Ale:**
```kotlin
// PIERWSZE wywołanie - dźwięk ZAGRANY
notify(100, notification)

// Nowe zamówienie, NOWE ID - dźwięk ZAGRANY
notify(101, notification)
```

W przypadku OrderAlarmService:
- Każde zamówienie ma **UNIKALNE** notificationId (z hashCode orderId)
- Więc `.setOnlyAlertOnce(true)` **NIE BLOKUJE** dźwięku

---

**Data rozwiązania:** 2026-02-12  
**Wersja aplikacji:** v1.7.x  
**Status:** ✅ Problem rozwiązany - wymagana reinstalacja lub zmiana ID kanału

