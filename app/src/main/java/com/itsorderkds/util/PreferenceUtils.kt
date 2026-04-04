package com.itsorderkds.util


import android.content.SharedPreferences
import android.util.Log

object PreferenceUtils {
    fun getInt(preferences: SharedPreferences, key: String, default: Int = 0): Int {
        return try {
            getString(preferences, key)?.let { value ->
                if (value.isEmpty()) {
                    default
                } else Integer.parseInt(value)
            } ?: default
        } catch (e: NumberFormatException) {
            default
        }
    }

    fun setInt(preferences: SharedPreferences, key: String, value: Int = 0) {
        setString(preferences, key, value.toString())
    }

    fun setFloat(preferences: SharedPreferences, key: String, value: Float) {
        setString(preferences, key, value.toString())
    }
    fun getLong(preferences: SharedPreferences, key: String, default: Long = 0L): Long {
        return preferences.getLong(key, default)
    }
    fun getFloat(preferences: SharedPreferences, key: String, default: Float = 0f): Float {
        return preferences.getFloat(key, default)
    }
    fun setLong(preferences: SharedPreferences, key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }

    fun getString(preferences: SharedPreferences, key: String, defaultValue: String? = ""): String? {
        return preferences.getString(key, defaultValue)
    }

    fun setString(preferences: SharedPreferences, key: String, value: String?) {
        val editor = preferences.edit()
        if (value.isNullOrEmpty()) {
            editor.remove(key)
        } else {
            editor.putString(key, value)
        }
        editor.apply()
    }

    fun getBoolean(preferences: SharedPreferences, key: String, default: Boolean = false): Boolean {
        return preferences.getBoolean(key, default)
    }

    fun setBoolean(preferences: SharedPreferences, key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }
}
