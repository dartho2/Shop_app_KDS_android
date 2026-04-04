package com.itsorderkds.ui.settings.printer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.itsorderkds.data.model.Printer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ekran zarządzania drukarkami - lista wszystkich drukarek.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintersListScreen(
    navController: NavController,
    viewModel: PrintersViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val printers by viewModel.printers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var printerToEdit by remember { mutableStateOf<Printer?>(null) }
    var printerToDelete by remember { mutableStateOf<Printer?>(null) }
    var aidlTestMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, "Dodaj drukarkę")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                printers.isEmpty() -> {
                    // Pusty stan
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Brak drukarek",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Dodaj pierwszą drukarkę klikając przycisk +",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                else -> {
                    // Lista drukarek
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = printers,
                            key = { _, printer -> printer.id }
                        ) { index, printer ->
                            PrinterListItem(
                                printer = printer,
                                onEdit = { printerToEdit = printer },
                                onDelete = { printerToDelete = printer },
                                onToggleEnabled = { viewModel.toggleEnabled(printer.id) }
                            )
                        }
                    }
                }
            }

            // Komunikat o błędzie
            errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            // Komunikat testu AIDL
            aidlTestMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (errorMessage != null) 80.dp else 16.dp),
                    action = {
                        TextButton(onClick = { aidlTestMessage = null }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    }

    // Dialog dodawania/edycji
    if (showAddDialog || printerToEdit != null) {
        AddEditPrinterDialog(
            printer = printerToEdit,
            onDismiss = {
                showAddDialog = false
                printerToEdit = null
            },
            onSave = { printer: Printer ->
                if (printerToEdit != null) {
                    viewModel.updatePrinter(printerToEdit!!.id, printer)
                } else {
                    viewModel.addPrinter(printer)
                }
                showAddDialog = false
                printerToEdit = null
            }
        )
    }

    // Dialog potwierdzenia usunięcia
    printerToDelete?.let { printer ->
        AlertDialog(
            onDismissRequest = { printerToDelete = null },
            title = { Text("Usuń drukarkę") },
            text = { Text("Czy na pewno chcesz usunąć drukarkę '${printer.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePrinter(printer.id)
                        printerToDelete = null
                    }
                ) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { printerToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

/**
 * Pojedynczy element listy drukarek.
 */
@Composable
private fun PrinterListItem(
    printer: Printer,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lewa strona: Nazwa + Info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nazwa + Detale
                Column {
                    Text(
                        text = printer.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    // Typ drukarki pod nazwą
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = printer.printerType.getLabel(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = printer.getProfileDisplayName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = printer.deviceId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Prawa strona: Switch + Przyciski
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Switch ON/OFF
                Switch(
                    checked = printer.enabled,
                    onCheckedChange = { onToggleEnabled() }
                )

                // Przycisk Edytuj
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edytuj")
                }

                // Przycisk Usuń
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "Usuń",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
