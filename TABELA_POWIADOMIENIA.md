# 📋 Szybka Referencyjna Tabela Powiadomień

## 🔔 Kiedy Pokazuje Się Powiadomienie?

### Warunki Podstawowe (WSZYSTKIE muszą być spełnione)

| Warunek | Opis | Jeśli NIE |
|---------|------|-----------|
| ✅ Uprawnienia | POST_NOTIFICATIONS (Android 13+) | ❌ Brak powiadomienia |
| ✅ Rola | userRole = STAFF (nie COURIER) | ❌ Brak powiadomienia |
| ✅ Status | orderStatus = PROCESSING | ❌ Brak powiadomienia |
| ✅ Kanał | "Alarm zamówień" włączony | ❌ Brak powiadomienia |
| ✅ Kolejka | pendingOrdersQueue.size > 0 | ❌ Brak powiadomienia |

---

## 📊 Scenariusze

| # | Kolejka | Dialog | Suppress | Akcja | Powiadomienie | Dźwięk | Dialog |
|---|---------|--------|----------|-------|---------------|--------|--------|
| 1 | [O1] | ❌ | ❌ | START | ✅ Nowe | ✅ Loop | ✅ O1 |
| 2 | [O1, O2] | ✅ O1 | ❌ | RING | ✅ Update | ✅ Restart | ❌ O1 |
| 3 | [O2] | ❌ | ✅ | RING | ✅ Update | ✅ Restart | ❌ (czeka 700ms) |
| 4 | [O2] | ❌ | ❌ | START | ✅ Tak | ✅ Gra | ✅ O2 |
| 5 | [] | ✅ → ❌ | - | STOP | ❌ Usuwa | ❌ Stop | ❌ |

**Legenda:**
- O1, O2 = Order1, Order2
- ✅ = Aktywne/Tak
- ❌ = Nieaktywne/Nie
- RING = Odświeżenie (bez full screen)
- START = Pełny start (z full screen)

---

## ⏱️ Timeline Przykładowy

### Scenariusz: 2 Zamówienia Po Sobie

```
00:00 - Zamówienie ORDER-001 przychodzi
  ├─► Dodane do bazy (PROCESSING)
  └─► Kolejka: [ORDER-001]
  
00:01 - observePendingOrdersQueue() reaguje
  ├─► CASE 1: Brak dialogu + brak suppress
  ├─► startAlarmService(ORDER-001, ringOnly=false)
  ├─► ✅ Powiadomienie pokazane
  ├─► ✅ Dźwięk zaczyna grać (loop)
  └─► ✅ Dialog otwiera się

02:00 - Zamówienie ORDER-002 przychodzi
  ├─► Dodane do bazy (PROCESSING)
  └─► Kolejka: [ORDER-001, ORDER-002]
  
02:01 - observePendingOrdersQueue() reaguje
  ├─► CASE 2: Dialog otwarty + nowe zamówienie
  ├─► startAlarmService(ORDER-002, ringOnly=true)
  ├─► ✅ Powiadomienie zaktualizowane
  ├─► ✅ Dźwięk restartuje (krótka przerwa)
  └─► ❌ Dialog pozostaje dla ORDER-001

05:00 - Użytkownik zamyka dialog ORDER-001
  ├─► dismissDialog()
  ├─► lastClosedOrderId = ORDER-001
  ├─► next = ORDER-002 (jest w kolejce!)
  ├─► ❌ NIE zatrzymuje alarmu
  ├─► startAlarmService(ORDER-002, ringOnly=true)
  └─► _suppressAutoOpen = true (na 700ms)
  
05:00.700 - Suppress kończy się
  ├─► _suppressAutoOpen = false
  ├─► observePendingOrdersQueue() reaguje
  ├─► CASE 1: Brak dialogu + brak suppress
  └─► ✅ Dialog otwiera się dla ORDER-002

07:00 - Użytkownik zamyka dialog ORDER-002
  ├─► dismissDialog()
  ├─► next = null (brak kolejnych)
  ├─► stopAlarmService()
  ├─► ❌ Dźwięk zatrzymuje się
  ├─► ❌ Powiadomienie usunięte
  └─► ❌ Serwis zatrzymany
```

---

## 🎬 Akcje i Efekty

### ACTION_START (Pełny Start)

**Kiedy:**
- Pierwsze zamówienie w kolejce
- Dialog zamknięty + suppress nieaktywny

**Efekt:**
```
✅ Powiadomienie foreground
✅ Dźwięk zaczyna grać (loop)
✅ Full Screen Intent (może otworzyć app)
✅ Dialog otwiera się
```

---

### ACTION_RING (Odświeżenie)

**Kiedy:**
- Kolejne zamówienie gdy dialog otwarty
- Nowe zamówienie w suppress window
- Po zamknięciu dialogu (jest następne)

**Efekt:**
```
✅ Powiadomienie aktualizowane
✅ Dźwięk restartuje (bump)
❌ Full Screen Intent NIE uruchamia
❌ Dialog NIE zmienia się
```

---

### ACTION_STOP_ALARM (Zatrzymanie)

**Kiedy:**
- Zamknięcie ostatniego zamówienia
- Kliknięcie "Wycisz" w notyfikacji
- Manualne wywołanie stopAlarmService()

**Efekt:**
```
❌ Dźwięk zatrzymuje się
❌ Powiadomienie usunięte
❌ Serwis foreground zatrzymany
❌ MediaPlayer zwolniony
```

---

## ❌ Dlaczego Powiadomienie Się NIE Pokazało?

### Sprawdź Po Kolei:

#### 1. Uprawnienia
```bash
adb shell dumpsys notification_listener | grep POST_NOTIFICATIONS
```
**Jeśli DENIED:** Nadaj uprawnienie w ustawieniach

#### 2. Logi
```bash
adb logcat | grep "ALARM_DIAG"
```
**Szukaj:**
- `❌ ALARM BLOCKED: Brak uprawnień`
- `❌ ALARM BLOCKED: Okno suppress`

#### 3. Rola Użytkownika
```bash
adb logcat | grep "observePendingOrdersQueue"
```
**Szukaj:**
- `userRole: COURIER` → Powiadomienia wyłączone

#### 4. Status Zamówienia
```bash
adb logcat | grep "pendingOrdersQueue"
```
**Szukaj:**
- `queue.size: 0` → Zamówienie nie jest PROCESSING

#### 5. Kanał Powiadomień
```
Ustawienia → Aplikacje → Shop App → Powiadomienia → "Alarm zamówień"
```
**Sprawdź:** Czy włączony i priorytet = HIGH

---

## 🔍 Diagnostyka Szybka

### Test 1: Podstawowy
```
1. Wyślij zamówienie PROCESSING
2. Sprawdź logi:
   grep "ALARM CALL"
3. Powinno być:
   ✅ "ALARM STARTED SUCCESSFULLY"
```

### Test 2: Drugie Zamówienie
```
1. Zostaw dialog otwarty
2. Wyślij drugie zamówienie
3. Sprawdź logi:
   grep "CASE 2"
4. Powinno być:
   ✅ "ACTION_RING dla nowego"
```

### Test 3: Suppress Window
```
1. Zamknij dialog
2. Natychmiast wyślij nowe (< 700ms)
3. Sprawdź logi:
   grep "CASE 3"
4. Powinno być:
   ✅ "Suppress aktywny"
   ✅ Po 700ms: "CASE 1"
```

---

## 🎯 Najczęstsze Problemy

| Problem | Przyczyna | Rozwiązanie |
|---------|-----------|-------------|
| Brak powiadomienia | Brak uprawnień | Nadaj POST_NOTIFICATIONS |
| Brak dźwięku | DND włączony | Wyłącz DND lub ustaw bypass |
| Dialog nie otwiera się | Suppress aktywny | Czekaj 700ms |
| Powiadomienie ciche | Kanał wyłączony | Włącz w ustawieniach Android |
| Opóźnienie w tle | Battery optimization | Wyłącz optymalizację |

---

## 📞 Szybkie Komendy

### Sprawdź Kolejkę
```bash
adb logcat -s OrderAlarmService:D | grep "queue.size"
```

### Sprawdź Alarmy
```bash
adb logcat -s ALARM_DIAG:W
```

### Sprawdź Suppress
```bash
adb logcat | grep "suppress:"
```

### Sprawdź Serwis
```bash
adb shell dumpsys activity services | grep OrderAlarmService
```

---

## 🔑 Kluczowe Zmienne

| Zmienna | Typ | Opis |
|---------|-----|------|
| `pendingOrdersQueue` | StateFlow<List<Order>> | Zamówienia PROCESSING |
| `currentDialogId` | String? | ID otwartego dialogu |
| `_suppressAutoOpen` | MutableStateFlow<Boolean> | Okno 700ms po zamknięciu |
| `lastClosedOrderId` | String? | Ostatnio zamknięte |
| `lastRangOrderId` | String? | Ostatnio "RING" |

---

**Ostatnia aktualizacja:** 2026-02-04  
**Wersja:** 1.0  
**Pełna dokumentacja:** `DOKUMENTACJA_POWIADOMIEN.md`

