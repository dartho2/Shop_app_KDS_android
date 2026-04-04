package com.itsorderkds.ui.product.detail

/**
 * ViewModel do edycji szczegółów produktu
 *
 * NIEUŻYWANY - Edycja produktów została wyłączona.
 * Pozostawiony w kodzie dla ewentualnego przyszłego użycia.
 * Aktualnie dostępna tylko zmiana dostępności produktów przez Switch.
 */

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsorderkds.data.model.Availability
import com.itsorderkds.data.model.Category
import com.itsorderkds.data.model.LanguageOption
import com.itsorderkds.data.model.Product
import com.itsorderkds.data.model.toUpdateRequest
import com.itsorderkds.data.network.Resource
import com.itsorderkds.ui.category.CategoryRepository
import com.itsorderkds.ui.language.LanguagesRepository
import com.itsorderkds.data.repository.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val categoryOptions: List<Category> = emptyList(),
    val categoryIds: List<String> = emptyList(),
    val attributeIds: List<String> = emptyList(),
    val digitalFileIds: List<String> = emptyList(),
    val productType: String? = null, // jeśli chcesz sterować z dropdownu
    val statusEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val languages: List<LanguageOption> = emptyList(),
    val languageCode: String = "en",
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repository: ProductsRepository,
    private val categoryRepo: CategoryRepository,
    private val languagesRepo: LanguagesRepository,
    savedStateHandle: SavedStateHandle // Do odczytania ID produktu z nawigacji
) : ViewModel() {

    private val productId: String = checkNotNull(savedStateHandle["productId"])


    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 1) pobierz języki
            val langs = when (val r = languagesRepo.getLanguages()) {
                is Resource.Success -> r.value
                is Resource.Failure -> emptyList()
                else -> emptyList()
            }
            // wybierz bieżący albo fallback (pierwszy / "pl")
            val defaultCode = langs.firstOrNull()?.locale ?: "en"

            _uiState.update { it.copy(languages = langs, languageCode = defaultCode) }

            // 2) teraz pobierz dane zależne od języka
            refreshForLanguage()
        }
    }

    private fun fetchLanguages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = languagesRepo.getLanguages()) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, languages = result.value)
                }

                is Resource.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.errorMessage)
                }

                else -> {}
            }
        }
    }

    private fun refreshForLanguage() {
        val lang = _uiState.value.languageCode
        fetchProductDetails()
        fetchCategories(lang)
    }

    fun onLanguageSelected(code: String) {
        _uiState.update { it.copy(languageCode = code) }
        refreshForLanguage()
    }

    private fun fetchCategories(lang: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = categoryRepo.getCategories(lang)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, categoryOptions = result.value)
                }

                is Resource.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.errorMessage)
                }

                else -> {}
            }
        }
    }

    private fun fetchProductDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.getProductDetails(productId)) {
                is Resource.Success -> {
                    val p = result.value
                    val preselected = p.categories.mapNotNull { it.id } // filtruj null wartości
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            product = p.copy(availability = p.availability ?: Availability()),
                            categoryIds = preselected,
                            statusEnabled = p.status
                        )
                    }
                }

                is Resource.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.errorMessage)
                }

                else -> {}
            }
        }
    }

    fun onProductChange(updatedProduct: Product) {
        _uiState.update { it.copy(product = updatedProduct) }
    }

    fun setCategoryIds(ids: List<String>) =
        _uiState.update { it.copy(categoryIds = ids.distinct()) }

    fun addCategoryId(id: String) {
        _uiState.update { it.copy(categoryIds = (it.categoryIds + id).distinct()) }
    }

    fun removeCategoryId(id: String) {
        _uiState.update { it.copy(categoryIds = it.categoryIds - id) }
    }

    fun addAttributeId(id: String) {
        _uiState.update { it.copy(attributeIds = (it.attributeIds + id).distinct()) }
    }

    fun removeAttributeId(id: String) {
        _uiState.update { it.copy(attributeIds = it.attributeIds - id) }
    }

    fun addDigitalFileId(id: String) {
        _uiState.update { it.copy(digitalFileIds = (it.digitalFileIds + id).distinct()) }
    }

    fun removeDigitalFileId(id: String) {
        _uiState.update { it.copy(digitalFileIds = it.digitalFileIds - id) }
    }

    fun saveProduct() {
        val current = _uiState.value.product ?: return
        val ui = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveSuccess = false, error = null) }

            val req = try {
                current.toUpdateRequest(ui)
            } catch (e: Exception) {
                Log.e("UpdateProduct", "toUpdateRequest() failed", e)
                _uiState.update { s ->
                    s.copy(
                        isSaving = false,
                        error = e.message ?: "Invalid data"
                    )
                }
                return@launch
            }

            val lang = ui.languageCode // <<— tu jest wybrany język z paska
            when (val result = repository.updateProduct(productId, req)) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                }

                is Resource.Failure -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = result.errorMessage
                    )
                }

                else -> _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
