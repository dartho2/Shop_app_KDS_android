package com.itsorderkds.data.model

import com.google.gson.annotations.SerializedName
import com.itsorderkds.ui.order.DeliveryEnum

data class DispatchCourier(
    @SerializedName("courier") val courier: DeliveryEnum,
    @SerializedName("options") val options: Map<String, Any?>? = null,
    @SerializedName("timePrepare") val timePrepare: Int? = null,
    @SerializedName("timeDeliver") val timeDeliver: Int? = null,
    @SerializedName("timeDelivery") val timeDelivery: String? = null
)
