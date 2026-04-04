package com.itsorderkds.ui.theme.status

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.itsorderkds.R
import com.itsorderkds.data.enums.RestaurantStatus
import com.itsorderkds.ui.order.SourceEnum

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantStatusSheet(
    status: RestaurantStatus?,
    message: String?,
    untilIso: String?,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onOpen: (portals: List<SourceEnum>?) -> Unit,
    onClose: (portals: List<SourceEnum>?) -> Unit,
    onPauseClick: () -> Unit,
    onClearPause: () -> Unit,
    onEditOpenHours: () -> Unit,
    onNavigateToDisabledProducts: () -> Unit
) {
    var showOpenDialog by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Status restauracji", style = MaterialTheme.typography.titleMedium)

            val isPaused = status == RestaurantStatus.PAUSED
            val isClosed = status == RestaurantStatus.CLOSED

            StatusActionRow(
                icon = Icons.Default.CheckCircle,
                title = stringResource(R.string.status_action_open_store),
                subtitle = stringResource(R.string.status_action_open_store_desc),
                enabled = isPaused || isClosed,
                borderColor = Color(0xFF4CAF50), // Zielone obramowanie
                onClick = { showOpenDialog = true }
            )
            StatusActionRow(
                icon = Icons.Default.Close,
                title = stringResource(R.string.status_action_close_store),
                subtitle = stringResource(R.string.status_action_close_store_desc),
                enabled = true, // Zawsze dostępne
                borderColor = Color(0xFFF44336), // Czerwone obramowanie
                onClick = { showCloseDialog = true }
            )
            StatusActionRow(
                icon = Icons.Default.FilterList,
                title = stringResource(R.string.status_action_pause),
                subtitle = stringResource(R.string.status_action_pause_desc),
                enabled = status != RestaurantStatus.PAUSED,
                onClick = onPauseClick
            )
            StatusActionRow(
                icon = Icons.Default.DoneAll,
                title = stringResource(R.string.status_action_clear_pause),
                subtitle = message ?: stringResource(R.string.status_action_clear_pause_desc),
                enabled = isPaused,
                onClick = onClearPause
            )

            Spacer(Modifier.height(8.dp))

            StatusActionRow(
                icon = Icons.Default.Menu,
                title = stringResource(R.string.status_action_edit_hours),
                subtitle = stringResource(R.string.status_action_edit_hours_desc),
                enabled = true,
                onClick = onEditOpenHours
            )

            // ✅ KROK 2: DODAJ NOWY PRZYCISK
            StatusActionRow(
                icon = Icons.Default.Fastfood, // Możesz zmienić na np. Icons.Default.ToggleOff
                title = stringResource(R.string.status_action_disabled_products),
                subtitle = stringResource(R.string.status_action_disabled_products_desc),
                enabled = true,
                onClick = onNavigateToDisabledProducts
            )

            Spacer(Modifier.height(12.dp))
        }
    }

    // Dialogi wyboru portali
    if (showOpenDialog) {
        OpenCloseStoreDialog(
            isOpenAction = true,
            onDismiss = { showOpenDialog = false },
            onConfirm = { portals ->
                onOpen(portals)
                showOpenDialog = false
            }
        )
    }

    if (showCloseDialog) {
        OpenCloseStoreDialog(
            isOpenAction = false,
            onDismiss = { showCloseDialog = false },
            onConfirm = { portals ->
                onClose(portals)
                showCloseDialog = false
            }
        )
    }
}

@Composable
private fun StatusActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    borderColor: Color? = null
) {
    val alpha = if (enabled) 1f else 0.4f
    val borderModifier = if (borderColor != null) {
        Modifier.border(
            width = 1.dp,
            color = if (enabled) borderColor else borderColor.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp)
        )
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .alpha(alpha)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (borderColor != null && enabled) borderColor else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
