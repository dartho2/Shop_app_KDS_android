package com.itsorderkds.data.entity

import java.io.Serializable

data class OrderProductEntity(
    val discount: Double,
    val price: Double,
    val comment: String,
    val note: List<String>?,  // Uwagi do produktu jako tablica stringów
    val salePrice: Double,
    val name: String,
    val quantity: Int,
    val addonsGroup: List<AddonsGroupEntity>
) : Serializable
