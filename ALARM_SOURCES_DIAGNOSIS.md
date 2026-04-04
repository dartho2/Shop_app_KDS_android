# 🔍 DIAGNOZA: Wszystkie Źródła Dźwięków Alarmowych

## ✅ ZNALEZIONE ŹRÓDŁA ALARMÓW

### 1. **OrderAlarmService** - ALARMY ZAMÓWIEŃ
📍 **Plik:** `service/OrderAlarmService.kt`  
🎵 **Dźwięk:** Customowy (z ustawień użytkownika)  
🔊 **Player:** `MediaPlayer` (zmienna `player`)  
📊 **Status:** ✅ **MA LOGI "ALARM START"**

**Funkcje odtwarzające:**
- `startAlarmSound()` - linia ~300
- `restartAlarmSound()` - linia ~359

---

### 2. **SocketService** - ALARMY POŁĄCZENIA/SESJI ⚠️
📍 **Plik:** `service/SocketService.kt`  
🎵 **Dźwięk:** Systemowy alarm (RingtoneManager.TYPE_ALARM)  
🔊 **Player:** `MediaPlayer` (zmienna `alarmPlayer`)  
📊 **Status:** ✅ **MA LOGI "ALARM START"**

**Funkcje odtwarzające:**
- `startAlarmSound()` - linia ~143

**Wyzwalacze:**
1. Utrata połączenia WebSocket > 30s (`scheduleWsDisconnectAlarm()`)
2. Wygaśnięcie sesji - 401 (`handleAuthExpiry()`)

---

### 3. **NotificationHelper** - KANAŁY POWIADOMIEŃ
📍 **Plik:** `notification/NotificationHelper.kt`  
🎵 **Dźwięki:** Ustawione w kanałach, ale NIE odtwarzane przez MediaPlayer  
🔊 **Player:** Brak - system Android zarządza dźwiękami kanałów  
📊 **Status:** ⚠️ **Może odtwarzać dźwięki przy powiadomieniach**

**Kanały z dźwiękiem:**
1. `ORDER_ALARM_CHANNEL_ID` - linia 234: `setSound(soundUri, attrs)`
2. `WS_ALERT_CHANNEL_ID` - linia 131: `setSound(soundUri, attrs)`

**UWAGA:** Te kanały mogą odtwarzać dźwięki **niezależnie** od MediaPlayera, jeśli:
- Powiadomienie jest wysyłane z `notify()`
- Kanał ma ustawiony dźwięk
- Użytkownik nie wyłączył dźwięku w ustawieniach systemu

---

## 🎯 CO ZAKOMENTOWAŁEŚ?

Zakomentowałeś **OrderAlarmService.startAlarmSound()** (linie 336-347):
```kotlin
// player = MediaPlayer().apply { ... }
```

**Problem:** To zakomentowało WŁAŚCIWY serwis alarmów zamówień!

**Dlaczego nadal słyszysz alarm?**

### Możliwość 1: SocketService gra ⚠️
Jeśli masz problem z połączeniem WebSocket:
```
30s po utracie połączenia → SocketService.startAlarmSound()
```

### Możliwość 2: Kanał powiadomień gra
Jeśli wysyłane są powiadomienia przez `NotificationHelper`:
```
NotificationManagerCompat.notify() → Android odtwarza dźwięk z kanału
```

### Możliwość 3: Oba serwisy grają jednocześnie
```
Nowe zamówienie → OrderAlarmService (ale zakomentowany)
+ Utrata WebSocket → SocketService ✅ GRA!
```

---

## 🔍 JAK ZDIAGNOZOWAĆ?

### Krok 1: Sprawdź logi z tagiem "ALARM START"
```bash
adb logcat -s "ALARM START:*"
```

**Szukaj:**
- `[OrderAlarmService] startAlarmSound()` - powinien być BLOCKED (player zakomentowany)
- `[SocketService] startAlarmSound() - WS DISCONNECT ALARM` - **TO MOŻE GRA!**

### Krok 2: Sprawdź powiadomienia
Na pasku powiadomień szukaj:
- ❌ "Nowe zamówienie" - OrderAlarmService (zakomentowany, nie powinien grać)
- ⚠️ "Brak połączenia z serwerem" - **SocketService - SPRAWDŹ TO!**
- ⚠️ "Sesja wygasła" - **SocketService - SPRAWDŹ TO!**

### Krok 3: Sprawdź status WebSocket
W logach szukaj:
```
SocketService: WS: brak połączenia > ALARM
```

Jeśli to widzisz - **to SocketService odtwarza alarm!**

---

## ✅ ROZWIĄZANIE

### Jeśli chcesz WYŁĄCZYĆ wszystkie alarmy:

#### 1. Zakomentuj MediaPlayer w OrderAlarmService (już zrobione ✅)
```kotlin
// Już zakomentowane w linii 336-347
```

#### 2. Zakomentuj MediaPlayer w SocketService
**Plik:** `service/SocketService.kt`, linia ~151

```kotlin
private fun startAlarmSound() {
    Timber.tag("ALARM START").w("🔇 [SocketService] startAlarmSound() WYŁĄCZONY")
    Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    
    // ZAKOMENTUJ PONIŻEJ:
    /*
    try {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        alarmPlayer?.release()
        alarmPlayer = MediaPlayer().apply {
            setAudioAttributes(attrs)
            setDataSource(this@SocketService, uri)
            isLooping = true
            prepare()
            start()
        }
        Timber.tag("ALARM START").w("✅ [SocketService] Alarm WS DISCONNECT uruchomiony!")
        Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    } catch (t: Throwable) {
        Timber.tag("ALARM START").e(t, "❌ [SocketService] Nie udało się uruchomić dźwięku alarmu")
        Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag("SocketService").e(t, "Nie udało się uruchomić dźwięku alarmu")
    }
    */
}
```

#### 3. Opcjonalnie: Wyłącz dźwięki w kanałach powiadomień
**Plik:** `notification/NotificationHelper.kt`

**Linia 234:**
```kotlin
// setSound(soundUri, attrs) // ZAKOMENTUJ
setSound(null, null) // DODAJ TO
```

**Linia 131:**
```kotlin
// setSound(soundUri, attrs) // ZAKOMENTUJ
setSound(null, null) // DODAJ TO
```

---

## 🎯 REKOMENDACJA

**Zamiast wyłączać wszystkie alarmy, zalecam:**

### 1. Odkomentuję OrderAlarmService (to główny alarm zamówień!)
Przywróć MediaPlayera w `OrderAlarmService.startAlarmSound()`

### 2. OPCJONALNIE: Wyłącz tylko SocketService alarm
Jeśli nie potrzebujesz alarmu dla utraty połączenia

### 3. Użyj logów do diagnozy
Sprawdź dokładnie w logach, który serwis odtwarza dźwięk

---

## 📊 PODSUMOWANIE

✅ **OrderAlarmService** - zakomentowany przez Ciebie  
⚠️ **SocketService** - **PRAWDOPODOBNIE TO GRA!**  
⚠️ **NotificationHelper** - kanały mogą grać niezależnie  

**Następny krok:**
1. Odkomentuj OrderAlarmService (to właściwy alarm!)
2. Sprawdź logi czy SocketService nie gra
3. Jeśli tak - zakomentuj SocketService.startAlarmSound()

---

**Data diagnozy:** 2026-02-12  
**Status:** ✅ Wszystkie źródła zidentyfikowane

