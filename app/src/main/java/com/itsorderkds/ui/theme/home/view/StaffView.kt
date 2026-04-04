package com.itsorderkds.ui.theme.home.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text // Użyj androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itsorderkds.R
import com.itsorderkds.data.model.Order
import com.itsorderkds.ui.order.OrderDelivery
import com.itsorderkds.ui.order.OrderStatusEnum
import com.itsorderkds.ui.order.SourceEnum
import com.itsorderkds.ui.theme.home.completedSubTabs
import com.itsorderkds.ui.theme.home.components.EmptyPlaceholder
import com.itsorderkds.ui.theme.home.components.OrderListItem
import com.itsorderkds.ui.theme.home.staffTabs
import com.itsorderkds.util.AppPrefs
import java.util.Locale

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun StaffView(
    modifier: Modifier = Modifier,
    // ✅ POPRAWIONE PARAMETRY (zgodne z HomeScreen.kt)
    activeOrders: List<Order>,
    completedOrders: List<Order>,
    dineInOrders: List<Order>,
    groupedOrders: Map<SourceEnum?, List<Order>>,
    onOrderClick: (Order) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    // Pager ma teraz 4 strony (0, 1, 2, 3) zgodnie z nową definicją staffTabs
    val pagerState = rememberPagerState(pageCount = { staffTabs.size })
    var selectedSubTabIndex by remember { mutableIntStateOf(0) }

    // Synchronizacja stanu z HomeScreen -> do pagera
    LaunchedEffect(selectedTabIndex) {
        selectedSubTabIndex = 0
        if (pagerState.currentPage != selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }
    }

    // Synchronizacja stanu z pagera -> do HomeScreen
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            onTabSelected(pagerState.currentPage)
        }
    }

    // Główna struktura layoutu
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        key = { staffTabs[it].titleRes }
    ) { pageIndex ->

        val isCompletedTabSelected = (pageIndex == 1) // "Zakończone" to teraz indeks 1

        Column(modifier = Modifier.fillMaxSize()) {
            // PillSwitch (dla zakładki "Zakończone", jeśli ma sub-taby)
            if (isCompletedTabSelected && completedSubTabs.isNotEmpty()) {
                val subTabLabels = completedSubTabs.map { stringResource(id = it.titleRes) }
                PillSwitch(
                    items = subTabLabels,
                    selectedIndex = selectedSubTabIndex,
                    onIndexSelect = { selectedSubTabIndex = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // ✅ ZAKTUALIZOWANA LOGIKA WYBORU WIDOKU
            when (pageIndex) {
                // Zakładka 0: "Zamówienia"
                0 -> ActiveOrdersList(
                    allOrders = activeOrders,
                    isRefreshing = isRefreshing,
                    onOrderClick = onOrderClick,
                    onRefresh = onRefresh
                )
                // Zakładka 1: "Zakończone"
                1 -> StandardOrdersList(
                    visibleOrders = completedOrders,
                    isRefreshing = isRefreshing,
                    onOrderClick = onOrderClick,
                    pageIndex = pageIndex,
                    selectedSubTabIndex = selectedSubTabIndex,
                    isCompletedTabSelected = isCompletedTabSelected,
                    onRefresh = onRefresh
                )
                // Zakładka 2: "Na miejscu" (DINE_IN / ROOM_SERVICE)
                2 -> DineInOrdersList(
                    dineInOrders = dineInOrders,
                    isRefreshing = isRefreshing,
                    onOrderClick = onOrderClick,
                    onRefresh = onRefresh
                )
                // Zakładka 3: "Inne"
                3 -> GroupedOrdersList(
                    groupedOrders = groupedOrders,
                    isRefreshing = isRefreshing,
                    onOrderClick = onOrderClick
                )
            }
        }
    }
}

/**
 * Renderuje standardową, płaską listę zamówień (DLA "ZAKOŃCZONE").
 */
@Composable
private fun StandardOrdersList(
    visibleOrders: List<Order>,
    isRefreshing: Boolean,
    onOrderClick: (Order) -> Unit,
    pageIndex: Int,
    selectedSubTabIndex: Int,
    isCompletedTabSelected: Boolean,
    onRefresh: () -> Unit
) {
    val listState = rememberLazyListState()

    val ordersToShow by remember(visibleOrders, isCompletedTabSelected, selectedSubTabIndex) {
        derivedStateOf {
            if (isCompletedTabSelected && completedSubTabs.isNotEmpty()) {
                val activeSubTabStatus = completedSubTabs.getOrNull(selectedSubTabIndex)?.status
                if (activeSubTabStatus != null) {
                    visibleOrders.filter {
                        val slugEnum = it.orderStatus.slug?.let { slug ->
                            runCatching { OrderStatusEnum.valueOf(slug) }.getOrNull()
                        }
                        slugEnum == activeSubTabStatus
                    }
                } else {
                    visibleOrders
                }
            } else {
                visibleOrders
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (ordersToShow.isEmpty() && !isRefreshing) {
                item { EmptyPlaceholder() }
            } else {
                items(ordersToShow, key = { "completed-${it.orderId}" }) { order ->
                    OrderListItem(order, onClick = { onOrderClick(order) })
                }
            }
        }
    }
}

/**
 * Renderuje listę zamówień DINE_IN i ROOM_SERVICE (NA MIEJSCU).
 */
@Composable
private fun DineInOrdersList(
    dineInOrders: List<Order>,
    isRefreshing: Boolean,
    onOrderClick: (Order) -> Unit,
    onRefresh: () -> Unit
) {
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (dineInOrders.isEmpty() && !isRefreshing) {
                item { EmptyPlaceholder() }
            } else {
                items(dineInOrders, key = { "dine-in-${it.orderId}" }) { order ->
                    OrderListItem(order, onClick = { onOrderClick(order) })
                }
            }
        }
    }
}

/**
 * ✅ ZAKTUALIZOWANY KOMPONENT: Renderuje listę dla zakładki "Zamówienia"
 * (Processing, Accepted, OutForDelivery).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActiveOrdersList(
    allOrders: List<Order>, // Przyjmuje 'activeOrdersList' (PROCESSING, ACCEPTED, OUT_FOR_DELIVERY)
    isRefreshing: Boolean,
    onOrderClick: (Order) -> Unit,
    onRefresh: () -> Unit
) {
    val listState = rememberLazyListState()
    var isOutForDeliveryExpanded by remember { mutableStateOf(false) }

    // 1. Podziel listę na 3 kategorie
    // ✅ POPRAWKA: Usunięto 'pending' i błędy składniowe
    val (processingOrders, outForDeliveryOrders, acceptedOrders) = remember(allOrders) {

        val processingList = mutableListOf<Order>()
        val outForDeliveryList = mutableListOf<Order>()
        val acceptedList = mutableListOf<Order>()

        allOrders.forEach {
            val slugEnum = it.orderStatus.slug?.let { slug ->
                runCatching { OrderStatusEnum.valueOf(slug) }.getOrNull()
            }

            when (slugEnum) {
                OrderStatusEnum.PROCESSING -> processingList.add(it)
                OrderStatusEnum.OUT_FOR_DELIVERY -> outForDeliveryList.add(it)
                OrderStatusEnum.ACCEPTED -> acceptedList.add(it)
                else -> {} // Ignoruj PENDING i inne
            }
        }
        // Zwróć 3 listy jako Triple
        Triple(processingList, outForDeliveryList, acceptedList)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Zaktualizowano warunek pustej listy
            if (processingOrders.isEmpty() && outForDeliveryOrders.isEmpty() && acceptedOrders.isEmpty() && !isRefreshing) {
                item { EmptyPlaceholder() }
                return@LazyColumn
            }

            // --- SEKCJA 1: PROCESSING (NOWE) ---
            if (processingOrders.isNotEmpty()) {
                // Zwykły nagłówek tekstowy
                item {
                    Text(
                        text = stringResource(R.string.order), // "Do zrobienia"
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }
                // Lista zamówień (zawsze widoczna)
                items(processingOrders, key = { "processing-${it.orderId}" }) { order ->
                    OrderListItem(order, onClick = { onOrderClick(order) })
                }
            }

            // --- SEKCJA 2: ACCEPTED (główna lista) ---
            if (acceptedOrders.isNotEmpty()) {
                // Tytuł dla głównej listy
                item {
                    Text(
                        text = stringResource(R.string.orders_accepted_to_prepare), // "Zaakceptowane"
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp) // Większy odstęp od sekcji wyżej
                    )
                }
                items(acceptedOrders, key = { "accepted-${it.orderId}" }) { order ->
                    OrderListItem(order, onClick = { onOrderClick(order) })
                }
            }

            // --- SEKCJA 3: OUT FOR DELIVERY (zwijana) ---
            if (outForDeliveryOrders.isNotEmpty()) {
                stickyHeader {
                    ExpandableHeader(
                        title = stringResource(R.string.status_out_for_delivery), // "W dostawie"
                        icon = Icons.Default.LocalShipping, // Ikona dostawy
                        orderCount = outForDeliveryOrders.size,
                        totalAmount = outForDeliveryOrders.sumOf { it.total },
                        isExpanded = isOutForDeliveryExpanded,
                        onClick = { isOutForDeliveryExpanded = !isOutForDeliveryExpanded },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                if (isOutForDeliveryExpanded) {
                    items(outForDeliveryOrders, key = { "out-${it.orderId}" }) { order ->
                        OrderListItem(order, onClick = { onOrderClick(order) })
                    }
                }
            }
        }
    }
}


/**
 * Renderuje listę pogrupowaną wg portalu (Source) dla zakładki "Inne".
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupedOrdersList(
    groupedOrders: Map<SourceEnum?, List<Order>>,
    isRefreshing: Boolean,
    onOrderClick: (Order) -> Unit
) {
    val listState = rememberLazyListState()
    var expandedSource by remember { mutableStateOf<SourceEnum?>(null) }

    val grandTotalCount = remember(groupedOrders) { groupedOrders.values.sumOf { it.size } }
    val grandTotalAmount = remember(groupedOrders) { groupedOrders.values.flatten().sumOf { it.total } }
    val deliveryCount = remember(groupedOrders) {
        groupedOrders.values.flatten().count {
            it.deliveryType == OrderDelivery.DELIVERY || it.deliveryType == OrderDelivery.DELIVERY_EXTERNAL
        }
    }
    val pickupCount = remember(groupedOrders) {
        groupedOrders.values.flatten().count {
            it.deliveryType == OrderDelivery.PICKUP || it.deliveryType == OrderDelivery.PICK_UP
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (groupedOrders.isEmpty() && !isRefreshing) {
            item { EmptyPlaceholder() }
        } else {
            item {
                GrandTotalSummary(
                    totalCount = grandTotalCount,
                    totalAmount = grandTotalAmount,
                    deliveryCount = deliveryCount,
                    pickupCount = pickupCount
                )
                Spacer(Modifier.height(8.dp))
            }

            val sortedGroups = groupedOrders.entries.sortedWith(
                compareBy(nullsLast()) { it.key?.name }
            )

            sortedGroups.forEach { (sourceEnum, ordersInGroup) ->
                val orderCount = ordersInGroup.size
                val totalAmount = ordersInGroup.sumOf { it.total }
                val isExpanded = expandedSource == sourceEnum

                stickyHeader {
                    SourceHeader(
                        source = sourceEnum,
                        orderCount = orderCount,
                        totalAmount = totalAmount,
                        isExpanded = isExpanded,
                        onClick = {
                            expandedSource = if (isExpanded) null else sourceEnum
                        }
                    )
                }

                if (isExpanded) {
                    items(ordersInGroup, key = { it.orderId }) { order ->
                        OrderListItem(order, onClick = { onOrderClick(order) })
                    }
                }
            }
        }
    }
}

/**
 * Nagłówek z sumą całkowitą
 */
@Composable
private fun GrandTotalSummary(
    totalCount: Int,
    totalAmount: Double,
    deliveryCount: Int,
    pickupCount: Int
) {
    val formattedAmount = stringResource(R.string.money_pln, totalAmount, AppPrefs.getCurrency())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.total_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.order_count_header, totalCount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.summary_deliveries, deliveryCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.summary_pickups, pickupCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}


/**
 * Komponent nagłówka dla grupy
 */
@Composable
private fun SourceHeader(
    source: SourceEnum?,
    orderCount: Int,
    totalAmount: Double,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val (painter, name) = when (source) {
        SourceEnum.UBER -> painterResource(id = R.drawable.ic_uber) to "UBER"
        SourceEnum.GLOVO -> painterResource(id = R.drawable.logo_glovo_80) to "GLOVO"
        SourceEnum.WOLT -> painterResource(id = R.drawable.logo_wolt_80) to "WOLT"
        SourceEnum.BOLT -> painterResource(id = R.drawable.logo_bolt_80) to "BOLT"
        SourceEnum.TAKEAWAY -> painterResource(id = R.drawable.ic_takeaway) to "TAKEAWAY"
        SourceEnum.WOOCOMMERCE -> painterResource(id = R.drawable.ic_woo) to "WOOCOMMERCE"
        SourceEnum.GOPOS -> painterResource(id = R.drawable.ic_gopos) to "GOPOS"
        SourceEnum.WOO -> painterResource(id = R.drawable.ic_woo) to "WOOCOMMERCE"
        null, SourceEnum.OTHER, SourceEnum.UNKNOWN, SourceEnum.ITS -> {
            painterResource(id = R.drawable.logo) to stringResource(R.string.status_other)
        }
    }
    val formattedAmount = stringResource(R.string.money_pln, totalAmount, AppPrefs.getCurrency())

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painter,
                contentDescription = name,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = name.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.order_count_header, orderCount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            val rotationAngle by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "ArrowRotation"
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                modifier = Modifier.rotate(rotationAngle)
            )
        }
    }
}

/**
 * Generyczny nagłówek dla zwijanych sekcji
 */
@Composable
private fun ExpandableHeader(
    title: String,
    icon: ImageVector,
    orderCount: Int,
    isExpanded: Boolean,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    totalAmount: Double? = null
) {
    val formattedAmount = totalAmount?.let { stringResource(R.string.money_pln, it, AppPrefs.getCurrency()) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        color = containerColor,
        shadowElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Spacer(Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.order_count_header, orderCount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                if (formattedAmount != null) {
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))
            val rotationAngle by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "ArrowRotation"
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                modifier = Modifier.rotate(rotationAngle),
                tint = contentColor
            )
        }
    }
}


/**
 * Komponent `PillSwitch` (bez zmian).
 */
@Composable
fun PillSwitch(
    items: List<String>,
    selectedIndex: Int,
    onIndexSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                CircleShape
            )
            .clip(CircleShape)
            .padding(4.dp)
    ) {
        val itemWidth = maxWidth / items.size
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "indicatorOffset"
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(itemWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface, CircleShape)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, text ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onIndexSelect(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedIndex == index) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
