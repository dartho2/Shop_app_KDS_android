//package com.itsorderkds.ui.dialog
//
//import android.app.DatePickerDialog
//import android.app.TimePickerDialog
//import android.content.Intent
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.core.animateFloatAsState
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.ColumnScope
//import androidx.compose.foundation.layout.ExperimentalLayoutApi
//import androidx.compose.foundation.layout.FlowRow
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.CalendarMonth
//import androidx.compose.material.icons.filled.Fastfood
//import androidx.compose.material.icons.filled.KeyboardArrowDown
//// Usunięto 'Print', ponieważ jest teraz w TopAppBar
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.HorizontalDivider
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.OutlinedButton
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.rememberUpdatedState
//import androidx.compose.runtime.setValue
//import androidx.compose.runtime.key
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.draw.rotate
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.text.SpanStyle
//import androidx.compose.ui.text.buildAnnotatedString
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.text.style.TextDecoration
//import androidx.compose.ui.text.withStyle
//import androidx.compose.ui.unit.dp
//import com.itsorderkds.R
//import com.itsorderkds.data.model.Order
//import com.itsorderkds.data.model.OrderProduct
//import com.itsorderkds.data.responses.UserRole
//import com.itsorderkds.ui.order.Callbacks
//import com.itsorderkds.ui.order.DeliveryEnum
//import com.itsorderkds.ui.order.DeliveryStatusEnum
//import com.itsorderkds.ui.order.OrderDelivery
//import com.itsorderkds.ui.order.OrderStatusEnum
//import com.itsorderkds.ui.order.SourceEnum
//import com.itsorderkds.util.AppPrefs
//import com.itsorderkds.util.DateTimeUtils
//import com.itsorderkds.service.OrderAlarmService
//import java.time.LocalDateTime
//import java.time.ZonedDateTime
//import java.time.format.DateTimeFormatter
//import java.util.Locale
//import kotlinx.coroutines.launch
//import timber.log.Timber
//import java.time.ZoneId
//
//// --- GŁÓWNY KOMPONENT ARKUSZA ---
//@Composable
//fun AcceptOrderSheetContent(
//    modifier: Modifier = Modifier,
//    order: Order,
//    userRole: UserRole?,
//    userId: String?,
//    onClose: () -> Unit,
//    callbacks: Callbacks,
//) {
//    // Ensure internal remembered UI state is reset when we open the sheet for a different order
//    key(order.orderId) {
//        LazyColumn(
//            modifier = modifier.fillMaxWidth(), // Zmieniono na fillMaxWidth
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(16.dp),
//        ) {
//            item {
//                SheetHeader(order)
//            }
//            item {
//                ActionsSection(order, userRole, userId, onClose, callbacks)
//            }
//            item {
//                ProductSection(order)
//            }
//            item {
//                Spacer(Modifier.height(32.dp))
//            }
//        }
//    }
//}
//
//
//// --- KOMPONENTY UI ---
//
//@Composable
//private fun SheetHeader(order: Order) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp),
//        verticalAlignment = Alignment.Top,
//        horizontalArrangement = Arrangement.SpaceBetween,
//    ) {
//        Column(
//            modifier = Modifier.weight(1f),
//            verticalArrangement = Arrangement.spacedBy(8.dp),
//        ) {
//            Text(
//                "#${order.orderNumber}",
//                style = MaterialTheme.typography.headlineSmall,
//                fontWeight = FontWeight.Bold,
//            )
//            Text(
//                // ✅ POPRAWKA: Użyj 'name' zamiast 'slug.name'
//                order.orderStatus.name,
//                style = MaterialTheme.typography.labelLarge,
//                color = MaterialTheme.colorScheme.primary,
//            )
//            DeliveryTimeChip(order = order)
//        }
//        Column(
//            horizontalAlignment = Alignment.End,
//            verticalArrangement = Arrangement.spacedBy(4.dp),
//            modifier = Modifier.padding(start = 8.dp),
//        ) {
//            SourceIconOrSpace(
//                source = order.source?.name, // Zakładam, że 'source' to obiekt SourceOrder
//                modifier = Modifier.size(48.dp),
//            )
//            Text(
//                text = formatCreatedAt(order.createdAt, key = order.orderId),
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant,
//            )
//        }
//    }
//}
//
//@Composable
//private fun formatCreatedAt(dateString: String?, key: Any? = null): String {
//    return remember(key ?: dateString) {
//        DateTimeUtils.formatStringToLocalShort(dateString)
//    }
//}
//
//@Composable
//private fun formatDeliveryInterval(dateString: String?, key: Any? = null): String {
//    return remember(key ?: dateString) {
//        DateTimeUtils.formatStringToLocalShort(dateString)
//    }
//}
//
//@Composable
//private fun DeliveryTimeChip(order: Order) {
//    val isAsap = order.isAsap
//
//    val text = if (isAsap) {
//        stringResource(R.string.as_soon_as_possible)
//    } else {
//        formatDeliveryInterval(order.deliveryInterval, key = order.orderId)
//    }
//
//    val (icon, color) = if (isAsap) {
//        Icons.Default.Fastfood to MaterialTheme.colorScheme.primary
//    } else {
//        Icons.Default.CalendarMonth to MaterialTheme.colorScheme.secondary
//    }
//
//    Surface(
//        shape = MaterialTheme.shapes.small,
//        color = color.copy(alpha = 0.1f),
//        contentColor = color,
//    ) {
//        Row(
//            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.spacedBy(6.dp),
//        ) {
//            Icon(
//                imageVector = icon,
//                contentDescription = null,
//                modifier = Modifier.size(16.dp),
//            )
//            Text(
//                text = text,
//                style = MaterialTheme.typography.labelMedium,
//                fontWeight = FontWeight.Bold,
//            )
//        }
//    }
//}
//
//@Composable
//private fun ProductSection(order: Order) {
//    // Reset expansion when showing a different order
//    var isExpanded by remember(order.orderId) { mutableStateOf(false) }
//    val currency = AppPrefs.getCurrency()
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp),
//        shape = MaterialTheme.shapes.large,
//        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
//    ) {
//        Column(modifier = Modifier.padding(16.dp)) {
//
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clip(MaterialTheme.shapes.small)
//                    .clickable { isExpanded = !isExpanded }
//                    .padding(vertical = 4.dp),
//                verticalAlignment = Alignment.CenterVertically,
//            ) {
//                Text(
//                    text = stringResource(R.string.order_details_and_products),
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold,
//                    modifier = Modifier.weight(1f),
//                )
//                val rotationAngle by animateFloatAsState(
//                    targetValue = if (isExpanded) 180f else 0f,
//                    animationSpec = tween(durationMillis = 300),
//                    label = "ArrowRotation",
//                )
//                Icon(
//                    imageVector = Icons.Default.KeyboardArrowDown,
//                    contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
//                    modifier = Modifier.rotate(rotationAngle),
//                )
//            }
//
//            AnimatedVisibility(visible = isExpanded) {
//                ExpandedOrderDetails(order, currency)
//            }
//        }
//    }
//}
//
//@Composable
//private fun InfoRow(label: String, value: String, isBold: Boolean = false) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 2.dp),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.Top,
//    ) {
//        Text(
//            text = label,
//            style = MaterialTheme.typography.bodyMedium,
//            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
//            modifier = Modifier.weight(0.4f),
//        )
//        Text(
//            text = value,
//            style = MaterialTheme.typography.bodyMedium,
//            textAlign = TextAlign.End,
//            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
//            modifier = Modifier.weight(0.6f),
//        )
//    }
//}
//
//@Composable
//private fun ExpandedOrderDetails(order: Order, currency: String) {
//    Column {
//        HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp))
//
//        // Customer Information
//        CustomerInfo(order)
//
//        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
//
//        // Products List
//        order.products.forEach { product ->
//            ProductListItem(product)
//        }
//
//        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
//
//        // Delivery Address
//        DeliveryAddressInfo(order)
//
//        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
//
//        // Order Summary
//        OrderSummary(order, currency)
//    }
//}
//
//@Composable
//private fun CustomerInfo(order: Order) {
//    InfoRow(
//        label = stringResource(R.string.customer),
//        value = order.consumer?.name ?: stringResource(R.string.guest),
//    )
//    InfoRow(
//        label = stringResource(R.string.phone),
//        value = order.consumer?.phone ?: stringResource(R.string.common_dash),
//    )
//    order.courier?.name?.let { courierName ->
//        InfoRow(
//            label = stringResource(R.string.courier),
//            value = courierName,
//        )
//    }
//}
//
//@Composable
//private fun DeliveryAddressInfo(order: Order) {
//    val isDelivery = order.deliveryType == OrderDelivery.DELIVERY ||
//                     order.deliveryType == OrderDelivery.DELIVERY_EXTERNAL
//
//    val addressValue = if (isDelivery) {
//        buildString {
//            append(order.shippingAddress.street)
//            append(" ")
//            append(order.shippingAddress.numberHome)
//            append("/")
//            append(order.shippingAddress.numberFlat)
//            append(", ")
//            append(order.shippingAddress.city)
//        }
//    } else {
//        stringResource(R.string.personal_pickup)
//    }
//
//    InfoRow(
//        label = stringResource(R.string.address),
//        value = addressValue,
//    )
//}
//
//@Composable
//private fun OrderSummary(order: Order, currency: String) {
//    // Obliczenia
//    val serviceFee = order.additionalFeeTotal ?: 0.0
//    val point = order.pointsAmount ?: 0.0
//    val delivery = order.shippingTotal ?: 0.0
//    val discount = order.couponTotalDiscount ?: 0.0
//    val grandTotal = order.total ?: 0.0
//    val amount = order.amount ?: 0.0
//    val discountTotal = discount + point
//    val basketTotal =  amount + serviceFee + delivery
//
//    // Subtotal i opłaty
//    InfoRow(
//        label = stringResource(R.string.summary_subtotal),
//        value = stringResource(R.string.price_format_currency, order.amount, currency),
//    )
//    InfoRow(
//        label = stringResource(R.string.summary_delivery_fee),
//        value = stringResource(R.string.price_format_currency, order.shippingTotal, currency),
//    )
//
//    // Dodatkowe opłaty (jeśli istnieją)
//    if (serviceFee > 0.0) {
//        InfoRow(
//            label = stringResource(R.string.summary_service_fee),
//            value = stringResource(R.string.price_format_currency, serviceFee, currency),
//        )
//    }
//
//    // Zniżki (jeśli istnieją)
//    if (point > 0.0) {
//        InfoRow(
//            label = stringResource(R.string.line_point_use),
//            value = stringResource(R.string.price_format_currency_minus, point, currency),
//        )
//    }
//    if (discount > 0.0) {
//        InfoRow(
//            label = stringResource(R.string.line_discount),
//            value = stringResource(R.string.price_format_currency_minus, discount, currency),
//        )
//    }
//
//    Spacer(Modifier.height(8.dp))
//
//    // Podsumowanie końcowe
//    InfoRow(
//        label = stringResource(R.string.summary_total_basket),
//        value = stringResource(R.string.price_format_currency, basketTotal, currency),
//        isBold = true,
//    )
//    InfoRow(
//        label = stringResource(R.string.summary_total_discount),
//        value = stringResource(R.string.price_format_currency_minus, discountTotal, currency),
//        isBold = true,
//    )
//    InfoRow(
//        label = stringResource(R.string.summary_total),
//        value = stringResource(R.string.price_format_currency, grandTotal, currency),
//        isBold = true,
//    )
//}
//
//@Composable
//private fun ProductListItem(product: OrderProduct) {
//    // ✅ POPRAWKA: Obliczenie sumy dla linii
//    val lineTotal = product.price * product.quantity
//
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 2.dp),
//        horizontalArrangement = Arrangement.SpaceBetween,
//    ) {
//        Column(modifier = Modifier.weight(1f)) {
//            Text(
//                text = "${product.quantity}x ${product.name}",
//                style = MaterialTheme.typography.bodyMedium,
//            )
//
//            val c = product.comment?.trim()
//            if (!c.isNullOrEmpty()) {
//                Text(
//                    text = buildAnnotatedString {
//                        withStyle(
//                            SpanStyle(textDecoration = TextDecoration.Underline),
//                        ) { append(c) }
//                    },
//                    style = MaterialTheme.typography.bodySmall,
//                    modifier = Modifier.padding(top = 2.dp),
//                )
//            }
//        }
//
//        Text(
//            text = stringResource(R.string.price_format_currency, lineTotal, AppPrefs.getCurrency()),
//            style = MaterialTheme.typography.bodyMedium,
//            textAlign = TextAlign.End,
//        )
//    }
//}
//
//
//@OptIn(ExperimentalLayoutApi::class)
//@Composable
//private fun ActionsSection(
//    order: Order,
//    role: UserRole?,
//    userId: String?,
//    onClose: () -> Unit,
//    callbacks: Callbacks,
//) {
//    val latestOnStatusChange by rememberUpdatedState(callbacks.onStatusChange ?: {})
//    val latestOnCourierChange by rememberUpdatedState(callbacks.onCourierChange ?: {})
//    val latestOnClose by rememberUpdatedState(onClose)
//    val latestOnSendExternalCourier by rememberUpdatedState(callbacks.onSendExternalCourier ?: { _, _, _, _ -> })
//    val latestOnCancelExternalCourier by rememberUpdatedState(callbacks.onCancelExternalCourier ?: {})
//    val scope = rememberCoroutineScope()
//    val context = LocalContext.current
//
//    // Stan dla dialogu wyboru czasu przygotowania. Scoped to order.orderId so it resets
//    var showPreparationTimeDialog by remember(order.orderId) { mutableStateOf(false) }
//    var selectedCourier by remember(order.orderId) { mutableStateOf<DeliveryEnum?>(null) }
//    var courierDisplayName by remember(order.orderId) { mutableStateOf("") }
//
//    // Dialog wyboru czasu przygotowania
//    if (showPreparationTimeDialog && selectedCourier != null) {
//        PreparationTimeDialog(
//            courierName = courierDisplayName,
//            orderId = order.orderId,
//            isAsap = order.isAsap,
//            deliveryTimeIso = order.deliveryTime,
//            onDismiss = {
//                showPreparationTimeDialog = false
//                selectedCourier = null
//            },
//            onConfirm = { timePrepare, timeDelivery ->
//                scope.launch {
//                    // Zatrzymaj alarm PRZED wszystkim, aby uniknąć restartu dźwięku
//                    context.startService(Intent(context, OrderAlarmService::class.java).apply {
//                        action = OrderAlarmService.ACTION_STOP_ALARM
//                    })
//                    selectedCourier?.let { courier ->
//                        latestOnSendExternalCourier(order, courier, timePrepare, timeDelivery)
//                    }
//                    showPreparationTimeDialog = false
//                    selectedCourier = null
//                    latestOnClose()
//                }
//            },
//        )
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(16.dp),
//    ) {
//        if (role == UserRole.COURIER) {
//            // --- Logika dla kuriera ---
//            when {
//                order.courier?.id.isNullOrBlank() -> Button(
//                    onClick = {
//                        scope.launch {
//                            // Zatrzymaj alarm PRZED wszystkim
//                            context.startService(Intent(context, OrderAlarmService::class.java).apply {
//                                action = OrderAlarmService.ACTION_STOP_ALARM
//                            })
//                            latestOnCourierChange(true)
//                            latestOnClose()
//                        }
//                    },
//                    modifier = Modifier.fillMaxWidth(),
//                ) { Text(stringResource(R.string.action_take_order)) }
//
//                order.courier?.id == userId -> Button(
//                    onClick = {
//                        scope.launch {
//                            // Zatrzymaj alarm PRZED wszystkim
//                            context.startService(Intent(context, OrderAlarmService::class.java).apply {
//                                action = OrderAlarmService.ACTION_STOP_ALARM
//                            })
//                            latestOnCourierChange(false)
//                            latestOnClose()
//                        }
//                    },
//                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
//                    modifier = Modifier.fillMaxWidth(),
//                ) { Text(stringResource(R.string.action_resign)) }
//            }
//            if (order.courier?.id == userId) {
//                val slugEnum = order.orderStatus.slug?.let {
//                    runCatching { OrderStatusEnum.valueOf(it) }.getOrNull()
//                } ?: OrderStatusEnum.UNKNOWN
//
//                when (slugEnum) {
//                    OrderStatusEnum.ACCEPTED -> Button(
//                        onClick = {
//                            scope.launch {
//                                // Zatrzymaj alarm PRZED wszystkim
//                                context.startService(Intent(context, OrderAlarmService::class.java).apply {
//                                    action = OrderAlarmService.ACTION_STOP_ALARM
//                                })
//                                latestOnStatusChange(OrderStatusEnum.OUT_FOR_DELIVERY)
//                                latestOnClose()
//                            }
//                        },
//                        enabled = slugEnum != OrderStatusEnum.OUT_FOR_DELIVERY,
//                        modifier = Modifier.fillMaxWidth(),
//                    ) { Text(stringResource(R.string.action_out_for_delivery)) }
//
//                    OrderStatusEnum.OUT_FOR_DELIVERY -> Button(
//                        onClick = {
//                            scope.launch {
//                                // Zatrzymaj alarm PRZED wszystkim
//                                context.startService(Intent(context, OrderAlarmService::class.java).apply {
//                                    action = OrderAlarmService.ACTION_STOP_ALARM
//                                })
//                                latestOnStatusChange(OrderStatusEnum.COMPLETED)
//                                latestOnClose()
//                            }
//                        },
//                        enabled = slugEnum != OrderStatusEnum.COMPLETED,
//                        modifier = Modifier.fillMaxWidth(),
//                    ) { Text(stringResource(R.string.action_delivered)) }
//
//                    else -> Unit
//                }
//            }
//        } else {
//            // --- Logika dla Staff ---
//            val slugEnum = order.orderStatus.slug?.let {
//                runCatching { OrderStatusEnum.valueOf(it) }.getOrNull()
//            } ?: OrderStatusEnum.UNKNOWN
//
//            when (slugEnum) {
//                OrderStatusEnum.PROCESSING -> ProcessingTimeSelection(
//                    order = order,
//                    onClose = latestOnClose,
//                    callbacks = callbacks,
//                )
//
//                OrderStatusEnum.PENDING -> {
//                    Text(
//                        text = stringResource(R.string.order_awaiting_payment_info),
//                        style = MaterialTheme.typography.bodyMedium,
//                        fontWeight = FontWeight.Bold,
//                        color = MaterialTheme.colorScheme.primary,
//                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
//                    )
//                }
//
//                else -> {
//                    val isDelivery = order.deliveryType == OrderDelivery.DELIVERY ||
//                        order.deliveryType == OrderDelivery.DELIVERY_EXTERNAL
//                    val noInternalCourierAssigned = order.courier?.id.isNullOrBlank()
//
//                    if (noInternalCourierAssigned) {
//                        ActionGroup(title = stringResource(R.string.action_group_status)) {
//                            val allowed = mutableSetOf(
//                                OrderStatusEnum.OUT_FOR_DELIVERY,
//                                OrderStatusEnum.COMPLETED,
//                            )
//                            if (order.deliveryType == OrderDelivery.PICKUP || order.deliveryType == OrderDelivery.PICK_UP) {
//                                allowed.remove(OrderStatusEnum.OUT_FOR_DELIVERY)
//                            }
//
//                            FlowRow(
//                                modifier = Modifier.fillMaxWidth(),
//                                horizontalArrangement = Arrangement.spacedBy(
//                                    8.dp,
//                                    Alignment.CenterHorizontally,
//                                ),
//                                verticalArrangement = Arrangement.spacedBy(8.dp),
//                            ) {
//                                if (OrderStatusEnum.OUT_FOR_DELIVERY in allowed) {
//                                    Button(
//                                        onClick = {
//                                            scope.launch {
//                                                // Zatrzymaj alarm PRZED wszystkim
//                                                context.startService(Intent(context, OrderAlarmService::class.java).apply {
//                                                    action = OrderAlarmService.ACTION_STOP_ALARM
//                                                })
//                                                latestOnStatusChange(OrderStatusEnum.OUT_FOR_DELIVERY)
//                                                latestOnClose()
//                                            }
//                                        },
//                                        enabled = slugEnum != OrderStatusEnum.OUT_FOR_DELIVERY,
//                                    ) { Text(stringResource(R.string.action_out_for_delivery)) }
//                                }
//                                if (OrderStatusEnum.COMPLETED in allowed) {
//                                    Button(
//                                        onClick = {
//                                            scope.launch {
//                                                // Zatrzymaj alarm PRZED wszystkim
//                                                context.startService(Intent(context, OrderAlarmService::class.java).apply {
//                                                    action = OrderAlarmService.ACTION_STOP_ALARM
//                                                })
//                                                latestOnStatusChange(OrderStatusEnum.COMPLETED)
//                                                latestOnClose()
//                                            }
//                                        },
//                                        enabled = slugEnum != OrderStatusEnum.COMPLETED,
//                                    ) { Text(stringResource(R.string.action_completed)) }
//                                }
//                            }
//                        }
//                    }
//
//                    if (slugEnum == OrderStatusEnum.ACCEPTED && isDelivery && noInternalCourierAssigned) {
//                        // Sprawdzenie czy external courier już został przydzielony
//                        val hasActiveExternalCourier = order.externalDelivery?.let { delivery ->
//                            delivery.status != null && delivery.status in listOf(
//                                DeliveryStatusEnum.REQUESTED,
//                                DeliveryStatusEnum.ACCEPTED,
//                                DeliveryStatusEnum.IN_TRANSIT,
//                                DeliveryStatusEnum.DELIVERED,
//                            )
//                        } ?: false
//
//                        if (hasActiveExternalCourier) {
//                            // Kurierz już przydzielony - wyświetl informację
//                            ActionGroup(title = stringResource(R.string.action_group_delivery)) {
//                                Card(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .padding(8.dp),
//                                    colors = CardDefaults.cardColors(
//                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
//                                    ),
//                                ) {
//                                    Column(
//                                        modifier = Modifier
//                                            .fillMaxWidth()
//                                            .padding(16.dp),
//                                        verticalArrangement = Arrangement.spacedBy(12.dp),
//                                    ) {
//                                        Row(
//                                            modifier = Modifier.fillMaxWidth(),
//                                            horizontalArrangement = Arrangement.SpaceBetween,
//                                            verticalAlignment = Alignment.CenterVertically,
//                                        ) {
//                                            Text(
//                                                text = "🚚 ${stringResource(R.string.courier_already_assigned)}",
//                                                style = MaterialTheme.typography.bodyMedium,
//                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
//                                                modifier = Modifier.weight(1f),
//                                            )
//                                            OutlinedButton(
//                                                onClick = {
//                                                    scope.launch {
//                                                        // Zatrzymaj alarm PRZED wszystkim
//                                                        context.startService(Intent(context, OrderAlarmService::class.java).apply {
//                                                            action = OrderAlarmService.ACTION_STOP_ALARM
//                                                        })
//                                                        latestOnCancelExternalCourier(order)
//                                                        latestOnClose()
//                                                    }
//                                                },
//                                                modifier = Modifier.padding(start = 8.dp),
//                                            ) {
//                                                Text(
//                                                    stringResource(R.string.btn_cancel_external_courier),
//                                                    style = MaterialTheme.typography.labelSmall,
//                                                )
//                                            }
//                                        }
//                                        Text(
//                                            text = order.externalDelivery?.courier?.name
//                                                ?: stringResource(R.string.unknown_courier),
//                                            style = MaterialTheme.typography.labelLarge,
//                                            fontWeight = FontWeight.Bold,
//                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
//                                        )
//                                    }
//                                }
//                            }
//                        } else {
//                            // Brak kuriera - wyświetl przyciski do wyboru
//                            // Sprawdź które integracje są aktywne
//                            val isStavaActive = remember { AppPrefs.getStavaIntegrationActive() }
//                            val isWoltActive = remember { AppPrefs.getWoltDriveIntegrationActive() }
//                            val isStuartActive = remember { AppPrefs.getStuartIntegrationActive() }
//
//                            // Wyświetl przyciski tylko jeśli przynajmniej jedna integracja jest aktywna
//                            if (isStavaActive || isWoltActive || isStuartActive) {
//                                ActionGroup(title = stringResource(R.string.action_group_delivery)) {
//                                    FlowRow(
//                                        modifier = Modifier.fillMaxWidth(),
//                                        horizontalArrangement = Arrangement.spacedBy(
//                                            8.dp,
//                                            Alignment.CenterHorizontally,
//                                        ),
//                                        verticalArrangement = Arrangement.spacedBy(8.dp),
//                                    ) {
//                                        // Stava - wyświetl tylko jeśli aktywna
//                                        if (isStavaActive) {
//                                            Button(
//                                                onClick = {
//                                                    selectedCourier = DeliveryEnum.STAVA
//                                                    courierDisplayName = "Stava"
//                                                    showPreparationTimeDialog = true
//                                                },
//                                            ) {
//                                                Icon(
//                                                    painterResource(id = R.drawable.stava),
//                                                    contentDescription = null,
//                                                    modifier = Modifier.size(ButtonDefaults.IconSize),
//                                                )
//                                                Spacer(Modifier.width(8.dp))
//                                                Text(stringResource(R.string.action_send_to_stava))
//                                            }
//                                        }
//
//                                        // Stuart - wyświetl tylko jeśli aktywna
//                                        if (isStuartActive) {
//                                            Button(
//                                                onClick = {
//                                                    selectedCourier = DeliveryEnum.STUART
//                                                    courierDisplayName = "Stuart"
//                                                    showPreparationTimeDialog = true
//                                                },
//                                            ) {
//                                                Icon(
//                                                    painterResource(id = R.drawable.stuart),
//                                                    contentDescription = null,
//                                                    modifier = Modifier.size(ButtonDefaults.IconSize),
//                                                )
//                                                Spacer(Modifier.width(8.dp))
//                                                Text(stringResource(R.string.action_send_to_stuart))
//                                            }
//                                        }
//
//                                        // Wolt - wyświetl tylko jeśli aktywna
//                                        if (isWoltActive) {
//                                            Button(
//                                                onClick = {
//                                                    selectedCourier = DeliveryEnum.WOLT
//                                                    courierDisplayName = "Wolt"
//                                                    showPreparationTimeDialog = true
//                                                },
//                                            ) {
//                                                Icon(
//                                                    painterResource(id = R.drawable.logo_wolt_80),
//                                                    contentDescription = null,
//                                                    modifier = Modifier.size(ButtonDefaults.IconSize),
//                                                )
//                                                Spacer(Modifier.width(8.dp))
//                                                Text(stringResource(R.string.action_send_to_wolt))
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
///**
// * Nagłówek i separator dla grupy akcji
// */
//@Composable
//private fun ActionGroup(
//    title: String,
//    content: @Composable ColumnScope.() -> Unit,
//) {
//    Column(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalAlignment = Alignment.CenterHorizontally,
//    ) {
//        Text(
//            text = title,
//            style = MaterialTheme.typography.titleSmall,
//            fontWeight = FontWeight.Bold,
//            color = MaterialTheme.colorScheme.primary,
//        )
//        HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
//        content()
//    }
//}
//
//@OptIn(ExperimentalLayoutApi::class)
//@Composable
//private fun ProcessingTimeSelection(
//    order: Order,
//    onClose: () -> Unit,
//    callbacks: Callbacks,
//) {
//    val latestOnClose by rememberUpdatedState(onClose)
//    val latestOnTimeSelect by rememberUpdatedState(callbacks.onTimeSelected ?: {})
//    val scope = rememberCoroutineScope()
//    val context = LocalContext.current
//
//    if (order.isAsap) {
//        val presetMinutes = remember { listOf(15, 30, 45, 60, 90) }
//        FlowRow(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 16.dp),
//            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
//            verticalArrangement = Arrangement.spacedBy(8.dp),
//        ) {
//            presetMinutes.forEach { minutes ->
//                Button(
//                    onClick = {
//                        scope.launch {
//                            // Zatrzymaj alarm PRZED wszystkim
//                            context.startService(Intent(context, OrderAlarmService::class.java).apply {
//                                action = OrderAlarmService.ACTION_STOP_ALARM
//                            })
//                            Timber.tag("DUPSKO")
//                                .d("PrintDebug Button +${minutes} min clicked. Wywołuję onTimeSelect i onPrintOrder")
//                            val newTime = ZonedDateTime.now().plusMinutes(minutes.toLong())
//                            Timber.tag("DUPSKO")
//                                .d("PrintDebug TIME +${DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(newTime)} min clicked. Wywołuję onTimeSelect i onPrintOrder")
//
//                            latestOnTimeSelect(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(newTime))
//                            // ✅ Drukowanie odbyw się w printAfterOrderAccepted (ViewModel)
//                            // latestOnPrint(order) - usunięte aby uniknąć duplikatów
//                            latestOnClose()
//                        }
//                    },
//                ) {
//                    Text(stringResource(R.string.add_minutes_format, minutes))
//                }
//            }
//        }
//    } else {
//        DateTimePickers(
//            order = order,
//            onClose = latestOnClose,
//            callbacks = callbacks,
//        )
//    }
//}
//
//@Composable
//private fun DateTimePickers(
//    order: Order,
//    onClose: () -> Unit,
//    callbacks: Callbacks,
//) {
//    val context = LocalContext.current
//    val initialInterval = order.deliveryInterval
//    val initialDateTime = remember(order.orderId, initialInterval) {
//        DateTimeUtils.parseToZonedDateTime(initialInterval)
//            ?.withZoneSameInstant(ZoneId
//                .systemDefault())
//            ?.toLocalDateTime()
//            ?: run {
//                Timber.d("initialInterval parse failed or null, falling back to now: %s", initialInterval)
//                LocalDateTime.now()
//            }
//    }
//    var selectedDate by remember { mutableStateOf(initialDateTime.toLocalDate()) }
//    var selectedTime by remember { mutableStateOf(initialDateTime.toLocalTime()) }
//    val datePickerDialog = DatePickerDialog(
//        context,
//        { _, year, month, dayOfMonth ->
//            selectedDate = LocalDateTime.of(year, month + 1, dayOfMonth, 0, 0).toLocalDate()
//        },
//        selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth,
//    )
//    val timePickerDialog = TimePickerDialog(
//        context,
//        { _, hourOfDay, minute ->
//            selectedTime = LocalDateTime.of(1, 1, 1, hourOfDay, minute).toLocalTime()
//        },
//        selectedTime.hour, selectedTime.minute, true,
//    )
//    val latestOnClose by rememberUpdatedState(onClose)
//    val latestOnTimeSelect by rememberUpdatedState(callbacks.onTimeSelected ?: {})
//    val scope = rememberCoroutineScope()
//
//    Column(
//        modifier = Modifier.fillMaxWidth(),
//        verticalArrangement = Arrangement.spacedBy(8.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//    ) {
//        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//            OutlinedButton(
//                onClick = { datePickerDialog.show() },
//                modifier = Modifier.weight(1f),
//            ) {
//                Text(
//                    selectedDate.format(
//                        DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()),
//                    ),
//                )
//            }
//            OutlinedButton(
//                onClick = { timePickerDialog.show() },
//                modifier = Modifier.weight(1f),
//            ) {
//                Text(
//                    selectedTime.format(
//                        DateTimeFormatter.ofPattern("HH:mm"),
//                    ),
//                )
//            }
//        }
//        Button(
//            onClick = {
//                scope.launch {
//                    // Zatrzymaj alarm PRZED wszystkim
//                    context.startService(Intent(context, OrderAlarmService::class.java).apply {
//                        action = OrderAlarmService.ACTION_STOP_ALARM
//                    })
//                    val finalDateTime = ZonedDateTime.of(selectedDate, selectedTime, ZoneId.systemDefault())
//                    latestOnTimeSelect(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(finalDateTime))
//                    // ✅ Drukowanie odbyw się w printAfterOrderAccepted (ViewModel)
//                    // latestOnPrint(order) - usunięte aby uniknąć duplikatów
//                    latestOnClose()
//                }
//            },
//            modifier = Modifier.fillMaxWidth(),
//        ) {
//            Text(stringResource(R.string.action_confirm_time))
//        }
//    }
//}
//
//
//@Composable
//private fun SourceIconOrSpace(source: SourceEnum?, modifier: Modifier = Modifier) {
//    val painter = when (source) {
//        SourceEnum.UBER -> painterResource(id = R.drawable.ic_uber)
//        SourceEnum.GLOVO -> painterResource(id = R.drawable.logo_glovo_80)
//        SourceEnum.WOLT -> painterResource(id = R.drawable.logo_wolt_80)
//        SourceEnum.BOLT -> painterResource(id = R.drawable.logo_bolt_80)
//        SourceEnum.TAKEAWAY -> painterResource(id = R.drawable.ic_takeaway)
//        SourceEnum.WOOCOMMERCE, SourceEnum.WOO -> painterResource(id = R.drawable.ic_woo)
//        SourceEnum.GOPOS -> painterResource(id = R.drawable.ic_gopos)
//        null, SourceEnum.OTHER, SourceEnum.UNKNOWN, SourceEnum.ITS -> painterResource(id = R.drawable.logo)
//    }
//    Icon(
//        painter = painter,
//        contentDescription = stringResource(
//            R.string.source_cd,
//            source?.name ?: stringResource(R.string.dash),
//        ),
//        tint = Color.Unspecified,
//        modifier = modifier,
//    )
//}
