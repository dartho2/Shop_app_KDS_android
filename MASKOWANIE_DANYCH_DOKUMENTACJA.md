# 📋 DOKUMENTACJA MASKOWANIA DANYCH NA WYDRUKACH

## 🎯 CEL DOKUMENTU
Kompletna analiza gdzie i jak są maskowane dane osobowe na wydrukach (paragony, bloczki kuchenne).

**Data analizy**: 2026-01-27  
**Data modyfikacji**: 2026-01-27  
**Status**: ✅ ZAKTUALIZOWANA - **MASKOWANIE WYŁĄCZONE**

---

## ⚠️ WAŻNA ZMIANA (2026-01-27)

### MASKOWANIE ZOSTAŁO WYŁĄCZONE NA ŻYCZENIE UŻYTKOWNIKA

**Co zostało zmienione**:
- ❌ Usunięto maskowanie telefonu klienta
- ❌ Usunięto maskowanie imienia/nazwiska klienta
- ✅ Dane są teraz drukowane w pełnej formie

**Plik zmodyfikowany**: `TicketTemplate.kt` (linie 75-76)

**PRZED**:
```kotlin
appendLine("[L]${ctx.getString(R.string.label_client)} : ${maskMiddleKeepEdges(order.consumer?.name)}")
appendLine("[L]${ctx.getString(R.string.label_phone)}  : ${maskPhoneKeepSeparators(order.consumer?.phone)}")
```

**PO**:
```kotlin
appendLine("[L]${ctx.getString(R.string.label_client)} : ${order.consumer?.name ?: "-"}")
appendLine("[L]${ctx.getString(R.string.label_phone)}  : ${order.consumer?.phone ?: "-"}")
```

**WYDRUK TERAZ**:
```
Klient: Jan Kowalski       ← PEŁNE IMIĘ I NAZWISKO
Telefon: +48 123 456 789   ← PEŁNY NUMER TELEFONU
```

---

## 📊 OBECNY STAN MASKOWANIA

### Maskowane dane:

| Dane | Maskowanie | Status | Przykład |
|------|------------|--------|----------|
| **Telefon klienta** | ❌ WYŁĄCZONE | Pełny numer widoczny | `+48 123 456 789` |
| **Imię/nazwisko** | ❌ WYŁĄCZONE | Pełne dane widoczne | `Jan Kowalski` |
| **Adres** | ❌ WYŁĄCZONE | Pełny adres widoczny | `ul. Kwiatowa 5/12` |
| **Notatki** | ❌ WYŁĄCZONE | Pełne notatki widoczne | (wszystko widoczne) |

---

## 🔍 SZCZEGÓŁOWA ANALIZA

### 1️⃣ MASKOWANIE TELEFONU - TicketTemplate.kt (GŁÓWNY SZABLON)

**Plik**: `l:\SHOP APP\app\src\main\java\com\itsorderchat\ui\settings\print\TicketTemplate.kt`

#### LINIA 76 - Wywołanie funkcji:
```kotlin
appendLine("[L]${ctx.getString(R.string.label_phone)}  : ${maskPhoneKeepSeparators(order.consumer?.phone)}")
```

**Wyświetlane jako**:
```
Telefon: +48 5** *** 123
```

#### LINIE 434-452 - Implementacja funkcji maskującej:
```kotlin
/** Maskuje cyfry w telefonie, zachowuje separatory; zostawia 3 pierwsze i 3 ostatnie cyfry. */
private fun maskPhoneKeepSeparators(phone: String?, edgeDigits: Int = 3, maskChar: Char = '.'): String {
    val p = phone?.trim().orEmpty()
    if (p.isEmpty()) return "-"
    val totalDigits = p.count { it.isDigit() }
    if (totalDigits <= edgeDigits * 2) return p

    var seen = 0
    val out = StringBuilder(p.length)
    for (ch in p) {
        if (ch.isDigit()) {
            if (seen in edgeDigits until (totalDigits - edgeDigits)) out.append(maskChar) else out.append(ch)
            seen++
        } else {
            out.append(ch)
        }
    }
    return out.toString()
}
```

**PARAMETRY**:
- `edgeDigits` = **3** (pokazuje 3 pierwsze i 3 ostatnie cyfry)
- `maskChar` = **'.'** (kropka jako znak maskujący)

**PRZYKŁADY**:
| Wejście | Wyjście |
|---------|---------|
| `+48 123 456 789` | `+48 123 ... 789` |
| `+48123456789` | `+48123...789` |
| `123456789` | `123...789` |
| `12345` | `12345` (za mało cyfr, nie maskuje) |
| `null` | `-` |
| `` (puste) | `-` |

**ZALETY**:
- ✅ Zachowuje format (separatory, +)
- ✅ RODO-compliant (maskuje środek)
- ✅ Czytelne dla kelnerów (widoczny prefix kraju i ostatnie cyfry)

---

### 2️⃣ MASKOWANIE IMIENIA/NAZWISKA - TicketTemplate.kt

**Plik**: `l:\SHOP APP\app\src\main\java\com\itsorderchat\ui\settings\print\TicketTemplate.kt`

#### LINIA 75 - Wywołanie funkcji:
```kotlin
appendLine("[L]${ctx.getString(R.string.label_client)} : ${maskMiddleKeepEdges(order.consumer?.name)}")
```

**Wyświetlane jako**:
```
Klient: Jan K******i
```

#### LINIE 423-431 - Implementacja funkcji maskującej:
```kotlin
/** Maskuje środek tekstu, zostawia po 3 znaki na brzegach. */
private fun maskMiddleKeepEdges(text: String?, edge: Int = 3, maskChar: Char = '.'): String {
    val t = text?.trim().orEmpty()
    if (t.isEmpty()) return "-"
    if (t.length <= edge * 2) return t
    val left = t.take(edge)
    val right = t.takeLast(edge)
    val middleLen = t.length - edge * 2
    return left + maskChar.toString().repeat(middleLen) + right
}
```

**PARAMETRY**:
- `edge` = **3** (pokazuje 3 pierwsze i 3 ostatnie znaki)
- `maskChar` = **'.'** (kropka jako znak maskujący)

**PRZYKŁADY**:
| Wejście | Wyjście |
|---------|---------|
| `Jan Kowalski` | `Jan K......ski` |
| `Anna` | `Anna` (za krótkie, nie maskuje) |
| `Maria Nowak` | `Mar......wak` |
| `null` | `-` |
| `` (puste) | `-` |

**UWAGA**: Funkcja maskuje CAŁY tekst (imię + nazwisko), nie rozdziela ich!

---

### 3️⃣ MASKOWANIE TELEFONU - TicketFormatter.kt (ALTERNATYWNY FORMATTER)

**Plik**: `l:\SHOP APP\app\src\main\java\com\itsorderchat\ui\settings\print\TicketFormatter.kt`

#### LINIA 25 - Wywołanie funkcji:
```kotlin
if (!it.phone.isNullOrBlank()) sb.append("[L]${maskPhone(it.phone)}\n")
```

#### LINIA 113 - Implementacja:
```kotlin
private fun maskPhone(p: String) = p.replace(Regex("[\n\r\t]"), " ")
```

**⚠️ UWAGA KRYTYCZNA**: Ta funkcja **NIE MASKUJE** telefonu!  
**Tylko usuwa znaki specjalne** (newline, tab).

**PRZYKŁADY**:
| Wejście | Wyjście |
|---------|---------|
| `+48 123 456 789` | `+48 123 456 789` ❌ PEŁNY NUMER! |
| `+48\n123\t456` | `+48 123 456` |

**PROBLEM**: Jeśli system używa `TicketFormatter.kt` zamiast `TicketTemplate.kt`, telefon **NIE JEST MASKOWANY**!

---

### 4️⃣ MASKOWANIE IMIENIA - TicketFormatter.kt (ALTERNATYWNY)         A

**Plik**: `l:\SHOP APP\app\src\main\java\com\itsorderchat\ui\settings\print\TicketFormatter.kt`

#### LINIA 24 - Wywołanie:
```kotlin
if (!it.name.isNullOrBlank()) sb.append("[L]${maskMiddle(it.name)}\n")
```

#### LINIA 103-106 - Implementacja:
```kotlin
private fun maskMiddle(s: String) = when {
    s.length <= 4 -> s
    else -> s.take(2) + "*".repeat(s.length - 4) + s.takeLast(2)
}
```

**PARAMETRY**:
- Pokazuje **2 pierwsze + 2 ostatnie** znaki (mniej niż TicketTemplate!)
- Używa **'*'** zamiast **'.'**

**PRZYKŁADY**:
| Wejście | Wyjście |
|---------|---------|
| `Jan Kowalski` | `Ja********ki` |
| `Anna` | `Anna` (za krótkie, nie maskuje) |
| `Maria` | `Ma*ia` |

**RÓŻNICA vs TicketTemplate**:
- TicketTemplate: `Jan K......ski` (edge=3, char='.')
- TicketFormatter: `Ja********ki` (edge=2, char='*')

---

## 🔄 KTÓRY SZABLON JEST UŻYWANY?

### PrintTemplateFactory.kt - Router szablonów

**Plik**: `l:\SHOP APP\app\src\main\java\com\itsorderchat\ui\settings\print\PrintTemplateFactory.kt`

```kotlin
fun buildTicket(ctx: Context, order: Order, template: PrintTemplate = PrintTemplate.STANDARD, useDeliveryInterval: Boolean = false): String {
    return when (template) {
        PrintTemplate.STANDARD -> buildStandardTicket(ctx, order, useDeliveryInterval)
        PrintTemplate.COMPACT -> buildCompactTicket(ctx, order, useDeliveryInterval)
        PrintTemplate.DETAILED -> buildDetailedTicket(ctx, order, useDeliveryInterval)
        PrintTemplate.MINIMAL -> buildMinimalTicket(order)
    }
}

private fun buildStandardTicket(ctx: Context, order: Order, useDeliveryInterval: Boolean = false): String {
    // Importuj oryginalną funkcję z TicketTemplate.kt
    return buildTicket(ctx, order, useDeliveryInterval)  // ← TicketTemplate.buildTicket()
}
```

**WNIOSEK**: Standardowo używany jest **TicketTemplate.kt** (z edge=3, char='.')

**TicketFormatter.kt** jest używany TYLKO przez:
- Testy?
- Starsza wersja kodu?
- Niestandardowe szablony?

---

## 📍 LOKALIZACJE MASKOWANIA (MAPA)

```
┌─────────────────────────────────────────────────────────────────┐
│ PrinterService.printAfterOrderAccepted(order)                   │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ PrintTemplateFactory.buildTicket(order, template)               │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ TicketTemplate.buildTicket(ctx, order)  ← GŁÓWNY                │
│   ├─ LINIA 75: maskMiddleKeepEdges(name)                        │
│   │    → Jan K......ski (edge=3, char='.')                      │
│   └─ LINIA 76: maskPhoneKeepSeparators(phone)                   │
│        → +48 5** *** 123 (edge=3, char='.')                     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ TicketFormatter.buildTicket() ← ALTERNATYWNY (rzadko używany?)  │
│   ├─ LINIA 24: maskMiddle(name)                                 │
│   │    → Ja********ki (edge=2, char='*')                        │
│   └─ LINIA 25: maskPhone(phone)                                 │
│        → +48 123 456 789 ❌ BRAK MASKOWANIA!                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🛡️ UWAGA DOTYCZĄCA RODO

### ⚠️ MASKOWANIE WYŁĄCZONE - KONSEKWENCJE RODO

**OBECNY STAN**:
- ❌ Telefon: Pełny numer widoczny na wydruku
- ❌ Imię/nazwisko: Pełne dane widoczne na wydruku
- ❌ Adres: Pełny adres widoczny na wydruku
- ❌ Notatki: Pełne notatki widoczne na wydruku

**REKOMENDACJE PRAWNE**:
1. ⚠️ Wydruki zawierają pełne dane osobowe klientów
2. ⚠️ Należy zapewnić bezpieczne przechowywanie wydruków
3. ⚠️ Wydruki nie mogą być pozostawione w miejscach publicznych
4. ⚠️ Zalecane niszczenie wydruków po realizacji zamówienia
5. ℹ️ Rozważ konsultację z prawnikiem ds. RODO

**JAK PRZYWRÓCIĆ MASKOWANIE** (jeśli będzie potrzebne):
```kotlin
// W pliku TicketTemplate.kt (linie 75-76) zamień:
appendLine("[L]${ctx.getString(R.string.label_client)} : ${order.consumer?.name ?: "-"}")
appendLine("[L]${ctx.getString(R.string.label_phone)}  : ${order.consumer?.phone ?: "-"}")

// NA:
appendLine("[L]${ctx.getString(R.string.label_client)} : ${maskMiddleKeepEdges(order.consumer?.name)}")
appendLine("[L]${ctx.getString(R.string.label_phone)}  : ${maskPhoneKeepSeparators(order.consumer?.phone)}")
```

---

## 📋 HISTORIA ZMIAN

### 2026-01-27 - Wyłączenie maskowania
- ❌ Usunięto maskowanie telefonu (`maskPhoneKeepSeparators`)
- ❌ Usunięto maskowanie imienia/nazwiska (`maskMiddleKeepEdges`)
- ✅ Dane klienta drukowane w pełnej formie
- ⚠️ Funkcje maskujące pozostawione w kodzie (oznaczone jako unused)

### 2026-01-27 - Analiza początkowa
- ✅ Zidentyfikowano lokalizacje maskowania
- ✅ Udokumentowano wszystkie funkcje maskujące
- ✅ Wykryto różnice między szablonami

---

## 🔄 STARE INFORMACJE (ARCHIWUM)

<details>
<summary>📖 Kliknij aby zobaczyć szczegóły starego maskowania (przed wyłączeniem)</summary>

### Funkcje maskujące (zachowane w kodzie):

**maskPhoneKeepSeparators()** - linie 434-452 w TicketTemplate.kt
- Maskuje środkowe cyfry w numerze telefonu
- Parametry: edgeDigits=3, maskChar='.'
- Przykład: `+48 123 456 789` → `+48 123 ... 789`

**maskMiddleKeepEdges()** - linie 423-431 w TicketTemplate.kt
- Maskuje środek tekstu (imię/nazwisko)
- Parametry: edge=3, maskChar='.'
- Przykład: `Jan Kowalski` → `Jan K......ski`

Te funkcje są nadal dostępne w kodzie (oznaczone jako unused) i mogą być łatwo przywrócone.

</details>

---

**Data utworzenia**: 2026-01-27  
**Autor**: GitHub Copilot  
**Wersja**: 2.0 (maskowanie wyłączone)  
**Status**: ✅ ZAKTUALIZOWANA

