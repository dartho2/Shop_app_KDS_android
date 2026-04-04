package com.itsorderkds.ui.theme.status

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.itsorderkds.R
import com.itsorderkds.ui.order.SourceEnum

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
 * Dialog do otwierania lub zamykania sklepu z wyborem portali
 *
 * @param isOpenAction true = otwieranie sklepu, false = zamykanie sklepu
 * @param onDismiss callback po zamknięciu dialogu
 * @param onConfirm callback po potwierdzeniu (portals: null = wszystkie portale, lista = wybrane portale)
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OpenCloseStoreDialog(
    isOpenAction: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (portals: List<SourceEnum>?) -> Unit
) {
    var selectedPortals by remember { mutableStateOf<Set<SourceEnum>>(setOf(SourceEnum.ITS)) }

    val portalsToSend: List<SourceEnum>? = if (selectedPortals.isEmpty()) {
        null // Akcja globalna (wszystkie portale)
    } else {
        selectedPortals.toList() // Akcja na wybrane portale
    }

    val title = if (isOpenAction) {
        stringResource(R.string.status_action_open_store)
    } else {
        stringResource(R.string.status_action_close_store)
    }

    val subtitleText = if (portalsToSend == null) {
        if (isOpenAction) {
            stringResource(R.string.open_all_portals)
        } else {
            stringResource(R.string.close_all_portals)
        }
    } else {
        if (isOpenAction) {
            stringResource(R.string.open_selected_portals, portalsToSend.joinToString { it.name })
        } else {
            stringResource(R.string.close_selected_portals, portalsToSend.joinToString { it.name })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. SELEKTOR PORTALI
                Text(
                    stringResource(R.string.select_portals_to_change),
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(portalsToSend)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOpenAction) {
                        Color(0xFF4CAF50) // Zielony dla otwórz
                    } else {
                        Color(0xFFF44336) // Czerwony dla zamknij
                    }
                )
            ) {
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

/**
 * Przycisk-Logo z nakładką 'check'
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
                    contentDescription = stringResource(R.string.selected),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Funkcja mapująca Enumy na zasoby
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

