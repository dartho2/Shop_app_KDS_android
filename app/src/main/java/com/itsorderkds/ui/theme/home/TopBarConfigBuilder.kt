@file:OptIn(ExperimentalMaterial3Api::class)

package com.itsorderkds.ui.theme.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.itsorderkds.R
import com.itsorderkds.service.SocketEventsRepository
import com.itsorderkds.ui.kds.KdsTopBarActions
import com.itsorderkds.ui.kds.KdsViewModel
import com.itsorderkds.ui.order.OrdersViewModel

/**
 * buildTopBarConfig — mapuje aktualną route na TopBarConfig.
 * Centralne miejsce konfiguracji TopBar dla każdego ekranu.
 */
@Composable
fun buildTopBarConfig(
    currentRoute: String,
    navController: NavHostController,
    onMenuClick: () -> Unit,
    ordersViewModel: OrdersViewModel,
    socketEventsRepo: SocketEventsRepository,
    kdsViewModel: KdsViewModel? = null
): TopBarConfig {
    val isHomeScreen = currentRoute == AppDestinations.HOME

    var config = TopBarConfig(
        title = { },
        navigationIcon    = if (isHomeScreen) NavigationIconType.MENU else NavigationIconType.BACK,
        onNavigationClick = if (isHomeScreen) onMenuClick else { { navController.navigateUp(); Unit } }
    )

    when {
        // ── HOME — KDS ────────────────────────────────────────────────────────
        currentRoute == AppDestinations.HOME -> {
            val kdsUiState  = kdsViewModel?.uiState?.collectAsStateWithLifecycle()?.value
            val kdsTickets  = kdsViewModel?.filteredTickets?.collectAsStateWithLifecycle()?.value
            val ticketCount = kdsTickets?.size ?: 0
            val isConnected = kdsUiState?.socketConnected ?: false
            val isLoading   = kdsUiState?.isLoading ?: false

            config = TopBarConfig(
                title = {
                    Column {
                        Text("KDS — Kuchnia")
                        Text(
                            text  = if (ticketCount == 0) "Brak aktywnych ticketów"
                                    else "$ticketCount aktywnych",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon    = NavigationIconType.MENU,
                onNavigationClick = onMenuClick,
                trailingContent = {
                    KdsTopBarActions(
                        isConnected = isConnected,
                        isLoading   = isLoading,
                        onRefresh   = { kdsViewModel?.loadActiveTickets() }
                    )
                }
            )
        }

        // ── Ustawienia ────────────────────────────────────────────────────────
        currentRoute == AppDestinations.SETTINGS_MAIN -> {
            config = TopBarConfig(
                title             = { Text(stringResource(R.string.settings_title)) },
                navigationIcon    = NavigationIconType.BACK,
                onNavigationClick = { navController.navigateUp() }
            )
        }

        currentRoute == AppDestinations.SETTINGS_MAIN_CONFIG -> {
            config = TopBarConfig(
                title             = { Text(stringResource(R.string.settings_main_title)) },
                navigationIcon    = NavigationIconType.BACK,
                onNavigationClick = { navController.navigateUp() }
            )
        }

        currentRoute == AppDestinations.SETTINGS_PRINT -> {
            config = TopBarConfig(
                title             = { Text(stringResource(R.string.settings_print_title)) },
                navigationIcon    = NavigationIconType.BACK,
                onNavigationClick = { navController.navigateUp() }
            )
        }

        currentRoute == AppDestinations.SETTINGS_LOGS -> {
            config = TopBarConfig(
                title             = { Text(stringResource(R.string.settings_logs_title)) },
                navigationIcon    = NavigationIconType.BACK,
                onNavigationClick = { navController.navigateUp() }
            )
        }

        currentRoute == AppDestinations.SETTINGS_NOTIFICATIONS -> {
            config = TopBarConfig(
                title             = { Text(stringResource(R.string.settings_notifications_title)) },
                navigationIcon    = NavigationIconType.BACK,
                onNavigationClick = { navController.navigateUp() }
            )
        }

        currentRoute == AppDestinations.PRINTERS_LIST -> {
            config = TopBarConfig(
                title             = { Text(stringResource(R.string.printers_manage_title)) },
                navigationIcon    = NavigationIconType.BACK,
                onNavigationClick = { navController.navigateUp() }
            )
        }

        currentRoute == AppDestinations.SERIAL_DIAGNOSTIC -> {
            config = TopBarConfig(
                title             = { Text("Serial Diagnostic") },
                navigationIcon    = NavigationIconType.BACK,
                onNavigationClick = { navController.navigateUp() }
            )
        }
    }

    return config
}


