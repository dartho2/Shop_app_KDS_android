package com.itsorderkds.data.model

import com.google.gson.annotations.SerializedName

/* ───── pomocniczy typ dla pozycji GPS ───── */
data class GeoPoint(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

/* ───── PATCH zamówienia (np. /orders/{id}) ───── */
data class UpdateOrderData(
    @SerializedName("delivery_time")
    val deliveryTime: String? = null,
    @SerializedName("lastKnownLocation")
    val lastKnownLocation: GeoPoint? = null,
    @SerializedName("lastLocationTimestamp")
    val lastLocationTimestamp: String? = null
)
//UPDATE OUT_FOR_DELIVERY
data class BatchUpdateStatusRequest(
    @SerializedName("courierId")
    val courierId: String? = null,
    @SerializedName("orderIds")
    val orderIds: Set<String>,
    @SerializedName("newStatus")
    val newStatus: String,
    @SerializedName("delivery_time")
    val deliveryTime: String? = null,
    @SerializedName("lastKnownLocation")
    val lastKnownLocation: GeoPoint? = null,
    @SerializedName("lastLocationTimestamp")
    val lastLocationTimestamp: String? = null
)


/* ───── przypisanie / odpięcie kuriera ───── */
data class UpdateCourierOrder(
    @SerializedName("courier_id")
    val courierId: String? = null                   // null = odpięcie
)




