# 🚨 PILNE - ZATRZYMAJ PĘTLĘ TASK REOPEN!

## NATYCHMIAST DO WYKONANIA!

---

## ❌ PROBLEM

Aplikacja jest w **nieskończonej pętli** - Task Reopen restartuje aplikację co ~200-300ms!

**Skutki:**
- 🔋 Ogromne zużycie baterii
- 🔥 Wysoka temperatura urządzenia
- 💥 CPU na 100%
- 📱 Aplikacja praktycznie nie działa normalnie

---

## ✅ ROZWIĄZANIE NATYCHMIASTOWE (30 sekund!)

### WYŁĄCZ Task Reopen w ustawieniach:

1. Otwórz aplikację (mimo pętli spróbuj wejść)
2. Szybko kliknij **⚙️ Ustawienia** (lewy dolny róg lub menu)
3. Znajdź opcję **"Task Reopen"** lub **"Przywracanie aplikacji z taska"**
4. **WYŁĄCZ** przełącznik
5. Zrestartuj aplikację

**To NATYCHMIAST zatrzyma pętlę!**

---

## 📱 ALTERNATYWA: Zainstaluj nową wersję (2 minuty)

Jeśli nie możesz wejść do ustawień (pętla jest zbyt szybka):

### Krok 1: Odinstaluj starą wersję

```powershell
adb uninstall com.itsorderchat
```

### Krok 2: Zainstaluj nową wersję

```powershell
adb install "L:\SHOP APP\app\build\outputs\apk\debug\app-debug.apk"
```

### Krok 3: Uruchom aplikację

Nowa wersja ma:
- ✅ Task Reopen **domyślnie WYŁĄCZONY**
- ✅ Flagę zapobiegającą pętli (gdybyś włączył Task Reopen)

---

## 🔍 JAK SPRAWDZIĆ CZY DZIAŁA?

### Logi PRZED naprawą (ZŁE):

```
14:44:53.134  🔄 Task Reopen włączony - przywracanie
14:44:53.338  START HomeActivity
14:44:53.496  🔄 Task Reopen włączony - przywracanie (PĘTLA!)
14:44:53.707  START HomeActivity (ZNOWU!)
14:44:53.873  🔄 Task Reopen włączony - przywracanie (PĘTLA!)
(powtarza się w nieskończoność...)
```

**Częstotliwość:** ~3-4 restarty na sekundę!

### Logi PO naprawie (DOBRE):

```
onPause() - aplikacja weszła w background
onStop() - aplikacja całkowicie w tle
⏸️ Task Reopen wyłączony - aplikacja pozostaje w tle
(KONIEC - brak dalszych logów)
```

---

## ⚙️ CO ZOSTAŁO NAPRAWIONE W NOWEJ WERSJI?

### Zmiana 1: Domyślnie Task Reopen WYŁĄCZONY

**Przed:**
```kotlin
suspend fun isTaskReopenEnabled(): Boolean =
    dataStore.data.map { it[Keys.TASK_REOPEN_ENABLED] ?: true }.first() // ❌ domyślnie włączone
```

**Po:**
```kotlin
suspend fun isTaskReopenEnabled(): Boolean =
    dataStore.data.map { it[Keys.TASK_REOPEN_ENABLED] ?: false }.first() // ✅ domyślnie WYŁĄCZONE
```

### Zmiana 2: Flaga zapobiegająca pętli

```kotlin
@Volatile
private var isReopeningTask = false

override fun onStop() {
    // ✅ Sprawdź flagę
    if (isReopeningTask) {
        Timber.d("⏭️ Task Reopen już w toku - pomijam aby uniknąć pętli")
        return
    }
    
    if (taskReopenEnabled) {
        // ✅ Ustaw flagę PRZED startActivity()
        isReopeningTask = true
        
        startActivity(...)
        
        // Reset po 1s
        delay(1000)
        isReopeningTask = false
    }
}

override fun onResume() {
    // ✅ Reset flagi gdy Activity wraca
    isReopeningTask = false
}
```

---

## 📊 MONITOROWANIE

### Sprawdź czy pętla się zatrzymała:

```powershell
adb logcat -s HomeActivity\$onStop:*
```

**Jeśli działa poprawnie:**
- Zobaczysz log `"⏸️ Task Reopen wyłączony"` lub
- Log `"⏭️ Task Reopen już w toku - pomijam"` (max 1 raz, nie w pętli)

**Jeśli nadal problem:**
- Widzisz ciągle `"🔄 Task Reopen włączony - przywracanie"` (co 200-300ms)
- Znaczy że NIE zainstalowałeś nowej wersji lub nie wyłączyłeś w ustawieniach

---

## ⚠️ DLACZEGO TO SIĘ STAŁO?

### Task Reopen miał błąd w implementacji:

1. Gdy aplikacja idzie w tło → wywołuje się `onStop()`
2. Task Reopen wykrywa `onStop()` → wywołuje `startActivity(REORDER_TO_FRONT)`
3. `startActivity()` powoduje że **poprzednia Activity** przechodzi przez `onStop()` **ponownie**
4. To znowu wywołuje Task Reopen → **PĘTLA!**

### Rozwiązanie:

Flaga `isReopeningTask` zapobiega ponownemu wywołaniu:
- Przed `startActivity()` → ustaw flagę `true`
- W `onStop()` → sprawdź flagę, jeśli `true` → **STOP!**
- Po powrocie Activity → reset flagi

---

## 🎯 AKCJA TERAZ!

### NAJPIERW: Wyłącz Task Reopen w ustawieniach

LUB

### ALTERNATYWNIE: Zainstaluj nową wersję

```powershell
# Odinstaluj starą
adb uninstall com.itsorderchat

# Zainstaluj nową
adb install "L:\SHOP APP\app\build\outputs\apk\debug\app-debug.apk"
```

---

## 📝 PODSUMOWANIE

| Element | Status |
|---------|--------|
| **Nowa wersja APK** | ✅ Zbudowana |
| **Task Reopen domyślnie** | ✅ WYŁĄCZONY |
| **Flaga zapobiegająca pętli** | ✅ Dodana |
| **Build** | ✅ SUCCESSFUL |
| **Gotowe do instalacji** | ✅ TAK |

---

**PILNE:** Wykonaj TERAZ jedną z opcji powyżej, aby zatrzymać pętlę!

**Lokalizacja APK:** `L:\SHOP APP\app\build\outputs\apk\debug\app-debug.apk`

---

**Data:** 2026-02-13 14:50  
**Priorytet:** 🔴 **CRITICAL**  
**Typ:** Emergency Bug Fix

