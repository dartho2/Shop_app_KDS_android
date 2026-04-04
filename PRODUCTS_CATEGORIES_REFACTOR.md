# Refaktoryzacja: Dwupoziomowa nawigacja produktów (Kategorie → Produkty)

## 📋 Podsumowanie zmian

### Cel refaktoryzacji
Zmiana wyświetlania produktów z jednego płaskiego widoku na dwupoziomową nawigację:
- **Poziom 1:** Lista kategorii
- **Poziom 2:** Produkty w wybranej kategorii + dodatki (z możliwością włączania/wyłączania)

### ⚡ Optymalizacja
**Wykorzystano istniejący endpoint API** `GET /client/v3/api/admin/categories/product` zamiast manualnego grupowania produktów.
- Dane są już pogrupowane po kategoriach przez backend
- Mniej przetwarzania po stronie klienta
- Szybsze ładowanie i lepsze performance

---

## 🆕 Nowe pliki

### 1. `CategoriesListScreen.kt`
**Lokalizacja:** `ui/product/CategoriesListScreen.kt`

**Opis:** Główny ekran listy kategorii produktów.

**Funkcjonalność:**
- Wyświetla wszystkie kategorie produktów jako karty
- Każda karta pokazuje:
  - Ikonę kategorii (lub inicjał nazwy jako placeholder)
  - Nazwę kategorii
  - Liczbę produktów w kategorii
  - Chevron wskazujący możliwość kliknięcia
- Pull-to-refresh
- Obsługa stanów: loading, error, pusta lista
- Kliknięcie kategorii → nawigacja do `CategoryProductsScreen`

**Kluczowe komponenty:**
```kotlin
@Composable
fun CategoriesListScreen(
    viewModel: ProductsViewModel,
    onCategoryClick: (categoryId: String, categoryName: String) -> Unit
)

@Composable
private fun CategoryListItem(
    category: CategoryProduct,
    onClick: () -> Unit
)
```

---

### 2. `CategoryProductsScreen.kt`
**Lokalizacja:** `ui/product/CategoryProductsScreen.kt`

**Opis:** Ekran produktów dla konkretnej kategorii.

**Funkcjonalność:**
- Wyświetla produkty wybranej kategorii
- Każdy produkt zawiera:
  - Zdjęcie produktu
  - Nazwę i cenę
  - Switch do włączania/wyłączania (StockStatus)
  - Sekcję dodatków (jeśli istnieją)
- Dodatki zawsze widoczne (nie ma collapse/expand)
- Każdy dodatek ma własny switch
- Pull-to-refresh
- Obsługa stanów: loading, error, pusta lista
- Kliknięcie produktu → nawigacja do `ProductDetailScreen`

**Kluczowe komponenty:**
```kotlin
@Composable
fun CategoryProductsScreen(
    categoryId: String,
    categoryName: String,
    viewModel: ProductsViewModel,
    onProductClick: (String) -> Unit
)

@Composable
private fun ProductListItem(
    product: Product,
    isUpdating: Boolean,
    updatingAddonIds: Set<String>,
    onClick: () -> Unit,
    onStockStatusChange: (StockStatusEnum) -> Unit,
    onAddonStatusChange: (String, StockStatusEnum) -> Unit
)
```

---

## 🌐 Zmiany API

### 1. `ProductApi.kt` (DODANE)
**Endpoint:** `GET /client/v3/api/admin/categories/product`
```kotlin
@GET("client/v3/api/admin/categories/product")
suspend fun getProductsByCategories(): Response<ProductsCategoriesResponse>
```

**Response model:** `ProductsCategoriesResponse`
```kotlin
data class ProductsCategoriesResponse(
    val data: List<CategoryProduct>
)
```

**Zalety tego podejścia:**
- ✅ Backend już grupuje produkty po kategoriach
- ✅ Pojedyncze zapytanie API zamiast wielu
- ✅ Szybsze ładowanie danych
- ✅ Mniej logiki po stronie klienta

---

### 2. `ProductsRepository.kt` (DODANE)
```kotlin
suspend fun getProductsByCategories(): Resource<List<CategoryProduct>> =
    safeApiCall { api.getProductsByCategories() }
        .mapSuccess { it.data ?: emptyList() }
```

---

## 🔄 Zaktualizowane pliki

### 1. `Product.kt` (Model)
**Zmiany:**
- Dodano pole `categoryIcon` do `CategoryAdminDto`:
  ```kotlin
  data class CategoryAdminDto(
      val id: String,
      val name: String,
      val slug: String,
      val status: Boolean,
      val description: String,
      val products: List<Product>? = null,
      @SerializedName("category_icon") val categoryIcon: AttachmentAdminDto? = null
  )
  ```
- Dodano alias typu:
  ```kotlin
  typealias CategoryProduct = CategoryAdminDto
  ```

---

### 2. `ProductsViewModel.kt`
**Zmiany:**

#### a) Dodano nowe Flows dla kategorii z API:
```kotlin
// Nowy Flow dla kategorii z API
private val _categoriesFlow = MutableStateFlow<List<CategoryProduct>>(emptyList())
private val _categoriesLoadingFlow = MutableStateFlow(false)
private val _categoriesErrorFlow = MutableStateFlow<Resource.Failure?>(null)
```

#### b) Dodano funkcję `loadCategories()`:
```kotlin
fun loadCategories() {
    viewModelScope.launch(Dispatchers.IO) {
        _categoriesLoadingFlow.value = true
        repo.getProductsByCategories().fold(
            onSuccess = { categories ->
                _categoriesFlow.value = categories
                _categoriesErrorFlow.value = null
            },
            onFailure = { error ->
                _categoriesFlow.value = emptyList()
                _categoriesErrorFlow.value = error
            }
        )
        _categoriesLoadingFlow.value = false
    }
}
```

#### c) Zaktualizowano `uiState` - używa danych z API zamiast manualnego grupowania:
```kotlin
val uiState: StateFlow<ProductsUiState> = combine(
    itemsFlow,
    _categoriesFlow,  // ← NOWE: dane z API
    isLoadingFlow,
    _categoriesLoadingFlow,
    errorFlow,
    _categoriesErrorFlow,
    _localState
) { products, categories, isLoading, categoriesLoading, error, categoriesError, localState ->
    
    // Użyj kategorii z API (już pogrupowane przez backend)
    val categoriesWithProducts = categories
    
    ProductsUiState(
        isLoading = isLoading || categoriesLoading,
        products = filteredProducts,
        categoriesWithProducts = categoriesWithProducts,  // ← Bezpośrednio z API
        // ...
    )
}
```

**Przed zmianą:** Manualne grupowanie produktów w ViewModelu
```kotlin
// ❌ STARE: Manualne grupowanie
filteredProducts.forEach { product ->
    product.categories.forEach { category ->
        categoriesMap.getOrPut(categoryId) { mutableListOf() }.add(product)
        // ...
    }
}
```

**Po zmianie:** Użycie danych z API
```kotlin
// ✅ NOWE: Bezpośrednio z API
val categoriesWithProducts = categories  // Już pogrupowane przez backend
```

---

### 3. `HomeActivity.kt` (Nawigacja)

#### a) `AppDestinations` - dodano nowe destynacje:
```kotlin
object AppDestinations {
    const val CATEGORIES_LIST = "categories_list"        // ← NOWE
    const val CATEGORY_PRODUCTS = "category_products"    // ← NOWE
    
    const val CATEGORY_PRODUCTS_TEMPLATE = 
        "$CATEGORY_PRODUCTS/{categoryId}/{categoryName}"  // ← NOWE
    
    // ...pozostałe destynacje...
}
```

#### b) `AppNavHost` - dodano nowy routing:
```kotlin
// Lista kategorii - nowy główny ekran produktów
composable(AppDestinations.CATEGORIES_LIST) {
    CategoriesListScreen(
        onCategoryClick = { categoryId, categoryName ->
            navController.navigate(
                "${AppDestinations.CATEGORY_PRODUCTS}/$categoryId/$categoryName"
            )
        }
    )
}

// Produkty konkretnej kategorii
composable(
    route = AppDestinations.CATEGORY_PRODUCTS_TEMPLATE,
    arguments = listOf(
        navArgument("categoryId") { type = NavType.StringType },
        navArgument("categoryName") { type = NavType.StringType }
    )
) { backStackEntry ->
    val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
    val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
    CategoryProductsScreen(
        categoryId = categoryId,
        categoryName = categoryName,
        onProductClick = { productId ->
            navController.navigate("${AppDestinations.PRODUCT_DETAIL_PREFIX}/$productId")
        }
    )
}

// Stary ekran produktów (zachowany dla kompatybilności z filtrem "disabled")
composable(
    route = AppDestinations.PRODUCTS_LIST_TEMPLATE,
    arguments = listOf(navArgument(AppDestinations.PRODUCTS_FILTER_ARG) {
        type = NavType.StringType
        nullable = true
        defaultValue = null
    })
) { backStackEntry ->
    val filter = backStackEntry.arguments?.getString(AppDestinations.PRODUCTS_FILTER_ARG)
    ProductsScreen(
        filterArg = filter,
        onProductClick = { productId ->
            navController.navigate("${AppDestinations.PRODUCT_DETAIL_PREFIX}/$productId")
        }
    )
}
```

#### c) Dodano importy:
```kotlin
import com.itsorderchat.ui.product.CategoriesListScreen
import com.itsorderchat.ui.product.CategoryProductsScreen
```

---

### 4. `strings.xml` (Angielski)
Dodano nowe stringi:
```xml
<!-- Categories -->
<string name="no_categories">No categories</string>
<string name="no_categories_subtitle">No categories available</string>
<string name="products_count">%1$d products</string>
<string name="open_category">Open category</string>

<!-- Products -->
<string name="no_products">No products</string>
<string name="no_products_in_category">No products in %1$s</string>

<!-- Error messages -->
<string name="error_occurred">Error occurred</string>
<string name="error_fetch_categories">Could not fetch categories.</string>
<string name="error_fetch_products">Could not fetch products.</string>
<string name="try_again">Try again</string>
```

---

### 5. `strings-pl.xml` (Polski)
Dodano polskie tłumaczenia:
```xml
<!-- Categories -->
<string name="no_categories">Brak kategorii</string>
<string name="no_categories_subtitle">Brak dostępnych kategorii</string>
<string name="products_count">%1$d produktów</string>
<string name="open_category">Otwórz kategorię</string>

<!-- Products -->
<string name="no_products">Brak produktów</string>
<string name="no_products_in_category">Brak produktów w %1$s</string>

<!-- Error messages -->
<string name="error_occurred">Wystąpił błąd</string>
<string name="error_fetch_categories">Nie udało się pobrać kategorii.</string>
<string name="error_fetch_products">Nie udało się pobrać produktów.</string>
<string name="try_again">Spróbuj ponownie</string>
```

---

## 🎯 Przepływ użytkownika

### Scenariusz 1: Przeglądanie produktów przez kategorie

1. **Użytkownik otwiera menu** → wybiera "Produkty"
2. **Wyświetla się `CategoriesListScreen`**
   - Lista wszystkich kategorii (Pizza, Napoje, Desery, itp.)
   - Każda kategoria pokazuje liczbę produktów
3. **Użytkownik klika kategorię** np. "Pizza"
4. **Wyświetla się `CategoryProductsScreen`**
   - Lista produktów w kategorii "Pizza"
   - Każdy produkt ma switch (włącz/wyłącz)
   - Dodatki zawsze widoczne pod produktem
5. **Użytkownik włącza/wyłącza produkty**
   - Kliknięcie switch → update API → optymistyczne UI update
6. **Użytkownik włącza/wyłącza dodatki**
   - Kliknięcie switch dodatku → update API → optymistyczne UI update
7. **Użytkownik klika produkt** → nawigacja do `ProductDetailScreen`

### Scenariusz 2: Wyłączone produkty (zachowana kompatybilność)

1. **Użytkownik otwiera RestaurantStatusSheet**
2. **Klika "Disabled products"**
3. **Wyświetla się stary `ProductsScreen`** z filtrem `filter=disabled`
4. Pokazuje TYLKO wyłączone produkty (flat lista, bez podziału na kategorie)

---

## ✅ Korzyści z refaktoryzacji

### 1. **Lepsze UX**
- Przejrzysta struktura: kategorie → produkty
- Mniej scrollowania
- Łatwiejsze znalezienie produktu

### 2. **Wydajność**
- Mniejsze listy (produkty grupowane po kategorii)
- Lazy loading per kategoria
- Optymalne renderowanie

### 3. **Skalowalność**
- Łatwo dodać nowe kategorie
- Można dodać filtry per kategoria
- Możliwość dodania breadcrumbs

### 4. **Elastyczność**
- Zachowano stary ekran dla "disabled products"
- Można łatwo przywrócić stary widok jeśli trzeba
- Osobne ViewModels dla kategorii i produktów (w przyszłości)

---

## 🔧 Dalsze możliwe usprawnienia

### 1. **Search w kategoriach**
Dodać wyszukiwarkę na poziomie kategorii (filtrowanie nazw kategorii)

### 2. **Sortowanie kategorii**
Umożliwić sortowanie kategorii (alfabetycznie, po popularności, custom order)

### 3. **Ikony kategorii z backend**
Backend powinien zwracać `category_icon` URL dla każdej kategorii

### 4. **Batch toggle**
Przycisk "Włącz wszystkie" / "Wyłącz wszystkie" produkty w kategorii

### 5. **Statystyki**
Pokazać w karcie kategorii: "5/10 produktów aktywnych"

### 6. **Breadcrumbs**
Dodać breadcrumbs: "Produkty > Pizza > Margherita"

---

## 🐛 Znane ograniczenia

1. **Brak ikony kategorii z backendu**
   - Obecnie używany placeholder (pierwsza litera nazwy)
   - Wymaga dodania pola `category_icon` w API response

2. **Zachowano stary ekran ProductsScreen**
   - Używany dla filtra "disabled"
   - Duplikacja kodu UI produktów
   - **Rekomendacja:** Rozważyć unifikację w przyszłości

3. **Kategorie mogą być puste**
   - Jeśli kategoria nie ma produktów, pokazuje się komunikat
   - Backend powinien filtrować puste kategorie

---

## 📚 Pliki do przeglądu

**Nowe:**
- `ui/product/CategoriesListScreen.kt`
- `ui/product/CategoryProductsScreen.kt`

**Zmodyfikowane:**
- `ui/product/ProductsViewModel.kt`
- `ui/theme/home/HomeActivity.kt`
- `data/model/Product.kt`
- `res/values/strings.xml`
- `res/values-pl/strings.xml`

**Zachowane (bez zmian):**
- `ui/product/ProductsScreen.kt` (dla kompatybilności)
- `ui/product/ProductDetailScreen.kt`

---

## ✅ Status: GOTOWE DO TESTOWANIA

Wszystkie zmiany zostały zaimplementowane i skompilowane bez błędów.
Tylko ostrzeżenia (unused imports, itp.) - do posprzątania później.

**Następny krok:** Testowanie UI i przepływu nawigacji.

