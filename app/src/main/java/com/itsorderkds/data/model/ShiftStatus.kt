package com.itsorderkds.data.model

import com.google.gson.annotations.SerializedName
import com.itsorderkds.ui.order.CourierStatus
import java.io.Serializable

data class ShiftResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("shift")
    val shift: Shift?
) : Serializable {
    fun toShiftStatus(): ShiftStatus {
        return ShiftStatus(isAssigned = shift != null)
    }
}

data class Shift(
    @SerializedName("_id")
    val id: String,

    @SerializedName("courier")
    val courier: String,

    @SerializedName("date")
    val date: String,

    @SerializedName("initialLocation")
    val initialLocation: Location?,

    @SerializedName("startTime")
    val startTime: String?,

    @SerializedName("status")
    val status: CourierStatus?
) : Serializable

data class Location(
    @SerializedName("type")
    val type: String,

    @SerializedName("coordinates")
    val coordinates: List<Double>
) : Serializable

data class ShiftStatus(
    @SerializedName("isAssigned")
    val isAssigned: Boolean
) : Serializable

data class ShiftCheckIn(
    @SerializedName("date")
    val date: String,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("vehicleId")
    val vehicleId: String? = null,

    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("vehicleInfo")
    val vehicleInfo: String? = null
) : Serializable

data class ShiftCheckOut(
    @SerializedName("date")
    val date: String
) : Serializable
