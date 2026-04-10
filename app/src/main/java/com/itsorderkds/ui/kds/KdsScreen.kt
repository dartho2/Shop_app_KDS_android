package com.itsorderkds.ui.kds

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PrintDisabled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalWifiStatusbarConnectedNoInternet4
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.itsorderkds.data.model.KdsTicket
import com.itsorderkds.ui.theme.KdsCard
import com.itsorderkds.ui.theme.KdsCardBorder
import com.itsorderkds.ui.theme.KdsScheduled
import com.itsorderkds.ui.theme.KdsSlaGreen
import com.itsorderkds.ui.theme.KdsSlaRed
import com.itsorderkds.ui.theme.KdsSlaYellow
import com.itsorderkds.ui.theme.KdsTextMuted
import com.itsorderkds.ui.theme.KdsTextPrimary
import com.itsorderkds.ui.theme.KdsTextSecondary

// ─── Główny ekran KDS ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KdsScreen(
    viewModel: KdsViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val tickets        by viewModel.filteredTickets.collectAsStateWithLifecycle()
    val scheduledCount by viewModel.scheduledFutureCount.collectAsStateWithLifecycle()
    val historyTickets by viewModel.historyTickets.collectAsStateWithLifecycle()
    val historyLoading by viewModel.historyLoading.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    // ── Odśwież dane przy powrocie z tła / ustawień Androida ─────────────────
    // Bez tego po wejściu w ustawienia Androida i powrocie produkty w bloczkach
    // mogły być niewidoczne — ViewModel żył, socket był połączony, ale dane
    // były "stare" (StateFlow nie re-emituje, Compose nie wie że coś się zmieniło).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Obsługa eventów ──────────────────────────────────────────────────────
    // Dźwięk nowego zamówienia pochodzi z powiadomienia systemowego (NotificationHelper).
    // Tutaj obsługujemy tylko eventy UI (błędy, sukcesy, drukowanie).
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is KdsEvent.NewTicket -> {
                    // Dźwięk obsługuje NotificationHelper.showNewKdsTicket() w KdsSocketEventsHandler
                    // — nie odtwarzamy tu żadnego MediaPlayer, żeby nie grać podwójnie
                }
                is KdsEvent.Error -> {
                    snackbarState.showSnackbar(event.message)
                }
                is KdsEvent.Success -> {
                    snackbarState.showSnackbar(event.message)
                }
                is KdsEvent.PrintTicket -> {
                    // obsługa drukowania jest w ViewModelu
                }
                is KdsEvent.ScheduledSoon -> {
                    // powiadomienie zaplanowanego zamówienia — pomijamy tutaj
                }
            }
        }
    }

    uiState.cancelDialogTicketId?.let { id ->
        CancelTicketDialog(
            onConfirm = { reason -> viewModel.cancelTicket(id, reason) },
            onDismiss = { viewModel.dismissCancelDialog() }
        )
    }

    uiState.readyConfirmTicketId?.let { id ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissReadyConfirm() },
            title = { Text("Potwierdzenie") },
            text  = { Text("Oznaczyć zamówienie jako GOTOWE?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmReady(id) }) {
                    Text("GOTOWE", color = Color(0xFF2E7D32))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissReadyConfirm() }) { Text("Anuluj") }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (keyEvent.key) {
                    Key.DirectionRight, Key.Tab        -> { viewModel.navigateNext();   true }
                    Key.DirectionLeft                  -> { viewModel.navigatePrev();   true }
                    Key.Enter, Key.NumPadEnter, Key.F1 -> { viewModel.confirmFocused(); true }
                    Key.Escape, Key.F2                 -> { viewModel.cancelFocused();  true }
                    else                               -> false
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ─── Lewy wąski panel ──────────────────────────────────────
            // Brak paddingów systemowych — belka pokrywa CAŁĄ wysokość ekranu (status bar + nav bar)
            KdsSideRail(
                uiState              = uiState,
                scheduledCount       = scheduledCount,
                isLoading            = uiState.isLoading,
                onRefresh            = { viewModel.loadActiveTickets() },
                onFilterChange       = { viewModel.setFilter(it) },
                onNavigateToSettings = onNavigateToSettings,
                onToggleHistory      = { viewModel.toggleHistoryPanel() },
                onToggleSound        = { viewModel.toggleSoundMuted() },
                onTogglePrint        = { viewModel.togglePrintingPaused() }
            )

            // ─── Panel historii (rozwijany obok szyny) ─────────────────
            if (uiState.showHistoryPanel) {
                VerticalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                KdsHistoryPanel(
                    tickets  = historyTickets,
                    isLoading = historyLoading,
                    onRefresh = { viewModel.loadHistoryTickets() },
                    onClose   = { viewModel.toggleHistoryPanel() }
                )
                VerticalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
            } else {
                VerticalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
            }

            // ─── Główna treść ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when {
                    uiState.isLoading -> LoadingContent()
                    tickets.isEmpty() -> EmptyKdsPlaceholder(activeFilter = uiState.activeFilter)
                    else -> {
                        if (uiState.showProductionSummary && tickets.isNotEmpty()) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                ProductionSummaryPanel(
                                    tickets          = tickets,
                                    minQty           = uiState.productionMinQty,
                                    columns          = uiState.productionColumns,
                                    excludedKeywords = uiState.excludedKeywords
                                )
                                KdsContentArea(
                                    tickets      = tickets,
                                    viewModel    = viewModel,
                                    uiState      = uiState,
                                    hasTopPanel  = true
                                )
                            }
                        } else {
                            KdsContentArea(
                                tickets      = tickets,
                                viewModel    = viewModel,
                                uiState      = uiState,
                                hasTopPanel  = false
                            )
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarState,
                    modifier  = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                )
            }
        }
    }
}

// ─── Lewy panel nawigacyjny (wąska szyna ikon) ───────────────────────────────

@Composable
private fun KdsSideRail(
    uiState:              KdsUiState,
    scheduledCount:       Int,
    isLoading:            Boolean,
    onRefresh:            () -> Unit,
    onFilterChange:       (String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onToggleHistory:      () -> Unit,
    onToggleSound:        () -> Unit,
    onTogglePrint:        () -> Unit
) {
    Column(
        modifier              = Modifier
            .width(56.dp)
            .fillMaxHeight()
            .background(KdsCard),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.SpaceBetween
    ) {
        // ── Górna część: WiFi + filtr stanu ──────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier            = Modifier
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            // Wskaźnik połączenia
            Icon(
                imageVector        = if (uiState.socketConnected) Icons.Default.Wifi
                                     else Icons.Default.SignalWifiStatusbarConnectedNoInternet4,
                contentDescription = if (uiState.socketConnected) "Połączony" else "Brak połączenia",
                tint               = if (uiState.socketConnected) KdsSlaGreen else KdsSlaRed,
                modifier           = Modifier.size(20.dp)
            )

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(4.dp))

            // Wszystkie aktywne (brak filtra)
            RailItem(
                icon     = Icons.Default.GridView,
                label    = "Aktywne",
                selected = uiState.activeFilter == null,
                tint     = KdsSlaGreen,
                onClick  = { onFilterChange(null) }
            )

            // Filtr: Nowe
            if (uiState.queueMode) {
                RailItem(
                    icon     = Icons.Default.PlayArrow,
                    label    = "Nowe",
                    selected = uiState.activeFilter == "NEW",
                    tint     = Color(0xFF42A5F5),
                    onClick  = { onFilterChange("NEW") }
                )
                RailItem(
                    icon     = Icons.Default.FastForward,
                    label    = "W toku",
                    selected = uiState.activeFilter == "IN_PROGRESS",
                    tint     = KdsSlaYellow,
                    onClick  = { onFilterChange("IN_PROGRESS") }
                )
                RailItem(
                    icon     = Icons.Default.CheckCircle,
                    label    = "Gotowe",
                    selected = uiState.activeFilter == "READY",
                    tint     = KdsSlaGreen,
                    onClick  = { onFilterChange("READY") }
                )
            }

            // Zaplanowane — z badge jeśli są w kolejce
            BadgedBox(
                badge = {
                    if (scheduledCount > 0) {
                        Badge(
                            containerColor = KdsScheduled,
                            contentColor   = Color.White
                        ) {
                            Text(
                                text  = if (scheduledCount > 9) "9+" else "$scheduledCount",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                            )
                        }
                    }
                }
            ) {
                RailItem(
                    icon     = Icons.Default.Schedule,
                    label    = "Plan",
                    selected = uiState.activeFilter == "SCHEDULED",
                    tint     = KdsScheduled,
                    badge    = scheduledCount,
                    onClick  = { onFilterChange("SCHEDULED") }
                )
            }

            // Historia wydanych/anulowanych
            RailItem(
                icon     = Icons.Default.History,
                label    = "Historia",
                selected = uiState.showHistoryPanel,
                tint     = Color(0xFF90A4AE),
                onClick  = onToggleHistory
            )
        }

        // ── Dolna część: Quick toggles + Odśwież + Ustawienia ───────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier            = Modifier
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(2.dp))

            // Dźwięk — szybki toggle
            val soundTint = if (uiState.soundMuted) KdsSlaRed else KdsSlaGreen
            RailItem(
                icon     = if (uiState.soundMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                label    = if (uiState.soundMuted) "Cisza" else "Dźwięk",
                tint     = soundTint,
                selected = uiState.soundMuted,
                onClick  = onToggleSound
            )

            // Drukowanie — szybki toggle
            val printTint = if (uiState.printingPaused) KdsSlaRed else Color(0xFF90A4AE)
            RailItem(
                icon     = if (uiState.printingPaused) Icons.Default.PrintDisabled else Icons.Default.Print,
                label    = if (uiState.printingPaused) "Brak dr." else "Druk",
                tint     = printTint,
                selected = uiState.printingPaused,
                onClick  = onTogglePrint
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(2.dp))

            // Odśwież
            if (isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color       = KdsTextMuted
                )
            } else {
                RailItem(
                    icon    = Icons.Default.Refresh,
                    label   = "Odśwież",
                    tint    = KdsTextMuted,
                    onClick = onRefresh
                )
            }

            // Ustawienia
            RailItem(
                icon    = Icons.Default.Settings,
                label   = "Ustaw.",
                tint    = KdsTextMuted,
                onClick = onNavigateToSettings
            )
        }
    }
}

// ─── Pojedynczy element szyny ─────────────────────────────────────────────────

@Composable
private fun RailItem(
    icon:     ImageVector,
    label:    String,
    tint:     Color,
    onClick:  () -> Unit,
    selected: Boolean = false,
    badge:    Int     = 0
) {
    val bg = if (selected) tint.copy(alpha = 0.15f) else Color.Transparent
    Column(
        modifier            = Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = if (selected) tint else tint.copy(alpha = 0.55f),
            modifier           = Modifier.size(22.dp)
        )
        Text(
            text      = label,
            style     = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color     = if (selected) tint else tint.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            maxLines  = 1
        )
    }
}

// ─── Przełącznik trybów wyświetlania ─────────────────────────────────────────

@Composable
private fun KdsContentArea(
    tickets: List<KdsTicketEntry>,
    viewModel: KdsViewModel,
    uiState: KdsUiState,
    hasTopPanel: Boolean = false
) {
    val ticketsMap = remember(tickets) { tickets.associateBy { it.ticket.id } }

    val callbacks = remember(viewModel) {
        TicketCallbacks(
            onAck       = { id -> viewModel.ackTicket(id) },
            onStart     = { id -> viewModel.startTicket(id) },
            onReady     = { id -> viewModel.readyTicket(id) },
            onHandoff   = { id -> viewModel.handoffTicket(id) },
            onCancel    = { id -> viewModel.showCancelDialog(id) },
            onStartItem = { itemId, ticketId -> viewModel.startItem(itemId, ticketId) },
            onReadyItem = { itemId, ticketId -> viewModel.readyItem(itemId, ticketId) }
        )
    }

    val displayMode = if (uiState.activeFilter == "SCHEDULED") {
        KdsDisplayMode.COMPACT_FLOW
    } else {
        uiState.displayMode
    }

    when (displayMode) {
        KdsDisplayMode.COMPACT_FLOW -> CompactFlowLayout(
            tickets             = tickets,
            callbacks           = callbacks,
            queueMode           = uiState.queueMode,
            gridColumns         = uiState.gridColumns,
            focusedIndex        = uiState.focusedIndex,
            prepTimePickupMin   = uiState.prepTimePickupMin,
            prepTimeDeliveryMin = uiState.prepTimeDeliveryMin,
            cancelEnabled       = uiState.cancelEnabled,
            showNotes           = uiState.showNotes,
            headerTapMode       = uiState.headerTapMode,
            excludedKeywords    = uiState.excludedKeywords,
            hasTopPanel         = hasTopPanel,
            compactCardMode     = uiState.compactCardMode,
            inFlightIds         = uiState.inFlightIds
        )
        KdsDisplayMode.STABLE_GRID -> StableGridLayout(
            ticketsMap          = ticketsMap,
            slotMap             = uiState.slotMap,
            callbacks           = callbacks,
            queueMode           = uiState.queueMode,
            gridColumns         = uiState.gridColumns,
            focusedIndex        = uiState.focusedIndex,
            prepTimePickupMin   = uiState.prepTimePickupMin,
            prepTimeDeliveryMin = uiState.prepTimeDeliveryMin,
            cancelEnabled       = uiState.cancelEnabled,
            showNotes           = uiState.showNotes,
            headerTapMode       = uiState.headerTapMode,
            excludedKeywords    = uiState.excludedKeywords,
            hasTopPanel         = hasTopPanel,
            compactCardMode     = uiState.compactCardMode,
            inFlightIds         = uiState.inFlightIds
        )
        KdsDisplayMode.COLUMN_MODE -> ColumnModeLayout(
            tickets             = tickets,
            callbacks           = callbacks,
            queueMode           = uiState.queueMode,
            gridColumns         = uiState.gridColumns,
            focusedIndex        = uiState.focusedIndex,
            prepTimePickupMin   = uiState.prepTimePickupMin,
            prepTimeDeliveryMin = uiState.prepTimeDeliveryMin,
            cancelEnabled       = uiState.cancelEnabled,
            showNotes           = uiState.showNotes,
            headerTapMode       = uiState.headerTapMode,
            excludedKeywords    = uiState.excludedKeywords,
            hasTopPanel         = hasTopPanel,
            compactCardMode     = uiState.compactCardMode,
            inFlightIds         = uiState.inFlightIds
        )
    }
}

// ─── Loading ─────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Ładowanie zamówień…", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ─── Empty state ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyKdsPlaceholder(activeFilter: String? = null) {
    val (emoji, title, subtitle) = when (activeFilter) {
        "SCHEDULED" -> Triple(
            "📅",
            "Brak zaplanowanych zamówień",
            "Zaplanowane zamówienia pojawią się tutaj"
        )
        "NEW"         -> Triple("🆕", "Brak nowych zamówień", "Nowe zamówienia pojawią się automatycznie")
        "IN_PROGRESS" -> Triple("⏳", "Brak zamówień w toku", "")
        "READY"       -> Triple("✅", "Brak gotowych zamówień", "")
        else          -> Triple("🍳", "Brak aktywnych zamówień", "Nowe zamówienia pojawią się automatycznie")
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text  = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        title            = { Text("Anuluj zamówienie") },
        text = {
            Column {
                Text(text = "Podaj powód anulowania:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value          = reason,
                    onValueChange  = { if (it.length <= 200) reason = it },
                    label          = { Text("Powód (wymagany)") },
                    singleLine     = true,
                    isError        = reason.isNotBlank() && !isValid,
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

// ─── TopBar actions (zachowane dla innych ekranów) ────────────────────────────

@Composable
fun KdsTopBarActions(
    isConnected: Boolean,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Icon(
        imageVector        = if (isConnected) Icons.Default.Wifi
                             else Icons.Default.SignalWifiStatusbarConnectedNoInternet4,
        contentDescription = if (isConnected) "Połączony" else "Brak połączenia",
        tint               = if (isConnected) Color(0xFF2E7D32) else Color(0xFFC62828),
        modifier           = Modifier.padding(end = 2.dp)
    )
    if (isLoading) {
        CircularProgressIndicator(
            modifier    = Modifier.size(20.dp).padding(end = 4.dp),
            strokeWidth = 2.dp
        )
    } else {
        androidx.compose.material3.IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Odswież")
        }
    }
}

// ─── Panel historii ───────────────────────────────────────────────────────────

@Composable
private fun KdsHistoryPanel(
    tickets:   List<KdsTicket>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onClose:   () -> Unit
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(KdsCard)
    ) {
        // Naglowek
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .background(KdsCardBorder.copy(alpha = 0.4f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Historia",
                style      = MaterialTheme.typography.labelMedium,
                color      = KdsTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(14.dp),
                        strokeWidth = 1.5.dp,
                        color       = KdsTextMuted
                    )
                } else {
                    Icon(
                        imageVector        = Icons.Default.Refresh,
                        contentDescription = "Odswiez historie",
                        tint               = KdsTextMuted,
                        modifier           = Modifier
                            .size(16.dp)
                            .clickable { onRefresh() }
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector        = Icons.Default.Cancel,
                    contentDescription = "Zamknij",
                    tint               = KdsTextMuted,
                    modifier           = Modifier
                        .size(16.dp)
                        .clickable { onClose() }
                )
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

        // Lista
        when {
            isLoading && tickets.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = KdsTextMuted)
                }
            }
            tickets.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", fontSize = 28.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Brak historii", style = MaterialTheme.typography.bodySmall, color = KdsTextMuted)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(tickets, key = { it.id }) { ticket ->
                        HistoryTicketRow(ticket = ticket)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTicketRow(ticket: KdsTicket) {
    val isCancelled = ticket.state == "CANCELLED"
    val accentColor = if (isCancelled) KdsSlaRed else KdsSlaGreen
    val stateLabel  = if (isCancelled) "Anulowane" else "Wydane"
    val stateIcon   = if (isCancelled) "x" else "v"

    val timeLabel = remember(ticket.handedOffAt, ticket.cancelledAt, ticket.updatedAt) {
        val raw = ticket.handedOffAt ?: ticket.cancelledAt ?: ticket.updatedAt
        runCatching {
            val instant = java.time.Instant.parse(raw)
            val local   = instant.atZone(java.time.ZoneId.systemDefault())
            "%02d:%02d".format(local.hour, local.minute)
        }.getOrElse { "--:--" }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = KdsCardBorder.copy(alpha = 0.35f)),
        shape    = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text       = ticket.displayNumber,
                        style      = MaterialTheme.typography.labelMedium,
                        color      = KdsTextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    val typeIcon = when (ticket.orderType?.lowercase()) {
                        "delivery" -> "D"
                        "dine_in"  -> "M"
                        else       -> "W"
                    }
                    Text(
                        text  = typeIcon,
                        style = MaterialTheme.typography.labelSmall,
                        color = KdsTextSecondary,
                        fontSize = 9.sp
                    )
                }
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text  = "$stateIcon $stateLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                    Text(
                        text  = "· $timeLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = KdsTextMuted
                    )
                }
            }
        }
    }
}
