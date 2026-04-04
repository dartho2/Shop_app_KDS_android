package com.itsorderkds.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class OrderProduct(
    @SerializedName("discount") val discount: Double,
    @SerializedName("price") val price: Double,
    @SerializedName("comment") val comment: String?,
    @SerializedName("note") val note: List<String>?,  // Uwagi do produktu jako tablica stringów
    @SerializedName("sale_price") val salePrice: Double,
    @SerializedName("name") val name: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("addons_group") val addonsGroup: List<AddonsGroup>
) : Serializable

data class AddonsGroup(
    @SerializedName("addons") val addons: List<Addon>
) : Serializable

data class Addon(
    @SerializedName("price") val price: Double,
    @SerializedName("name") val name: String
) : Serializable
