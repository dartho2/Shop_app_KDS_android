package com.itsorderkds.ui.theme.home.view


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itsorderkds.ui.order.OrderRouteState
import com.itsorderkds.ui.order.OrderStatusEnum
import com.itsorderkds.ui.order.OrdersViewModel
import com.itsorderkds.ui.theme.home.components.AcceptedOrdersTabContent
import com.itsorderkds.ui.theme.home.components.MapOrdersTabContent
import com.itsorderkds.ui.theme.home.components.RouteOutForDeliveryTabContent
import com.itsorderkds.ui.theme.home.components.RouteTabContent
import com.itsorderkds.ui.theme.home.components.TabsChipsRow
import com.itsorderkds.ui.theme.home.courierTabs

@Composable
fun CourierView(
    modifier: Modifier = Modifier,
    viewModel: OrdersViewModel,
    selectedOrderIds: Set<String>,
    isSelectionModeActive: Boolean,
    onSelectionChange: (Set<String>, OrderStatusEnum?) -> Unit
) {
    // Pobieramy stany z Flow tylko raz i korzystamy z uiState w komponencie
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allOrders by viewModel.orders.collectAsStateWithLifecycle()

    // Zarządzanie stanem zakładek kuriera
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    // Każda zakładka ma własny stan listy dla płynnego scrollowania
    val routeListState: LazyListState = rememberLazyListState()
    val acceptedListState: LazyListState = rememberLazyListState()
    val polylinePoints by viewModel.routeClusters.collectAsStateWithLifecycle() // <-- To też już masz
    val allClusters by viewModel.routeClusters.collectAsStateWithLifecycle()
    val inDeliveryListState: LazyListState = rememberLazyListState()
    val visibleClusters by viewModel.visibleRouteClusters.collectAsStateWithLifecycle()
    val lockedIds by viewModel.lockedOrderIds.collectAsStateWithLifecycle()
    val selectedClusterIndex by viewModel.selectedClusterIndex.collectAsStateWithLifecycle()


    // Pobranie trasy i synchronizacja przy pierwszym uruchomieniu
    LaunchedEffect(Unit) {
        viewModel.getOrderTras()
        viewModel.syncOrdersFromApiStartOfDay()
    }

    val tabLabels = courierTabs.map { stringResource(id = it.titleRes) }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        TabsChipsRow(
            tabs = tabLabels,
            selectedTabIndex = selectedTabIndex,
            onSelect = { selectedTabIndex = it }
        )

        when (selectedTabIndex) {
            0 -> RouteTabContent(
                modifier = Modifier,
                listState = routeListState,
                routeState = uiState.routeState,
                onRefresh = { viewModel.getOrderTras() },
                selectedOrderIds = selectedOrderIds,
                isSelectionModeActive = isSelectionModeActive,
                onSelectionChange = onSelectionChange,
                isOrderLocked = { id -> id in lockedIds }
            )

            1 -> RouteOutForDeliveryTabContent(
                modifier = Modifier,
                listState = routeListState,
                routeState = uiState.routeState,
                onRefresh = { viewModel.getOrderTras() },
                selectedOrderIds = selectedOrderIds,
                isSelectionModeActive = isSelectionModeActive,
                onSelectionChange = onSelectionChange,
                isOrderLocked = { id -> id in lockedIds }
            )

            2 -> AcceptedOrdersTabContent(
                modifier = Modifier,
                listState = acceptedListState,
                allOrders = allOrders,
                onOrderClick = { viewModel.triggerOpenDialog(it) },
                isRefreshing = uiState.isGlobalLoading,
                onRefresh = {  // (viewModel.syncOrdersFromApiStartOfDay()
                }
            )

            3 -> {
                val allRouteStops =
                    (uiState.routeState as? OrderRouteState.Success)?.route ?: emptyList()

                MapOrdersTabContent(
                    modifier = Modifier,
                    allStops = allRouteStops, // Przekaż pełną listę do filtrowania
                    visibleClusters = visibleClusters, // Przekaż przefiltrowane klastry do rysowania
                    allClustersCount = allClusters.size,
                    selectedClusterIndex = selectedClusterIndex,
                    onClusterSelected = { index -> viewModel.selectCluster(index) }
                )
            }
        }
    }
}