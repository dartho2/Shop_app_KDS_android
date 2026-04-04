// ⚠️ DEPRECATED - PLIK PRZENIESIONY
// Nowa lokalizacja: com.itsorderkds.data.repository.SettingsRepository
// Ten plik powinien zostać usunięty po weryfikacji, że wszystko działa.
// Data przeniesienia: 2025-01-03

@file:Suppress("DEPRECATION", "unused")
package com.itsorderkds.ui.settings.deprecated_old_file

import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.network.SettingsApi
import com.itsorderkds.data.repository.BaseRepository
import com.itsorderkds.ui.settings.SettingsDto
import com.itsorderkds.ui.theme.GlobalMessageManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val api: SettingsApi,
    messageManager: GlobalMessageManager
) : BaseRepository(messageManager) {


    suspend fun getSettings(): Resource<SettingsDto> =
        safeApiCall { api.getSettings() }

    suspend fun getCourierSettings(): Resource<SettingsDto> =
        safeApiCall { api.getCourierSettings() }
}
