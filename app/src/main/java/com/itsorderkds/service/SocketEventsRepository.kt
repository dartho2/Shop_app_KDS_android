package com.itsorderkds.service

import android.content.ContentValues.TAG
import android.util.Log
import com.itsorderkds.data.model.Order
import com.itsorderkds.data.model.OrderTras
import com.itsorderkds.ui.order.DeliveryStatusEnum
import com.itsorderkds.ui.order.OrderStatusEnum
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketEventsRepository @Inject constructor() {

    private val _orders = MutableSharedFlow<Order>(replay = 1)
    val orders = _orders.asSharedFlow()

    private val _ordersTras = MutableSharedFlow<List<OrderTras>>(replay = 1)
    val ordersTras = _ordersTras.asSharedFlow()

    private val _statuses = MutableSharedFlow<StatusEvent>(replay = 1)
    val statuses = _statuses.asSharedFlow()

    private val _connection = MutableSharedFlow<Boolean>(replay = 1)
    val connection = _connection.asSharedFlow()

    fun emitOrderTras(order: List<OrderTras>) {
        val success = _ordersTras.tryEmit(order)
    }
    fun emitOrder(order: Order) {
        Timber.tag(TAG).d("[emit] Order: $order")
        val success = _orders.tryEmit(order)
    }
    fun emitCourier(order: Order) {
        val success = _orders.tryEmit(order)
    }

    fun emitStatus(orderId: String, newStatus: OrderStatusEnum) {
        val success = _statuses.tryEmit(StatusEvent(orderId, newStatus))
    }
    fun emitExternalDeliveryUpdate(orderId: String, newStatus: OrderStatusEnum) {
        val success = _statuses.tryEmit(StatusEvent(orderId, newStatus))
    }

//    fun emitExternalCourierStatusUpdate(orderId: String, deliveryStatus: DeliveryStatusEnum) {
//        val success = _externalCourierStatuses.tryEmit(ExternalCourierStatusEvent(orderId, deliveryStatus))
//    }

    fun emitConnected() {
        _connection.tryEmit(true)
    }

    fun emitDisconnected() {
        _connection.tryEmit(false)
    }

    data class StatusEvent(val orderId: String, val newStatus: OrderStatusEnum)
}
