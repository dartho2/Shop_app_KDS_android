package com.itsorderkds.data.repository

import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.network.SettingsApi
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

