@file:OptIn(ExperimentalMaterial3Api::class)

package com.itsorderkds.ui.settings

import android.media.MediaPlayer
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.itsorderkds.R
import com.itsorderkds.data.preferences.AppPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun SettingsMainScreen(
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToPrintersList: () -> Unit,
    onNavigateToPrintSettings: () -> Unit,
    onNavigateToMainSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        // Główne ustawienia - niebieski
        item {
            SettingsItem(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.settings_main_title),
                subtitle = stringResource(R.string.settings_main_subtitle),
                onClick = onNavigateToMainSettings,
                iconBackgroundColor = Color(0xFF2196F3).copy(alpha = 0.15f),
                iconTint = Color(0xFF2196F3)
            )
        }
        // Ustawienia drukowania - fioletowy
        item {
            SettingsItem(
                icon = Icons.Default.Print,
                title = stringResource(R.string.settings_print_title),
                subtitle = "Zarządzaj ustawieniami automatycznego drukowania",
                onClick = onNavigateToPrintSettings,
                iconBackgroundColor = Color(0xFF9C27B0).copy(alpha = 0.15f),
                iconTint = Color(0xFF9C27B0)
            )
        }
        // Zarządzanie drukarkami - pomarańczowy
        item {
            SettingsItem(
                icon = Icons.Default.Print,
                title = stringResource(R.string.printers_manage_title),
                subtitle = stringResource(R.string.printers_manage_subtitle),
                onClick = onNavigateToPrintersList,
                iconBackgroundColor = Color(0xFFFF9800).copy(alpha = 0.15f),
                iconTint = Color(0xFFFF9800)
            )
        }
        // Powiadomienia - różowy
        item {
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.settings_notifications_title),
                subtitle = stringResource(R.string.settings_notifications_subtitle),
                onClick = onNavigateToNotificationSettings,
                iconBackgroundColor = Color(0xFFE91E63).copy(alpha = 0.15f),
                iconTint = Color(0xFFE91E63)
            )
        }
        // Uprawnienia - cyjan/turkusowy
        item {
            SettingsItem(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.settings_permissions_title),
                subtitle = stringResource(R.string.settings_permissions_subtitle),
                onClick = onNavigateToPermissions,
                iconBackgroundColor = Color(0xFF00BCD4).copy(alpha = 0.15f),
                iconTint = Color(0xFF00BCD4)
            )
        }
        // KDS nie zarządza godzinami otwarcia - usunięto
    }
}

// --- REUŻYWALNY KOMPONENT DLA POZYCJI W USTAWIENIACH ---
// Z kolorowymi kółkami w tle ikon dla lepszego wyglądu
@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconBackgroundColor: Color? = null,
    iconTint: Color? = null
) {
    val backgroundColor = iconBackgroundColor ?: MaterialTheme.colorScheme.primaryContainer
    val tintColor = iconTint ?: MaterialTheme.colorScheme.onPrimaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kolorowe kółko z ikoną
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = backgroundColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = tintColor
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}


// --- Ekran ustawień powiadomień (wybór dźwięku) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val sounds = viewModel.availableSounds
    val context = LocalContext.current
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var previewJob by remember { mutableStateOf<Job?>(null) }
    val selectedUri by viewModel.selectedUri.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMutedFlow.collectAsStateWithLifecycle()
    val currentTypeIndex by viewModel.currentTypeIndexFlow.collectAsStateWithLifecycle()
    val currentType = viewModel.types[currentTypeIndex]

    // Czyszczenie MediaPlayera przy opuszczaniu ekranu
    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.let { p ->
                try { p.stop(); p.release() } catch (_: Exception) {}
            }
        }
    }

    Column {
        TabRow(selectedTabIndex = currentTypeIndex) {
            viewModel.types.forEachIndexed { idx, type ->
                Tab(
                    selected = currentTypeIndex == idx,
                    onClick = { viewModel.selectType(idx) },
                    text = {
                        Text(
                            when (type) {
                                NotificationType.ORDER_ALARM -> stringResource(R.string.order_alarm_title)
                                NotificationType.ROUTE_UPDATED -> stringResource(R.string.new_route_notification_title)
                                NotificationType.GENERAL -> stringResource(R.string.notification_channel_general_id)
                            }
                        )
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.order_alarm_mute), modifier = Modifier.weight(1f))
            Switch(
                checked = isMuted,
                onCheckedChange = { viewModel.setMuted(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(sounds.size) { index ->
                val sound = sounds[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedUri == sound.uri,
                        onClick = { viewModel.setNotificationSound(currentType, sound.uri) },
                        enabled = !isMuted
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(text = sound.label, style = MaterialTheme.typography.bodyLarge)
                        Text(text = sound.uri, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = {
                        if (isMuted) return@Button
                        try {
                            previewJob?.cancel()
                            previewJob = null
                            previewPlayer?.let { p ->
                                try { p.stop(); p.release() } catch (_: Exception) {}
                            }
                            previewPlayer = null
                            val player = MediaPlayer().apply {
                                setDataSource(context, sound.uri.toUri())
                                setOnPreparedListener { start() }
                                setOnCompletionListener {
                                    try { stop(); release() } catch (_: Exception) {}
                                    previewPlayer = null
                                }
                                prepareAsync()
                            }
                            previewPlayer = player
                        } catch (_: Exception) { /* ignoruj preview error */ }
                    }) { Text(stringResource(R.string.common_ok)) }
                }
                HorizontalDivider()
            }
        }
    }
}

// --- Ekran Głównych Ustawień ---
@Composable
fun MainSettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: MainSettingsViewModel = hiltViewModel()
) {
    val kioskModeEnabled by viewModel.kioskModeEnabled.collectAsStateWithLifecycle()
    val autoRestartEnabled by viewModel.autoRestartEnabled.collectAsStateWithLifecycle()
    val taskReopenEnabled by viewModel.taskReopenEnabled.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_main_config_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
        }

        item {
            SettingsItemWithSwitch(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.settings_main_kiosk_mode_title),
                subtitle = stringResource(R.string.settings_main_kiosk_mode_subtitle),
                isEnabled = kioskModeEnabled,
                onToggle = { viewModel.setKioskModeEnabled(it) }
            )
        }

        item {
            SettingsItemWithSwitch(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.settings_main_auto_restart_title),
                subtitle = stringResource(R.string.settings_main_auto_restart_subtitle),
                isEnabled = autoRestartEnabled,
                onToggle = { viewModel.setAutoRestartEnabled(it) }
            )
        }

        item {
            SettingsItemWithSwitch(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.settings_main_task_reopen_title),
                subtitle = stringResource(R.string.settings_main_task_reopen_subtitle),
                isEnabled = taskReopenEnabled,
                onToggle = { viewModel.setTaskReopenEnabled(it) }
            )
        }

        item {
            Text(
                text = stringResource(R.string.settings_main_other_section),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.settings_main_about_title),
                subtitle = stringResource(R.string.settings_main_about_subtitle),
                onClick = { /* TODO: O aplikacji */ }
            )
        }

        // ✅ NOWY: Przycisk Test Crash (tylko w DEBUG)
        if (com.itsorderkds.BuildConfig.DEBUG) {
            item {
                SettingsItem(
                    icon = Icons.Default.BugReport,
                    title = "Test Crash & Auto-Restart",
                    subtitle = "Symuluje crash aplikacji i sprawdza auto-restart (tylko DEBUG)",
                    onClick = {
                        // Celowy crash po 1 sekundzie (żeby UI zdążyło się odświeżyć)
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            timber.log.Timber.tag("TEST_CRASH").w("🧪 Symulowany crash - test auto-restart")
                            throw RuntimeException("🧪 TEST CRASH: Sprawdzenie mechanizmu auto-restart")
                        }, 1000)
                    },
                    iconBackgroundColor = Color(0xFFFF5252).copy(alpha = 0.15f),
                    iconTint = Color(0xFFFF5252)
                )
            }
        }
    }
}

// --- Komponent ustawień z Switch'em ---
@Composable
fun SettingsItemWithSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors()
        )
    }
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}

enum class NotificationType { ORDER_ALARM, ROUTE_UPDATED, GENERAL }

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val prefs: AppPreferencesManager,
    private val app: android.app.Application
) : androidx.lifecycle.ViewModel() {
    data class SoundItem(val label: String, val uri: String)

    // Lista dostępnych dźwięków z zasobów raw (używa tłumaczonych nazw)
    val availableSounds: List<SoundItem> by lazy {
        listOf(
            SoundItem(app.getString(R.string.sound_name_default), "android.resource://com.itsorderkds/${R.raw.order_iphone}"),
            SoundItem(app.getString(R.string.sound_name_soft_chime), "android.resource://com.itsorderkds/${R.raw.sound1}"),
            SoundItem(app.getString(R.string.sound_name_classic_bell), "android.resource://com.itsorderkds/${R.raw.sound2}")
        )
    }

    val types: List<NotificationType> = listOf(
        NotificationType.ORDER_ALARM,
        NotificationType.ROUTE_UPDATED,
        NotificationType.GENERAL
    )

    private val _currentTypeIndex = MutableStateFlow(0)
    val currentTypeIndexFlow: StateFlow<Int> = _currentTypeIndex

    private val _selectedUri = MutableStateFlow<String?>(null)
    val selectedUri: StateFlow<String?> = _selectedUri

    private val _isMuted = MutableStateFlow(false)
    val isMutedFlow: StateFlow<Boolean> = _isMuted

    init {
        viewModelScope.launch { loadForType(types[_currentTypeIndex.value]) }
    }

    private suspend fun loadForType(type: NotificationType) {
        val key = type.toKey()
        _selectedUri.value = prefs.getNotificationSoundUri(key)
        _isMuted.value = prefs.isNotificationSoundMuted(key)
    }

    fun selectType(index: Int) {
        if (index == _currentTypeIndex.value) return
        _currentTypeIndex.value = index
        viewModelScope.launch { loadForType(types[index]) }
    }

    fun setNotificationSound(type: NotificationType, uri: String) {
        viewModelScope.launch {
            val key = type.toKey()
            prefs.setNotificationSoundUri(key, uri)
            _selectedUri.value = uri
        }
    }

    fun setMuted(muted: Boolean) {
        viewModelScope.launch {
            val key = types[_currentTypeIndex.value].toKey()
            prefs.setNotificationSoundMuted(key, muted)
            _isMuted.value = muted
        }
    }

    private fun NotificationType.toKey(): String = when (this) {
        NotificationType.ORDER_ALARM -> "order_alarm"
        NotificationType.ROUTE_UPDATED -> "route_updated"
        NotificationType.GENERAL -> "general"
    }
}

// --- Ekran Uprawnień ---
@Composable
fun PermissionsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // State do wymuszenia re-composicji po przyznaniu uprawnień
    var permissionRefreshTrigger by remember { mutableStateOf(0) }

    // Sprawdzamy status uprawnień (z zależnością od refreshTrigger)
    val notificationsGranted = remember(permissionRefreshTrigger) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Na starszych wersjach nie wymaga uprawnienia
        }
    }

    val locationGranted = remember(permissionRefreshTrigger) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val bluetoothGranted = remember(permissionRefreshTrigger) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val connectGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val scanGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            connectGranted && scanGranted
        } else {
            true // Na starszych wersjach nie wymaga tych uprawnień
        }
    }

    // Launcher dla uprawnień powiadomień (Android 13+)
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        timber.log.Timber.d("🔔 Notification permission result: $isGranted")
        permissionRefreshTrigger++
    }

    // Launcher dla uprawnień lokalizacji
    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        timber.log.Timber.d("📍 Location permission result: $isGranted")
        permissionRefreshTrigger++
    }

    // Launcher dla uprawnień Bluetooth (Android 12+)
    val bluetoothPermissionsLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        timber.log.Timber.d("📱 Bluetooth permissions result: $permissions")
        permissionRefreshTrigger++
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_permissions_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Powiadomienia
        item {
            PermissionItem(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.permission_notifications_title),
                subtitle = stringResource(R.string.permission_notifications_desc),
                isGranted = notificationsGranted,
                onClick = {
                    if (!notificationsGranted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        // Poproś o uprawnienie bezpośrednio
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Już przyznane - otwórz ustawienia
                        openAppSettings(context)
                    }
                }
            )
        }

        // Lokalizacja
        item {
            PermissionItem(
                icon = Icons.Default.AccessTime, // Możesz zmienić na inną ikonę jak Icons.Default.LocationOn
                title = stringResource(R.string.permission_location_title),
                subtitle = stringResource(R.string.permission_location_desc),
                isGranted = locationGranted,
                onClick = {
                    if (!locationGranted) {
                        // Poproś o uprawnienie bezpośrednio
                        locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        // Już przyznane - otwórz ustawienia
                        openAppSettings(context)
                    }
                }
            )
        }

        // Bluetooth / Urządzenia w pobliżu
        item {
            PermissionItem(
                icon = Icons.Default.Print, // Możesz zmienić na Icons.Default.Bluetooth
                title = stringResource(R.string.permission_bluetooth_title),
                subtitle = stringResource(R.string.permission_bluetooth_desc),
                isGranted = bluetoothGranted,
                onClick = {
                    if (!bluetoothGranted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        // Poproś o oba uprawnienia Bluetooth
                        bluetoothPermissionsLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.BLUETOOTH_CONNECT,
                                android.Manifest.permission.BLUETOOTH_SCAN
                            )
                        )
                    } else {
                        // Już przyznane lub starsza wersja - otwórz ustawienia
                        openAppSettings(context)
                    }
                }
            )
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kolorowe kółko z ikoną
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (isGranted) {
                            Color(0xFF4CAF50).copy(alpha = 0.15f)
                        } else {
                            Color(0xFFF44336).copy(alpha = 0.15f)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isGranted) {
                        Color(0xFF4CAF50)
                    } else {
                        Color(0xFFF44336)
                    }
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Przycisk akcji
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGranted) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                ) {
                    Text(
                        text = if (isGranted) {
                            stringResource(R.string.permission_open_settings)
                        } else {
                            stringResource(R.string.permission_request)
                        }
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
    }
}

private fun openAppSettings(context: android.content.Context) {
    val intent = android.content.Intent(
        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        android.net.Uri.fromParts("package", context.packageName, null)
    )
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}


