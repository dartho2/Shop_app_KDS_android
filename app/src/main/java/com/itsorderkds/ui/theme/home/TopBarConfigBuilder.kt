@file:OptIn(ExperimentalMaterial3Api::class)

package com.itsorderkds.ui.theme.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.itsorderkds.R
import com.itsorderkds.data.responses.UserRole
import com.itsorderkds.service.SocketEventsRepository
import com.itsorderkds.ui.order.OrdersViewModel
import com.itsorderkds.ui.theme.home.components.SocketStatusIndicator


/**
 * buildTopBarConfig - mapuje aktualną route na TopBarConfig
 *
 * To jest CENTRALNE MIEJSCE do definiowania topBar'a dla każdego ekranu.
 * Dodaj nowy ekran? Dodaj case tutaj.
 *
 * @param currentRoute Aktualna trasa nawigacji
 * @param navController NavHostController do nawigacji
 * @param onMenuClick Callback dla Menu (drawer)
 * @param ordersViewModel OrdersViewModel dla danych (courier, shift status itp.)
 * @param onCheckOut Callback dla checkout'u
 * @param socketEventsRepo Repository dla statusu połączenia Socket.IO
 */
@Composable
fun buildTopBarConfig(
    currentRoute: String,
    navController: NavHostController,
    onMenuClick: () -> Unit,
    ordersViewModel: OrdersViewModel,
    socketEventsRepo: SocketEventsRepository
): TopBarConfig {
    val ordersUiState = ordersViewModel.uiState.collectAsStateWithLifecycle().value
    val isCourier = ordersUiState.userRole == UserRole.COURIER
    val isHomeScreen = currentRoute == AppDestinations.HOME

    // Domyślna konfiguracja (dla większości ekranów)
    var config = TopBarConfig(
        title = { /* Default title - will be overridden */ },
        navigationIcon = if (isHomeScreen) NavigationIconType.MENU else NavigationIconType.BACK,
        onNavigationClick = if (isHomeScreen) onMenuClick else { { navController.navigateUp(); Unit } }
    )

    // Specyficzne konfiguracje dla poszczególnych ekranów
    when {
        // HOME ekran - bez tytułu, Menu + RestaurantStatusChip (dla Staff)
        currentRoute == AppDestinations.HOME -> {
            config = TopBarConfig(
                title = { /* Pusty tytuł na Home */ },
                navigationIcon = NavigationIconType.MENU,
                onNavigationClick = onMenuClick,
                actions = buildHomeActions(isHomeScreen,  ordersViewModel),
                trailingContent = if (!isCourier && isHomeScreen) {
                    {
                        // RestaurantStatusActionItem dla Staff (z wskaźnikiem Socket)
                        RestaurantStatusActionItem(
                            ordersViewModel = ordersViewModel,
                            navController = navController,
                            socketEventsRepo = socketEventsRepo
                        )
                    }
                } else null
            )
        }

        // SETTINGS MAIN
        currentRoute == AppDestinations.SETTINGS_MAIN -> {
            config = TopBarConfig(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = NavigationIconType.BACK,
                onNavigationClick = { navController.navigateUp() }
            )
        }

        // SETTINGS: MAIN CONFIG
        currentRoute == AppDestinations.SETTINGS_MAIN_CONFIG -> {
            config = TopBarConfig(
                title = { Text(stringResource(R.string.settings_main_title)) },
                navigationIcon = NavigationIconType.BACK,
                onNavigationClick = { navController.navigateUp() }
            )
        }

        // SETTINGS: PRINT
        currentRoute == AppDestinations.SETTINGS_PRINT -> {
            config = TopBarConfig(
                title = { Text(stringResource(R.string.settings_print_title)) },
                navigationIcon = NavigationIconType.BACK,
                onNavigationClick = { navController.navigateUp() }
            )
        }

        // SETTINGS: LOGS
        currentRoute == AppDestinations.SETTINGS_LOGS -> {
            config = TopBarConfig(
                title = { Text(stringResource(R.string.settings_logs_title)) },
                navigationIcon = NavigationIconType.BACK,
                onNavigationClick = { navController.navigateUp() }
            )
        }


        // SETTINGS: NOTIFICATIONS
        currentRoute == AppDestinations.SETTINGS_NOTIFICATIONS -> {
            config = TopBarConfig(
                title = { Text(stringResource(R.string.settings_notifications_title)) },
                navigationIcon = NavigationIconType.BACK,
                onNavigationClick = { navController.navigateUp() }
            )
        }

        // SETTINGS: PRINTERS
        currentRoute == AppDestinations.PRINTERS_LIST -> {
            config = TopBarConfig(
                title = { Text(stringResource(R.string.printers_manage_title)) },
                navigationIcon = NavigationIconType.BACK,
                onNavigationClick = { navController.navigateUp() }
            )
        }

        // SETTINGS: SERIAL DIAGNOSTIC
        currentRoute == AppDestinations.SERIAL_DIAGNOSTIC -> {
            config = TopBarConfig(
                title = { Text("Serial Diagnostic") },
                navigationIcon = NavigationIconType.BACK,
                onNavigationClick = { navController.navigateUp() }
            )
        }
    }

    return config
}

/**
 * buildHomeActions - buduje actions dla Home ekranu
 *
 * Zwraca odpowiednie actions w zależności od roli użytkownika i stanu aplikacji
 */
@Composable
private fun buildHomeActions(
    isHomeScreen: Boolean,
    ordersViewModel: OrdersViewModel,
): List<TopBarAction> {
    val ordersUiState = ordersViewModel.uiState.collectAsStateWithLifecycle().value
    val actions = mutableListOf<TopBarAction>()

    return actions
}

/**
 * RestaurantStatusActionItem - wyświetla status restauracji i wskaźnik połączenia Socket
 *
 * Composable używany w TopBar na Home ekranie dla Staff.
 * Pokazuje:
 * - Wskaźnik Socket.IO (zielona/czerwona kropka)
 * - Chip ze statusem restauracji (otwarta/zamknięta/pauza)
 */
@Composable
private fun RestaurantStatusActionItem(
    ordersViewModel: OrdersViewModel,
    navController: NavHostController,
    socketEventsRepo: SocketEventsRepository
) {

    var showStatusSheet by remember { mutableStateOf(false) }
    var showPauseDialog by remember { mutableStateOf(false) }

    // Row z wskaźnikiem Socket i statusem restauracji
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Wskaźnik połączenia Socket.IO
        SocketStatusIndicator(
            socketEventsRepo = socketEventsRepo,
            showLabel = false // Tylko kropka, bez tekstu
        )

        // Status restauracji

    }
}

