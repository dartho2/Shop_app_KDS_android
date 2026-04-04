package com.itsorderkds.data.network

import com.itsorderkds.data.model.AvailabilityNowDto
import com.itsorderkds.data.model.OpenHoursAdminDto
import com.itsorderkds.data.model.OpenHoursDto
import com.itsorderkds.data.model.PauseRequest
import com.itsorderkds.data.model.UpdateOpenHoursRequest
import retrofit2.Response
import retrofit2.http.*

interface OpenHoursApi {

    // PUBLIC
    @GET("client/v3/api/openhours/now")
    suspend fun getAvailabilityNow(
        // kind: "all" | "delivery" | "pickup"
        @Query("kind") kind: String? = null
    ): Response<AvailabilityNowDto>

    @GET("client/v3/api/admin/openhours")
    suspend fun getOpenHoursPublic(): Response<OpenHoursDto>

    // ADMIN
    @GET("client/v3/api/admin/openhours")
    suspend fun getOpenHoursAdmin(): Response<OpenHoursAdminDto>

    @POST("client/v3/api/admin/openhours")
    suspend fun createOpenHours(
        @Body body: UpdateOpenHoursRequest
    ): Response<OpenHoursAdminDto>

    @PUT("client/v3/api/admin/openhours")
    suspend fun updateOpenHours(
        @Body body: UpdateOpenHoursRequest
    ): Response<OpenHoursAdminDto>

    @POST("client/v3/api/admin/openhours/pause")
    suspend fun setPause(
        @Body body: PauseRequest
    ): Response<AvailabilityNowDto>

    @DELETE("client/v3/api/admin/openhours/pause")
    suspend fun clearPause(): Response<AvailabilityNowDto>
}
