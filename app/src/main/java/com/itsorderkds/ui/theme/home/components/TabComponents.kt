//package com.itsorderkds.ui.theme.home.components
//
//
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.LazyListState
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.ExperimentalMaterialApi
//import androidx.compose.material.pullrefresh.PullRefreshIndicator
//import androidx.compose.material.pullrefresh.pullRefresh
//import androidx.compose.material.pullrefresh.rememberPullRefreshState
//import androidx.compose.material3.Badge
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import com.google.android.gms.maps.CameraUpdateFactory
//import com.google.android.gms.maps.model.CameraPosition
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.gms.maps.model.LatLngBounds
//import com.google.maps.android.compose.GoogleMap
//import com.google.maps.android.compose.MapProperties
//import com.google.maps.android.compose.MapUiSettings
//import com.google.maps.android.compose.Marker
//import com.google.maps.android.compose.MarkerState
//import com.google.maps.android.compose.Polyline
//import com.google.maps.android.compose.rememberCameraPositionState
//import com.itsorderkds.data.model.Order
//import com.itsorderkds.data.model.OrderTras
////import com.itsorderkds.ui.order.OrderRouteState
//import com.itsorderkds.ui.order.OrderStatusEnum
//import com.itsorderkds.ui.order.RouteCluster
//
//@Composable
//fun TabsChipsRow(
//    tabs: List<String>,
//    selectedTabIndex: Int,
//    onSelect: (Int) -> Unit,
//    modifier: Modifier = Modifier,
//    badgeCounts: List<Int>? = null // Opcjonalna lista z liczbami do plakietek
//) {
//    val listState = rememberLazyListState()
//    // Automatyczne przewijanie do aktywnej zakładki
//    LaunchedEffect(selectedTabIndex) {
//        listState.animateScrollToItem(selectedTabIndex)
//    }
//
//    LazyRow(
//        state = listState,
//        modifier = modifier
//            .fillMaxWidth()
//            .padding(horizontal = 12.dp, vertical = 6.dp),
//        horizontalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        itemsIndexed(tabs) { index, label ->
//            val badgeCount = badgeCounts?.getOrNull(index)
//            TabChip(
//                label = label,
//                selected = index == selectedTabIndex,
//                onClick = { onSelect(index) },
//                badgeCount = badgeCount
//            )
//        }
//    }
//}
//
//@Composable
//private fun TabChip(
//    label: String,
//    selected: Boolean,
//    onClick: () -> Unit,
//    badgeCount: Int? = null
//) {
//    val backgroundColor =
//        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
//    val contentColor =
//        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
//
//    Surface(
//        onClick = onClick,
//        shape = RoundedCornerShape(50),
//        color = backgroundColor,
//        contentColor = contentColor,
//        tonalElevation = if (selected) 3.dp else 0.dp
//    ) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
//        ) {
//            Text(
//                text = label,
//                style = MaterialTheme.typography.labelLarge,
//                fontWeight = FontWeight.Medium
//            )
//            if (badgeCount != null && badgeCount > 0) {
//                Spacer(Modifier.width(6.dp))
//                Badge(
//                    containerColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
//                    contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
//                ) {
//                    Text(badgeCount.toString())
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun RouteOutForDeliveryTabContent(
//    modifier: Modifier = Modifier,
//    listState: LazyListState,
//    routeState: OrderRouteState,
//    onRefresh: () -> Unit,
//    selectedOrderIds: Set<String>,
//    isSelectionModeActive: Boolean,
//    onSelectionChange: (Set<String>, OrderStatusEnum?) -> Unit,
//    isOrderLocked: (String) -> Boolean
//) {
//    // przefiltruj tylko przystanki ze statusem OUT_FOR_DELIVERY
//    val filteredState = when (routeState) {
//        is OrderRouteState.Success -> {
//            val filtered = routeState.route.filter { it.status == OrderStatusEnum.OUT_FOR_DELIVERY }
//            OrderRouteState.Success(filtered)
//        }
//
//        is OrderRouteState.Error -> routeState
//        OrderRouteState.Loading -> routeState
//        else -> {}
//    }
//
//    RouteTabContent(
//        modifier = modifier,
//        listState = listState,
//        routeState = filteredState,
//        onRefresh = onRefresh,
//        selectedOrderIds = selectedOrderIds,
//        isSelectionModeActive = isSelectionModeActive,
//        onSelectionChange = onSelectionChange,
//        isOrderLocked = isOrderLocked
//    )
//}
//
//@OptIn(ExperimentalMaterialApi::class)
//@Composable
//fun RouteTabContent(
//    modifier: Modifier = Modifier,
//    listState: LazyListState,
//    routeState: Any,
//    onRefresh: () -> Unit,
//    selectedOrderIds: Set<String>,
//    isSelectionModeActive: Boolean,
//    onSelectionChange: (Set<String>, OrderStatusEnum?) -> Unit,
//    isOrderLocked: (String) -> Boolean
//) {
//
//    val isRefreshing = routeState is OrderRouteState.Loading
//    val pullRefreshState =
//        rememberPullRefreshState(refreshing = isRefreshing, onRefresh = onRefresh)
//
//    Box(
//        modifier = modifier
//            .fillMaxSize()
//            .pullRefresh(pullRefreshState)
//    ) {
//        when (routeState) {
//            is OrderRouteState.Success -> {
//                if (routeState.route.isEmpty()) {
//                    EmptyPlaceholder()
//                } else {
//                    LazyColumn(
//                        state = listState,
//                        modifier = Modifier.fillMaxSize(),
//                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
//                    ) {
//                        itemsIndexed(
//                            routeState.route,
//                            key = { _, item -> item.id }) { index, routeItem ->
//                            RouteListRow(
//                                item = routeItem,
//                                isSelected = routeItem.orderId in selectedOrderIds,
//                                isFirst = index == 0,
//                                isLast = index == routeState.route.lastIndex,
//                                isSelectionModeActive = isSelectionModeActive,
//                                onClick = {
//                                    if (routeItem.orderNumber.equals(
//                                            "RETURN",
//                                            ignoreCase = true
//                                        )
//                                    ) return@RouteListRow
//
//                                    val currentStatus = routeState.route
//                                        .find { it.orderId == selectedOrderIds.firstOrNull() }?.status
//                                    val clickedStatus = routeItem.status
//
//                                    if (!isSelectionModeActive) {
//                                        onSelectionChange(setOf(routeItem.orderId), clickedStatus)
//                                    } else if (currentStatus == null || clickedStatus == currentStatus) {
//                                        val newSelection =
//                                            if (routeItem.orderId in selectedOrderIds)
//                                                selectedOrderIds - routeItem.orderId
//                                            else
//                                                selectedOrderIds + routeItem.orderId
//                                        val finalStatus = newSelection.takeIf { it.isNotEmpty() }
//                                            ?.let { clickedStatus }
//                                        onSelectionChange(newSelection, finalStatus)
//                                    } else {
//                                        onSelectionChange(setOf(routeItem.orderId), clickedStatus)
//                                    }
//                                },
//                                isOrderLocked = isOrderLocked
//                            )
//                        }
//                    }
//                }
//            }
//
//            is OrderRouteState.Error -> {
//                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    Text("Błąd ładowania trasy.")
//                }
//            }
//
//            OrderRouteState.Loading -> {
//                // Można dodać wskaźnik ładowania tutaj
//            }
//
//            else -> {}
//        }
//        PullRefreshIndicator(
//            refreshing = isRefreshing,
//            state = pullRefreshState,
//            modifier = Modifier.align(Alignment.TopCenter)
//        )
//    }
//}
//
//@OptIn(ExperimentalMaterialApi::class)
//@Composable
//fun AcceptedOrdersTabContent(
//    modifier: Modifier = Modifier,
//    listState: LazyListState,
//    allOrders: List<Order>,
//    onOrderClick: (Order) -> Unit,
//    isRefreshing: Boolean,
//    onRefresh: () -> Unit,
//) {
//    val acceptedOrders =
//        remember(allOrders) {
//            allOrders.filter {
//                val slugEnum = it.orderStatus.slug?.let { slug ->
//                    runCatching { OrderStatusEnum.valueOf(slug) }.getOrNull()
//                }
//                slugEnum == OrderStatusEnum.ACCEPTED
//            }
//        }
//    val pullRefreshState =
//        rememberPullRefreshState(refreshing = isRefreshing, onRefresh = onRefresh)
//
//    Box(
//        modifier = modifier
//            .fillMaxSize()
//            .pullRefresh(pullRefreshState)
//    ) {
//        if (acceptedOrders.isEmpty() && !isRefreshing) {
//            EmptyPlaceholder()
//        } else {
//            LazyColumn(
//                state = listState,
//                modifier = Modifier.fillMaxSize(),
//                contentPadding = PaddingValues(16.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                items(acceptedOrders, key = { it.orderId }) { order ->
//                    OrderListItem(order, onClick = { onOrderClick(order) })
//                }
//            }
//        }
//        PullRefreshIndicator(
//            refreshing = isRefreshing,
//            state = pullRefreshState,
//            modifier = Modifier.align(Alignment.TopCenter)
//        )
//    }
//}
//
//@Composable
//fun MapOrdersTabContent(
//    modifier: Modifier = Modifier,
//    // Pełna lista przystanków, potrzebna do filtrowania
//    allStops: List<OrderTras>,
//    // Widoczne klastry (wszystkie lub jeden wybrany)
//    visibleClusters: List<RouteCluster>,
//    // Dane do przełącznika
//    allClustersCount: Int,
//    selectedClusterIndex: Int?,
//    onClusterSelected: (Int?) -> Unit
//) {
//    // Lista predefiniowanych kolorów dla kolejnych klastrów
//    val clusterColors = listOf(
//        MaterialTheme.colorScheme.primary, // Kolor 1
//        Color(0xFF66BB6A),                 // Zielony
//        Color(0xFFFFA726),                 // Pomarańczowy
//        Color(0xFF42A5F5),                 // Niebieski
//        Color(0xFFAB47BC)                  // Fioletowy
//    )
//
//    // Etykiety dla przełącznika (bez zmian)
//    val tabLabels = remember(allClustersCount) {
//        listOf("Wszystkie") + (0 until allClustersCount).map { "Kurs ${it + 1}" }
//    }
//
//    // ✅ POPRAWKA: Filtrujemy znaczniki na podstawie widocznych klastrów
//    val visibleStopsWithLocation = remember(visibleClusters, allStops) {
//        // 1. Zbierz numery zamówień z widocznych segmentów
//        val visibleOrderNumbers =
//            visibleClusters.flatMap { it.segments }.map { it.orderNumber }.toSet()
//        // 2. Wybierz pasujące przystanki z pełnej listy
//        allStops.filter { it.orderNumber in visibleOrderNumbers && it.lat != null && it.lng != null }
//    }
//
//    // Stan kamery (bez zmian)
//    val cameraPositionState = rememberCameraPositionState {
//        position = CameraPosition.fromLatLngZoom(LatLng(52.237049, 21.017532), 6f)
//    }
//
//    Column(modifier = modifier.fillMaxSize()) {
//        // Przełącznik (bez zmian)
//        if (allClustersCount > 1) {
//            TabsChipsRow(
//                tabs = tabLabels,
//                selectedTabIndex = selectedClusterIndex?.let { it + 1 } ?: 0,
//                onSelect = { selectedTabIndex ->
//                    val newSelection = if (selectedTabIndex == 0) null else selectedTabIndex - 1
//                    onClusterSelected(newSelection)
//                }
//            )
//        }
//
//        // ✅ POPRAWKA: Efekt animacji kamery teraz działa na `visibleStopsWithLocation`
//        LaunchedEffect(visibleStopsWithLocation) {
//            if (visibleStopsWithLocation.isNotEmpty()) {
//                val boundsBuilder = LatLngBounds.builder()
//                visibleStopsWithLocation.forEach { stop ->
//                    boundsBuilder.include(LatLng(stop.lat!!, stop.lng!!))
//                }
//                cameraPositionState.animate(
//                    update = CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150),
//                    durationMs = 1000
//                )
//            }
//        }
//
//        GoogleMap(
//            modifier = Modifier.fillMaxSize(),
//            cameraPositionState = cameraPositionState,
//            properties = MapProperties(isMyLocationEnabled = true),
//            uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true)
//        ) {
//            // ✅ POPRAWKA: Rysowanie znaczników na podstawie `visibleStopsWithLocation`
//            visibleStopsWithLocation.forEach { stop ->
//                Marker(
//                    state = MarkerState(position = LatLng(stop.lat!!, stop.lng!!)),
//                    title = "Zamówienie #${stop.orderNumber}",
//                    snippet = stop.address
//                )
//            }
//
//            // ✅ POPRAWKA: Rysowanie polilinii na podstawie `visibleClusters`
//            visibleClusters.forEach { cluster ->
//                val clusterColor = clusterColors[cluster.clusterIndex % clusterColors.size]
//                cluster.segments.forEach { segment ->
//                    val segmentColor =
//                        if (segment.status == OrderStatusEnum.COMPLETED) { // Użyj statusu DELIVERED
//                            Color.Gray
//                        } else {
//                            clusterColor
//                        }
//                    Polyline(
//                        points = segment.points,
//                        color = segmentColor,
//                        width = 15f
//                    )
//                }
//            }
//        }
//    }
//}
