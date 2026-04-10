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

    /**
     * Zaplanowana godzina realizacji (deliveryInterval z Order).
     * Jeśli ustawione — kuchnia powinna zacząć gotować odpowiednio wcześniej.
     * Backend przekazuje to pole gdy zamówienie ma ustaloną godzinę dostawy.
     */
    @SerializedName("scheduledFor")
    val scheduledFor: String? = null,  // ISO 8601

    @SerializedName("createdAt")
    val createdAt: String,  // ISO 8601

    @SerializedName("updatedAt")
    val updatedAt: String,  // ISO 8601

    /**
     * Typ zamówienia: "pickup" | "delivery" | "dine_in"
     * Używany do obliczenia czasu rozpoczęcia gotowania (prepTime).
     */
    @SerializedName("orderType")
    val orderType: String? = null,

    /**
     * Czytelny numer KDS z prefiksem typu zamówienia — zwracany przez API.
     * Format: {PREFIX}-{NNN}, np. "W-003", "D-001", "M-007"
     *   W = Wynos (PICKUP)
     *   D = Dostawa (DELIVERY)
     *   M = Na miejscu (DINE_IN)
     * Licznik dzienny — reset o północy (strefa czasu tenanta).
     * Null = API jeszcze nie obsługuje tego pola (fallback: wyświetl orderNumber).
     */
    @SerializedName("kdsTicketNumber")
    val kdsTicketNumber: String? = null
) {
    /**
     * Czytelny numer do wyświetlenia na KDS i w powiadomieniach.
     * Zwraca kdsTicketNumber (np. "W-003") jeśli API go przysyła,
     * w przeciwnym razie fallback na orderNumber (np. "KR-24").
     */
    val displayNumber: String get() = kdsTicketNumber ?: orderNumber

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

    /**
     * Czy to zamówienie zaplanowane (ma ustaloną godzinę realizacji).
     */
    fun isScheduled(): Boolean = scheduledFor != null

    /**
     * Ile milisekund pozostało do planowanej godziny realizacji.
     * Ujemna wartość = czas już minął. Null = brak scheduledFor.
     *
     * Obsługuje formaty ISO 8601:
     *  - "2026-04-07T07:00:00.000Z"  (Instant / UTC z milisekundami)
     *  - "2026-04-07T07:00:00Z"      (Instant / UTC bez milisekund)
     *  - "2026-04-07T09:00:00+02:00" (ZonedDateTime z offsetem)
     */
    fun msUntilScheduled(nowMs: Long = System.currentTimeMillis()): Long? {
        val sf = scheduledFor ?: return null
        return runCatching {
            // Próba 1: Instant.parse — obsługuje Z i .000Z
            java.time.Instant.parse(sf).toEpochMilli() - nowMs
        }.recoverCatching {
            // Próba 2: ZonedDateTime — obsługuje offset +02:00
            java.time.ZonedDateTime.parse(sf).toInstant().toEpochMilli() - nowMs
        }.recoverCatching {
            // Próba 3: OffsetDateTime — fallback
            java.time.OffsetDateTime.parse(sf).toInstant().toEpochMilli() - nowMs
        }.getOrNull()
    }

    /**
     * Ile minut pozostało do planowanej godziny realizacji.
     */
    fun minutesUntilScheduled(nowMs: Long = System.currentTimeMillis()): Long? =
        msUntilScheduled(nowMs)?.let { it / 60_000 }

    /**
     * Czy zamówienie zaplanowane jest jeszcze "daleko w przyszłości" (> 60 min).
     * Ukrywamy je z widoku aktywnych — trafia do zakładki "Zaplanowane".
     *
     * WAŻNE: jeśli SLA już minęło, zamówienie ZAWSZE trafia do Aktywnych —
     * nawet jeśli scheduledFor jest daleko. Dotyczy np. zamówień ASAP z
     * deliveryInterval ustawionym przez klienta na daleki termin.
     */
    fun isScheduledFuture(nowMs: Long = System.currentTimeMillis()): Boolean {
        val mins = minutesUntilScheduled(nowMs) ?: return false
        if (mins <= 60) return false  // bliskie — pokaż w aktywnych

        // Sprawdź SLA — jeśli minęło, zamówienie trafia do Aktywnych
        val slaMs = slaTargetAt?.let {
            runCatching {
                java.time.ZonedDateTime.parse(it).toInstant().toEpochMilli()
            }.getOrNull()
        }
        if (slaMs != null && slaMs <= nowMs) return false  // SLA minęło — nie chowaj!

        return true  // scheduledFor daleko + SLA jeszcze trwa = chowaj w Zaplanowane
    }

    /**
     * Czy zbliża się czas realizacji (0–60 min) — czas zacząć gotować.
     * Ticket automatycznie wskakuje do widoku aktywnych.
     */
    fun isScheduledSoon(nowMs: Long = System.currentTimeMillis()): Boolean {
        val mins = minutesUntilScheduled(nowMs) ?: return false
        return mins in 0..60
    }
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

