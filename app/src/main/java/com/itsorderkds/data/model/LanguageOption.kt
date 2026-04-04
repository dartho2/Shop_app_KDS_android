package com.itsorderkds.data.model
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.itsorderkds.data.util.FlexibleDoubleAdapter


data class LanguageOption (
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,                   // String lub mapa tłumaczeń
    @SerializedName("flag") val flag: String?,
    @SerializedName("is_rtl") val isRtl: Boolean?,
    @SerializedName("status") val status: Boolean?,
    @SerializedName("system_reserve") val systemReserve: Int?,
    @SerializedName("locale") val locale: String
)
data class LanguagesResponse(
    val data: List<LanguageOption>
)