package com.itsorderkds.service

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.itsorderkds.data.entity.mapper.OrderMapper
import com.itsorderkds.data.model.CourierChangePayload
import com.itsorderkds.data.model.ExternalDeliveryOutboxPayload
import com.itsorderkds.data.model.OrderStatusWrapper
import com.itsorderkds.data.model.OrderTras
import com.itsorderkds.data.model.OrderWrapper
import com.itsorderkds.data.network.Resource
import com.itsorderkds.data.network.preferences.TokenProvider
import com.itsorderkds.data.preferences.AppPreferencesManager
import com.itsorderkds.data.repository.OrdersRepository
import com.itsorderkds.data.responses.UserRole
import com.itsorderkds.data.socket.SocketAction
import com.itsorderkds.data.util.AppStateManager
import com.itsorderkds.notifications.NotificationHelper
import com.itsorderkds.ui.order.OrderStatusEnum
import com.itsorderkds.ui.settings.print.PrinterService
import com.itsorderkds.ui.theme.home.HomeActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketStaffEventsHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ordersRepository: OrdersRepository,
    private val socketEventsRepo: SocketEventsRepository,
    private val tokenProvider: TokenProvider,
    private val printerService: PrinterService,
    private val appPreferencesManager: AppPreferencesManager
) {
    private val job = SupervisorJob()
    private val ioScope = CoroutineScope(job + Dispatchers.IO)
    private val gson: Gson = Gson()

    private fun onStatusUpdate(action: String): (Array<Any>) -> Unit = { args ->
        handleStatusUpdate(args, action)
    }


//    private fun onExternalCourierUpdate(action: String): (Array<Any>) -> Unit = { args ->
//        handleExternalCourierUpdate(args, action)
//    }

    private val eventHandlers: Map<SocketEvent, (Array<Any>) -> Unit> = mapOf(
        SocketEvent.REJECT_ORDER_COURIER to { args -> handleCourierChange(args, isAccept = false) },
        SocketEvent.ACCEPT_ORDER_COURIER to { args -> handleCourierChange(args, isAccept = true) },
        SocketEvent.TRAS_COURIER_ORDER_NEW to ::handleCourierTrasChange,
        SocketEvent.ORDER_CREATED to ::handleNewOrProcessingOrder,
        SocketEvent.ORDER_PROCESSING to onStatusUpdate(SocketAction.Action.ORDER_PROCESSING),
        SocketEvent.ORDER_PENDING to onStatusUpdate(SocketAction.Action.ORDER_PENDING),
        SocketEvent.ORDER_CANCELLED to onStatusUpdate(SocketAction.Action.ORDER_CANCELLED),
        SocketEvent.ORDER_ACCEPTED to onStatusUpdate(SocketAction.Action.ORDER_ACCEPTED),
        SocketEvent.ORDER_OUT_FOR_DELIVERY to onStatusUpdate(SocketAction.Action.ORDER_OUT_FOR_DELIVERY),
        SocketEvent.ORDER_COMPLETED to onStatusUpdate(SocketAction.Action.ORDER_COMPLETED),
        SocketEvent.ORDER_NEW_COURIER to onStatusUpdate(SocketAction.Action.ORDER_NEW_COURIER),
        SocketEvent.ORDER_UPDATE to onStatusUpdate(SocketAction.Action.ORDER_UPDATE),
//        SocketEvent.OPEN_HOURS_UPDATED to onOpenHoursUpdate(SocketAction.Action.OPEN_HOURS_UPDATED),
//        SocketEvent.OPEN_HOURS_PAUSE_CLEARED to onOpenHoursUpdate(SocketAction.Action.OPEN_HOURS_PAUSE_CLEARED),
//        SocketEvent.OPEN_HOURS_CREATED to onOpenHoursUpdate(SocketAction.Action.OPEN_HOURS_CREATED),
//        SocketEvent.OPEN_HOURS_PAUSE_SET to onOpenHoursUpdate(SocketAction.Action.OPEN_HOURS_PAUSE_SET),
        SocketEvent.ORDER_SEND_TO_EXTERNAL_SUCCESS to onExternalOutboxUpdate(SocketAction.Action.ORDER_SEND_TO_EXTERNAL_SUCCESS),
        SocketEvent.ORDER_SEND_TO_EXTERNAL_FAILED to onExternalOutboxUpdate(SocketAction.Action.ORDER_SEND_TO_EXTERNAL_FAILED),
        SocketEvent.ORDER_SEND_TO_EXTERNAL_COURIER to onExternalOutboxUpdate(SocketAction.Action.ORDER_SEND_TO_EXTERNAL_COURIER),
        // ORDER_REMINDER usunięty — KDS nie używa alarmu dla niezaakceptowanych zamówień
    )

    fun register() {
        eventHandlers.forEach { (event, handler) ->
            try {
                SocketManager.on(event.name, handler)
            } catch (t: Throwable) {
                Timber.tag(TAG).e(t, "Failed to register socket handler for ${event.name}")
            }
        }
    }

    fun shutdown() {
        job.cancel()
    }

    private fun onExternalOutboxUpdate(action: String): (Array<Any>) -> Unit = { args ->
        handleExternalOutboxUpdate(args, action)
    }

    private fun handleExternalOutboxUpdate(args: Array<Any>, action: String) {
        val payload = parsePayload(args, ExternalDeliveryOutboxPayload::class.java) ?: return

        Timber.tag(TAG).i(
            "🚚 ExternalOutboxUpdate received: orderId=%s, orderNumber=%s, integration=%s, status=%s, pickupEta=%s, dropoffEta=%s",
            payload.orderId ?: "null",
            payload.orderNumber ?: "null",
            payload.integration,
            payload.status,
            payload.pickupEta,
            payload.dropoffEta
        )

        val orderId = payload.orderId
        val orderNumber = payload.orderNumber

        // Powiadomienie MUSI się pokazać nawet bez orderId/orderNumber
        NotificationHelper.showExternalDeliverySuccess(
            context = context,
            orderId = orderId,
            orderNumber = orderNumber,
            status = payload.status
        )

        // Jeśli nie mamy orderId – nie próbujemy fetcha, tylko logujemy i lecimy dalej
        if (orderId.isNullOrBlank()) {
            Timber.tag(TAG).w("⚠️ Cannot fetch updated order from API – missing orderId in payload")
        } else {
            ioScope.launch {
                runCatching {
                    Timber.tag(TAG).d("🔄 Fetching updated order from API for orderId=$orderId")

                    when (val res = ordersRepository.fetchOrderByIdFromApi(orderId)) {
                        is Resource.Success -> {
                            val order = res.value
                            Timber.tag(TAG).d(
                                "✅ Fetched updated order: orderId=%s, externalDelivery.status=%s, pickupEta=%s, dropoffEta=%s",
                                order.orderId,
                                order.externalDelivery?.status,
                                order.externalDelivery?.pickupEta,
                                order.externalDelivery?.dropoffEta
                            )
                            Timber.tag(TAG).d("📤 Emitting order to socketEventsRepo...")
                            socketEventsRepo.emitOrder(order)
                            Timber.tag(TAG).d("✅ Order emitted successfully")
                        }
                        is Resource.Failure -> {
                            Timber.tag(TAG).w("❌ Failed to fetch updated order from API: ${res.errorMessage}")
                        }
                        is Resource.Loading -> {
                            Timber.tag(TAG).d("⏳ Fetching order is in progress...")
                        }
                    }
                }.onFailure {
                    Timber.tag(TAG).e(it, "💥 Failed to fetch and update order: $orderId")
                }
            }
        }

        sendJsonBroadcast(action, gson.toJson(payload))
    }

//    private fun handleExternalCourierUpdate(args: Array<Any>, action: String) {
//        val payload = parsePayload(args, ExternalCourierPayload::class.java) ?: return
//
//        Timber.tag(TAG).i(
//            "🚚 ExternalCourierUpdate received: orderId=%s, orderNumber=%s, courier=%s, status=%s",
//            payload.orderId ?: "null",
//            payload.orderNumber ?: "null",
//            payload.courier,
//            payload.deliveryStatus
//        )
//
//        val orderId = payload.orderId
//
//        if (orderId.isNullOrBlank()) {
//            Timber.tag(TAG).w("⚠️ Cannot update external courier status – missing orderId in payload")
//        } else {
//            ioScope.launch {
//                runCatching {
//                    Timber.tag(TAG).d("🔄 Updating external courier status for orderId=$orderId with status=${payload.deliveryStatus}")
//
//                    // Emituj status update do socketEventsRepo
//                    if (payload.deliveryStatus != null) {
//                        socketEventsRepo.emitExternalCourierStatusUpdate(orderId, payload.deliveryStatus)
//                        Timber.tag(TAG).d("✅ External courier status update emitted: orderId=$orderId, status=${payload.deliveryStatus}")
//                    }
//
//                    // Pobierz zaktualizowane zamówienie z API (opcjonalne, aby mieć pełne dane)
//                    when (val res = ordersRepository.fetchOrderByIdFromApi(orderId)) {
//                        is Resource.Success -> {
//                            Timber.tag(TAG).d("✅ Fetched updated order with external courier status, emitting to UI")
//                            socketEventsRepo.emitOrder(res.value)
//                        }
//                        is Resource.Failure -> {
//                            Timber.tag(TAG).w("⚠️ Failed to fetch updated order from API: ${res.errorMessage}")
//                        }
//                        is Resource.Loading -> {
//                            Timber.tag(TAG).d("⏳ Fetching order is in progress...")
//                        }
//                    }
//                }.onFailure {
//                    Timber.tag(TAG).e(it, "💥 Failed to update external courier status: $orderId")
//                }
//            }
//        }
//
//        sendJsonBroadcast(action, gson.toJson(payload))
//    }

    private fun handleCourierChange(args: Array<Any>, isAccept: Boolean) {
        val payload = parsePayload(args, CourierChangePayload::class.java) ?: return
        val action =
            if (isAccept) SocketAction.Action.ACCEPT_ORDER_COURIER else SocketAction.Action.REJECT_ORDER_COURIER
        Timber.tag(TAG).i("Handling courier change ($action) for order: ${payload.orderId}")

        ioScope.launch {
            runCatching {
                ordersRepository.updateOrderCourier(payload.orderId, payload.courier)
                val myId = tokenProvider.getUserID()
                if (myId != null && myId == payload.courier?.id) {
                    when (val res = ordersRepository.fetchOrderByIdFromApi(payload.orderId)) {
                        is Resource.Success -> socketEventsRepo.emitOrder(res.value)
                        else -> socketEventsRepo.emitStatus(payload.orderId, OrderStatusEnum.ACCEPTED)
                    }
                }
            }.onFailure {
                Timber.tag(TAG).e(it, "Error handling courier change for order ${payload.orderId}")
            }
        }

        sendJsonBroadcast(action, gson.toJson(payload))
    }

    private fun handleCourierTrasChange(args: Array<Any>) {
        val jsonString = args.firstOrNull()?.toString() ?: return
        try {
            val type = object : TypeToken<List<OrderTras>>() {}.type
            val routes: List<OrderTras> = gson.fromJson(jsonString, type)
            Timber.tag(TAG).i("Handling new courier route, ${routes.size} orders.")
            NotificationHelper.showSimpleNotification(context, routes.size)
            ioScope.launch {
                runCatching { socketEventsRepo.emitOrderTras(routes) }
                    .onFailure { Timber.tag(TAG).e(it, "emitOrderTras failed") }
            }
        } catch (e: JsonSyntaxException) {
            Timber.tag(TAG).e(e, "Error parsing routes JSON")
        }
    }


    private fun openHomeWithPayload(jsonPayload: String, isReminder: Boolean = false) {
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(SocketAction.Extra.ORDER_JSON, jsonPayload)
            if (isReminder) putExtra("EXTRA_IS_REMINDER", true)
        }
        context.startActivity(intent)
    }

    private fun handleNewOrProcessingOrder(args: Array<Any>) {
        val orderWrapper = parsePayload(args, OrderWrapper::class.java) ?: return
        val order = orderWrapper.order
        val json = gson.toJson(orderWrapper)

        // ✅ LOGI DIAGNOSTYCZNE: Wiek zamówienia
        val createdAtMs = runCatching {
            java.time.Instant.parse(order.createdAt).toEpochMilli()
        }.getOrNull() ?: 0L
        val ageSeconds = (System.currentTimeMillis() - createdAtMs) / 1000
        val ageMinutes = ageSeconds / 60

        Timber.tag("EVENT").i("ORDER: ${order.orderNumber}, Action: ORDER_CREATED")

        val slugEnum = order.orderStatus.slug?.let {
            runCatching { OrderStatusEnum.valueOf(it) }.getOrNull()
        } ?: OrderStatusEnum.UNKNOWN

        val isProcessing = slugEnum == OrderStatusEnum.PROCESSING

        Timber.tag("SOCKET_EVENT").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag("SOCKET_EVENT").d("📥 ORDER_CREATED/PROCESSING received")
        Timber.tag("SOCKET_EVENT").d("   ├─ orderId: ${order.orderId}")
        Timber.tag("SOCKET_EVENT").d("   ├─ orderNumber: ${order.orderNumber}")
        Timber.tag("SOCKET_EVENT").d("   ├─ status: ${order.orderStatus.slug}")
        Timber.tag("SOCKET_EVENT").d("   ├─ createdAt: ${order.createdAt}")
        Timber.tag("SOCKET_EVENT").d("   ├─ age: ${ageSeconds}s (${ageMinutes}min)")
        Timber.tag("SOCKET_EVENT").d("   ├─ isProcessing: $isProcessing")
        Timber.tag("SOCKET_EVENT").d("   ├─ deliveryType: ${order.deliveryType}")
        Timber.tag("SOCKET_EVENT").d("   └─ Will emit to Flow: $isProcessing")

        if (isProcessing) {
            Timber.tag(TAG).d("Handling new order: ${order.orderId} (Status: PROCESSING) -> STARTING ALARM")
        } else {
            Timber.tag(TAG).d("Handling new order: ${order.orderId} (Status: ${order.orderStatus.slug}) -> SILENTLY SAVING")
        }

        ioScope.launch {
            if (tokenProvider.getRole() != UserRole.COURIER) {
                runCatching {
                    ordersRepository.insertOrUpdateOrder(OrderMapper.fromOrder(order))
                    socketEventsRepo.emitOrder(order)

                    // ✅ Auto-print dla nowych zamówień (PROCESSING) z DataStore
                    // WYKLUCZAMY DINE_IN/ROOM_SERVICE - są obsługiwane w OrdersViewModel
                    if (isProcessing &&
                        order.deliveryType != com.itsorderkds.ui.order.OrderDelivery.DINE_IN &&
                        order.deliveryType != com.itsorderkds.ui.order.OrderDelivery.ROOM_SERVICE) {
                        val autoPrintEnabled = appPreferencesManager.getAutoPrintEnabled()
                        Timber.tag(TAG).d("Auto-print (new order) enabled=$autoPrintEnabled for ${order.orderNumber}")
                        if (autoPrintEnabled) {
                            try {
                                printerService.printOrder(order, useDeliveryInterval = true)
                                Timber.tag(TAG).d("✅ Auto-print done for ${order.orderNumber}")
                            } catch (e: Exception) {
                                Timber.tag(TAG).e(e, "❌ Auto-print failed for order: ${order.orderId}")
                            }
                        }
                    }
                }.onFailure {
                    Timber.tag(TAG).e(it, "Failed handling new order ${order.orderId}")
                }
            }
        }

        if (isProcessing && !AppStateManager.isAppInForeground) {
            Timber.tag(TAG).d("App is in background, starting HomeActivity for new order.")
            openHomeWithPayload(json, isReminder = false)
        }

        sendJsonBroadcast(SocketAction.Action.NEW_ORDER, json)
    }

    private fun handleStatusUpdate(args: Array<Any>, action: String) {
        val rawJson = args.firstOrNull()?.toString() ?: return
        val wrapper = parsePayload(args, OrderStatusWrapper::class.java) ?: return
        val orderId = wrapper.orderId

        Timber.tag("SOCKET_EVENT").d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag("SOCKET_EVENT").d("📊 STATUS UPDATE received")
        Timber.tag("SOCKET_EVENT").d("   ├─ orderId: $orderId")
        Timber.tag("SOCKET_EVENT").d("   ├─ action: $action")
        Timber.tag("SOCKET_EVENT").d("   ├─ newStatus: ${wrapper.orderStatus.slug}")
        Timber.tag("SOCKET_EVENT").d("   ├─ Source: WebSocket")
        Timber.tag("SOCKET_EVENT").d("   └─ Timestamp: ${System.currentTimeMillis()}")

        Timber.tag("EVENT").i("ORDER: $orderId, Action: $action")

        val isProcessing = action == SocketAction.Action.ORDER_PROCESSING ||
                "processing".equals(wrapper.orderStatus.slug.toString(), ignoreCase = true)

        if (isProcessing) {
            Timber.tag(TAG).d("Handling status update: ${orderId} (Status: PROCESSING) -> STARTING ALARM")
            if (!AppStateManager.isAppInForeground) {
                Timber.tag(TAG).d("OrderAlarmService App in background, starting HomeActivity for processing order.")
                openHomeWithPayload(rawJson, isReminder = false)
            }
        }

        // ✅ POPRAWKA: Dla ORDER_UPDATE i ORDER_ACCEPTED pobieramy pełne zamówienie z API
        // aby zaktualizować ceny i produkty (nie tylko status!)
        val shouldFetchFullOrder = action == SocketAction.Action.ORDER_UPDATE ||
                action == SocketAction.Action.ORDER_ACCEPTED

        ioScope.launch {
            runCatching {
                val slugString = wrapper.orderStatus.slug
                val slugEnum = slugString?.let {
                    runCatching { OrderStatusEnum.valueOf(it) }.getOrNull()
                } ?: OrderStatusEnum.UNKNOWN

                if (shouldFetchFullOrder) {
                    // ✅ Pobierz pełne zamówienie z API (ze zaktualizowanymi cenami i produktami)
                    Timber.tag(TAG).d("🔄 Fetching full order from API for $action (orderId=$orderId)")

                    when (val res = ordersRepository.fetchOrderByIdFromApi(orderId)) {
                        is Resource.Success -> {
                            val order = res.value
                            Timber.tag(TAG).d(
                                "✅ Fetched full order: orderId=%s, total=%.2f, products=%d, status=%s",
                                order.orderId,
                                order.total,
                                order.products?.size ?: 0,
                                order.orderStatus?.slug
                            )
                            // Zapisz pełne zamówienie do bazy
                            ordersRepository.insertOrUpdateOrder(OrderMapper.fromOrder(order))
                            // Emituj pełne zamówienie do UI
                            socketEventsRepo.emitOrder(order)
                        }
                        is Resource.Failure -> {
                            Timber.tag(TAG).w("⚠️ Failed to fetch full order from API: ${res.errorMessage}, falling back to status update only")
                            // Fallback: aktualizuj tylko status
                            ordersRepository.updateOrderStatusSlug(orderId, slugEnum)
                            socketEventsRepo.emitStatus(orderId, slugEnum)
                        }
                        is Resource.Loading -> {
                            Timber.tag(TAG).d("⏳ Fetching full order is in progress...")
                        }
                    }
                } else {
                    // Dla innych eventów (ORDER_COMPLETED, ORDER_CANCELLED, etc.) - tylko status
                    ordersRepository.updateOrderStatusSlug(orderId, slugEnum)
                    socketEventsRepo.emitStatus(orderId, slugEnum)
                }
            }.onFailure {
                Timber.tag(TAG).e(it, "Failed updating status for order $orderId")
            }
        }

        sendJsonBroadcast(action, rawJson)
    }


    private fun <T> parsePayload(args: Array<Any>, type: Class<T>): T? {
        val json = args.firstOrNull()?.toString() ?: run {
            Timber.tag(TAG).w("Socket event payload was null for type ${type.simpleName}")
            return null
        }
        return try {
            gson.fromJson(json, type)
        } catch (e: JsonSyntaxException) {
            Timber.tag(TAG).e(e, "Failed to parse JSON for type ${type.simpleName}")
            null
        }
    }

    private fun sendJsonBroadcast(action: String, jsonPayload: String) {
        Intent(action).apply {
            putExtra(SocketAction.Extra.ORDER_JSON, jsonPayload)
            setPackage(context.packageName)
        }.also {
            try {
                context.sendBroadcast(it)
            } catch (t: Throwable) {
                Timber.tag(TAG).e(t, "Failed to send broadcast for action $action")
            }
        }
    }

    private companion object {
        const val TAG = "SocketStaffEventsHandler"
    }
}
