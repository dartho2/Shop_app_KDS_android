package com.itsorderkds.data.model

/**
 * Stan ticketu KDS (Kitchen Display System)
 */
enum class KdsTicketState(val displayName: String, val apiValue: String) {
    NEW("Nowe", "NEW"),
    ACKED("Zaakceptowane", "ACKED"),
    IN_PROGRESS("W trakcie", "IN_PROGRESS"),
    READY("Gotowe", "READY"),
    HANDED_OFF("Wydane", "HANDED_OFF"),
    CANCELLED("Anulowane", "CANCELLED");

    companion object {
        fun fromApiValue(v: String?): KdsTicketState? =
            entries.find { it.apiValue.equals(v, ignoreCase = true) }

        /** Statusy ktore mozna wybrac jako trigger wydruku */
        fun triggerOptions(): List<KdsTicketState> =
            listOf(NEW, ACKED, IN_PROGRESS, READY, HANDED_OFF)
    }
}

/**
 * Stan pozycji ticketu KDS
 */
enum class KdsItemState {
    QUEUED,   // Czeka na przygotowanie
    COOKING,  // W trakcie gotowania
    READY,    // Gotowe
    SERVED,   // Wydane gościowi
    VOID      // Unieważnione
}

/**
 * Priorytet ticketu
 */
enum class KdsPriority {
    NORMAL,
    RUSH
}

// KdsStationEnum i KdsPrinterEnum są zdefiniowane w KdsStationEnum.kt

