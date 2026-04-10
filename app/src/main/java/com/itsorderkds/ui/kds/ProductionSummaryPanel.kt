package com.itsorderkds.ui.kds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Panel pomocniczy "Production Summary" — agreguje pozycje ze wszystkich aktywnych
 * zamówień i pokazuje ile razy dana pozycja występuje łącznie.
 *
 * Parametry:
 * - [minQty]   — minimalna ilość do wyświetlenia (1 = wszystkie, 2 = tylko qty >= 2)
 * - [columns]  — liczba kolumn listy (1 lub 2)
 */
@Composable
fun ProductionSummaryPanel(
    tickets:           List<KdsTicketEntry>,
    modifier:          Modifier = Modifier,
    minQty:            Int = 1,
    columns:           Int = 1,
    excludedKeywords:  List<String> = emptyList()
) {
    if (tickets.isEmpty()) return

    // Agregacja: displayName → suma qty, z filtrowaniem wykluczonych słów kluczowych i minQty
    val summary: List<Pair<String, Int>> = remember(tickets, minQty, excludedKeywords) {
        tickets
            .flatMap { entry -> entry.items.filter { it.state !in listOf("READY", "SERVED", "VOID") } }
            .filter { item ->
                if (excludedKeywords.isEmpty()) return@filter true
                val nameLower = item.displayName.trim().lowercase()
                excludedKeywords.none { keyword -> nameLower.contains(keyword) }
            }
            .groupBy { it.displayName.trim() }
            .map { (name, items) -> name to items.sumOf { it.qty } }
            .filter { (_, qty) -> qty >= minQty }
            .sortedByDescending { it.second }
    }

    if (summary.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }

    val visibleCount = summary.size
    val filteredLabel = if (minQty > 1) " · min. ${minQty}×" else ""

    Surface(
        modifier        = modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
        color           = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 2.dp
    ) {
        Column {
            // ─── Nagłówek (klikalny) ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(
                        start  = 14.dp,
                        end    = 14.dp,
                        top    = 8.dp,
                        bottom = 8.dp
                    ),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint     = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text  = "Produkcja łącznie ($visibleCount poz.$filteredLabel)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Icon(
                    imageVector        = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Zwiń" else "Rozwiń",
                    tint               = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier           = Modifier.size(20.dp)
                )
            }

            // ─── Zawartość (zwijana) ─────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (columns >= 2) {
                        // ── Dwukolumnowy układ ──────────────────────
                        val chunks = summary.chunked(2)
                        chunks.forEach { pair ->
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { (name, qty) ->
                                    ProductionItem(
                                        name     = name,
                                        qty      = qty,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Jeśli nieparzysta liczba — uzupełnij pustą komórką
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        // ── Jednokolumnowy układ (domyślny) ─────────
                        summary.forEach { (name, qty) ->
                            ProductionItem(
                                name     = name,
                                qty      = qty,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Pojedyncza pozycja produkcji ────────────────────────────────────────────

@Composable
private fun ProductionItem(
    name:     String,
    qty:      Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier          = modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (qty >= 5) Color(0xFFC62828).copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surface
        ) {
            Text(
                text     = "${qty}×",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style    = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = if (qty >= 5) Color(0xFFC62828)
                                 else MaterialTheme.colorScheme.primary
                )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text     = name,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )
    }
}

