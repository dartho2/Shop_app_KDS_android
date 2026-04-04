package com.itsorderkds.data.entity

import androidx.room.Embedded
import java.io.Serializable

data class ShippingAddressEntity(
    val street: String,
    val city: String,
    val numberHome: String,
    val numberFlat: String,
    @Embedded(prefix = "shipping_coordinates_")
    val coordinates: CoordinatesEntity,
) : Serializable

data class CoordinatesEntity(
    val lat: Double?,
    val lng: Double?
) : Serializable