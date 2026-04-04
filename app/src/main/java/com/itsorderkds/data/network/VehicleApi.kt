package com.itsorderkds.data.network

import com.itsorderkds.data.model.VehicleResponse
import retrofit2.Response
import retrofit2.http.GET

interface VehicleApi {

    @GET("client/v3/api/admin/vehicle")
    suspend fun getVehicles(): Response<VehicleResponse>

}

