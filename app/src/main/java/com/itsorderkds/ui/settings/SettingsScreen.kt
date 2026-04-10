@file:OptIn(ExperimentalMaterial3Api::class)

package com.itsorderkds.ui.settings

import android.media.MediaPlayer
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.lazy.items
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
import com.itsorderkds.notifications.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
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


// --- Ekran ustawień powiadomień (wybór dźwięku KDS) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val sounds      = viewModel.availableSounds
    val context     = LocalContext.current
    val selectedUri by viewModel.selectedUri.collectAsStateWithLifecycle()
    val isMuted     by viewModel.isMutedFlow.collectAsStateWithLifecycle()

    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.let { try { it.stop(); it.release() } catch (_: Exception) {} }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // ─── Wyciszenie ──────────────────────────────────────────────────
        item {
            Text(
                text     = "Dźwięk nowego zamówienia KDS",
                style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color    = MaterialTheme.colorScheme.primary
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Wycisz powiadomienia", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (isMuted) "Nowe zamówienia bez dźwięku"
                        else "Nowe zamówienia grają wybrany dźwięk",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked  = isMuted,
                    onCheckedChange = { viewModel.setMuted(it) }
                )
            }
            HorizontalDivider()
        }

        // ─── Wybór dźwięku ───────────────────────────────────────────────
        item {
            Text(
                text     = "Wybierz dźwięk",
                style    = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(sounds.size) { index ->
            val sound      = sounds[index]
            val isSelected = selectedUri == sound.uri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isMuted) {
                        viewModel.setNotificationSound(sound.uri, context)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick  = { viewModel.setNotificationSound(sound.uri, context) },
                    enabled  = !isMuted
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text  = sound.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isMuted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                else MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected && !isMuted) {
                        Text(
                            text  = "Aktywny",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                // Przycisk podglądu dźwięku
                IconButton(
                    enabled = !isMuted,
                    onClick = {
                        previewPlayer?.let { try { it.stop(); it.release() } catch (_: Exception) {} }
                        previewPlayer = null
                        runCatching {
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
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Odtwórz podgląd",
                        tint = if (isMuted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                               else MaterialTheme.colorScheme.primary
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
        }

        // ─── Info o kanale systemowym ────────────────────────────────────
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ℹ️ Informacja",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Wybrany dźwięk jest odtwarzany bezpośrednio przez aplikację przy każdym nowym zamówieniu — " +
                        "zmiana działa natychmiast bez potrzeby restartowania aplikacji. " +
                        "Głośność regulujesz suwakiem powiadomień w systemie Android.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
    val kioskModeEnabled           by viewModel.kioskModeEnabled.collectAsStateWithLifecycle()
    val autoRestartEnabled         by viewModel.autoRestartEnabled.collectAsStateWithLifecycle()
    val taskReopenEnabled          by viewModel.taskReopenEnabled.collectAsStateWithLifecycle()
    val kdsQueueMode               by viewModel.kdsQueueMode.collectAsStateWithLifecycle()
    val kdsAutoPrintOnReady        by viewModel.kdsAutoPrintOnReady.collectAsStateWithLifecycle()
    val kdsShowScheduled           by viewModel.kdsShowScheduled.collectAsStateWithLifecycle()
    val kdsRequireReadyConfirm     by viewModel.kdsRequireReadyConfirm.collectAsStateWithLifecycle()
    val kdsGridColumns             by viewModel.kdsGridColumns.collectAsStateWithLifecycle()
    val kdsShowProductionSummary   by viewModel.kdsShowProductionSummary.collectAsStateWithLifecycle()
    val kdsProductionMinQty        by viewModel.kdsProductionMinQty.collectAsStateWithLifecycle()
    val kdsProductionColumns       by viewModel.kdsProductionColumns.collectAsStateWithLifecycle()
    val kdsDisplayMode             by viewModel.kdsDisplayMode.collectAsStateWithLifecycle()
    val kdsFillGaps                by viewModel.kdsFillGaps.collectAsStateWithLifecycle()
    val kdsPrepTimePickup          by viewModel.kdsPrepTimePickup.collectAsStateWithLifecycle()
    val kdsPrepTimeDelivery        by viewModel.kdsPrepTimeDelivery.collectAsStateWithLifecycle()
    val kdsCancelEnabled           by viewModel.kdsCancelEnabled.collectAsStateWithLifecycle()
    val kdsShowNotes               by viewModel.kdsShowNotes.collectAsStateWithLifecycle()
    val kdsHeaderTapMode           by viewModel.kdsHeaderTapMode.collectAsStateWithLifecycle()
    val kdsScheduledActiveWindow   by viewModel.kdsScheduledActiveWindow.collectAsStateWithLifecycle()
    val kdsExcludedKeywords        by viewModel.kdsExcludedKeywords.collectAsStateWithLifecycle()
    val kdsCompactCardMode         by viewModel.kdsCompactCardMode.collectAsStateWithLifecycle()
    // Lokalna kopia do edycji w TextField — synchronizowana z Flow
    var kdsExcludedKeywordsEdit by remember(kdsExcludedKeywords) { mutableStateOf(kdsExcludedKeywords) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // ─── Sekcja: Workflow KDS ────────────────────────────────────────
        item {
            Text(
                text  = "Tryb pracy KDS",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            SettingsItemWithSwitch(
                icon     = Icons.Default.Settings,
                title    = "Tryb kolejkowy",
                subtitle = if (kdsQueueMode)
                    "Włączony: NEW → ACKED → IN_PROGRESS → READY → HANDOFF"
                else
                    "Wyłączony (domyślny): tylko GOTOWE — idealny dla sushi/baru",
                isEnabled = kdsQueueMode,
                onToggle  = { viewModel.setKdsQueueMode(it) }
            )
        }
        item {
            SettingsItemWithSwitch(
                icon     = Icons.Default.Print,
                title    = "Drukuj automatycznie po GOTOWE",
                subtitle = "Po oznaczeniu zamówienia jako GOTOWE drukuje bilet na drukarce",
                isEnabled = kdsAutoPrintOnReady,
                onToggle  = { viewModel.setKdsAutoPrintOnReady(it) }
            )
        }
        item {
            SettingsItemWithSwitch(
                icon     = Icons.Default.AccessTime,
                title    = "Zakładka Zaplanowane",
                subtitle = "Pokazuje zamówienia zaplanowane na konkretną godzinę (> 60 min)",
                isEnabled = kdsShowScheduled,
                onToggle  = { viewModel.setKdsShowScheduled(it) }
            )
        }
        item {
            SettingsItemWithSwitch(
                icon      = Icons.Default.CheckCircle,
                title     = "Potwierdzenie przed GOTOWE",
                subtitle  = "Wymaga dodatkowego kliknięcia przed zmianą statusu na GOTOWE",
                isEnabled = kdsRequireReadyConfirm,
                onToggle  = { viewModel.setKdsRequireReadyConfirm(it) }
            )
        }
        item {
            SettingsItemWithSwitch(
                icon      = Icons.Default.Settings,
                title     = "Panel Production Summary",
                subtitle  = "Pokazuje agregację pozycji ze wszystkich aktywnych zamówień (np. 5× Zestaw A)",
                isEnabled = kdsShowProductionSummary,
                onToggle  = { viewModel.setKdsShowProductionSummary(it) }
            )
        }
        if (kdsShowProductionSummary) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        "Minimalna ilość pozycji do pokazania",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (kdsProductionMinQty <= 1) "Pokazuj wszystkie pozycje (nawet 1×)"
                        else "Pokazuj tylko pozycje z ilością ≥ ${kdsProductionMinQty}×",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "Wszystkie", 2 to "≥ 2×", 3 to "≥ 3×", 5 to "≥ 5×").forEach { (value, label) ->
                            FilterChip(
                                selected = kdsProductionMinQty == value,
                                onClick  = { viewModel.setKdsProductionMinQty(value) },
                                label    = { Text(label, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        "Układ kolumn Production Summary",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (kdsProductionColumns >= 2) "2 kolumny — więcej pozycji widocznych naraz"
                        else "1 kolumna — standardowy układ pionowy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "1 kolumna", 2 to "2 kolumny").forEach { (value, label) ->
                            FilterChip(
                                selected = kdsProductionColumns == value,
                                onClick  = { viewModel.setKdsProductionColumns(value) },
                                label    = { Text(label, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
            }
        }
        item {
            Text(
                text  = "Układ zamówień",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text("Tryb wyświetlania bloczków", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    when (kdsDisplayMode) {
                        "STABLE_GRID"  -> "Stabilna siatka — zamówienia nie przeskakują po zmianach (zalecane)"
                        "COLUMN_MODE"  -> "Niezależne kolumny — każda kolumna zarządzana osobno"
                        else           -> "Kompaktowy przepływ — standardowy układ automatyczny"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "COMPACT_FLOW" to "Przepływ",
                        "STABLE_GRID"  to "Stabilna",
                        "COLUMN_MODE"  to "Kolumny"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = kdsDisplayMode == value,
                            onClick  = { viewModel.setKdsDisplayMode(value) },
                            label    = { Text(label) }
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp, start = 16.dp))
        }
        item {
            SettingsItemWithSwitch(
                icon      = Icons.Default.Settings,
                title     = "Wypełniaj wolne miejsca",
                subtitle  = "Nowe zamówienie trafia na pierwsze wolne miejsce (tylko Stabilna siatka)",
                isEnabled = kdsFillGaps,
                onToggle  = { viewModel.setKdsFillGaps(it) }
            )
        }
        item {
            SettingsItemWithSwitch(
                icon      = Icons.Default.Close,
                title     = "Zezwól na anulowanie zamówień",
                subtitle  = "Pokazuje przycisk ANULUJ na karcie zamówienia. Domyślnie wyłączone.",
                isEnabled = kdsCancelEnabled,
                onToggle  = { viewModel.setKdsCancelEnabled(it) }
            )
        }
        item {
            SettingsItemWithSwitch(
                icon      = Icons.Default.Edit,
                title     = "Pokazuj modyfikacje pozycji",
                subtitle  = "Wyświetla notatki/dodatki przy każdej pozycji zamówienia (np. bez cebuli, extra sos).",
                isEnabled = kdsShowNotes,
                onToggle  = { viewModel.setKdsShowNotes(it) }
            )
        }
        item {
            SettingsItemWithSwitch(
                icon      = Icons.Default.Gesture,
                title     = "Tryb tapowania nagłówka",
                subtitle  = "Dotknięcie nagłówka zamówienia zmienia jego status. Przyciski PRZYJMIJ/GOTOWE znikają — więcej miejsca na ekranie.",
                isEnabled = kdsHeaderTapMode,
                onToggle  = { viewModel.setKdsHeaderTapMode(it) }
            )
        }
        item {
            SettingsItemWithSwitch(
                icon      = Icons.Default.GridView,
                title     = "Kompaktowy bloczek kuchenny",
                subtitle  = "Skrócony widok: numer wew., numer zew., źródło, typ zamówienia i lista pozycji. Mniej miejsca, więcej bloczków na ekranie.",
                isEnabled = kdsCompactCardMode,
                onToggle  = { viewModel.setKdsCompactCardMode(it) }
            )
        }

        // ─── Sekcja: Czasy przygotowania ─────────────────────────────────
        item {
            Text(
                text     = "Czasy przygotowania",
                style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color    = MaterialTheme.colorScheme.primary
            )
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    "🏠 Odbiór osobisty — zacznij gotować X minut przed",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Np. zamówienie na 12:00 → zacznij gotować o ${
                        "%02d:%02d".format(
                            (12 * 60 - kdsPrepTimePickup) / 60,
                            (12 * 60 - kdsPrepTimePickup) % 60
                        )
                    } (${kdsPrepTimePickup} min przed)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    listOf(15, 20, 30, 45, 60).forEach { min ->
                        FilterChip(
                            selected = kdsPrepTimePickup == min,
                            onClick  = { viewModel.setKdsPrepTimePickup(min) },
                            label    = { Text("${min}min") }
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp, start = 16.dp))
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    "🚗 Dostawa — zacznij gotować X minut przed",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Np. zamówienie na 12:00 → zacznij gotować o ${
                        "%02d:%02d".format(
                            (12 * 60 - kdsPrepTimeDelivery) / 60,
                            (12 * 60 - kdsPrepTimeDelivery) % 60
                        )
                    } (${kdsPrepTimeDelivery} min przed)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    listOf(30, 45, 60, 75, 90).forEach { min ->
                        FilterChip(
                            selected = kdsPrepTimeDelivery == min,
                            onClick  = { viewModel.setKdsPrepTimeDelivery(min) },
                            label    = { Text("${min}min") }
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp, start = 16.dp))
        }

        // ─── Sekcja: Układ ekranu ────────────────────────────────────────
        item {
            Text(
                text  = "Układ ekranu",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text("Liczba kolumn bloczków", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Auto — dopasowuje się do rozmiaru ekranu",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("auto" to "Auto", "2" to "2", "3" to "3", "4" to "4").forEach { (value, label) ->
                        FilterChip(
                            selected = kdsGridColumns == value,
                            onClick  = { viewModel.setKdsGridColumns(value) },
                            label    = { Text(label) }
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp, start = 16.dp))
        }

        // ─── Ustawienie: okno aktywacji zaplanowanych ────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    "📅 Okno aktywacji zaplanowanych zamówień",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Zamówienie zaplanowane pojawia się w Aktywnych na $kdsScheduledActiveWindow min przed godziną realizacji. " +
                    "Wcześniej widoczne tylko w zakładce Plan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    listOf(30, 45, 60, 90, 120).forEach { min ->
                        FilterChip(
                            selected = kdsScheduledActiveWindow == min,
                            onClick  = { viewModel.setKdsScheduledActiveWindow(min) },
                            label    = { Text("${min}min") }
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp, start = 16.dp))
        }

        // ─── Sekcja: Filtrowanie produktów ──────────────────────────────
        item {
            Text(
                text     = "Filtrowanie produktów",
                style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color    = MaterialTheme.colorScheme.primary
            )
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    "🚫 Ukryj produkty zawierające słowa kluczowe",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Produkty, których nazwa zawiera dowolne z podanych słów, nie będą wyświetlane " +
                    "na bloczkach KDS ani w Produkcji łącznej. Rozdziel słowa przecinkami. " +
                    "Przykład: opłata,fee,delivery charge",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value         = kdsExcludedKeywordsEdit,
                    onValueChange = { kdsExcludedKeywordsEdit = it },
                    modifier      = Modifier.fillMaxWidth(),
                    label         = { Text("Słowa kluczowe (oddzielone przecinkami)") },
                    placeholder   = { Text("opłata,fee,extra charge") },
                    singleLine    = false,
                    maxLines      = 3
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            viewModel.setKdsExcludedKeywords(kdsExcludedKeywordsEdit.trim())
                        }
                    ) { Text("Zapisz") }
                    androidx.compose.material3.TextButton(
                        onClick = {
                            kdsExcludedKeywordsEdit = "opłata"
                            viewModel.setKdsExcludedKeywords("opłata")
                        }
                    ) { Text("Resetuj") }
                }
                // Podgląd aktualnych aktywnych słów kluczowych
                val keywords = kdsExcludedKeywordsEdit.split(",")
                    .map { it.trim() }.filter { it.isNotBlank() }
                if (keywords.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text  = "Aktywne filtry: ${keywords.joinToString(" · ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp, start = 16.dp))
        }

        // ─── Sekcja: Terminal ────────────────────────────────────────────
        item {
            Text(
                text  = stringResource(R.string.settings_main_config_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            SettingsItemWithSwitch(
                icon     = Icons.Default.Notifications,
                title    = stringResource(R.string.settings_main_kiosk_mode_title),
                subtitle = stringResource(R.string.settings_main_kiosk_mode_subtitle),
                isEnabled = kioskModeEnabled,
                onToggle  = { viewModel.setKioskModeEnabled(it) }
            )
        }
        item {
            SettingsItemWithSwitch(
                icon     = Icons.Default.Settings,
                title    = stringResource(R.string.settings_main_auto_restart_title),
                subtitle = stringResource(R.string.settings_main_auto_restart_subtitle),
                isEnabled = autoRestartEnabled,
                onToggle  = { viewModel.setAutoRestartEnabled(it) }
            )
        }
        item {
            SettingsItemWithSwitch(
                icon     = Icons.Default.Settings,
                title    = stringResource(R.string.settings_main_task_reopen_title),
                subtitle = stringResource(R.string.settings_main_task_reopen_subtitle),
                isEnabled = taskReopenEnabled,
                onToggle  = { viewModel.setTaskReopenEnabled(it) }
            )
        }

        // ─── Sekcja: Inne ────────────────────────────────────────────────
        item {
            Text(
                text  = stringResource(R.string.settings_main_other_section),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            SettingsItem(
                icon     = Icons.Default.Settings,
                title    = stringResource(R.string.settings_main_about_title),
                subtitle = stringResource(R.string.settings_main_about_subtitle),
                onClick  = { }
            )
        }

        if (com.itsorderkds.BuildConfig.DEBUG) {
            item {
                SettingsItem(
                    icon     = Icons.Default.BugReport,
                    title    = "Test Crash & Auto-Restart",
                    subtitle = "Symuluje crash aplikacji i sprawdza auto-restart (tylko DEBUG)",
                    onClick  = {
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

    // Dostępne dźwięki z zasobów raw
    val availableSounds: List<SoundItem> by lazy {
        listOf(
            SoundItem(app.getString(R.string.sound_name_default),      "android.resource://com.itsorderkds/${R.raw.alarm1}"),
            SoundItem(app.getString(R.string.sound_name_default),      "android.resource://com.itsorderkds/${R.raw.order_iphone}"),
            SoundItem(app.getString(R.string.sound_name_soft_chime),   "android.resource://com.itsorderkds/${R.raw.sound1}"),
            SoundItem(app.getString(R.string.sound_name_classic_bell), "android.resource://com.itsorderkds/${R.raw.sound2}")
        )
    }

    // Zachowujemy dla kompatybilności wstecznej
    val types: List<NotificationType> = listOf(NotificationType.ORDER_ALARM)

    // Dla kompatybilności z TabRow (nie używane aktualnie)
    private val _currentTypeIndex = MutableStateFlow(0)
    val currentTypeIndexFlow: StateFlow<Int> = _currentTypeIndex

    private val _selectedUri = MutableStateFlow<String?>(null)
    val selectedUri: StateFlow<String?> = _selectedUri

    private val _isMuted = MutableStateFlow(false)
    val isMutedFlow: StateFlow<Boolean> = _isMuted

    init {
        viewModelScope.launch {
            _selectedUri.value = prefs.getKdsNotificationSoundUri()
                ?: "android.resource://com.itsorderkds/${R.raw.alarm1}"
            _isMuted.value = prefs.isNotificationSoundMuted("order_alarm")
        }
    }

    fun selectType(index: Int) {}  // nieużywane, zachowane dla kompatybilności

    /** Ustaw dźwięk i zaktualizuj kanał systemowy Android. */
    fun setNotificationSound(uri: String, context: android.content.Context) {
        viewModelScope.launch {
            prefs.setKdsNotificationSoundUri(uri)
            _selectedUri.value = uri
            NotificationHelper.updateKdsTicketChannelSound(context, uri)
        }
    }

    /** Stary API — zachowany dla kompatybilności. */
    fun setNotificationSound(type: NotificationType, uri: String) {
        viewModelScope.launch {
            prefs.setKdsNotificationSoundUri(uri)
            _selectedUri.value = uri
        }
    }

    fun setMuted(muted: Boolean) {
        viewModelScope.launch {
            prefs.setNotificationSoundMuted("order_alarm", muted)
            prefs.setKdsSoundMuted(muted)   // synchronizuj szybki przełącznik z panelu KDS
            _isMuted.value = muted
        }
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


