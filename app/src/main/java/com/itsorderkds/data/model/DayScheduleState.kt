package  com.itsorderkds.data.model

import java.time.LocalTime

/**
 * DayScheduleState - stan jednego dnia w harmonogramie
 */
data class DayScheduleState(
    val dayOfWeek: Int,           // 0 = poniedziałek, 6 = niedziela
    val dayLabel: String,          // "Poniedziałek"
    val isClosed: Boolean = false, // Czy dzień zamknięty
    val openHour: Int = 9,         // Godzina otwarcia
    val openMinute: Int = 0,       // Minuta otwarcia
    val closeHour: Int = 17,       // Godzina zamknięcia
    val closeMinute: Int = 0       // Minuta zamknięcia
) {
    fun getOpenTimeString(): String = "%02d:%02d".format(openHour, openMinute)
    fun getCloseTimeString(): String = "%02d:%02d".format(closeHour, closeMinute)
    fun getSummary(): String = when {
        isClosed -> "Zamknięte"
        else -> "${getOpenTimeString()}–${getCloseTimeString()}"
    }
}

/**
 * TimePickerEvent - event po wyborze czasu
 */
sealed class TimePickerEvent {
    data class OnTimeSelected(val hour: Int, val minute: Int) : TimePickerEvent()
    object OnDismiss : TimePickerEvent()
}

