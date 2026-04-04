package com.itsorderkds.ui.product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.itsorderkds.data.model.CategoryProduct

/**
 * Ekran listy kategorii - pierwszy poziom nawigacji
 *
 * Po kliknięciu kategorii przechodzi do CategoryProductsScreen
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CategoriesListScreen(
    modifier: Modifier = Modifier,
    viewModel: ProductsViewModel = hiltViewModel(),
    onCategoryClick: (categoryId: String, categoryName: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = { viewModel.loadCategories(forceRefresh = true) } // Force refresh
    )

    Box(modifier = modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        when {
            // Ładowanie
            uiState.isLoading && uiState.categoriesWithProducts.isEmpty() -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // Błąd
            uiState.error != null && uiState.categoriesWithProducts.isEmpty() -> {
                StatePlaceholder(
                    icon = Icons.Default.CloudOff,
                    title = stringResource(R.string.error_occurred),
                    subtitle = stringResource(R.string.error_fetch_categories),
                    actionButtonText = stringResource(R.string.try_again),
                    onActionClick = { viewModel.loadCategories(forceRefresh = true) } // Force refresh
                )
            }

            // Pusta lista
            uiState.categoriesWithProducts.isEmpty() -> {
                StatePlaceholder(
                    icon = Icons.Default.CloudOff,
                    title = stringResource(R.string.no_categories),
                    subtitle = stringResource(R.string.no_categories_subtitle)
                )
            }

            // Lista kategorii
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.categoriesWithProducts,
                        key = { it.id ?: it.slug ?: "unknown" }
                    ) { category ->
                        CategoryListItem(
                            category = category,
                            onClick = {
                                onCategoryClick(
                                    category.id ?: category.slug ?: "unknown",
                                    category.name ?: "Unknown Category"
                                )
                            }
                        )
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = uiState.isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun CategoryListItem(
    category: CategoryProduct,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ikona kategorii
            if (category.categoryIcon != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(category.categoryIcon.originalUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.image)
                        .error(R.drawable.image)
                        .build(),
                    contentDescription = category.name ?: "Category",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.small)
                )
            } else {
                // Placeholder jeśli brak ikony
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.small),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (category.name ?: "?").take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Nazwa kategorii
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name ?: "Unknown Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = stringResource(
                        R.string.products_count,
                        category.products?.size ?: 0
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.open_category),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

