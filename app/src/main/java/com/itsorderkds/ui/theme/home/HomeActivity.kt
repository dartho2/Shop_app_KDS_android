package com.itsorderkds.ui.theme.home


import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.itsorderkds.LoginActivity
import com.itsorderkds.R
import com.itsorderkds.data.model.ShiftCheckOut
import com.itsorderkds.data.responses.UserRole
import com.itsorderkds.data.socket.SocketAction
import com.itsorderkds.service.SocketService
import com.itsorderkds.ui.kds.KdsScreen
import com.itsorderkds.ui.loading.AppLoadingScreen
//import com.itsorderkds.ui.order.OrderEvent
//import com.itsorderkds.ui.order.OrderRouteState
import com.itsorderkds.ui.order.OrderStatusEnum
//import com.itsorderkds.ui.order.OrdersViewModel
import com.itsorderkds.ui.settings.MainSettingsScreen
import com.itsorderkds.ui.settings.NotificationSettingsScreen
import com.itsorderkds.ui.settings.PermissionsScreen
import com.itsorderkds.ui.settings.PrintSettingsScreen
import com.itsorderkds.ui.settings.SettingsMainScreen
import com.itsorderkds.ui.settings.log.LogsScreen
import com.itsorderkds.ui.startNewActivity
import com.itsorderkds.ui.theme.GlobalMessageManager
import com.itsorderkds.ui.theme.ItsOrderChatTheme
import com.itsorderkds.ui.theme.home.components.DrawerContent
import com.itsorderkds.ui.theme.home.components.ShiftStatusAction
import com.itsorderkds.ui.util.planRouteOnMap
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

object AppDestinations {
    // Główny ekran KDS
    const val HOME = "home"

    // Ustawienia
    const val SETTINGS_MAIN = "settings_main"
    const val SETTINGS_MAIN_CONFIG = "settings_main_config"
    const val SETTINGS_PRINT = "settings_print"
    const val SETTINGS_LOGS = "settings_logs"
    const val SETTINGS_NOTIFICATIONS = "settings_notifications"
    const val SETTINGS_PERMISSIONS = "settings_permissions"

    // Drukarki
    const val PRINTERS_LIST = "printers_list"
    const val SERIAL_DIAGNOSTIC = "serial_diagnostic"
}

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
//    private val ordersViewModel: OrdersViewModel by viewModels()

    // 🎯 Task Management: NavController do sprawdzenia backstack
    private var navController: NavHostController? = null

    // 🎯 Task Management: Callback dla onBackPressed z Composable
    var onBackPressedCallback: (() -> Unit)? = null

    companion object {
        const val EXTRA_SKIP_INITIAL_LOADER = "skip_initial_loader"
    }

    @Inject
    lateinit var userPreferences: com.itsorderkds.data.preferences.UserPreferences

    @Inject
    lateinit var tokenProvider: com.itsorderkds.data.network.preferences.TokenProvider

    @Inject
    lateinit var messageManager: GlobalMessageManager

    @Inject
    lateinit var appPreferencesManager: com.itsorderkds.data.preferences.AppPreferencesManager

    @Inject
    lateinit var socketEventsRepo: com.itsorderkds.service.SocketEventsRepository

    override fun onResume() {
        Timber.tag(TAG).d("[emit] Wlacza Home] mozliwe ze otrzymane Order")
        super.onResume()

        // ✅ Reset flagi Task Reopen gdy Activity wraca na pierwszy plan
        isReopeningTask = false

        lifecycleScope.launch {
            try {
                val kioskModeEnabled = appPreferencesManager.isKioskModeEnabled()
                if (kioskModeEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startLockTask()
                    Timber.d("🔒 Kiosk Mode WŁĄCZONY - aplikacja zablokowana")
                } else {
                    Timber.d("✅ Kiosk Mode wyłączony")
                }
            } catch (e: Exception) {
                Timber.w("⚠️ Błąd przy włączaniu Kiosk Mode")
            }
        }
    }

    // ✅ Flaga zapobiegająca pętli Task Reopen
    @Volatile
    private var isReopeningTask = false

    override fun onPause() {
        super.onPause()
        Timber.d("📱 onPause() - aplikacja weszła w background")
        // ❌ USUNIĘTE: Task Reopen tutaj powoduje nieskończoną pętlę!
        // Task Reopen przeniesiony do onStop()
    }

    override fun onStop() {
        super.onStop()
        Timber.d("⏸️ onStop() - aplikacja całkowicie w tle")

        // ✅ CRITICAL: Sprawdź flagę aby uniknąć pętli
        if (isReopeningTask) {
            Timber.d("⏭️ Task Reopen już w toku - pomijam aby uniknąć pętli")
            return
        }

        lifecycleScope.launch {
            // Sprawdź czy Task Reopen jest włączony
            val taskReopenEnabled = appPreferencesManager.isTaskReopenEnabled()

            if (taskReopenEnabled && !isFinishing && !isChangingConfigurations) {
                Timber.d("🔄 Task Reopen włączony - przywracanie aplikacji z taska...")

                // ✅ Ustaw flagę aby zapobiec ponownemu wywołaniu
                isReopeningTask = true

                // Małe opóźnienie aby system zdążył przetworzyć stop
                kotlinx.coroutines.delay(200)

                // Przywróć aplikację na pierwszy plan
                val intent = Intent(this@HomeActivity, HomeActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)

                Timber.tag("HomeActivity\$onPause").d("✅ Aplikacja przywrócona na widok z background")

                // Reset flagi po pewnym czasie (gdyby coś poszło nie tak)
                kotlinx.coroutines.delay(1000)
                isReopeningTask = false
            } else {
                Timber.d("⏸️ Task Reopen wyłączony lub Activity finishing - aplikacja pozostaje w tle")
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, SocketService::class.java))
        Timber.tag(TAG).d("[emit] Wlacza onCreate] mozliwe ze wlacza socketService")

        // Rejestruj receiver na FORCE_LOGOUT
        val filter = IntentFilter(SocketService.ACTION_FORCE_LOGOUT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logoutReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(logoutReceiver, filter)
        }

        // 🎯 NOWE: Rejestruj receiver na PACKAGE_RESTARTED
        val restartFilter = IntentFilter(Intent.ACTION_PACKAGE_RESTARTED)
        restartFilter.addDataScheme("package")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(restartReceiver, restartFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(restartReceiver, restartFilter)
        }
        Timber.d("🔄 Registered PACKAGE_RESTARTED receiver")

//        handleIntentExtras(intent)
        setContent {
            ItsOrderChatTheme {
                MainAppContainer(
//                    ordersViewModel = ordersViewModel,
                    messageManager = messageManager,
                    socketEventsRepo = socketEventsRepo,
                    onLogout = { logout() },
                    onNavControllerReady = { navController = it }  // 🎯 Przypisz do Activity
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(logoutReceiver)
            unregisterReceiver(restartReceiver)
            Timber.d("✅ Receivers wyrejestrowane")
        } catch (e: Exception) {
            // Ignorujemy, jeśli nie były zarejestrowane
        }

        lifecycleScope.launch {
            try {
                val autoRestartEnabled = appPreferencesManager.isAutoRestartEnabled()
                if (autoRestartEnabled) {
                    Timber.d("⚠️ HomeActivity destroyed - Auto-restart WŁĄCZONY")
                    val restartIntent = Intent(applicationContext, HomeActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

                    Thread {
                        Thread.sleep(500)
                        applicationContext.startActivity(restartIntent)
                        Timber.d("✅ Aplikacja uruchomiona ponownie (Auto-restart)")
                    }.start()
                } else {
                    Timber.d("🛑 HomeActivity destroyed - Auto-restart wyłączony")
                }
            } catch (e: Exception) {
                Timber.w(e, "⚠️ Błąd w onDestroy - Auto-restart")
            }
        }
    }

//    override fun onNewIntent(intent: Intent?) {
//        super.onNewIntent(intent)
//        Timber.tag(TAG).d("[emit] Otrzymano onNewIntent")
//        setIntent(intent)
//        handleIntentExtras(intent)
//    }

//    private fun handleIntentExtras(intent: Intent?) {
//        intent?.getStringExtra(SocketAction.Extra.ORDER_JSON)?.let { orderJson ->
//            Timber.tag(TAG).d("HomeActivity przechwyciło orderJson z Intentu.")
//            ordersViewModel.handleNewOrderFromIntent(orderJson)
//            intent.removeExtra(SocketAction.Extra.ORDER_JSON)
//        }
//    }




    private fun logout() = lifecycleScope.launch {
        userPreferences.clear()
        applicationContext.cacheDir.deleteRecursively()
        stopService(Intent(applicationContext, SocketService::class.java))
        startNewActivity(LoginActivity::class.java)
        finish()
    }

    // 2. Definiujemy Odbiornik (Receiver)
    private val logoutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SocketService.ACTION_FORCE_LOGOUT) {
                Timber.tag(TAG).e("Otrzymano sygnał FORCE_LOGOUT z SocketService")
                // Wywołujemy Twoją istniejącą funkcję logout()
                logout()
            }
        }
    }

    // 🎯 NOWE: Receiver dla restartu aplikacji
    private val restartReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_PACKAGE_RESTARTED) {
                Timber.d("🔄 Otrzymano sygnał PACKAGE_RESTARTED - uruchamianie aplikacji ponownie")
                // Uruchom aplikację ponownie
                val restartIntent = Intent(context, com.itsorderkds.MainActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context?.startActivity(restartIntent)
            }
        }
    }

    // 🎯 TASK MANAGEMENT: Inteligentne cofnięcie
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // Użyj callback z Composable'a aby sprawdzić route
        onBackPressedCallback?.invoke() ?: run {
            // Fallback: jeśli callback nie ustawiony, zwykłe zamknięcie
            super.onBackPressed()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
//    ordersViewModel: OrdersViewModel,
    messageManager: GlobalMessageManager,
    socketEventsRepo: com.itsorderkds.service.SocketEventsRepository,
    onLogout: () -> Unit,
    onNavControllerReady: (NavHostController) -> Unit = {}
) {
    val kdsViewModel: com.itsorderkds.ui.kds.KdsViewModel = hiltViewModel()
    val kdsUiState by kdsViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    // 🎯 Pobierz aktualną route
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppDestinations.HOME

    // 🎯 Pobierz Activity context
    val activity = LocalContext.current as? HomeActivity

    // 🎯 Przekaż NavController do Activity
    LaunchedEffect(navController) {
        onNavControllerReady(navController)
    }

    // 🎯 Ustawianie callback dla onBackPressed
    LaunchedEffect(currentRoute, navController, activity) {
        activity?.onBackPressedCallback = {
            if (currentRoute == AppDestinations.HOME) {
                // Jesteśmy na HOME - zrzuć do taska
                activity.moveTaskToBack(true)
                Timber.d("📱 onBackPressed() HOME → zrzucona do taska")
            } else {
                // Jesteśmy na innym ekranie - nawiguj wstecz
                navController.navigateUp()
                Timber.d("⬅️ onBackPressed() ${currentRoute} → navigateUp()")
            }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute == AppDestinations.HOME,
        drawerContent = {
            DrawerContent(
                currentRoute    = currentRoute,
                userName        = "",
                userRole        = null,
                onLogout        = {
                    onLogout()
                    scope.launch { drawerState.close() }
                },
                onNavigateToHome = {
                    navController.navigate(AppDestinations.HOME) {
                        popUpTo(AppDestinations.HOME) { inclusive = true }
                    }
                    scope.launch { drawerState.close() }
                },
                onNavigateToSettings = {
                    navController.navigate(AppDestinations.SETTINGS_MAIN)
                    scope.launch { drawerState.close() }
                },
                onCloseDrawer = { scope.launch { drawerState.close() } },
                showHome = true
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            NavHost(
                navController    = navController,
                startDestination = AppDestinations.HOME,
                modifier         = Modifier.padding(innerPadding)
            ) {
                composable(AppDestinations.HOME) {
                    KdsScreen(
                        onNavigateToSettings = {
                            navController.navigate(AppDestinations.SETTINGS_MAIN)
                        }
                    )
                }
                composable(AppDestinations.SETTINGS_MAIN) {
                    SettingsMainScreen(
                        onNavigateToNotificationSettings = { navController.navigate(AppDestinations.SETTINGS_NOTIFICATIONS) },
                        onNavigateToPrintersList         = { navController.navigate(AppDestinations.PRINTERS_LIST) },
                        onNavigateToPrintSettings        = { navController.navigate(AppDestinations.SETTINGS_PRINT) },
                        onNavigateToMainSettings         = { navController.navigate(AppDestinations.SETTINGS_MAIN_CONFIG) },
                        onNavigateToPermissions          = { navController.navigate(AppDestinations.SETTINGS_PERMISSIONS) }
                    )
                }
                composable(AppDestinations.SETTINGS_MAIN_CONFIG) {
                    MainSettingsScreen(onNavigateBack = { navController.navigateUp() })
                }
                composable(AppDestinations.SETTINGS_PRINT) {
                    PrintSettingsScreen(onNavigateBack = { navController.navigateUp() })
                }
                composable(AppDestinations.SETTINGS_LOGS) {
                    LogsScreen()
                }
                composable(AppDestinations.SETTINGS_NOTIFICATIONS) {
                    NotificationSettingsScreen(onNavigateBack = { navController.navigateUp() })
                }
                composable(AppDestinations.SETTINGS_PERMISSIONS) {
                    PermissionsScreen(onNavigateBack = { navController.navigateUp() })
                }
                composable(AppDestinations.PRINTERS_LIST) {
                    com.itsorderkds.ui.settings.printer.PrintersListScreen(navController = navController)
                }
                composable(AppDestinations.SERIAL_DIAGNOSTIC) {
                    com.itsorderkds.ui.settings.printer.SerialPortDiagnosticScreen(navController = navController)
                }
            }
        }
    }
}

//    val currentDialogOrderIdRef =
//        rememberUpdatedState(ordersViewModel.uiState.collectAsStateWithLifecycle().value.orderToShowInDialog?.orderId)
//    val unknownError = stringResource(R.string.unknown_error_occurred)

    // 📍 Launcher dla uprawnień lokalizacji
//    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
//        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
//    ) { permissions ->
//        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
//        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
//
//        if (fineLocationGranted || coarseLocationGranted) {
//            Timber.d("📍 Uprawnienia lokalizacji przyznane - można ponowić akcję")
//            scope.launch {
//                snackbarHostState.showSnackbar("Uprawnienia lokalizacji przyznane. Spróbuj ponownie.")
//            }
//        } else {
//            Timber.w("📍 Uprawnienia lokalizacji odrzucone")
//            scope.launch {
//                snackbarHostState.showSnackbar("Brak uprawnień do lokalizacji. Włącz w ustawieniach aplikacji.")
//            }
//        }
//    }

//    LaunchedEffect(Unit) {
////        ordersViewModel.fetchGeneralSettings()
//        ordersViewModel.event.collectLatest { event ->
//            when (event) {
//                is OrderEvent.Error -> {
//                    val msg = event.body?.toString() ?: unknownError
//                    snackbarHostState.showSnackbar(message = msg)
//                }
//
//                is OrderEvent.Success -> {
//                    Timber.tag("OrderAlarmService").d("!!! dismissDialog stopAlarmService()")
//                    val openId = currentDialogOrderIdRef.value
//                    if (openId != null && event.message == openId) ordersViewModel.dismissDialog()
//                    snackbarHostState.showSnackbar(event.message)
//                }
//
//                OrderEvent.RequestLocationPermission -> {
//                    // 📍 Poproś o uprawnienia lokalizacji
//                    Timber.d("📍 Requesting location permissions...")
//                    locationPermissionLauncher.launch(
//                        arrayOf(
//                            Manifest.permission.ACCESS_FINE_LOCATION,
//                            Manifest.permission.ACCESS_COARSE_LOCATION
//                        )
//                    )
//                }
//
//                OrderEvent.Loading -> Unit
//                else -> {}
//            }
//        }
//    }
//
//    LaunchedEffect(Unit) {
//        messageManager.messages.collect { msg -> snackbarHostState.showSnackbar(msg.text) }
//    }
//
//    LaunchedEffect(key1 = navController, key2 = ordersViewModel) {
//        ordersViewModel.navigationEvent.collectLatest { destinationRoute ->
//            Timber.d("Odebrano event nawigacji do: $destinationRoute")
//
//            // Sprawdź czy NavController ma ustawiony graf
//            if (navController.graph.id == 0) {
//                Timber.w("⚠️ NavController nie ma jeszcze ustawionego grafu nawigacji - pomijam nawigację")
//                return@collectLatest
//            }
//
//            val currentRoute = navController.currentDestination?.route
//            if (currentRoute != destinationRoute) {
//                Timber.d("Nawiguję, bo $currentRoute != $destinationRoute")
//                try {
//                    navController.navigate(destinationRoute) {
//                        popUpTo(AppDestinations.HOME) { inclusive = true }
//                        launchSingleTop = true
//                    }
//                } catch (e: IllegalArgumentException) {
//                    Timber.e(e, "❌ Błąd nawigacji do $destinationRoute")
//                }
//            } else {
//                Timber.d("Pomijam nawigację, już jestem na $currentRoute")
//            }
//        }
//    }
//
//    Crossfade(
//        targetState = isHomeReady,
//        animationSpec = tween(durationMillis = 350),
//        label = "GateFade"
//    ) { ready ->
//        if (!ready) {
//            AppLoadingScreen()
//        } else {
//            MainScaffoldContent(
//                navController = navController,
//                drawerState = drawerState,
//                snackbarHostState = snackbarHostState,
//                scope = scope,
//                ordersViewModel = ordersViewModel,
//                kdsViewModel = kdsViewModel,
//                onLogout = onLogout,
//                socketEventsRepo = socketEventsRepo
//            )
//        }
//    }
//}

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun MainScaffoldContent(
//    navController: NavHostController,
//    drawerState: DrawerState,
//    snackbarHostState: SnackbarHostState,
//    scope: CoroutineScope,
//    ordersViewModel: OrdersViewModel,
//    kdsViewModel: com.itsorderkds.ui.kds.KdsViewModel,
//    onLogout: () -> Unit,
//    socketEventsRepo: com.itsorderkds.service.SocketEventsRepository
//) {
//    val backStackEntry by navController.currentBackStackEntryAsState()
//    val currentRoute = backStackEntry?.destination?.route ?: AppDestinations.HOME
//
//    val uiState by ordersViewModel.uiState.collectAsStateWithLifecycle()
//    val isCourier = uiState.userRole == UserRole.COURIER
//
//    var selectedStaffTabIndex by remember { mutableIntStateOf(0) }
//    // KDS: selectedOrderIds i currentBatchStatus nie są używane w głównym flow KDS
//
//    val showMainScaffoldLayout =
//        remember(uiState.isInitializing) { if (isCourier) true else !uiState.isInitializing }
//
//    LaunchedEffect(selectedStaffTabIndex) { ordersViewModel.clearSelection() }
//    LaunchedEffect(currentRoute) { if (currentRoute != AppDestinations.HOME) ordersViewModel.clearSelection() }
//
//    LaunchedEffect(isCourier, currentRoute) {
//        if (isCourier && currentRoute != AppDestinations.HOME) {
//            navController.navigate(AppDestinations.HOME) {
//                popUpTo(AppDestinations.HOME) { inclusive = true }
//                launchSingleTop = true
//            }
//        }
//    }
//
//    ModalNavigationDrawer(
//        drawerState = drawerState,
//        // gesturesEnabled = true — swipe z lewej krawędzi otwiera menu.
//        // Na ekranach ustawień (nie-HOME) gest działa normalnie.
//        // Na HOME (KDS) dodatkowo dostępny przycisk ≡ w lewym górnym rogu
//        // dla tabletów gdzie swipe może być przechwytywany przez system.
//        gesturesEnabled = showMainScaffoldLayout,
//        drawerContent = {
//            DrawerContent(
//                currentRoute = currentRoute,
//                // ✅ TŁUMACZENIE
//                userName = uiState.userId ?: stringResource(R.string.guest),
//                userRole = uiState.userRole?.name,
//                onLogout = {
//                    onLogout()
//                    scope.launch { drawerState.close() }
//                },
//                onNavigateToHome = {
//                    navController.navigate(AppDestinations.HOME) {
//                        popUpTo(AppDestinations.HOME) { inclusive = true }
//                    }
//                    scope.launch { drawerState.close() }
//                },
//                onNavigateToSettings = {
//                    if (!isCourier) {
//                        navController.navigate(AppDestinations.SETTINGS_MAIN)
//                        scope.launch { drawerState.close() }
//                    }
//                },
//                onCloseDrawer = { scope.launch { drawerState.close() } },
//                showHome = true
//            )
//        }
//    ) {
//        Scaffold(
//            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
//            topBar = {
//                // Na głównym ekranie KDS (HOME) — brak TopBar, więcej miejsca na zamówienia
//                // Na pozostałych ekranach (ustawienia itp.) TopBar jest widoczny
//                if (showMainScaffoldLayout && currentRoute != AppDestinations.HOME) {
//                    AppTopBar(
//                        config = buildTopBarConfig(
//                            currentRoute = currentRoute,
//                            navController = navController,
//                            onMenuClick = { scope.launch { drawerState.open() } },
//                            ordersViewModel = ordersViewModel,
//                            socketEventsRepo = socketEventsRepo,
//                            kdsViewModel = kdsViewModel
//                        )
//                    )
//                }
//            },
//            bottomBar = {
//                // KDS nie używa dolnej belki nawigacyjnej
//            },
//            floatingActionButton = {
//                // KDS nie używa FAB
//            }
//        ) { innerPadding ->
//            AppNavHost(
//                navController = navController,
//                // Na HOME (KDS) — zerowy padding górny (brak TopBar)
//                modifier = if (currentRoute == AppDestinations.HOME)
//                    Modifier
//                else
//                    Modifier.padding(innerPadding),
//                ordersViewModel = ordersViewModel,
//                selectedStaffTabIndex = selectedStaffTabIndex,
//                onStaffTabSelected = { selectedStaffTabIndex = it },
//                isCourier = isCourier
//            )
//        }
//    }
//}
//
//
//@Composable
//private fun CourierFloatingActionButtons(
//    ordersViewModel: OrdersViewModel,
//    selectedOrderIds: Set<String>,
//    currentBatchStatus: OrderStatusEnum?,
//    onLaunchPermissionRequest: () -> Unit
//) {
//    if (selectedOrderIds.isEmpty()) return
//
//    val uiState by ordersViewModel.uiState.collectAsStateWithLifecycle()
//    val routeState = uiState.routeState
//    val context = LocalContext.current
//    var showConfirmationDialog by remember { mutableStateOf(false) }
//
//    if (showConfirmationDialog && currentBatchStatus != null) {
//        val newStatus = when (currentBatchStatus) {
//            OrderStatusEnum.ACCEPTED -> OrderStatusEnum.OUT_FOR_DELIVERY
//            OrderStatusEnum.OUT_FOR_DELIVERY -> OrderStatusEnum.COMPLETED
//            else -> null
//        }
//
//        if (newStatus != null) {
//            AlertDialog(
//                onDismissRequest = { showConfirmationDialog = false },
//                // ✅ TŁUMACZENIE
//                title = { Text(stringResource(R.string.confirmation)) },
//                text = {
//                    val route = (routeState as? OrderRouteState.Success)?.route ?: emptyList()
//                    val selectedOrdersDetails = route.filter { it.orderId in selectedOrderIds }
//                    val count = selectedOrderIds.size
//
//                    // ✅ TŁUMACZENIE (PLURALS)
//                    val questionText = when (newStatus) {
//                        OrderStatusEnum.OUT_FOR_DELIVERY -> pluralStringResource(
//                            R.plurals.confirm_status_change_in_delivery,
//                            count,
//                            count
//                        )
//
//                        OrderStatusEnum.COMPLETED -> pluralStringResource(
//                            R.plurals.confirm_status_change_delivered,
//                            count,
//                            count
//                        )
//
//                        else -> ""
//                    }
//                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//                        // ✅ TŁUMACZENIE
//                        Text(
//                            stringResource(R.string.selected_orders),
//                            fontWeight = FontWeight.Bold
//                        )
//                        Spacer(modifier = Modifier.height(4.dp))
//                        selectedOrdersDetails.forEach { order ->
//                            Column(modifier = Modifier.padding(bottom = 8.dp)) {
//                                // ✅ TŁUMACZENIE
//                                Text(
//                                    stringResource(
//                                        R.string.order_number_list_item,
//                                        order.orderNumber
//                                    )
//                                )
//                                // ✅ TŁUMACZENIE
//                                Text(
//                                    stringResource(
//                                        R.string.order_address_list_item,
//                                        order.address ?: stringResource(R.string.no_address)
//                                    ),
//                                    style = MaterialTheme.typography.bodySmall,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
//                            }
//                        }
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Text(questionText, fontWeight = FontWeight.Bold)
//                    }
//                },
//                confirmButton = {
//                    TextButton(onClick = {
//                        showConfirmationDialog = false
//                        if (ContextCompat.checkSelfPermission(
//                                context,
//                                Manifest.permission.ACCESS_FINE_LOCATION
//                            )
//                            == PackageManager.PERMISSION_GRANTED
//                        ) {
//                        } else {
//                            onLaunchPermissionRequest()
//                        }
//                    }) { Text(stringResource(R.string.common_confirm)) } // ✅ TŁUMACZENIE
//                },
//                dismissButton = {
//                    TextButton(onClick = {
//                        showConfirmationDialog = false
//                    }) { Text(stringResource(R.string.common_cancel)) } // ✅ TŁUMACZENIE
//                }
//            )
//        }
//    }
//
//    if (uiState.userRole == UserRole.COURIER) {
//        val singleSelectedOrderPhone = if (selectedOrderIds.size == 1) {
//            (routeState as? OrderRouteState.Success)?.route?.find { it.orderId == selectedOrderIds.first() }?.phone
//        } else null
//
//        Column(
//            modifier = Modifier.padding(end = 16.dp, bottom = 16.dp),
//            horizontalAlignment = Alignment.End,
//            verticalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            if (selectedOrderIds.isNotEmpty()) {
//                FloatingActionButton(onClick = {
//                    val stops =
//                        (routeState as? OrderRouteState.Success)?.route?.filter { it.orderId in selectedOrderIds }
//                            ?: emptyList()
//                    planRouteOnMap(context, stops)
//                }) {
//                    Icon(
//                        Icons.Filled.Directions,
//                        contentDescription = stringResource(R.string.plan_route)
//                    ) // ✅ TŁUMACZENIE
//                }
//            }
//            if (singleSelectedOrderPhone != null) {
//                FloatingActionButton(
//                    onClick = {
//                        val intent =
//                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$singleSelectedOrderPhone"))
//                        try {
//                            context.startActivity(intent)
//                        } catch (_: ActivityNotFoundException) {
//                            // ✅ TŁUMACZENIE
////                            Toast.makeText(
////                                context,
////                                stringResource(R.string.phone_app_not_found),
////                                Toast.LENGTH_SHORT
////                            ).show()
//                        }
//                    },
//                    containerColor = MaterialTheme.colorScheme.primaryContainer
//                ) {
//                    Icon(
//                        Icons.Filled.Call,
//                        contentDescription = stringResource(R.string.call)
//                    ) // ✅ TŁUMACZENIE
//                }
//            }
//            if (currentBatchStatus != null) {
//                // Te stringi (in_delivery_with_count, delivered, accepted)
//                // były już zasobami, więc są OK.
//                val (buttonText, buttonIcon) = when (currentBatchStatus) {
//                    OrderStatusEnum.ACCEPTED -> stringResource(
//                        R.string.in_delivery_with_count,
//                        selectedOrderIds.size
//                    ) to Icons.Filled.LocalShipping
//
//                    OrderStatusEnum.OUT_FOR_DELIVERY -> stringResource(
//                        R.string.delivered,
//                        selectedOrderIds.size
//                    ) to Icons.Filled.CheckCircle
//
//                    else -> stringResource(
//                        R.string.accepted,
//                        selectedOrderIds.size
//                    ) to Icons.Filled.DoneAll
//                }
//                ExtendedFloatingActionButton(
//                    text = { Text(buttonText) },
//                    icon = { Icon(buttonIcon, contentDescription = buttonText) },
//                    onClick = { showConfirmationDialog = true }
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun AppNavHost(
//    navController: NavHostController,
//    modifier: Modifier,
//    ordersViewModel: OrdersViewModel,  // zachowane dla ustawień/drawerów
//    selectedStaffTabIndex: Int = 0,
//    onStaffTabSelected: (Int) -> Unit = {},
//    isCourier: Boolean = false
//) {
//    NavHost(
//        navController = navController,
//        startDestination = AppDestinations.HOME,
//        modifier = modifier
//    ) {
//        composable(AppDestinations.HOME) {
//            KdsScreen(
//                onNavigateToSettings = { navController.navigate(AppDestinations.SETTINGS_MAIN) }
//            )
//        }
//
//        // Ustawienia
//        composable(AppDestinations.SETTINGS_MAIN) {
//            SettingsMainScreen(
//                onNavigateToNotificationSettings = { navController.navigate(AppDestinations.SETTINGS_NOTIFICATIONS) },
//                onNavigateToPrintersList = { navController.navigate(AppDestinations.PRINTERS_LIST) },
//                onNavigateToPrintSettings = { navController.navigate(AppDestinations.SETTINGS_PRINT) },
//                onNavigateToMainSettings = { navController.navigate(AppDestinations.SETTINGS_MAIN_CONFIG) },
//                onNavigateToPermissions = { navController.navigate(AppDestinations.SETTINGS_PERMISSIONS) }
//            )
//        }
//        // 🎯 NOWE: Główne ustawienia
//        composable(AppDestinations.SETTINGS_MAIN_CONFIG) {
//            MainSettingsScreen(
//                onNavigateBack = { navController.navigateUp() })
//        }
//        composable(AppDestinations.SETTINGS_PRINT) {
//            PrintSettingsScreen(onNavigateBack = { navController.navigateUp() })
//        }
//        composable(AppDestinations.SETTINGS_LOGS) { LogsScreen() }
//        // OpenHours - usunięte (nie potrzebne w KDS)
//        composable(AppDestinations.SETTINGS_NOTIFICATIONS) {
//            NotificationSettingsScreen(onNavigateBack = { navController.navigateUp() })
//        }
//        composable(AppDestinations.SETTINGS_PERMISSIONS) {
//            PermissionsScreen(onNavigateBack = { navController.navigateUp() })
//        }
//        composable(AppDestinations.PRINTERS_LIST) {
//            com.itsorderkds.ui.settings.printer.PrintersListScreen(navController = navController)
//        }
//        composable(AppDestinations.SERIAL_DIAGNOSTIC) {
//            com.itsorderkds.ui.settings.printer.SerialPortDiagnosticScreen(navController = navController)
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun DynamicTopAppBar(
//    navController: NavHostController,
//    currentRoute: String,
//    onMenuClick: () -> Unit,
//    onNavigateBack: () -> Unit,
//    ordersViewModel: OrdersViewModel,
//    onCheckOut: () -> Unit
//) {
//    val isHomeScreen = currentRoute == AppDestinations.HOME
//    val ordersUiState by ordersViewModel.uiState.collectAsStateWithLifecycle()
//    val isCourier = ordersUiState.userRole == UserRole.COURIER
//
//    TopAppBar(
//        title = {
//            // Tytuły dla ekranów KDS
//            when (currentRoute) {
//                AppDestinations.HOME -> Unit
//                AppDestinations.SETTINGS_MAIN -> Text(stringResource(R.string.settings_title))
//                AppDestinations.SETTINGS_PRINT -> Text(stringResource(R.string.settings_print_title))
//                AppDestinations.SETTINGS_LOGS -> Text(stringResource(R.string.settings_logs_title))
//                AppDestinations.SETTINGS_NOTIFICATIONS -> Text(stringResource(R.string.settings_notifications_title))
//                AppDestinations.PRINTERS_LIST -> Text(stringResource(R.string.printers_manage_title))
//                else -> Unit
//            }
//        },
//        navigationIcon = {
//            if (isHomeScreen) {
//                IconButton(onClick = onMenuClick) {
//                    Icon(
//                        Icons.Filled.Menu,
//                        stringResource(R.string.common_menu)
//                    )
//                }
//            } else {
//                IconButton(onClick = onNavigateBack) {
//                    Icon(
//                        Icons.AutoMirrored.Filled.ArrowBack,
//                        stringResource(R.string.common_back)
//                    )
//                }
//            }
//        },
//        actions = {
//            // Akcje dla kuriera - zmiana zmiany
//            if (isCourier && ordersUiState.isShiftActive) {
//                ShiftStatusAction(onCheckOut = onCheckOut)
//            }
//
//            // KDS nie wymaga zarządzania statusem restauracji
//            // Usunięto: RestaurantStatusActionItem
//        }
//    )
//}

// -----------------------------------------------------------------
// FUNKCJE POMOCNICZE (BEZ ZMIAN, JUŻ UŻYWAJĄ TŁUMACZEŃ)
// -----------------------------------------------------------------
// DOLNA BELKA NAWIGACYJNA DLA PRACOWNIKÓW RESTAURACJI
// -----------------------------------------------------------------

/**
 * Komponent dolnej belki nawigacyjnej dla roli STAFF.
 *
 * Wyświetla 3 główne zakładki:
 * - Zakładka 0: Zamówienia (PROCESSING, ACCEPTED, OUT_FOR_DELIVERY)
 * - Zakładka 1: Zakończone (OUT_FOR_DELIVERY, COMPLETED)
 * - Zakładka 2: Inne (grupowane według portalu)
 *
 * Funkcjonalność:
 * - Synchronizacja dwukierunkowa z HorizontalPager w StaffView
 * - Wsparcie dla gestów przesuwania (swipe)
 * - Automatyczne tłumaczenia z strings.xml
 * - Responsywny design Material 3
 *
 * @param selectedTabIndex Indeks aktualnie wybranej zakładki (0-2)
 * @param onTabSelected Callback wywoływany przy zmianie zakładki
 *
 * @see staffTabs Definicja zakładek w MainScreen.kt
 * @see com.itsorderkds.ui.theme.home.view.StaffView Komponent wyświetlający zawartość zakładek
 */
@Composable
private fun StaffBottomBar(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar {
        staffTabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                // Podświetlenie aktywnej zakładki
                selected = (selectedTabIndex == index),

                // Obsługa kliknięcia - wywołuje callback z indeksem zakładki
                onClick = { onTabSelected(index) },

                // Ikona zakładki (Material Icons)
                icon = {
                    Icon(
                        tab.icon,
                        contentDescription = stringResource(tab.titleRes)
                    )
                },

                // Etykieta zawsze widoczna (nawet dla nieaktywnych zakładek)
                alwaysShowLabel = true,

                // Tekst etykiety z automatycznym tłumaczeniem
                label = {
                    Text(
                        stringResource(tab.titleRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

// RestaurantStatusActionItem usunięty - KDS nie zarządza statusem restauracji

