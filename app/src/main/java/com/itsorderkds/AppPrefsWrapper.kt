package com.itsorderkds

import com.itsorderkds.data.preferences.AppPreferencesManager
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper dla kompatybilności wstecznej.
 *
 * @deprecated Używaj bezpośrednio AppPreferencesManager przez Hilt injection
 */
@Deprecated("Use AppPreferencesManager directly")
@Singleton
class AppPrefsWrapper @Inject constructor(
    private val preferencesManager: AppPreferencesManager
) {
    fun setFCMToken(token: String) = runBlocking {
        preferencesManager.setFcmToken(token)
    }

    fun getFCMToken(): String? = runBlocking {
        preferencesManager.getFcmToken()
    }
}
