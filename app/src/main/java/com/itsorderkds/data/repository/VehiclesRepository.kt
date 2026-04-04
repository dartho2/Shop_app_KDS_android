package com.itsorderkds.data.repository

import com.itsorderkds.data.model.Vehicle
import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.network.VehicleApi
import com.itsorderkds.ui.theme.GlobalMessageManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository do zarządzania pojazdami.
 *
 * Nazwa zmieniona z VehicleRepository na VehiclesRepository dla konsystencji
 * z OrdersRepository i ProductsRepository.
 */
@Singleton
class VehiclesRepository @Inject constructor(
    private val api: VehicleApi,
    messageManager: GlobalMessageManager
) : BaseRepository(messageManager) {

    suspend fun getVehicles(): Resource<List<Vehicle>> =
        safeApiCall { api.getVehicles() }
            .mapSuccess { it.data ?: emptyList() }
}

