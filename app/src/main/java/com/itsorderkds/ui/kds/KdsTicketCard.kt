package com.itsorderkds.ui.kds

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itsorderkds.data.model.KdsTicket
import com.itsorderkds.data.model.KdsTicketItem
import com.itsorderkds.ui.theme.KdsCard
import com.itsorderkds.ui.theme.KdsCardBorder
import com.itsorderkds.ui.theme.KdsScheduled
import com.itsorderkds.ui.theme.KdsSlaBlue
import com.itsorderkds.ui.theme.KdsSlaGray
import com.itsorderkds.ui.theme.KdsSlaGreen
import com.itsorderkds.ui.theme.KdsSlaRed
import com.itsorderkds.ui.theme.KdsSlaYellow
import com.itsorderkds.ui.theme.KdsTextMuted
import com.itsorderkds.ui.theme.KdsTextPrimary
import com.itsorderkds.ui.theme.KdsTextSecondary
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

// ─── Kolory SLA — zoptymalizowane pod ciemny motyw KDS ──────────────────────

private val SlaGreen       = KdsSlaGreen      // > 5 min — spokojnie
private val SlaYellow      = KdsSlaYellow     // 0–5 min — uwaga
private val SlaRed         = KdsSlaRed        // minął czas — alarm
private val SlaGray        = KdsSlaGray       // wygasły / done
private val SlaBlue        = KdsSlaBlue       // wydano
private val ScheduledColor = KdsScheduled     // zaplanowane

// ─── Baner zaplanowanego zamówienia ─────────────────────────────────────────

/**
 * Baner wyświetlany na karcie gdy zamówienie ma ustaloną godzinę realizacji.
 * Pokazuje: czas zaplanowania + odliczanie do początku gotowania.
 */
@Composable
private fun ScheduledBanner(
    ticket: KdsTicket,
    prepTimePickupMin: Int = 30,
    prepTimeDeliveryMin: Int = 60
) {
    val sf = ticket.scheduledFor ?: return

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(60_000); nowMs = System.currentTimeMillis() }
    }

    val scheduledMs = runCatching {
        ZonedDateTime.parse(sf).toInstant().toEpochMilli()
    }.getOrNull() ?: return

    // Czas realizacji (np. "12:00")
    val scheduledTime = runCatching {
        ZonedDateTime.parse(sf)
            .withZoneSameInstant(java.time.ZoneId.systemDefault())
            .toLocalTime()
            .let { "%02d:%02d".format(it.hour, it.minute) }
    }.getOrElse { sf }

    // Dobierz prepTime na podstawie typu zamówienia
    val prepMin = when (ticket.orderType?.lowercase()) {
        "delivery" -> prepTimeDeliveryMin
        else       -> prepTimePickupMin   // pickup, dine_in, null → domyślnie pickup
    }

    // Godzina "zacznij gotować" = scheduledFor - prepTime
    val startCookMs = scheduledMs - prepMin * 60_000L
    val startCookTime = runCatching {
        java.time.Instant.ofEpochMilli(startCookMs)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
            .let { "%02d:%02d".format(it.hour, it.minute) }
    }.getOrElse { "" }

    val diffMin        = (scheduledMs - nowMs) / 60_000       // do godziny realizacji
    val diffCookMin    = (startCookMs - nowMs) / 60_000       // do czasu gotowania

    val (bannerColor, countdownText) = when {
        diffMin <= 0     -> SlaRed         to "🔴 SPÓŹNIONE o ${kotlin.math.abs(diffMin)}min! (na $scheduledTime)"  // po czasie — czerwony alarm
        diffCookMin > 60 -> ScheduledColor to "Zacznij gotować o $startCookTime"                                    // daleko — fiolet
        diffCookMin > 0  -> SlaYellow      to "⏰ Zacznij gotować o $startCookTime (za ${diffCookMin}min!)"         // za chwilę — żółty
        else             -> Color(0xFFE65100) to "🔥 ZACZNIJ GOTOWAĆ TERAZ! (na $scheduledTime)"                    // czas prepTime minął — pomarańcz
    }

    val orderTypeLabel = when (ticket.orderType?.lowercase()) {
        "delivery" -> "🚗 Dostawa"
        "pickup"   -> "🏠 Odbiór osobisty"
        "dine_in"  -> "🍽 Na miejscu"
        else       -> null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(6.dp),
        color    = bannerColor.copy(alpha = 0.12f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            // Wiersz 1: ikona + godzina realizacji + typ zamówienia
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint     = bannerColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text  = "Zaplanowane na $scheduledTime",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = bannerColor
                    )
                }
//                if (orderTypeLabel != null) {
//                    Text(
//                        text  = orderTypeLabel,
//                        style = MaterialTheme.typography.labelSmall,
//                        color = bannerColor.copy(alpha = 0.8f)
//                    )
//                }
            }
            // Wiersz 2: kiedy zacząć gotować
            if (countdownText.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = countdownText,
                    style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color    = bannerColor,
                    modifier = Modifier.padding(start = 20.dp)
                )
            }
        }
    }
}

// ─── Karta ticketu ───────────────────────────────────────────────────────────

/**
 * Karta KDS wyświetlana na głównym ekranie kuchni.
 * Pokazuje: numer zamówienia, priorytet, timer SLA, pozycje, przyciski akcji.
 */
@Composable
fun KdsTicketCard(
    entry: KdsTicketEntry,
    queueMode: Boolean = false,
    isFocused: Boolean = false,
    prepTimePickupMin: Int = 30,
    prepTimeDeliveryMin: Int = 60,
    cancelEnabled: Boolean = false,
    showNotes: Boolean = true,
    headerTapMode: Boolean = false,
    excludedKeywords: List<String> = emptyList(),
    showProductionsInCard: Boolean = false,
    isInFlight: Boolean = false,
    onAck: () -> Unit,
    onStart: () -> Unit,
    onReady: () -> Unit,
    onHandoff: () -> Unit,
    onCancel: () -> Unit,
    onStartItem: (itemId: String) -> Unit,
    onReadyItem: (itemId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ticket = entry.ticket
    val items = remember(entry.items, excludedKeywords) {
        entry.items
            .filter { item ->
                if (excludedKeywords.isEmpty()) return@filter true
                val nameLower = item.displayName.trim().lowercase()
                excludedKeywords.none { keyword -> nameLower.contains(keyword) }
            }
            .sortedBy { it.sequence }
    }

    val isFuture = ticket.isScheduledFuture()

    // Kolor paska stanu po lewej stronie — bardzo wyraźny
    val stateAccentColor = when {
        isFocused                           -> Color(0xFFFFCA28)  // złoty — fokus HID
        isInFlight                          -> Color(0xFF90A4AE)  // szary — w toku
        ticket.state == "READY"             -> Color(0xFF1565C0)  // niebieski — gotowe, czeka na wydanie
        ticket.state == "IN_PROGRESS"       -> Color(0xFFF9A825)  // żółty — w przygotowaniu
        ticket.state == "ACKED"             -> Color(0xFF2E7D32)  // zielony — przyjęte
        ticket.state == "NEW"               -> Color(0xFFE65100)  // pomarańczowy — nowe
        isFuture                            -> ScheduledColor     // fiolet — zaplanowane daleko
        else                                -> slaColor(ticket)   // SLA-based
    }

    // Tło nagłówka — subtelne, czytelne
    val headerBg = when {
        isInFlight                    -> Color(0xFF37474F).copy(alpha = 0.6f)
        ticket.state == "READY"       -> Color(0xFF1565C0).copy(alpha = 0.18f)
        ticket.state == "IN_PROGRESS" -> Color(0xFFF9A825).copy(alpha = 0.12f)
        ticket.state == "ACKED"       -> Color(0xFF2E7D32).copy(alpha = 0.10f)
        ticket.state == "NEW"         -> Color(0xFFE65100).copy(alpha = 0.10f)
        else                          -> Color.Transparent
    }

    val borderColor by animateColorAsState(
        targetValue   = stateAccentColor,
        animationSpec = tween(300),
        label         = "state_border"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = KdsCardBorder, shape = RoundedCornerShape(0.dp)),
        shape     = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 12.dp else if (isFuture) 2.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isFocused -> KdsCard.copy(alpha = 0.95f)
                isFuture  -> KdsCard.copy(alpha = 0.75f)
                else      -> KdsCard
            }
        )
    ) {
        // Główny Row: pionowy pasek stanu + treść karty
        Row(modifier = Modifier.fillMaxWidth()) {

            // ─── Pionowy pasek stanu (lewy bok karty) ─────────────────
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(borderColor)
            )

            // ─── Treść karty ───────────────────────────────────────────
            Column(modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {

            // ─── Baner zaplanowanego zamówienia (jeśli dotyczy) ───────
            if (ticket.isScheduled()) {
                ScheduledBanner(
                    ticket              = ticket,
                    prepTimePickupMin   = prepTimePickupMin,
                    prepTimeDeliveryMin = prepTimeDeliveryMin
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ─── Nagłówek — klikalny w trybie tap, zwykły w trybie przycisków ──
            val tapAction: (() -> Unit)? = if (headerTapMode && !isInFlight) nextStateAction(
                state    = ticket.state,
                isFuture = isFuture,
                onAck    = onAck,
                onStart  = onStart,
                onReady  = onReady,
                onHandoff = onHandoff
            ) else null

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(headerBg)
                    .then(
                        if (tapAction != null) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = tapAction
                        ) else Modifier
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Column {
                    TicketHeader(ticket = ticket, isInFlight = isInFlight)
                    if (headerTapMode && tapAction != null) {
                        Spacer(Modifier.height(4.dp))
                        TapHint(state = ticket.state)
                    }
                    if (isInFlight) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text  = "⏳ Przetwarzanie…",
                            style = MaterialTheme.typography.labelSmall,
                            color = KdsTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // ─── Notatka do zamówienia ─────────────────────────────────
            if (!ticket.note.isNullOrBlank()) {
                NoteChip(note = ticket.note)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ─── Lista pozycji ─────────────────────────────────────────
            Column {
                items.forEach { item ->
                    KdsItemRow(
                        item                  = item,
                        showNotes             = showNotes,
                        showProductionsInCard = showProductionsInCard,
                        onStartItem           = { onStartItem(item.id) },
                        onReadyItem           = { onReadyItem(item.id) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // ─── Przyciski akcji — zablokowane gdy isInFlight ──────────
            val actionsEnabled = !isInFlight
            if (!headerTapMode) {
                TicketActions(
                    ticket        = ticket,
                    queueMode     = queueMode,
                    isFuture      = isFuture,
                    cancelEnabled = cancelEnabled,
                    enabled       = actionsEnabled,
                    onAck         = onAck,
                    onStart       = onStart,
                    onReady       = onReady,
                    onHandoff     = onHandoff,
                    onCancel      = onCancel
                )
            } else if (cancelEnabled) {
                OutlinedButton(
                    onClick  = onCancel,
                    enabled  = actionsEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = SlaRed)
                ) {
                    Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ANULUJ")
                }
            }
        } // Column treści
        } // Row główny
    }
}


// ─── Nagłówek karty ──────────────────────────────────────────────────────────

@Composable
private fun TicketHeader(ticket: KdsTicket, isInFlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            // Główny numer — kdsTicketNumber gdy API zwraca (np. "W-003"), inaczej orderNumber
            if (ticket.kdsTicketNumber != null) {
                Text(
                    text     = ticket.kdsTicketNumber,
                    style    = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    fontSize = 26.sp,
                    color    = KdsTextPrimary
                )
                // orderNumber jako mały podpis (do weryfikacji z kasą)
                Text(
                    text  = ticket.orderNumber,
                    style = MaterialTheme.typography.labelSmall,
                    color = KdsTextMuted
                )
            } else {
                // Fallback — API jeszcze nie zwraca kdsTicketNumber
                Text(
                    text     = ticket.orderNumber,
                    style    = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    fontSize = 22.sp,
                    color    = KdsTextPrimary
                )
            }
            // Stan ticketu + typ zamówienia w jednym wierszu
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text  = stateLabel(ticket.state),
                    style = MaterialTheme.typography.labelMedium,
                    color = stateColor(ticket.state)
                )
                val orderTypeLabel = when (ticket.orderType?.lowercase()) {
                    "delivery" -> "🚗 Dostawa"
                    "pickup"   -> "🏠 Wynos"
                    "dine_in"  -> "🍽 Na miejscu"
                    else       -> null
                }
                if (orderTypeLabel != null) {
                    Text(
                        text  = "·",
                        style = MaterialTheme.typography.labelMedium,
                        color = KdsTextMuted
                    )
                    Text(
                        text  = orderTypeLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = KdsTextSecondary
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            // Chip RUSH
            if (ticket.priority == "rush") {
                RushChip()
                Spacer(modifier = Modifier.height(4.dp))
            }
            // Timer SLA
            SlaTimer(ticket = ticket)
        }
    }
}

// ─── Chip RUSH ───────────────────────────────────────────────────────────────

@Composable
private fun RushChip() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFFB71C1C)
    ) {
        Text(
            text = "⚡ RUSH",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
    }
}

// ─── Timer SLA ───────────────────────────────────────────────────────────────

@Composable
private fun SlaTimer(ticket: KdsTicket) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(60_000); nowMs = System.currentTimeMillis() }
    }

    // Dla zaplanowanych — liczymy do scheduledFor, nie do slaTargetAt
    // (slaTargetAt to ~15 min od złożenia, dla zaplanowanych jest bez sensu)
    val targetMs: Long = if (ticket.isScheduled()) {
        ticket.scheduledFor?.let {
            runCatching { ZonedDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
        } ?: return
    } else {
        ticket.slaTargetAt?.let {
            runCatching { ZonedDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
        } ?: return
    }

    val diffMs  = targetMs - nowMs
    val diffSec = diffMs / 1_000
    val absMin  = kotlin.math.abs(diffSec / 60)
    val sign    = if (diffSec < 0) "-" else ""

    // Dla zaplanowanych — kolor bazuje na scheduledFor, nie na slaTargetAt
    val color = if (ticket.isScheduled()) scheduledTimerColor(diffMs) else slaColor(ticket)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector        = Icons.Default.Schedule,
            contentDescription = null,
            tint               = color,
            modifier           = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text  = "%s%d min".format(sign, absMin),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

/** Kolor timera dla zamówień zaplanowanych — bazuje na scheduledFor, nie slaTargetAt */
private fun scheduledTimerColor(msUntilScheduled: Long): Color = when {
    msUntilScheduled > 60 * 60_000L -> ScheduledColor  // > 60 min  — fiolet
    msUntilScheduled > 10 * 60_000L -> SlaGreen        // 10–60 min — zielony
    msUntilScheduled > 0            -> SlaYellow        // 0–10 min  — żółty (czas gotować!)
    else                            -> SlaRed           // po czasie — czerwony (spóźnienie!)
}

// ─── Notatka ─────────────────────────────────────────────────────────────────

@Composable
private fun NoteChip(note: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = "📝 $note",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// ─── Wiersz pozycji ──────────────────────────────────────────────────────────

@Composable
private fun KdsItemRow(
    item: KdsTicketItem,
    showNotes: Boolean = true,
    showProductionsInCard: Boolean = false,
    onStartItem: () -> Unit,
    onReadyItem: () -> Unit
) {
    val isDone = item.state == "READY" || item.state == "SERVED"

    // Kliknięcie całego wiersza wykonuje akcję w zależności od stanu pozycji
    val rowAction: (() -> Unit)? = when (item.state) {
        "QUEUED"  -> onStartItem
        "COOKING" -> onReadyItem
        else      -> null
    }

    // Kolor tła wiersza zależny od stanu — subtelna wskazówka dla kucharza
    val rowBg = when (item.state) {
        "COOKING" -> Color(0xFF1565C0).copy(alpha = 0.08f)
        "READY"   -> Color(0xFF2E7D32).copy(alpha = 0.08f)
        else      -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(rowBg)
            .then(
                if (rowAction != null)
                    Modifier.clickable(onClick = rowAction)
                else Modifier
            )
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        // Główny wiersz: ilość + nazwa + wskaźnik stanu
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ilość
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(KdsCardBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "×${item.qty}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = KdsTextPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Nazwa + notatki (opcjonalnie)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight     = if (isDone) FontWeight.Normal else FontWeight.Medium,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isDone) KdsTextMuted else KdsTextPrimary
                )
                if (showNotes && item.notes.isNotEmpty()) {
                    Text(
                        text  = item.notes.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = KdsTextSecondary
                    )
                }
            }

            // Mały wskaźnik stanu (bez ikony przycisku — zastąpiony kliknięciem wiersza)
            when (item.state) {
                "COOKING" -> {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1565C0))
                    )
                }
                "READY" -> {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint     = Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                }
                else -> { /* QUEUED / SERVED / VOID - brak wskaźnika */ }
            }
        }

        // ─── Sekcje produkcyjne (productions[]) — punkt 13.5 ─────────────
        // Wyświetl jako wcięta lista pod nazwą produktu (tylko gdy włączone w ustawieniach)
        val productions = item.productions
        if (showProductionsInCard && !productions.isNullOrEmpty() && !isDone) {
            Spacer(modifier = Modifier.height(3.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp) // wyrównaj z nazwą produktu (po ilości)
            ) {
                productions.forEach { task ->
                    val label = task.label?.takeIf { it.isNotBlank() } ?: return@forEach
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = "  • $label",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                            color = KdsTextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        if (task.qty > 0) {
                            Text(
                                text  = "×${task.qty}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = KdsTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Przyciski akcji ticketu ─────────────────────────────────────────────────

@Composable
private fun TicketActions(
    ticket: KdsTicket,
    queueMode: Boolean = false,
    isFuture: Boolean = false,
    cancelEnabled: Boolean = false,
    enabled: Boolean = true,
    onAck: () -> Unit,
    onStart: () -> Unit,
    onReady: () -> Unit,
    onHandoff: () -> Unit,
    onCancel: () -> Unit
) {
    // Zaplanowane zbyt daleko — blokujemy START, tylko ACK i CANCEL dostępne
    if (isFuture && ticket.state in listOf("NEW", "ACKED", "ACTIVE")) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(8.dp),
                color    = ScheduledColor.copy(alpha = 0.08f)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = ScheduledColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Poczekaj na czas realizacji", style = MaterialTheme.typography.bodySmall.copy(color = ScheduledColor))
                }
            }
            if (ticket.state == "NEW" || ticket.state == "ACTIVE") {
                OutlinedButton(onClick = onAck, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                    Text("PRZYJMIJ (zaplanowane)")
                }
            }
            if (cancelEnabled) {
                OutlinedButton(onClick = onCancel, enabled = enabled, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlaRed)
                ) { Text("ANULUJ") }
            }
        }
        return
    }

    // ─── TRYB PROSTY: tylko GOTOWE + (opcjonalnie) ANULUJ ────────────────
    if (!queueMode) {
        when (ticket.state) {
            "NEW", "ACTIVE", "ACKED", "IN_PROGRESS" -> {
                Button(
                    onClick  = onReady,
                    enabled  = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GOTOWE", style = MaterialTheme.typography.titleMedium)
                }
                if (cancelEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick  = onCancel,
                        enabled  = enabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = SlaRed)
                    ) {
                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ANULUJ")
                    }
                }
            }
            "READY" -> {
                Button(
                    onClick  = onHandoff,
                    enabled  = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WYDAJ")
                }
            }
            "HANDED_OFF" -> HandedOffBadge()
            "CANCELLED"  -> CancelledBadge()
        }
        return
    }

    // ─── TRYB KOLEJKOWY: pełny workflow ──────────────────────────────────
    when (ticket.state) {
        "NEW" -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick  = onAck,
                    enabled  = enabled,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PRZYJMIJ")
                }
                Button(
                    onClick  = onStart,
                    enabled  = enabled,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.FastForward, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("START")
                }
            }
            if (cancelEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onCancel, enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlaRed)
                ) {
                    Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ANULUJ")
                }
            }
        }

        "ACKED" -> {
            Button(
                onClick = onStart, enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.FastForward, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ZACZNIJ PRZYGOTOWANIE")
            }
            if (cancelEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(onClick = onCancel, enabled = enabled, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlaRed)
                ) { Text("ANULUJ") }
            }
        }

        "IN_PROGRESS" -> {
            Button(
                onClick = onReady, enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF9A825))
            ) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("GOTOWE", color = if (enabled) Color.Black else Color.Gray)
            }
            if (cancelEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(onClick = onCancel, enabled = enabled, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlaRed)
                ) { Text("ANULUJ") }
            }
        }

        "READY" -> {
            Button(
                onClick = onHandoff, enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WYDAJ")
            }
        }


        "HANDED_OFF" -> HandedOffBadge()
        "CANCELLED"  -> CancelledBadge()
    }
}

@Composable
private fun HandedOffBadge() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(8.dp),
        color    = KdsSlaBlue.copy(alpha = 0.15f)
    ) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.DoneAll, null, tint = KdsSlaBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Wydane", color = KdsSlaBlue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CancelledBadge() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(8.dp),
        color    = KdsSlaGray.copy(alpha = 0.15f)
    ) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Cancel, null, tint = SlaGray)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Anulowane", color = SlaGray, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Tap mode helpers ────────────────────────────────────────────────────────

/** Zwraca lambdę akcji dla kolejnego stanu lub null gdy brak przejścia */
private fun nextStateAction(
    state: String,
    isFuture: Boolean,
    onAck: () -> Unit,
    onStart: () -> Unit,
    onReady: () -> Unit,
    onHandoff: () -> Unit
): (() -> Unit)? = when {
    isFuture                -> onAck       // zaplanowane — tylko przyjmij
    state == "NEW"          -> onReady     // NEW → od razu READY (najprostsze)
    state == "ACKED"        -> onReady
    state == "IN_PROGRESS"  -> onReady
    state == "READY"        -> onHandoff
    else                    -> null
}

/** Kolor podpowiedzi następnego stanu — pasuje do koloru SLA/stanu */
private fun nextStateColor(state: String): Color = when (state) {
    "NEW"         -> KdsSlaGreen
    "ACKED"       -> KdsSlaGreen
    "IN_PROGRESS" -> KdsSlaYellow
    "READY"       -> KdsSlaBlue
    else          -> KdsSlaGray
}

/** Mała podpowiedź "Dotknij → GOTOWE" pod nagłówkiem */
@Composable
private fun TapHint(state: String) {
    val label = when (state) {
        "NEW", "ACKED", "IN_PROGRESS" -> "Dotknij → GOTOWE"
        "READY"                       -> "Dotknij → WYDAJ"
        else                          -> return
    }
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = nextStateColor(state).copy(alpha = 0.7f),
            letterSpacing = 0.5.sp
        )
    }
}

// ─── Helpers ────────────────────────────────────────────────────────────────

private fun slaColor(ticket: KdsTicket): Color {
    return when (ticket.state) {
        "CANCELLED"  -> SlaGray
        "HANDED_OFF" -> SlaBlue
        else -> {
            // Dla zaplanowanych — kolor bazuje na scheduledFor, nie slaTargetAt
            // (slaTargetAt to ~15 min od złożenia, dla zaplanowanych jest nieistotny)
            if (ticket.isScheduled()) {
                val scheduledMs = ticket.scheduledFor?.let {
                    runCatching { ZonedDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
                } ?: return ScheduledColor
                return scheduledTimerColor(scheduledMs - System.currentTimeMillis())
            }

            // Zwykłe zamówienia — standardowy SLA
            val slaMs = ticket.slaTargetAt?.let {
                runCatching { ZonedDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
            } ?: return SlaGreen

            val diffMs = slaMs - System.currentTimeMillis()
            when {
                diffMs >= 2 * 60_000L -> SlaGreen
                diffMs > 0            -> SlaYellow
                else                  -> SlaRed
            }
        }
    }
}

private fun stateLabel(state: String): String = when (state) {
    "NEW"         -> "Nowe"
    "ACKED"       -> "Przyjęte"
    "IN_PROGRESS" -> "W przygotowaniu"
    "READY"       -> "Gotowe"
    "HANDED_OFF"  -> "Wydane"
    "CANCELLED"   -> "Anulowane"
    else           -> state
}

private fun stateColor(state: String): Color = when (state) {
    "NEW"         -> Color(0xFF42A5F5)
    "ACKED"       -> KdsScheduled
    "IN_PROGRESS" -> KdsSlaYellow
    "READY"       -> KdsSlaGreen
    "HANDED_OFF"  -> KdsSlaBlue
    "CANCELLED"   -> KdsSlaGray
    else           -> KdsTextMuted
}

// ─── KOMPAKTOWY BLOCZEK KUCHENNY ─────────────────────────────────────────────
//
//  Uproszczony widok dla kuchni — maksymalnie dużo informacji w małej przestrzeni.
//  Nagłówek:  [NR WEW] [NR ZEW] [SOURCE]        [TYP]
//  Pozycje:   ×2  Hosomaki Tuna
//             ×1  Edamame
//
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun KdsCompactTicketCard(
    entry:            KdsTicketEntry,
    excludedKeywords: List<String>  = emptyList(),
    onStart:          () -> Unit    = {},
    onReady:          () -> Unit    = {},
    onHandoff:        () -> Unit    = {},
    onStartItem:      (String) -> Unit = {},
    onReadyItem:      (String) -> Unit = {},
    modifier:         Modifier      = Modifier
) {
    val ticket = entry.ticket
    val items = remember(entry.items, excludedKeywords) {
        entry.items
            .filter { item ->
                if (excludedKeywords.isEmpty()) return@filter true
                val n = item.displayName.trim().lowercase()
                excludedKeywords.none { kw -> n.contains(kw) }
            }
            .sortedBy { it.sequence }
    }

    val borderColor = slaColor(ticket)

    // Kolor i ikona typu zamówienia
    val (orderTypeIcon, orderTypeColor) = when (ticket.orderType?.lowercase()) {
        "delivery" -> "🚗" to Color(0xFF42A5F5)
        "pickup"   -> "🏠" to KdsSlaGreen
        "dine_in"  -> "🍽" to Color(0xFFAB47BC)
        else       -> "📋" to KdsTextMuted
    }
    val orderTypeLabel = when (ticket.orderType?.lowercase()) {
        "delivery" -> "DOSTAWA"
        "pickup"   -> "WYNOS"
        "dine_in"  -> "MIEJSCE"
        else       -> ticket.orderType?.uppercase() ?: "—"
    }

    // Etykieta source
    val sourceLabel = when (ticket.source?.uppercase()) {
        "CHECKOUT"   -> "WWW"
        "PORTAL"     -> "PORTAL"
        "WOOCOMMERCE"-> "WOO"
        "POS"        -> "POS"
        null         -> null
        else         -> ticket.source.uppercase().take(6)
    }

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(4.dp)),
        shape     = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors    = CardDefaults.cardColors(containerColor = KdsCard)
    ) {
        Column {

            // ── Nagłówek ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(orderTypeColor.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Lewa część: numer wew. + numer zew. + source
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Numer wewnętrzny KDS (np. W-003) — duży, bold
                    Text(
                        text  = ticket.kdsTicketNumber ?: ticket.orderNumber,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 18.sp
                        ),
                        color = KdsTextPrimary
                    )
                    // Numer zewnętrzny (orderNumber) — tylko gdy kdsTicketNumber jest dostępny
                    if (ticket.kdsTicketNumber != null) {
                        Text(
                            text  = "#${ticket.orderNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = KdsTextMuted
                        )
                    }
                    // Source chip
                    if (sourceLabel != null) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = KdsCardBorder
                        ) {
                            Text(
                                text     = sourceLabel,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                style    = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 9.sp
                                ),
                                color = KdsTextSecondary
                            )
                        }
                    }
                }

                // Prawa część: ikona + typ zamówienia
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(text = orderTypeIcon, fontSize = 13.sp)
                    Text(
                        text  = orderTypeLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize   = 11.sp
                        ),
                        color = orderTypeColor
                    )
                }
            }

            HorizontalDivider(color = borderColor.copy(alpha = 0.3f), thickness = 1.dp)

            // ── Lista pozycji ─────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                items.forEach { item ->
                    CompactItemRow(
                        item        = item,
                        onStartItem = { onStartItem(item.id) },
                        onReadyItem = { onReadyItem(item.id) }
                    )
                }
            }

            // ── Akcja (tylko w trybie gotowe/wydaj) ───────────────────
            val actionLabel = when (ticket.state) {
                "ACKED", "NEW"    -> null   // akcja przez tap pozycji
                "IN_PROGRESS"     -> "GOTOWE"
                "READY"           -> "WYDAJ"
                else              -> null
            }
            val actionColor = when (ticket.state) {
                "IN_PROGRESS" -> KdsSlaGreen
                "READY"       -> KdsSlaBlue
                else          -> KdsSlaGray
            }
            val actionFn: (() -> Unit)? = when (ticket.state) {
                "IN_PROGRESS" -> onReady
                "READY"       -> onHandoff
                else          -> null
            }

            if (actionLabel != null && actionFn != null) {
                HorizontalDivider(color = actionColor.copy(alpha = 0.15f), thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = actionFn)
                        .background(actionColor.copy(alpha = 0.08f))
                        .padding(vertical = 7.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = actionLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize   = 12.sp
                        ),
                        color = actionColor
                    )
                }
            }
        }
    }
}

// ─── Wiersz pozycji w widoku kompaktowym ─────────────────────────────────────

@Composable
private fun CompactItemRow(
    item:        KdsTicketItem,
    onStartItem: () -> Unit,
    onReadyItem: () -> Unit
) {
    val isDone = item.state == "READY" || item.state == "SERVED"
    val rowAction: (() -> Unit)? = when (item.state) {
        "QUEUED"  -> onStartItem
        "COOKING" -> onReadyItem
        else      -> null
    }
    val rowBg = when (item.state) {
        "COOKING" -> Color(0xFF1565C0).copy(alpha = 0.07f)
        "READY"   -> Color(0xFF2E7D32).copy(alpha = 0.07f)
        else      -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(rowBg)
            .then(if (rowAction != null) Modifier.clickable(onClick = rowAction) else Modifier)
            .padding(vertical = 3.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Ilość — kompaktowy badge
        Text(
            text  = "×${item.qty}",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize   = 13.sp
            ),
            color = if (isDone) KdsTextMuted else KdsSlaGreen
        )
        // Nazwa
        Text(
            text     = item.displayName,
            style    = MaterialTheme.typography.bodySmall.copy(
                fontWeight     = if (isDone) FontWeight.Normal else FontWeight.Medium,
                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                fontSize       = 13.sp
            ),
            color    = if (isDone) KdsTextMuted else KdsTextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )
        // Mały wskaźnik stanu
        when (item.state) {
            "COOKING" -> Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1565C0))
            )
            "READY" -> Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint     = KdsSlaGreen,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}




