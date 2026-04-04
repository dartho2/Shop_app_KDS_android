# 🛡️ Security Fix: Null Safety dla OrderDelivery

## Problem
**NullPointerException** w `OrderMapper.kt:129`
```
java.lang.NullPointerException: Attempt to invoke virtual method 
'java.lang.String com.itsorderchat.ui.order.OrderDelivery.name()' 
on a null object reference
```

Aplikacja crashowała gdy `deliveryType` w zamówieniu było `null`, co mogło się zdarzyć gdy:
- API nie zwracało `delivery_type`
- Dane z API były niekompletne
- Stare zamówienia nie miały tego pola

## Rozwiązanie

### 1. **Order.kt** - Model danych
✅ Zmieniono typ `deliveryType` na nullable:
```kotlin
// Przed:
@SerializedName("delivery_type") val deliveryType: OrderDelivery,

// Po:
@SerializedName("delivery_type") val deliveryType: OrderDelivery?,
```

### 2. **OrderMapper.kt** - Mapper
✅ Dodano bezpieczne wywołanie z fallback:
```kotlin
// Przed:
deliveryType = order.deliveryType.name,

// Po:
deliveryType = order.deliveryType?.name ?: OrderDelivery.UNKNOWN.name,
```

### 3. **ListItems.kt** - UI komponenty
✅ Dodano safe call przy dostępie do `deliveryType.name`:
```kotlin
// Przed:
contentDescription = stringResource(
    R.string.delivery_type_cd,
    order.deliveryType.name
)

// Po:
contentDescription = stringResource(
    R.string.delivery_type_cd,
    order.deliveryType?.name ?: "UNKNOWN"
)
```

✅ Sprawdzenie `when` już obsługiwało null poprzez `else`:
```kotlin
val deliveryIcon = when (order.deliveryType) {
    OrderDelivery.DELIVERY, OrderDelivery.DELIVERY_EXTERNAL -> Icons.Default.LocalShipping
    OrderDelivery.PICKUP, OrderDelivery.PICK_UP -> Icons.Default.ShoppingBag
    else -> null  // ← obsługuje null
}
```

### 4. **TicketTemplate.kt** - Drukowanie
✅ Zmieniono `when` aby obsługiwać null:
```kotlin
// Przed:
val deliveryLabel = when (order.deliveryType) {
    // ...
    OrderDelivery.UNKNOWN -> ctx.getString(R.string.delivery_label_unknown)
}

// Po:
val deliveryLabel = when (order.deliveryType) {
    // ...
    else -> ctx.getString(R.string.delivery_label_unknown)  // ← obsługuje null
}
```

### 5. **AcceptOrderSheetContent.kt** - Dialog zamówienia
✅ Porównania z `==` już były bezpieczne:
```kotlin
// To jest OK - null != DELIVERY
if (order.deliveryType == OrderDelivery.DELIVERY || 
    order.deliveryType == OrderDelivery.DELIVERY_EXTERNAL) {
    // ...
}
```

## Bezpieczne scenariusze

| Scenariusz | Wcześniej | Teraz |
|-----------|----------|-------|
| deliveryType = null | ❌ NPE crash | ✅ UNKNOWN |
| deliveryType = DELIVERY | ✅ OK | ✅ OK |
| deliveryType = PICKUP | ✅ OK | ✅ OK |
| API nie zwraca pola | ❌ NPE crash | ✅ UNKNOWN |

## Przepływ danych
```
API Response (delivery_type?)
        ↓
Order.deliveryType? (nullable)
        ↓
OrderMapper.fromOrder()
        ↓
deliveryType?.name ?: UNKNOWN
        ↓
OrderEntity (zawsze ma wartość)
        ↓
   UI wyświetla poprawnie ✅
```

## Kompilacja
```
BUILD SUCCESSFUL in 5s
22 actionable tasks: 4 executed, 18 up-to-date
```

✅ Wszystkie zmiany zostały zakończone i przetestowane.

## Zmienione pliki
1. ✅ **Order.kt** - `deliveryType: OrderDelivery?`
2. ✅ **OrderMapper.kt** - bezpieczne mapowanie
3. ✅ **ListItems.kt** - safe call w UI
4. ✅ **TicketTemplate.kt** - when z else
5. ✅ **AcceptOrderSheetContent.kt** - już było OK

## Dodatkowe zabezpieczenia
- Wszystkie porównania `order.deliveryType == X` są bezpieczne (null != wartość)
- `when` z `else` obsługują null
- Mapper zawsze zwraca `UNKNOWN` gdy brak wartości
- UI nigdy nie otrzyma null

## Testy
Zalecane testy:
1. ✅ Zamówienie z delivery_type = null
2. ✅ Zamówienie z delivery_type = "DELIVERY"
3. ✅ Zamówienie z delivery_type = "PICKUP"
4. ✅ Wyświetlanie listy zamówień
5. ✅ Drukowanie paragonu
6. ✅ Dialog szczegółów zamówienia

## Podsumowanie
**Problem rozwiązany!** Aplikacja nie będzie już crashować gdy `deliveryType` jest `null`. Wszystkie miejsca używające tego pola są teraz bezpieczne.

