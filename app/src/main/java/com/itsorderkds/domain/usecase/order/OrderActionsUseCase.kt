package com.itsorderkds.domain.usecase.order

import com.itsorderkds.data.model.BatchUpdateStatusRequest
import com.itsorderkds.data.model.DispatchCourier
import com.itsorderkds.data.model.Order
import com.itsorderkds.data.model.UpdateCourierOrder
import com.itsorderkds.data.model.UpdateOrderData
import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.repository.OrdersRepository
import com.itsorderkds.ui.order.DeliveryEnum
import com.itsorderkds.ui.order.OrderStatusEnum
import javax.inject.Inject

/**
 * Use Case zawierający logikę biznesową akcji na zamówieniach.
 * Wydzielony z OrdersViewModel dla lepszej separacji odpowiedzialności.
 */
class OrderActionsUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository
) {

    /**
     * Wysyła zamówienie do zewnętrznego API kurierskiego.
     */
    suspend fun sendToExternalCourier(
        orderId: String,
        courierDelivery: DeliveryEnum,
        timePrepare: Int
    ): Resource<Any> {
        val body = DispatchCourier(
            courier = courierDelivery,
            timePrepare = timePrepare
        )
        return ordersRepository.sendToExternalCourier(orderId, body)
    }

    /**
     * Aktualizuje kuriera przypisanego do zamówienia.
     */
    suspend fun updateOrderCourier(
        orderId: String,
        body: UpdateCourierOrder
    ): Resource<Order> {
        return ordersRepository.assignCourier(orderId, body)
    }

    /**
     * Aktualizuje status zamówienia.
     */
    suspend fun updateOrderStatus(
        orderId: String,
        status: OrderStatusEnum,
        data: UpdateOrderData
    ): Resource<Order> {
        return ordersRepository.updateOrder(orderId, status, data)
    }

    /**
     * Aktualizuje status wielu zamówień jednocześnie (batch).
     */
    suspend fun batchUpdateOrdersStatus(
        orderIds: Set<String>,
        newStatus: OrderStatusEnum
    ): Resource<Any> {
        val body = BatchUpdateStatusRequest(
            orderIds = orderIds,
            newStatus = newStatus.name.lowercase()
        )
        return ordersRepository.batchUpdateOrderStatus(body)
    }
}

