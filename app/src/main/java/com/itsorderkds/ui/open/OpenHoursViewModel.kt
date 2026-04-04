package com.itsorderkds.ui.open

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsorderkds.data.model.UpdateOpenHoursRequest
import com.itsorderkds.data.network.Resource
import com.itsorderkds.ui.theme.status.OpenHoursRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.*

/**
 * OpenHoursViewModel - zarządza stanem ekranu godzin otwarcia
 */
@HiltViewModel
class OpenHoursViewModel @Inject constructor(
    private val repository: OpenHoursRepository,
    private val dayOfWeekProvider: DayOfWeekProvider
) : ViewModel() {

    private val _ui = MutableStateFlow(OpenHoursUiState())
    val ui: StateFlow<OpenHoursUiState> = _ui.asStateFlow()

    init {
        loadSchedules()
    }

    private fun loadSchedules() {
        viewModelScope.launch {
            when (val result = repository.getOpenHoursAdmin()) {
                is Resource.Success -> {
                    val data = result.value

                    val dayLabels = dayOfWeekProvider.getDayLabels()

                    // Mapuj weekly schedule z API
                    val initialDays = dayLabels.mapIndexed { idx, label ->
                        val dayKey = when (idx) {
                            0 -> "mon"
                            1 -> "tue"
                            2 -> "wed"
                            3 -> "thu"
                            4 -> "fri"
                            5 -> "sat"
                            6 -> "sun"
                            else -> ""
                        }

                        val ranges = data.weekly[dayKey]
                        if (ranges != null && ranges.isNotEmpty()) {
                            val range = ranges[0]
                            val (openH, openM) = parseTime(range.start)
                            val (closeH, closeM) = parseTime(range.end)
                            DayWithRanges(
                                dayOfWeek = idx,
                                label = label,
                                isClosed = false,
                                openHour = openH,
                                openMinute = openM,
                                closeHour = closeH,
                                closeMinute = closeM
                            )
                        } else {
                            // Dzień zamknięty
                            DayWithRanges(
                                dayOfWeek = idx,
                                label = label,
                                isClosed = true,
                                openHour = 9,
                                openMinute = 0,
                                closeHour = 17,
                                closeMinute = 0
                            )
                        }
                    }

                    // Mapuj exceptions na DateException
                    val exceptions = data.exceptions.map { exc ->
                        val timeRanges = exc.ranges?.mapNotNull { range ->
                            val (startH, startM) = parseTime(range.start)
                            val (endH, endM) = parseTime(range.end)
                            TimeRange(startH, startM, endH, endM)
                        } ?: emptyList()

                        DateException(
                            date = exc.date,
                            closed = exc.closed ?: false,
                            timeRanges = timeRanges
                        )
                    }

                    _ui.value = _ui.value.copy(
                        days = initialDays,
                        timezone = data.timezone,
                        isOpen = data.isOpen,
                        exceptions = exceptions,
                        isLoading = false
                    )
                }
                is Resource.Failure -> {
                    val dayLabels = dayOfWeekProvider.getDayLabels()
                    val initialDays = dayLabels.mapIndexed { idx, label ->
                        DayWithRanges(
                            dayOfWeek = idx,
                            label = label,
                            isClosed = false,
                            openHour = 9,
                            openMinute = 0,
                            closeHour = 17,
                            closeMinute = 0
                        )
                    }

                    val defaultTimezone = TimeZone.getDefault().id
                    _ui.value = _ui.value.copy(
                        days = initialDays,
                        timezone = defaultTimezone,
                        isOpen = true,
                        isLoading = false,
                        error = result.errorMessage
                    )
                }
                is Resource.Loading -> {
                    _ui.value = _ui.value.copy(isLoading = true)
                }
            }
        }
    }

    /**
     * Helper: Konwertuj czas z formatu "HH:mm" na parę (hour, minute)
     */
    private fun parseTime(timeStr: String): Pair<Int, Int> {
        return try {
            val parts = timeStr.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            Pair(hour, minute)
        } catch (e: Exception) {
            Pair(9, 0)
        }
    }

    // --- Zarządzanie statusem dnia ---

    fun updateDayClosed(dayIndex: Int, isClosed: Boolean) {
        val updated = _ui.value.days.toMutableList()
        updated[dayIndex] = updated[dayIndex].copy(isClosed = isClosed)
        _ui.value = _ui.value.copy(days = updated)
    }

    fun updateDayTime(dayIndex: Int, isStartTime: Boolean, hour: Int, minute: Int) {
        val updated = _ui.value.days.toMutableList()
        updated[dayIndex] = if (isStartTime) {
            updated[dayIndex].copy(openHour = hour, openMinute = minute)
        } else {
            updated[dayIndex].copy(closeHour = hour, closeMinute = minute)
        }
        _ui.value = _ui.value.copy(days = updated)
    }

    // --- Zarządzanie statusem sklepu ---

    fun toggleIsOpen(newValue: Boolean) {
        viewModelScope.launch {
            // Optimistic update UI
            _ui.value = _ui.value.copy(isOpen = newValue)

            // Wyślij request na API
            val request = UpdateOpenHoursRequest(isOpen = newValue)
            when (val result = repository.updateOpenHours(request)) {
                is Resource.Success -> {
                    // Aktualizuj UI z odpowiedzi API
                    _ui.value = _ui.value.copy(
                        isOpen = result.value.isOpen,
                        timezone = result.value.timezone,
                        savedOk = true
                    )
                    // Schowaj komunikat po 2 sekundach
                    kotlinx.coroutines.delay(2000)
                    _ui.value = _ui.value.copy(savedOk = false)
                }
                is Resource.Failure -> {
                    // Cofnij zmianę UI jeśli błąd
                    _ui.value = _ui.value.copy(
                        isOpen = !newValue,
                        error = result.errorMessage
                    )
                    // Schowaj komunikat błędu po 3 sekundach
                    kotlinx.coroutines.delay(3000)
                    _ui.value = _ui.value.copy(error = null)
                }
                is Resource.Loading -> {
                    // Nic nie rób
                }
            }
        }
    }

    fun setTimezone(tz: String) {
        _ui.value = _ui.value.copy(timezone = tz)
    }

    // --- Zarządzanie wyjątkami (datami wyjątkowymi) ---

    fun addException() {
        val currentDate = java.time.LocalDate.now().plusDays(1).toString() // Jutro
        val newException = DateException(
            date = currentDate,
            closed = false,
            timeRanges = listOf(TimeRange(9, 0, 17, 0))
        )
        val updated = _ui.value.exceptions.toMutableList()
        updated.add(newException)
        _ui.value = _ui.value.copy(exceptions = updated)
    }

    fun updateException(index: Int, date: String? = null, closed: Boolean? = null) {
        val updated = _ui.value.exceptions.toMutableList()
        if (index < updated.size) {
            updated[index] = updated[index].copy(
                date = date ?: updated[index].date,
                closed = closed ?: updated[index].closed
            )
            _ui.value = _ui.value.copy(exceptions = updated)
        }
    }

    fun addExceptionTimeRange(exceptionIndex: Int) {
        val updated = _ui.value.exceptions.toMutableList()
        if (exceptionIndex < updated.size) {
            val ranges = updated[exceptionIndex].timeRanges.toMutableList()
            ranges.add(TimeRange(9, 0, 17, 0))
            updated[exceptionIndex] = updated[exceptionIndex].copy(timeRanges = ranges)
            _ui.value = _ui.value.copy(exceptions = updated)
        }
    }

    fun updateExceptionTimeRange(exceptionIndex: Int, rangeIndex: Int, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        val updated = _ui.value.exceptions.toMutableList()
        if (exceptionIndex < updated.size) {
            val ranges = updated[exceptionIndex].timeRanges.toMutableList()
            if (rangeIndex < ranges.size) {
                ranges[rangeIndex] = TimeRange(startHour, startMinute, endHour, endMinute)
                updated[exceptionIndex] = updated[exceptionIndex].copy(timeRanges = ranges)
                _ui.value = _ui.value.copy(exceptions = updated)
            }
        }
    }

    fun removeExceptionTimeRange(exceptionIndex: Int, rangeIndex: Int) {
        val updated = _ui.value.exceptions.toMutableList()
        if (exceptionIndex < updated.size) {
            val ranges = updated[exceptionIndex].timeRanges.toMutableList()
            if (rangeIndex < ranges.size) {
                ranges.removeAt(rangeIndex)
                updated[exceptionIndex] = updated[exceptionIndex].copy(timeRanges = ranges)
                _ui.value = _ui.value.copy(exceptions = updated)
            }
        }
    }

    fun removeException(index: Int) {
        val updated = _ui.value.exceptions.toMutableList()
        if (index < updated.size) {
            updated.removeAt(index)
            _ui.value = _ui.value.copy(exceptions = updated)
        }
    }

    // --- Zapis ---

    fun saveSchedules() {
        viewModelScope.launch {
            try {
                // Konwertuj UI model na API format
                val weekly = mutableMapOf<String, List<com.itsorderkds.data.model.TimeRangeOpen>>()

                ui.value.days.forEach { day ->
                    val dayKey = when (day.dayOfWeek) {
                        0 -> "mon"
                        1 -> "tue"
                        2 -> "wed"
                        3 -> "thu"
                        4 -> "fri"
                        5 -> "sat"
                        6 -> "sun"
                        else -> return@forEach
                    }

                    if (!day.isClosed) {
                        weekly[dayKey] = listOf(
                            com.itsorderkds.data.model.TimeRangeOpen(
                                start = formatTimeForApi(day.openHour, day.openMinute),
                                end = formatTimeForApi(day.closeHour, day.closeMinute)
                            )
                        )
                    }
                }

                // Konwertuj exceptions
                val exceptions = ui.value.exceptions.map { exc ->
                    com.itsorderkds.data.model.ExceptionDay(
                        date = exc.date,
                        closed = exc.closed,
                        ranges = exc.timeRanges.map { range ->
                            com.itsorderkds.data.model.TimeRangeOpen(
                                start = formatTimeForApi(range.startHour, range.startMinute),
                                end = formatTimeForApi(range.endHour, range.endMinute)
                            )
                        }
                    )
                }

                val request = UpdateOpenHoursRequest(
                    timezone = ui.value.timezone,
                    isOpen = ui.value.isOpen,
                    weekly = weekly.mapValues { (_, ranges) ->
                        ranges.map { com.itsorderkds.data.model.TimeRangeOpen(it.start, it.end) }
                    },
                    exceptions = exceptions
                )

                when (val result = repository.updateOpenHours(request)) {
                    is Resource.Success -> {
                        _ui.value = _ui.value.copy(
                            savedOk = true,
                            error = null
                        )
                        // Schowaj komunikat po 2 sekundach
                        kotlinx.coroutines.delay(2000)
                        _ui.value = _ui.value.copy(savedOk = false)
                    }
                    is Resource.Failure -> {
                        _ui.value = _ui.value.copy(
                            error = dayOfWeekProvider.getErrorSaveFailed(result.errorMessage)
                        )
                        // Schowaj komunikat po 3 sekundach
                        kotlinx.coroutines.delay(3000)
                        _ui.value = _ui.value.copy(error = null)
                    }
                    is Resource.Loading -> {
                        // Nic nie rób
                    }
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    error = dayOfWeekProvider.getErrorGeneric(e.message)
                )
                kotlinx.coroutines.delay(3000)
                _ui.value = _ui.value.copy(error = null)
            }
        }
    }

    /**
     * Helper: Konwertuj godziny na format API "HH:mm"
     */
    private fun formatTimeForApi(hour: Int, minute: Int): String {
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }
}

