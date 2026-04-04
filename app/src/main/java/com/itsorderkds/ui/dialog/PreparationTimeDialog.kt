package com.itsorderkds.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.itsorderkds.R
import com.itsorderkds.util.DateTimeUtils
import java.time.*
import java.time.format.DateTimeFormatter
import timber.log.Timber
import java.util.Locale
import android.app.TimePickerDialog
import android.widget.TimePicker
import androidx.compose.ui.platform.LocalContext

/**
 * Dialog do wyboru czasu przygotowania zamówienia przed wysłaniem do zewnętrznego kuriera
 * Umożliwia wybranie predefiniowanych czasów lub ręczne ustawienie czasu dostawy
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreparationTimeDialog(
    courierName: String,
    onDismiss: () -> Unit,
    onConfirm: (timeInMinutes: Int, timeDelivery: String?) -> Unit,
    orderId: String? = null,
    isAsap: Boolean = true,
    deliveryTimeIso: String? = null // nowy parametr - ISO string z serwera jeśli nie ASAP
) {
    // Key state by orderId and deliveryTimeIso to avoid leaking selection between different orders
    // Initialize synchronously from deliveryTimeIso to avoid default 12:00 flash when opening dialog quickly
    val initialSelected = remember(orderId, deliveryTimeIso) {
        runCatching {
            if (deliveryTimeIso.isNullOrBlank()) 15
            else {
                val target = ZonedDateTime.parse(deliveryTimeIso).withZoneSameInstant(ZoneId.systemDefault())
                val now = ZonedDateTime.now(ZoneId.systemDefault())
                val minutesUntil = Duration.between(now, target).toMinutes().coerceAtLeast(0)
                val nearest = listOf(15, 30, 60, 90).minByOrNull { kotlin.math.abs(it - minutesUntil.toInt()) } ?: 15
                nearest
            }
        }.getOrDefault(15)
    }

    // Key state by orderId and deliveryTimeIso to avoid leaking selection between different orders
    var selectedTime by remember(orderId, deliveryTimeIso) { mutableStateOf(initialSelected) }
    var showCustomTimeDialog by remember(orderId, deliveryTimeIso) { mutableStateOf(false) }
    // przechowujemy ISO string (UTC Z) jeśli użytkownik ustawił ręczny czas lub jeśli order ma deliveryTime
    var customDeliveryTimeIso by remember(orderId, deliveryTimeIso) { mutableStateOf(deliveryTimeIso) }

    // Zaktualizowane opcje (zgodnie z prośbą): 15,30,60,90
    val timeOptions = listOf(15, 30, 60, 90)

    // Funkcja pomocnicza do formatowania ISO -> czytelny lokalny tekst
    fun formatIsoToLocalDisplay(iso: String?): String? {
        if (iso.isNullOrBlank()) return null
        return try {
            val z = ZonedDateTime.parse(iso)
            val local = z.withZoneSameInstant(ZoneId.systemDefault())
            local.format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault()))
        } catch (_: Exception) {
            // fallback - zwróć surowy string
            iso
        }
    }

    // Reset state / inicjalizacja when orderId or deliveryTimeIso changes
    LaunchedEffect(orderId, deliveryTimeIso, isAsap) {
        // only reset transient UI flags; keep customDeliveryTimeIso/selectedTime which were initialized synchronously
        showCustomTimeDialog = false
        if (!deliveryTimeIso.isNullOrBlank()) {
            customDeliveryTimeIso = deliveryTimeIso
            // also update selectedTime to current target if deliveryTimeIso changed
            try {
                val target = ZonedDateTime.parse(deliveryTimeIso).withZoneSameInstant(ZoneId.systemDefault())
                val now = ZonedDateTime.now(ZoneId.systemDefault())
                val minutesUntil = Duration.between(now, target).toMinutes().coerceAtLeast(0)
                val nearest = timeOptions.minByOrNull { kotlin.math.abs(it - minutesUntil.toInt()) } ?: selectedTime
                selectedTime = nearest
            } catch (_: Exception) {
                // ignore parsing error
            }
        }
    }

    if (showCustomTimeDialog) {
        CustomDeliveryTimeDialog(
            onDismiss = { showCustomTimeDialog = false },
            onConfirm = { timeString ->
                customDeliveryTimeIso = timeString
                showCustomTimeDialog = false
            },
            // pass deliveryTimeIso first if available so dialog initializes immediately with server value
            initialTimeIso = deliveryTimeIso ?: customDeliveryTimeIso
        )
    }

    val formattedCustomDeliveryTime = remember(customDeliveryTimeIso) { formatIsoToLocalDisplay(customDeliveryTimeIso) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.preparation_time_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.preparation_time_message, courierName),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Predefiniowane opcje czasu (statyczna siatka 2 kolumny, nie-scrollująca)
                Text(
                    text = stringResource(R.string.predefined_times_label),
                    style = MaterialTheme.typography.labelLarge
                )

                // Zbuduj wiersze po 2 elementy
                val rows = timeOptions.chunked(2)
                Column(modifier = Modifier.fillMaxWidth()) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { time ->
                                Button(
                                    onClick = {
                                        selectedTime = time
                                        customDeliveryTimeIso = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text(
                                        text = stringResource(R.string.preparation_time_min, time),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.preparation_time_ready, selectedTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Opcja ręcznego ustawienia czasu dostawy
                Button(
                    onClick = { showCustomTimeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp)
                    )
                    Text(stringResource(R.string.set_delivery_time))
                }

                // Wyświetl wybrany czas dostawy (sformatowany)
                if (formattedCustomDeliveryTime != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Text(
                            text = "${stringResource(R.string.delivery_time)}: $formattedCustomDeliveryTime",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedTime, customDeliveryTimeIso) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    stringResource(R.string.btn_send),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    stringResource(R.string.btn_cancel),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        modifier = Modifier.fillMaxWidth(0.9f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDeliveryTimeDialog(
    onDismiss: () -> Unit,
    onConfirm: (timeString: String) -> Unit,
    initialTimeIso: String? = null
) {
    // Initialize state synchronously from initialTimeIso (if present) to avoid flashing default values
    val initialZoned = remember(initialTimeIso) {
        // try main parser first
        val p1 = DateTimeUtils.parseToZonedDateTime(initialTimeIso)
        if (p1 != null) return@remember p1

        if (initialTimeIso.isNullOrBlank()) return@remember null
        val iso = initialTimeIso.trim()

        // Try a few normalizations synchronously
        val attempts = listOf(
            { runCatching { java.time.OffsetDateTime.parse(iso).toZonedDateTime() }.getOrNull() },
            { runCatching { java.time.Instant.parse(iso).let { ZonedDateTime.ofInstant(it, ZoneId.systemDefault()) } }.getOrNull() },
            { runCatching { val norm = if (iso.endsWith("+00:00")) iso.replace("+00:00", "Z") else iso; ZonedDateTime.parse(norm).withZoneSameInstant(ZoneId.systemDefault()) }.getOrNull() },
            { runCatching { val noMillis = iso.replace(Regex("\\.\\d{3}"), ""); java.time.OffsetDateTime.parse(noMillis).toZonedDateTime() }.getOrNull() }
        )

        for (a in attempts) {
            val r = a()
            if (r != null) return@remember r
        }
        null
    }

    val initialDate = initialZoned?.toLocalDate() ?: LocalDate.now()
    val initialHour = initialZoned?.hour?.toFloat() ?: 12f
    val initialMinute = initialZoned?.minute?.toFloat() ?: 0f

    var selectedDateState = remember(initialTimeIso) { mutableStateOf(initialDate) }
    var selectedHourState = remember(initialTimeIso) { mutableStateOf(initialHour) }
    var selectedMinuteState = remember(initialTimeIso) { mutableStateOf(initialMinute) }

    // używamy bezpośrednio .value z state'ów zamiast tworzyć dodatkowe delegated properties
    // jeśli mamy initialTimeIso chcemy od razu pokazać widok wyboru godziny (aby nie zobaczyć 12:00)
    var showDatePicker by remember(initialTimeIso) { mutableStateOf(initialTimeIso.isNullOrBlank()) }

    val context = LocalContext.current
    // prepare a TimePickerDialog that updates selectedHour/Minute when user picks
    val timePickerDialog = remember(initialTimeIso, context) {
        TimePickerDialog(
            context,
            { _: TimePicker, hourOfDay: Int, minute: Int ->
                selectedHourState.value = hourOfDay.toFloat()
                selectedMinuteState.value = minute.toFloat()
            },
            initialHour.toInt(),
            initialMinute.toInt(),
            true
        )
    }

    // show the native time picker immediately if we have an initial ISO time and are in time selection
    var timePickerShown by remember { mutableStateOf(false) }
    LaunchedEffect(initialTimeIso, showDatePicker) {
        if (!showDatePicker && !initialTimeIso.isNullOrBlank() && !timePickerShown) {
            try {
                timePickerDialog.show()
                timePickerShown = true
            } catch (e: Exception) {
                Timber.w(e, "Failed to show TimePickerDialog immediately")
            }
        }
    }

    // Debug log to help diagnose why initialTimeIso might not parse on user's device
    LaunchedEffect(initialTimeIso) {
        Timber.d("CustomDeliveryTimeDialog: initialTimeIso=%s, parsed=%s", initialTimeIso, initialZoned)
        // If parse failed, try once more with normalization and update state variables synchronously
        if (initialTimeIso != null && initialZoned == null) {
            val retry = DateTimeUtils.parseToZonedDateTime(initialTimeIso)
            if (retry != null) {
                Timber.d("CustomDeliveryTimeDialog: retry parse succeeded=%s", retry)
            } else {
                Timber.d("CustomDeliveryTimeDialog: retry parse failed for %s", initialTimeIso)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delivery_time)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                if (showDatePicker) {
                    // Wybór daty
                    Text(
                        text = stringResource(R.string.select_date),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = selectedDateState.value.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { selectedDateState.value = selectedDateState.value.minusDays(1) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("← Poprzedni")
                        }
                        Button(
                            onClick = { selectedDateState.value = LocalDate.now() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.today))
                        }
                        Button(
                            onClick = { selectedDateState.value = selectedDateState.value.plusDays(1) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Następny →")
                        }
                    }

                    Button(
                        onClick = { showDatePicker = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            stringResource(R.string.select_time),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    // Wybór godziny i minut
                    Text(
                        text = stringResource(R.string.select_time),
                        style = MaterialTheme.typography.labelLarge
                    )

                    // Przycisk otwierający TimePicker
                    OutlinedButton(
                        onClick = { timePickerDialog.show() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", selectedHourState.value.toInt(), selectedMinuteState.value.toInt()),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    // Podsumowanie
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "${stringResource(R.string.date)}: ${selectedDateState.value.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                "${stringResource(R.string.hour)}: ${String.format(Locale.getDefault(), "%02d:%02d", selectedHourState.value.toInt(), selectedMinuteState.value.toInt())}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("← ${stringResource(R.string.back_date_selection)}")
                    }
                }
            }
        },
        confirmButton = {
            if (!showDatePicker) {
                Button(
                    onClick = {
                        // Konwertuj LocalDateTime na ZonedDateTime w UTC i sformatuj na ISO 8601
                        val selectedTime = LocalTime.of(selectedHourState.value.toInt(), selectedMinuteState.value.toInt())
                        val zonedDateTime = ZonedDateTime.of(selectedDateState.value, selectedTime, ZoneId.systemDefault())
                            .withZoneSameInstant(ZoneOffset.UTC)
                        val timeString = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
                        onConfirm(timeString)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.btn_send))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
