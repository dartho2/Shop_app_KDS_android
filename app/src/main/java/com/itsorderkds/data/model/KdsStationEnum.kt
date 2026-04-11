package com.itsorderkds.data.model

/**
 * Stacje KDS — identyfikator tabletu/stanowiska kuchni.
 * Zgodnie z dokumentacją (punkt 13.1, 13.3).
 *
 * Tablet KDS może być skonfigurowany jako jedna ze stacji.
 * Stacja MAIN widzi wszystkie pozycje.
 * Inne stacje widzą tylko pozycje z pasującym item.station.
 */
enum class KdsStationEnum(val apiValue: String, val displayName: String) {
    MAIN("MAIN", "Główna (wszystkie)"),
    KITCHEN("KITCHEN", "Kuchnia"),
    SUSHI("SUSHI", "Sushi Bar"),
    BAR("BAR", "Bar"),
    DESSERT("DESSERT", "Desery");

    companion object {
        /**
         * Parsuj wartość z API (case-insensitive).
         * Gdy brak lub nieznana wartość → domyślnie MAIN.
         */
        fun fromApiValue(value: String?): KdsStationEnum =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: MAIN
    }
}

/**
 * Drukarki KDS — identyfikator fizycznej drukarki przypisanej do pozycji.
 * Zgodnie z dokumentacją (punkt 13.1).
 *
 * Pole `printer` na KdsTicketItem wskazuje na której drukarce drukować.
 * null = brak przypisania (drukuj domyślnie lub wcale).
 *
 * WAŻNE: `printer` jest NIEZALEŻNE od `station`!
 * Np. station=MAIN + printer=KITCHEN → wyświetl na MAIN, wydrukuj na KITCHEN.
 */
enum class KdsPrinterEnum(val apiValue: String, val displayName: String) {
    MAIN("MAIN", "Drukarka główna"),
    KITCHEN("KITCHEN", "Drukarka kuchenna"),
    SUSHI("SUSHI", "Drukarka sushi"),
    BAR("BAR", "Drukarka bar"),
    DESSERT("DESSERT", "Drukarka desery");

    companion object {
        /**
         * Parsuj wartość z API (case-insensitive).
         * Gdy brak lub null → zwraca null (brak przypisania drukarki).
         */
        fun fromApiValueOrNull(value: String?): KdsPrinterEnum? =
            if (value.isNullOrBlank()) null
            else entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }

        /**
         * Parsuj z fallbackiem (gdy mamy pewność że wartość powinna być znana).
         */
        fun fromApiValue(value: String?, fallback: KdsPrinterEnum = MAIN): KdsPrinterEnum =
            fromApiValueOrNull(value) ?: fallback
    }
}

