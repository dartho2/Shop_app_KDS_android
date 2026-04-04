package com.itsorderkds.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.itsorderkds.R
import com.itsorderkds.data.model.Product
import com.itsorderkds.data.model.StockStatusEnum
import java.util.Locale
import timber.log.Timber

/**
 * Ekran produktów dla konkretnej kategorii - drugi poziom nawigacji
 *
 * UWAGA: Edycja produktów wyłączona - dostępna tylko zmiana dostępności (Switch).
 * Implementacja edycji pozostawiona w kodzie ale nieużywana.
 *
 * Wyświetla produkty danej kategorii z możliwością włączania/wyłączania
 * produktów i dodatków
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CategoryProductsScreen(
    categoryId: String,
    categoryName: String,
    modifier: Modifier = Modifier,
    viewModel: ProductsViewModel = hiltViewModel(),
    onProductClick: (String) -> Unit  // NIEUŻYWANE - edycja wyłączona
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Filtruj produkty dla danej kategorii - BEZ remember, aby reaktywnie aktualizować!
    val categoryProducts = uiState.categoriesWithProducts
        .find { it.id == categoryId || it.slug == categoryId }
        ?.products
        ?: emptyList()

    // DEBUG: Log przy każdej rekomponowaniu
    Timber.d("🔄 CategoryProductsScreen recompose - categoryProducts count: ${categoryProducts.size}")
    categoryProducts.forEach { product ->
        Timber.d("  Product: ${product.name}, status: ${product.stockStatus}")
    }

    // Sprawdź czy dane są już załadowane (cache hit)
    val hasData = categoryProducts.isNotEmpty()
    val isActuallyLoading = uiState.isLoading && !hasData

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isActuallyLoading,
        onRefresh = { viewModel.loadCategories(forceRefresh = true) } // Force refresh
    )

    Box(modifier = modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        when {
            // Ładowanie - tylko jeśli NIE mamy danych w cache
            isActuallyLoading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // Błąd - tylko jeśli NIE mamy danych w cache
            uiState.error != null && !hasData -> {
                StatePlaceholder(
                    icon = Icons.Default.CloudOff,
                    title = stringResource(R.string.error_occurred),
                    subtitle = stringResource(R.string.error_fetch_products),
                    actionButtonText = stringResource(R.string.try_again),
                    onActionClick = { viewModel.loadCategories(forceRefresh = true) } // Force refresh
                )
            }

            // Pusta lista
            categoryProducts.isEmpty() -> {
                StatePlaceholder(
                    icon = Icons.Default.SentimentDissatisfied,
                    title = stringResource(R.string.no_products),
                    subtitle = stringResource(R.string.no_products_in_category, categoryName)
                )
            }

            // Lista produktów
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = categoryProducts,
                        key = { it.id }
                    ) { product ->
                        ProductListItem(
                            product = product,
                            isUpdating = product.id in uiState.updatingProductIds,
                            updatingAddonIds = uiState.updatingAddonIds,
                            // WYŁĄCZONE: Edycja produktów - pozostawiona tylko zmiana dostępności
                            onClick = { /* onProductClick(product.id) */ },
                            onStockStatusChange = { newStatus ->
                                viewModel.updateStockStatus(product.id, newStatus)
                            },
                            onAddonStatusChange = { addonId, newStatus ->
                                viewModel.updateAddonStatus(addonId, newStatus)
                            }
                        )
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isActuallyLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
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
                // Zdjęcie produktu
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

                // Nazwa i cena
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f zł", product.price ?: 0.0),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Switch statusu produktu
                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Switch(
                        checked = product.stockStatus == StockStatusEnum.IN_STOCK,
                        onCheckedChange = { isChecked ->
                            val newStatus = if (isChecked)
                                StockStatusEnum.IN_STOCK
                            else
                                StockStatusEnum.OUT_OF_STOCK
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
                    product.addonsGroup.forEach { group ->
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )

                        group.addons.forEach { addon ->
                            AddonRow(
                                addon = addon,
                                isUpdating = addon.id in updatingAddonIds,
                                onStatusChange = { newStatus ->
                                    onAddonStatusChange(addon.id, newStatus)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddonRow(
    addon: com.itsorderkds.data.model.AddonAdminDto,
    isUpdating: Boolean,
    onStatusChange: (StockStatusEnum) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = addon.name,
                style = MaterialTheme.typography.bodyMedium
            )
            if (addon.price != null && addon.price!! > 0) {
                Text(
                    text = "+${String.format(Locale.getDefault(), "%.2f zł", addon.price)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isUpdating) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        } else {
            Switch(
                checked = addon.stockStatus == StockStatusEnum.IN_STOCK,
                onCheckedChange = { isChecked ->
                    val newStatus = if (isChecked)
                        StockStatusEnum.IN_STOCK
                    else
                        StockStatusEnum.OUT_OF_STOCK
                    onStatusChange(newStatus)
                }
            )
        }
    }
}

