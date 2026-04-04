package com.itsorderkds.ui.open

/**
 * Stan UI dla ekranu godzin otwarcia
 */
data class OpenHoursUiState(
    val isLoading: Boolean = false,
    val days: List<DayWithRanges> = emptyList(),
    val timezone: String = "",  // Będzie ustawiony z systemowego timezone w ViewModel
    val isOpen: Boolean = true,
    val exceptions: List<DateException> = emptyList(),
    val error: String? = null,
    val savedOk: Boolean = false
)

/**
 * Reprezentuje dzień tygodnia z godzinami otwarcia
 */
data class DayWithRanges(
    val dayOfWeek: Int,           // 0-6 (Pon-Nd)
    val label: String,             // "Poniedziałek", "Wtorek", itd.
    val isClosed: Boolean = false, // Czy dzień jest wolny
    val openHour: Int = 9,         // Godzina otwarcia (0-23)
    val openMinute: Int = 0,       // Minuta otwarcia (0-59)
    val closeHour: Int = 17,       // Godzina zamknięcia (0-23)
    val closeMinute: Int = 0       // Minuta zamknięcia (0-59)
)

/**
 * Reprezentuje wyjątek dla konkretnej daty (np. dzień wolny, zmienione godziny)
 */
data class DateException(
    val date: String,              // Format: YYYY-MM-DD
    val closed: Boolean = false,   // Czy cały dzień zamknięty
    val timeRanges: List<TimeRange> = emptyList()  // Alternatywne godziny (jeśli nie closed)
)

/**
 * Reprezentuje przedział czasowy (od-do)
 */
data class TimeRange(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
)


