package com.itsorderkds.data.model

import com.google.gson.annotations.SerializedName
import com.itsorderkds.ui.order.DeliveryStatusEnum

/**
 * Payload dla eventu ORDER_SEND_TO_EXTERNAL_COURIER
 * Zawiera informacje o statusie dostawy przez zewnętrznego kuriera
 */
data class ExternalCourierPayload(
    @SerializedName("orderId")
    val orderId: String?,

    @SerializedName("orderNumber")
    val orderNumber: String?,

    @SerializedName("courier")
    val courier: String?,

    @SerializedName("deliveryStatus")
    val deliveryStatus: DeliveryStatusEnum?
)

