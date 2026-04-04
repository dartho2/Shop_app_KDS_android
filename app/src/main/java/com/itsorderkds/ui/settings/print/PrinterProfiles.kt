package com.itsorderkds.ui.settings.print

/**
 * Predefiniowane profile drukarek ESC/POS
 */
data class PrinterProfile(
    val id: String,
    val name: String,
    val encodingName: String,
    val codepageNumber: Int?,
    val description: String
)

object PrinterProfiles {
    val PROFILES = listOf(
        PrinterProfile(
            id = "profile_utf8",
            name = "UTF-8 (uniwersalny)",
            encodingName = "UTF-8",
            codepageNumber = null,
            description = "Kodowanie UTF-8 bez ESC t"
        ),
        PrinterProfile(
            id = "profile_cp852",
            name = "CP852 (PC852 - Polski)",
            encodingName = "Cp852",
            codepageNumber = 13,
            description = "Dla drukarek z PC852, ESC t 13"
        ),
        PrinterProfile(
            id = "profile_cp437",
            name = "CP437 (PC437 - Standard)",
            encodingName = "Cp437",
            codepageNumber = 0,
            description = "Standard ASCII, ESC t 0"
        ),
        PrinterProfile(
            id = "profile_cp1250",
            name = "CP1250 (Central Europe)",
            encodingName = "Cp1250",
            codepageNumber = 11,
            description = "Dla krajów Europy Środkowej"
        ),
        PrinterProfile(
            id = "profile_cp858",
            name = "CP858 (Multilingual)",
            encodingName = "Cp858",
            codepageNumber = 3,
            description = "Obsługa znaków wielojęzycznych"
        ),
        PrinterProfile(
            id = "profile_custom",
            name = "Niestandardowy",
            encodingName = "",
            codepageNumber = null,
            description = "Wpisz ręcznie kodowanie i codepage"
        )
    )

    fun getProfileById(id: String): PrinterProfile? {
        return PROFILES.find { it.id == id }
    }

    fun getProfileByName(name: String): PrinterProfile? {
        return PROFILES.find { it.name == name }
    }
}

