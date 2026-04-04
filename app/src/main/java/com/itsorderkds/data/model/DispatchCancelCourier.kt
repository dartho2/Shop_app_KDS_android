package com.itsorderkds.data.model

import com.google.gson.annotations.SerializedName
import com.itsorderkds.ui.order.CourierEnum

data class DispatchCancelCourier(
    @SerializedName("courier") val courier: CourierEnum?,
    @SerializedName("reason") val reason: String,
)
