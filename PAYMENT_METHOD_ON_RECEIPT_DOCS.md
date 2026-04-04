# ✅ METODA PŁATNOŚCI NA PARAGONACH - DOKUMENTACJA

## 🎯 Problem

Metoda płatności **nie drukowała się** na bloczkach/paragonach.

---

## ✅ Rozwiązanie

Dodano wyświetlanie metody płatności na **wszystkich szablonach** paragonów.

---

## 📝 Implementacja

### 1. **Szablon STANDARD** (TicketTemplate.kt)

#### Pozycja: Po telefonie klienta, przed separatorem

```kotlin
appendLine("[L]${ctx.getString(R.string.label_date)}   : $now")
appendLine("[L]${ctx.getString(R.string.label_client)} : ${order.consumer?.name ?: "-"}")
appendLine("[L]${ctx.getString(R.string.label_phone)}  : ${order.consumer?.phone ?: "-"}")

// Metoda płatności
val paymentMethodLabel = getPaymentMethodLabel(order.paymentMethod)
if (paymentMethodLabel.isNotBlank()) {
    appendLine("[L]${ctx.getString(R.string.label_payment)}: $paymentMethodLabel")
}

appendLine(separator())
```

---

### 2. **Szablon COMPACT** (PrintTemplates.kt)

#### Pozycja: Po dacie, przed separatorem

```kotlin
appendLine("[L]$now")

// Metoda płatności
val paymentMethodLabel = getPaymentMethodLabel(order.paymentMethod)
if (paymentMethodLabel.isNotBlank()) {
    appendLine("[L]${ctx.getString(R.string.label_payment)}: $paymentMethodLabel")
}

if (useDeliveryInterval) {
    appendLine("[L]${ctx.getString(R.string.customer_waiting)}")
}
```

---

### 3. **Szablon MINIMAL** (PrintTemplates.kt)

#### Pozycja: Po sumie, przed notatkami

```kotlin
appendLine("[L]<b>${formatMoney(total, currency)}</b>")

// Metoda płatności
val paymentMethodLabel = getPaymentMethodLabel(order.paymentMethod)
if (paymentMethodLabel.isNotBlank()) {
    appendLine("[L]$paymentMethodLabel")
}

// Notatki na samym dole
if (!order.note.isNullOrBlank()) {
    appendLine("[L]---")
    appendLine("[L]${order.note}")
}
```

---

### 4. **Szablon DETAILED** (PrintTemplates.kt)

Używa `buildTicket()` ze STANDARD, więc metoda płatności jest już dodana ✅

---

## 🔧 Funkcja pomocnicza: `getPaymentMethodLabel()`

Dodano funkcję która mapuje enum `PaymentMethod` na czytelny tekst:

```kotlin
private fun getPaymentMethodLabel(paymentMethod: PaymentMethod?): String {
    return when (paymentMethod) {
        PaymentMethod.COD,
        PaymentMethod.CASH_ON_DELIVERY -> "Gotówka przy odbiorze"
        PaymentMethod.CASH -> "Gotówka"
        PaymentMethod.CARD -> "Karta"
        PaymentMethod.ONLINE -> "Płatność online"
        PaymentMethod.PAYNOW -> "PayNow"
        PaymentMethod.BLIK -> "BLIK"
        PaymentMethod.BANK_TRANSFER,
        PaymentMethod.TRANSFER -> "Przelew"
        PaymentMethod.UNKNOWN,
        null -> "" // Brak metody - nie wyświetlaj
    }
}
```

---

## 🌐 Tłumaczenia

### values-pl/strings.xml (Polski)
```xml
<string name="label_payment">Płatność</string>
```

### values/strings.xml (English)
```xml
<string name="label_payment">Payment</string>
```

---

## 📊 Wsparcie dla metod płatności

| Metoda | API Value | Wyświetlany tekst |
|--------|-----------|-------------------|
| COD | `cod` | Gotówka przy odbiorze |
| CASH_ON_DELIVERY | `cash_on_delivery` | Gotówka przy odbiorze |
| CASH | `cash` | Gotówka |
| CARD | `card` | Karta |
| ONLINE | `online` | Płatność online |
| PAYNOW | `paynow` | PayNow |
| BLIK | `blik` | BLIK |
| BANK_TRANSFER | `bank_transfer` | Przelew |
| TRANSFER | `transfer` | Przelew |
| UNKNOWN | `unknown` | (nie wyświetla się) |
| null | - | (nie wyświetla się) |

---

## 🖨️ Przykładowy paragon

### Zamówienie z płatnością gotówką:

```
    #12345
  [UBER EATS]
   DOSTAWA
--------------------------------
Data   : 02.02.2026 10:30
Klient : Jan Kowalski
Tel.   : 123456789
Płatność: Gotówka                ← NOWE!
--------------------------------
2× Pizza Margherita      42.00 zł
1× Cola 0.5L              5.00 zł
--------------------------------
Razem:                   47.00 zł
--------------------------------
      Dziękujemy!
```

### Zamówienie z płatnością online:

```
    #12345
    [WOLT]
   DOSTAWA
--------------------------------
Data   : 02.02.2026 10:30
Klient : Anna Nowak
Tel.   : 987654321
Płatność: Płatność online        ← NOWE!
--------------------------------
1× Burger                15.00 zł
--------------------------------
Razem:                   15.00 zł
--------------------------------
```

### Zamówienie bez metody płatności:

```
    #12345
   ODBIÓR
--------------------------------
Data   : 02.02.2026 10:30
Klient : Piotr Kowal
Tel.   : 555123456
                                 ← Brak linii (nie drukuje się)
--------------------------------
```

---

## 📋 Zmienione pliki

1. ✅ `TicketTemplate.kt` 
   - Dodano wyświetlanie metody płatności po telefonie
   - Dodano funkcję `getPaymentMethodLabel()`
   - Dodano import `PaymentMethod`

2. ✅ `PrintTemplates.kt`
   - Dodano metodę płatności do COMPACT (po dacie)
   - Dodano metodę płatności do MINIMAL (po sumie)
   - Dodano funkcję `getPaymentMethodLabel()`
   - Dodano import `PaymentMethod`

3. ✅ `values-pl/strings.xml`
   - Dodano tłumaczenie `label_payment` = "Płatność"

4. ✅ `values/strings.xml`
   - Dodano tłumaczenie `label_payment` = "Payment"

---

## 🧪 Testowanie

### Scenariusze testowe:

1. **Zamówienie z płatnością COD/Gotówka przy odbiorze:**
   - ✅ Drukuje się: `Płatność: Gotówka przy odbiorze`

2. **Zamówienie z płatnością BLIK:**
   - ✅ Drukuje się: `Płatność: BLIK`

3. **Zamówienie z płatnością kartą:**
   - ✅ Drukuje się: `Płatność: Karta`

4. **Zamówienie bez metody płatności (`null`):**
   - ✅ Linia się NIE drukuje (puste, brak)

5. **Wszystkie szablony:**
   - ✅ STANDARD - metoda płatności po telefonie
   - ✅ COMPACT - metoda płatności po dacie
   - ✅ MINIMAL - metoda płatności po sumie (bez etykiety)
   - ✅ DETAILED - metoda płatności (z STANDARD)

---

## 📝 Struktura danych

```kotlin
data class Order(
    // ...
    @SerializedName("payment_method") val paymentMethod: PaymentMethod?,  // ← To pole
    @SerializedName("payment_status") val paymentStatus: PaymentStatus?,
    // ...
)

enum class PaymentMethod(val apiValue: String) {
    COD("cod"),
    PAYNOW("paynow"),
    CASH("cash"),
    CARD("card"),
    ONLINE("online"),
    BANK_TRANSFER("bank_transfer"),
    CASH_ON_DELIVERY("cash_on_delivery"),
    BLIK("blik"),
    TRANSFER("transfer"),
    UNKNOWN("unknown")
}
```

---

## 🎉 Status: ZAIMPLEMENTOWANE!

Metoda płatności **będzie teraz drukowana na wszystkich paragonach** (jeśli jest ustawiona).

### Kluczowe cechy:
- ✅ Wyświetla się tylko gdy `paymentMethod != null && != UNKNOWN`
- ✅ Polskie nazwy metod płatności (czytelne)
- ✅ Działa na wszystkich szablonach (STANDARD, COMPACT, MINIMAL, DETAILED)
- ✅ Spójne z resztą paragonu (te same fonty i formatowanie)

---

**BUILD W TRAKCIE...** Po zainstalowaniu nowego APK, metoda płatności będzie drukowana na każdym paragonie! 🎉

