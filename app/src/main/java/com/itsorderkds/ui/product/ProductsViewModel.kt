package com.itsorderkds.ui.product

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.itsorderkds.data.model.Product
import com.itsorderkds.data.model.StockStatusEnum
import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.repository.ProductsRepository
import com.itsorderkds.data.util.BaseListViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val repo: ProductsRepository
) : BaseListViewModel<Product>() {

    private val _searchQuery = MutableStateFlow("")
    private val _updatingProductIds = MutableStateFlow<Set<String>>(emptySet())
    private val _showOnlyOutOfStock = MutableStateFlow(false)
    private val _updatingAddonIds = MutableStateFlow<Set<String>>(emptySet())

    // Nowy Flow dla kategorii z API
    private val _categoriesFlow = MutableStateFlow<List<com.itsorderkds.data.model.CategoryProduct>>(emptyList())
    private val _categoriesLoadingFlow = MutableStateFlow(false)
    private val _categoriesErrorFlow = MutableStateFlow<Resource.Failure?>(null)

    // --- Stan Lokalny ---
    private val _localState = combine(
        _searchQuery,
        _updatingProductIds,
        _updatingAddonIds,
        _showOnlyOutOfStock
    ) { query, updatingIds, updatingAddons, outOfStock ->
        LocalUiState(query, updatingIds, updatingAddons, outOfStock)
    }

    // Zgrupuj loading states
    private val _loadingState = combine(
        isLoadingFlow,
        _categoriesLoadingFlow
    ) { productsLoading, categoriesLoading ->
        productsLoading || categoriesLoading
    }

    // Zgrupuj error states
    private val _errorState = combine(
        errorFlow,
        _categoriesErrorFlow
    ) { productsError, categoriesError ->
        productsError ?: categoriesError
    }

    // --- Stan Publiczny dla UI ---
    val uiState: StateFlow<ProductsUiState> = combine(
        itemsFlow,
        _categoriesFlow,
        _loadingState,
        _errorState,
        _localState
    ) { products, categories, isLoading, error, localState ->

        Timber.d("🔄 uiState combine triggered - categories count: ${categories.size}")

        val filteredProducts = if (localState.searchQuery.isBlank()) {
            products
        } else {
            products.filter {
                it.name.contains(localState.searchQuery, ignoreCase = true)
            }
        }

        // Użyj kategorii z API (już pogrupowane)
        val categoriesWithProducts = categories
        Timber.d("📦 categoriesWithProducts assigned, count: ${categoriesWithProducts.size}")

        ProductsUiState(
            isLoading = isLoading,
            products = filteredProducts,
            categoriesWithProducts = categoriesWithProducts,
            searchQuery = localState.searchQuery,
            error = error,
            updatingProductIds = localState.updatingProductIds,
            showOnlyOutOfStock = localState.showOnlyOutOfStock,
            updatingAddonIds = localState.updatingAddonIds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProductsUiState(isLoading = true)
    )


    init {
        observeSearchAndFilter()
        loadCategories() // Ładuj kategorie z API przy starcie (tylko jeśli brak cache)
    }

    fun loadCategories(forceRefresh: Boolean = false) {
        // Cache check - nie ładuj ponownie jeśli mamy dane (chyba że force refresh)
        if (!forceRefresh && _categoriesFlow.value.isNotEmpty()) {
            Timber.d("🚀 Categories already loaded, skipping API call")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _categoriesLoadingFlow.value = true
            Timber.d("🌐 Loading categories from API...")
            repo.getProductsByCategories().fold(
                onSuccess = { categories ->
                    Timber.d("✅ Categories loaded: ${categories.size} categories")
                    _categoriesFlow.value = categories
                    _categoriesErrorFlow.value = null
                },
                onFailure = { error ->
                    Timber.e("❌ Error loading categories: $error")
                    _categoriesFlow.value = emptyList()
                    _categoriesErrorFlow.value = error
                }
            )
            _categoriesLoadingFlow.value = false
        }
    }

    // ✅ ZAKTUALIZOWANA FUNKCJA DLA ADDONÓW (STOCK_STATUS)
    fun updateAddonStatus(addonId: String, newStatus: StockStatusEnum) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _updatingAddonIds.update { it + addonId }

                // 1. Strzał do API (repozytorium musi przyjmować StockStatusEnum)
                repo.updateAddonStatus(addonId, newStatus)

                // 2. Lokalna aktualizacja płaskiej listy (dla ProductsScreen)
                itemsFlow.update { currentProducts ->
                    currentProducts.map { product ->
                        val hasThisAddon = product.addonsGroup.any { group ->
                            group.addons.any { it.id == addonId }
                        }

                        if (hasThisAddon) {
                            product.copy(
                                addonsGroup = product.addonsGroup.map { group ->
                                    group.copy(
                                        addons = group.addons.map { addon ->
                                            if (addon.id == addonId) {
                                                addon.copy(stockStatus = newStatus)
                                            } else {
                                                addon
                                            }
                                        }
                                    )
                                }
                            )
                        } else {
                            product
                        }
                    }
                }

                // WAŻNE: Aktualizuj również kategorie (dla CategoryProductsScreen)
                val currentCategories = _categoriesFlow.value
                val updatedCategories = currentCategories.map { category ->
                    // Aktualizuj produkty z dodatkami w kategorii
                    val updatedProducts = category.products?.map { product ->
                        val hasThisAddon = product.addonsGroup.any { group ->
                            group.addons.any { it.id == addonId }
                        }

                        if (hasThisAddon) {
                            product.copy(
                                addonsGroup = product.addonsGroup.map { group ->
                                    group.copy(
                                        addons = group.addons.map { addon ->
                                            if (addon.id == addonId) {
                                                addon.copy(stockStatus = newStatus)
                                            } else {
                                                addon
                                            }
                                        }
                                    )
                                }
                            )
                        } else {
                            product
                        }
                    }

                    // Zwróć kategorię z zaktualizowanymi produktami (tylko jeśli produkty się zmieniły)
                    if (updatedProducts != category.products) {
                        category.copy(products = updatedProducts)
                    } else {
                        category
                    }
                }
                _categoriesFlow.value = updatedCategories  // Bezpośrednie przypisanie!
            } catch (e: Exception) {
                Log.e("ProductsViewModel", "Błąd aktualizacji dodatku: $e")
            } finally {
                _updatingAddonIds.update { it - addonId }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchAndFilter() {
        combine(
            _searchQuery,
            _showOnlyOutOfStock
        ) { query, onlyOutOfStock ->
            Pair(query, onlyOutOfStock)
        }
            .debounce(500L)
            .distinctUntilChanged()
            .onEach { (query, onlyOutOfStock) ->
                val statusParam = if (onlyOutOfStock) false else null

                if (query.isBlank()) {
                    loadProducts(search = null, status = statusParam)
                } else if (query.length >= 3) {
                    loadProducts(search = query, status = statusParam)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterToggle(showOnlyOutOfStock: Boolean) {
        _showOnlyOutOfStock.value = showOnlyOutOfStock
    }

    fun updateStockStatus(productId: String, newStatus: StockStatusEnum) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Timber.d("🔄 updateStockStatus: productId=$productId, newStatus=$newStatus")
                _updatingProductIds.update { it + productId }

                Timber.d("📡 Sending API request...")
                repo.updateStockStatus(productId, newStatus)
                Timber.d("✅ API request successful")

                // Aktualizuj płaską listę produktów (dla ProductsScreen)
                Timber.d("🔄 Updating itemsFlow...")
                itemsFlow.update { currentProducts ->
                    currentProducts.map { product ->
                        if (product.id == productId) product.copy(stockStatus = newStatus) else product
                    }
                }
                Timber.d("✅ itemsFlow updated")

                // WAŻNE: Aktualizuj również kategorie (dla CategoryProductsScreen)
                Timber.d("🔄 Updating _categoriesFlow...")
                val currentCategories = _categoriesFlow.value
                Timber.d("📦 Current categories count: ${currentCategories.size}")

                // Aktualizuj tylko produkty w kategoriach, kategorii NIE zmieniamy
                val updatedCategories = currentCategories.map { category ->
                    // Aktualizuj produkty w kategorii
                    val updatedProducts = category.products?.map { product ->
                        if (product.id == productId) {
                            Timber.d("✅ Found product ${product.name} in category ${category.name}, updating status to $newStatus")
                            product.copy(stockStatus = newStatus)
                        } else {
                            product
                        }
                    }

                    // Zwróć kategorię z zaktualizowanymi produktami (tylko jeśli produkty się zmieniły)
                    if (updatedProducts != category.products) {
                        category.copy(products = updatedProducts)
                    } else {
                        category
                    }
                }

                Timber.d("✅ Updated categories count: ${updatedCategories.size}")
                _categoriesFlow.value = updatedCategories  // Bezpośrednie przypisanie!
                Timber.d("✅ _categoriesFlow.value assigned")
            } catch (e: Exception) {
                Timber.e(e, "❌ Error updating stock status: ${e.message}")
            } finally {
                _updatingProductIds.update { it - productId }
                Timber.d("✅ Removed from updatingProductIds")
            }
        }
    }

    fun loadProducts(limit: Int? = 200, search: String? = null, status: Boolean? = null) {
        request(viewModelScope) {
            repo.getProducts(limit, search = search, status = status)
        }
    }
}

private data class LocalUiState(
    val searchQuery: String,
    val updatingProductIds: Set<String>,
    val updatingAddonIds: Set<String>,
    val showOnlyOutOfStock: Boolean
)

data class ProductsUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val categoriesWithProducts: List<com.itsorderkds.data.model.CategoryProduct> = emptyList(),
    val searchQuery: String = "",
    val error: Resource.Failure? = null,
    val updatingProductIds: Set<String> = emptySet(),
    val showOnlyOutOfStock: Boolean = false,
    val updatingAddonIds: Set<String> = emptySet()
)
