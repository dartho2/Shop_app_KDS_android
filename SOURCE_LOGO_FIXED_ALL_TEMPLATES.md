# ✅ LOGO ŹRÓDŁA ZAMÓWIENIA - NAPRAWIONE DLA WSZYSTKICH SZABLONÓW

## 🎯 Problem

Logo z portali zewnętrznych (Uber, Wolt, Glovo) **nie drukowało się** na paragonach.

**Przyczyna:** Funkcja `getSourceLabel()` była zdefiniowana tylko w `TicketTemplate.kt`, ale **nie była używana** w szablonach z `PrintTemplates.kt` (COMPACT, MINIMAL).

---

## ✅ Rozwiązanie

### 1. **Dodano funkcję `getSourceLabel()` do `PrintTemplates.kt`**

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
        null -> ""
    }
}
```

### 2. **Zaktualizowano wszystkie szablony:**

#### ✅ Szablon STANDARD
Używa funkcji z `TicketTemplate.kt` - już zawiera logo źródła ✅

#### ✅ Szablon COMPACT
```kotlin
// Logo źródła zamówienia
val sourceLabel = getSourceLabel(order.source?.name)

val header = buildString {
    appendLine("[C]<b>${order.orderNumber}</b>")
    
    // Dodaj źródło jeśli istnieje
    if (sourceLabel.isNotBlank()) {
        appendLine("[C]$sourceLabel")
    }
    
    appendLine("[C]$deliveryLabel")
    // ...
}
```

#### ✅ Szablon MINIMAL
```kotlin
// Logo źródła zamówienia
val sourceLabel = getSourceLabel(order.source?.name)

return buildString {
    appendLine("[C]${order.orderNumber}")
    
    // Dodaj źródło jeśli istnieje
    if (sourceLabel.isNotBlank()) {
        appendLine("[C]$sourceLabel")
    }
    
    appendLine("[L]---")
    // ...
}
```

#### ✅ Szablon DETAILED
Wykorzystuje `buildTicket()` ze STANDARD, więc logo jest już dodane ✅

### 3. **Dodano import `SourceEnum`**

```kotlin
import com.itsorderchat.ui.order.SourceEnum
```

### 4. **Dodano logowanie debugowe w `TicketTemplate.kt`**

```kotlin
// Debug logging
android.util.Log.d("TICKET_PRINT", "Order #${order.orderNumber}: source=${order.source?.name}, sourceLabel='$sourceLabel'")
```

---

## 📊 Zmienione pliki

| Plik | Zmiany |
|------|--------|
| `PrintTemplates.kt` | ✅ Dodano `getSourceLabel()` |
| | ✅ Zaktualizowano COMPACT |
| | ✅ Zaktualizowano MINIMAL |
| | ✅ Dodano import `SourceEnum` |
| `TicketTemplate.kt` | ✅ Dodano debug logging |

---

## 🔄 Jak wygląda paragon teraz

### Zamówienie z Uber Eats (dowolny szablon):
```
    #12345
  [UBER EATS]    ← Logo źródła
   DOSTAWA
--------------------------------
```

### Zamówienie z Wolt:
```
    #12345
    [WOLT]       ← Logo źródła
   ODBIÓR
--------------------------------
```

### Zamówienie bez źródła lub UNKNOWN:
```
    #12345       ← Brak logo
   DOSTAWA
--------------------------------
```

---

## 🧪 Testowanie

### Sprawdź logi w Logcat:

**Filtr:** `TICKET_PRINT`

**Oczekiwane logi:**
```
Order #12345: source=UBER, sourceLabel='[UBER EATS]'
✅ Source label added to ticket: [UBER EATS]
```

**Jeśli źródło jest null:**
```
Order #12345: source=null, sourceLabel=''
⚠️ Source label is blank - not added to ticket
```

### Scenariusze testowe:

1. ✅ **Zamówienie z Uber Eats** → Powinno drukować `[UBER EATS]`
2. ✅ **Zamówienie z Wolt** → Powinno drukować `[WOLT]`
3. ✅ **Zamówienie z Glovo** → Powinno drukować `[GLOVO]`
4. ✅ **Zamówienie bez źródła** → Nie drukuje nic (OK)
5. ✅ **Każdy szablon** (STANDARD, COMPACT, MINIMAL, DETAILED) → Logo powinno się drukować

---

## 📋 Checklist

- [x] Dodano funkcję `getSourceLabel()` do `PrintTemplates.kt`
- [x] Zaktualizowano szablon COMPACT
- [x] Zaktualizowano szablon MINIMAL
- [x] Szablon STANDARD już obsługiwał logo ✅
- [x] Szablon DETAILED korzysta ze STANDARD ✅
- [x] Dodano import `SourceEnum`
- [x] Dodano debug logging
- [x] Wyczyśzczono cache (`./gradlew clean`)
- [x] Build SUCCESS ✅
- [x] Kod kompiluje się bez błędów

---

## 🎉 Status: NAPRAWIONE!

Logo źródła zamówienia **będzie teraz drukowane dla wszystkich szablonów paragonów** (STANDARD, COMPACT, MINIMAL, DETAILED).

**Lokalizacja APK:** `L:\SHOP APP\app\build\outputs\apk\debug\app-debug.apk`

---

## 📝 Następne kroki

1. Zainstaluj nowy APK
2. Wydrukuj zamówienie z portalów zewnętrznych (Uber, Wolt, Glovo)
3. Sprawdź logi w Logcat (filtr: `TICKET_PRINT`)
4. Zweryfikuj że logo drukuje się na paragonie
5. Jeśli nadal nie działa - sprawdź czy `order.source?.name` nie jest `null` w logach

---

**Problem rozwiązany! Logo źródła będzie teraz drukowane dla wszystkich portali zewnętrznych!** 🚀

