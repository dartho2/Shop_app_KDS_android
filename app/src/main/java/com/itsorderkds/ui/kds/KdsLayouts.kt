package com.itsorderkds.ui.kds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars

// ─── Helpery ─────────────────────────────────────────────────────────────────

private fun resolveColumns(gridColumns: Int, screenWidthDp: Int): Int = when {
    gridColumns > 0       -> gridColumns
    screenWidthDp >= 1200 -> 4
    screenWidthDp >= 900  -> 3
    else                  -> 2
}

/**
 * Callbacks dla karty ticketu — przekazywane jako lambdy zamiast całego ViewModelu,
 * dzięki czemu KdsLayouts.kt nie ma bezpośredniej zależności od KdsViewModel.
 */
data class TicketCallbacks(
    val onAck:       (ticketId: String) -> Unit,
    val onStart:     (ticketId: String) -> Unit,
    val onReady:     (ticketId: String) -> Unit,
    val onHandoff:   (ticketId: String) -> Unit,
    val onCancel:    (ticketId: String) -> Unit,
    val onStartItem: (itemId: String, ticketId: String) -> Unit,
    val onReadyItem: (itemId: String, ticketId: String) -> Unit
)

// ─── Wspólny composable karty ─────────────────────────────────────────────────

@Composable
private fun TicketSlot(
    entry: KdsTicketEntry,
    isFocused: Boolean,
    queueMode: Boolean,
    prepTimePickupMin: Int,
    prepTimeDeliveryMin: Int,
    cancelEnabled: Boolean,
    showNotes: Boolean,
    headerTapMode: Boolean,
    excludedKeywords: List<String> = emptyList(),
    compactCardMode: Boolean = false,
    isInFlight: Boolean = false,
    callbacks: TicketCallbacks
) {
    val id = entry.ticket.id
    if (compactCardMode) {
        KdsCompactTicketCard(
            entry            = entry,
            excludedKeywords = excludedKeywords,
            onStart          = { callbacks.onStart(id) },
            onReady          = { callbacks.onReady(id) },
            onHandoff        = { callbacks.onHandoff(id) },
            onStartItem      = { itemId -> callbacks.onStartItem(itemId, id) },
            onReadyItem      = { itemId -> callbacks.onReadyItem(itemId, id) }
        )
    } else {
        KdsTicketCard(
            entry               = entry,
            queueMode           = queueMode,
            isFocused           = isFocused,
            prepTimePickupMin   = prepTimePickupMin,
            prepTimeDeliveryMin = prepTimeDeliveryMin,
            cancelEnabled       = cancelEnabled,
            showNotes           = showNotes,
            headerTapMode       = headerTapMode,
            excludedKeywords    = excludedKeywords,
            isInFlight          = isInFlight,
            onAck       = { callbacks.onAck(id) },
            onStart     = { callbacks.onStart(id) },
            onReady     = { callbacks.onReady(id) },
            onHandoff   = { callbacks.onHandoff(id) },
            onCancel    = { callbacks.onCancel(id) },
            onStartItem = { itemId -> callbacks.onStartItem(itemId, id) },
            onReadyItem = { itemId -> callbacks.onReadyItem(itemId, id) }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. COMPACT_FLOW
//    Standardowy LazyVerticalGrid — elementy przesuwają się automatycznie.
//    Stable key zapobiega miganiu, ale pozycje mogą się zmienić po usunięciu.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CompactFlowLayout(
    tickets:             List<KdsTicketEntry>,
    callbacks:           TicketCallbacks,
    queueMode:           Boolean,
    gridColumns:         Int,
    focusedIndex:        Int,
    prepTimePickupMin:   Int = 30,
    prepTimeDeliveryMin: Int = 60,
    cancelEnabled:       Boolean = false,
    showNotes:           Boolean = true,
    headerTapMode:       Boolean = false,
    excludedKeywords:    List<String> = emptyList(),
    hasTopPanel:         Boolean = false,
    compactCardMode:     Boolean = false,
    inFlightIds:         Set<String> = emptySet()
) {
    val cols = resolveColumns(gridColumns, LocalConfiguration.current.screenWidthDp)
    val statusInsets = WindowInsets.statusBars.asPaddingValues()
    val navInsets    = WindowInsets.navigationBars.asPaddingValues()
    val ld           = LocalLayoutDirection.current
    LazyVerticalGrid(
        columns               = GridCells.Fixed(cols),
        modifier              = Modifier.fillMaxSize(),
        contentPadding        = PaddingValues(
            start  = 10.dp + statusInsets.calculateStartPadding(ld),
            end    = 10.dp + statusInsets.calculateEndPadding(ld),
            top    = 8.dp  + if (hasTopPanel) 0.dp else statusInsets.calculateTopPadding(),
            bottom = 8.dp  + navInsets.calculateBottomPadding()
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement   = Arrangement.spacedBy(10.dp)
    ) {
        items(items = tickets, key = { it.ticket.id }) { entry ->
            val idx = tickets.indexOf(entry)
            TicketSlot(
                entry               = entry,
                isFocused           = (idx == focusedIndex),
                queueMode           = queueMode,
                prepTimePickupMin   = prepTimePickupMin,
                prepTimeDeliveryMin = prepTimeDeliveryMin,
                cancelEnabled       = cancelEnabled,
                showNotes           = showNotes,
                headerTapMode       = headerTapMode,
                excludedKeywords    = excludedKeywords,
                compactCardMode     = compactCardMode,
                isInFlight          = entry.ticket.id in inFlightIds,
                callbacks           = callbacks
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. STABLE_GRID
//    Stałe sloty — każdy ticket ma przypisany slot-index.
//    Po usunięciu slot zostaje PUSTY (niewidoczny placeholder).
//    Inne tickety NIE zmieniają swoich pozycji.
//    Nowy ticket wypełnia pierwszy wolny slot (fillGaps=true) lub idzie na koniec.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StableGridLayout(
    ticketsMap:          Map<String, KdsTicketEntry>,
    slotMap:             SlotMap,
    callbacks:           TicketCallbacks,
    queueMode:           Boolean,
    gridColumns:         Int,
    focusedIndex:        Int,
    prepTimePickupMin:   Int = 30,
    prepTimeDeliveryMin: Int = 60,
    cancelEnabled:       Boolean = false,
    showNotes:           Boolean = true,
    headerTapMode:       Boolean = false,
    excludedKeywords:    List<String> = emptyList(),
    hasTopPanel:         Boolean = false,
    compactCardMode:     Boolean = false,
    inFlightIds:         Set<String> = emptySet()
) {
    val cols        = resolveColumns(gridColumns, LocalConfiguration.current.screenWidthDp)
    val sortedSlots = remember(slotMap) { slotMap.toSortedList() }
    val statusInsets = WindowInsets.statusBars.asPaddingValues()
    val navInsets    = WindowInsets.navigationBars.asPaddingValues()
    val ld           = LocalLayoutDirection.current
    LazyVerticalGrid(
        columns               = GridCells.Fixed(cols),
        modifier              = Modifier.fillMaxSize(),
        contentPadding        = PaddingValues(
            start  = 10.dp + statusInsets.calculateStartPadding(ld),
            end    = 10.dp + statusInsets.calculateEndPadding(ld),
            top    = 8.dp  + if (hasTopPanel) 0.dp else statusInsets.calculateTopPadding(),
            bottom = 8.dp  + navInsets.calculateBottomPadding()
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement   = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = sortedSlots,
            key   = { slot: Pair<Int, String?> -> slot.second ?: "empty_${slot.first}" }
        ) { slot: Pair<Int, String?> ->
            val slotIdx  = slot.first
            val ticketId = slot.second
            val entry = if (ticketId != null) ticketsMap[ticketId] else null
            if (entry != null) {
                TicketSlot(
                    entry               = entry,
                    isFocused           = (slotIdx == focusedIndex),
                    queueMode           = queueMode,
                    prepTimePickupMin   = prepTimePickupMin,
                    prepTimeDeliveryMin = prepTimeDeliveryMin,
                    cancelEnabled       = cancelEnabled,
                    showNotes           = showNotes,
                    headerTapMode       = headerTapMode,
                    excludedKeywords    = excludedKeywords,
                    compactCardMode     = compactCardMode,
                    isInFlight          = ticketId in inFlightIds,
                    callbacks           = callbacks
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. COLUMN_MODE
//    Niezależne pionowe kolumny (round-robin).
//    Usunięcie z jednej kolumny nie wpływa na inne.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ColumnModeLayout(
    tickets:             List<KdsTicketEntry>,
    callbacks:           TicketCallbacks,
    queueMode:           Boolean,
    gridColumns:         Int,
    focusedIndex:        Int,
    prepTimePickupMin:   Int = 30,
    prepTimeDeliveryMin: Int = 60,
    cancelEnabled:       Boolean = false,
    showNotes:           Boolean = true,
    headerTapMode:       Boolean = false,
    excludedKeywords:    List<String> = emptyList(),
    hasTopPanel:         Boolean = false,
    compactCardMode:     Boolean = false,
    inFlightIds:         Set<String> = emptySet()
) {
    val cols = resolveColumns(gridColumns, LocalConfiguration.current.screenWidthDp)
    val columns: List<List<Pair<Int, KdsTicketEntry>>> = remember(tickets, cols) {
        List(cols) { colIdx ->
            tickets.mapIndexedNotNull { idx, entry ->
                if (idx % cols == colIdx) idx to entry else null
            }
        }
    }
    val statusInsets = WindowInsets.statusBars.asPaddingValues()
    val navInsets    = WindowInsets.navigationBars.asPaddingValues()
    Row(
        modifier              = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        columns.forEach { colEntries ->
            LazyColumn(
                modifier            = Modifier.weight(1f),
                contentPadding      = PaddingValues(
                    start  = 5.dp,
                    end    = 5.dp,
                    top    = 8.dp + if (hasTopPanel) 0.dp else statusInsets.calculateTopPadding(),
                    bottom = 8.dp + navInsets.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = colEntries,
                    key   = { pair: Pair<Int, KdsTicketEntry> -> pair.second.ticket.id }
                ) { pair: Pair<Int, KdsTicketEntry> ->
                    TicketSlot(
                        entry               = pair.second,
                        isFocused           = (pair.first == focusedIndex),
                        queueMode           = queueMode,
                        prepTimePickupMin   = prepTimePickupMin,
                        prepTimeDeliveryMin = prepTimeDeliveryMin,
                        cancelEnabled       = cancelEnabled,
                        showNotes           = showNotes,
                        headerTapMode       = headerTapMode,
                        excludedKeywords    = excludedKeywords,
                        compactCardMode     = compactCardMode,
                        isInFlight          = pair.second.ticket.id in inFlightIds,
                        callbacks           = callbacks
                    )
                }
            }
        }
    }
}
