package com.itsorderkds.ui.kds

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

// ─── Kolory SLA ─────────────────────────────────────────────────────────────

private val SlaGreen  = Color(0xFF2E7D32)
private val SlaYellow = Color(0xFFF9A825)
private val SlaRed    = Color(0xFFC62828)
private val SlaGray   = Color(0xFF757575)
private val SlaBlue   = Color(0xFF1565C0)
private val ScheduledColor = Color(0xFF6A1B9A)   // fioletowy dla zaplanowanych

// ─── Baner zaplanowanego zamówienia ─────────────────────────────────────────

/**
 * Baner wyświetlany na karcie gdy zamówienie ma ustaloną godzinę realizacji.
 * Pokazuje: czas zaplanowania + odliczanie do początku gotowania.
 */
@Composable
private fun ScheduledBanner(ticket: KdsTicket) {
    val sf = ticket.scheduledFor ?: return

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(60_000); nowMs = System.currentTimeMillis() }
    }

    val scheduledMs = runCatching {
        ZonedDateTime.parse(sf).toInstant().toEpochMilli()
    }.getOrNull() ?: return

    val diffMin = (scheduledMs - nowMs) / 60_000

    // Sformatuj godzinę realizacji w lokalnej strefie czasowej urządzenia (HH:mm)
    val scheduledTime = runCatching {
        ZonedDateTime.parse(sf)
            .withZoneSameInstant(java.time.ZoneId.systemDefault())
            .toLocalTime()
            .let { "%02d:%02d".format(it.hour, it.minute) }
    }.getOrElse { sf }

    val (bannerColor, countdownText) = when {
        diffMin > 120 -> ScheduledColor to "za ${diffMin / 60}h ${diffMin % 60}min"
        diffMin > 60  -> Color(0xFFE65100) to "za ${diffMin / 60}h ${diffMin % 60}min"  // zbliża się
        diffMin > 0   -> SlaRed to "za ${diffMin}min — zacznij gotować!"
        diffMin > -10 -> SlaRed to "CZAS ZACZĄĆ!"
        else          -> SlaGray to "spóźnione ${-diffMin}min"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(6.dp),
        color    = bannerColor.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint   = bannerColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text  = "Zaplanowane na $scheduledTime",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = bannerColor
                )
            }
            Text(
                text  = countdownText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = bannerColor
            )
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
    val items  = entry.items.sortedBy { it.sequence }

    // Czy za wcześnie na gotowanie (> 30 min do zaplanowanej godziny)
    val isFuture = ticket.isScheduledFuture()

    val borderColor by animateColorAsState(
        targetValue = if (isFuture) ScheduledColor else slaColor(ticket),
        animationSpec = tween(600),
        label = "sla_border"
    )

    Card(
        modifier = modifier
            .width(320.dp)
            .border(width = if (isFuture) 2.dp else 3.dp, color = borderColor, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFuture) 1.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFuture)
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // ─── Baner zaplanowanego zamówienia (jeśli dotyczy) ───────
            if (ticket.isScheduled()) {
                ScheduledBanner(ticket = ticket)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ─── Nagłówek ──────────────────────────────────────────────
            TicketHeader(ticket = ticket)

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // ─── Notatka do zamówienia ─────────────────────────────────
            if (!ticket.note.isNullOrBlank()) {
                NoteChip(note = ticket.note)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ─── Lista pozycji ─────────────────────────────────────────
            items.forEach { item ->
                KdsItemRow(
                    item = item,
                    onStartItem = { onStartItem(item.id) },
                    onReadyItem = { onReadyItem(item.id) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // ─── Przyciski akcji (zablokowane gdy za wcześnie) ─────────
            TicketActions(
                ticket    = ticket,
                isFuture  = isFuture,
                onAck     = onAck,
                onStart   = onStart,
                onReady   = onReady,
                onHandoff = onHandoff,
                onCancel  = onCancel
            )
        }
    }
}

// ─── Nagłówek karty ──────────────────────────────────────────────────────────

@Composable
private fun TicketHeader(ticket: KdsTicket) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            // Numer zamówienia
            Text(
                text = ticket.orderNumber,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                fontSize = 22.sp
            )
            // Stan ticketu
            Text(
                text = stateLabel(ticket.state),
                style = MaterialTheme.typography.labelMedium,
                color = stateColor(ticket.state)
            )
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
    // Odświeżamy co sekundę
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            nowMs = System.currentTimeMillis()
        }
    }

    val slaMs = ticket.slaTargetAt?.let {
        runCatching { ZonedDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
    } ?: return

    val diffMs  = slaMs - nowMs
    val diffSec = diffMs / 1_000
    val absSec  = kotlin.math.abs(diffSec)
    val mm      = absSec / 60
    val ss      = absSec % 60
    val sign    = if (diffSec < 0) "-" else ""
    val color   = slaColor(ticket)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "%s%02d:%02d".format(sign, mm, ss),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
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
    onStartItem: () -> Unit,
    onReadyItem: () -> Unit
) {
    val isDone = item.state == "READY" || item.state == "SERVED"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ilość
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "×${item.qty}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Nazwa + notatki
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isDone) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
            )
            if (item.notes.isNotEmpty()) {
                Text(
                    text = item.notes.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Przycisk akcji pozycji
        when (item.state) {
            "QUEUED" -> {
                ItemActionButton(label = "▶", tint = Color(0xFF1565C0), onClick = onStartItem)
            }
            "COOKING" -> {
                ItemActionButton(label = "✓", tint = Color(0xFF2E7D32), onClick = onReadyItem)
            }
            "READY" -> {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(24.dp)
                )
            }
            else -> { /* SERVED / VOID - brak przycisku */ }
        }
    }
}

@Composable
private fun ItemActionButton(label: String, tint: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = tint.copy(alpha = 0.12f),
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = tint
            )
        )
    }
}

// ─── Przyciski akcji ticketu ─────────────────────────────────────────────────

@Composable
private fun TicketActions(
    ticket: KdsTicket,
    isFuture: Boolean = false,
    onAck: () -> Unit,
    onStart: () -> Unit,
    onReady: () -> Unit,
    onHandoff: () -> Unit,
    onCancel: () -> Unit
) {
    // Zaplanowane zbyt daleko — blokujemy START, tylko ACK i CANCEL dostępne
    if (isFuture && ticket.state in listOf("NEW", "ACKED")) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(8.dp),
                color    = ScheduledColor.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint     = ScheduledColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text  = "Poczekaj na czas realizacji",
                        style = MaterialTheme.typography.bodySmall.copy(color = ScheduledColor)
                    )
                }
            }
            if (ticket.state == "NEW") {
                OutlinedButton(
                    onClick = onAck,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("PRZYJMIJ (zaplanowane)") }
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SlaRed)
            ) { Text("ANULUJ") }
        }
        return
    }

    when (ticket.state) {
        "NEW" -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAck,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PRZYJMIJ")
                }
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.FastForward, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("START")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SlaRed)
            ) {
                Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ANULUJ")
            }
        }

        "ACKED" -> {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.FastForward, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ZACZNIJ PRZYGOTOWANIE")
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SlaRed)
            ) {
                Text("ANULUJ")
            }
        }

        "IN_PROGRESS" -> {
            Button(
                onClick = onReady,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF9A825))
            ) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("GOTOWE", color = Color.Black)
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SlaRed)
            ) {
                Text("ANULUJ")
            }
        }

        "READY" -> {
            Button(
                onClick = onHandoff,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WYDAJ")
            }
        }

        "HANDED_OFF" -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = SlaBlue.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.DoneAll, null, tint = SlaBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wydane", color = SlaBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        "CANCELLED" -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = SlaGray.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Cancel, null, tint = SlaGray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Anulowane", color = SlaGray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────────────

private fun slaColor(ticket: KdsTicket): Color {
    return when (ticket.state) {
        "CANCELLED"  -> SlaGray
        "HANDED_OFF" -> SlaBlue
        else -> {
            val slaMs = ticket.slaTargetAt?.let {
                runCatching { ZonedDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
            } ?: return SlaGreen

            val nowMs = System.currentTimeMillis()
            val diffMs = slaMs - nowMs
            when {
                diffMs >= 2 * 60 * 1_000 -> SlaGreen   // > 2 min  → zielony
                diffMs > 0               -> SlaYellow  // 0..2 min → żółty
                else                     -> SlaRed      // < 0      → czerwony
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
    "NEW"         -> Color(0xFF1565C0)
    "ACKED"       -> Color(0xFF6A1B9A)
    "IN_PROGRESS" -> Color(0xFFF9A825)
    "READY"       -> Color(0xFF2E7D32)
    "HANDED_OFF"  -> SlaBlue
    "CANCELLED"   -> SlaGray
    else           -> Color.Gray
}




