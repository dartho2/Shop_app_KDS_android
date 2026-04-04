package com.itsorderkds.ui.language

import com.itsorderkds.data.model.LanguageOption
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
class LanguagesRepository @Inject constructor(
    private val api: ApiService,
    messageManager: GlobalMessageManager
) : BaseRepository(messageManager) {

    // ───── API CALLS ───────────────────────────────────────────────────────

    suspend fun getLanguages(): Resource<List<LanguageOption>> =
        safeApiCall { api.getLanguages() }
            .mapSuccess { it.data ?: emptyList() }
}