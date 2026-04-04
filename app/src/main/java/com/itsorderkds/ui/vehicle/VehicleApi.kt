// ⚠️ DEPRECATED - PLIK PRZENIESIONY
// Nowa lokalizacja: com.itsorderkds.data.network.VehicleApi
// Ten plik powinien zostać usunięty po weryfikacji, że wszystko działa.
// Data przeniesienia: 2025-01-03

@file:Suppress("DEPRECATION", "unused")
package com.itsorderkds.ui.vehicle.deprecated_old_file

import com.itsorderkds.data.model.VehicleResponse
import retrofit2.Response
import retrofit2.http.GET

interface VehicleApi {

    @GET("client/v3/api/admin/vehicle")
    suspend fun getVehicles(): Response<VehicleResponse>

}
