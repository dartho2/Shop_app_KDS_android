// ⚠️ DEPRECATED - PLIK PRZENIESIONY
// Nowa lokalizacja: com.itsorderkds.data.repository.VehiclesRepository (zmieniono nazwę!)
// Ten plik powinien zostać usunięty po weryfikacji, że wszystko działa.
// Data przeniesienia: 2025-01-03
// Data zmiany nazwy: 2025-01-03

@file:Suppress("DEPRECATION", "unused")
package com.itsorderkds.ui.vehicle.deprecated_old_file

import com.itsorderkds.data.model.Vehicle
import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.network.VehicleApi
import com.itsorderkds.data.repository.BaseRepository
import com.itsorderkds.ui.theme.GlobalMessageManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Repozytoria zazwyczaj powinny być Singletonami
class VehicleRepository @Inject constructor(
    private val api: VehicleApi,
    messageManager: GlobalMessageManager
) : BaseRepository(messageManager) {


    suspend fun getVehicles(): Resource<List<Vehicle>> =
        safeApiCall { api.getVehicles() }     // Retrofit → Response<ApiDto>
            .mapSuccess { it.data ?: emptyList() }         // ApiDto → List<Product>


}
