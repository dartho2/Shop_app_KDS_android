package com.itsorderkds.data.model

import com.google.gson.annotations.SerializedName
import com.itsorderkds.ui.order.CourierEnum
import com.itsorderkds.ui.order.DeliveryStatusEnum
import com.itsorderkds.ui.order.OrderDelivery
import com.itsorderkds.ui.order.OrderStatusEnum
import com.itsorderkds.ui.order.PaymentMethod
import com.itsorderkds.ui.order.PaymentStatus
import com.itsorderkds.ui.order.SourceEnum
import com.itsorderkds.ui.order.TypeOrderEnum
import java.io.Serializable

// Strona paginowana zamówień z API
data class PaginatedOrders(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("data") val orders: List<Order>
)

data class OrderWrapper(
    val order: Order
)

data class CourierChangeTrasPayload(
    val orderId: String,
    val courier: Courier?
)

data class CourierChangePayload(
    val orderId: String,
    val courier: Courier?
)

data class OrderStatusWrapper(
    val orderStatus: OrderStatus,
    val orderId: String
)

// Główna encja zamówienia
data class Order(
    @SerializedName("id") val orderId: String,
    val status: Boolean,
    @SerializedName("total") val total: Double,
    val consumer: Consumer?,
    @SerializedName("order_number") val orderNumber: String,
    @SerializedName("order_status") val orderStatus: OrderStatus,
    @SerializedName("order_status_activities") val orderStatusActivities: List<OrderStatusActivity>?,
    @SerializedName("payment_method") val paymentMethod: PaymentMethod?,
    @SerializedName("payment_status") val paymentStatus: PaymentStatus?,
    @SerializedName("payment_status_rank") val paymentStatusRank: Int?,
    @SerializedName("amount") val amount: Double,
    @SerializedName("tax_total") val taxTotal: Double?,
    @SerializedName("shipping_total") val shippingTotal: Double,
    @SerializedName("wallet_balance") val walletBalance: Double,
    @SerializedName("additional_fee_total") val additionalFeeTotal: Double?,
    @SerializedName("additional_fees") val additionalFees: List<AdditionalFee> = emptyList(),
    @SerializedName("coupon_total_discount") val couponTotalDiscount: Double?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("is_guest") val isGuest: Boolean?,
    @SerializedName("points_amount") val pointsAmount: Double?,
    @SerializedName("used_point") val usedPoint: Double?,
    @SerializedName("createdAt") val createdAt: String?,        // <-- zmiana z Date? na String?
    @SerializedName("updatedAt") val updatedAt: String?,        // <-- zmiana
    @SerializedName("shipping_address") val shippingAddress: ShippingAddress,
    @SerializedName("delivery_interval") val deliveryInterval: String?, // <-- zmiana
    @SerializedName("delivery_time") val deliveryTime: String?,         // <-- zmiana
    @SerializedName("is_asap") val isAsap: Boolean,
    @SerializedName("products") val products: List<OrderProduct> = emptyList(),
    @SerializedName("note") val note: String?,
    @SerializedName("delivery_type") val deliveryType: OrderDelivery?,
    @SerializedName("courier") val courier: Courier?,
    @SerializedName("source") val source: SourceOrder?,
    @SerializedName("order_key") val orderKey: String?,
    @SerializedName("ip") val ip: String?,
    @SerializedName("delivery") val externalDelivery: ExternalDelivery?,
    @SerializedName("type") val type: TypeOrderEnum?,
    val isScheduled: Boolean = false,
) : Serializable

data class AdditionalFee(
    val label: String,
    val amount: Double,
)

data class SourceOrder(
    val sourceId: String,
    val number: String,
    val name: SourceEnum,
)

data class Courier(
    val id: String,
    val name: String,
)

// Dane klienta
data class Consumer(
    val name: String,
    val email: String,
    val phone: String,
    @SerializedName("country_code") val countryCode: String
) : Serializable

// Status zamówienia
data class OrderStatus(
    //    val id: String,
    val name: String,
    val sequence: Int,
    val slug: String?,
    //    @SerializedName("system_reserve") val systemReserve: Boolean,
    //    val status: Boolean
) : Serializable

// Adres Dostawy
data class ShippingAddress(
    val street: String,
    val city: String,
    @SerializedName("number_home") val numberHome: String,
    @SerializedName("number_flat") val numberFlat: String,
    @SerializedName("coordinates") val coordinates: Coordinates? = null
) : Serializable

data class Coordinates(
    val lat: Double,
    val lng: Double
) : Serializable

// Aktywności statusów
data class OrderStatusActivity(
    @SerializedName("status") val status: String?,
    @SerializedName("at") val at: String?,   // <-- zmiana z Date? na String?
    @SerializedName("by") val by: String?
)

// Zewnętrzna dostawa
data class ExternalDelivery(
    @SerializedName("courier") val courier: CourierEnum? = null,
    @SerializedName("status") val status: DeliveryStatusEnum? = null,
    @SerializedName("trackingUrl") val trackingUrl: String? = null,
    @SerializedName("deliveryId") val deliveryId: String? = null,
    @SerializedName("orderReference") val orderReference: String? = null,
    @SerializedName("pickupEta") val pickupEta: String?,     // null w JSON
    @SerializedName("dropoffEta") val dropoffEta: String?,   // null w JSON
    @SerializedName("acceptedByCourier") val acceptedByCourier: Boolean? = null,
    @SerializedName("acceptedAt") val acceptedAt: String?,   // null w JSON
    @SerializedName("completedAt") val completedAt: String?, // null w JSON
    @SerializedName("rejected") val rejected: Boolean? = null,
    @SerializedName("rejectedReason") val rejectedReason: String? = null,
    @SerializedName("rejectedAt") val rejectedAt: String?,   // null w JSON
    @SerializedName("handshakePin") val handshakePin: String? = null,
    @SerializedName("courierLocation") val courierLocation: CourierLocation? = null
) : Serializable

data class CourierLocation(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("updatedAt") val updatedAt: String
) : Serializable

// ====== Enumy pomocnicze dla zewnętrznej dostawy ======

// PAMIĘTAJ: Dodaj Serializable także do OrderProduct i innych, jeśli je przekazujesz!
