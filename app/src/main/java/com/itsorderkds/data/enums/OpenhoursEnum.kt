package com.itsorderkds.data.enums

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// status z /openhours/now
enum class RestaurantStatus : Serializable {
    @SerializedName("OPEN")
    OPEN,
    @SerializedName("PAUSED")
    PAUSED,
    @SerializedName("CLOSED")
    CLOSED
}

// źródło aktualnego statusu (harmonogram / ręcznie / pauza)
enum class SourceOrder : Serializable {
    @SerializedName("SCHEDULE")
    SCHEDULE,
    @SerializedName("MANUAL")
    MANUAL,
    @SerializedName("PAUSE")
    PAUSE,
    @SerializedName("EXCEPTION")
    EXCEPTION,
    @SerializedName("WEEKLY")
    WEEKLY
}

// z backendu do pauzy: all | delivery | pickup
enum class OrderScope : Serializable {
    @SerializedName("ALL")
    ALL,
    @SerializedName("DELIVERY")
    DELIVERY,
    @SerializedName("PICKUP")
    PICKUP
}