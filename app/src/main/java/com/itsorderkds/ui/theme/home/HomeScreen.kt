package com.itsorderkds.ui.theme.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itsorderkds.data.model.UpdateCourierOrder
import com.itsorderkds.data.model.UpdateOrderData
import com.itsorderkds.data.responses.UserRole
//import com.itsorderkds.ui.dialog.AcceptOrderDialog
import com.itsorderkds.ui.order.OrderStatusEnum
import com.itsorderkds.ui.order.OrdersViewModel
import com.itsorderkds.ui.theme.home.components.AssignmentPrompt
import com.itsorderkds.ui.theme.home.view.CourierView
import com.itsorderkds.ui.theme.home.view.StaffView

// ✅ DODANE IMPORTY
import androidx.compose.ui.res.stringResource
import com.itsorderkds.R
import com.itsorderkds.ui.dialog.AcceptOrderSheetContent
import com.itsorderkds.ui.order.Callbacks
import timber.log.Timber
import androidx.compose.runtime.key

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: OrdersViewModel,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val allOrders by viewModel.orders.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedOrderIds.collectAsStateWithLifecycle()
//    val newOrders by viewModel.newOrdersList.collectAsStateWithLifecycle()
//    val acceptedOrders by viewModel.acceptedOrdersList.collectAsStateWithLifecycle()
    val activeOrders by viewModel.activeOrdersList.collectAsStateWithLifecycle()
    val completedOrders by viewModel.completedOrdersList.collectAsStateWithLifecycle()
    val dineInOrders by viewModel.dineInOrdersList.collectAsStateWithLifecycle()
    val groupedOrders by viewModel.groupedOrdersMap.collectAsStateWithLifecycle()
    // Jeśli pokazujemy prompt i lista pusta – dociągnij pojazdy
    LaunchedEffect(uiState.showAssignmentPrompt) {
        if (uiState.userRole == UserRole.COURIER &&
            uiState.showAssignmentPrompt &&
            !uiState.isFetchingVehicles &&
            vehicles.isEmpty()
        ) {
            viewModel.fetchAvailableVehicles()
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (uiState.userRole) {
            UserRole.COURIER -> {
                // Bez dodatkowej animacji (Loader->App już wygładza przejście)
                androidx.compose.animation.Crossfade(
                    targetState = uiState.showAssignmentPrompt,
                    animationSpec = androidx.compose.animation.core.tween(0),
                    label = "CourierViewSwitch"
                ) { showPrompt ->
                    if (showPrompt) {
                        AssignmentPrompt(
                            vehicles = vehicles,
                            isLoading = uiState.isFetchingVehicles,
                            onStartShiftWithVehicle = viewModel::checkIn
                        )
                    } else {
                        CourierView(
                            viewModel = viewModel,
                            selectedOrderIds = selectedIds,
                            isSelectionModeActive = selectedIds.isNotEmpty(),
                            onSelectionChange = viewModel::updateSelectedOrders
                        )
                    }
                }
            }

            else -> {
                StaffView(
//                    newOrders = newOrders,
//                    acceptedOrders = acceptedOrders,
                    activeOrders = activeOrders,
                    completedOrders = completedOrders,
                    dineInOrders = dineInOrders,
                    groupedOrders = groupedOrders,
//                    allOrders = allOrders,
                    onOrderClick = { viewModel.triggerOpenDialog(it) },
                    isRefreshing = uiState.isGlobalLoading,
                    onRefresh = { viewModel.syncOrdersFromApiStartOfDay() },
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = onTabSelected
                )
            }
        }
    }

    // ✅ Dialog przeniesiony tutaj – działa dla STAFF
    val orderToShow = uiState.orderToShowInDialog
    if (orderToShow != null && uiState.userRole != UserRole.COURIER) {
        Dialog(
            onDismissRequest = { viewModel.dismissDialog() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false
            )
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        // ✅ TŁUMACZENIE
                        title = { Text(stringResource(R.string.order_details)) },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.dismissDialog() }) {
                                Icon(
                                    Icons.Default.Close,
                                    // ✅ TŁUMACZENIE
                                    contentDescription = stringResource(R.string.common_close)
                                )
                            }
                        },
                        actions = {
                            // Kolorowy przycisk drukarki - bardziej widoczny
                            FilledTonalIconButton(
                                onClick = { viewModel.printOrder(orderToShow) },
                                enabled = !uiState.isPrinting,  // Zablokuj podczas drukowania
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                if (uiState.isPrinting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = stringResource(R.string.action_print)
                                    )
                                }
                            }
                        }
                    )
                }
            ) { padding ->
                // Ensure dialog content is recreated for a different order
                key(orderToShow.orderId) {
                    AcceptOrderSheetContent(
                        modifier = Modifier.padding(padding),
                        order = orderToShow,
                        userRole = uiState.userRole,
                        userId = uiState.userId,
                        onClose = { viewModel.dismissDialog() },
                        callbacks = Callbacks(
                            onTimeSelected = { time ->
                                Timber.tag("DUPSKO").d("PrintDebug Button +${time} min clicked")
                                viewModel.updateOrder(
                                    orderToShow.orderId,
                                    OrderStatusEnum.ACCEPTED,
                                    UpdateOrderData(deliveryTime = time)
                                )
                            },
                            onStatusChange = { newStatus ->
                                viewModel.updateOrder(orderToShow.orderId, newStatus, UpdateOrderData())
                            },
                            onCourierChange = { assign ->
                                val courierId = if (assign) uiState.userId.orEmpty() else ""
                                viewModel.updateOrderCourier(
                                    orderToShow.orderId,
                                    UpdateCourierOrder(courierId)
                                )
                            },
                            onPrintOrder = viewModel::printOrder,
                            onSendExternalCourier = { order, courierDelivery, timePrepare, timeDelivery ->
                                viewModel.sendToExternalApi(order, courierDelivery, timePrepare, timeDelivery)
                            },
                            onCancelExternalCourier = { order ->
                                viewModel.cancelExternalCourier(order)
                            },
                            isPrinting = uiState.isPrinting  // Przekaż flagę drukowania
                        )
                    )
                }
            }
        }
    }

    // Dialog wyboru drukarki
    if (uiState.showPrinterSelectionDialog && uiState.selectedOrderForPrinting != null) {
        PrinterSelectionDialog(
            onPrinterSelected = { printerIndex ->
                viewModel.printOrderOnSelectedPrinter(uiState.selectedOrderForPrinting!!, printerIndex)
            },
            onDismiss = {
                viewModel.dismissPrinterSelectionDialog()
            }
        )
    }
}

// --- Komponent Dialog wyboru drukarki ---
@Composable
fun PrinterSelectionDialog(
    onPrinterSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val printers = com.itsorderkds.data.preferences.PrinterPreferences.getPrinters(context)
    val enabledPrinters = printers.filter { it.enabled && it.deviceId.isNotBlank() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🖨️ Wybierz drukarkę")
        },
        text = {
            LazyColumn {
                // ✅ NOWE: Opcja "Wszystkie drukarki" (tylko gdy jest więcej niż 1)
                if (enabledPrinters.size > 1) {
                    item {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "📄 Wszystkie drukarki",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            supportingContent = {
                                Text("Wydrukuj na ${enabledPrinters.size} drukarkach")
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable {
                                onPrinterSelected(-1) // -1 = wszystkie drukarki
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                // Lista poszczególnych drukarek
                items(enabledPrinters) { printer ->
                    val index = enabledPrinters.indexOf(printer)
                    ListItem(
                        headlineContent = {
                            Text(printer.name)
                        },
                        supportingContent = {
                            Text(
                                when (printer.connectionType) {
                                    com.itsorderkds.data.model.PrinterConnectionType.BLUETOOTH -> "Bluetooth"
                                    com.itsorderkds.data.model.PrinterConnectionType.NETWORK -> "Sieć"
                                    com.itsorderkds.data.model.PrinterConnectionType.BUILTIN -> "Wbudowana"
                                }
                            )
                        },
                        modifier = Modifier.clickable {
                            onPrinterSelected(index)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}
