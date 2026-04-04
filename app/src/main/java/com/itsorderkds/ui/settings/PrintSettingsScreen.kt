@file:OptIn(ExperimentalMaterial3Api::class)

package com.itsorderkds.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsorderkds.R
import com.itsorderkds.data.preferences.AppPreferencesManager
import com.itsorderkds.data.preferences.PrinterPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrintSettingsViewModel @Inject constructor(
    private val appPreferencesManager: AppPreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _autoPrintEnabled = MutableStateFlow(false)
    val autoPrintEnabled: StateFlow<Boolean> = _autoPrintEnabled

    private val _autoPrintAcceptedEnabled = MutableStateFlow(false)
    val autoPrintAcceptedEnabled: StateFlow<Boolean> = _autoPrintAcceptedEnabled

    private val _autoPrintAcceptedPrinters = MutableStateFlow("both")
    val autoPrintAcceptedPrinters: StateFlow<String> = _autoPrintAcceptedPrinters

    private val _autoPrintDineInEnabled = MutableStateFlow(false)
    val autoPrintDineInEnabled: StateFlow<Boolean> = _autoPrintDineInEnabled

    private val _autoPrintDineInPrinters = MutableStateFlow("both")
    val autoPrintDineInPrinters: StateFlow<String> = _autoPrintDineInPrinters

    private val _autoPrintKitchenEnabled = MutableStateFlow(true)
    val autoPrintKitchenEnabled: StateFlow<Boolean> = _autoPrintKitchenEnabled

    private val _hasKitchenPrinter = MutableStateFlow(false)
    val hasKitchenPrinter: StateFlow<Boolean> = _hasKitchenPrinter

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _autoPrintEnabled.value = appPreferencesManager.getAutoPrintEnabled()
            _autoPrintAcceptedEnabled.value = appPreferencesManager.getAutoPrintAcceptedEnabled()
            _autoPrintAcceptedPrinters.value = appPreferencesManager.getAutoPrintAcceptedPrinters()
            _autoPrintDineInEnabled.value = appPreferencesManager.getAutoPrintDineInEnabled()
            _autoPrintDineInPrinters.value = appPreferencesManager.getAutoPrintDineInPrinters()
            _autoPrintKitchenEnabled.value = appPreferencesManager.getAutoPrintKitchenEnabled()

            // Sprawdź czy jest skonfigurowana drukarka kuchenna
            val kitchenPrinters = PrinterPreferences.getPrinters(context).filter {
                it.printerType == com.itsorderkds.data.model.PrinterType.KITCHEN && it.enabled
            }
            _hasKitchenPrinter.value = kitchenPrinters.isNotEmpty()
        }
    }

    fun setAutoPrintEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesManager.setAutoPrintEnabled(enabled)
            _autoPrintEnabled.value = enabled
        }
    }

    fun setAutoPrintAcceptedEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesManager.setAutoPrintAcceptedEnabled(enabled)
            _autoPrintAcceptedEnabled.value = enabled
        }
    }

    fun setAutoPrintAcceptedPrinters(printers: String) {
        viewModelScope.launch {
            appPreferencesManager.setAutoPrintAcceptedPrinters(printers)
            _autoPrintAcceptedPrinters.value = printers
        }
    }

    fun setAutoPrintDineInEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesManager.setAutoPrintDineInEnabled(enabled)
            _autoPrintDineInEnabled.value = enabled
        }
    }

    fun setAutoPrintDineInPrinters(printers: String) {
        viewModelScope.launch {
            appPreferencesManager.setAutoPrintDineInPrinters(printers)
            _autoPrintDineInPrinters.value = printers
        }
    }

    fun setAutoPrintKitchenEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesManager.setAutoPrintKitchenEnabled(enabled)
            _autoPrintKitchenEnabled.value = enabled
        }
    }

    fun refreshKitchenPrinterStatus() {
        viewModelScope.launch {
            val kitchenPrinters = PrinterPreferences.getPrinters(context).filter {
                it.printerType == com.itsorderkds.data.model.PrinterType.KITCHEN && it.enabled
            }
            _hasKitchenPrinter.value = kitchenPrinters.isNotEmpty()
        }
    }
}

@Composable
fun PrintSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PrintSettingsViewModel = hiltViewModel()
) {
    val autoPrintEnabled by viewModel.autoPrintEnabled.collectAsState()
    val autoPrintAcceptedEnabled by viewModel.autoPrintAcceptedEnabled.collectAsState()
    val autoPrintAcceptedPrinters by viewModel.autoPrintAcceptedPrinters.collectAsState()
    val autoPrintDineInEnabled by viewModel.autoPrintDineInEnabled.collectAsState()
    val autoPrintDineInPrinters by viewModel.autoPrintDineInPrinters.collectAsState()
    val autoPrintKitchenEnabled by viewModel.autoPrintKitchenEnabled.collectAsState()
    val hasKitchenPrinter by viewModel.hasKitchenPrinter.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshKitchenPrinterStatus()
    }

    Scaffold(
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Sekcja: Automatyczne drukowanie przy nowym zamówieniu
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_print_auto_print_section),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_print_auto_print_new_order),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.settings_print_auto_print_new_order_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoPrintEnabled,
                                onCheckedChange = { viewModel.setAutoPrintEnabled(it) }
                            )
                        }
                    }
                }
            }

            // Sekcja: Drukowanie po zaakceptowaniu
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_print_after_accept_section),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_print_auto_print_accepted),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.settings_print_auto_print_accepted_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoPrintAcceptedEnabled,
                                onCheckedChange = { viewModel.setAutoPrintAcceptedEnabled(it) }
                            )
                        }

                        // Wybór drukarek (tylko gdy auto-druk włączony)
                        if (autoPrintAcceptedEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Drukuj na:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Drukarka główna
                                FilterChip(
                                    selected = autoPrintAcceptedPrinters == "main",
                                    onClick = { viewModel.setAutoPrintAcceptedPrinters("main") },
                                    label = { Text("Główna") },
                                    modifier = Modifier.weight(1f)
                                )

                                // Drukarka kuchenna
                                FilterChip(
                                    selected = autoPrintAcceptedPrinters == "kitchen",
                                    onClick = { viewModel.setAutoPrintAcceptedPrinters("kitchen") },
                                    label = { Text("Kuchnia") },
                                    enabled = hasKitchenPrinter,
                                    modifier = Modifier.weight(1f)
                                )

                                // Obie drukarki
                                FilterChip(
                                    selected = autoPrintAcceptedPrinters == "both",
                                    onClick = { viewModel.setAutoPrintAcceptedPrinters("both") },
                                    label = { Text("Obie") },
                                    enabled = hasKitchenPrinter,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Info gdy brak drukarki kuchennej
                            if (!hasKitchenPrinter && (autoPrintAcceptedPrinters == "kitchen" || autoPrintAcceptedPrinters == "both")) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "⚠️ Drukarka kuchenna nie jest skonfigurowana",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Sekcja: Drukowanie zamówień DINE_IN / ROOM_SERVICE
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_print_auto_print_dine_in),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_print_auto_print_dine_in),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.settings_print_auto_print_dine_in_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoPrintDineInEnabled,
                                onCheckedChange = { viewModel.setAutoPrintDineInEnabled(it) }
                            )
                        }

                        // Wybór drukarek (tylko gdy auto-druk włączony)
                        if (autoPrintDineInEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Drukuj na:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Drukarka główna
                                FilterChip(
                                    selected = autoPrintDineInPrinters == "main",
                                    onClick = { viewModel.setAutoPrintDineInPrinters("main") },
                                    label = { Text("Główna") },
                                    modifier = Modifier.weight(1f)
                                )

                                // Drukarka kuchenna
                                FilterChip(
                                    selected = autoPrintDineInPrinters == "kitchen",
                                    onClick = { viewModel.setAutoPrintDineInPrinters("kitchen") },
                                    label = { Text("Kuchnia") },
                                    enabled = hasKitchenPrinter,
                                    modifier = Modifier.weight(1f)
                                )

                                // Obie drukarki
                                FilterChip(
                                    selected = autoPrintDineInPrinters == "both",
                                    onClick = { viewModel.setAutoPrintDineInPrinters("both") },
                                    label = { Text("Obie") },
                                    enabled = hasKitchenPrinter,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Info gdy brak drukarki kuchennej
                            if (!hasKitchenPrinter && (autoPrintDineInPrinters == "kitchen" || autoPrintDineInPrinters == "both")) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "⚠️ Drukarka kuchenna nie jest skonfigurowana",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Sekcja: Drukowanie na kuchni (tylko jeśli jest skonfigurowana drukarka kuchenna)
            if (hasKitchenPrinter) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.settings_print_kitchen_section),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_print_auto_print_kitchen),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_print_auto_print_kitchen_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoPrintKitchenEnabled,
                                    onCheckedChange = { viewModel.setAutoPrintKitchenEnabled(it) }
                                )
                            }
                        }
                    }
                }
            }

            // Info jeśli nie ma drukarki kuchennej
            if (!hasKitchenPrinter) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.settings_print_no_kitchen_printer),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.settings_print_no_kitchen_printer_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

