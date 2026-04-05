package com.itsorderkds.ui.kds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifiStatusbarConnectedNoInternet4
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

// ─── Filtry ──────────────────────────────────────────────────────────────────

private data class KdsFilter(val label: String, val state: String?)

private val kdsFilters = listOf(
    KdsFilter("Aktywne",          null),
    KdsFilter("Nowe",             "NEW"),
    KdsFilter("Przyjęte",         "ACKED"),
    KdsFilter("W przygotowaniu",  "IN_PROGRESS"),
    KdsFilter("Gotowe",           "READY"),
    KdsFilter("📅 Zaplanowane",   "SCHEDULED"),
)

// ─── Publiczne composable dla TopBar actions (używane w TopBarConfigBuilder) ─

/**
 * Wskaźnik połączenia + przycisk odświeżania — wstrzykiwany do TopBarConfig.actions
 * przez buildTopBarConfig gdy currentRoute == HOME.
 */
@Composable
fun KdsTopBarActions(
    isConnected: Boolean,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Icon(
        imageVector = if (isConnected) Icons.Default.Wifi
                      else Icons.Default.SignalWifiStatusbarConnectedNoInternet4,
        contentDescription = if (isConnected) "Połączony" else "Brak połączenia",
        tint = if (isConnected) Color(0xFF2E7D32) else Color(0xFFC62828),
        modifier = Modifier.padding(end = 2.dp)
    )
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(20.dp)
                .padding(end = 4.dp),
            strokeWidth = 2.dp
        )
    } else {
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Odśwież")
        }
    }
}

// ─── Ekran KDS ───────────────────────────────────────────────────────────────

/**
 * Główny ekran Kitchen Display System.
 * Nie posiada własnego TopBar — korzysta z AppTopBar w HomeActivity (hamburger menu → ustawienia).
 * Layout: FilterRow na górze + LazyRow kart ticketów.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KdsScreen(
    viewModel: KdsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val tickets       by viewModel.filteredTickets.collectAsStateWithLifecycle()
    val scheduledCount by viewModel.scheduledFutureCount.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    // ─── Obsługa eventów (snackbar) ──────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is KdsEvent.NewTicket ->
                    snackbarState.showSnackbar("🆕 Nowe zamówienie: ${event.orderNumber}")
                is KdsEvent.Error ->
                    snackbarState.showSnackbar("❌ ${event.message}")
                is KdsEvent.Success ->
                    snackbarState.showSnackbar("✅ ${event.message}")
                is KdsEvent.ScheduledSoon ->
                    snackbarState.showSnackbar(
                        "⏰ Zaplanowane zamówienie ${event.orderNumber} — za ${event.minutesLeft} min!"
                    )
            }
        }
    }

    // ─── Dialog anulowania ───────────────────────────────────────────────
    val cancelTicketId = uiState.cancelDialogTicketId
    if (cancelTicketId != null) {
        CancelTicketDialog(
            onConfirm = { reason -> viewModel.cancelTicket(cancelTicketId, reason) },
            onDismiss = { viewModel.dismissCancelDialog() }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ─── Filtry inline (bez osobnego TopBar) ─────────────────
            KdsFilterRow(
                activeFilter     = uiState.activeFilter,
                scheduledCount   = scheduledCount,
                onFilterSelected = { state -> viewModel.setFilter(state) }
            )

            // ─── Zawartość ───────────────────────────────────────────
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ładowanie ticketów…",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                tickets.isEmpty() -> EmptyKdsPlaceholder()

                else -> KdsTicketList(tickets = tickets, viewModel = viewModel)
            }
        }

        // Snackbar na dole ekranu
        SnackbarHost(
            hostState = snackbarState,
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ─── Filtry ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KdsFilterRow(
    activeFilter: String?,
    scheduledCount: Int,
    onFilterSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        kdsFilters.forEach { filter ->
            val selected = activeFilter == filter.state
            if (filter.state == "SCHEDULED" && scheduledCount > 0) {
                // Badge z liczbą zaplanowanych
                BadgedBox(
                    badge = {
                        Badge { Text("$scheduledCount", fontSize = 10.sp) }
                    }
                ) {
                    FilterChip(
                        selected = selected,
                        onClick  = { onFilterSelected(filter.state) },
                        label    = { Text(filter.label, fontSize = 12.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            } else {
                FilterChip(
                    selected = selected,
                    onClick  = { onFilterSelected(filter.state) },
                    label    = { Text(filter.label, fontSize = 12.sp) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}

// ─── Lista kart ───────────────────────────────────────────────────────────────

@Composable
private fun KdsTicketList(
    tickets: List<KdsTicketEntry>,
    viewModel: KdsViewModel
) {
    val listState = rememberLazyListState()

    LaunchedEffect(tickets.size) {
        if (tickets.isNotEmpty()) listState.animateScrollToItem(0)
    }

    LazyRow(
        state                 = listState,
        modifier              = Modifier.fillMaxSize(),
        contentPadding        = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = tickets, key = { it.ticket.id }) { entry ->
            KdsTicketCard(
                entry       = entry,
                onAck       = { viewModel.ackTicket(entry.ticket.id) },
                onStart     = { viewModel.startTicket(entry.ticket.id) },
                onReady     = { viewModel.readyTicket(entry.ticket.id) },
                onHandoff   = { viewModel.handoffTicket(entry.ticket.id) },
                onCancel    = { viewModel.showCancelDialog(entry.ticket.id) },
                onStartItem = { itemId -> viewModel.startItem(itemId, entry.ticket.id) },
                onReadyItem = { itemId -> viewModel.readyItem(itemId, entry.ticket.id) }
            )
        }
    }
}

// ─── Empty state ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyKdsPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "🍳", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text  = "Brak aktywnych zamówień",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = "Nowe tickety pojawią się automatycznie",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Dialog anulowania ───────────────────────────────────────────────────────

@Composable
private fun CancelTicketDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val isValid = reason.trim().isNotBlank() && reason.length <= 200

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Anuluj zamówienie") },
        text = {
            Column {
                Text(
                    text  = "Podaj powód anulowania:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value         = reason,
                    onValueChange = { if (it.length <= 200) reason = it },
                    label         = { Text("Powód (wymagany)") },
                    singleLine    = true,
                    isError       = reason.isNotBlank() && !isValid,
                    supportingText = {
                        if (reason.isNotBlank() && !isValid)
                            Text("Maks. 200 znaków", color = MaterialTheme.colorScheme.error)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason.trim()) }, enabled = isValid) {
                Text("Anuluj zamówienie", color = Color(0xFFC62828))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Wróć") }
        }
    )
}
