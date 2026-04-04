package com.itsorderkds.ui.theme.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.itsorderkds.R

/**
 * NavigationIconType - typ ikony nawigacji w TopAppBar
 */
enum class NavigationIconType {
    MENU,   // Hamburger menu (Home ekran)
    BACK,   // Back arrow (pozostałe ekrany)
    NONE    // Brak ikony
}

/**
 * TopBarAction - definicja akcji (ikona) w TopAppBar
 */
data class TopBarAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)

/**
 * TopBarConfig - kompletna konfiguracja TopAppBar'a
 */
data class TopBarConfig(
    val title: @Composable () -> Unit,
    val navigationIcon: NavigationIconType = NavigationIconType.BACK,
    val actions: List<TopBarAction> = emptyList(),
    val trailingContent: (@Composable () -> Unit)? = null,
    val onNavigationClick: (() -> Unit)? = null
)

/**
 * AppTopBar - uniwersalny TopAppBar dla całej aplikacji
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(config: TopBarConfig) {
    TopAppBar(
        title = config.title,
        navigationIcon = {
            when (config.navigationIcon) {
                NavigationIconType.MENU -> {
                    IconButton(onClick = config.onNavigationClick ?: {}) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.common_menu),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                NavigationIconType.BACK -> {
                    IconButton(onClick = config.onNavigationClick ?: {}) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                NavigationIconType.NONE -> {
                    // Nie wyświetlaj nic
                }
            }
        },
        actions = {
            // Standardowe icon actions
            config.actions.forEach { action ->
                IconButton(onClick = action.onClick, enabled = action.enabled) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.contentDescription,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Custom trailing content (np. RestaurantStatusChip)
            config.trailingContent?.invoke()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

