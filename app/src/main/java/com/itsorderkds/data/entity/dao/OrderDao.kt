package com.itsorderkds.data.dao

import androidx.room.*
import com.itsorderkds.data.entity.CourierEntity
import com.itsorderkds.data.entity.OrderEntity
import com.itsorderkds.ui.order.DeliveryStatusEnum
import com.itsorderkds.ui.order.OrderStatusEnum
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(order: OrderEntity)

    @Query("SELECT orderId FROM orders")
    suspend fun getAllOrderIds(): List<String>

    @Query("""
       UPDATE orders
          SET courier_id   = :courierId,
              courier_name = :courierName
        WHERE orderId = :orderId
""")
    suspend fun updateCourier(
        orderId:     String,
        courierId:   String?,   // ← null = wyzeruj
        courierName: String?    // ← null = wyzeruj
    )

    @Query("""
       UPDATE orders
          SET external_pickupEta = :externalPickupEta,
              external_dropoffEta = :externalDropoffEta,
              external_status = :status
        WHERE orderId = :orderId
""")
    suspend fun updateExternalDelivery(
        orderId: String,
        externalPickupEta: String?,
        externalDropoffEta: String?,
        status: String?
    )

    /**
     * Usuwa zamówienia, które są "przestarzałe" (stale) DLA KONKRETNEJ DATY.
     *
     * Używamy strftime('%Y-%m-%d', createdAt), aby porównać tylko datę (YYYY-MM-DD)
     * z pełnego ISO stringa (np. "2025-10-31T10:00:00Z").
     *
     * @param datePrefix Data w formacie "YYYY-MM-DD".
     * @param remoteIds Lista ID zamówień pobranych z API dla tej daty.
     */
    /**
     * ✅ POPRAWIONA FUNKCJA
     * Usuwa zamówienia, które są "przestarzałe" (stale) DLA KONKRETNEJ DATY.
     * Używa SUBSTR, aby porównać tylko datę (YYYY-MM-DD).
     */
    @Query("""
        DELETE FROM orders
        WHERE orderId NOT IN (:remoteIds)       -- ✅ POPRAWKA: Których nie ma w API
    """)
    suspend fun deleteStaleOrdersForDate(remoteIds: List<String>)

    @Query("""
        DELETE FROM orders
        WHERE createdAt < :syncDate
    """)
    suspend fun deleteOldCompletedOrders(syncDate: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(orders: List<OrderEntity>)

    @Query("DELETE FROM orders WHERE orderId NOT IN (:ids)")
    suspend fun deleteOrdersNotIn(ids: List<String>)

    @Query("DELETE FROM orders")
    suspend fun deleteAll()

    @Update
    suspend fun update(order: OrderEntity)

    @Query("DELETE FROM orders WHERE orderId = :id")
    suspend fun deleteById(id: String)

    @Delete
    suspend fun delete(order: OrderEntity)

    @Query("SELECT * FROM orders WHERE orderId = :id")
    suspend fun getById(id: String): OrderEntity?

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAll(): Flow<List<OrderEntity>>

    @Query("DELETE FROM orders")
    suspend fun clearAll()

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("DELETE FROM orders WHERE orderId NOT IN (:remoteOrderIds)")
    suspend fun deleteOrdersNotInList(remoteOrderIds: List<String>)

//    @Query(
//    """
//        UPDATE orders SET
//            order_status_name          = :stName,
//            order_status_sequence      = :stSeq,
//            order_status_slug          = :stSlug
//        WHERE orderId = :orderId
//        """
//    )
//    suspend fun updateStatus(
//        orderId:  String,
//        stId:     String,
//        stName:   String,
//        stSeq:    Int,
//        stSlug:   String,
//        stSysRes: Boolean,
//        stActive: Boolean
//    )
    @Query("UPDATE orders SET order_status_slug = :stSlug WHERE orderId = :orderId")
    suspend fun updateStatusSlug(orderId: String, stSlug: OrderStatusEnum)

    @Query("SELECT * FROM orders WHERE orderId = :id LIMIT 1")
    suspend fun getOrderById(id: String): OrderEntity?

}
