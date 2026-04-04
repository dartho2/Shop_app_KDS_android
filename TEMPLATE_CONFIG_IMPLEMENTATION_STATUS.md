# Status Implementacji Konfigurowalnych Szablonów Wydruku

## ✅ CO ZOSTAŁO ZROBIONE

### 1. Model Danych (`Printer.kt`)

✅ **Utworzono `TemplateConfig`** - model konfiguracji szablonu z opcjami:
```kotlin
data class TemplateConfig(
    val showPrices: Boolean = true,              // Czy pokazywać ceny
    val showPaymentMethod: Boolean = true,       // Czy pokazywać formę płatności
    val productSpacing: Int = 0,                 // Odstępy między produktami (0-3)
    val showProductOptions: Boolean = true,      // Dodatki/modyfikatory
    val showCustomerNotes: Boolean = true,       // Uwagi klienta
    val showDeliveryAddress: Boolean = true,     // Adres dostawy
    val showOrderSource: Boolean = true,         // Źródło (Uber, Glovo)
    val showTaxes: Boolean = true,               // Podatki
    val showDiscounts: Boolean = true,           // Rabaty/kupony
    val compactMode: Boolean = false             // Tryb kompaktowy
)
```

✅ **Dodano do modelu `Printer`**:
```kotlin
data class Printer(
    // ...existing fields...
    val templateConfig: TemplateConfig = TemplateConfig.standard(),
    // ...
)
```

✅ **Predefiniowane konfiguracje**:
- `TemplateConfig.standard()` - pełny paragon (dla drukarki głównej)
- `TemplateConfig.kitchen()` - bilet kuchenny (bez cen, bez płatności)

### 2. Sygnatura Funkcji (`TicketTemplate.kt`)

✅ **Zmieniono sygnaturę `buildTicket`**:
```kotlin
fun buildTicket(
    ctx: Context, 
    order: Order, 
    config: TemplateConfig = TemplateConfig.standard(),  // ← NOWY PARAMETR
    useDeliveryInterval: Boolean = false
): String
```

### 3. Naprawiono Wywołania (`PrintTemplates.kt`)

✅ **Zaktualizowano wszystkie wywołania**:
- `buildStandardTicket` - używa `TemplateConfig.standard()`
- `buildDetailedTicket` - używa `TemplateConfig.standard()`

---

## ⏳ CO TRZEBA JESZCZE ZROBIĆ

### 1. Zmodyfikować Logikę `buildTicket` ✋ **NAJWAŻNIEJSZE**

Trzeba zmodyfikować funkcję `buildTicket` w `TicketTemplate.kt` żeby używała `config`:

#### a) Pokazywanie źródła zamówienia
```kotlin
// PRZED:
if (sourceLabel.isNotBlank()) {
    appendLine("[C]<font size='tall'>$sourceLabel</font>")
}

// PO:
if (config.showOrderSource && sourceLabel.isNotBlank()) {
    appendLine("[C]<font size='tall'>$sourceLabel</font>")
}
```

#### b) Pokazywanie adresu dostawy
```kotlin
// PRZED:
if (order.deliveryType in listOf(...)) {
    // wyświetl adres
}

// PO:
if (config.showDeliveryAddress && order.deliveryType in listOf(...)) {
    // wyświetl adres
}
```

#### c) Pokazywanie cen produktów
```kotlin
// PRZED:
appendLine(formatProductLine(
    qty = p.quantity,
    name = p.name,
    price = total,  // ← ZAWSZE
    currency = currency
))

// PO:
appendLine(formatProductLine(
    qty = p.quantity,
    name = p.name,
    price = if (config.showPrices) total else null,  // ← WARUNKOWO
    currency = if (config.showPrices) currency else ""
))
```

#### d) Odstępy między produktami
```kotlin
// PO KAŻDYM PRODUKCIE:
repeat(config.productSpacing) { appendLine() }
```

#### e) Pokazywanie opcji produktów
```kotlin
// W formatProductLine():
if (config.showProductOptions) {
    // wyświetl comment, note
}
```

#### f) Pokazywanie sum i cen
```kotlin
// PRZED:
val totals = buildString {
    appendLine(separator())
    appendLine("[L]Subtotal: ${fmtMoney(subtotal)}")
    // ...
}

// PO:
val totals = if (config.showPrices) {
    buildString {
        appendLine(separator())
        appendLine("[L]Subtotal: ${fmtMoney(subtotal)}")
        // ...
    }
} else {
    "" // Pusta sekcja gdy brak cen
}
```

#### g) Pokazywanie metody płatności
```kotlin
// PRZED:
val paymentLine = getPaymentMethodLabel(...)

// PO:
val paymentLine = if (config.showPaymentMethod) {
    getPaymentMethodLabel(...)
} else {
    ""
}
```

#### h) Pokazywanie rabatów
```kotlin
// PO:
if (config.showDiscounts && discount > 0.0) {
    appendLine("[L]Zniżka: -${fmtMoney(discount)}")
}
```

#### i) Pokazywanie podatków
```kotlin
// PO:
if (config.showTaxes && taxTotal > 0.0) {
    appendLine("[L]VAT: ${fmtMoney(taxTotal)}")
}
```

#### j) Tryb kompaktowy
```kotlin
// Zmniejsz nagłówek:
if (config.compactMode) {
    appendLine("[C]<b>${order.orderNumber}</b>")  // bez 'size=wide'
} else {
    appendLine("[C]<font size='wide'><b>${order.orderNumber}</b></font>")
}
```

---

### 2. Zaktualizować `PrinterService` ✅ **CZĘŚCIOWO ZROBIONE**

Trzeba przekazać `printer.templateConfig` do `buildTicket`:

```kotlin
// W PrinterService.kt:
private suspend fun printOne(target: TargetConfig, order: Order, useDeliveryInterval: Boolean) {
    // ...
    val ticket = buildTicket(
        ctx = context,
        order = order,
        config = target.printer.templateConfig,  // ← PRZEKAŻ CONFIG
        useDeliveryInterval = useDeliveryInterval
    )
    // ...
}
```

---

### 3. UI do Edycji Konfiguracji 🎨 **DO ZROBIENIA**

Trzeba stworzyć ekran edycji drukarki z sekcją "Konfiguracja szablonu":

#### Lokalizacja: `PrinterEditScreen.kt` (nowy plik)

```kotlin
@Composable
fun TemplateConfigSection(
    config: TemplateConfig,
    onConfigChange: (TemplateConfig) -> Unit
) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Konfiguracja szablonu", style = MaterialTheme.typography.titleMedium)
            
            // Ceny
            SwitchRow(
                label = "Pokazuj ceny",
                checked = config.showPrices,
                onCheckedChange = { onConfigChange(config.copy(showPrices = it)) }
            )
            
            // Forma płatności
            SwitchRow(
                label = "Pokazuj formę płatności",
                checked = config.showPaymentMethod,
                onCheckedChange = { onConfigChange(config.copy(showPaymentMethod = it)) }
            )
            
            // Odstępy między produktami
            SliderRow(
                label = "Odstępy między produktami",
                value = config.productSpacing,
                valueRange = 0f..3f,
                onValueChange = { onConfigChange(config.copy(productSpacing = it.toInt())) }
            )
            
            // Opcje produktów
            SwitchRow(
                label = "Pokazuj dodatki/modyfikatory",
                checked = config.showProductOptions,
                onCheckedChange = { onConfigChange(config.copy(showProductOptions = it)) }
            )
            
            // Uwagi klienta
            SwitchRow(
                label = "Pokazuj uwagi klienta",
                checked = config.showCustomerNotes,
                onCheckedChange = { onConfigChange(config.copy(showCustomerNotes = it)) }
            )
            
            // Adres dostawy
            SwitchRow(
                label = "Pokazuj adres dostawy",
                checked = config.showDeliveryAddress,
                onCheckedChange = { onConfigChange(config.copy(showDeliveryAddress = it)) }
            )
            
            // Źródło zamówienia
            SwitchRow(
                label = "Pokazuj źródło (Uber, Glovo)",
                checked = config.showOrderSource,
                onCheckedChange = { onConfigChange(config.copy(showOrderSource = it)) }
            )
            
            // Podatki
            SwitchRow(
                label = "Pokazuj podatki",
                checked = config.showTaxes,
                onCheckedChange = { onConfigChange(config.copy(showTaxes = it)) }
            )
            
            // Rabaty
            SwitchRow(
                label = "Pokazuj rabaty",
                checked = config.showDiscounts,
                onCheckedChange = { onConfigChange(config.copy(showDiscounts = it)) }
            )
            
            // Tryb kompaktowy
            SwitchRow(
                label = "Tryb kompaktowy",
                checked = config.compactMode,
                onCheckedChange = { onConfigChange(config.copy(compactMode = it)) }
            )
        }
    }
}
```

---

### 4. Dodać Stringi 📝 **DO ZROBIENIA**

#### `strings.xml` (EN)
```xml
<string name="template_config_section">Template Configuration</string>
<string name="template_show_prices">Show prices</string>
<string name="template_show_payment">Show payment method</string>
<string name="template_product_spacing">Product spacing</string>
<string name="template_show_options">Show product options</string>
<string name="template_show_notes">Show customer notes</string>
<string name="template_show_address">Show delivery address</string>
<string name="template_show_source">Show order source</string>
<string name="template_show_taxes">Show taxes</string>
<string name="template_show_discounts">Show discounts</string>
<string name="template_compact_mode">Compact mode</string>
```

#### `strings.xml` (PL)
```xml
<string name="template_config_section">Konfiguracja szablonu</string>
<string name="template_show_prices">Pokazuj ceny</string>
<string name="template_show_payment">Pokazuj formę płatności</string>
<string name="template_product_spacing">Odstępy między produktami</string>
<string name="template_show_options">Pokazuj dodatki/modyfikatory</string>
<string name="template_show_notes">Pokazuj uwagi klienta</string>
<string name="template_show_address">Pokazuj adres dostawy</string>
<string name="template_show_source">Pokazuj źródło zamówienia</string>
<string name="template_show_taxes">Pokazuj podatki</string>
<string name="template_show_discounts">Pokazuj rabaty</string>
<string name="template_compact_mode">Tryb kompaktowy</string>
```

---

### 5. Predefiniowane Szablony 📋 **OPCJONALNE**

Dodać przyciski "Szybka konfiguracja":

```kotlin
Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(
        onClick = { onConfigChange(TemplateConfig.standard()) },
        modifier = Modifier.weight(1f)
    ) {
        Text("Standardowy")
    }
    
    Button(
        onClick = { onConfigChange(TemplateConfig.kitchen()) },
        modifier = Modifier.weight(1f)
    ) {
        Text("Kuchenny")
    }
}
```

---

## 📊 Postęp

| Zadanie | Status | Priorytet |
|---------|--------|-----------|
| Model `TemplateConfig` | ✅ Gotowe | Wysoki |
| Dodanie do `Printer` | ✅ Gotowe | Wysoki |
| Zmiana sygnatury `buildTicket` | ✅ Gotowe | Wysoki |
| Naprawienie wywołań | ✅ Gotowe | Wysoki |
| Logika warunkowa w `buildTicket` | ⏳ **DO ZROBIENIA** | **KRYTYCZNY** |
| Przekazanie config w `PrinterService` | ⏳ DO ZROBIENIA | Wysoki |
| UI edycji konfiguracji | ⏳ DO ZROBIENIA | Średni |
| Stringi | ⏳ DO ZROBIENIA | Średni |
| Testy | ⏳ DO ZROBIENIA | Niski |

---

## 🚀 Następne Kroki

1. **KRYTYCZNE**: Zmodyfikuj `buildTicket` w `TicketTemplate.kt` żeby używał `config`
2. **WAŻNE**: Przekaż `templateConfig` w `PrinterService`
3. **UI**: Stwórz ekran edycji konfiguracji
4. **Stringi**: Dodaj tłumaczenia
5. **Testy**: Przetestuj na urządzeniu

---

## 💡 Przykład Użycia (Po Implementacji)

```kotlin
// Drukarka główna - pełny paragon
val mainPrinter = Printer(
    id = "main-1",
    name = "Drukarka Główna",
    templateConfig = TemplateConfig.standard()
)

// Drukarka kuchenna - bez cen, bez płatności
val kitchenPrinter = Printer(
    id = "kitchen-1",
    name = "Kuchnia",
    templateConfig = TemplateConfig.kitchen()
)

// Niestandardowa konfiguracja
val customPrinter = Printer(
    id = "custom-1",
    name = "Bar",
    templateConfig = TemplateConfig(
        showPrices = false,
        showPaymentMethod = false,
        productSpacing = 2,
        showProductOptions = true,
        showCustomerNotes = false,
        showDeliveryAddress = false,
        showOrderSource = true,
        showTaxes = false,
        showDiscounts = false,
        compactMode = true
    )
)
```

---

**Data**: 2026-02-02  
**Status**: W trakcie implementacji (60% gotowe)  
**Następny krok**: Zmodyfikować logikę `buildTicket`

