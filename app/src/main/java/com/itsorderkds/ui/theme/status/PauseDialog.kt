package com.itsorderkds.ui.theme.status

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.itsorderkds.R
import com.itsorderkds.ui.order.SourceEnum
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Lista portali, które użytkownik może wybrać.
 */
private val AVAILABLE_PORTALS = listOf(
    SourceEnum.TAKEAWAY,
    SourceEnum.GLOVO,
    SourceEnum.UBER,
    SourceEnum.WOLT,
    SourceEnum.BOLT,
    SourceEnum.WOOCOMMERCE,
    SourceEnum.ITS
)

/**
 * Definicja opcji czasu do wyboru.
 */
private data class QuickMinuteOption(val label: String, val minutes: Int)

/**
 * ZMODERNIZOWANY DIALOG PAUZY (z przewijaną linią chipów)
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PauseDialog(
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int, message: String?, portals: List<SourceEnum>?) -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(60) }
    var msg by remember { mutableStateOf("") }
    var selectedPortals by remember { mutableStateOf<Set<SourceEnum>>(setOf(SourceEnum.ITS)) }

    val endOfDayMinutes = remember { minutesUntilEndOfDay() }
    val quickMinuteOptions = remember {
        listOf(
            QuickMinuteOption("15 min", 15),
            QuickMinuteOption("30 min", 30),
            QuickMinuteOption("45 min", 45),
            QuickMinuteOption("60 min", 60),
            QuickMinuteOption("90 min", 90),
            QuickMinuteOption("120 min", 120),
            QuickMinuteOption("180 min", 180), // Dodałem jedną więcej dla lepszego scrolla
            QuickMinuteOption("Do końca dnia", endOfDayMinutes) // TODO: String
        )
    }

    val portalsToSend: List<SourceEnum>? = if (selectedPortals.isEmpty()) {
        null // Pauza globalna
    } else {
        selectedPortals.toList() // Pauza na wybrane portale
    }

    val subtitleText = if (portalsToSend == null) {
        "Wstrzymaj WSZYSTKIE portale" // TODO: String
    } else {
        "Wstrzymaj: ${portalsToSend.joinToString { it.name }}" // TODO: String
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ustaw pauzę") }, // TODO: String
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. SELEKTOR PORTALI (bez zmian)
                Text(
                    "Wybierz portale do wstrzymania", // TODO: String
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AVAILABLE_PORTALS.forEach { portal ->
                        PortalLogoButton(
                            portal = portal,
                            isSelected = portal in selectedPortals,
                            onClick = {
                                selectedPortals = if (portal in selectedPortals) {
                                    selectedPortals - portal
                                } else {
                                    selectedPortals + portal
                                }
                            }
                        )
                    }
                }

                Divider()

                // ---- ZMIANA 2: Sekcja czasu (LazyRow z FilterChips) ----
                Text(
                    "Czas trwania", // TODO: String
                    style = MaterialTheme.typography.labelLarge,
                )

                // Używamy LazyRow dla pojedynczej, przewijanej linii
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp) // Lekki padding
                ) {
                    items(quickMinuteOptions) { option ->
                        val isSelected = (option.minutes == selectedMinutes)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMinutes = option.minutes },
                            label = { Text(option.label) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, "Wybrano") }
                            } else {
                                null
                            }
                        )
                    }
                }
                // ----------------------------------------------------

                Divider()

                // 3. POWÓD (OPCJONALNIE) (bez zmian)
                OutlinedTextField(
                    value = msg,
                    onValueChange = { msg = it },
                    label = { Text("Komunikat (opcjonalnie)") }, // TODO: String
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(selectedMinutes, msg.ifBlank { null }, portalsToSend)
                onDismiss()
            }) { Text("Zatwierdź") } // TODO: String
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } } // TODO: String
    )
}

/**
 * Funkcja pomocnicza do obliczania czasu do północy
 */
private fun minutesUntilEndOfDay(): Int {
    val now = ZonedDateTime.now(ZoneId.systemDefault())
    val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return Duration.between(now, midnight).toMinutes().toInt().coerceIn(1, 1440)
}

/**
 * Przycisk-Logo z nakładką 'check' (bez zmian)
 */
@Composable
private fun PortalLogoButton(
    portal: SourceEnum,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterForSource(source = portal),
            contentDescription = portal.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                )
                .padding(2.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )

        if (isSelected) {
            // Nakładka "check"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Wybrano", // TODO: String
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Funkcja mapująca Twoje Enumy na zasoby (bez zmian)
 */
@Composable
private fun painterForSource(source: SourceEnum?): Painter {
    return when (source) {
        SourceEnum.UBER -> painterResource(id = R.drawable.ic_uber)
        SourceEnum.GLOVO -> painterResource(id = R.drawable.logo_glovo_80)
        SourceEnum.WOLT -> painterResource(id = R.drawable.logo_wolt_80)
        SourceEnum.BOLT -> painterResource(id = R.drawable.logo_bolt_80)
        SourceEnum.TAKEAWAY -> painterResource(id = R.drawable.ic_takeaway)
        SourceEnum.GOPOS -> painterResource(id = R.drawable.ic_gopos)
        SourceEnum.WOOCOMMERCE, SourceEnum.WOO -> painterResource(id = R.drawable.ic_woo)
        null, SourceEnum.UNKNOWN, SourceEnum.OTHER, SourceEnum.ITS -> painterResource(id = R.drawable.logo)
    }
}
