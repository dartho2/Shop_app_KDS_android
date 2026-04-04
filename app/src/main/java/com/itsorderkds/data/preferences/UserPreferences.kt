package com.itsorderkds.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

// Rozszerzenie kontekstu do uzyskania DataStore<Preferences>
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "courier_store")

class UserPreferences(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        // Generuje unikalny klucz na podstawie daty
        private fun shiftKey(date: String): Preferences.Key<Boolean> =
            booleanPreferencesKey("shift_assigned_$date")
    }

    /** Czyści wszystkie zapisane preferencje */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    /** Ustawia flagę, że kurier rozpoczął zmianę w danym dniu */
    suspend fun setShiftAssigned(date: String, assigned: Boolean) {
        val key = shiftKey(date)
        dataStore.edit { prefs ->
            prefs[key] = assigned
        }
    }

    /** Zwraca Flow<Boolean> mówiący, czy kurier rozpoczął zmianę danego dnia */
    fun isShiftAssignedFlow(date: String): Flow<Boolean> {
        val key = shiftKey(date)
        return dataStore.data.map { prefs ->
            prefs[key] ?: false
        }
    }

    /** Jednorazowo pobiera informację, czy kurier rozpoczął zmianę danego dnia */
    suspend fun isShiftAssigned(date: String): Boolean {
        return isShiftAssignedFlow(date).firstOrNull() ?: false
    }
}
