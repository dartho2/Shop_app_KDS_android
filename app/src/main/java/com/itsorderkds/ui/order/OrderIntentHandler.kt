//package com.itsorderkds.ui.order
//
//import android.content.Intent
//import android.util.Log
//import com.google.gson.Gson
//import com.google.gson.JsonObject
//import com.itsorderkds.data.model.Order
//import com.itsorderkds.data.model.OrderStatusWrapper
//import com.itsorderkds.data.socket.SocketAction
//import javax.inject.Inject
//
//class OrderIntentHandler @Inject constructor(private val gson: Gson) {
//    fun handle(intent: Intent?, viewModel: OrdersViewModel) {
//        val action = intent?.action
//        val json    = intent?.getStringExtra(SocketAction.Extra.ORDER_JSON) ?: return
//
//        when (action) {
//            SocketAction.Action.NEW_ORDER, null -> parseFullOrder(json)?.let { order ->
//                if (order.orderStatus?.slug == OrderStatusEnum.PROCESSING) {
//                    Log.d(TAG, "New PROCESSING order: ${order.orderNumber}")
//                    viewModel.addOrUpdateOrder(order)
//                }
//            }
//
//            SocketAction.Action.ORDER_PROCESSING -> parseStatusWrapper(json)?.let { wrapper ->
//                parseJsonObject(json)?.get("orderId")?.asString?.let { id ->
//                    viewModel.updateOrderStatusSlug(id, wrapper.orderStatus.slug)
//                }
//            }
//
//            else -> parseJsonObject(json)?.let { obj ->
//                obj.get("orderId")?.asString?.let { id ->
////                    obj.getAsJsonObject("orderStatus")?.get("slug")?.asString?.takeIf { it.isNotBlank() }?.let { slug ->
////                        // Local status update
////                        viewModel.updateOrderStatus(id, slug)
////                    } ?: viewModel.refreshOrderById(id)
//                }
//            }
//        }
//    }
//
//    private fun parseFullOrder(json: String) = runCatching {
//        gson.fromJson(json, Order::class.java)
//    }.getOrNull()
//
//    private fun parseStatusWrapper(json: String) = runCatching {
//        gson.fromJson(json, OrderStatusWrapper::class.java)
//    }.getOrNull()
//
//    private fun parseJsonObject(json: String) = runCatching {
//        gson.fromJson(json, JsonObject::class.java)
//    }.getOrNull()
//
//    companion object { private const val TAG = "OrderIntentHandler" }
//}
