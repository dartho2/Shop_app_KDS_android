package com.itsorderkds.ui.settings

import com.google.gson.annotations.SerializedName

import java.io.Serializable

data class SettingsDto(
    @SerializedName("values") val values: ValuesDto,  // "HH:mm"
) : Serializable

data class ValuesDto(
    @SerializedName("general") val general: SettingsGeneralDto,
    @SerializedName("integrations") val integrations: integrationsDto
) : Serializable

data class SettingsGeneralDto(
    @SerializedName("default_currency") val default_currency: SettingsGeneralCurrencyDto?
) : Serializable

data class integrationsDto(
    @SerializedName("gopos") val gopos: IntegrationItemDto?,
    @SerializedName("goorder") val goorder: IntegrationItemDto?,
    @SerializedName("woocommerce") val woocommerce: IntegrationItemDto?,
    @SerializedName("stava") val stava: IntegrationItemDto?,
    @SerializedName("woltDrive") val woltDrive: IntegrationItemDto?,
    @SerializedName("stuart") val stuart: IntegrationItemDto?  // Dodaję też Stuart na przyszłość
) : Serializable

data class IntegrationItemDto(
    @SerializedName("title") val title: String?,
    @SerializedName("isActive") val isActive: Boolean?
) : Serializable

data class SettingsGeneralCurrencyDto(
    @SerializedName("code") val code: String?,
    @SerializedName("symbol") val symbol: String? ,
    @SerializedName("symbol_position") val symbol_position: String?
):Serializable
