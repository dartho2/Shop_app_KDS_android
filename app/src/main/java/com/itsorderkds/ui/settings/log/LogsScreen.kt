package com.itsorderkds.ui.settings.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itsorderkds.ui.HandleApiError
import com.itsorderkds.ui.util.LogTimeFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    modifier: Modifier = Modifier,
    viewModel: LogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHost = remember { SnackbarHostState() }

    HandleApiError(
        errorState = uiState.error,
        snackbarHostState = snackbarHost,
        onErrorShown = { viewModel.errorShown() }
    )

    LaunchedEffect(uiState.isFilterPanelVisible) {
        if (uiState.isFilterPanelVisible) sheetState.show() else sheetState.hide()
    }

    if (uiState.isFilterPanelVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleFilterVisibility() },
            sheetState = sheetState
        ) {
            FilterPanel(
                onApplyFilters = { date, level, search ->
                    viewModel.loadLogs(date = date, level = level, search = search)
                    viewModel.toggleFilterVisibility()
                },
                onDismiss = { viewModel.toggleFilterVisibility() }
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        val logSources = listOf(LogSource.API, LogSource.APP)
        TabRow(selectedTabIndex = logSources.indexOf(uiState.activeSource)) {
            logSources.forEach { source ->
                Tab(
                    selected = uiState.activeSource == source,
                    onClick = { viewModel.setActiveSource(source) },
                    text = { Text(source.displayName) }
                )
            }
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            !uiState.hasSearched -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Użyj filtra, aby pobrać logi.") }
            uiState.logs.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Brak logów dla podanych kryteriów.") }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = uiState.logs,
                    key = { i, log ->
                        "${log.timestamp}|${log.level}|${log.message?.hashCode() ?: 0}#$i"
                    }
                ) { _, log ->
                    LogItem(log)
                }
            }
        }
    }
}

@Composable
private fun FilterPanel(
    onApplyFilters: (date: String, level: String?, search: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var dateText by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var selectedLevel by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }
    val logLevels = listOf("Wszystkie", "INFO", "WARN", "ERROR", "DEBUG")

    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Filtruj logi", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = dateText, onValueChange = { dateText = it }, label = { Text("Data (RRRR-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            Column {
                Text("Poziom logów", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    logLevels.forEach { level ->
                        val isSelected = if (level == "Wszystkie") selectedLevel == null else selectedLevel == level.lowercase()
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedLevel = if (level == "Wszystkie") null else level.lowercase() },
                            label = { Text(level) },
                            leadingIcon = if (isSelected) { { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) } } else null
                        )
                    }
                }
            }
            OutlinedTextField(value = searchText, onValueChange = { searchText = it }, label = { Text("Szukaj w wiadomości") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    onApplyFilters(dateText.trim(), selectedLevel, searchText.trim().ifEmpty { null })
                    onDismiss()
                },
                modifier = Modifier.align(Alignment.End)
            ) { Text("Zastosuj filtry") }
        }
    }
}

@Composable
private fun LogItem(log: LogEntry) {
    val displayTime = remember(log.timestamp) { LogTimeFormatter.formatDevice(log.timestamp) }
    Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayTime ?: "Brak czasu",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                LogLevelBadge(level = log.level)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = log.message ?: "Brak wiadomości", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LogLevelBadge(level: String?) {
    val (color, label) = when (level?.lowercase()) {
        "error" -> MaterialTheme.colorScheme.errorContainer to "ERROR"
        "warn" -> Color(0xFFFFD700).copy(alpha = 0.3f) to "WARN"
        "info" -> MaterialTheme.colorScheme.primaryContainer to "INFO"
        "debug" -> MaterialTheme.colorScheme.tertiaryContainer to "DEBUG"
        else -> MaterialTheme.colorScheme.secondaryContainer to (level?.uppercase() ?: "LOG")
    }
    Text(
        text = label,
        color = contentColorFor(color),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.background(color, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
