  # ✅ WSKAŹNIK STATUSU SOCKET.IO - DOKUMENTACJA

## 🎯 Funkcjonalność

Dodano wskaźnik połączenia Socket.IO w TopBar aplikacji, obok statusu restauracji.

### Gdzie jest widoczny?

- **Lokalizacja:** TopBar na ekranie głównym (HOME)
- **Dla kogo:** Tylko dla Staff (nie dla kurierów)
- **Pozycja:** Po lewej stronie statusu restauracji

### Jak wygląda?

**Kolorowa kropka (10dp):**
- 🟢 **Zielona** - Socket połączony, odbiera zamówienia
- 🔴 **Czerwona** - Socket rozłączony, brak połączenia

---

## 📁 Nowe/Zmienione pliki

### 1. **SocketStatusIndicator.kt** ✅ (NOWY)
```
L:\SHOP APP\app\src\main\java\com\itsorderchat\ui\theme\home\components\SocketStatusIndicator.kt
```

**Komponent:**
- Wyświetla kolorową kropkę (zielona/czerwona)
- Reaguje na zmiany statusu połączenia Socket.IO
- Opcjonalny tekst obok kropki (`showLabel = true/false`)

**Parametry:**
- `socketEventsRepo: SocketEventsRepository` - źródło danych o połączeniu
- `showLabel: Boolean = false` - czy pokazywać tekst "Connected"/"Disconnected"
- `modifier: Modifier = Modifier`

**Stan:**
```kotlin
val isConnected by socketEventsRepo.connection.collectAsStateWithLifecycle(initialValue = false)
```

---

### 2. **TopBarConfigBuilder.kt** ✅ (ZMIENIONY)

**Zmiany:**
1. Dodano import `SocketEventsRepository` i `SocketStatusIndicator`
2. Dodano parametr `socketEventsRepo` do `buildTopBarConfig()`
3. Zmieniono `RestaurantStatusActionItem()` aby pokazywał `Row` z:
   - `SocketStatusIndicator` (kropka)
   - `RestaurantStatusChip` (status restauracji)

**Kod:**
```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    SocketStatusIndicator(
        socketEventsRepo = socketEventsRepo,
        showLabel = false
    )
    RestaurantStatusChip(
        status = statusUi?.status ?: RestaurantStatus.CLOSED,
        onClick = { showStatusSheet = true }
    )
}
```

---

### 3. **HomeActivity.kt** ✅ (ZMIENIONY)

**Zmiany:**
1. Dodano pole `@Inject lateinit var socketEventsRepo: SocketEventsRepository`
2. Przekazano `socketEventsRepo` przez:
   - `MainAppContainer` → `MainScaffoldContent` → `buildTopBarConfig`

**Przepływ danych:**
```
HomeActivity (Inject)
  ↓
MainAppContainer (param)
  ↓
MainScaffoldContent (param)
  ↓
buildTopBarConfig (param)
  ↓
RestaurantStatusActionItem (param)
  ↓
SocketStatusIndicator (subskrypcja Flow)
```

---

### 4. **strings.xml** ✅ (ZMIENIONY)

**Dodane tłumaczenia (angielski):**
```xml
<string name="socket_connected">Connected</string>
<string name="socket_disconnected">Disconnected</string>
```

**Dodane tłumaczenia (polski - values-pl):**
```xml
<string name="socket_connected">Połączony</string>
<string name="socket_disconnected">Rozłączony</string>
```

---

## 🔄 Jak działa?

### Źródło danych

**SocketEventsRepository:**
```kotlin
private val _connection = MutableSharedFlow<Boolean>(replay = 1)
val connection = _connection.asSharedFlow()

fun emitConnected() {
    _connection.tryEmit(true)
}

fun emitDisconnected() {
    _connection.tryEmit(false)
}
```

**SocketService (onCreate):**
```kotlin
SocketManager.onConnect = {
    socketEventsRepo.emitConnected()
}
SocketManager.onDisconnect = {
    socketEventsRepo.emitDisconnected()
}
```

### Reaktywność

Komponent `SocketStatusIndicator` używa `collectAsStateWithLifecycle()`:
```kotlin
val isConnected by socketEventsRepo.connection.collectAsStateWithLifecycle(initialValue = false)
```

**Gdy zmienia się status połączenia:**
1. `SocketManager` emituje event (connect/disconnect)
2. `SocketEventsRepository` aktualizuje Flow `_connection`
3. `SocketStatusIndicator` rekomponuje się z nowym kolorem kropki

---

## 🎨 Wygląd

### Kolory

**Zielony (połączony):**
```kotlin
Color(0xFF4CAF50) // Material Green 500
```

**Czerwony (rozłączony):**
```kotlin
Color(0xFFF44336) // Material Red 500
```

### Rozmiar

- Kropka: `10.dp`
- Spacing między kropką a statusem restauracji: `12.dp`

### Layout

```
┌──────────────────────────────────────┐
│  [Menu] TopBar              [●][🟢]  │
│                              ↑   ↑   │
│                         Socket Status│
└──────────────────────────────────────┘
```

---

## 📊 Przypadki użycia

### 1. Normalna praca
- Socket połączony → Zielona kropka
- Zamówienia są odbierane w czasie rzeczywistym

### 2. Problemy z siecią
- Socket rozłączony → Czerwona kropka
- Brak nowych zamówień (nie są odbierane)
- Powiadomienie dla użytkownika że coś jest nie tak

### 3. Reconnect
- Socket ponownie się łączy → Kropka zmienia się z czerwonej na zieloną
- Użytkownik widzi że połączenie zostało przywrócone

---

## 🔧 Testowanie

### Scenariusz 1: Połączenie działa
1. Uruchom aplikację
2. Sprawdź TopBar
3. **Oczekiwane:** Zielona kropka obok statusu restauracji

### Scenariusz 2: Symulacja rozłączenia
1. Wyłącz WiFi/dane mobilne
2. Poczekaj kilka sekund
3. **Oczekiwane:** Kropka zmienia się na czerwoną

### Scenariusz 3: Reconnect
1. Włącz ponownie WiFi/dane
2. Poczekaj na reconnect
3. **Oczekiwane:** Kropka zmienia się na zieloną

---

## ⚙️ Konfiguracja

### Domyślne ustawienia

- **showLabel:** `false` - tylko kropka, bez tekstu
- **initialValue:** `false` - zakłada rozłączenie na starcie (bezpieczniejsze)

### Opcjonalnie - z tekstem

Można włączyć wyświetlanie tekstu:
```kotlin
SocketStatusIndicator(
    socketEventsRepo = socketEventsRepo,
    showLabel = true  // Pokaże "Połączony" / "Rozłączony"
)
```

---

## 🐛 Troubleshooting

### Kropka nie zmienia koloru
**Przyczyna:** SocketService nie emituje eventów  
**Rozwiązanie:** Sprawdź logi `SocketManager` w Logcat

### Brak kropki na ekranie
**Przyczyna:** Użytkownik jest kurierem (courier) lub nie jest na HOME  
**Rozwiązanie:** Wskaźnik jest tylko dla Staff na ekranie głównym

### Kropka zawsze czerwona
**Przyczyna:** Socket nie może się połączyć  
**Rozwiązanie:** 
- Sprawdź URL serwera w ustawieniach
- Sprawdź token autoryzacji
- Sprawdź logi błędów połączenia

---

## 📋 Checklist wdrożenia

- [x] Utworzono `SocketStatusIndicator.kt`
- [x] Dodano import w `TopBarConfigBuilder.kt`
- [x] Dodano parametr `socketEventsRepo` do `buildTopBarConfig()`
- [x] Zmieniono `RestaurantStatusActionItem` na Row z kropką
- [x] Wstrzyknięto `SocketEventsRepository` w `HomeActivity`
- [x] Przekazano przez wszystkie warstwy Compose
- [x] Dodano tłumaczenia (EN + PL)
- [x] Build SUCCESS ✅

---

## 🎉 Status: GOTOWE

Wskaźnik statusu Socket.IO został pomyślnie zaimplementowany i jest gotowy do użycia!

**Lokalizacja APK:** `L:\SHOP APP\app\build\outputs\apk\debug\app-debug.apk`

