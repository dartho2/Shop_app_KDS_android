package com.itsorderkds.data.model

import com.google.gson.annotations.SerializedName

/**
 * Typ drukarki przypisanej do pozycji ticketu KDS.
 * Zachowany dla kompatybilności wstecznej — używaj KdsPrinterEnum.
 */
@Deprecated("Use KdsPrinterEnum instead", ReplaceWith("KdsPrinterEnum"))
enum class KdsItemPrinterType {
    KITCHEN,
    STANDARD;

    companion object {
        fun fromString(value: String?): KdsItemPrinterType =
            when (value?.uppercase()) {
                "KITCHEN" -> KITCHEN
                else -> STANDARD
            }
    }
}

/**
 * Sekcja produkcyjna pozycji KDS.
 * Jeden item może mieć wiele sekcji (np. składniki z różnych stacji).
 * Zgodnie z API — breaking change kwiecień 2026 (punkt 13).
 *
 * Przykład: Hosomaki Tamago ×2 → productions = [
 *   { label="Krewetka w tempurze", station="KITCHEN", printer="KITCHEN", qty=4 },
 *   { label="Łosoś pieczony",      station="KITCHEN", printer="KITCHEN", qty=12 }
 * ]
 */
data class KdsProductionTask(
    /** Stacja KDS dla tej sekcji produkcyjnej */
    @SerializedName("station")
    val station: String? = null,

    /** Drukarka dla tej sekcji */
    @SerializedName("printer")
    val printer: String? = null,

    /** Nazwa sekcji (np. "Krewetka w tempurze") */
    @SerializedName("label")
    val label: String? = null,

    /** Ilość = zamówiona_ilość × task.quantity */
    @SerializedName("qty")
    val qty: Int = 0
) {
    val stationEnum: KdsStationEnum get() = KdsStationEnum.fromApiValue(station)
    val printerEnum: KdsPrinterEnum? get() = KdsPrinterEnum.fromApiValueOrNull(printer)
}

/**
 * Pozycja ticketu KDS - pojedynczy produkt w zamówieniu.
 *
 * Breaking change kwiecień 2026 (punkt 13 dokumentacji):
 *  - Każdy produkt = zawsze 1 KdsTicketItem (nie N itemów per sekcja)
 *  - [displayName] = zawsze nazwa produktu (nie label sekcji)
 *  - [productions] = lista sekcji produkcyjnych z wyliczonymi ilościami
 *
 * Kluczowe pola:
 *  - [station] — który TABLET wyświetla tę pozycję (MAIN, KITCHEN, SUSHI, BAR, DESSERT)
 *  - [printer] — która DRUKARKA drukuje label tej pozycji (może być inna niż station!)
 *    np. station=MAIN ale printer=KITCHEN oznacza: wyświetl na MAIN, wydrukuj na KITCHEN
 *    null = brak drukowania lub drukuj domyślnie wg konfiguracji
 *  - [productions] — sekcje produkcyjne (null jeśli brak)
 */
data class KdsTicketItem(
    @SerializedName("_id")
    val id: String,

    @SerializedName("ticketId")
    val ticketId: String,

    @SerializedName("orderId")
    val orderId: String,

    @SerializedName("productId")
    val productId: String? = null,

    @SerializedName("sku")
    val sku: String? = null,

    @SerializedName("posId")
    val posId: String? = null,

    /** Nazwa produktu — zawsze = product.name, nigdy zastępowana przez task.label */
    @SerializedName("displayName")
    val displayName: String,

    /**
     * Kopia nazwy produktu (nowe pole z API kwiecień 2026).
     * Identyczne z [displayName] — dla czytelności kodu.
     */
    @SerializedName("productName")
    val productName: String? = null,

    @SerializedName("qty")
    val qty: Int,

    /**
     * Stacja KDS (który tablet wyświetla).
     * Wartości: "MAIN" | "KITCHEN" | "SUSHI" | "BAR" | "DESSERT"
     * null → domyślnie MAIN
     */
    @SerializedName("station")
    val station: String? = null,

    @SerializedName("stationId")
    val stationId: String? = null,

    @SerializedName("state")
    val state: String,  // QUEUED | COOKING | READY | SERVED | VOID

    @SerializedName("notes")
    val notes: List<String> = emptyList(),

    /**
     * Drukarka fizyczna dla tej pozycji.
     * Wartości: "MAIN" | "KITCHEN" | "SUSHI" | "BAR" | "DESSERT" | null
     * null = brak przypisania (drukuj domyślnie lub wcale)
     *
     * WAŻNE: niezależne od [station]!
     * Np. station=MAIN + printer=KITCHEN → wyświetl na MAIN, wydrukuj na KITCHEN
     */
    @SerializedName("printer")
    val printer: String? = null,

    /**
     * Sekcje produkcyjne (breaking change kwiecień 2026).
     * Jeden item = jeden produkt. Sekcje jako lista szczegółów.
     * null = produkt bez sekcji produkcyjnych (prosty item).
     *
     * Przykład: Hosomaki Tamago ×2 z 2 sekcjami:
     * productions = [
     *   { label="Krewetka w tempurze", qty=4 },
     *   { label="Łosoś pieczony",      qty=12 }
     * ]
     */
    @SerializedName("productions")
    val productions: List<KdsProductionTask>? = null,

    @SerializedName("firedAt")
    val firedAt: String? = null,

    @SerializedName("doneAt")
    val doneAt: String? = null,

    @SerializedName("sequence")
    val sequence: Int = 0,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String
) {
    /** Enum stacji z API. Domyślnie MAIN gdy brak pola. */
    val stationEnum: KdsStationEnum
        get() = KdsStationEnum.fromApiValue(station)

    /** Enum drukarki z API. null gdy brak przypisania (pole printer = null/pusty). */
    val printerEnum: KdsPrinterEnum?
        get() = KdsPrinterEnum.fromApiValueOrNull(printer)

    /**
     * Sprawdza czy ta pozycja powinna być wydrukowana na drukarce [role].
     * Używane przy filtrowaniu items per-drukarka.
     */
    fun shouldPrintOn(role: KdsPrinterEnum): Boolean {
        if (printerEnum == role) return true
        // Fallback: jeśli printer = null i brak productions, to patrzymy na station
        if (printerEnum == null && productions.isNullOrEmpty() && stationEnum.apiValue == role.apiValue) return true
        return false
    }

    /**
     * Sprawdza czy ta pozycja ma przypisaną drukarkę kuchenną (legacy compat).
     */
    @Deprecated("Use shouldPrintOn(KdsPrinterEnum.KITCHEN)")
    fun isKitchenItem(): Boolean = printerEnum == KdsPrinterEnum.KITCHEN

    /**
     * Sprawdza czy ta pozycja powinna być widoczna na tablecie o stacji [myStation].
     *
     * Logika zgodna z dokumentacją (punkt 13.3):
     *  - MAIN → pokazuj wszystkie items (brak filtrowania)
     *  - Inne stacje → pokazuj tylko items gdzie item.station == myStation
     *
     * Dodatkowo: item bez przypisanej stacji (station=null) zawsze widoczny na MAIN.
     */
    fun isVisibleOnStation(myStation: KdsStationEnum): Boolean {
        if (myStation == KdsStationEnum.MAIN) return true   // MAIN = widzi wszystko
        return stationEnum == myStation
    }

    /** Czy pozycja ma sekcje produkcyjne (nowe API). */
    fun hasProductions(): Boolean = !productions.isNullOrEmpty()

    fun isQueued(): Boolean = state == "QUEUED"
    fun isCooking(): Boolean = state == "COOKING"
    fun isReady(): Boolean = state == "READY"
    fun isServed(): Boolean = state == "SERVED"
    fun isVoid(): Boolean = state == "VOID"

    fun getNotesText(): String = notes.joinToString(", ")
}


