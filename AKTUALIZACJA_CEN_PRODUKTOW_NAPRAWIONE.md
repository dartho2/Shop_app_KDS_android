# 🔄 NAPRAWIONE: Aktualizacja cen i produktów na bieżąco

## 🐛 Problem

**Symptom**: Gdy zamówienie zostało edytowane (zmieniono produkty lub ceny), aplikacja nie aktualizowała tych zmian na bieżąco. Pokazywała stare ceny i stare produkty.

### Przykład z logów:

```
18:29:46 - ORDER_CREATED: Hosomaki awokado, cena: 16.0 PLN
18:30:53 - ORDER_UPDATE/ORDER_ACCEPTED: (zaktualizowano zamówienie)
          → Aplikacja NIE aktualizuje produktów/cen!
          → Nadal pokazuje starą cenę 16.0 PLN
```

---

## 🔍 Diagnoza

### Co się działo:

1. **Nowe zamówienie** (`ORDER_CREATED`) - aplikacja zapisywała pełne zamówienie ✅
2. **Aktualizacja zamówienia** (`ORDER_ACCEPTED`, `ORDER_UPDATE`) - aplikacja aktualizowała **TYLKO status** ❌
3. Produkty, ceny, additionalFees, shipping - **NIE były aktualizowane** ❌

### Kod przed poprawką:

```kotlin
// SocketStaffEventsHandler.kt - handleStatusUpdate()
private fun handleStatusUpdate(args: Array<Any>, action: String) {
    // ...
    ioScope.launch {
        runCatching {
            val slugEnum = // parsowanie statusu
            
            // ❌ PROBLEM: Aktualizuje TYLKO status!
            ordersRepository.updateOrderStatusSlug(orderId, slugEnum)
            socketEventsRepo.emitStatus(orderId, slugEnum)
            
            // ❌ NIE pobiera pełnego zamówienia z API!
            // ❌ Ceny, produkty, additionalFees - NIE są aktualizowane!
        }
    }
}
```

### Dlaczego tak było zrobione?

- Dla większości eventów (np. `ORDER_COMPLETED`, `ORDER_CANCELLED`) wystarczy aktualizacja statusu
- Pobieranie pełnego zamówienia z API przy KAŻDYM evencie byłoby nieefektywne
- **ALE** dla `ORDER_UPDATE` i `ORDER_ACCEPTED` POTRZEBUJEMY pełnego zamówienia!

---

## ✅ Rozwiązanie

### Co zostało zmienione:

Dodano **inteligentne pobieranie pełnego zamówienia** tylko dla eventów, które mogą zmienić produkty/ceny:

```kotlin
// ✅ POPRAWKA: Dla ORDER_UPDATE i ORDER_ACCEPTED pobieramy pełne zamówienie z API
val shouldFetchFullOrder = action == SocketAction.Action.ORDER_UPDATE ||
        action == SocketAction.Action.ORDER_ACCEPTED

ioScope.launch {
    runCatching {
        if (shouldFetchFullOrder) {
            // ✅ Pobierz pełne zamówienie z API (ze zaktualizowanymi cenami i produktami)
            when (val res = ordersRepository.fetchOrderByIdFromApi(orderId)) {
                is Resource.Success -> {
                    val order = res.value
                    // ✅ Zapisz pełne zamówienie do bazy
                    ordersRepository.insertOrUpdateOrder(OrderMapper.fromOrder(order))
                    // ✅ Emituj pełne zamówienie do UI
                    socketEventsRepo.emitOrder(order)
                }
                is Resource.Failure -> {
                    // Fallback: aktualizuj tylko status
                    ordersRepository.updateOrderStatusSlug(orderId, slugEnum)
                    socketEventsRepo.emitStatus(orderId, slugEnum)
                }
            }
        } else {
            // Dla innych eventów - tylko status (jak wcześniej)
            ordersRepository.updateOrderStatusSlug(orderId, slugEnum)
            socketEventsRepo.emitStatus(orderId, slugEnum)
        }
    }
}
```

---

## 📊 Flow przed i po poprawce

### ❌ PRZED (stare zachowanie):

```
1. Socket: ORDER_CREATED
   └─ Aplikacja: Zapisuje pełne zamówienie (produkty, ceny) ✅

2. (Admin edytuje zamówienie - zmienia cenę z 16 zł na 20 zł)

3. Socket: ORDER_ACCEPTED
   └─ Aplikacja: Aktualizuje TYLKO status ❌
   └─ UI: Nadal pokazuje 16 zł ❌

4. Użytkownik: "Dlaczego cena się nie zmieniła?!" 🤔
```

### ✅ PO (nowe zachowanie):

```
1. Socket: ORDER_CREATED
   └─ Aplikacja: Zapisuje pełne zamówienie (produkty, ceny) ✅

2. (Admin edytuje zamówienie - zmienia cenę z 16 zł na 20 zł)

3. Socket: ORDER_ACCEPTED
   └─ Aplikacja: Wykrywa że to ORDER_ACCEPTED
   └─ Aplikacja: Pobiera pełne zamówienie z API ✅
   └─ Aplikacja: Zapisuje zaktualizowane zamówienie (nowa cena: 20 zł) ✅
   └─ UI: Pokazuje 20 zł ✅

4. Użytkownik: "Świetnie! Cena się zaktualizowała!" ✨
```

---

## 🎯 Które eventy pobierają pełne zamówienie?

| Event | Pobiera pełne zamówienie? | Powód |
|-------|---------------------------|-------|
| `ORDER_CREATED` | ✅ TAK | Już było w kodzie - działa poprawnie |
| `ORDER_PROCESSING` | ❌ NIE | Tylko zmiana statusu |
| **`ORDER_ACCEPTED`** | ✅ **TAK (NOWE!)** | Może zawierać zmienione produkty/ceny |
| **`ORDER_UPDATE`** | ✅ **TAK (NOWE!)** | Może zawierać zmienione produkty/ceny |
| `ORDER_COMPLETED` | ❌ NIE | Tylko zmiana statusu |
| `ORDER_CANCELLED` | ❌ NIE | Tylko zmiana statusu |
| `ORDER_OUT_FOR_DELIVERY` | ❌ NIE | Tylko zmiana statusu |
| `ORDER_SEND_TO_EXTERNAL_SUCCESS` | ✅ TAK | Już było w kodzie - działa poprawnie |

---

## 📝 Co zostanie zaktualizowane?

Gdy przychodzi `ORDER_UPDATE` lub `ORDER_ACCEPTED`, aplikacja pobiera z API i aktualizuje:

- ✅ **Produkty** (`products[]`)
- ✅ **Ceny produktów** (`price`, `salePrice`)
- ✅ **Ilości** (`quantity`)
- ✅ **Dodatki** (`addonsGroup[]`)
- ✅ **Total** (`total`, `amount`)
- ✅ **Opłaty dodatkowe** (`additionalFees[]`, `additionalFeeTotal`)
- ✅ **Koszty dostawy** (`shippingTotal`)
- ✅ **Podatki** (`taxTotal`)
- ✅ **Status** (`orderStatus`)
- ✅ **Czas dostawy** (`deliveryTime`, `deliveryInterval`)
- ✅ **Wszystkie inne pola zamówienia**

---

## 🧪 Jak przetestować?

### Test 1: Zmiana ceny produktu

1. Utwórz nowe zamówienie w aplikacji
2. Aplikacja pokazuje zamówienie z oryginalną ceną (np. 16 zł)
3. **W panelu admina**: Edytuj zamówienie → zmień cenę produktu (np. na 20 zł)
4. **W panelu admina**: Zapisz zmiany
5. **W aplikacji**: Zamówienie powinno się automatycznie zaktualizować do 20 zł ✅

### Test 2: Dodanie produktu

1. Utwórz nowe zamówienie z 1 produktem
2. **W panelu admina**: Edytuj zamówienie → dodaj drugi produkt
3. **W aplikacji**: Zamówienie powinno pokazywać 2 produkty ✅

### Test 3: Zmiana ilości

1. Utwórz zamówienie z produktem (ilość: 1)
2. **W panelu admina**: Edytuj zamówienie → zmień ilość na 3
3. **W aplikacji**: Zamówienie powinno pokazywać ilość: 3 ✅

---

## 🔧 Pliki zmodyfikowane:

1. **SocketStaffEventsHandler.kt** (linie 412-480)
   - Funkcja `handleStatusUpdate()` rozszerzona
   - Dodano pobieranie pełnego zamówienia dla `ORDER_UPDATE` i `ORDER_ACCEPTED`
   - Dodano logi diagnostyczne
   - Dodano fallback (jeśli API zwróci błąd, aktualizuj tylko status)

---

## 📊 Logi diagnostyczne

Po poprawce, w Logcat zobaczysz:

```
📊 STATUS UPDATE received
   ├─ orderId: 69d13c7bfdf8f3f49c4c5927
   ├─ action: ACTION_ORDER_UPDATE
   ├─ newStatus: ACCEPTED

🔄 Fetching full order from API for ACTION_ORDER_UPDATE (orderId=69d13c7bfdf8f3f49c4c5927)

✅ Fetched full order: orderId=69d13c7bfdf8f3f49c4c5927, total=20.00, products=2, status=ACCEPTED
```

Jeśli zobaczysz:
```
⚠️ Failed to fetch full order from API: ..., falling back to status update only
```
To znaczy że API zwróciło błąd, ale aplikacja zadziałała bezpiecznie (aktualizowała tylko status).

---

## ⚡ Wydajność

### Czy to nie obciąży API?

**NIE**, ponieważ:
1. Pełne zamówienie jest pobierane **TYLKO** dla `ORDER_UPDATE` i `ORDER_ACCEPTED`
2. Te eventy są rzadkie (1-2 razy na zamówienie)
3. Inne eventy (`ORDER_COMPLETED`, `ORDER_CANCELLED`) nadal aktualizują tylko status (szybko!)

### Porównanie:

| Event | Przed | Po | Różnica |
|-------|-------|-----|---------|
| `ORDER_COMPLETED` | 1 UPDATE SQL | 1 UPDATE SQL | Bez zmian |
| `ORDER_ACCEPTED` | 1 UPDATE SQL | 1 API call + 1 INSERT SQL | +1 API call |
| `ORDER_UPDATE` | 1 UPDATE SQL | 1 API call + 1 INSERT SQL | +1 API call |

**Wniosek**: Minimalne obciążenie, duży zysk (poprawne ceny i produkty!)

---

## ✅ Podsumowanie

| Aspekt | Przed | Po |
|--------|-------|-----|
| Aktualizacja cen | ❌ NIE | ✅ TAK |
| Aktualizacja produktów | ❌ NIE | ✅ TAK |
| Aktualizacja ilości | ❌ NIE | ✅ TAK |
| Aktualizacja dodatków | ❌ NIE | ✅ TAK |
| Fallback przy błędzie API | ❌ NIE | ✅ TAK |
| Logi diagnostyczne | ⚠️ Podstawowe | ✅ Szczegółowe |

---

**Data:** 2026-04-04  
**Wersja:** 1.0  
**Status:** ✅ GOTOWE DO TESTOWANIA

