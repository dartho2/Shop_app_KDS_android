package com.itsorderkds.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.itsorderkds.data.responses.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralny menedżer preferencji aplikacji używający DataStore.
 * Zastępuje AppPrefs, DataStoreTokenProvider i inne rozproszone rozwiązania.
 *
 * @author Refactored - 2025-01-03
 */
@Singleton
class AppPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = "itsorderchat_preferences"
    )

    private val dataStore get() = context.dataStore

    // ═══════════════════════════════════════════════════════════════════════
    // Keys Definition
    // ═══════════════════════════════════════════════════════════════════════

    private object Keys {
        // Authentication
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val REFRESH_TOKEN_ID = stringPreferencesKey("refresh_token_id")
        val TENANT_KEY = stringPreferencesKey("tenant_key")

        // User Data
        val USER_ID = stringPreferencesKey("user_id")
        val USER_ROLE = stringPreferencesKey("user_role")

        // Server Configuration
        val BASE_URL = stringPreferencesKey("base_url")

        // App Settings
        val CURRENCY = stringPreferencesKey("currency")
        val FCM_TOKEN = stringPreferencesKey("fcm_token")
        val NOTIFICATION_SOUND_URI = stringPreferencesKey("notification_sound_uri")
        // per-type prefix
        fun notificationSoundKey(type: String) = stringPreferencesKey("notification_sound_uri_" + type)
        fun notificationMuteKey(type: String) = booleanPreferencesKey("notification_sound_mute_" + type)

        // Printer Settings
        val PRINTER_TYPE = stringPreferencesKey("printer_type") // "usb" | "bluetooth"
        val PRINTER_ID = stringPreferencesKey("printer_id")     // deviceName (USB) lub MAC (BT)
        val AUTO_PRINT = booleanPreferencesKey("auto_print")
        val AUTO_PRINT_ACCEPTED = booleanPreferencesKey("auto_print_accepted")
        val AUTO_PRINT_ACCEPTED_PRINTERS = stringPreferencesKey("auto_print_accepted_printers") // "main", "kitchen", "both"
        val AUTO_PRINT_DINE_IN = booleanPreferencesKey("auto_print_dine_in")
        val AUTO_PRINT_DINE_IN_PRINTERS = stringPreferencesKey("auto_print_dine_in_printers") // "main", "kitchen", "both"
        val AUTO_PRINT_KITCHEN = booleanPreferencesKey("auto_print_kitchen") // Czy drukować na kuchni po zaakceptowaniu (LEGACY - replaced by AUTO_PRINT_ACCEPTED_PRINTERS)

        // Printed Orders Tracking
        val PRINTED_ORDER_IDS = stringPreferencesKey("printed_order_ids") // Rozdzielone przecinkami

        // Terminal Settings (Kiosk Mode + Auto-restart + Task Reopen)
        val KIOSK_MODE_ENABLED = booleanPreferencesKey("kiosk_mode_enabled")
        val AUTO_RESTART_ENABLED = booleanPreferencesKey("auto_restart_enabled")
        val TASK_REOPEN_ENABLED = booleanPreferencesKey("task_reopen_enabled")

        // Legacy/Compatibility
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Authentication - Access Token (synchronous for interceptors)
    // ═══════════════════════════════════════════════════════════════════════

    @Volatile
    private var cachedAccessToken: String? = null

    /**
     * Pobiera access token synchronicznie (z cache).
     * Używane w HTTP interceptors.
     */
    fun getAccessToken(): String? = cachedAccessToken

    /**
     * Flow dla access token - reaguje na zmiany.
     */
    val accessTokenFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.ACCESS_TOKEN].also { cachedAccessToken = it }
    }

    /**
     * Zapisuje tokeny autoryzacji.
     */
    suspend fun saveAuthTokens(
        accessToken: String,
        refreshToken: String,
        refreshTokenId: String,
        tenantKey: String,
        role: UserRole?,
        userId: String?
    ) {
        require(accessToken.isNotBlank()) { "Access token nie może być pusty" }
        require(refreshToken.isNotBlank()) { "Refresh token nie może być pusty" }

        dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            prefs[Keys.REFRESH_TOKEN] = refreshToken
            prefs[Keys.REFRESH_TOKEN_ID] = refreshTokenId
            prefs[Keys.TENANT_KEY] = tenantKey

            // Bezpieczne ustawienie roli - tylko jeśli nie jest null
            if (role != null) {
                prefs[Keys.USER_ROLE] = role.name
            } else {
                prefs.remove(Keys.USER_ROLE)
            }

            // Bezpieczne ustawienie userId - tylko jeśli nie jest null
            if (userId != null && userId.isNotBlank()) {
                prefs[Keys.USER_ID] = userId
            } else {
                prefs.remove(Keys.USER_ID)
            }
        }
        cachedAccessToken = accessToken
    }

    /**
     * Czyści tokeny autoryzacji (logout).
     */
    suspend fun clearAuthTokens() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN_ID)
            prefs.remove(Keys.TENANT_KEY)
            prefs.remove(Keys.USER_ROLE)
            prefs.remove(Keys.USER_ID)
        }
        cachedAccessToken = null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Authentication - Other Tokens
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun getRefreshToken(): String? =
        dataStore.data.map { it[Keys.REFRESH_TOKEN] }.first()

    suspend fun getRefreshTokenId(): String? =
        dataStore.data.map { it[Keys.REFRESH_TOKEN_ID] }.first()

    suspend fun getTenantKey(): String? =
        dataStore.data.map { it[Keys.TENANT_KEY] }.first()

    // ═══════════════════════════════════════════════════════════════════════
    // User Data
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun getUserId(): String? =
        dataStore.data.map { it[Keys.USER_ID] }.first()

    suspend fun setUserId(userId: String) {
        dataStore.edit { it[Keys.USER_ID] = userId }
    }

    suspend fun getUserRole(): UserRole? =
        dataStore.data.map { prefs ->
            prefs[Keys.USER_ROLE]?.let { UserRole.valueOf(it) }
        }.first()

    suspend fun setUserRole(role: UserRole) {
        dataStore.edit { it[Keys.USER_ROLE] = role.name }
    }

    val userRoleFlow: Flow<UserRole?> = dataStore.data.map { prefs ->
        prefs[Keys.USER_ROLE]?.let { UserRole.valueOf(it) }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Server Configuration
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun getBaseUrl(): String =
        dataStore.data.map { it[Keys.BASE_URL] ?: "https://localhost:8001/" }.first()

    suspend fun setBaseUrl(url: String) {
        val trimmed = url.trim()
        require(trimmed.isNotEmpty()) { "Adres serwera jest pusty" }

        // Dodaj schemat jeśli brakuje
        val withScheme = when {
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }

        // Dodaj trailing slash
        val normalized = if (withScheme.endsWith("/")) withScheme else "$withScheme/"

        dataStore.edit { it[Keys.BASE_URL] = normalized }
    }

    val baseUrlFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.BASE_URL] ?: "https://localhost:8001/"
    }

    // ═══════════════════════════════════════════════════════════════════════
    // App Settings
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun getCurrency(): String =
        dataStore.data.map { it[Keys.CURRENCY] ?: "PLN" }.first()

    suspend fun setCurrency(currency: String) {
        dataStore.edit { it[Keys.CURRENCY] = currency }
    }

    val currencyFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.CURRENCY] ?: "PLN"
    }

    suspend fun getFcmToken(): String? =
        dataStore.data.map { it[Keys.FCM_TOKEN] }.first()

    suspend fun setFcmToken(token: String) {
        dataStore.edit { it[Keys.FCM_TOKEN] = token }
    }

    val notificationSoundUri: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATION_SOUND_URI]
    }.distinctUntilChanged()

    suspend fun getNotificationSoundUri(): String? =
        dataStore.data.map { it[Keys.NOTIFICATION_SOUND_URI] }.first()

    suspend fun setNotificationSoundUri(uri: String) {
        dataStore.edit { it[Keys.NOTIFICATION_SOUND_URI] = uri }
    }

    // Per-type notification sound
    suspend fun getNotificationSoundUri(type: String): String? =
        dataStore.data.map { it[Keys.notificationSoundKey(type)] }.first()

    suspend fun setNotificationSoundUri(type: String, uri: String) {
        dataStore.edit { it[Keys.notificationSoundKey(type)] = uri }
    }

    // Per-type notification mute
    suspend fun isNotificationSoundMuted(type: String): Boolean =
        dataStore.data.map { it[Keys.notificationMuteKey(type)] ?: false }.first()

    suspend fun setNotificationSoundMuted(type: String, muted: Boolean) {
        dataStore.edit { it[Keys.notificationMuteKey(type)] = muted }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Printer Settings
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun savePrinter(type: String, id: String) {
        dataStore.edit { prefs ->
            prefs[Keys.PRINTER_TYPE] = type
            prefs[Keys.PRINTER_ID] = id
        }
    }

    suspend fun getPrinter(): Pair<String?, String?> {
        return dataStore.data.map { prefs ->
            val type = prefs[Keys.PRINTER_TYPE]
            val id = prefs[Keys.PRINTER_ID]
            type to id
        }.first()
    }

    suspend fun getAutoPrintEnabled(): Boolean =
        dataStore.data.map { it[Keys.AUTO_PRINT] ?: false }.first()

    suspend fun setAutoPrintEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_PRINT] = enabled }
    }

    suspend fun getAutoPrintAcceptedEnabled(): Boolean =
        dataStore.data.map { it[Keys.AUTO_PRINT_ACCEPTED] ?: false }.first()

    suspend fun setAutoPrintAcceptedEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_PRINT_ACCEPTED] = enabled }
    }

    suspend fun getAutoPrintAcceptedPrinters(): String =
        dataStore.data.map { it[Keys.AUTO_PRINT_ACCEPTED_PRINTERS] ?: "both" }.first()

    suspend fun setAutoPrintAcceptedPrinters(printers: String) {
        dataStore.edit { it[Keys.AUTO_PRINT_ACCEPTED_PRINTERS] = printers }
    }

    suspend fun getAutoPrintDineInEnabled(): Boolean =
        dataStore.data.map { it[Keys.AUTO_PRINT_DINE_IN] ?: false }.first()

    suspend fun setAutoPrintDineInEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_PRINT_DINE_IN] = enabled }
    }

    suspend fun getAutoPrintDineInPrinters(): String =
        dataStore.data.map { it[Keys.AUTO_PRINT_DINE_IN_PRINTERS] ?: "both" }.first()

    suspend fun setAutoPrintDineInPrinters(printers: String) {
        dataStore.edit { it[Keys.AUTO_PRINT_DINE_IN_PRINTERS] = printers }
    }

    // LEGACY - zachowane dla kompatybilności wstecznej
    @Deprecated("Use getAutoPrintDineInPrinters() instead")
    suspend fun getAutoPrintDineInPrinter(): String =
        dataStore.data.map { it[Keys.AUTO_PRINT_DINE_IN_PRINTERS] ?: "main" }.first()

    @Deprecated("Use setAutoPrintDineInPrinters() instead")
    suspend fun setAutoPrintDineInPrinter(printer: String) {
        // Konwersja starego formatu na nowy
        val newFormat = when (printer) {
            "kitchen" -> "kitchen"
            else -> "main"
        }
        dataStore.edit { it[Keys.AUTO_PRINT_DINE_IN_PRINTERS] = newFormat }
    }

    suspend fun getAutoPrintKitchenEnabled(): Boolean =
        dataStore.data.map { it[Keys.AUTO_PRINT_KITCHEN] ?: true }.first() // domyślnie true

    suspend fun setAutoPrintKitchenEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_PRINT_KITCHEN] = enabled }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Printed Orders Tracking (zapobieganie duplikacji przy restarcie)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Pobiera Set ID zamówień które już zostały wydrukowane
     */
    suspend fun getPrintedOrderIds(): Set<String> {
        val stored = dataStore.data.map { it[Keys.PRINTED_ORDER_IDS] }.first()
        return if (stored.isNullOrBlank()) {
            emptySet()
        } else {
            stored.split(",").filter { it.isNotBlank() }.toSet()
        }
    }

    /**
     * Zapisuje Set ID zamówień które zostały wydrukowane
     */
    suspend fun savePrintedOrderIds(orderIds: Set<String>) {
        val joined = orderIds.joinToString(",")
        dataStore.edit { it[Keys.PRINTED_ORDER_IDS] = joined }
    }

    /**
     * Dodaje ID zamówienia do listy wydrukowanych
     */
    suspend fun addPrintedOrderId(orderId: String) {
        val current = getPrintedOrderIds().toMutableSet()
        current.add(orderId)

        // Ogranicz rozmiar do ostatnich 1000 zamówień (żeby nie rosło w nieskończoność)
        val limited = if (current.size > 1000) {
            current.toList().takeLast(1000).toSet()
        } else {
            current
        }

        savePrintedOrderIds(limited)
    }

    /**
     * Sprawdza czy zamówienie było już wydrukowane
     */
    suspend fun wasPrinted(orderId: String): Boolean {
        return getPrintedOrderIds().contains(orderId)
    }

    /**
     * Czyści listę wydrukowanych zamówień (opcjonalnie, dla maintenance)
     */
    suspend fun clearPrintedOrderIds() {
        dataStore.edit { it.remove(Keys.PRINTED_ORDER_IDS) }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Legacy/Compatibility Methods
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun getShopName(): String? =
        dataStore.data.map { it[Keys.SHOP_NAME] }.first()

    suspend fun setShopName(name: String) {
        dataStore.edit { it[Keys.SHOP_NAME] = name }
    }

    suspend fun getCurrencySymbol(): String? =
        dataStore.data.map { it[Keys.CURRENCY_SYMBOL] }.first()

    suspend fun setCurrencySymbol(symbol: String) {
        dataStore.edit { it[Keys.CURRENCY_SYMBOL] = symbol }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Terminal Settings - Kiosk Mode & Auto-restart
    // ═══════════════════════════════════════════════════════════════════════

    suspend fun isKioskModeEnabled(): Boolean =
        dataStore.data.map { it[Keys.KIOSK_MODE_ENABLED] ?: false }.first()

    suspend fun setKioskModeEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.KIOSK_MODE_ENABLED] = enabled }
    }

    val kioskModeEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.KIOSK_MODE_ENABLED] ?: false
    }.distinctUntilChanged()

    suspend fun isAutoRestartEnabled(): Boolean =
        dataStore.data.map { it[Keys.AUTO_RESTART_ENABLED] ?: false }.first()

    suspend fun setAutoRestartEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_RESTART_ENABLED] = enabled }
    }

    val autoRestartEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.AUTO_RESTART_ENABLED] ?: false
    }.distinctUntilChanged()

    suspend fun isTaskReopenEnabled(): Boolean =
        dataStore.data.map { it[Keys.TASK_REOPEN_ENABLED] ?: false }.first() // ✅ domyślnie WYŁĄCZONE (zapobiega pętli!)

    suspend fun setTaskReopenEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.TASK_REOPEN_ENABLED] = enabled }
    }

    val taskReopenEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.TASK_REOPEN_ENABLED] ?: true // domyślnie włączone
    }.distinctUntilChanged()

    // ═══════════════════════════════════════════════════════════════════════
    // Clear All
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Czyści wszystkie preferencje (factory reset).
     */
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
        cachedAccessToken = null
    }
}
