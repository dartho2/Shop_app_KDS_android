package com.itsorderkds.ui.theme.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itsorderkds.R
import com.itsorderkds.service.SocketEventsRepository

/**
 * Wskaźnik statusu połączenia Socket.IO
 *
 * Wyświetla kolorową kropkę:
 * - Zielona: Socket połączony, odbiera zamówienia
 * - Czerwona: Socket rozłączony, brak połączenia
 */
@Composable
fun SocketStatusIndicator(
    socketEventsRepo: SocketEventsRepository,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false
) {
    val isConnected by socketEventsRepo.connection.collectAsStateWithLifecycle(initialValue = false)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kolorowa kropka
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (isConnected) {
                        Color(0xFF4CAF50) // Zielony - połączony
                    } else {
                        Color(0xFFF44336) // Czerwony - rozłączony
                    }
                )
        )

        // Opcjonalny tekst obok kropki
        if (showLabel) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isConnected) {
                    stringResource(R.string.socket_connected)
                } else {
                    stringResource(R.string.socket_disconnected)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

