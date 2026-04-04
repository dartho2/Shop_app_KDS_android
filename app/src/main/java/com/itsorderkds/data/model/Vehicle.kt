package com.itsorderkds.data.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Vehicle(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("brand") val brand: String,
    @SerializedName("registrationNumber") val registrationNumber: String,
    @SerializedName("currentMileageMeters") val currentMileageMeters: Int,
    @SerializedName("photoUrl") val photoUrl: String?,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("inUse") val inUse: Boolean,
    @SerializedName("inUseBy") val inUseBy: String?,
    @SerializedName("routes") val routes: List<Route>,
    @SerializedName("lastUsedAt") val lastUsedAt: Date?
)

data class Route(
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String,
    @SerializedName("distanceMeters") val distanceMeters: Number,
    @SerializedName("completedAt") val completedAt: String,
    @SerializedName("courierId") val courierId: String?
)

data class VehicleResponse(
    @SerializedName("data") val data: List<Vehicle>,
    @SerializedName("total") val total: Int
)
