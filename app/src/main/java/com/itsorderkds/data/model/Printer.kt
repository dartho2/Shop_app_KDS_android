package com.itsorderkds.data.model

import kotlinx.serialization.Serializable

/**
 * Typ połączenia z drukarką.
 */
@Serializable
enum class PrinterConnectionType(val displayName: String, val description: String, val icon: String) {
    BLUETOOTH("Bluetooth", "Połączenie bezprzewodowe BT", "📶"),
    NETWORK("Sieć", "Połączenie przez Wi-Fi/LAN", "🌐"),
    BUILTIN("Wbudowana", "Wbudowana drukarka urządzenia", "🖨️");

    fun getLabel(): String = "$icon $displayName"

    companion object {
        fun fromString(value: String?): PrinterConnectionType {
            return try {
                valueOf(value?.uppercase() ?: "BLUETOOTH")
            } catch (_: Exception) {
                BLUETOOTH
            }
        }
    }
}

/**
 * Typy drukarek - określa rolę i przeznaczenie drukarki.
 * Każda drukarka ma jeden typ (KITCHEN, STANDARD, BAR itp.)
 */
@Serializable
enum class PrinterType(val displayName: String, val description: String, val icon: String) {
    KITCHEN("Kuchnia", "Drukarka dla personelu kuchni", "🍳"),
    STANDARD("Standard", "Główna drukarka zamówień", "🧾"),
    BAR("Bar", "Drukarka dla baru/napoi", "🍹"),
    DESSERTS("Desery", "Drukarka dla sekcji deserów", "🍰"),
    PIZZA("Pizza", "Drukarka dla pieczarni", "🍕"),
    DELIVERY("Dostawa", "Drukarka dla etykiet wysyłki", "📦"),
    LABELS("Etykiety", "Drukarka dla etykiet produktów", "🏷️"),
    CUSTOM("Niestandardowa", "Drukarka do celów specjalnych", "⚙️");

    /**
     * Zwraca czytelny tekst dla UI
     */
    fun getLabel(): String = "$icon $displayName"

    companion object {
        /**
         * Pobiera typ z nazwy (bezpiecznie)
         */
        fun fromString(value: String?): PrinterType {
            return try {
                valueOf(value?.uppercase() ?: "STANDARD")
            } catch (_: Exception) {
                STANDARD
            }
        }

        /**
         * Lista wszystkich typów do wyboru w UI
         */
        fun getAllOptions(): List<PrinterType> = entries.toList()
    }
}

/**
 * Konfiguracja szablonu wydruku dla drukarki.
 * Każda drukarka może mieć własne ustawienia formatowania.
 */
@Serializable
data class TemplateConfig(
    val showPrices: Boolean = true,              // Czy pokazywać ceny produktów i sumy
    val showPaymentMethod: Boolean = true,       // Czy pokazywać formę płatności
    val productSpacing: Int = 0,                 // Odstępy między produktami (0-3 linie)
    val showProductOptions: Boolean = true,      // Czy pokazywać opcje produktów (dodatki, modyfikatory)
    val showCustomerNotes: Boolean = true,       // Czy pokazywać uwagi klienta
    val showDeliveryAddress: Boolean = true,     // Czy pokazywać adres dostawy
    val showOrderSource: Boolean = true,         // Czy pokazywać źródło zamówienia (Uber, Glovo itp.)
    val showTaxes: Boolean = true,               // Czy pokazywać podatki
    val showDiscounts: Boolean = true,           // Czy pokazywać rabaty/kupony
    val compactMode: Boolean = false             // Tryb kompaktowy (mniejsze odstępy, mniejsza czcionka nagłówka)
) {
    companion object {
        /**
         * Domyślna konfiguracja dla drukarki głównej (pełny paragon).
         */
        fun standard() = TemplateConfig(
            showPrices = true,
            showPaymentMethod = true,
            productSpacing = 0,
            showProductOptions = true,
            showCustomerNotes = true,
            showDeliveryAddress = true,
            showOrderSource = true,
            showTaxes = true,
            showDiscounts = true,
            compactMode = false
        )

        /**
         * Domyślna konfiguracja dla drukarki kuchennej (bilet kuchenny).
         * Ukrywa ceny, płatność, ale pokazuje wszystkie opcje produktów.
         */
        fun kitchen() = TemplateConfig(
            showPrices = false,
            showPaymentMethod = false,
            productSpacing = 1,
            showProductOptions = true,
            showCustomerNotes = true,
            showDeliveryAddress = false,
            showOrderSource = true,
            showTaxes = false,
            showDiscounts = false,
            compactMode = false
        )
    }
}

/**
 * Model pojedynczej drukarki w systemie.
 * Może być nieograniczona liczba drukarek.
 */
@Serializable
data class Printer(
    val id: String,
    val name: String,
    val deviceId: String,
    val connectionType: PrinterConnectionType = PrinterConnectionType.BLUETOOTH,
    val networkIp: String? = null,
    val networkPort: Int = 9100,
    val printerType: PrinterType = PrinterType.STANDARD,
    /**
     * Rola tej drukarki fizycznej w kontekście KDS API.
     * Określa które pozycje (KdsTicketItem.printer) mają być na niej drukowane.
     *
     * Przykłady:
     *  - KdsPrinterEnum.KITCHEN → drukuj items gdzie printer="KITCHEN"
     *  - KdsPrinterEnum.SUSHI   → drukuj items gdzie printer="SUSHI"
     *  - KdsPrinterEnum.MAIN    → drukuj items gdzie printer="MAIN" lub printer=null (fallback)
     *  - null                   → stare zachowanie: drukuj wszystkie pozycje (brak filtrowania)
     */
    val kdsRole: KdsPrinterEnum? = null,
    val profileId: String,
    val templateId: String,
    val templateConfig: TemplateConfig = TemplateConfig.standard(),
    val encoding: String = "UTF-8",
    val codepage: Int? = null,
    val autoCut: Boolean = false,
    val enabled: Boolean = true,
    val order: Int = 0,
    val plainTextMode: Boolean = false
) {
    /**
     * Konwersja do legacy PrinterSettings (backward compatibility).
     * Używane podczas migracji i w starym kodzie.
     */
    fun toPrinterSettings(): PrinterSettings {
        return PrinterSettings(
            deviceId = deviceId,
            encodingName = encoding,
            codepageNumber = codepage,
            templateId = templateId,
            autoCut = autoCut,
            lineChars = 32 // domyślna szerokość dla ESC/POS
        )
    }

    /**
     * Czy drukarka jest typu DUAL (BT Classic + BLE).
     * Dual mode wymaga dłuższych timeoutów i specjalnej obsługi.
     */
    fun isDualMode(): Boolean {
        return profileId == "profile_pos_8390_dual"
    }

    /**
     * Zwraca czytelną nazwę profilu.
     */
    fun getProfileDisplayName(): String {
        return when (profileId) {
            "profile_pos_8390_dual" -> "POS-8390 (DUAL)"
            "profile_mobile_ssp" -> "Mobile (SSP)"
            "profile_custom" -> "Niestandardowy"
            else -> profileId
        }
    }

    /**
     * Zwraca czytelną nazwę szablonu.
     */
    fun getTemplateDisplayName(): String {
        return when (templateId) {
            "template_standard"     -> "Standardowy"
            "template_compact"      -> "Kompaktowy"
            "template_detailed"     -> "Szczegółowy"
            "template_minimal"      -> "Minimalny"
            "template_kitchen_only" -> "Kuchenny (filtered)"
            else -> templateId
        }
    }

    /**
     * Zwraca czytelną nazwę roli KDS lub informację o braku filtrowania.
     */
    fun kdsRoleDisplayName(): String = kdsRole?.displayName ?: "Wszystkie pozycje (brak filtrowania)"

    /**
     * Czy drukarka ma zdefiniowaną rolę KDS — używane do decyzji o filtrowaniu items.
     */
    fun hasKdsRole(): Boolean = kdsRole != null
}

/**
 * Legacy model ustawień drukarki (backward compatibility).
 * Używany w starym kodzie PrinterService.
 */
data class PrinterSettings(
    val deviceId: String,
    val encodingName: String,
    val codepageNumber: Int?,
    val templateId: String,
    val autoCut: Boolean,
    val lineChars: Int = 32
)
