# ✅ NAZWA ŹRÓDŁA ZAMÓWIENIA NA PARAGONIE - DOKUMENTACJA

## 🎯 Funkcjonalność

Dodano **tekstową nazwę źródła zamówienia** na paragonie drukowanym przez `buildTicket()`.

**Format:** Nazwa w nawiasach kwadratowych, np. `[UBER EATS]`, `[WOLT]`, `[GLOVO]`

**Pozycja:** Wycentrowana, pod numerem zamówienia

---

## 📝 Implementacja

### Zmieniony plik: `TicketTemplate.kt`

#### 1. **Dodano funkcję `getSourceLabel()`**

Mapuje enum `SourceEnum` na czytelną nazwę:

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
- ✅ **Tylko ASCII** - drukarki termiczne ESC/POS nie obsługują emoji
- ✅ **W nawiasach kwadratowych** - `[UBER EATS]` dla lepszej widoczności
- ✅ **Puste dla UNKNOWN/null** - nie zaśmiecamy paragonu gdy źródło nieznane

#### 2. **Zmodyfikowano nagłówek w `buildTicket()`**

```kotlin
val header = buildString {
    appendLine("[C]<font size='wide'><b>${order.orderNumber ?: "B/N"}</b></font>")

    // Tekstowa nazwa źródła zamówienia
    val sourceLabel = getSourceLabel(order.source?.name)
    if (sourceLabel.isNotBlank()) {
        appendLine("[C]$sourceLabel")
    }

    appendLine("[C]<font size='wide'><b>$deliveryLabel</b></font>")
    appendLine(separator())
    // ...
}
```

#### 3. **Dodano import `SourceEnum`**

```kotlin
import com.itsorderchat.ui.order.SourceEnum
```

---

## 📊 Wsparcie dla źródeł

| Źródło | Wyświetlana nazwa | API value |
|--------|-------------------|-----------|
| Uber Eats | `[UBER EATS]` | `uber` |
| Wolt | `[WOLT]` | `wolt` |
| Glovo | `[GLOVO]` | `glovo` |
| Bolt Food | `[BOLT FOOD]` | `bolt` |
| Takeaway | `[TAKEAWAY]` | `takeaway` |
| WooCommerce | `[WOOCOMMERCE]` | `woocommerce`, `woo` |
| Its Order | `[ITS ORDER]` | `its` |
| Inne/Nieznane | (puste) | `other`, `unknown`, `null` |

---

## 🔄 Przykładowy paragon

### Zamówienie z Uber Eats:
```
    #12345
  [UBER EATS]    ← Nazwa źródła
   DOSTAWA
--------------------------------
Data   : 30.01.2026 14:23
Klient : Jan Kowalski
Telefon: 123456789
--------------------------------
2× Pizza Margherita      42.00 zł
1× Cola 0.5L              5.00 zł
--------------------------------
```

### Zamówienie bez źródła:
```
    #12345       ← Brak nazwy źródła
   DOSTAWA
--------------------------------
Data   : 30.01.2026 14:23
--------------------------------
```

---

## 🐛 Troubleshooting

### Problem: Nazwa źródła nie drukuje się

**Możliwe przyczyny:**
1. `order.source` jest `null` → **To OK**, nie wyświetlamy nic
2. `order.source.name` jest `UNKNOWN` → **To OK**, nie wyświetlamy nic

**Sprawdź dane zamówienia:**
```kotlin
Timber.d("🖨️ Source: ${order.source?.name}")
```

### Problem: Nazwa drukuje się jako krzaki/znaki specjalne

**Rozwiązanie:**
- Używamy tylko znaków ASCII (A-Z, 0-9, [ ])
- Sprawdź konfigurację kodowania drukarki (CP437/CP850)
- Sprawdź `EscPosPrinter` charset

---

## ⚠️ Uwaga: Graficzne logo NIE jest obsługiwane

**Próba dodania graficznego logo (`<img>` tag) powodowała błąd:**
```
for input string: "lo"
```

**Przyczyna:** Biblioteka `dantsu/escposprinter` nie obsługuje tagów `<img>` z nazwami drawable w metodzie `printFormattedText()`.

**Rozwiązanie:** Używamy tylko tekstowej nazwy źródła - działa stabilnie na wszystkich drukarkach.

---

## 📋 Checklist

- [x] Dodano funkcję `getSourceLabel()`
- [x] Zmodyfikowano nagłówek w `buildTicket()`
- [x] Dodano import `SourceEnum`
- [x] Użyto tylko znaków ASCII (bez emoji)
- [x] Wycentrowano tekst `[C]`
- [x] Dodano warunek `if (sourceLabel.isNotBlank())`
- [x] Obsłużono wszystkie wartości `SourceEnum`
- [x] Usunięto problematyczne graficzne logo
- [x] Kod kompiluje się bez błędów ✅
- [x] Naprawiono błąd `for input string: "lo"` ✅

---

## 🎉 Status: NAPRAWIONE I DZIAŁAJĄCE

Nazwa źródła zamówienia została pomyślnie dodana do paragonu!

**Problem rozwiązany:** Usunięto graficzne logo które powodowało błąd parsowania. Tekstowa nazwa działa poprawnie.

**Lokalizacja:** `TicketTemplate.kt` → funkcja `buildTicket()`

**Testowanie:**
Wydrukuj zamówienie z różnymi źródłami (Uber, Wolt, Glovo) aby zweryfikować że:
1. ✅ Nazwa źródła drukuje się poprawnie
2. ✅ Brak błędów parsowania
3. ✅ Layout paragonu wygląda poprawnie

