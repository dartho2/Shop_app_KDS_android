package com.itsorderkds.data.model

import com.google.gson.annotations.SerializedName

/**
 * Ticket KDS - karta zamówienia w systemie kuchennym
 * Zgodnie z dokumentacją KDS API
 */
data class KdsTicket(
    @SerializedName("_id")
    val id: String,

    @SerializedName("orderId")
    val orderId: String,

    @SerializedName("orderNumber")
    val orderNumber: String,

    @SerializedName("source")
    val source: String? = null,  // "checkout", "portal", etc.

    @SerializedName("priority")
    val priority: String = "normal",  // "normal" | "rush"

    @SerializedName("state")
    val state: String,  // KdsTicketState

    @SerializedName("note")
    val note: String? = null,

    @SerializedName("startedAt")
    val startedAt: String? = null,  // ISO 8601

    @SerializedName("readyAt")
    val readyAt: String? = null,  // ISO 8601

    @SerializedName("handedOffAt")
    val handedOffAt: String? = null,  // ISO 8601

    @SerializedName("cancelledAt")
    val cancelledAt: String? = null,  // ISO 8601

    @SerializedName("slaTargetAt")
    val slaTargetAt: String? = null,  // Deadline SLA (domyślnie: createdAt + 15 min)

    @SerializedName("createdAt")
    val createdAt: String,  // ISO 8601

    @SerializedName("updatedAt")
    val updatedAt: String  // ISO 8601
) {
    /**
     * Sprawdza czy ticket jest nowy
     */
    fun isNew(): Boolean = state == "NEW"

    /**
     * Sprawdza czy ticket jest potwierdzony
     */
    fun isAcked(): Boolean = state == "ACKED"

    /**
     * Sprawdza czy ticket jest w trakcie przygotowania
     */
    fun isInProgress(): Boolean = state == "IN_PROGRESS"

    /**
     * Sprawdza czy ticket jest gotowy
     */
    fun isReady(): Boolean = state == "READY"

    /**
     * Sprawdza czy ticket został wydany
     */
    fun isHandedOff(): Boolean = state == "HANDED_OFF"

    /**
     * Sprawdza czy ticket został anulowany
     */
    fun isCancelled(): Boolean = state == "CANCELLED"

    /**
     * Sprawdza czy ticket ma wysoki priorytet
     */
    fun isRush(): Boolean = priority == "rush"
}

/**
 * Odpowiedź API dla listy ticketów
 */
data class KdsTicketsResponse(
    @SerializedName("data")
    val data: List<KdsTicket>,

    @SerializedName("count")
    val count: Int
)

/**
 * Odpowiedź API dla pojedynczego ticketu z pozycjami
 */
data class KdsTicketWithItemsResponse(
    @SerializedName("ticket")
    val ticket: KdsTicket,

    @SerializedName("items")
    val items: List<KdsTicketItem>
)

