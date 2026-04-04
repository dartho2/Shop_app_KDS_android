package com.itsorderkds.data.model

import com.google.gson.annotations.SerializedName

data class Category(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,                   // String lub mapa tłumaczeń
    @SerializedName("description") val description: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("status") val status: Boolean,
)
data class CategoryResponse(
    val data: List<Category>
)