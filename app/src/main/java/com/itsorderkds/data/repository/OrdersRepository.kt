package com.itsorderkds.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.google.gson.Gson
import com.itsorderkds.data.dao.OrderDao
import com.itsorderkds.data.database.AppDatabase
import com.itsorderkds.data.entity.CourierEntity
import com.itsorderkds.data.entity.OrderEntity
import com.itsorderkds.data.model.BatchUpdateStatusRequest
import com.itsorderkds.data.model.Courier
import com.itsorderkds.data.model.DispatchCourier
import com.itsorderkds.data.model.DispatchCancelCourier
import com.itsorderkds.data.model.ExternalDeliveryOutboxPayload
import com.itsorderkds.data.model.Order
import com.itsorderkds.data.model.OrderTras
import com.itsorderkds.data.model.PaginatedOrders
import com.itsorderkds.data.model.ShiftCheckIn
import com.itsorderkds.data.model.ShiftCheckOut
import com.itsorderkds.data.model.ShiftResponse
import com.itsorderkds.data.model.UpdateCourierOrder
import com.itsorderkds.data.model.UpdateOrderData
import com.itsorderkds.data.network.OrderApi
import com.itsorderkds.data.network.Resource
import com.itsorderkds.ui.order.OrderStatusEnum
import com.itsorderkds.ui.theme.GlobalMessageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrdersRepository @Inject constructor(
    private val api: OrderApi,
    private val orderDao: OrderDao,
    private val database: AppDatabase,
    messageManager: GlobalMessageManager,
    @ApplicationContext private val context: Context
) : BaseRepository(messageManager) {

    private val gson = Gson()

    // ───── API CALLS ───────────────────────────────────────────────────────

    suspend fun getOrdersFromApi(startDate: String): Resource<PaginatedOrders> =
        safeApiCall { api.getOrders(startDate) }

    suspend fun sendToExternalCourier(orderId: String, body: DispatchCourier) =
        safeApiCall { api.sendToExternalCourier(orderId, body) }

    suspend fun cancelExternalCourier(orderId: String, body: DispatchCancelCourier) =
        safeApiCall { api.cancledToExternalCourier(orderId, body) }

    suspend fun fetchOrderByIdFromApi(id: String): Resource<Order> =
        safeApiCall { api.getOrderById(id) }

    suspend fun updateOrder(
        orderId: String,
        status: OrderStatusEnum,
        data: UpdateOrderData
    ): Resource<Order> =
        safeApiCall { api.updateOrder(orderId, status.name.lowercase(), data) }

    suspend fun assignCourier(orderId: String, courier: UpdateCourierOrder): Resource<Order> =
        safeApiCall { api.assignCourier(orderId, courier) }

    suspend fun getOrderTras(): Resource<List<OrderTras>> =
        safeApiCall { api.getOrderTras() }

    suspend fun getShiftStatus(date: String): Resource<ShiftResponse> =
        safeApiCall { api.shiftStatus(date) }

    suspend fun checkIn(body: ShiftCheckIn): Resource<ShiftResponse> =
        safeApiCall { api.checkIn(body) }

    suspend fun checkOut(body: ShiftCheckOut): Resource<ShiftResponse> =
        safeApiCall { api.checkOut(body) }

    suspend fun batchUpdateOrderStatus(body: BatchUpdateStatusRequest) =
        safeApiCall { api.batchUpdateOrderStatus(body) }

    // ───── ROOM (DAO) & SYNC LOGIC ─────────────────────────────────────────

    fun getAllOrdersFlow() = orderDao.getAllOrders()

    /**
     * Kluczowa funkcja do synchronizacji.
     * 1. Pobiera ID zamówień, które już mamy lokalnie.
     * 2. Wykonuje transakcję synchronizacji (usuń stare, wstaw nowe).
     * 3. Po transakcji, filtruje zamówienia, które są *naprawdę* nowe
     * i mają status "processing".
     * 4. Uruchamia dla nich usługę alarmu.
     */
    /**
     * ✅ POPRAWKA #2: Inteligentna synchronizacja z API
     *
     * Kluczowa funkcja do synchronizacji z mechanizmem ochrony przed nadpisywaniem nowszych statusów.
     *
     * PROBLEM: Wcześniejsza wersja ślepo nadpisywała wszystkie zamówienia z API,
     * co powodowało że lokalne zaakceptowane zamówienia wracały do PROCESSING
     * jeśli backend miał opóźnienie w synchronizacji.
     *
     * ROZWIĄZANIE: Sprawdzanie czy lokalny status jest "wyższy" (bardziej zaawansowany)
     * niż zdalny. Nie nadpisujemy jeśli lokalne zamówienie jest już zaakceptowane/ukończone.
     *
     * 1. Dla każdego zamówienia z API sprawdza czy istnieje lokalnie
     * 2. Porównuje statusy (czy lokalny jest bardziej zaawansowany)
     * 3. Aktualizuje tylko jeśli remote jest nowsze LUB zamówienie jest nowe
     * 4. Loguje wszystkie konflikty dla diagnostyki
     * 5. Po transakcji usuwa zamówienia których już nie ma w API
     */
    suspend fun syncLocalDatabaseWithRemote(
        remoteOrders: List<OrderEntity>,
        syncDate: String
    ) {
        val datePrefix = syncDate.substring(0, 10) // np. "2025-11-03"
        val remoteOrderIds = remoteOrders.map { it.orderId }

        Timber.tag("SYNC_CONFLICT").d("═══════════════════════════════════════")
        Timber.tag("SYNC_CONFLICT").d("🔄 SYNC START")
        Timber.tag("SYNC_CONFLICT").d("   ├─ Date: $datePrefix")
        Timber.tag("SYNC_CONFLICT").d("   ├─ Remote orders: ${remoteOrders.size}")
        Timber.tag("SYNC_CONFLICT").d("   └─ Timestamp: ${System.currentTimeMillis()}")

        database.withTransaction {
            // ✅ INTELIGENTNA SYNCHRONIZACJA: Nie nadpisuj bardziej zaawansowanych statusów
            remoteOrders.forEach { remote ->
                val local = orderDao.getOrderById(remote.orderId)

                val shouldUpdate = when {
                    local == null -> {
                        // Nowe zamówienie - zawsze dodaj
                        Timber.tag("SYNC_CONFLICT").d("➕ NEW ORDER: ${remote.orderId} (${remote.orderNumber})")
                        true
                    }
                    else -> {
                        // Sprawdź czy lokalny status jest bardziej zaawansowany
                        val localStatus = local.orderStatus.slug
                        val remoteStatus = remote.orderStatus.slug

                        // Status sequence: PROCESSING(2) < ACCEPTED(4) < OUT_FOR_DELIVERY(6) < COMPLETED(7)
                        val localSeq = local.orderStatus.sequence
                        val remoteSeq = remote.orderStatus.sequence

                        val shouldUpdate = remoteSeq >= localSeq

                        if (shouldUpdate) {
                            Timber.tag("SYNC_CONFLICT").d("═══════════════════════════════════════")
                            Timber.tag("SYNC_CONFLICT").d("🔄 UPDATING ORDER: ${remote.orderId}")
                            Timber.tag("SYNC_CONFLICT").d("   ├─ orderNumber: ${remote.orderNumber}")
                            Timber.tag("SYNC_CONFLICT").d("   ├─ Remote status: $remoteStatus (seq=$remoteSeq)")
                            Timber.tag("SYNC_CONFLICT").d("   ├─ Local status: $localStatus (seq=$localSeq)")
                            Timber.tag("SYNC_CONFLICT").d("   └─ Decision: REMOTE >= LOCAL → UPDATE")
                        } else {
                            Timber.tag("SYNC_CONFLICT").d("═══════════════════════════════════════")
                            Timber.tag("SYNC_CONFLICT").d("⏭️ SKIPPING ORDER: ${remote.orderId}")
                            Timber.tag("SYNC_CONFLICT").d("   ├─ orderNumber: ${remote.orderNumber}")
                            Timber.tag("SYNC_CONFLICT").d("   ├─ Remote status: $remoteStatus (seq=$remoteSeq)")
                            Timber.tag("SYNC_CONFLICT").d("   ├─ Local status: $localStatus (seq=$localSeq)")
                            Timber.tag("SYNC_CONFLICT").d("   └─ Decision: LOCAL > REMOTE → SKIP (protect local status)")
                        }

                        shouldUpdate
                    }
                }

                if (shouldUpdate) {
                    orderDao.insertOrUpdate(remote)
                }
            }

            // ✅ Usuń zamówienia których już nie ma w API (dla tej daty)
            orderDao.deleteStaleOrdersForDate(remoteOrderIds)
            Timber.tag("SYNC_CONFLICT").d("🗑️ Deleted stale orders not in remote list")
        }

        Timber.tag("SYNC_CONFLICT").d("✅ SYNC COMPLETE for $datePrefix")
        Timber.tag("SYNC_CONFLICT").d("═══════════════════════════════════════")
        Timber.tag("OrdersRepository").i("Sync: Baza danych zaktualizowana dla $datePrefix.")
    }

    suspend fun insertOrUpdateOrder(order: OrderEntity) {
        orderDao.insertOrUpdate(order)
    }

    suspend fun updateOrderStatusSlug(orderId: String, slug: OrderStatusEnum) {
        orderDao.updateStatusSlug(orderId, slug)
    }

    suspend fun updateOrderCourier(orderId: String, courier: Courier?) {
        val entity = courier.toEntity()
        orderDao.updateCourier(
            orderId = orderId,
            courierId = entity?.id,
            courierName = entity?.name
        )
    }
    suspend fun updateExternalDeliveryInfo(orderId: String, payload: ExternalDeliveryOutboxPayload)  {
        orderDao.updateExternalDelivery(
            orderId = orderId,
            externalPickupEta = payload.pickupEta,
            externalDropoffEta = payload.dropoffEta,
            status = payload.status?.name
        )
    }

    // ───── HELPER ──────────────────────────────────────────────────────────

    private fun Courier?.toEntity(): CourierEntity? {
        return this?.let { CourierEntity(id = it.id, name = it.name) }
    }
}
