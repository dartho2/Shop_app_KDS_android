package com.itsorderkds.data.model

import com.google.gson.annotations.SerializedName

/**
 * Typ drukarki przypisanej do pozycji ticketu KDS.
 * Zgodnie z API: printer: 'KITCHEN' | 'STANDARD'
 */
enum class KdsItemPrinterType {
    KITCHEN,
    STANDARD;

    companion object {
        fun fromString(value: String?): KdsItemPrinterType =
            when (value?.uppercase()) {
                "KITCHEN" -> KITCHEN
                "STANDARD" -> STANDARD
                else -> STANDARD  // domyślnie STANDARD gdy brak pola
            }
    }
}

/**
 * Pozycja ticketu KDS - pojedynczy produkt w zamówieniu
 * Zgodnie z dokumentacją KDS API
 */
data class KdsTicketItem(
    @SerializedName("_id")
    val id: String,

    @SerializedName("ticketId")
    val ticketId: String,  // Ref do KdsTicket._id

    @SerializedName("orderId")
    val orderId: String,

    @SerializedName("productId")
    val productId: String? = null,

    @SerializedName("sku")
    val sku: String? = null,

    @SerializedName("posId")
    val posId: String? = null,

    @SerializedName("displayName")
    val displayName: String,  // Nazwa produktu do wyświetlenia

    @SerializedName("qty")
    val qty: Int,  // Ilość sztuk

    @SerializedName("stationId")
    val stationId: String? = null,  // ID stanowiska (MVP: opcjonalne)

    @SerializedName("state")
    val state: String,  // KdsItemState: QUEUED, COOKING, READY, SERVED, VOID

    @SerializedName("notes")
    val notes: List<String> = emptyList(),  // Alergeny, modyfikacje, życzenia

    @SerializedName("printer")
    val printer: String? = null,  // "KITCHEN" | "STANDARD" — typ drukarki dla pozycji

    @SerializedName("firedAt")
    val firedAt: String? = null,  // ISO 8601 — kiedy zaczęto gotować

    @SerializedName("doneAt")
    val doneAt: String? = null,  // ISO 8601 — kiedy ukończono

    @SerializedName("sequence")
    val sequence: Int = 0,  // Kolejność wyświetlania (0-based)

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String
) {
    /** Zwraca enum KdsItemPrinterType na podstawie pola printer */
    val printerType: KdsItemPrinterType
        get() = KdsItemPrinterType.fromString(printer)

    /** true gdy pozycja należy do drukarki kuchennej */
    fun isKitchenItem(): Boolean = printerType == KdsItemPrinterType.KITCHEN

    /** true gdy pozycja należy do drukarki standardowej */
    fun isStandardItem(): Boolean = printerType == KdsItemPrinterType.STANDARD

    /**
     * Sprawdza czy pozycja czeka na przygotowanie
     */
    fun isQueued(): Boolean = state == "QUEUED"

    /**
     * Sprawdza czy pozycja jest w trakcie gotowania
     */
    fun isCooking(): Boolean = state == "COOKING"

    /**
     * Sprawdza czy pozycja jest gotowa
     */
    fun isReady(): Boolean = state == "READY"

    /**
     * Sprawdza czy pozycja została wydana
     */
    fun isServed(): Boolean = state == "SERVED"

    /**
     * Sprawdza czy pozycja została unieważniona
     */
    fun isVoid(): Boolean = state == "VOID"

    /**
     * Zwraca sformatowane notatki jako jeden string
     */
    fun getNotesText(): String = notes.joinToString(", ")
}

