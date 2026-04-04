package com.itsorderkds.data.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.itsorderkds.ui.order.CourierEnum
import com.itsorderkds.ui.order.DeliveryStatusEnum
import com.itsorderkds.ui.order.SourceEnum
import com.itsorderkds.ui.order.TypeOrderEnum
import java.io.Serializable

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey
    val orderId: String,

    val status: Boolean,
    val total: Double,

    @Embedded(prefix = "consumer_")
    val consumer: ConsumerEntity,

    val orderNumber: String,

    @Embedded(prefix = "order_status_")
    val orderStatus: OrderStatusEntity,

    val paymentMethod: String,
    val paymentStatus: String,

    // było String – musi być nullable
    val createdAt: String?,              // <--

    @Embedded(prefix = "shipping_")
    val shippingAddress: ShippingAddressEntity,

    val amount: Double,
    val pointsAmount: Double,
    val shippingTotal: Double,
    val walletBalance: Double,

    // było String – też nullable
    val deliveryInterval: String?,       // <--

    val deliveryTime: String?,           // już było nullable

    val isAsap: Boolean,

    @Embedded(prefix = "source_")
    val source: SourceEntity,

    @Embedded(prefix = "courier_")
    val courier: CourierEntity?,

    val note: String = "",

    @ColumnInfo(name = "products")
    val products: List<OrderProductEntity>,

    @ColumnInfo(name = "additionalFees")
    val additionalFees: List<AdditionalFeeEntity>,

    @Embedded(prefix = "external_")
    val externalDelivery: ExternalDeliveryEntity?,

    val additionalFeeTotal: Double,
    val couponTotalDiscount: Double,
    val deliveryType: String,

    // Typ zamówienia: PREORDER, ASAP, SCHEDULE, UNKNOWN
    val type: TypeOrderEnum? = null,
) : Serializable

data class ExternalDeliveryEntity(
    val courier: CourierEnum,
    val status: DeliveryStatusEnum,          // <--
    val pickupEta: String? = null,       // <--
    val dropoffEta: String? = null,       // <--
) : Serializable

data class AdditionalFeeEntity(
    val label: String,
    val amount: Double,
) : Serializable

data class SourceEntity(
    val number: String,
    val name: SourceEnum,
    val sourceId: String,
) : Serializable
