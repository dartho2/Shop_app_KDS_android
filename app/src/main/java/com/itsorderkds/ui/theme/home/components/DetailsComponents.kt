package com.itsorderkds.ui.theme.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.itsorderkds.data.model.OrderTras
import com.itsorderkds.ui.order.OrderStatusEnum
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
@Composable
fun TimelineNode(
    isFirst: Boolean,
    isLast: Boolean,
    status: OrderStatusEnum,
    isReturn: Boolean // Kluczowy parametr do identyfikacji powrotu
) {
    // Twoja paleta kolorów została zachowana
    val activeGreenColor = Color(0xFF388E3C)
    val inactiveColor = Color.LightGray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight()
    ) {
        // Górna linia - ujednolicona grubość
        Box(
            modifier = Modifier
                .weight(1f) // ZMIANA: Nadajemy jej elastyczną wagę
                .width(2.dp)
                .background(if (isFirst) Color.Transparent else inactiveColor)
        )

        // Ikona - teraz dynamicznie wybiera domek lub status
        Icon(
            imageVector = if (isReturn) {
                Icons.Default.Home // Ikona domku dla powrotu do restauracji
            } else {
                when (status) { // Twoja logika ikon dla statusów
                    OrderStatusEnum.OUT_FOR_DELIVERY -> Icons.Filled.RadioButtonChecked
                    OrderStatusEnum.COMPLETED -> Icons.Filled.CheckCircle
                    else -> Icons.Filled.RadioButtonUnchecked
                }
            },
            contentDescription = "Status Trasy",
            tint = when (status) { // Twoja logika kolorów
                OrderStatusEnum.OUT_FOR_DELIVERY, OrderStatusEnum.COMPLETED -> activeGreenColor
                else -> Color.Gray
            },
            modifier = Modifier.size(24.dp) // Zwiększony rozmiar dla lepszej widoczności
        )

        // Dolna linia - ujednolicona grubość
        Box(
            modifier = Modifier
                .weight(1f) // ZMIANA: Ta linia już miała wagę, teraz obie są równe
                .width(2.dp)
                .background(if (isLast) Color.Transparent else inactiveColor)
        )
    }
}
@Composable
fun DetailItem(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    isImportant: Boolean = false,
    color: Color = Color.Unspecified // Domyślnie używa koloru z TextStyle
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Opis nie jest konieczny dla ikon dekoracyjnych
            modifier = Modifier.size(if (isImportant) 20.dp else 16.dp),
            tint = if (isImportant) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column {
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = text,
                style = if (isImportant) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                fontWeight = if (isImportant) FontWeight.Normal else FontWeight.SemiBold,
                color = if (color != Color.Unspecified) color else Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DelayIndicator(delayMinutes: Int) {
    val (text, color) = when {
        delayMinutes > 3 -> "+$delayMinutes min" to Color(0xFFD32F2F) // Czerwony
        delayMinutes < -3 -> "$delayMinutes min" to Color(0xFF388E3C) // Zielony
        else -> "W czasie" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun StatusBadge(status: OrderStatusEnum) {
    // Ta data class pomaga w czytelny sposób zarządzać wyglądem statusu
    data class StatusDisplayInfo(
        val text: String,
        val color: Color,
        val icon: androidx.compose.ui.graphics.vector.ImageVector
    )

    val displayInfo = when (status) {
        OrderStatusEnum.ACCEPTED -> StatusDisplayInfo("", Color(0xFF1976D2), Icons.Default.ThumbUp)
        OrderStatusEnum.OUT_FOR_DELIVERY -> StatusDisplayInfo(
            "",
            Color(0xFF388E3C),
            Icons.Default.LocalShipping
        )

        OrderStatusEnum.COMPLETED -> StatusDisplayInfo("", Color.Gray, Icons.Default.CheckCircle)
        else -> StatusDisplayInfo("", Color.DarkGray, Icons.Default.Info)
    }

    Surface(
        shape = RoundedCornerShape(50), // Kształt pigułki
        color = displayInfo.color.copy(alpha = 0.1f),
        contentColor = displayInfo.color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = displayInfo.icon,
                contentDescription = "Status",
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = displayInfo.text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

//@Composable
//fun TimeInfoSection(item: OrderTras) {
//    fun formatDate(dateString: String?): String {
//        if (dateString.isNullOrBlank()) return "N/A"
//        return try {
//            ZonedDateTime.parse(dateString).withZoneSameInstant(ZoneId.systemDefault())
//                .format(DateTimeFormatter.ofPattern("HH:mm"))
//        } catch (e: Exception) {
//            "Błąd"
//        }
//    }
//    Surface(
//        shape = MaterialTheme.shapes.medium,
//        modifier = Modifier.fillMaxWidth()
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 12.dp, vertical = 8.dp),
//            horizontalArrangement = Arrangement.SpaceAround,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            if (item.isAsap) {
//                DetailItem(icon = Icons.Default.Bolt, label = "Tryb", text = "ASAP")
//            } else {
//                DetailItem(
//                    icon = Icons.Default.Schedule,
//                    label = "Planowo",
//                    text = formatDate(item.plannedDeliveryTime)
//                )
//            }
//            DetailItem(
//                icon = Icons.Default.Flag,
//                label = "Start",
//                text = formatDate(item.estimatedStartTime)
//            )
//            DetailItem(icon = Icons.Default.Timer, label = "ETA", text = formatDate(item.eta))
//        }
//    }
//}