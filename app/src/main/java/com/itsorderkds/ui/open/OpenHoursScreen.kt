package com.itsorderkds.ui.open

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.itsorderkds.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenHoursScreen(
    vm: OpenHoursViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    if (ui.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Stan rozwinięć dni tygodnia
    var expandedDays by remember { mutableStateOf(setOf<Int>()) }

    // Stan edytora czasu
    var timeEditor by remember { mutableStateOf<TimeEditorState?>(null) }

    Scaffold(
        bottomBar = {
            Button(
                onClick = vm::saveSchedules,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(stringResource(R.string.common_save))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
        // --- 1) STATUS SKLEPU ---
        item {
            SectionTitle(stringResource(R.string.store_status_title))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.accept_orders_by_schedule)) },
                    supportingContent = {
                        Text(
                            if (ui.isOpen) stringResource(R.string.store_can_accept_by_schedule)
                            else stringResource(R.string.store_paused_by_global_switch)
                        )
                    },
                    trailingContent = {
                        Switch(checked = ui.isOpen, onCheckedChange = vm::toggleIsOpen)
                    }
                )
            }
        }

        // --- 2) STREFA CZASOWA ---
        item {
            SectionTitle(stringResource(R.string.configuration))
            OutlinedTextField(
                value = ui.timezone,
                onValueChange = vm::setTimezone,
                label = { Text(stringResource(R.string.timezone_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // --- 3) HARMONOGRAM TYGODNIOWY (ROZWIJALNE KARTY DNI) ---
        item { SectionTitle(stringResource(R.string.weekly_schedule)) }

        itemsIndexed(ui.days) { dayIdx, day ->
            OpenHoursDayCard(
                day = day,
                isExpanded = dayIdx in expandedDays,
                onToggleExpand = {
                    expandedDays = if (dayIdx in expandedDays) {
                        expandedDays - dayIdx
                    } else {
                        expandedDays + dayIdx
                    }
                },
                onOpenTimeClick = { isStart ->
                    timeEditor = TimeEditorState(
                        dayIndex = dayIdx,
                        isStartTime = isStart,
                        currentHour = if (isStart) day.openHour else day.closeHour,
                        currentMinute = if (isStart) day.openMinute else day.closeMinute
                    )
                },
                onClosedToggle = { isClosed ->
                    vm.updateDayClosed(dayIdx, isClosed)
                }
            )
        }

        // --- 4) WYJĄTKI (DATY WYJĄTKOWE) ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle(stringResource(R.string.exceptions_dates))
                TextButton(onClick = vm::addException) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.common_add))
                }
            }
        }

        itemsIndexed(ui.exceptions) { exIdx, exception ->
            ExceptionCard(
                exception = exception,
                onDateChange = { newDate ->
                    vm.updateException(exIdx, date = newDate)
                },
                onClosedChange = { isClosed ->
                    vm.updateException(exIdx, closed = isClosed)
                },
                onAddTimeRange = {
                    vm.addExceptionTimeRange(exIdx)
                },
                onTimeRangeChange = { rangeIdx, startH, startM, endH, endM ->
                    vm.updateExceptionTimeRange(exIdx, rangeIdx, startH, startM, endH, endM)
                },
                onRemoveTimeRange = { rangeIdx ->
                    vm.removeExceptionTimeRange(exIdx, rangeIdx)
                },
                onRemoveException = {
                    vm.removeException(exIdx)
                }
            )
        }

        // --- STATUS I SPACER ---
        if (ui.error != null) {
            item {
                Text(
                    ui.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (ui.savedOk) {
            item {
                Text(
                    stringResource(R.string.common_saved),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // Dialog wyboru czasu
    timeEditor?.let { editor ->
        TimePickerDialog(
            initialHour = editor.currentHour,
            initialMinute = editor.currentMinute,
            onDismiss = { timeEditor = null },
            onConfirm = { hour, minute ->
                vm.updateDayTime(
                    dayIndex = editor.dayIndex,
                    isStartTime = editor.isStartTime,
                    hour = hour,
                    minute = minute
                )
                timeEditor = null
            }
        )
    }
}

/* ----------------------------- UI Components ----------------------------- */

@Composable
private fun OpenHoursDayCard(
    day: DayWithRanges,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenTimeClick: (isStart: Boolean) -> Unit,
    onClosedToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onToggleExpand() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Nagłówek: nazwa dnia + chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            // Podsumowanie: godziny lub "Zamknięte"
            val summary = if (day.isClosed) {
                stringResource(R.string.closed)
            } else {
                "${formatTime(day.openHour, day.openMinute)} – ${formatTime(day.closeHour, day.closeMinute)}"
            }

            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Rozwijane szczegóły
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                    // Przełącznik "Zamknięte"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.closed),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = day.isClosed,
                            onCheckedChange = onClosedToggle
                        )
                    }

                    if (!day.isClosed) {
                        Spacer(Modifier.height(12.dp))

                        // Godzina od
                        TimeInputRow(
                            label = stringResource(R.string.open_time_from),
                            hour = day.openHour,
                            minute = day.openMinute,
                            onClick = { onOpenTimeClick(true) }
                        )

                        Spacer(Modifier.height(8.dp))

                        // Godzina do
                        TimeInputRow(
                            label = stringResource(R.string.open_time_to),
                            hour = day.closeHour,
                            minute = day.closeMinute,
                            onClick = { onOpenTimeClick(false) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeInputRow(
    label: String,
    hour: Int,
    minute: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.width(120.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 4.dp)
            )
            Text(formatTime(hour, minute))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_time)) },
        text = {
            // TimePicker w trybie input (digital) zamiast analogowego
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(timePickerState.hour, timePickerState.minute)
            }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

/* ----------------------------- Data & Utils ----------------------------- */

data class TimeEditorState(
    val dayIndex: Int,
    val isStartTime: Boolean,
    val currentHour: Int,
    val currentMinute: Int
)

private fun formatTime(hour: Int, minute: Int): String {
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun ExceptionCard(
    exception: DateException,
    onDateChange: (String) -> Unit,
    onClosedChange: (Boolean) -> Unit,
    onAddTimeRange: () -> Unit,
    onTimeRangeChange: (Int, Int, Int, Int, Int) -> Unit,
    onRemoveTimeRange: (Int) -> Unit,
    onRemoveException: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = exception.date,
                onValueChange = onDateChange,
                label = { Text(stringResource(R.string.date_yyyy_mm_dd)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.closed), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = exception.closed, onCheckedChange = onClosedChange)
            }

            if (!exception.closed && exception.timeRanges.isNotEmpty()) {
                HorizontalDivider()
                exception.timeRanges.forEachIndexed { idx, range ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { /* TODO: Time picker */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(formatTime(range.startHour, range.startMinute))
                        }
                        Text("–")
                        FilledTonalButton(
                            onClick = { /* TODO: Time picker */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(formatTime(range.endHour, range.endMinute))
                        }
                        IconButton(onClick = { onRemoveTimeRange(idx) }) {
                            Icon(Icons.Default.Delete, null)
                        }
                    }
                }
            }

            if (!exception.closed) {
                OutlinedButton(onClick = onAddTimeRange, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AccessTime, null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.add_time_range))
                }
            }

            HorizontalDivider()
            TextButton(
                onClick = onRemoveException,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.remove_exception))
            }
        }
    }
}


