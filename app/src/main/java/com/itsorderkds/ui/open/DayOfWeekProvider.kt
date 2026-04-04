package com.itsorderkds.ui.open

import android.content.Context
import com.itsorderkds.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class do dostarczania przetłumaczonych nazw dni tygodnia
 */
@Singleton
class DayOfWeekProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getDayLabels(): List<String> {
        return listOf(
            context.getString(R.string.day_monday),
            context.getString(R.string.day_tuesday),
            context.getString(R.string.day_wednesday),
            context.getString(R.string.day_thursday),
            context.getString(R.string.day_friday),
            context.getString(R.string.day_saturday),
            context.getString(R.string.day_sunday)
        )
    }

    fun getErrorSaveFailed(message: String?): String {
        return context.getString(R.string.error_save_failed, message ?: "")
    }

    fun getErrorGeneric(message: String?): String {
        return context.getString(R.string.error_generic, message ?: "")
    }
}

