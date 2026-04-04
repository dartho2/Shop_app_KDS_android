# Diagram Przepływu Logowania AIDL

## 🔄 Pełny Cykl Połączenia, Drukowania i Rozłączenia

```
┌─────────────────────────────────────────────────────────────────┐
│           APLIKACJA POS - MONITOROWANIE DRUKARKI AIDL           │
└─────────────────────────────────────────────────────────────────┘

                          🚀 START APLIKACJI
                                 │
                                 ���
                ┌─────────────────────────────────┐
                │  Użytkownik klika "Drukuj"      │
                └─────────────────────────────────┘
                                 │
                                 ▼
        ╔═════════════════════════════════════════════╗
        ║  printText(text, autoCut)                  ║
        ║  🖨️ ===== START DRUKOWANIA TEKSTU =====  ║
        ║  ⏱️ Timestamp: 1674158123800               ║
        ║  📝 Tekst: [dane do wydruku]              ║
        ║  📊 isConnected: true/false               ║
        ╚═════════════════════════════════════════════╝
                                 │
                    ┌────────────┴────────────┐
                    │                         │
           NIE   isConnected?          TAK
                    │                         │
                    ▼                         ▼
        ┌───────────────────────┐   ┌──────────────────┐
        │ connect()             │   │ Drukuj od razu   │
        │ 🔄 START POŁĄCZENIA   │   │                  │
        └───────────────────────┘   └──────────────────┘
                    │
         ┌──────────┼──────────┐
         │          │          │
       PRÓBA1    PRÓBA2     PRÓBA3
         │          │          │
    KLON/REC      SENR.      SUNMI
         │          │          │
         ▼          ▼          ▼
    ┌────────┐┌────────┐┌────────┐
    │bindTo()││bindTo()││bindTo()│
    │🔗      ││🔗      ││🔗      │
    └────────┘└────────┘└────────┘
         │          │          │
    ✅TRU?    ��TRU?    ✅TRU?
         │          │          │
    POWÓD    POWÓD    POWÓD
         │          │          │
         └──────────┼──────────┘
                    │
                    ▼
    ╔════════════════════════════════════════════════╗
    ║  onServiceConnected(name, service)            ║
    ║  🔗 ===== POŁĄCZENIE Z DRUKÄRKĄ AIDL =====   ║
    ║  ⏱️ Timestamp: [chwila po bindService]       ║
    ║  📦 Pakiet: [recieptservice/senraise/woyou]  ║
    ║  ✅ Typ: [KLON/SENRAISE/WOYOU]               ║
    ║  ✅ Status: POŁĄCZONO POMYŚLNIE              ║
    ║  isConnected = true                           ║
    ║  currentServiceType = [TYPE]                  ║
    ╚════════════════════════════════════════════════╝
                    │
                    ▼
    ╔════════════════════════════════════════════════╗
    ║  printText() - KONTYNUACJA                    ║
    ║  🖨️ ===== START DRUKOWANIA TEKSTU =====     ║
    ║  📊 isConnected: true                        ║
    ║  🔌 currentServiceType: [TYPE]               ║
    ║                                               ║
    ║  Wysłanie do drukarki:                        ║
    ║  ├─ when(currentServiceType)                 ║
    ║  ├─ CLONE → handleClonePrint()               ║
    ║  ├─ SENRAISE → s.printText()                 ║
    ║  └─ WOYOU → s.printText()                    ║
    ║                                               ║
    ║  Metody wysyłania:                           ║
    ║  ├─ 🚀 printImage(byte[])                    ║
    ║  ├─ 🚀 printText(String)                     ║
    ║  └─ 🚀 nextLine(3)                           ║
    ║                                               ║
    ║  ✂️ AutoCut?                                 ║
    ║  └─ cutPaper()                               ║
    ║                                               ║
    ║  ✅ Drukowanie zakończone pomyślnie         ║
    ║  🖨️ ===== KONIEC DRUKOWANIA =====           ║
    ╚════════════════════════════════════════════════╝
                    │
                    ▼
        ┌───────────────────────┐
        │ Użytkownik klika      │
        │ "Rozłącz" lub koniec  │
        └───────────────────────┘
                    │
                    ▼
    ╔════════════════════════════════════════════════╗
    ║  disconnect()                                 ║
    ║  🔌 ===== START PROCESU ROZŁĄCZENIA =====    ║
    ║  ⏱️ Timestamp: [chwila rozłączenia]          ║
    ║  🔌 Typ usługi: [CLONE/SENRAISE/WOYOU]      ║
    ║  📊 isConnected: true                        ║
    ║                                               ║
    ║  context.unbindService(connection)           ║
    ║  ✅ unbindService wykonany pomyślnie        ║
    ║                                               ║
    ║  Czyszczenie:                                ║
    ║  ├─ isConnected = false                      ║
    ║  ├─ currentServiceType = NONE                ║
    ║  ├─ cloneService = null                      ║
    ║  ├─ senraiseService = null                   ║
    ║  └─ woyouService = null                      ║
    ║                                               ║
    ║  ✅ Status: ROZŁĄCZONO (zmienne wyzerowane) ║
    ║  🔌 ===== KONIEC PROCESU ROZŁĄCZENIA =====  ║
    ╚════════════════════════════════════════════════╝
                    │
                    ▼
    ╔════════════════════════════════════════════════╗
    ║  onServiceDisconnected(name)                  ║
    ║  ❌ ===== ROZŁĄCZENIE Z DRUKÄRKĄ AIDL =====  ║
    ║  ⏱️ Timestamp: [chwila callback]             ║
    ║  📦 Pakiet: [pacjent usługi]                 ║
    ║  🔌 Typ usługi: [wcześniej połączona]       ║
    ║  ❌ Status: ROZŁĄCZONO                       ║
    ║  ❌ ===== KONIEC LOGOWANIA ROZŁĄCZENIA ===== ║
    ╚════════════════════════════════════════════════╝
                    │
                    ▼
            🏁 KONIEC CYKLU

```

## 📊 Scenariusz Sukcesu

```
✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅

🔄 START PROCESU POŁĄCZENIA
   ↓
📍 Próba 1: Klon (recieptservice)
   ↓
✅ bindService zwrócił: true
   ↓
✅ Próba 1 POWIODŁA SIĘ!
   ↓
🔗 POŁĄCZENIE Z DRUKÄRKĄ AIDL
   ↓
✅ Typ: KLON (recieptservice)
✅ Status: POŁĄCZONO POMYŚLNIE
   ↓
🖨️ START DRUKOWANIA TEKSTU
   ↓
✅ Drukowanie zakończone pomyślnie
   ↓
🔌 START PROCESU ROZŁĄCZENIA
   ↓
✅ unbindService wykonany pomyślnie
   ↓
❌ ROZŁĄCZENIE Z DRUKÄRKĄ AIDL
   ↓
❌ Status: ROZŁĄCZONO

✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅ ✅
```

## ❌ Scenariusz Błędu - Brak Drukarki

```
❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌

🔄 START PROCESU POŁĄCZENIA
   ↓
📍 Próba 1: Klon (recieptservice)
   ↓
❌ bindService zwrócił: false
❌ Próba 1 NIEUDANA
   ↓
📍 Próba 2: Senraise
   ↓
❌ bindService zwrócił: false
❌ Próba 2 NIEUDANA
   ↓
📍 Próba 3: Sunmi/Woyou
   ↓
❌ bindService zwrócił: false
❌ Próba 3 NIEUDANA
   ↓
❌ WSZYSTKIE PRÓBY NIEUDANE!
❌ Status: BRAK POŁĄCZENIA
   ↓
⚠️ Fallback socket: false

❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌ ❌
```

## ⚠️ Scenariusz Błędu - Drukuje, ale Error

```
⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️

✅ POŁĄCZONO POMYŚLNIE
   ↓
🖨️ START DRUKOWANIA TEKSTU
   ↓
🚀 METODA A: printImage(byte[])
   ↓
❌ METODA A BŁĄD: [komunikat błędu]
   ↓
🚀 METODA B: printText(String)
   ↓
❌ METODA B BŁĄD: [komunikat błędu]
   ↓
❌ Błąd druku AIDL
❌ Wiadomość: [szczegóły]
   ↓
❌ Drukowanie nie powiodło się

⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️ ⚠️
```

## ⏱️ Pomiar Czasu

```
Timestamp START:  1674158123000
                        │
                        │ [OPERACJA]
                        │
                        ▼
Timestamp KONIEC: 1674158124234

CZAS OPERACJI = 1674158124234 - 1674158123000 = 1234ms
```

## 🎯 Punkty Kontrolne

```
PUNKT 1: connect() START
         └─ Zmień: 📊 isConnected z false na true
         └─ Zmień: 🔌 currentServiceType na [TYPE]

PUNKT 2: onServiceConnected() CALLBACK
         └─ Sprawdź: 📦 Pakiet = oczekiwany
         └─ Sprawdź: ✅ Status = POMYŚLNIE

PUNKT 3: printText() START
         └─ Sprawdź: 📊 isConnected = true
         └─ Sprawdź: 🔌 Type = KLON/SENRAISE/WOYOU

PUNKT 4: Wysłanie do drukarki
         └─ Sprawdź: 🚀 Wysłana metoda
         └─ Sprawdź: 📦 Liczba bajtów

PUNKT 5: printText() KONIEC
         └─ Sprawdź: ✅ Drukowanie zakończone

PUNKT 6: disconnect() START
         └─ Zmień: 📊 isConnected na false
         └─ Zmień: 🔌 currentServiceType na NONE

PUNKT 7: onServiceDisconnected() CALLBACK
         └─ Potwierdź: ❌ Status = ROZŁĄCZONO
```

## 🔍 Jeśli Coś Idzie Nie Tak

```
PROBLEM: printText() START widoczny, ale nie KONIEC

AKCJA:
  1. Szukaj: ❌ METODA A BŁĄD
  2. Szukaj: ❌ METODA B BŁĄD
  3. Szukaj: ❌ Błąd druku AIDL
  4. Przeczytaj: 📦 DANE: [bajty]
     └─ Czy bajty wyglądają prawidłowo?

ROZWIĄZANIE:
  - Sprawdź encoding
  - Sprawdź codepage
  - Sprawdź papier w drukarce
  - Sprawdź połączenie sprzętowe
```

