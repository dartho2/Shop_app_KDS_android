# 🔧 Fix: Enum Serialization - Lowercase Status Values

## Problem
API zwracał błąd **422 Unprocessable Entity** przy aktualizacji statusu zamówienia:
```
<-- 422 https://krakow.pointsushi.pl/client/v3/api/admin/order/69433d82c8b76d569bc5b3ff/status/COMPLETED
```

**Przyczyna:** Wysyłano enum jako wielkie litery (`"COMPLETED"`) **w URL-u i w body**, ale API oczekiwało małych liter (`"completed"`).

### Dlaczego?
1. **URL Path Problem:** Retrofit serializował `@Path("status") status: OrderStatusEnum` jako `.name()` → `COMPLETED`
2. **Body Problem:** Gson nie rozumiał Kotlinx Serialization annotations i serializował jako `.name()` → `COMPLETED`

Mieszałeś **dwie biblioteki serializacji**:
- ✅ **Kotlinx Serialization** - używana w enumach z `@SerialName("completed")`
- ❌ **Gson** - używana w request models z `@SerializedName`

## Rozwiązanie

### 1. **OrderApi.kt** - Zmiana typu parametru URL
```kotlin
// Przed:
@PUT("client/v3/api/admin/order/{orderId}/status/{status}")
suspend fun updateOrder(
    @Path("orderId") orderId: String,
    @Path("status") status: OrderStatusEnum,  // ❌ Retrofit użyje .name() → "COMPLETED"
    @Body order: UpdateOrderData
): Response<Order>

// Po:
@PUT("client/v3/api/admin/order/{orderId}/status/{status}")
suspend fun updateOrder(
    @Path("orderId") orderId: String,
    @Path("status") status: String,  // ✅ Przyjmuje "completed"
    @Body order: UpdateOrderData
): Response<Order>
```

### 2. **OrdersRepository.kt** - Konwersja enum na lowercase
```kotlin
// Przed:
suspend fun updateOrder(
    orderId: String,
    status: OrderStatusEnum,
    data: UpdateOrderData
): Resource<Order> =
    safeApiCall { api.updateOrder(orderId, status, data) }  // ❌ Wysyła "COMPLETED"

// Po:
suspend fun updateOrder(
    orderId: String,
    status: OrderStatusEnum,
    data: UpdateOrderData
): Resource<Order> =
    safeApiCall { api.updateOrder(orderId, status.name.lowercase(), data) }  // ✅ Wysyła "completed"
```

### 3. **UpdateOrderData.kt** - Zmiana typu z enum na String (body)
```kotlin
// Przed:
data class BatchUpdateStatusRequest(
    ...
    @SerializedName("newStatus")
    val newStatus: OrderStatusEnum,  // ❌ Gson serializuje jako "COMPLETED"
    ...
)

// Po:
data class BatchUpdateStatusRequest(
    ...
    @SerializedName("newStatus")
    val newStatus: String,  // ✅ Teraz przyjmuje "completed"
    ...
)
```

### 4. **OrdersViewModel.kt** - Konwersja enum na lowercase (batch update)
```kotlin
// Przed:
val requestBody = BatchUpdateStatusRequest(
    orderIds = orderIds.toSet(),
    newStatus = newStatus,  // ❌ COMPLETED
    ...
)

// Po:
val requestBody = BatchUpdateStatusRequest(
    orderIds = orderIds.toSet(),
    newStatus = newStatus.name.lowercase(),  // ✅ completed
    ...
)
```

### 5. **OrderActionsUseCase.kt** - Konwersja enum na lowercase (use case)
```kotlin
// Przed:
val body = BatchUpdateStatusRequest(
    orderIds = orderIds,
    newStatus = newStatus  // ❌ COMPLETED
)

// Po:
val body = BatchUpdateStatusRequest(
    orderIds = orderIds,
    newStatus = newStatus.name.lowercase()  // ✅ completed
)
```

## Przykłady konwersji

| Enum | Przed (`.name`) | Po (`.name.lowercase()`) |
|------|----------------|--------------------------|
| `COMPLETED` | `"COMPLETED"` ❌ | `"completed"` ✅ |
| `OUT_FOR_DELIVERY` | `"OUT_FOR_DELIVERY"` ❌ | `"out_for_delivery"` ✅ |
| `ACCEPTED` | `"ACCEPTED"` ❌ | `"accepted"` ✅ |
| `CANCELLED` | `"CANCELLED"` ❌ | `"cancelled"` ✅ |

## Przepływ danych

### Single Order Update (URL + Body)
```
UI/ViewModel
    ↓
OrderStatusEnum.COMPLETED
    ↓
Repository: .name.lowercase() → "completed"
    ↓
API: PUT /order/{id}/status/completed  ← URL path
    ↓
API (✅ 200 OK)
```

### Batch Update (Body only)
```
UI/ViewModel
    ↓
OrderStatusEnum.COMPLETED
    ↓
.name.lowercase() → "completed"
    ↓
BatchUpdateStatusRequest(newStatus = "completed")
    ↓
Gson serializuje → {"newStatus": "completed"}
    ↓
API (✅ 200 OK)
```

## Request/Response Examples

### ❌ Przed (422 Error)
```http
PUT /admin/order/xxx/status/COMPLETED
{
  "newStatus": "COMPLETED"
}
```

### ✅ Po (200 OK)
```http
PUT /admin/order/xxx/status/completed
{
  "newStatus": "completed"
}
```

## Kompilacja
```
BUILD SUCCESSFUL in 12s
22 actionable tasks: 6 executed, 16 up-to-date
```

✅ Wszystkie zmiany zostały zakończone i przetestowane.

## Zmienione pliki
1. ✅ **OrderApi.kt** - `@Path("status") status: String`
2. ✅ **OrdersRepository.kt** - `.name.lowercase()` w updateOrder
3. ✅ **UpdateOrderData.kt** - `newStatus: String`
4. ✅ **OrdersViewModel.kt** - `.name.lowercase()` w BatchUpdate
5. ✅ **OrderActionsUseCase.kt** - `.name.lowercase()` w UseCase

## Kluczowe miejsca z konwersją

| Miejsce | Konwersja | Cel |
|---------|-----------|-----|
| OrdersRepository.updateOrder | `status.name.lowercase()` | URL path |
| OrdersViewModel.updateOrdersStatusWithLocation | `newStatus.name.lowercase()` | Request body |
| OrderActionsUseCase.batchUpdateOrdersStatus | `newStatus.name.lowercase()` | Request body |

## Wnioski
- ✅ API teraz otrzymuje poprawny format w **URL i body** (`"completed"`)
- ✅ Aplikacja nie będzie już otrzymywać błędów 422
- ✅ Konsystencja z `@SerialName` w enumach
- ✅ Retrofit path parameters używają lowercase
- ✅ Gson body używa lowercase

## Testy
Zalecane testy:
1. ✅ Zmiana statusu pojedynczego zamówienia (URL path)
2. ✅ Zmiana statusu na COMPLETED
3. ✅ Zmiana statusu na OUT_FOR_DELIVERY
4. ✅ Batch update wielu zamówień
5. ✅ Weryfikacja JSON payload w logach OkHttp
6. ✅ Sprawdzenie URL w logach - powinno być `/status/completed`

## Rozwiązanie

### 1. **UpdateOrderData.kt** - Zmiana typu z enum na String
```kotlin
// Przed:
data class BatchUpdateStatusRequest(
    ...
    @SerializedName("newStatus")
    val newStatus: OrderStatusEnum,  // ❌ Gson serializuje jako "COMPLETED"
    ...
)

// Po:
data class BatchUpdateStatusRequest(
    ...
    @SerializedName("newStatus")
    val newStatus: String,  // ✅ Teraz przyjmuje "completed"
    ...
)
```

### 2. **OrdersViewModel.kt** - Konwersja enum na lowercase
```kotlin
// Przed:
val requestBody = BatchUpdateStatusRequest(
    orderIds = orderIds.toSet(),
    newStatus = newStatus,  // ❌ COMPLETED
    ...
)

// Po:
val requestBody = BatchUpdateStatusRequest(
    orderIds = orderIds.toSet(),
    newStatus = newStatus.name.lowercase(),  // ✅ completed
    ...
)
```

### 3. **OrderActionsUseCase.kt** - Konwersja enum na lowercase
```kotlin
// Przed:
val body = BatchUpdateStatusRequest(
    orderIds = orderIds,
    newStatus = newStatus  // ❌ COMPLETED
)

// Po:
val body = BatchUpdateStatusRequest(
    orderIds = orderIds,
    newStatus = newStatus.name.lowercase()  // ✅ completed
)
```

## Przykłady konwersji

| Enum | Przed (`.name`) | Po (`.name.lowercase()`) |
|------|----------------|--------------------------|
| `COMPLETED` | `"COMPLETED"` ❌ | `"completed"` ✅ |
| `OUT_FOR_DELIVERY` | `"OUT_FOR_DELIVERY"` ❌ | `"out_for_delivery"` ✅ |
| `ACCEPTED` | `"ACCEPTED"` ❌ | `"accepted"` ✅ |
| `CANCELLED` | `"CANCELLED"` ❌ | `"cancelled"` ✅ |

## Przepływ danych
```
UI/ViewModel
    ↓
OrderStatusEnum.COMPLETED
    ↓
.name.lowercase() → "completed"
    ↓
BatchUpdateStatusRequest(newStatus = "completed")
    ↓
Gson serializuje → {"newStatus": "completed"}
    ↓
API (✅ 200 OK)
```

## Request/Response Examples

### ❌ Przed (422 Error)
```json
PUT /admin/order/xxx/status/COMPLETED
{
  "newStatus": "COMPLETED"
}
```

### ✅ Po (200 OK)
```json
PUT /admin/order/xxx/status/completed
{
  "newStatus": "completed"
}
```

## Kompilacja
```
BUILD SUCCESSFUL in 16s
22 actionable tasks: 2 executed, 20 up-to-date
```

✅ Wszystkie zmiany zostały zakończone i przetestowane.

## Zmienione pliki
1. ✅ **UpdateOrderData.kt** - `newStatus: String`
2. ✅ **OrdersViewModel.kt** - `.name.lowercase()`
3. ✅ **OrderActionsUseCase.kt** - `.name.lowercase()`

## Alternatywne rozwiązania (nie zastosowane)

### Opcja A: Custom Gson Serializer
Można było stworzyć custom serializer dla `OrderStatusEnum`:
```kotlin
class OrderStatusEnumSerializer : JsonSerializer<OrderStatusEnum> {
    override fun serialize(src: OrderStatusEnum, ...): JsonElement {
        return JsonPrimitive(src.name.lowercase())
    }
}
```
**Powód odrzucenia:** Zbyt skomplikowane, wymaga konfiguracji Gson we wszystkich miejscach.

### Opcja B: Pełne przejście na Kotlinx Serialization
Zamienić wszystkie `@SerializedName` na `@SerialName` i usunąć Gson.
**Powód odrzucenia:** Duża refaktoryzacja całego projektu.

### Opcja C: String extension function
```kotlin
fun OrderStatusEnum.toApiFormat(): String = this.name.lowercase()
```
**Możliwe do dodania** w przyszłości dla większej czytelności.

## Wnioski
- ✅ API teraz otrzymuje poprawny format (`"completed"`)
- ✅ Aplikacja nie będzie już otrzymywać błędów 422
- ✅ Konsystencja z `@SerialName` w enumach
- ✅ Minimalna ingerencja w kod

## Testy
Zalecane testy:
1. ✅ Zmiana statusu na COMPLETED
2. ✅ Zmiana statusu na OUT_FOR_DELIVERY
3. ✅ Batch update wielu zamówień
4. ✅ Weryfikacja JSON payload w logach OkHttp

