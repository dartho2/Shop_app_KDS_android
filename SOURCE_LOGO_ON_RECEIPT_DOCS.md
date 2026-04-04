# ✅ LOGO ŹRÓDŁA ZAMÓWIENIA NA PARAGONIE - DOKUMENTACJA

## 🎯 Funkcjonalność

Dodano **graficzne logo** oraz nazwę źródła zamówienia (source) na paragonie drukowanym przez `buildTicket()`.

**Dwa poziomy identyfikacji:**
1. 🖼️ **Graficzne logo** - na samej górze paragonu (bitmap/PNG)
2. 📝 **Tekstowa nazwa** - poniżej numeru zamówienia (ASCII, backup)

---

## 📝 Implementacja

### Zmieniony plik: `TicketTemplate.kt`

#### 1. **Dodano funkcję `getSourceLogoImage()`** 🆕

Generuje tag ESC/POS do drukowania graficznego logo:

```kotlin
private fun getSourceLogoImage(context: Context, source: SourceEnum?): String {
    val drawableName = when (source) {
        SourceEnum.UBER -> "logo_uber_80"
        SourceEnum.WOLT -> "logo_wolt_80"
        SourceEnum.GLOVO -> "logo_glovo_80"
        SourceEnum.BOLT -> "logo_bolt_80"
        SourceEnum.TAKEAWAY -> "logo_takeaway_80"
        SourceEnum.WOOCOMMERCE,
        SourceEnum.WOO -> "ic_woo"
        SourceEnum.ITS -> "logo"
        SourceEnum.OTHER,
        SourceEnum.UNKNOWN,
        null -> return "" // Brak źródła - nie drukuj logo
    }
    
    // Sprawdź czy drawable istnieje
    val resourceId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
    if (resourceId == 0) {
        Timber.w("⚠️ Logo drawable not found: $drawableName for source: $source")
        return ""
    }
    
    // Zwróć tag ESC/POS do drukowania obrazka (wycentrowany)
    return "[C]<img>$drawableName</img>"
}
```

**Kluczowe decyzje:**
- ✅ **Tag `<img>`** - biblioteka `dantsu/escposprinter` obsługuje drukowanie grafiki
- ✅ **Wycentrowanie `[C]`** - logo wyświetla się na środku paragonu
- ✅ **Walidacja drawable** - sprawdza czy plik istnieje przed drukowaniem
- ✅ **Logging** - loguje błąd jeśli drawable nie został znaleziony

#### 2. **Dodano funkcję `getSourceLabel()`**

#### 2. **Dodano funkcję `getSourceLabel()`**

Mapuje enum `SourceEnum` na czytelną nazwę tekstową (backup dla grafiki):

```kotlin
private fun getSourceLabel(source: SourceEnum?): String {
    return when (source) {
        SourceEnum.UBER -> "[UBER EATS]"
        SourceEnum.WOLT -> "[WOLT]"
        SourceEnum.GLOVO -> "[GLOVO]"
        SourceEnum.BOLT -> "[BOLT FOOD]"
        SourceEnum.TAKEAWAY -> "[TAKEAWAY]"
        SourceEnum.WOOCOMMERCE,
        SourceEnum.WOO -> "[WOOCOMMERCE]"
        SourceEnum.ITS -> "[ITS ORDER]"
        SourceEnum.OTHER,
        SourceEnum.UNKNOWN,
        null -> "" // Brak źródła - nie wyświetlaj nic
    }
}
```

**Kluczowe decyzje:**
- ✅ **Bez emoji** - drukarki termiczne ESC/POS nie obsługują Unicode emoji
- ✅ **W nawiasach kwadratowych** - `[UBER EATS]` dla lepszej widoczności
- ✅ **Puste dla UNKNOWN/null** - nie zaśmiecamy paragonu gdy źródło nieznane
- ✅ **Backup dla grafiki** - jeśli logo graficzne się nie wydrukuje, tekst nadal będzie

#### 3. **Zmodyfikowano nagłówek w `buildTicket()`** 🔥

Dodano **graficzne logo na samej górze** + tekstową nazwę jako backup:

```kotlin
/* ------ Nagłówek ------ */
val header = buildString {
    // 🖼️ Graficzne logo źródła zamówienia na samej górze
    val logoImage = getSourceLogoImage(context, order.source?.name)
    if (logoImage.isNotBlank()) {
        appendLine(logoImage)
        appendLine("") // Odstęp po logo
    }
    
    appendLine("[C]<font size='wide'><b>${order.orderNumber ?: "B/N"}</b></font>")

    // 📝 Tekstowa nazwa źródła (backup jeśli logo się nie wydrukuje)
    val sourceLabel = getSourceLabel(order.source?.name)
    if (sourceLabel.isNotBlank()) {
        appendLine("[C]$sourceLabel")
    }

    appendLine("[C]<font size='wide'><b>$deliveryLabel</b></font>")
    // ...
}
```

**Pozycja na paragonie:**
```
    [LOGO UBER]    ← 🖼️ GRAFIKA
  
    #12345
  [UBER EATS]      ← 📝 TEKST (backup)
   DOSTAWA
--------------------------------
```

#### 4. **Dodano importy**

```kotlin
import timber.log.Timber  // Do logowania błędów
// Context już był zaimportowany
```

---

## 📊 Wsparcie dla źródeł

| Źródło | Graficzne logo | Tekstowa nazwa | Drawable | API value |
|--------|----------------|----------------|----------|-----------|
| Uber Eats | 🖼️ `logo_uber_80.xml` | `[UBER EATS]` | ✅ | `uber` |
| Wolt | 🖼️ `logo_wolt_80.xml` | `[WOLT]` | ✅ | `wolt` |
| Glovo | 🖼️ `logo_glovo_80.xml` | `[GLOVO]` | ✅ | `glovo` |
| Bolt Food | 🖼️ `logo_bolt_80.xml` | `[BOLT FOOD]` | ✅ | `bolt` |
| Takeaway | 🖼️ `logo_takeaway_80.xml` | `[TAKEAWAY]` | ✅ | `takeaway` |
| WooCommerce | 🖼️ `ic_woo.xml` | `[WOOCOMMERCE]` | ✅ | `woocommerce`, `woo` |
| Its Order | 🖼️ `logo.xml` | `[ITS ORDER]` | ✅ | `its` |
| Inne/Nieznane | - | (puste) | - | `other`, `unknown`, `null` |

**Uwagi:**
- Wszystkie logo znajdują się w `res/drawable/`
- Rozmiar logo: 80px (optymalizacja dla drukarek termicznych)
- Format: XML Vector Drawable (konwertowane do bitmap przy drukowaniu)

---

## 🔄 Przykładowy paragon

### Przed zmianą:
```
    #12345
   DOSTAWA
--------------------------------
Data   : 30.01.2026 14:23
Klient : Jan Kowalski
Telefon: 123456789
--------------------------------
```

### Po zmianie (zamówienie z Uber Eats):
```
  [LOGO UBER]    ← 🖼️ Graficzne logo (80px)
  
    #12345
  [UBER EATS]    ← 📝 Tekstowa nazwa (backup)
   DOSTAWA
--------------------------------
Data   : 30.01.2026 14:23
Klient : Jan Kowalski
Telefon: 123456789
--------------------------------
```

### Po zmianie (zamówienie bez źródła):
```
    #12345       ← Brak logo i nazwy
   DOSTAWA
--------------------------------
Data   : 30.01.2026 14:23
Klient : Jan Kowalski
Telefon: 123456789
--------------------------------
```

### Po zmianie (zamówienie z Wolt):
```
  [LOGO WOLT]    ← 🖼️ Graficzne logo
  
    #12345
    [WOLT]       ← 📝 Tekstowa nazwa
   DOSTAWA
--------------------------------
```

---

## ⚙️ Szczegóły techniczne

### Struktura danych

**Order model:**
```kotlin
data class Order(
    val orderNumber: String,
    val source: SourceOrder?,  // ← To pole
    // ...
)

data class SourceOrder(
    val sourceId: String,
    val number: String,
    val name: SourceEnum  // ← Używamy tego
)

enum class SourceEnum(val apiValue: String) {
    UBER("uber"),
    WOLT("wolt"),
    GLOVO("glovo"),
    BOLT("bolt"),
    TAKEAWAY("takeaway"),
    WOOCOMMERCE("woocommerce"),
    WOO("woo"),
    ITS("its"),
    OTHER("other"),
    UNKNOWN("unknown")
}
```

### Wywołanie w kodzie

```kotlin
// W nagłówku paragonu
val sourceLabel = getSourceLabel(order.source?.name)
if (sourceLabel.isNotBlank()) {
    appendLine("[C]$sourceLabel")  // [C] = wycentrowane
}
```

**Safe call operator (`?.`):**
- Jeśli `order.source` jest `null` → zwraca `null` → `getSourceLabel(null)` → `""`
- Jeśli `order.source.name` jest `UNKNOWN` → zwraca `""`
- Tylko gdy źródło jest znane → wyświetla nazwę

---

## 🐛 Troubleshooting

### Problem: Graficzne logo nie drukuje się

**Możliwe przyczyny:**
1. `order.source` jest `null` → **To OK**, nie wyświetlamy nic
2. `order.source.name` jest `UNKNOWN` → **To OK**, nie wyświetlamy nic
3. Drawable nie istnieje → Sprawdź logi: `⚠️ Logo drawable not found`
4. Drukarka nie obsługuje grafiki → **Tekstowa nazwa się wydrukuje jako backup**

**Sprawdź logi:**
```kotlin
Timber.d("🖨️ Source: ${order.source?.name}")
Timber.w("⚠️ Logo drawable not found: $drawableName")
```

**Rozwiązanie:**
- Sprawdź czy pliki logo istnieją w `res/drawable/`
- Sprawdź nazwę drawable w funkcji `getSourceLogoImage()`
- Jeśli logo graficzne nie działa, tekstowa nazwa `[UBER EATS]` się wydrukuje

### Problem: Logo drukuje się jako czarne kwadraty/artefakty

**Przyczyna:** Drukarka termiczna ma problem z renderowaniem drawable XML  
**Rozwiązanie:**
- Biblioteka `dantsu/escposprinter` automatycznie konwertuje drawable na bitmap
- Jeśli problem występuje, zmniejsz rozmiar logo (aktualnie 80px)
- Upewnij się że drawable to prosty wektor (bez gradientów)

### Problem: Tekstowa nazwa drukuje się jako krzaki

**Rozwiązanie:** 
- Już używamy tylko znaków ASCII (A-Z, 0-9, [ ])
- Sprawdź `EscPosPrinter` charset configuration
- Upewnij się że drukarka obsługuje kodowanie CP437 lub CP850

---

## 📋 Checklist

- [x] Dodano funkcję `getSourceLogoImage()` dla graficznych logo
- [x] Dodano funkcję `getSourceLabel()` dla tekstowych nazw
- [x] Zmodyfikowano nagłówek w `buildTicket()` - logo na samej górze
- [x] Dodano import `Timber` dla logowania
- [x] Dodano import `SourceEnum`
- [x] Użyto tylko znaków ASCII dla tekstowych nazw (bez emoji)
- [x] Wycentrowano graficzne logo `[C]`
- [x] Wycentrowano tekstową nazwę `[C]`
- [x] Dodano warunek `if (logoImage.isNotBlank())`
- [x] Dodano warunek `if (sourceLabel.isNotBlank())`
- [x] Dodano walidację drawable `getIdentifier()`
- [x] Dodano logowanie błędów `Timber.w()`
- [x] Obsłużono wszystkie wartości `SourceEnum`
- [x] Kod kompiluje się bez błędów ✅

---

## 🎉 Status: ZAIMPLEMENTOWANE

Logo źródła zamówienia (graficzne + tekstowe) zostało pomyślnie dodane do paragonu!

**Lokalizacja:** `TicketTemplate.kt` → funkcja `buildTicket()`

**Wydruk testowy:**
Wydrukuj zamówienie z różnymi źródłami (Uber, Wolt, Glovo) aby zweryfikować:
1. Czy graficzne logo drukuje się na górze paragonu
2. Czy tekstowa nazwa drukuje się jako backup
3. Czy layout paragonu wygląda poprawnie

