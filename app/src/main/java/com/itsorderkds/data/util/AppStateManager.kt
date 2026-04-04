package com.itsorderkds.data.util

object AppStateManager {
    @Volatile
    var isAppInForeground: Boolean = false
}