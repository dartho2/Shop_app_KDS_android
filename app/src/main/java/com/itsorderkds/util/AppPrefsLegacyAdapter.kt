package com.itsorderkds.util

import android.content.Context
import com.itsorderkds.data.preferences.AppPreferencesManager
import com.itsorderkds.data.responses.UserRole
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * DEPRECATED: Legacy AppPrefs adapter.
 *
 * Ta klasa zapewnia kompatybilność wsteczną ze starym kodem używającym AppPrefs.
 * Wszystkie operacje są przekierowywane do AppPreferencesManager.
 *
 * ⚠️ UWAGA: Metody synchroniczne używają runBlocking - może być wolne!
 *
 * Migracja:
 * ```kotlin
 * // STARY KOD:
 * AppPrefs.getCurrency()
 *
 * // NOWY KOD (w ViewModel/Repository):
 * @Inject lateinit var preferencesManager: AppPreferencesManager
 * viewModelScope.launch {
 *     val currency = preferencesManager.getCurrency()
 * }
 * ```
 *
 * @deprecated Używaj AppPreferencesManager z Hilt injection
 */
@Deprecated(
    message = "Use AppPreferencesManager with dependency injection instead",
    replaceWith = ReplaceWith("AppPreferencesManager", "com.itsorderkds.data.preferences.AppPreferencesManager")
)
object AppPrefsLegacyAdapter {

    private lateinit var preferencesManager: AppPreferencesManager

    /**
     * Inicjalizacja adaptera. Wywoływane w Application.onCreate()
     */
    fun init(context: Context, manager: AppPreferencesManager) {
        this.preferencesManager = manager
    }

    // ═══════════════════════════════════════════════════════════════════════
    // User Data
    // ═══════════════════════════════════════════════════════════════════════

    fun getUserID(): String = runBlocking {
        preferencesManager.getUserId() ?: ""
    }

    fun setUserId(id: String) = runBlocking {
        preferencesManager.setUserId(id)
    }

    fun getUserRole(): String = runBlocking {
        preferencesManager.getUserRole()?.name ?: "User"
    }

    fun setUserRole(role: String) = runBlocking {
        try {
            preferencesManager.setUserRole(UserRole.valueOf(role))
        } catch (e: IllegalArgumentException) {
            // Ignore invalid role
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Server Configuration
    // ═══════════════════════════════════════════════════════════════════════

    fun getBaseUrl(): String = runBlocking {
        preferencesManager.getBaseUrl()
    }

    fun setBaseUrl(url: String) = runBlocking {
        preferencesManager.setBaseUrl(url)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Currency
    // ═══════════════════════════════════════════════════════════════════════

    fun getCurrency(): String = runBlocking {
        preferencesManager.getCurrency()
    }

    fun setCurrency(currency: String) = runBlocking {
        preferencesManager.setCurrency(currency)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FCM Token
    // ═══════════════════════════════════════════════════════════════════════

    fun getFCMToken(): String = runBlocking {
        preferencesManager.getFcmToken() ?: ""
    }

    fun setFCMToken(token: String) = runBlocking {
        preferencesManager.setFcmToken(token)
    }

    fun setFCMTokenChat(token: String) = setFCMToken(token)

    // ═══════════════════════════════════════════════════════════════════════
    // Printer Settings
    // ═══════════════════════════════════════════════════════════════════════

    fun savePrinter(type: String, id: String) = runBlocking {
        preferencesManager.savePrinter(type, id)
    }

    fun getPrinter(context: Context): Pair<String?, String?> = runBlocking {
        preferencesManager.getPrinter()
    }

    fun getAutoPrintEnabled(): Boolean = runBlocking {
        preferencesManager.getAutoPrintEnabled()
    }

    fun setAutoPrintEnabled(enabled: Boolean) = runBlocking {
        preferencesManager.setAutoPrintEnabled(enabled)
    }

    fun getAutoPrintAcceptedEnabled(): Boolean = runBlocking {
        preferencesManager.getAutoPrintAcceptedEnabled()
    }

    fun setAutoPrintAcceptedEnabled(enabled: Boolean) = runBlocking {
        preferencesManager.setAutoPrintAcceptedEnabled(enabled)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Legacy Methods (nieużywane ale dla kompatybilności)
    // ═══════════════════════════════════════════════════════════════════════

    fun getShopName(context: Context): String? = runBlocking {
        preferencesManager.getShopName()
    }

    fun setShopName(context: Context, name: String) = runBlocking {
        preferencesManager.setShopName(name)
    }

    fun getShopCurrencySymbol(context: Context): String? = runBlocking {
        preferencesManager.getCurrencySymbol()
    }

    fun setShopCurrencySymbol(context: Context, symbol: String) = runBlocking {
        preferencesManager.setCurrencySymbol(symbol)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Unused Methods (dla kompatybilności, nie robią nic)
    // ═══════════════════════════════════════════════════════════════════════

    @Deprecated("Not used anymore")
    fun saveAuthToken(context: Context, token: String) {
        // Tokeny są zarządzane przez AppPreferencesManager.saveAuthTokens()
    }

    @Deprecated("Not used anymore")
    fun getAuthToken(context: Context): String? = null

    @Deprecated("Not used anymore")
    fun setLoginUrl(context: Context, url: String) {}

    @Deprecated("Not used anymore")
    fun getLoginUrl(context: Context): String? = null

    @Deprecated("Not used anymore")
    fun setTokenChat(token: String) {}

    @Deprecated("Not used anymore")
    fun getTokenChat(): String = ""

    @Deprecated("Not used anymore")
    fun setIdChat(id: String) {}

    @Deprecated("Not used anymore")
    fun getIdChat(): String = ""

    @Deprecated("Not used anymore")
    fun setLoginKey(context: Context, key: String) {}

    @Deprecated("Not used anymore")
    fun getLoginKey(context: Context): String? = null

    @Deprecated("Not used anymore")
    fun setLoginSecurity(context: Context, security: String) {}

    @Deprecated("Not used anymore")
    fun getLoginSecurity(context: Context): String? = null

    @Deprecated("Not used anymore")
    fun setLoginStavaToken(context: Context, token: String) {}

    @Deprecated("Not used anymore")
    fun getLoginStavaToken(context: Context): String? = null

    @Deprecated("Not used anymore")
    fun setLoginStavaUrl(context: Context, url: String) {}

    @Deprecated("Not used anymore")
    fun getLoginStavaUrl(context: Context): String? = null

    @Deprecated("Not used anymore")
    fun clear(context: Context) = runBlocking {
        preferencesManager.clearAll()
    }

    @Deprecated("Not needed - AppPreferencesManager is injected by Hilt")
    fun isInitialized(): Boolean = ::preferencesManager.isInitialized
}

