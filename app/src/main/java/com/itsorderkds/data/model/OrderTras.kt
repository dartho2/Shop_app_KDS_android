package com.itsorderkds.data.model

import com.google.gson.annotations.SerializedName
import com.itsorderkds.ui.order.OrderStatusEnum
import com.itsorderkds.ui.order.SourceEnum
import java.io.Serializable

data class OrderTras(
    @SerializedName("id")  val id: String,
    @SerializedName("orderId")  val orderId: String,
    @SerializedName("courierId") val courierId: String,
    @SerializedName("polyline") val polyline: String,
    // Tu JSON potrafi zwrócić null
    @SerializedName("eta")  val eta: String? = null,

    @SerializedName("score") val score: Int = 0,
    @SerializedName("is_asap") val isAsap: Boolean = false,
    // orderNumber przy RETURN też bywa "RETURN" lub pusty
    @SerializedName("orderNumber") val orderNumber: String = "",

    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("phone") val phone: String? = null,

    @SerializedName("plannedDeliveryTime") val plannedDeliveryTime: String? = null,
    @SerializedName("delayMinutes") val delayMinutes: Int = 0,
    @SerializedName("estimatedStartTime") val estimatedStartTime: String? = null,
    @SerializedName("source") val source: SourceEnum? = null,
    // Użyj Double?, żeby Gson przyjmował zarówno 8.0 jak i 8
    @SerializedName("distanceKm") val distance: Double? = null,

    @SerializedName("status") val status: OrderStatusEnum,
) : Serializable
