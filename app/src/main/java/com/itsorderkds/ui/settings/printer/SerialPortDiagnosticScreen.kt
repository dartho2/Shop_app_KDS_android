package com.itsorderkds.ui.settings.printer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ekran diagnostyczny dla portów szeregowych i drukarek wbudowanych.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SerialPortDiagnosticScreen(
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    var diagnosticText by remember { mutableStateOf("Kliknij 'Skanuj porty' aby rozpocząć...") }
    var isScanning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostyka drukarki wbudowanej") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Powrót")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ℹ�� Informacje",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Ten ekran sprawdza czy urządzenie ma wbudowaną drukarkę (np. Sunmi H10). Sprawdzane są typowe porty szeregowe używane w urządzeniach POS.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Przyciski akcji
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        isScanning = true
                        diagnosticText = "Skanowanie portów szeregowych...\n"

                        // Wykonaj skanowanie w tle
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                SerialPortHelper.getDiagnosticInfo()
                            }
                            diagnosticText = result
                            isScanning = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isScanning
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isScanning) "Skanowanie..." else "Skanuj porty")
                }

                OutlinedButton(
                    onClick = {
                        diagnosticText = SerialPortHelper.testPrintExample()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isScanning
                ) {
                    Text("Instrukcja")
                }
            }

            // Informacje o urządzeniu
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📱 Informacje o urządzeniu",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    InfoRow("Producent:", android.os.Build.MANUFACTURER)
                    InfoRow("Model:", android.os.Build.MODEL)
                    InfoRow("Urządzenie:", android.os.Build.DEVICE)
                    InfoRow("Sunmi:", if (SerialPortHelper.isSunmiDevice()) "✅ TAK" else "❌ NIE")
                }
            }

            // Wynik diagnostyki
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "📋 Wyniki diagnostyki",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = diagnosticText,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }

            // Uwagi i wyjaśnienia
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠️ Ważne informacje",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = """
                            • Jeśli porty istnieją ale są niedostępne do zapisu, aplikacja wymaga uprawnień root lub specjalnych SELinux rules
                            • Większość standardowych urządzeń Android NIE MA wbudowanej drukarki
                            • Drukarki wbudowane występują głównie w urządzeniach POS (Sunmi, Urovo, itp.)
                            • Aby faktycznie drukować przez port szeregowy, potrzebna jest dodatkowa biblioteka (android-serialport-api)
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
    }
}

