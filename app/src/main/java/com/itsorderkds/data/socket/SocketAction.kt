package com.itsorderkds.data.socket

import androidx.annotation.StringDef

/** Klucze Intentów i Extrasów używane wyłącznie wewnątrz aplikacji. */
object SocketAction {
    private const val BASE_ACTION = "ACTION_"
    /** Akcje – widoczne tylko w tym module (internal). */
    internal object Action {

        // ---------- Statusy zamówień ----------
        // ---------- Ogólne ----------
        const val NEW_ORDER              = "${BASE_ACTION}NEW_ORDER"          // alias dla ORDER_NEW
        const val ORDER_UPDATE           = "${BASE_ACTION}ORDER_UPDATE"
        const val ORDER_NEW_COURIER      = "${BASE_ACTION}ORDER_NEW_COURIER"
        const val ACCEPT_ORDER_COURIER   = "${BASE_ACTION}ACCEPT_ORDER_COURIER"
        const val REJECT_ORDER_COURIER   = "${BASE_ACTION}REJECT_ORDER_COURIER"

        // ---------- Statusy zamówień ----------
        const val ORDER_PENDING          = "${BASE_ACTION}ORDER_PENDING"
        const val ORDER_CREATED          = "${BASE_ACTION}ORDER_CREATED"
        const val ORDER_CHANGE_STATUS    = "${BASE_ACTION}ORDER_CHANGE_STATUS"
        const val ORDER_NEW              = "${BASE_ACTION}ORDER_NEW"          // równoważne NEW_ORDER
        const val ORDER_PAID             = "${BASE_ACTION}ORDER_PAID"
        const val ORDER_PROCESSING       = "${BASE_ACTION}ORDER_PROCESSING"
        const val ORDER_ACCEPTED         = "${BASE_ACTION}ORDER_ACCEPTED"
        const val ORDER_CANCELLED        = "${BASE_ACTION}ORDER_CANCELLED"
        const val ORDER_REJECTED         = "${BASE_ACTION}ORDER_REJECTED"
        const val ORDER_OUT_FOR_DELIVERY = "${BASE_ACTION}ORDER_OUT_FOR_DELIVERY"
        const val ORDER_COMPLETED        = "${BASE_ACTION}ORDER_COMPLETED"
        const val ORDER_STATUS_ERROR     = "${BASE_ACTION}ORDER_STATUS_ERROR"
        const val ORDER_REMINDER = "${BASE_ACTION}ORDER_REMINDER"
        const val SOCKET_CONNECTED    =  "${BASE_ACTION}SOCKET_CONNECTED"
        const val SOCKET_DISCONNECTED =  "${BASE_ACTION}SOCKET_DISCONNECTED"
        const val TRAS_COURIER_ORDER_NEW =  "${BASE_ACTION}TRAS_COURIER_ORDER_NEW"
        const val OPEN_HOURS_UPDATED =  "${BASE_ACTION}OPEN_HOURS_UPDATED"
        const val OPEN_HOURS_PAUSE_CLEARED =  "${BASE_ACTION}OPEN_HOURS_PAUSE_CLEARED"
        const val OPEN_HOURS_PAUSE_SET =  "${BASE_ACTION}OPEN_HOURS_PAUSE_SET"
        const val OPEN_HOURS_CREATED =  "${BASE_ACTION}OPEN_HOURS_CREATED"
        const val ORDER_SEND_TO_EXTERNAL_SUCCESS = "${BASE_ACTION}ORDER_SEND_TO_EXTERNAL_SUCCESS"
        const val ORDER_SEND_TO_EXTERNAL_FAILED = "${BASE_ACTION}ORDER_SEND_TO_EXTERNAL_FAILED"
        const val ORDER_SEND_TO_EXTERNAL_COURIER = "${BASE_ACTION}ORDER_SEND_TO_EXTERNAL_COURIER"
        /** Adnotacja ograniczająca dozwolony zestaw akcji w czasie kompilacji. */
        @StringDef(
            ORDER_PENDING,
            ORDER_CREATED,
            ORDER_CHANGE_STATUS,
            ORDER_NEW,
            ORDER_PAID,
            ORDER_PROCESSING,
            ORDER_ACCEPTED,
            ORDER_CANCELLED,
            ORDER_REJECTED,
            ORDER_OUT_FOR_DELIVERY,
            ORDER_COMPLETED,
            ORDER_STATUS_ERROR,
            ORDER_UPDATE,
            ORDER_NEW_COURIER,
            ACCEPT_ORDER_COURIER,
            REJECT_ORDER_COURIER,
            SOCKET_CONNECTED,
            SOCKET_DISCONNECTED,
            OPEN_HOURS_UPDATED,
            OPEN_HOURS_PAUSE_CLEARED,
            OPEN_HOURS_PAUSE_SET,
            OPEN_HOURS_CREATED,
            ORDER_REMINDER

        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class Type
    }

    /** Klucze `putExtra` – również tylko w apk. */
    internal object Extra {
        const val PAYLOAD           = "PAYLOAD"
        const val OPEN_ORDER_OBJECT = "OPEN_ORDER_OBJECT"
        const val ORDER_JSON = "ORDER_JSON"
    }
}
