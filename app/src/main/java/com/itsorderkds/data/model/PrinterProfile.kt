package com.itsorderkds.data.model

import kotlinx.serialization.Serializable

/**
 * Predefiniowane profile drukarek z domyślnymi ustawieniami.
 * Ułatwia konfigurację dla znanych modeli drukarek.
 */
@Serializable
enum class PrinterProfile(
    val id: String,
    val displayName: String,
    val description: String,
    val encoding: String,
    val codepage: Int? = null,
    val autoCut: Boolean = false
) {
    POS_8390_DUAL(
        id = "profile_pos_8390_dual",
        displayName = "POS-8390 (DUAL)",
        description = "Drukarka termiczna YHD-8390 Dual Mode (BT Classic + BLE), PC852",
        encoding = "Cp852",
        codepage = 13,
        autoCut = true
    ),

    MOBILE_SSP(
        id = "profile_mobile_ssp",
        displayName = "Mobile (SSP)",
        description = "Mobilna drukarka Bluetooth SSP z UTF-8",
        encoding = "UTF-8",
        codepage = null,
        autoCut = false
    ),

    CUSTOM(
        id = "profile_custom",
        displayName = "Niestandardowy",
        description = "Niestandardowy profil - ustawienia ręczne",
        encoding = "UTF-8",
        codepage = null,
        autoCut = false
    );

    companion object {
        /**
         * Pobiera profil na podstawie ID.
         */
        fun fromId(id: String?): PrinterProfile {
            return values().find { it.id == id } ?: CUSTOM
        }

        /**
         * Zwraca wszystkie dostępne profile.
         */
        fun getAllProfiles(): List<PrinterProfile> {
            return listOf(POS_8390_DUAL, MOBILE_SSP, CUSTOM)
        }
    }
}

