package com.itsorderkds.ui.theme.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itsorderkds.data.enums.RestaurantStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Kompaktowy chip statusu restauracji do paska topAppBar.
 *
 * @param status    OPEN | PAUSED | CLOSED
 * @param untilIso  (opcjonalnie) ISO-8601 do kiedy trwa stan (np. koniec pauzy)
 * @param message   (opcjonalnie) komunikat (np. powód pauzy)
 * @param onClick   akcje po kliknięciu (otwarcie bottom-sheetu z opcjami)
 */
@Composable
fun RestaurantStatusChip(
    status: RestaurantStatus,
    modifier: Modifier = Modifier,
    untilIso: String? = null,
    message: String? = null,
    onClick: () -> Unit = {}
) {
    val (container, content) = when (status) {
        RestaurantStatus.OPEN   -> Color(0xFF4CAF50) to Color.White  // Zielony dla OPEN
        RestaurantStatus.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        RestaurantStatus.CLOSED -> Color(0xFFF44336) to Color.White  // Czerwony dla CLOSED
    }

    val (icon, label) = when (status) {
        RestaurantStatus.OPEN   -> Icons.Default.CheckCircle to "Otwarte"
        RestaurantStatus.PAUSED -> Icons.Default.FilterList  to "Pauza"
        RestaurantStatus.CLOSED -> Icons.Default.Close       to "Zamknięte"
    }

    val extra = when {
        status == RestaurantStatus.PAUSED && !untilIso.isNullOrBlank() -> {
            parseIsoToHour(untilIso)?.let { "do $it" }
        }
        status == RestaurantStatus.PAUSED && !message.isNullOrBlank() -> message
        else -> null
    }

    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label)
            Text(label, fontWeight = FontWeight.SemiBold)
            if (!extra.isNullOrBlank()) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = extra,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.alpha(0.9f)
                )
            }
        }
    }
}

private fun parseIsoToHour(iso: String): String? =
    runCatching {
        val dt = Instant.parse(iso).atZone(ZoneId.systemDefault())
        dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrNull()
