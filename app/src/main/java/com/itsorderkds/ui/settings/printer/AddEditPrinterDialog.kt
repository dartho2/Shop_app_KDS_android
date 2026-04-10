package com.itsorderkds.ui.settings.printer

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.itsorderkds.data.model.Printer
import com.itsorderkds.data.model.PrinterConnectionType
import com.itsorderkds.data.model.PrinterProfile
import com.itsorderkds.data.model.PrinterType
import timber.log.Timber
import java.util.UUID

/**
 * Dialog dodawania/edycji drukarki.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPrinterDialog(
    printer: Printer? = null,
    onDismiss: () -> Unit,
    onSave: (Printer) -> Unit
) {
    val context = LocalContext.current
    val isEditMode = printer != null

    // --- Stany formularza ---
    var name by remember { mutableStateOf(printer?.name ?: "") }
    var selectedConnectionType by remember { mutableStateOf(printer?.connectionType ?: PrinterConnectionType.BLUETOOTH) }
    var selectedPrinterType by remember { mutableStateOf(printer?.printerType ?: PrinterType.STANDARD) }

    // deviceId: dla BT to adres MAC, dla Builtin to "builtin", dla Network to puste (bo używamy networkIp)
    var selectedDeviceId by remember { mutableStateOf(printer?.deviceId ?: "") }

    var networkIp by remember { mutableStateOf(printer?.networkIp ?: "") }
    var networkPort by remember { mutableStateOf(printer?.networkPort?.toString() ?: "9100") }

    var selectedProfile by remember { mutableStateOf(
        PrinterProfile.fromId(printer?.profileId ?: "profile_custom")
    )}
    var selectedTemplate by remember { mutableStateOf(printer?.templateId ?: "template_standard") }
    var customEncoding by remember { mutableStateOf(printer?.encoding ?: "UTF-8") }
    var customCodepage by remember { mutableStateOf(printer?.codepage?.toString() ?: "") }
    var autoCut by remember { mutableStateOf(printer?.autoCut ?: false) }
    var enabled by remember { mutableStateOf(printer?.enabled ?: true) }
    var plainTextMode by remember { mutableStateOf(printer?.plainTextMode ?: false) }

    // Lista sparowanych urządzeń BT
    val pairedDevices = remember { getPairedBluetoothDevices(context) }

    // Czy pokazać niestandardowe pola (Custom profile)
    val isCustomProfile = selectedProfile == PrinterProfile.CUSTOM

    // Dropdown expanded states
    var connectionTypeExpanded by remember { mutableStateOf(false) }
    var deviceExpanded by remember { mutableStateOf(false) }
    var printerTypeExpanded by remember { mutableStateOf(false) }
    var profileExpanded by remember { mutableStateOf(false) }
    var templateExpanded by remember { mutableStateOf(false) }

    // --- Efekty uboczne (Logika automatyczna) ---

    // Automatyczne ustawienie deviceId dla BUILTIN przy inicjalizacji lub zmianie
    LaunchedEffect(selectedConnectionType) {
        if (selectedConnectionType == PrinterConnectionType.BUILTIN) {
            selectedDeviceId = "builtin"
            // Jeśli nazwa pusta, zasugeruj domyślną
            if (name.isBlank()) {
                name = "Wbudowana (H10)"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditMode) "Edytuj drukarkę" else "Dodaj drukarkę") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Nazwa drukarki
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa drukarki") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("np. Drukarka Główna") }
                )

                // 2. Typ połączenia (Bluetooth, Sieć, Wbudowana)
                ExposedDropdownMenuBox(
                    expanded = connectionTypeExpanded,
                    onExpandedChange = { connectionTypeExpanded = !connectionTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedConnectionType.getLabel(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Typ połączenia") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = connectionTypeExpanded) },
                        supportingText = { Text("Jak aplikacja łączy się z drukarką") }
                    )
                    ExposedDropdownMenu(
                        expanded = connectionTypeExpanded,
                        onDismissRequest = { connectionTypeExpanded = false }
                    ) {
                        PrinterConnectionType.entries.forEach { connType ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(connType.getLabel())
                                        Text(
                                            text = connType.description,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                onClick = {
                                    selectedConnectionType = connType
                                    connectionTypeExpanded = false

                                    // Automatyczne czyszczenie/ustawianie pól przy zmianie typu
                                    when (connType) {
                                        PrinterConnectionType.BUILTIN -> {
                                            selectedDeviceId = "builtin"
                                            networkIp = ""
                                            networkPort = "9100"
                                        }
                                        PrinterConnectionType.NETWORK -> {
                                            selectedDeviceId = "" // Tutaj ID to będzie IP przy zapisie
                                        }
                                        PrinterConnectionType.BLUETOOTH -> {
                                            selectedDeviceId = "" // Użytkownik musi wybrać z listy
                                            networkIp = ""
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // 3. Pola specyficzne dla wybranego typu połączenia
                when (selectedConnectionType) {
                    PrinterConnectionType.BLUETOOTH -> {
                        // Wybór urządzenia BT
                        ExposedDropdownMenuBox(
                            expanded = deviceExpanded,
                            onExpandedChange = { deviceExpanded = !deviceExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedDeviceId.ifEmpty { "Wybierz urządzenie..." },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Urządzenie Bluetooth") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deviceExpanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = deviceExpanded,
                                onDismissRequest = { deviceExpanded = false }
                            ) {
                                pairedDevices.forEach { device ->
                                    DropdownMenuItem(
                                        text = { Text("${device.first} (${device.second})") },
                                        onClick = {
                                            selectedDeviceId = device.second
                                            deviceExpanded = false
                                        }
                                    )
                                }
                                if (pairedDevices.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Brak sparowanych urządzeń") },
                                        onClick = {},
                                        enabled = false
                                    )
                                }
                            }
                        }
                    }
                    PrinterConnectionType.NETWORK -> {
                        // IP drukarki
                        OutlinedTextField(
                            value = networkIp,
                            onValueChange = { networkIp = it },
                            label = { Text("Adres IP drukarki") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("np. 192.168.1.100") }
                        )

                        // Port
                        OutlinedTextField(
                            value = networkPort,
                            onValueChange = { networkPort = it },
                            label = { Text("Port") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("9100") },
                            supportingText = { Text("Zwykle 9100 dla RAW TCP printing") }
                        )
                    }
                    PrinterConnectionType.BUILTIN -> {
                        // Informacja o wbudowanej drukarce (zablokowane pole dla jasności)
                        OutlinedTextField(
                            value = "Używa usługi systemowej (AIDL)",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Adres urządzenia") },
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = {
                                Text("Automatycznie wykrywa drukarkę wbudowaną w terminal POS")
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.primary,
                                disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // 4. Typ drukarki (KITCHEN, STANDARD, BAR itp.)
                ExposedDropdownMenuBox(
                    expanded = printerTypeExpanded,
                    onExpandedChange = { printerTypeExpanded = !printerTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPrinterType.getLabel(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Przeznaczenie drukarki") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = printerTypeExpanded) },
                        supportingText = { Text("Określa rolę (np. wydruk do kuchni, rachunek)") }
                    )
                    ExposedDropdownMenu(
                        expanded = printerTypeExpanded,
                        onDismissRequest = { printerTypeExpanded = false }
                    ) {
                        PrinterType.getAllOptions().forEach { printerType ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(printerType.getLabel())
                                        Text(
                                            text = printerType.description,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                onClick = {
                                    selectedPrinterType = printerType
                                    printerTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                // 5. Wybór profilu
                ExposedDropdownMenuBox(
                    expanded = profileExpanded,
                    onExpandedChange = { profileExpanded = !profileExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedProfile.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Profil sterownika") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = profileExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = profileExpanded,
                        onDismissRequest = { profileExpanded = false }
                    ) {
                        PrinterProfile.getAllProfiles().forEach { profile ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(profile.displayName)
                                        Text(
                                            text = profile.description,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                onClick = {
                                    selectedProfile = profile
                                    // Auto-fill z profilu (jeśli nie Custom)
                                    if (profile != PrinterProfile.CUSTOM) {
                                        customEncoding = profile.encoding
                                        customCodepage = profile.codepage?.toString() ?: ""
                                        autoCut = profile.autoCut
                                    }
                                    // Profil "Zwykła drukarka" automatycznie włącza plainTextMode
                                    if (profile == PrinterProfile.PLAIN_TEXT) {
                                        plainTextMode = true
                                    } else if (profile != PrinterProfile.CUSTOM) {
                                        plainTextMode = false
                                    }
                                    profileExpanded = false
                                }
                            )
                        }
                    }
                }

                // 6. Pola niestandardowe (tylko dla Custom)
                if (isCustomProfile) {
                    OutlinedTextField(
                        value = customEncoding,
                        onValueChange = { customEncoding = it },
                        label = { Text("Encoding") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("np. Cp852, UTF-8") }
                    )

                    OutlinedTextField(
                        value = customCodepage,
                        onValueChange = { customCodepage = it },
                        label = { Text("Codepage (opcjonalne)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("np. 13") }
                    )
                }

                // 7. Wybór szablonu
                ExposedDropdownMenuBox(
                    expanded = templateExpanded,
                    onExpandedChange = { templateExpanded = !templateExpanded }
                ) {
                    OutlinedTextField(
                        value = getTemplateDisplayName(selectedTemplate),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Szablon wydruku") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = templateExpanded,
                        onDismissRequest = { templateExpanded = false }
                    ) {
                        listOf(
                            "template_standard" to "Standardowy",
                            "template_compact" to "Kompaktowy",
                            "template_kitchen_only" to "Tylko Kuchnia"
                        ).forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedTemplate = id
                                    templateExpanded = false
                                }
                            )
                        }
                    }
                }

                // 8. Opcje przełączników
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Automatyczne cięcie papieru")
                    Switch(
                        checked = autoCut,
                        onCheckedChange = { autoCut = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Drukarka aktywna")
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }

                // Tryb zwykłego tekstu — tylko dla drukarek sieciowych
                if (selectedConnectionType == PrinterConnectionType.NETWORK) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tryb zwykłego tekstu")
                            Text(
                                text = "Dla drukarek biurowych/laserowych (nie ESC/POS). Wyłącz dla drukarek termicznych POS.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = plainTextMode,
                            onCheckedChange = { plainTextMode = it },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Walidacja podstawowa
                    if (name.isBlank()) {
                        Timber.w("AddEditPrinterDialog: Nazwa jest pusta")
                        return@TextButton
                    }

                    // Walidacja specyficzna dla typu połączenia
                    when (selectedConnectionType) {
                        PrinterConnectionType.BLUETOOTH -> {
                            if (selectedDeviceId.isBlank()) {
                                Timber.w("AddEditPrinterDialog: Bluetooth deviceId jest puste")
                                return@TextButton
                            }
                        }
                        PrinterConnectionType.NETWORK -> {
                            if (networkIp.isBlank()) {
                                Timber.w("AddEditPrinterDialog: Network IP jest puste")
                                return@TextButton
                            }
                            val port = networkPort.toIntOrNull()
                            if (port == null || port !in 1..65535) {
                                Timber.w("AddEditPrinterDialog: Network port nieprawidłowy")
                                return@TextButton
                            }
                        }
                        PrinterConnectionType.BUILTIN -> {
                            // Tutaj walidacja jest prosta, bo zawsze ustawiamy "builtin"
                        }
                    }

                    // Ustaw ostateczne deviceId w zależności od typu połączenia
                    val finalDeviceId = when (selectedConnectionType) {
                        PrinterConnectionType.BLUETOOTH -> selectedDeviceId
                        PrinterConnectionType.NETWORK -> networkIp // Dla sieci używamy IP jako deviceId
                        PrinterConnectionType.BUILTIN -> "builtin" // ZAWSZE "builtin" dla wbudowanej
                    }

                    // Utwórz obiekt Printer
                    val newPrinter = Printer(
                        id = printer?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        deviceId = finalDeviceId,
                        connectionType = selectedConnectionType,
                        networkIp = if (selectedConnectionType == PrinterConnectionType.NETWORK) networkIp else null,
                        networkPort = if (selectedConnectionType == PrinterConnectionType.NETWORK) {
                            networkPort.toIntOrNull() ?: 9100
                        } else {
                            9100
                        },
                        printerType = selectedPrinterType,
                        profileId = selectedProfile.id,
                        templateId = selectedTemplate,
                        encoding = if (isCustomProfile) customEncoding else selectedProfile.encoding,
                        codepage = if (isCustomProfile) {
                            customCodepage.toIntOrNull()
                        } else {
                            selectedProfile.codepage
                        },
                        autoCut = autoCut,
                        enabled = enabled,
                        order = printer?.order ?: 0,
                        plainTextMode = plainTextMode
                    )

                    onSave(newPrinter)
                }
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

/**
 * Pobiera listę sparowanych urządzeń Bluetooth.
 */
private fun getPairedBluetoothDevices(context: Context): List<Pair<String, String>> {
    return try {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter

        @Suppress("MissingPermission")
        bluetoothAdapter?.bondedDevices?.map { device ->
            (device.name ?: "Unknown") to device.address
        } ?: emptyList()
    } catch (e: Exception) {
        Timber.e(e, "Błąd pobierania urządzeń BT")
        emptyList()
    }
}

/**
 * Zwraca czytelną nazwę szablonu.
 */
private fun getTemplateDisplayName(templateId: String): String {
    return when (templateId) {
        "template_standard" -> "Standardowy"
        "template_compact" -> "Kompaktowy"
        "template_kitchen_only" -> "Tylko Kuchnia"
        else -> templateId
    }
}
