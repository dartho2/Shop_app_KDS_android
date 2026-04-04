package com.itsorderkds.util

import android.annotation.SuppressLint
import android.content.Context
import androidx.preference.PreferenceManager
import android.content.SharedPreferences
import com.itsorderkds.util.AppPrefs.DeletablePrefKey.BASE_URL
import com.itsorderkds.util.AppPrefs.DeletablePrefKey.TOKEN_CHAT
import com.itsorderkds.util.AppPrefs.DeletablePrefKey.ID_CHAT
import com.itsorderkds.util.AppPrefs.DeletablePrefKey.FCM_TOKEN
import com.itsorderkds.util.AppPrefs.DeletablePrefKey.ROLE
import com.itsorderkds.util.AppPrefs.DeletablePrefKey.USERID
import com.itsorderkds.util.AppPrefs.DeletablePrefKey.CURRENCY
import com.itsorderkds.util.AppPrefs.DeletablePrefKey.AUTO_PRINT
import com.itsorderkds.util.AppPrefs.DeletablePrefKey.AUTO_PRINT_ACCEPTED
import com.itsorderkds.util.AppPrefs.DeletablePrefKey.PRINT_TEMPLATE
import com.itsorderkds.util.AppPrefs.DeletablePrefKey.AUTO_PRINT_AFTER_ORDER
import com.itsorderkds.util.AppPrefs.DeletablePrefKey.INTEGRATION_STAVA_ACTIVE
import com.itsorderkds.util.AppPrefs.DeletablePrefKey.INTEGRATION_WOLT_DRIVE_ACTIVE
import com.itsorderkds.util.AppPrefs.DeletablePrefKey.INTEGRATION_STUART_ACTIVE
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber

@SuppressLint("StaticFieldLeak")
object AppPrefs {
    interface PrefKey
    private lateinit var context: Context

    private enum class DeletablePrefKey : PrefKey {
        BASE_URL, TOKEN_CHAT, ID_CHAT, FCM_TOKEN, ROLE, USERID, CURRENCY,
        AUTO_PRINT, AUTO_PRINT_ACCEPTED,
        INTEGRATION_STAVA_ACTIVE, INTEGRATION_WOLT_DRIVE_ACTIVE, INTEGRATION_STUART_ACTIVE,
        AUTO_PRINT_AFTER_ORDER, PRINT_TEMPLATE,
        // Drukarki – legacy/konfig
        PRINTER_PROFILE, PRINTER_TYPE, PRINTER_ID,
        PRINTER_KITCHEN_ENABLED, PRINTER_KITCHEN_ID, PRINTER_KITCHEN_ENCODING,
        PRINTER_KITCHEN_CODEPAGE, PRINTER_KITCHEN_AUTO_CUT
    }

    private const val PREFS_NAME = "itsorderchat_prefs"
    private const val MIGRATION_VERSION_KEY = "printer_migration_version"
    private const val CURRENT_MIGRATION_VERSION = 2

    private const val KEY_AUTH_TOKEN = "key_auth_token"
    private const val KEY_LOGIN_URL = "key_login_url"
    private const val KEY_LOGIN_KEY = "key_login_key"
    private const val KEY_LOGIN_SECURITY = "key_login_security"
    private const val KEY_SHOP_NAME = "key_shop_name"
    private const val KEY_CURRENCY_SYMBOL = "key_currency_symbol"
    private const val KEY_LOGIN_STAVA_TOKEN = "key_login_stava_token"
    private const val KEY_LOGIN_STAVA_URL = "key_login_stava_url"


    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    fun isInitialized(): Boolean { return ::context.isInitialized }
    fun saveAuthToken(context: Context, token: String) {
        val prefs = getSharedPreferences(context)
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_AUTH_TOKEN, null)
    }


    fun setLoginUrl(context: Context, url: String) {
        val prefs = getSharedPreferences(context)
        prefs.edit().putString(KEY_LOGIN_URL, url).apply()
    }
    fun setFCMToken(token: String) {
        setString(FCM_TOKEN, token)
    }
    fun setFCMTokenChat(token: String) {
        setString(FCM_TOKEN, token )
    }
    fun setUserRole(role: String) {
        setString(ROLE, role )
    }
    fun setUserId(id: String) {
        setString(USERID, id )
    }
    fun setCurrency(text: String) {
        setString(CURRENCY, text )
    }
    fun setAutoPrintEnabled(isEnabled: Boolean) {
        setBoolean(AUTO_PRINT, isEnabled )
    }
    fun setAutoPrintAcceptedEnabled(isEnabled: Boolean) {
        setBoolean(AUTO_PRINT_ACCEPTED, isEnabled )
    }
    fun setAutoPrintAfterOrder(isEnabled: Boolean) {
        setBoolean(AUTO_PRINT_AFTER_ORDER, isEnabled)
    }
    /* ---- Integracje kurierskie ---- */
    fun setStavaIntegrationActive(isActive: Boolean) {
        setBoolean(INTEGRATION_STAVA_ACTIVE, isActive)
    }

    fun setWoltDriveIntegrationActive(isActive: Boolean) {
        setBoolean(INTEGRATION_WOLT_DRIVE_ACTIVE, isActive)
    }

    fun setStuartIntegrationActive(isActive: Boolean) {
        setBoolean(INTEGRATION_STUART_ACTIVE, isActive)
    }

    fun getStavaIntegrationActive() = getBoolean(INTEGRATION_STAVA_ACTIVE, false)
    fun getWoltDriveIntegrationActive() = getBoolean(INTEGRATION_WOLT_DRIVE_ACTIVE, false)
    fun getStuartIntegrationActive() = getBoolean(INTEGRATION_STUART_ACTIVE, false)

    /* ---- drukarka (legacy removed) ---- */
    // Używamy wyłącznie PrinterPreferences/PrinterService; legacy klucze usunięte.

    /* ---- Szablony wydruku (Print Templates) ---- */

    fun getPrintTemplate(default: String = "template_standard"): String {
        return getString(PRINT_TEMPLATE, default)
    }

    fun setPrintTemplate(template: String) {
        setString(PRINT_TEMPLATE, template)
    }

    // Fallback encoding getter for legacy compatibility
//    private fun getPrinterEncoding(): String {
//        return "UTF-8" // default encoding
//    }

    // Preferred encoding with codepage (nullable) stored per-printer
    fun setPrinterPreferredEncodingFor(printerId: String, encoding: String, codepage: Int?) {
        if (printerId.isBlank()) return
        val prefs = getPreferences()
        prefs.edit().putString("printer_pref_encoding_" + printerId, encoding)
            .putInt("printer_pref_codepage_" + printerId, codepage ?: -1)
            .apply()
    }

    fun getPrinterPreferredEncodingFor(printerId: String?): Pair<String?, Int?> {
        if (printerId.isNullOrBlank()) return null to null
        val prefs = getPreferences()
        val enc = prefs.getString("printer_pref_encoding_" + printerId, null)
        val cp = prefs.getInt("printer_pref_codepage_" + printerId, -1)
        return enc to if (cp == -1) null else cp
    }

    fun clearPrinterPreferredEncodingFor(printerId: String) {
        if (printerId.isBlank()) return
        val prefs = getPreferences()
        prefs.edit().remove("printer_pref_encoding_" + printerId)
            .remove("printer_pref_codepage_" + printerId)
            .apply()
    }

    fun getPrinter(context: Context): Pair<String?, String?> {
        // Czytamy z domyślnych SharedPreferences (legacy)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val type = PreferenceUtils.getString(prefs, com.itsorderkds.util.PreferenceKeys.PRINTER_TYPE, "") ?: ""
        val id = PreferenceUtils.getString(prefs, com.itsorderkds.util.PreferenceKeys.PRINTER_ID, "") ?: ""
        return if (type.isBlank() || id.isBlank()) null to null else type to id
    }

    // Printer profile helpers
    fun setPrinterProfile(profile: String) {
        setString(DeletablePrefKey.PRINTER_PROFILE, profile)
    }

    fun getPrinterProfile(): String? {
        val p = getString(DeletablePrefKey.PRINTER_PROFILE, "")
        return if (p.isBlank()) null else p
    }

    // Per-printer profile mapping: stored under key printer_profile_<printerId>
    fun setPrinterProfileFor(printerId: String?, profileId: String) {
        if (printerId.isNullOrBlank()) return
        val key = "printer_profile_$printerId"
        getPreferences().edit().putString(key, profileId).apply()
    }

    fun getPrinterProfileFor(printerId: String?): String? {
        if (printerId.isNullOrBlank()) return getPrinterProfile()
        val p = getPreferences().getString("printer_profile_$printerId", null)
        return if (p.isNullOrBlank()) getPrinterProfile() else p
    }

    data class PrinterConfig(val encoding: String, val codepage: Int?, val hasCutter: Boolean)

    /**
     * Returns effective printer configuration for given printerId.
     * Resolution order:
     * 1) per-printer preferred encoding (setPrinterPreferredEncodingFor)
     * 2) if printerProfile or printerId contains known model (e.g. YHD) -> model defaults
     * 3) fallback to global printer encoding
     */
    fun getPrinterConfigFor(printerId: String?): PrinterConfig {
         // 1) per-printer preferred
         val (encPref, cpPref) = getPrinterPreferredEncodingFor(printerId)
         if (encPref != null) return PrinterConfig(encPref, cpPref, false)

         // 2) model profiles
         // Try per-printer profile first, fallback to global
         val profile = getPrinterProfileFor(printerId)
         if (!profile.isNullOrBlank() && profile.contains("YHD", ignoreCase = true)) {
             return PrinterConfig("CP852", 13, true)
         }
         if (!profile.isNullOrBlank() && profile.equals("Mobile", ignoreCase = true)) {
             return PrinterConfig("UTF-8", null, false)
         }

        // 3) fallback via printerId heuristics
        if (!printerId.isNullOrBlank() && printerId.contains("YHD", ignoreCase = true)) {
            return PrinterConfig("CP852", 13, true)
        }

        // default
        val fallbackEnc = getPrinterEncoding()
        return PrinterConfig(fallbackEnc, null, false)
    }

    // Per-printer auto-cut enabled flag
    fun setAutoCutEnabledFor(printerId: String, enabled: Boolean) {
        if (printerId.isBlank()) return
        getPreferences().edit().putBoolean("printer_auto_cut_" + printerId, enabled).apply()
    }

    fun getAutoCutEnabledFor(printerId: String?): Boolean {
        if (printerId.isNullOrBlank()) return false
        val key = "printer_auto_cut_" + printerId
        return getPreferences().getBoolean(key, false)
    }

    // Per-printer cutter flag
    fun setPrinterHasCutterFor(printerId: String, hasCutter: Boolean) {
        if (printerId.isBlank()) return
        getPreferences().edit().putBoolean("printer_pref_has_cutter_" + printerId, hasCutter).apply()
    }

    fun getPrinterHasCutterFor(printerId: String?): Boolean? {
        if (printerId.isNullOrBlank()) return null
        val key = "printer_pref_has_cutter_" + printerId
        val prefs = getPreferences()
        return if (prefs.contains(key)) prefs.getBoolean(key, false) else null
    }

    // Update getPrinterConfigFor to consider per-printer preferred encoding and cutter
    fun getPrinterConfigForUpdated(printerId: String?): PrinterConfig {
        // 1) per-printer preferred
        val (encPref, cpPref) = getPrinterPreferredEncodingFor(printerId)
        val cutterPref = getPrinterHasCutterFor(printerId) ?: false
        if (encPref != null) return PrinterConfig(encPref, cpPref, cutterPref)

        // 2) per-printer profile
        val profile = getPrinterProfileFor(printerId)
        if (!profile.isNullOrBlank() && profile.contains("YHD", ignoreCase = true)) {
            return PrinterConfig("CP852", 13, cutterPref)
        }
        if (!profile.isNullOrBlank() && profile.equals("Mobile", ignoreCase = true)) {
            return PrinterConfig("UTF-8", null, cutterPref)
        }

        // 3) fallback via printerId heuristics
        if (!printerId.isNullOrBlank() && printerId.contains("YHD", ignoreCase = true)) {
            return PrinterConfig("CP852", 13, cutterPref)
        }

        // default
        val fallbackEnc = getPrinterEncoding()
        return PrinterConfig(fallbackEnc, null, cutterPref)
    }

    fun setBaseUrl(raw: String) {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty()) { "Adres serwera jest pusty" }

        // 1) Schemat
        val withScheme = when {
            trimmed.startsWith("http://",  true) -> trimmed
            trimmed.startsWith("https://", true) -> trimmed
            else -> "https://$trimmed"
        }

        // 2) Slash na końcu (Retrofit tego wymaga)
        val normalized = if (withScheme.endsWith("/")) withScheme else "$withScheme/"

        // 3) Parsowanie + walidacja hosta
        val httpUrl = normalized.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Nieprawidłowy adres serwera")

        require(httpUrl.host.isNotBlank()) { "URL musi zawierać domenę/host" }

        // 4) Zapis (OkHttp kanonikalizuje – zachowaj to co zwraca)
        setString(BASE_URL, httpUrl.toString())
    }
    fun getCurrency() = getCurrencyRest(CURRENCY)
    fun getUserID() = getUserIDs(USERID)
    fun getUserRole() = getUserRoles(ROLE)
    fun getFCMToken() = getFCMTokens(FCM_TOKEN)
    fun getBaseUrl() = getBaseUrls(BASE_URL)
    fun getAutoPrintEnabled() = getAutoPrintEnabled(AUTO_PRINT)
    fun getAutoPrintAcceptedEnabled() = getAutoPrintEnabled(AUTO_PRINT_ACCEPTED)
    private fun getBaseUrls(key: PrefKey, defaultValue: String= "http://localhost:8001/"): String {
        return PreferenceUtils.getString(getPreferences(), key.toString(), defaultValue) ?: defaultValue
    }
    private fun getAutoPrintEnabled(key: PrefKey, defaultValue: Boolean= false): Boolean {
        return PreferenceUtils.getBoolean(getPreferences(), key.toString(), defaultValue) ?: defaultValue
    }
    fun getPreferences(): SharedPreferences {
        if (!::context.isInitialized) {
            throw IllegalStateException("AppPrefs has not been initialized. Call AppPrefs.init(context) in Application class.")
        }
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun getLoginUrl(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_LOGIN_URL, null)
    }
    fun setTokenChat(token: String) {
        setString(TOKEN_CHAT, token )
    }
    fun setIdChat(id: String) {
        setString(ID_CHAT, id )
    }
    fun getIdChat() = getString(ID_CHAT)
    fun getTokenChat() = getString(TOKEN_CHAT)
    fun setLoginKey(context: Context, key: String) {
        val prefs = getSharedPreferences(context)
        prefs.edit().putString(KEY_LOGIN_KEY, key).apply()
    }

    fun getLoginKey(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_LOGIN_KEY, null)
    }

    fun setLoginSecurity(context: Context, security: String) {
        val prefs = getSharedPreferences(context)
        prefs.edit().putString(KEY_LOGIN_SECURITY, security).apply()
    }

    fun getLoginSecurity(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_LOGIN_SECURITY, null)
    }

    fun setShopName(context: Context, shopName: String) {
        val prefs = getSharedPreferences(context)
        prefs.edit().putString(KEY_SHOP_NAME, shopName).apply()
    }

    fun getShopName(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_SHOP_NAME, null)
    }

    fun setShopCurrencySymbol(context: Context, currencySymbol: String) {
        val prefs = getSharedPreferences(context)
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, currencySymbol).apply()
    }
    private fun getFCMTokens(key: PrefKey, defaultValue: String= ""): String {
        return PreferenceUtils.getString(getPreferences(), key.toString(), defaultValue) ?: defaultValue
    }
    private fun getUserRoles(key: PrefKey, defaultValue: String= "User"): String {
        return PreferenceUtils.getString(getPreferences(), key.toString(), defaultValue) ?: defaultValue
    }
    private fun getCurrencyRest(key: PrefKey, defaultValue: String= "PLNX"): String {
        return PreferenceUtils.getString(getPreferences(), key.toString(), defaultValue) ?: defaultValue
    }
    private fun getUserIDs(key: PrefKey, defaultValue: String= ""): String {
        return PreferenceUtils.getString(getPreferences(), key.toString(), defaultValue) ?: defaultValue
    }
    fun getShopCurrencySymbol(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_CURRENCY_SYMBOL, null)
    }

    fun setLoginStavaToken(context: Context, token: String) {
        val prefs = getSharedPreferences(context)
        prefs.edit().putString(KEY_LOGIN_STAVA_TOKEN, token).apply()
    }

    fun getLoginStavaToken(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_LOGIN_STAVA_TOKEN, null)
    }

    fun setLoginStavaUrl(context: Context, url: String) {
        val prefs = getSharedPreferences(context)
        prefs.edit().putString(KEY_LOGIN_STAVA_URL, url).apply()
    }

    fun getLoginStavaUrl(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_LOGIN_STAVA_URL, null)
    }

    fun clear(context: Context) {
        val prefs = getSharedPreferences(context)
        prefs.edit().clear().apply()
    }

    fun init(context: Context) {
        AppPrefs.context = context.applicationContext
        performMigration()
    }

    /**
     * Migracja: jeśli wcześniej była jedna drukarka, ustaw ją jako "front/sala",
     * a "kitchen" zostaw pusta.
     */
    private fun performMigration() {
        val prefs = getPreferences()
        val currentVersion = prefs.getInt(MIGRATION_VERSION_KEY, 1)

        if (currentVersion < 2) {
            try {
                val (existingType, existingId) = getPrinter(context)
                if (!existingType.isNullOrBlank() && !existingId.isNullOrBlank()) {
                    // Ustaw starą drukarkę jako FRONT (sala)
                    setString(DeletablePrefKey.PRINTER_TYPE, existingType)
                    setString(DeletablePrefKey.PRINTER_ID, existingId)

                    // Kuchnia zostaje pusta / nie włączona
                    setBoolean(DeletablePrefKey.PRINTER_KITCHEN_ENABLED, false)
                }

                prefs.edit().putInt(MIGRATION_VERSION_KEY, CURRENT_MIGRATION_VERSION).apply()
                Timber.d("AppPrefs: migration to v2 completed")
            } catch (e: Exception) {
                Timber.w(e, "AppPrefs: migration error")
            }
        }
    }

    // === API dla drukarki KITCHEN ===

    fun isKitchenPrinterEnabled(): Boolean {
        return getBoolean(DeletablePrefKey.PRINTER_KITCHEN_ENABLED, false)
    }

    fun setKitchenPrinterEnabled(enabled: Boolean) {
        setBoolean(DeletablePrefKey.PRINTER_KITCHEN_ENABLED, enabled)
    }

    fun setKitchenPrinter(deviceId: String?, encoding: String = "UTF-8", codepage: Int? = null) {
        if (deviceId == null) {
            setString(DeletablePrefKey.PRINTER_KITCHEN_ID, "")
            return
        }
        setString(DeletablePrefKey.PRINTER_KITCHEN_ID, deviceId)
        setString(DeletablePrefKey.PRINTER_KITCHEN_ENCODING, encoding)
        codepage?.let { setInt(DeletablePrefKey.PRINTER_KITCHEN_CODEPAGE, it) }
    }

    fun getKitchenPrinterMac(): String? {
        val id = getString(DeletablePrefKey.PRINTER_KITCHEN_ID, "")
        return if (id.isBlank()) null else id
    }

    fun getKitchenPrinterConfig(): PrinterConfig {
        val enc = getString(DeletablePrefKey.PRINTER_KITCHEN_ENCODING, "UTF-8").ifBlank { "UTF-8" }
        val cp = getInt(DeletablePrefKey.PRINTER_KITCHEN_CODEPAGE, -1).takeIf { it != -1 }
        val autoCut = getBoolean(DeletablePrefKey.PRINTER_KITCHEN_AUTO_CUT, false)
        return PrinterConfig(enc, cp, autoCut)
    }

    fun setKitchenPrinterAutoCut(enabled: Boolean) {
        setBoolean(DeletablePrefKey.PRINTER_KITCHEN_AUTO_CUT, enabled)
    }

    fun getKitchenPrinterAutoCut(): Boolean {
        return getBoolean(DeletablePrefKey.PRINTER_KITCHEN_AUTO_CUT, false)
    }

    // === API dla drukarki FRONT (sala/standardowa) ===

    fun setFrontPrinterMac(deviceId: String?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit()
            .putString(com.itsorderkds.util.PreferenceKeys.PRINTER_TYPE, "bluetooth")
            .putString(com.itsorderkds.util.PreferenceKeys.PRINTER_ID, deviceId ?: "")
            .apply()
    }

    fun getFrontPrinterMac(): String? {
        val (_, id) = getPrinter(context)
        return if (id.isNullOrBlank()) null else id
    }

    fun getFrontPrinterConfig(): PrinterConfig {
        val (_, id) = getPrinter(context)
        return getPrinterConfigForUpdated(id)
    }

    fun setFrontPrinterAutoCut(enabled: Boolean) {
        val (_, id) = getPrinter(context)
        if (!id.isNullOrBlank()) {
            setAutoCutEnabledFor(id, enabled)
        }
    }

    fun getFrontPrinterAutoCut(): Boolean {
        val (_, id) = getPrinter(context)
        return if (id != null) getAutoCutEnabledFor(id) else false
    }

    // === KITCHEN PRINTER - PRINT TEMPLATES ===
    fun getKitchenPrintTemplate(): String {
        return getString(DeletablePrefKey.PRINT_TEMPLATE, "template_standard")
    }

    fun setKitchenPrintTemplate(templateId: String) {
        setString(DeletablePrefKey.PRINT_TEMPLATE, templateId)
    }

    // === KITCHEN PRINTER - PROFILES ===
    fun getKitchenPrinterProfile(): String {
        return getPreferences().getString("kitchen_printer_profile", "profile_utf8") ?: "profile_utf8"
    }

    fun setKitchenPrinterProfile(profileId: String) {
        getPreferences().edit().putString("kitchen_printer_profile", profileId).apply()
    }

    // === KITCHEN PRINTER - ENCODING ===
    fun setKitchenPrinterEncoding(encoding: String, codepage: Int? = null) {
        setString(DeletablePrefKey.PRINTER_KITCHEN_ENCODING, encoding)
        codepage?.let { setInt(DeletablePrefKey.PRINTER_KITCHEN_CODEPAGE, it) }
    }

    fun getKitchenPrinterEncoding(): Pair<String, Int?> {
        val enc = getString(DeletablePrefKey.PRINTER_KITCHEN_ENCODING, "UTF-8").ifBlank { "UTF-8" }
        val cp = getInt(DeletablePrefKey.PRINTER_KITCHEN_CODEPAGE, -1).takeIf { it != -1 }
        return enc to cp
    }

    // === Unified printer settings per type ===
    private fun prefKeyFor(type: com.itsorderkds.ui.settings.print.PrinterType, suffix: String): String {
        return when (type) {
            com.itsorderkds.ui.settings.print.PrinterType.STANDARD -> "printer_std_$suffix"
            com.itsorderkds.ui.settings.print.PrinterType.KITCHEN -> "printer_kitchen_$suffix"
        }
    }

    fun getPrinterSettings(type: com.itsorderkds.ui.settings.print.PrinterType): com.itsorderkds.ui.settings.print.PrinterSettings {
        val deviceId = getPreferences().getString(prefKeyFor(type, "id"), null)
        val profile = getPreferences().getString(prefKeyFor(type, "profile"), "profile_utf8") ?: "profile_utf8"
        val encoding = getPreferences().getString(prefKeyFor(type, "encoding"), "UTF-8") ?: "UTF-8"
        val codepage = getPreferences().getInt(prefKeyFor(type, "codepage"), -1).takeIf { it != -1 }
        val template = getPreferences().getString(prefKeyFor(type, "template"), if (type==com.itsorderkds.ui.settings.print.PrinterType.KITCHEN) "template_compact" else "template_standard")
            ?: "template_standard"
        val autoCut = getPreferences().getBoolean(prefKeyFor(type, "autocut"), false)
        val enabled = getPreferences().getBoolean(prefKeyFor(type, "enabled"), type == com.itsorderkds.ui.settings.print.PrinterType.STANDARD)
        return com.itsorderkds.ui.settings.print.PrinterSettings(type, deviceId, profile, encoding, codepage, template, autoCut, enabled)
    }

    fun setPrinterSettings(settings: com.itsorderkds.ui.settings.print.PrinterSettings) {
        getPreferences().edit().apply {
            putString(prefKeyFor(settings.type, "id"), settings.deviceId)
            putString(prefKeyFor(settings.type, "profile"), settings.profileId)
            putString(prefKeyFor(settings.type, "encoding"), settings.encoding)
            putInt(prefKeyFor(settings.type, "codepage"), settings.codepage ?: -1)
            putString(prefKeyFor(settings.type, "template"), settings.templateId)
            putBoolean(prefKeyFor(settings.type, "autocut"), settings.autoCut)
            putBoolean(prefKeyFor(settings.type, "enabled"), settings.enabled)
        }.apply()
    }

    // Helper for kitchen settings from unified API (used in UI)
    fun setKitchenPrinterConfig(settings: com.itsorderkds.ui.settings.print.PrinterSettings) {
        setPrinterSettings(settings)
    }

    // Globalne kodowanie – fallback (gdy brak profilu/per-printer)
    fun getPrinterEncoding(): String {
        return getPreferences().getString("printer_global_encoding", "UTF-8") ?: "UTF-8"
    }

    private fun setInt(key: PrefKey, value: Int) {
        PreferenceUtils.setInt(getPreferences(), key.toString(), value)
    }
    private fun getInt(key: PrefKey, defaultValue: Int): Int {
        return PreferenceUtils.getInt(getPreferences(), key.toString(), defaultValue) ?: defaultValue
    }
    private fun setString(key: PrefKey, value: String) {
        getPreferences().edit().putString(key.toString(), value).apply()
    }
    private fun getString(key: PrefKey, defaultValue: String = ""): String {
        return getPreferences().getString(key.toString(), defaultValue) ?: defaultValue
    }
    private fun setBoolean(key: PrefKey, value: Boolean) {
        getPreferences().edit().putBoolean(key.toString(), value).apply()
    }
    private fun getBoolean(key: PrefKey, defaultValue: Boolean = false): Boolean {
        return getPreferences().getBoolean(key.toString(), defaultValue)
    }

    // Przeciążenia bezpośrednio na String – dla kompatybilności miejsc, gdzie przekazujemy literalny klucz
    private fun setInt(key: String, value: Int) {
        PreferenceUtils.setInt(getPreferences(), key, value)
    }
    private fun getInt(key: String, defaultValue: Int): Int {
        return PreferenceUtils.getInt(getPreferences(), key, defaultValue) ?: defaultValue
    }
    private fun setString(key: String, value: String) {
        getPreferences().edit().putString(key, value).apply()
    }
    private fun getString(key: String, defaultValue: String = ""): String {
        return getPreferences().getString(key, defaultValue) ?: defaultValue
    }
    private fun setBoolean(key: String, value: Boolean) {
        getPreferences().edit().putBoolean(key, value).apply()
    }
    private fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return getPreferences().getBoolean(key, defaultValue)
    }
}
