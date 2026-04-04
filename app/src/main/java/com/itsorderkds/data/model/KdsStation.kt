package com.itsorderkds.data.model

import com.google.gson.annotations.SerializedName

/**
 * Stanowisko kuchni KDS
 * Zgodnie z dokumentacją KDS API
 */
data class KdsStation(
    @SerializedName("_id")
    val id: String,

    @SerializedName("code")
    val code: String,  // Unikalny kod, uppercase: "GRILL", "FRYER", "BAR"

    @SerializedName("name")
    val name: String,  // Nazwa do wyświetlenia

    @SerializedName("isActive")
    val isActive: Boolean = true,

    @SerializedName("displayOrder")
    val displayOrder: Int = 0,  // Kolejność sortowania w UI

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String
)

/**
 * Odpowiedź API dla listy stanowisk
 */
data class KdsStationsResponse(
    @SerializedName("data")
    val data: List<KdsStation>
)

