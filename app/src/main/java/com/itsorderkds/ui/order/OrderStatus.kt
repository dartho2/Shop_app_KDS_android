package com.itsorderkds.ui.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



/**
 * Single Source of Truth: Status zamówienia
 * - apiValue: wartość używana w API (lowercase/snake_case)
 * - toString(): zwraca apiValue dla Retrofit (@Path, @Query)
 * - fromApiValue(): bezpieczne mapowanie z API na enum
 */
@Serializable
enum class OrderStatusEnum(val apiValue: String) {
    @SerialName("pending") PENDING("pending"),
    @SerialName("processing") PROCESSING("processing"),
    @SerialName("completed") COMPLETED("completed"),
    @SerialName("shipped") SHIPPED("shipped"),
    @SerialName("on_hold") ON_HOLD("on_hold"),
    @SerialName("preorder") PREORDER("preorder"),
    @SerialName("cancelled") CANCELLED("cancelled"),
    @SerialName("refunded") REFUNDED("refunded"),
    @SerialName("failed") FAILED("failed"),
    @SerialName("out_for_delivery") OUT_FOR_DELIVERY("out_for_delivery"),
    @SerialName("accepted") ACCEPTED("accepted"),
    @SerialName("waiting_for_accepted") WAITING_FOR_ACCEPTED("waiting_for_accepted"),
    @SerialName("other") OTHER("other"),
    @SerialName("unknown") UNKNOWN("unknown"),
    @SerialName("return") RETURN("return");

    override fun toString(): String = apiValue

    companion object {
        /**
         * Bezpieczne mapowanie z API na enum.
         * @param value wartość z API (np. "completed")
         * @return odpowiedni enum lub UNKNOWN jeśli nie znaleziono
         */
        fun fromApiValue(value: String?): OrderStatusEnum =
            entries.find { it.apiValue == value } ?: UNKNOWN
    }
}

/**
 * Single Source of Truth: Typ dostawy zewnętrznej
 */
@Serializable
enum class DeliveryEnum(val apiValue: String) {
    @SerialName("stava") STAVA("stava"),
    @SerialName("stuart") STUART("stuart"),
    @SerialName("wolt") WOLT("wolt"),
    @SerialName("glovo") GLOVO("glovo"),
    @SerialName("uber") UBER("uber");

    override fun toString(): String = apiValue

    companion object {
        fun fromApiValue(value: String?): DeliveryEnum? =
            entries.find { it.apiValue == value }
    }
}

/**
 * Single Source of Truth: Typ kuriera zewnętrznego
 */
@Serializable
enum class CourierEnum(val apiValue: String) {
    @SerialName("stava") STAVA("stava"),
    @SerialName("stuart") STUART("stuart"),
    @SerialName("glovo") GLOVO("glovo"),
    @SerialName("uber") UBER("uber"),
    @SerialName("bolt") BOLT("bolt"),
    @SerialName("wolt") WOLT("wolt"),
    @SerialName("other") OTHER("other");

    override fun toString(): String = apiValue

    companion object {
        fun fromApiValue(value: String?): CourierEnum =
            entries.find { it.apiValue == value } ?: OTHER
    }
}

/**
 * Single Source of Truth: Status dostawy zewnętrznej
 */
@Serializable
enum class DeliveryStatusEnum(val apiValue: String) {
    /** Zamówienie zostało wysłane do kuriera, oczekuje na akceptację */
    @SerialName("requested") REQUESTED("requested"),

    /** Kurier zaakceptował zamówienie */
    @SerialName("accepted") ACCEPTED("accepted"),

    /** Kurier w drodze (pickup started / picked up / dropoff started) */
    @SerialName("in_transit") IN_TRANSIT("in_transit"),

    /** Dostawa zakończona pomyślnie */
    @SerialName("delivered") DELIVERED("delivered"),

    /** Zamówienie odrzucone przez kuriera */
    @SerialName("rejected") REJECTED("rejected"),

    /** Zamówienie anulowane */
    @SerialName("cancelled") CANCELLED("cancelled"),

    /** Dostawa nie powiodła się (customer no-show, inne błędy) */
    @SerialName("failed") FAILED("failed");

    override fun toString(): String = apiValue

    companion object {
        fun fromApiValue(value: String?): DeliveryStatusEnum =
            entries.find { it.apiValue == value } ?: REQUESTED
    }
}

/**
 * Single Source of Truth: Metoda płatności
 */
@Serializable
enum class PaymentMethod(val apiValue: String) {
    @SerialName("cod") COD("cod"),
    @SerialName("paynow") PAYNOW("paynow"),
    @SerialName("cash") CASH("cash"),
    @SerialName("card") CARD("card"),
    @SerialName("online") ONLINE("online"),
    @SerialName("bank_transfer") BANK_TRANSFER("bank_transfer"),
    @SerialName("cash_on_delivery") CASH_ON_DELIVERY("cash_on_delivery"),
    @SerialName("blik") BLIK("blik"),
    @SerialName("transfer") TRANSFER("transfer"),
    @SerialName("unknown") UNKNOWN("unknown");

    override fun toString(): String = apiValue

    companion object {
        fun fromApiValue(value: String?): PaymentMethod =
            entries.find { it.apiValue == value } ?: UNKNOWN
    }
}

@Serializable
enum class TypeOrderEnum(val apiValue: String) {
    @SerialName("asap") ASAP("asap"),
    @SerialName("preorder") PREORDER("preorder"),
    @SerialName("schedule") SCHEDULE("schedule"),
    @SerialName("unknown") UNKNOWN("unknown");
    override fun toString(): String = apiValue

    companion object {
        fun fromApiValue(value: String?): TypeOrderEnum =
            entries.find { it.apiValue == value } ?: UNKNOWN
    }
}

/**
 * Single Source of Truth: Status płatności
 */
@Serializable
enum class PaymentStatus(val apiValue: String) {
    @SerialName("pending") PENDING("pending"),
    @SerialName("abandoned") ABANDONED("abandoned"),
    @SerialName("processing") PROCESSING("processing"),
    @SerialName("failed") FAILED("failed"),
    @SerialName("failure") FAILURE("failure"),
    @SerialName("aborted") ABORTED("aborted"),
    @SerialName("expired") EXPIRED("expired"),
    @SerialName("refunded") REFUNDED("refunded"),
    @SerialName("awaiting_for_approval") AWAITING_FOR_APPROVAL("awaiting_for_approval"),
    @SerialName("completed") COMPLETED("completed"),
    @SerialName("confirmed") CONFIRMED("confirmed"),
    @SerialName("paid") PAID("paid"),
    @SerialName("new") NEW("new"),
    @SerialName("reject") REJECT("reject"),
    @SerialName("unknown") UNKNOWN("unknown");

    override fun toString(): String = apiValue

    companion object {
        fun fromApiValue(value: String?): PaymentStatus =
            entries.find { it.apiValue == value } ?: UNKNOWN
    }
}

/**
 * Single Source of Truth: Typ dostawy zamówienia
 */
@Serializable
enum class OrderDelivery(val apiValue: String) {
    @SerialName("flat_rate") FLAT_RATE("flat_rate"),
    @SerialName("pick_up") PICK_UP("pick_up"),
    @SerialName("local_pickup") LOCAL_PICKUP("local_pickup"),
    @SerialName("delivery_external") DELIVERY_EXTERNAL("delivery_external"),
    @SerialName("pickup") PICKUP("pickup"),
    @SerialName("delivery") DELIVERY("delivery"),
    @SerialName("dine_in") DINE_IN("dine_in"),
    @SerialName("room_service") ROOM_SERVICE("room_service"),
    @SerialName("unknown") UNKNOWN("unknown");

    override fun toString(): String = apiValue

    companion object {
        fun fromApiValue(value: String?): OrderDelivery =
            entries.find { it.apiValue == value } ?: UNKNOWN
    }
}

/**
 * Single Source of Truth: Status kuriera
 */
@Serializable
enum class CourierStatus(val apiValue: String) {
    @SerialName("active") ACTIVE("active"),
    @SerialName("finished") FINISHED("finished"),
    @SerialName("inactive") INACTIVE("inactive"),
    @SerialName("unknown") UNKNOWN("unknown");

    override fun toString(): String = apiValue

    companion object {
        fun fromApiValue(value: String?): CourierStatus =
            entries.find { it.apiValue == value } ?: UNKNOWN
    }
}

/**
 * Single Source of Truth: Źródło zamówienia
 */
@Serializable
enum class SourceEnum(val apiValue: String) {
    @SerialName("woocommerce") WOOCOMMERCE("woocommerce"),
    @SerialName("gopos") GOPOS("gopos"),
    @SerialName("woo") WOO("woo"),
    @SerialName("bolt") BOLT("bolt"),
    @SerialName("uber") UBER("uber"),
    @SerialName("wolt") WOLT("wolt"),
    @SerialName("glovo") GLOVO("glovo"),
    @SerialName("takeaway") TAKEAWAY("takeaway"),
    @SerialName("unknown") UNKNOWN("unknown"),
    @SerialName("other") OTHER("other"),
    @SerialName("its") ITS("its");

    override fun toString(): String = apiValue

    companion object {
        fun fromApiValue(value: String?): SourceEnum =
            entries.find { it.apiValue == value } ?: UNKNOWN
    }
}
