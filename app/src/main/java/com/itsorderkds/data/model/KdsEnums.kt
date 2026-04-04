package com.itsorderkds.data.model

/**
 * Stan ticketu KDS (Kitchen Display System)
 */
enum class KdsTicketState {
    NEW,           // Ticket właśnie przyszedł z zamówienia
    ACKED,         // Kuchnia potwierdziła przyjęcie
    IN_PROGRESS,   // Trwa przygotowanie
    READY,         // Gotowe do odbioru/wydania
    HANDED_OFF,    // Wydane kurierowi/obsłudze
    CANCELLED      // Anulowane
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

