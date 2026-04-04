package com.itsorderkds.util

/**
 * Stałe związane z zamówieniami
 */
object OrderConstants {
    const val MIN_PREPARATION_TIME = 15
    const val MAX_PREPARATION_TIME = 120
    const val DEFAULT_PREPARATION_TIME = 30

    val PREPARATION_TIME_OPTIONS = listOf(15, 30, 45, 60)
}

/**
 * ID powiadomień
 */
object NotificationIds {
    const val WS_DISCONNECT = 1997
    const val ORDER_ALARM = 2000
    const val ROUTE_UPDATE = 2
    const val EXTERNAL_DELIVERY_SUCCESS = 3000
    const val SESSION_EXPIRED = 999
}

/**
 * Stałe sieciowe
 */
object NetworkConstants {
    const val TIMEOUT_SECONDS = 30L
    const val MAX_RETRIES = 3
    const val CACHE_SIZE = 10L * 1024 * 1024 // 10 MB
}

/**
 * Stałe bazy danych
 */
object DatabaseConstants {
    const val DATABASE_NAME = "itsorderchat.db"
    const val DATABASE_VERSION = 1
}

/**
 * Klucze SharedPreferences / DataStore
 */
object PreferenceKeys {
    const val BASE_URL = "base_url"
    const val CURRENCY = "currency"
    const val AUTO_PRINT = "auto_print"
    const val AUTO_PRINT_ACCEPTED = "auto_print_accepted"
    const val PRINTER_TYPE = "printer_type"
    const val PRINTER_ID = "printer_id"
    const val USER_ROLE = "user_role"
    const val USER_ID = "user_id"
    const val FCM_TOKEN = "fcm_token"
}

/**
 * Kody błędów
 */
object ErrorCodes {
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val SERVER_ERROR = 500
    const val NETWORK_ERROR = -1
}

