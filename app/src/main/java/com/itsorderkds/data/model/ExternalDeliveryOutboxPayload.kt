package com.itsorderkds.data.model

import com.google.gson.annotations.SerializedName // Jeśli używasz Gson
import com.itsorderkds.ui.order.DeliveryEnum
import com.itsorderkds.ui.order.DeliveryStatusEnum

// import com.fasterxml.jackson.annotation.JsonProperty // Jeśli używasz Jackson

data class ExternalDeliveryOutboxPayload(
    val orderId: String,
    val orderNumber: String,
    // W JSON pole nazywa się "courier" (wartość: "WOLT")
    @SerializedName("courier")
    val integration: String?,

    // W JSON pole nazywa się "deliveryId"
    @SerializedName("deliveryId")
    val externalDeliveryId: String?,

    // W JSON to pole istnieje, warto je dodać
    val externalReference: String?,

    val trackingUrl: String?,

    // Poniższe pola są widoczne w JSON jako daty/metadane
    val createdAt: String?,
    val updatedAt: String?,

    // --- Pola, których NIE MA na zrzucie ekranu ---
    // Zostawiamy je jako nullable (?), bo mogą przyjść w innym statusie
    // lub trzeba je usunąć, jeśli nigdy nie przychodzą w tym obiekcie.
    val status: DeliveryStatusEnum?,
    val pickupEta: String?,
    val dropoffEta: String?,
    val courierName: String?,
    val courierPhone: String?
)
