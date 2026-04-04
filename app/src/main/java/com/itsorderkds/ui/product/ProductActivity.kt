//// w pliku ui/product/ProductActivity.kt
//package com.itsorderkds.ui.product
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.SnackbarHost
//import androidx.compose.material3.SnackbarHostState
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextButton
//import androidx.compose.material3.TopAppBar
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Modifier
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import androidx.navigation.NavHostController
//import androidx.navigation.NavType
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.currentBackStackEntryAsState
//import androidx.navigation.compose.rememberNavController
//import androidx.navigation.navArgument
//import com.itsorderkds.ui.product.detail.ProductDetailScreen
//import com.itsorderkds.ui.product.detail.ProductDetailViewModel
//import com.itsorderkds.ui.settings.SettingsScreen
//import com.itsorderkds.ui.theme.GlobalMessageManager
//import com.itsorderkds.ui.theme.ItsOrderChatTheme
//import dagger.hilt.android.AndroidEntryPoint
//import javax.inject.Inject
//
//
//object ProductDestinations {
//    const val LIST_ROUTE = "product_list"
//    const val DETAIL_ROUTE_PREFIX = "product_detail"
//    const val DETAIL_ROUTE_TEMPLATE = "$DETAIL_ROUTE_PREFIX/{productId}"
//}
//
//@AndroidEntryPoint
//class ProductActivity : ComponentActivity() {
//    @Inject
//    lateinit var messageManager: GlobalMessageManager
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            ItsOrderChatTheme {
//                Surface(modifier = Modifier.fillMaxSize()) {
//                    val navController = rememberNavController()
//                    val snackbarHostState = remember { SnackbarHostState() }
//
//                    // Global message listener
//                    LaunchedEffect(Unit) {
//                        messageManager.messages.collect { uiMessage ->
//                            snackbarHostState.showSnackbar(
//                                message = uiMessage.text,
//                                duration = uiMessage.duration
//                            )
//                        }
//                    }
//
//                    // Top-level Scaffold just for the SnackbarHost
//
//
//                        // Main NavHost that switches between screens
//                        NavHost(
//                            navController = navController,
//                            startDestination = ProductDestinations.LIST_ROUTE,
//                            modifier = Modifier
//                        ) {
//                            composable(ProductDestinations.LIST_ROUTE) {
//                                // The list screen is a self-contained unit
//                                ProductListScaffold(
//                                    onNavigateToDetails = { productId ->
//                                        navController.navigate("${ProductDestinations.DETAIL_ROUTE_PREFIX}/$productId")
//                                    },
//                                    onNavigateBack = { finish() }
//                                )
//                            }
//
//                            composable(
//                                route = ProductDestinations.DETAIL_ROUTE_TEMPLATE,
//                                arguments = listOf(navArgument("productId") { type = NavType.StringType })
//                            ) {
//                                // The detail screen is also a self-contained unit
//                                ProductDetailScaffold(
//                                    onNavigateBack = { navController.navigateUp() }
//                                )
//                            }
//                        }
//
//                }
//            }
//        }
//    }
//}
//
//// Helper composable for the Product List screen
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun ProductListScaffold(
//    onNavigateToDetails: (String) -> Unit,
//    onNavigateBack: () -> Unit
//) {
//    val viewModel: ProductsViewModel = hiltViewModel()
//    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    IntegratedSearchBar(
//                        query = uiState.searchQuery,
//                        onQueryChange = viewModel::onSearchQueryChange
//                    )
//                },
//                navigationIcon = {
//                    IconButton(onClick = onNavigateBack) {
//                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        ProductsScreen(
//            modifier = Modifier.padding(padding),
//            viewModel = viewModel,
//            onProductClick = onNavigateToDetails
//        )
//    }
//}
//
//// Helper composable for the Product Detail screen
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun ProductDetailScaffold(
//    onNavigateBack: () -> Unit
//) {
//    // The ViewModel is created safely within this composable's scope,
//    // ensuring the `productId` argument is available.
//    val viewModel: ProductDetailViewModel = hiltViewModel()
//    val uiState by viewModel.uiState.collectAsState()
//
//    // Navigate back automatically after a successful save
//    LaunchedEffect(uiState.saveSuccess, onNavigateBack) {
//        if (uiState.saveSuccess) {
//            onNavigateBack()
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Edit Product") },
//                navigationIcon = {
//                    IconButton(onClick = onNavigateBack) {
//                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
//                    }
//                },
//                actions = {
//                    TextButton(
//                        onClick = { viewModel.saveProduct() },
//                        enabled = !uiState.isSaving
//                    ) {
//                        Text("Save")
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        ProductDetailScreen(
//            modifier = Modifier.padding(padding),
//            viewModel = viewModel
//        )
//    }
//}