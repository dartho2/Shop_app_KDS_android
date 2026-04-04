package com.itsorderkds.ui.category

import com.itsorderkds.data.model.Category
import com.itsorderkds.data.network.ApiService
import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.repository.BaseRepository
import com.itsorderkds.ui.theme.GlobalMessageManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repozytorium do zarządzania produktami.
 * Zależność 'ProductsApi' jest teraz wstrzykiwana przez Hilt, a nie tworzona ręcznie.
 * Adnotacja @Singleton zapewnia, że w całej aplikacji będzie tylko jedna instancja tego repozytorium.
 */
@Singleton // Repozytoria zazwyczaj powinny być Singletonami
class CategoryRepository @Inject constructor(
    private val api: ApiService,
    messageManager: GlobalMessageManager
) : BaseRepository(messageManager) {

    // ───── API CALLS ───────────────────────────────────────────────────────

    suspend fun getCategories(lang: String?): Resource<List<Category>> =
        safeApiCall { api.getCategories(lang) }     // Retrofit → Response<ApiDto>
            .mapSuccess { it.data ?: emptyList() }         // ApiDto → List<Product>
}