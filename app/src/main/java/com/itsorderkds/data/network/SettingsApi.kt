package com.itsorderkds.data.network

import com.itsorderkds.ui.settings.SettingsDto
import retrofit2.Response
import retrofit2.http.GET

interface SettingsApi {
    @GET("client/v3/api/admin/settings")
    suspend fun getSettings(): Response<SettingsDto>

    @GET("client/v3/api/courier/settings")
    suspend fun getCourierSettings(): Response<SettingsDto>
}
