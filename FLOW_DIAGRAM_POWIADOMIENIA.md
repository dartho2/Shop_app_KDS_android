# 🔄 Flow Diagram: System Powiadomień

## 📊 Główny Przepływ

```
┌─────────────────────────────────────────────────────────────┐
│                  NOWE ZAMÓWIENIE (SOCKET)                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │  Dodane do bazy      │
              │  status: PROCESSING  │
              └──────────┬───────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │ getAllOrdersFlow()   │
              │ .filter(PROCESSING)  │
              └──────────┬───────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │ pendingOrdersQueue   │
              │ emituje zmianę       │
              └──────────┬───────────┘
                         │
                         ▼
        ┌────────────────────────────────────┐
        │ observePendingOrdersQueue()        │
        └────────────┬───────────────────────┘
                     │
        ┌────────────▼────────────┐
        │  Sprawdzenie warunków:  │
        │  • userRole != COURIER  │
        │  • queue.size > 0       │
        └────────────┬────────────┘
                     │
         ┌───────────┴──────────┐
         │                      │
         ▼                      ▼
    ┌────────┐           ┌─────────┐
    │COURIER │           │ STAFF   │
    └────┬───┘           └────┬────┘
         │                    │
         ▼                    ▼
    KONIEC           ┌──────────────────┐
                     │ Sprawdź warunki: │
                     │ • currentDialogId│
                     │ • suppress       │
                     │ • newestNotShown │
                     └────┬─────────────┘
                          │
              ┌───────────┴───────────┐
              │                       │
              ▼                       ▼
         ┌─────────┐            ┌─────────┐
         │ CASE 1  │            │ CASE 2  │
         │ Pełny   │            │ RING    │
         │ start   │            │ tylko   │
         └────┬────┘            └────┬────┘
              │                      │
              ▼                      ▼
    ┌──────────────────┐   ┌──────────────────┐
    │ ACTION_START     │   │ ACTION_RING      │
    │ • ringOnly=false │   │ • ringOnly=true  │
    └────┬─────────────┘   └────┬─────────────┘
         │                      │
         └──────────┬───────────┘
                    │
                    ▼
        ┌───────────────────────┐
        │ startAlarmService()   │
        └───────────┬───────────┘
                    │
         ┌──────────┴──────────┐
         │                     │
         ▼                     ▼
    ┌─────────┐         ┌──────────┐
    │ Uprawni-│         │ Suppress │
    │ enia?   │         │ check    │
    └────┬────┘         └────┬─────┘
         │                   │
    ┌────▼────┐         ┌────▼────┐
    │  BRAK   │         │ AKTYWNY │
    │  ❌     │         │  ❌     │
    └─────────┘         └─────────┘
         │                   │
         └────────┬──────────┘
                  │
                  ▼ (WSZYSTKIE OK)
       ┌──────────────────────┐
       │ OrderAlarmService    │
       │ startForegroundSrv() │
       └──────────┬───────────┘
                  │
       ┌──────────┴──────────┐
       │                     │
       ▼                     ▼
┌──────────────┐      ┌─────────────┐
│ Powiadomienie│      │   Dźwięk    │
│ • Foreground │      │ MediaPlayer │
│ • Full Screen│      │ • Loop      │
│ • 2 akcje    │      │ • ALARM     │
└──────────────┘      └─────────────┘
```

---

## 🔀 Decyzja: Który CASE?

```
┌──────────────────────────────────┐
│ observePendingOrdersQueue()      │
│ wykrywa zmianę w kolejce         │
└────────────┬─────────────────────┘
             │
             ▼
      ┌──────────────┐
      │ suppress?    │
      └──┬────────┬──┘
        NO      YES
         │       │
         │       ▼
         │  ┌───────────────────┐
         │  │ newestNotShown?   │
         │  └──┬────────────┬───┘
         │    YES          NO
         │     │            │
         │     ▼            ▼
         │  CASE 3      CASE 4
         │  RING         (skip)
         │
         ▼
   ┌──────────────┐
   │currentDialog?│
   └──┬────────┬──┘
     null    SET
      │       │
      │       ▼
      │  ┌───────────────────┐
      │  │ newestNotShown?   │
      │  └──┬────────────┬───┘
      │    YES          NO
      │     │            │
      │     ▼            ▼
      │  CASE 2      CASE 4
      │  RING         (skip)
      │
      ▼
   CASE 1
   START
```

**Wyjaśnienie:**
- **CASE 1:** Brak dialogu, brak suppress → Pełny start
- **CASE 2:** Dialog otwarty, nowe zamówienie → RING
- **CASE 3:** Suppress aktywny, nowe zamówienie → RING (bez dialogu)
- **CASE 4:** Inne kombinacje → Skip (nic nie robi)

---

## ⏹️ Zatrzymanie Alarmu

```
┌─────────────────────────┐
│ dismissDialog()         │
│ (użytkownik zamyka)     │
└──────────┬──────────────┘
           │
           ▼
    ┌──────────────┐
    │ Kolejka?     │
    └──┬────────┬──┘
      next    null
       │       │
       │       ▼
       │  ┌────────────────┐
       │  │stopAlarmService│
       │  └────────┬───────┘
       │           │
       │           ▼
       │  ┌─────────────────┐
       │  │ ACTION_STOP     │
       │  └────────┬────────┘
       │           │
       │           ▼
       │  ┌─────────────────────┐
       │  │ OrderAlarmService   │
       │  │ handleStopAction()  │
       │  └────────┬────────────┘
       │           │
       │      ┌────┴────┐
       │      │         │
       │      ▼         ▼
       │  ┌────────┐ ┌──────┐
       │  │STOP    │ │USUŃ  │
       │  │dźwięk  │ │notif │
       │  └────────┘ └──────┘
       │      │         │
       │      └────┬────┘
       │           │
       │           ▼
       │     ┌──────────┐
       │     │stopSelf()│
       │     └──────────┘
       │
       ▼
  ┌──────────────────┐
  │startAlarmService │
  │(next, RING)      │
  └────────┬─────────┘
           │
           ▼
  ┌──────────────────┐
  │Suppress 700ms    │
  └────────┬─────────┘
           │
      (po 700ms)
           │
           ▼
  ┌──────────────────┐
  │Dialog dla next   │
  └──────────────────┘
```

---

## 🔄 Cykl Życia Powiadomienia

```
START
  │
  ▼
┌──────────────────────┐
│ NIEAKTYWNE           │
│ • Brak serwisu       │
│ • Brak powiadomienia │
│ • Brak dźwięku       │
└──────────┬───────────┘
           │
    (nowe zamówienie)
           │
           ▼
┌──────────────────────┐
│ AKTYWNE (START)      │
│ • Serwis foreground  │
│ • Powiadomienie ON   │
│ • Dźwięk LOOP        │
│ • Dialog OTWARTY     │
└──────────┬───────────┘
           │
           ├──► (kolejne zamówienie)
           │    └─> AKTYWNE (RING)
           │        • Update notif
           │        • Restart dźwięk
           │        • Dialog bez zmian
           │
           ├──► (zamknięcie, jest next)
           │    └─> AKTYWNE (RING)
           │        • Dźwięk gra dalej
           │        • Suppress 700ms
           │        • Potem dialog dla next
           │
           └──► (zamknięcie, brak next)
                └─> NIEAKTYWNE
                    • stopAlarmService()
                    • Wszystko off
```

---

## 📱 Full Screen Intent Flow

```
┌─────────────────────────┐
│ ACTION_START            │
│ useFullScreen = true    │
└──────────┬──────────────┘
           │
           ▼
    ┌──────────────┐
    │ App w tle?   │
    └──┬────────┬──┘
     TAK      NIE
      │        │
      │        ▼
      │   ┌─────────────┐
      │   │ Notyfikacja │
      │   │ pokazana    │
      │   └─────────────┘
      │
      ▼
┌──────────────────────┐
│ Android 14+?         │
└──┬────────────────┬──┘
  TAK              NIE
   │                │
   ▼                ▼
┌─────────────┐  ┌──────────────┐
│Uprawnienie? │  │Full Screen   │
└──┬────────┬─┘  │automatycznie │
  TAK     NIE    └──────────────┘
   │       │
   │       ▼
   │  ┌─────────────┐
   │  │ Notyfikacja │
   │  │ tylko       │
   │  └─────────────┘
   │
   ▼
┌──────────────┐
│ Otwiera app  │
│ HomeActivity │
│ + orderJson  │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ Dialog       │
│ pokazany     │
└──────────────┘
```

---

## 🎵 Dźwięk Flow

```
┌──────────────────┐
│ startAlarmSound()│
└────────┬─────────┘
         │
         ▼
  ┌──────────────┐
  │ player?      │
  └──┬────────┬──┘
   null    exists
    │        │
    │        ▼
    │   ┌──────────┐
    │   │isPlaying?│
    │   └──┬───┬───┘
    │     YES NO
    │      │  │
    │      │  └──> (stop + new)
    │      │
    │      ▼
    │  ┌─────────┐
    │  │ RETURN  │
    │  │ (już gra)
    │  └─────────┘
    │
    ▼
┌────────────────────┐
│ MediaPlayer.create │
│ • USAGE_ALARM      │
│ • isLooping = true │
└────────┬───────────┘
         │
         ▼
┌────────────────────┐
│ prepare()          │
│ start()            │
└────────┬───────────┘
         │
         ▼
┌────────────────────┐
│ DŹWIĘK GRA         │
│ (non-stop loop)    │
└────────────────────┘
         │
    (ACTION_STOP)
         │
         ▼
┌────────────────────┐
│ stopAlarmSound()   │
│ • stop()           │
│ • release()        │
│ • player = null    │
└────────────────────┘
```

---

## 🔒 Suppress Window Flow

```
┌──────────────────┐
│ dismissDialog()  │
└────────┬─────────┘
         │
         ▼
┌────────────────────────┐
│ _suppressAutoOpen=true │
└────────┬───────────────┘
         │
         ▼
┌────────────────────┐
│ delay(700ms)       │
└────────┬───────────┘
         │
         │ (nowe zamówienie w tym czasie?)
         │
         ├──► TAK: CASE 3 - ACTION_RING (bez dialogu)
         │
         ▼
┌────────────────────────┐
│ _suppressAutoOpen=false│
└────────┬───────────────┘
         │
         ▼
┌────────────────────────┐
│ observePendingQueue()  │
│ wykrywa zmianę         │
└────────┬───────────────┘
         │
         ▼
┌────────────────────────┐
│ CASE 1 - Dialog opens  │
│ (jeśli jest w kolejce) │
└────────────────────────┘
```

**Cel suppress:**
- Zapobieganie natychmiastowemu ponownemu otwarciu dialogu
- Daje użytkownikowi 700ms na reakcję
- Dźwięk może grać (RING), ale dialog czeka

---

## 🧵 Thread Safety

```
┌──────────────────────────┐
│ Socket Thread            │
│ (otrzymuje nowe zamówienie│
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│ IO Thread                │
│ • insertOrUpdateOrder()  │
│ • Room zapisuje do bazy  │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│ Room emituje zmianę      │
│ (Flow observing DB)      │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│ Main Thread              │
│ • observePendingQueue()  │
│ • viewModelScope.launch  │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│ Service Thread           │
│ • OrderAlarmService      │
│ • onStartCommand()       │
└──────────────────────────┘
```

**Synchronizacja:**
- Room Flow automatycznie synchronizuje
- StateFlow jest thread-safe
- viewModelScope zarządza cyklem życia

---

**Data:** 2026-02-04  
**Wersja:** 1.0  
**Powiązane:** `DOKUMENTACJA_POWIADOMIEN.md`, `TABELA_POWIADOMIENIA.md`

