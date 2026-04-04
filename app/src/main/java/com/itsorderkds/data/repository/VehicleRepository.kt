package com.itsorderkds.data.repository

import com.itsorderkds.data.model.Vehicle
import com.itsorderkds.data.network.Resource
import com.itsorderkds.ui.theme.GlobalMessageManager
import com.itsorderkds.data.network.VehicleApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @deprecated Zmieniono nazwę na VehiclesRepository dla konsystencji.
 * Użyj VehiclesRepository zamiast tej klasy.
 */
@Deprecated(
    message = "Use VehiclesRepository instead for naming consistency",
    replaceWith = ReplaceWith("VehiclesRepository", "com.itsorderkds.data.repository.VehiclesRepository")
)
@Singleton
class VehicleRepository @Inject constructor(
    private val api: VehicleApi,
    messageManager: GlobalMessageManager
) : BaseRepository(messageManager) {

    suspend fun getVehicles(): Resource<List<Vehicle>> =
        safeApiCall { api.getVehicles() }
            .mapSuccess { it.data ?: emptyList() }
}

