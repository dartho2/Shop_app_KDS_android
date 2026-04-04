package com.itsorderkds.ui.product.detail

/**
 * Ekran edycji szczegółów produktu
 *
 * NIEUŻYWANY - Edycja produktów została wyłączona.
 * Pozostawiony w kodzie dla ewentualnego przyszłego użycia.
 * Aktualnie dostępna tylko zmiana dostępności produktów przez Switch w listach.
 */

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itsorderkds.data.model.Category
import com.itsorderkds.data.model.LanguageOption
import com.itsorderkds.data.model.ProductProductTypeEnum
import com.itsorderkds.data.model.ProductTypeEnum
import com.itsorderkds.data.model.StockStatusEnum
import com.itsorderkds.ui.theme.home.view.PillSwitch

@Composable
fun ProductDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val product = uiState.product

    when {
        uiState.isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        product != null -> {
            // Główna, przewijana kolumna dla całego formularza
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp) // Odstęp między sekcjami
            ) {
                if (uiState.languages.isNotEmpty()) {
                    LanguageBar(
                        options = uiState.languages,
                        selectedCode = uiState.languageCode,
                        onSelect = { code -> viewModel.onLanguageSelected(code) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                // --- SEKCJA: PODSTAWOWE INFORMACJE ---
                FormSection(title = "Podstawowe Informacje") {
                    OutlinedTextField(
                        value = product.name,
                        onValueChange = { viewModel.onProductChange(product.copy(name = it)) },
                        label = { Text("Nazwa produktu") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = product.description ?: "",
                        onValueChange = { viewModel.onProductChange(product.copy(description = it)) },
                        label = { Text("Opis") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = product.shortDescription ?: "",
                        onValueChange = { viewModel.onProductChange(product.copy(shortDescription = it)) },
                        label = { Text("Krótki opis (short_description)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = product.unit ?: "",
                        onValueChange = { viewModel.onProductChange(product.copy(unit = it)) },
                        label = { Text("Jednostka (unit)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = product.sku ?: "",
                        onValueChange = { viewModel.onProductChange(product.copy(sku = it)) },
                        label = { Text("SKU") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = product.quantity?.toString() ?: "",
                        onValueChange = { viewModel.onProductChange(product.copy(quantity = it.toIntOrNull())) },
                        label = { Text("Ilość") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                // --- SEKCJA: TYP I STATUS ---
                FormSection(title = "Typ i Status") {
                    // POPRAWKA: Używamy nowego komponentu EnumDropdown
                    EnumDropdown(
                        label = "Typ (Wymagane)",
                        options = ProductTypeEnum.values(),
                        selectedOption = product.type,
                        onOptionSelected = { viewModel.onProductChange(product.copy(type = it)) }
                    )

                    // POPRAWKA: Drugi EnumDropdown dla productType
                    EnumDropdown(
                        label = "Rodzaj produktu (Wymagane)",
                        options = ProductProductTypeEnum.values(),
                        selectedOption = product.productType,
                        onOptionSelected = { viewModel.onProductChange(product.copy(productType = it)) }
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("Status produktu", style = MaterialTheme.typography.bodyMedium)
                    PillSwitch(
                        items = listOf("Aktywny", "Nieaktywny"),
                        selectedIndex = if (product.status) 1 else 0,
                        onIndexSelect = { index ->
                            val newStatus = if (index == 0) false else true
                            viewModel.onProductChange(product.copy(status = newStatus))
                        }
                    )
                }
                // --- SEKCJA: PLIKI CYFROWE (tylko dla DIGITAL) ---
                FormSection(title = "Pliki cyfrowe") {
                    if (product.productType?.name == "DIGITAL") {
                        IdListInput(
                            label = "digital_file_ids",
                            ids = uiState.digitalFileIds,
                            onAdd = { id -> viewModel.addDigitalFileId(id) },
                            onRemove = { id -> viewModel.removeDigitalFileId(id) }
                        )
                        Text("Wymagane dla produktu DIGITAL.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Nie dotyczy (produkt PHYSICAL).")
                    }
                }

                // --- SEKCJA: KATEGORIE (ID wymagane przez API) ---
                // --- SEKCJA: KATEGORIE ---
                FormSection(title = "Kategorie") {
                    // podgląd wybranych nazw
                    if (uiState.categoryIds.isEmpty()) {
                        Text("Brak przypisanych kategorii.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        val names = uiState.categoryIds
                            .mapNotNull { id -> uiState.categoryOptions.find { it.id == id }?.name }
                        Text("Wybrane: ${names.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(Modifier.height(8.dp))

                    var showDialog by remember { mutableStateOf(false) }
                    Button(
                        onClick = { showDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Zarządzaj kategoriami") }

                    if (showDialog) {
                        CategoryMultiSelectDialog(
                            options = uiState.categoryOptions,
                            selectedIds = uiState.categoryIds,
                            onDismiss = { showDialog = false },
                            onConfirm = { ids ->
                                viewModel.setCategoryIds(ids)
                                showDialog = false
                            }
                        )
                    }
                }


// --- SEKCJA: ATRYBUTY (ID) ---
                FormSection(title = "Atrybuty (ID)") {
                    IdListInput(
                        label = "attributes_ids",
                        ids = uiState.attributeIds,
                        onAdd = { id -> viewModel.addAttributeId(id) },
                        onRemove = { id -> viewModel.removeAttributeId(id) }
                    )
                }

                // --- SEKCJA: CENY I DOSTĘPNOŚĆ ---
                FormSection(title = "Ceny i Dostępność") {
                    OutlinedTextField(
                        value = product.price?.toString() ?: "",
                        onValueChange = { viewModel.onProductChange(product.copy(price = it.toDoubleOrNull())) },
                        label = { Text("Cena") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = (product.discount as? Number)?.toString() ?: "",
                        onValueChange = { viewModel.onProductChange(product.copy(discount = it.toDoubleOrNull())) },
                        label = { Text("Rabat (%) / discount") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = product.salePrice?.toString() ?: "",
                        onValueChange = { viewModel.onProductChange(product.copy(salePrice = it.toDoubleOrNull())) },
                        label = { Text("Cena promocyjna (opcjonalnie)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("Status dostępności", style = MaterialTheme.typography.bodyMedium)
                    PillSwitch(
                        items = listOf("Aktywny", "Nieaktywny"),
                        selectedIndex = if (product.status) 0 else 1,   // ✅ int, nie boolean
                        onIndexSelect = { index ->
                            val newStatus = index == 0
                            viewModel.onProductChange(product.copy(status = newStatus))
                        }
                    )
                }

                // --- SEKCJA: USTAWIENIA ---
                FormSection(title = "Ustawienia") {
                    FormSwitch(
                        label = "Produkt polecany",
                        checked = product.isFeatured ?: false,
                        onCheckedChange = { viewModel.onProductChange(product.copy(isFeatured = it)) }
                    )
                    FormSwitch(
                        label = "Włącz promocję",
                        checked = product.isSaleEnable ?: false,
                        onCheckedChange = { viewModel.onProductChange(product.copy(isSaleEnable = it)) }
                    )
                }

                // --- SEKCJA: KATEGORIE (Placeholder) ---
//                FormSection(title = "Kategorie") {
//                    // Wyświetlanie aktualnie wybranych kategorii
//                    if (product.categories.isEmpty()) {
//                        Text("Brak przypisanych kategorii.", style = MaterialTheme.typography.bodyMedium)
//                    } else {
//                        Text(
//                            text = "Wybrane: " + product.categories.joinToString(", ") { it.name },
//                            style = MaterialTheme.typography.bodyMedium
//                        )
//                    }
//                    Spacer(Modifier.height(8.dp))
//                    Button(
//                        onClick = { /* TODO: Otwórz dialog wielokrotnego wyboru kategorii */ },
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Text("Zarządzaj kategoriami")
//                    }
//                }
//
//                // TODO: Dodaj kolejne sekcje dla pozostałych pól (galeria, dodatki, alergeny itp.)
            }
        }
        else -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nie udało się załadować produktu.")
            }
        }
    }
}
@Composable
fun CategoryMultiSelectDialog(
    options: List<Category>,
    selectedIds: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    // lokalny stan dialogu – KOPIA zaznaczeń jako stan Compose
    var local by remember(options, selectedIds) {
        mutableStateOf(selectedIds.toSet())
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wybierz kategorie") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn {
                items(
                    items = options,
                    key = { it.id } // stabilny klucz
                ) { opt ->
                    val checked = opt.id in local

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .toggleable( // jeden handler – klik w cały wiersz
                                value = checked,
                                onValueChange = { newChecked ->
                                    local = if (newChecked) local + opt.id else local - opt.id
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null // sterujemy toggleable na Row
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(opt.name)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(local.toList()) }) { Text("Zapisz") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}
@Composable
private fun IdListInput(
    label: String,
    ids: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("$label (ID)") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty()) {
                        // wspiera wklejenie po przecinku/spacji/nowej linii
                        trimmed
                            .split(',', ' ', '\n', '\t')
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .forEach(onAdd)
                        text = ""
                    }
                }
            ) { Text("Dodaj") }
        }
        if (ids.isEmpty()) {
            Text("Brak pozycji.", style = MaterialTheme.typography.bodyMedium)
        } else {
            // Proste „chipsy” na listę ID (tu jako zwykłe przyciski do usuwania)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ids.forEach { id ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(id, style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { onRemove(id) }) { Text("Usuń") }
                    }
                }
            }
        }
    }
}

// --- Komponenty pomocnicze dla formularza ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Enum<T>> EnumDropdown(
    label: String,
    options: Array<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            // Wyświetlamy nazwę wybranego enuma
            value = selectedOption?.name ?: "",
            onValueChange = {}, // Puste, bo pole jest tylko do odczytu
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor() // To jest "kotwica" dla rozwijanego menu
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption.name) },
                    onClick = {
                        onOptionSelected(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}
@Composable
fun LanguageBar(
    options: List<LanguageOption>,
    selectedCode: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            val selected = opt.locale.equals(selectedCode, ignoreCase = true)
            Button(
                onClick = { onSelect(opt.locale) },
                enabled = !selected
            ) {
                Text(opt.locale)
            }
        }
    }
}
@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun FormSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
