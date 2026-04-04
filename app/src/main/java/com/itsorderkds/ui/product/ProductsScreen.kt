package com.itsorderkds.ui.product

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.itsorderkds.R
import com.itsorderkds.data.model.Addon
import com.itsorderkds.data.model.AddonAdminDto
import com.itsorderkds.data.model.Product
import com.itsorderkds.data.model.StockStatusEnum
import java.util.Locale

/**
 * Ekran listy produktów (stary widok - zastępowany przez CategoryProductsScreen)
 *
 * UWAGA: Edycja produktów wyłączona - dostępna tylko zmiana dostępności (Switch).
 * Implementacja edycji pozostawiona w kodzie ale nieużywana.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProductsScreen(
    modifier: Modifier = Modifier,
    viewModel: ProductsViewModel = hiltViewModel(),
    onProductClick: (String) -> Unit,  // NIEUŻYWANE - edycja wyłączona
    filterArg: String? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Obsługa argumentu nawigacji (np. z menu "Zamknięte")
    LaunchedEffect(filterArg) {
        if (filterArg == "disabled") {
            viewModel.onFilterToggle(true)
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = { viewModel.loadProducts() }
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Filtr pod paskiem górnym
        FilterSwitch(
            checked = uiState.showOnlyOutOfStock,
            onCheckedChange = { viewModel.onFilterToggle(it) },
            // Jeśli wymuszono filtr przez nawigację, można go tu zablokować lub zostawić odblokowany
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Box(modifier = Modifier.weight(1f).pullRefresh(pullRefreshState)) {
            when {
                // 1. Ładowanie i pusta lista (pierwsze wejście)
                uiState.isLoading && uiState.products.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                // 2. Błąd i pusta lista
                uiState.error != null && uiState.products.isEmpty() -> {
                    StatePlaceholder(
                        icon = Icons.Default.CloudOff,
                        title = "Wystąpił błąd",
                        subtitle = "Nie mogliśmy pobrać produktów.",
                        actionButtonText = "Spróbuj ponownie",
                        onActionClick = { viewModel.loadProducts() }
                    )
                }

                // 3. Pusta lista (Brak wyników LUB za krótki wpis)
                uiState.products.isEmpty() -> {
                    // Sprawdzamy, czy użytkownik wpisał coś, ale mniej niż 3 znaki
                    val isQueryTooShort =
                        uiState.searchQuery.isNotBlank() && uiState.searchQuery.length < 3

                    if (isQueryTooShort) {
                        StatePlaceholder(
                            icon = Icons.Default.Search,
                            title = "Wpisz więcej znaków",
                            subtitle = "Wpisz co najmniej 3 znaki, aby rozpocząć wyszukiwanie."
                        )
                    } else {
                        // To jest stan, gdy po prostu nic nie znaleziono (lub wyszukiwanie jest puste i lista pusta)
                        StatePlaceholder(
                            icon = Icons.Default.SentimentDissatisfied,
                            title = if (uiState.searchQuery.isBlank()) "Brak produktów" else "Brak wyników",
                            subtitle = if (uiState.searchQuery.isBlank())
                                "Brak produktów do wyświetlenia."
                            else
                                "Nie znaleziono pasujących produktów."
                        )
                    }
                }

                // 4. Lista produktów
                else -> {
                    ProductList(
                        products = uiState.products,
                        updatingProductIds = uiState.updatingProductIds,
                        updatingAddonIds = uiState.updatingAddonIds,
                        onProductClick = onProductClick,
                        onStockStatusChange = { product, newStatus ->
                            viewModel.updateStockStatus(product.id, newStatus)
                        },
                        // Callback teraz przekazuje Enum prosto do ViewModelu
                        onAddonStatusChange = { addonId, newStatus ->
                            viewModel.updateAddonStatus(addonId, newStatus)
                        }
                    )
                }
            }

            PullRefreshIndicator(
                refreshing = uiState.isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
@Composable
private fun ProductList(
    products: List<Product>,
    updatingProductIds: Set<String>,
    updatingAddonIds: Set<String>,
    onProductClick: (String) -> Unit,
    onStockStatusChange: (Product, StockStatusEnum) -> Unit,
    onAddonStatusChange: (String, StockStatusEnum) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products, key = { it.id }) { product ->
            ProductListItem(
                product = product,
                isUpdating = product.id in updatingProductIds,
                updatingAddonIds = updatingAddonIds,
                // WYŁĄCZONE: Edycja produktów - pozostawiona tylko zmiana dostępności
                onClick = { /* onProductClick(product.id) */ },
                onStockStatusChange = { newStatus ->
                    onStockStatusChange(product, newStatus)
                },
                onAddonStatusChange = onAddonStatusChange
            )
        }
    }
}

@Composable
private fun ProductListItem(
    product: Product,
    isUpdating: Boolean,
    updatingAddonIds: Set<String>,
    onClick: () -> Unit,
    onStockStatusChange: (StockStatusEnum) -> Unit,
    onAddonStatusChange: (String, StockStatusEnum) -> Unit
) {
    // ✅ Usunięto stan 'expanded', bo dodatki mają być zawsze widoczne
    val hasAddons = product.addonsGroup.isNotEmpty()

    // WYŁĄCZONE: Kliknięcie w produkt nie prowadzi do edycji
    // Pozostawiona tylko możliwość zmiany dostępności przez Switch
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
        // onClick wyłączone - brak edycji produktów
    ) {
        Column {
            // --- WIERSZ GŁÓWNY PRODUKTU ---
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.productThumbnail?.originalUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.image)
                        .error(R.drawable.image)
                        .build(),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.small)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f zł", product.price ?: 0.0),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ❌ Usunięto IconButton ze strzałką (nie jest już potrzebny)

                // Switch statusu głównego produktu
                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Switch(
                        checked = product.stockStatus == StockStatusEnum.IN_STOCK,
                        onCheckedChange = { isChecked ->
                            val newStatus = if (isChecked) StockStatusEnum.IN_STOCK else StockStatusEnum.OUT_OF_STOCK
                            onStockStatusChange(newStatus)
                        }
                    )
                }
            }

            // --- SEKCJA DODATKÓW (Zawsze widoczna jeśli są dodatki) ---
            if (hasAddons) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                ) {
                    // Iteracja po grupach
                    product.addonsGroup.forEach { group ->
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )

                        // Iteracja po dodatkach w grupie
                        group.addons.forEach { addon ->
                            AddonRow(
                                addon = addon,
                                isUpdating = addon.id in updatingAddonIds,
                                onStatusChange = { newStatus -> onAddonStatusChange(addon.id, newStatus) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// AddonRow pozostaje bez zmian, ale dla kompletności:
@Composable
private fun AddonRow(
    addon: AddonAdminDto,
    isUpdating: Boolean,
    onStatusChange: (StockStatusEnum) -> Unit // ⚠️ ZMIANA TYPU
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${addon.name} (+${String.format(Locale.getDefault(), "%.2f", addon.price ?: 0.0)} zł)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (isUpdating) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Switch(
                // ⚠️ ZMIANA: Sprawdzamy Enum
                checked = addon.stockStatus == StockStatusEnum.IN_STOCK,
                onCheckedChange = { isChecked ->
                    // ⚠️ ZMIANA: Konwertujemy Boolean -> Enum
                    val newStatus = if (isChecked) StockStatusEnum.IN_STOCK else StockStatusEnum.OUT_OF_STOCK
                    onStatusChange(newStatus)
                },
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun FilterSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        ListItem(
            headlineContent = { Text("Pokaż tylko wyłączone") },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filtr"
                )
            },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = isEnabled
                )
            },
            modifier = Modifier.clickable(enabled = isEnabled) { onCheckedChange(!checked) }
        )
    }
}

// Ta funkcja musi tu zostać, bo używamy jej w HomeActivity (TopAppBar)
@Composable
fun IntegratedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Ikona wyszukiwania",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box {
                    innerTextField()
                    if (query.isEmpty()) {
                        Text(
                            text = "Szukaj produktów…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun StatePlaceholder(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionButtonText != null && onActionClick != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onActionClick) {
                Text(actionButtonText)
            }
        }
    }
}
