@file:OptIn(ExperimentalMaterial3Api::class)

package com.itsorderkds.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsorderkds.data.model.KdsTicketState
import com.itsorderkds.data.model.PrintStatusRule
import com.itsorderkds.data.model.Printer
import com.itsorderkds.data.preferences.AppPreferencesManager
import com.itsorderkds.data.preferences.PrintRulesRepository
import com.itsorderkds.data.preferences.PrinterPreferences
import com.itsorderkds.ui.settings.print.PrintTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════
// ViewModel
// ═══════════════════════════════════════════════════════════════════════

@HiltViewModel
class PrintSettingsViewModel @Inject constructor(
    private val appPreferencesManager: AppPreferencesManager,
    private val printRulesRepository: PrintRulesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _printOnNewOrder = MutableStateFlow(false)
    val printOnNewOrder: StateFlow<Boolean> = _printOnNewOrder

    private val _statusRules = MutableStateFlow<List<PrintStatusRule>>(emptyList())
    val statusRules: StateFlow<List<PrintStatusRule>> = _statusRules

    private val _printers = MutableStateFlow<List<Printer>>(emptyList())
    val printers: StateFlow<List<Printer>> = _printers.asStateFlow()

    private val _editingRule = MutableStateFlow<PrintStatusRule?>(null)
    val editingRule: StateFlow<PrintStatusRule?> = _editingRule

    init { loadAll() }

    private fun loadAll() {
        viewModelScope.launch {
            _printOnNewOrder.value = appPreferencesManager.getAutoPrintEnabled()
            _statusRules.value = printRulesRepository.getRules()
            _printers.value = PrinterPreferences.getPrinters(context)
        }
    }

    fun setPrintOnNewOrder(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesManager.setAutoPrintEnabled(enabled)
            _printOnNewOrder.value = enabled
        }
    }

    fun toggleRule(ruleId: String) {
        viewModelScope.launch {
            val updated = _statusRules.value.map {
                if (it.id == ruleId) it.copy(enabled = !it.enabled) else it
            }
            printRulesRepository.saveRules(updated)
            _statusRules.value = updated
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            printRulesRepository.deleteRule(ruleId)
            _statusRules.value = _statusRules.value.filter { it.id != ruleId }
        }
    }

    fun startAddRule() {
        _editingRule.value = PrintStatusRule(
            id = UUID.randomUUID().toString(),
            enabled = true,
            status = KdsTicketState.READY,
            printerIds = emptyList(),
            templateOverrideId = null
        )
    }

    fun startEditRule(rule: PrintStatusRule) { _editingRule.value = rule }
    fun dismissEditDialog() { _editingRule.value = null }

    fun saveRule(rule: PrintStatusRule) {
        viewModelScope.launch {
            val current = _statusRules.value.toMutableList()
            val idx = current.indexOfFirst { it.id == rule.id }
            if (idx >= 0) current[idx] = rule else current.add(rule)
            printRulesRepository.saveRules(current)
            _statusRules.value = current
            _editingRule.value = null
        }
    }

    fun reloadPrinters() {
        viewModelScope.launch { _printers.value = PrinterPreferences.getPrinters(context) }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Screen
// ═══════════════════════════════════════════════════════════════════════

@Composable
fun PrintSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PrintSettingsViewModel = hiltViewModel()
) {
    val printOnNewOrder by viewModel.printOnNewOrder.collectAsState()
    val statusRules by viewModel.statusRules.collectAsState()
    val printers by viewModel.printers.collectAsState()
    val editingRule by viewModel.editingRule.collectAsState()

    LaunchedEffect(Unit) { viewModel.reloadPrinters() }

    Scaffold(

    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()

                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // ── Sekcja 1: Drukuj przy nowym zamówieniu ───────────────
            item {
                PrintSectionCard(title = "1. Drukuj przy nowym zamówieniu") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Automatyczny wydruk po wpłynięciu zamówienia",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Drukuje na wszystkich aktywnych drukarkach",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = printOnNewOrder,
                            onCheckedChange = { viewModel.setPrintOnNewOrder(it) }
                        )
                    }
                }
            }

            // ── Sekcja 2: Automatyczne drukowanie po statusie ────────
            item {
                PrintSectionCard(
                    title = "2. Drukuj po zmianie statusu",
                    headerAction = {
                        TextButton(onClick = { viewModel.startAddRule() }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Dodaj regułę")
                        }
                    }
                ) {
                    if (statusRules.isEmpty()) {
                        Text(
                            "Brak regul. Kliknij 'Dodaj regule' aby skonfigurowac automatyczny wydruk po konkretnym statusie KDS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        statusRules.forEachIndexed { index, rule ->
                            StatusRuleRow(
                                rule = rule,
                                printers = printers,
                                onToggle = { viewModel.toggleRule(rule.id) },
                                onEdit = { viewModel.startEditRule(rule) },
                                onDelete = { viewModel.deleteRule(rule.id) }
                            )
                            if (index < statusRules.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Sekcja 3: Drukarki ───────────────────────────────────
            item {
                PrintSectionCard(title = "3. Drukarki") {
                    if (printers.isEmpty()) {
                        Text(
                            "Brak skonfigurowanych drukarek. Przejdź do Ustawień → Drukarki aby je dodać.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        printers.forEachIndexed { index, printer ->
                            PrinterInfoRow(printer = printer)
                            if (index < printers.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Sekcja 4: Szablony ───────────────────────────────────
            item {
                PrintSectionCard(title = "4. Szablony wydruków") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrintTemplate.entries.forEach { template ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        template.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        templateDescription(template),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    template.id,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Szablon przypisany do drukarki możesz zmienić w sekcji Drukarki → Edytuj drukarkę.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    editingRule?.let { rule ->
        EditRuleDialog(
            rule = rule,
            printers = printers,
            onDismiss = { viewModel.dismissEditDialog() },
            onSave = { viewModel.saveRule(it) }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Komponenty pomocnicze
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun PrintSectionCard(
    title: String,
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                headerAction?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun StatusRuleRow(
    rule: PrintStatusRule,
    printers: List<Printer>,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val printerNames = if (rule.printerIds.isEmpty()) {
        "Wszystkie drukarki"
    } else {
        rule.printerIds.mapNotNull { id -> printers.find { it.id == id }?.name }
            .joinToString(", ").ifBlank { "Usunięte drukarki" }
    }
    val templateLabel = rule.templateOverrideId?.let {
        PrintTemplate.fromId(it).displayName
    } ?: "Szablon drukarki"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Switch(checked = rule.enabled, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusBadge(rule.status)
                Text("→", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(printerNames, style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium)
            }
            Text(
                "Szablon: $templateLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Usuń",
                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StatusBadge(state: KdsTicketState) {
    val (bg, fg) = when (state) {
        KdsTicketState.NEW -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        KdsTicketState.ACKED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        KdsTicketState.IN_PROGRESS -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        KdsTicketState.READY -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        KdsTicketState.HANDED_OFF -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        KdsTicketState.CANCELLED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Text(
        text = state.displayName,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun PrinterInfoRow(printer: Printer) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(printer.printerType.icon, style = MaterialTheme.typography.titleMedium)
        Column(modifier = Modifier.weight(1f)) {
            Text(printer.name, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
            Text(
                "${printer.connectionType.displayName} · ${printer.printerType.displayName} · ${printer.getTemplateDisplayName()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!printer.enabled) {
            Text("Wyłączona", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Dialog edycji reguły
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun EditRuleDialog(
    rule: PrintStatusRule,
    printers: List<Printer>,
    onDismiss: () -> Unit,
    onSave: (PrintStatusRule) -> Unit
) {
    var selectedStatus by remember(rule.id) { mutableStateOf(rule.status) }
    var selectedPrinterIds by remember(rule.id) { mutableStateOf(rule.printerIds.toSet()) }
    var selectedTemplate by remember(rule.id) { mutableStateOf(rule.templateOverrideId) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var templateDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reguła drukowania") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Wybór statusu ─────────────────────────────────────
                Text("Wyzwalacz (status KDS)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)

                Box {
                    OutlinedButton(
                        onClick = { statusDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatusBadge(selectedStatus)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        KdsTicketState.triggerOptions().forEach { state ->
                            DropdownMenuItem(
                                text = { StatusBadge(state) },
                                onClick = {
                                    selectedStatus = state
                                    statusDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // ── Wybór drukarek ────────────────────────────────────
                Text("Drukuj na",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPrinterIds = emptySet() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = selectedPrinterIds.isEmpty(),
                        onCheckedChange = { if (it) selectedPrinterIds = emptySet() }
                    )
                    Column {
                        Text("Wszystkie włączone drukarki",
                            style = MaterialTheme.typography.bodyMedium)
                        Text("Drukuje na każdej aktywnej drukarce",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                printers.filter { it.enabled }.forEach { printer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val s = selectedPrinterIds.toMutableSet()
                                if (printer.id in s) s.remove(printer.id) else s.add(printer.id)
                                selectedPrinterIds = s
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = printer.id in selectedPrinterIds,
                            onCheckedChange = { checked ->
                                val s = selectedPrinterIds.toMutableSet()
                                if (checked) s.add(printer.id) else s.remove(printer.id)
                                selectedPrinterIds = s
                            }
                        )
                        Column {
                            Text(
                                "${printer.printerType.icon} ${printer.name}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${printer.connectionType.displayName} · ${printer.printerType.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Wybór szablonu ────────────────────────────────────
                Text("Szablon wydruku",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)

                Box {
                    OutlinedButton(
                        onClick = { templateDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            selectedTemplate?.let { PrintTemplate.fromId(it).displayName }
                                ?: "Szablon przypisany do drukarki",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = templateDropdownExpanded,
                        onDismissRequest = { templateDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Szablon przypisany do drukarki",
                                        fontWeight = FontWeight.Medium)
                                    Text("Każda drukarka używa własnego szablonu",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                selectedTemplate = null
                                templateDropdownExpanded = false
                            }
                        )
                        HorizontalDivider()
                        PrintTemplate.entries.forEach { tpl ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(tpl.displayName, fontWeight = FontWeight.Medium)
                                        Text(
                                            templateDescription(tpl),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedTemplate = tpl.id
                                    templateDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    rule.copy(
                        status = selectedStatus,
                        printerIds = selectedPrinterIds.toList(),
                        templateOverrideId = selectedTemplate
                    )
                )
            }) { Text("Zapisz") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}

private fun templateDescription(template: PrintTemplate): String = when (template) {
    PrintTemplate.STANDARD     -> "Pełny paragon: ceny, płatność, adres"
    PrintTemplate.COMPACT      -> "Skrócony: bez adresu i kuriera"
    PrintTemplate.DETAILED     -> "Szczegółowy: wszystkie pola + podatki"
    PrintTemplate.MINIMAL      -> "Minimalistyczny: tylko pozycje i numer"
    PrintTemplate.KITCHEN_ONLY -> "Kuchenny: tylko pozycje z printer=KITCHEN (bez cen)"
}
