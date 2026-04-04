# Implementacja wyboru portali przy otwieraniu/zamykaniu sklepu

## 📋 Podsumowanie zmian

### 1. **OpenCloseStoreDialog.kt** (NOWY PLIK)
Nowy dialog wyboru portali do otwierania lub zamykania sklepu, analogiczny do dialogu pauzy.

**Funkcje:**
- Wyświetla listę dostępnych portali (UBER, GLOVO, WOLT, BOLT, TAKEAWAY, WOOCOMMERCE, ITS)
- Pozwala na wybór wielu portali jednocześnie
- Pusta selekcja = akcja globalna (wszystkie portale)
- Kolor przycisku "Potwierdź":
  - 🟢 Zielony dla akcji "Otwórz sklep"
  - 🔴 Czerwony dla akcji "Zamknij sklep"

**Parametry:**
```kotlin
fun OpenCloseStoreDialog(
    isOpenAction: Boolean,        // true = otwieranie, false = zamykanie
    onDismiss: () -> Unit,
    onConfirm: (portals: List<SourceEnum>?) -> Unit
)
```

---

### 2. **RestaurantStatusSheet.kt** (ZAKTUALIZOWANY)
Zmodyfikowano aby używał nowego dialogu wyboru portali.

**Zmiany:**
- Zmieniono sygnatury `onOpen` i `onClose`:
  ```kotlin
  onOpen: (portals: List<SourceEnum>?) -> Unit
  onClose: (portals: List<SourceEnum>?) -> Unit
  ```
- Dodano stany dla dialogów:
  ```kotlin
  var showOpenDialog by remember { mutableStateOf(false) }
  var showCloseDialog by remember { mutableStateOf(false) }
  ```
- Kliknięcie "Otwórz sklep" lub "Zamknij sklep" teraz pokazuje dialog wyboru portali
- Obramowanie (1dp):
  - 🟢 Zielone dla "Otwórz sklep"
  - 🔴 Czerwone dla "Zamknij sklep"
- Wyszarzenie nieaktywnych opcji (alpha 0.4)

---

### 3. **OpenHoursRepository.kt** (ZAKTUALIZOWANY)
Rozszerzono funkcję `setClosed` o parametr `portals`.

**Zmiany:**
```kotlin
suspend fun setClosed(
    closed: Boolean, 
    optimistic: Boolean = true,
    portals: List<SourceEnum>? = null  // ← NOWY PARAMETR
): Resource<Unit>
```

**Logika:**
- Konwertuje `List<SourceEnum>` na `List<String>` (nazwy portali)
- Przekazuje do `UpdateOpenHoursRequest(isOpen = !closed, portals = portalStrings)`
- API otrzymuje payload:
  ```json
  {
    "is_open": true/false,
    "portals": ["UBER", "GLOVO"] lub null
  }
  ```

---

### 4. **Stringi** (DODANE)

#### `strings.xml` (angielski):
```xml
<string name="open_all_portals">Open ALL portals</string>
<string name="close_all_portals">Close ALL portals</string>
<string name="open_selected_portals">Open: %1$s</string>
<string name="close_selected_portals">Close: %1$s</string>
<string name="select_portals_to_change">Select portals to change</string>
<string name="selected">Selected</string>
```

#### `strings-pl.xml` (polski):
```xml
<string name="open_all_portals">Otwórz WSZYSTKIE portale</string>
<string name="close_all_portals">Zamknij WSZYSTKIE portale</string>
<string name="open_selected_portals">Otwórz: %1$s</string>
<string name="close_selected_portals">Zamknij: %1$s</string>
<string name="select_portals_to_change">Wybierz portale do zmiany</string>
<string name="selected">Wybrano</string>
```

---

## 🎯 Przepływ użytkownika

### Otwieranie sklepu:
1. Użytkownik klika chip statusu restauracji w top bar
2. Otwiera się `RestaurantStatusSheet` (bottom sheet)
3. Użytkownik klika "Otwórz sklep" (opcja z 🟢 zielonym obramowaniem)
4. Pokazuje się `OpenCloseStoreDialog` z wyborem portali
5. Użytkownik wybiera portale (lub zostawia puste dla "wszystkie")
6. Klika zielony przycisk "Potwierdź"
7. Wysyła się request: `PUT /admin/openhours` z `{"is_open": true, "portals": [...]}`

### Zamykanie sklepu:
1. Użytkownik klika chip statusu restauracji w top bar
2. Otwiera się `RestaurantStatusSheet` (bottom sheet)
3. Użytkownik klika "Zamknij sklep" (opcja z 🔴 czerwonym obramowaniem)
4. Pokazuje się `OpenCloseStoreDialog` z wyborem portali
5. Użytkownik wybiera portale (lub zostawia puste dla "wszystkie")
6. Klika czerwony przycisk "Potwierdź"
7. Wysyła się request: `PUT /admin/openhours` z `{"is_open": false, "portals": [...]}`

---

## 📡 Payload API

### Przykład 1: Otwórz wszystkie portale
```json
{
  "is_open": true,
  "portals": null
}
```

### Przykład 2: Otwórz tylko UBER i GLOVO
```json
{
  "is_open": true,
  "portals": ["UBER", "GLOVO"]
}
```

### Przykład 3: Zamknij tylko WOLT
```json
{
  "is_open": false,
  "portals": ["WOLT"]
}
```

---

## ✅ Gotowe do testowania

Wszystkie komponenty zostały zaimplementowane i są gotowe do użycia. Należy teraz zaktualizować miejsca w aplikacji, które wywołują `onOpen()` i `onClose()`, aby przekazywały wybrane portale.

**Następny krok:** Znaleźć i zaktualizować wszystkie wywołania `onOpen` i `onClose` w aplikacji (prawdopodobnie w `HomeActivity.kt` lub `HomeScreen.kt`), aby obsługiwały nowy parametr `portals`.

