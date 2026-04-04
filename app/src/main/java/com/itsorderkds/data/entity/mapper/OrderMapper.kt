package com.itsorderkds.data.entity.mapper

import com.itsorderkds.data.entity.AdditionalFeeEntity
import com.itsorderkds.data.entity.AddonEntity
import com.itsorderkds.data.entity.AddonsGroupEntity
import com.itsorderkds.data.entity.ConsumerEntity
import com.itsorderkds.data.entity.CoordinatesEntity
import com.itsorderkds.data.entity.CourierEntity
import com.itsorderkds.data.entity.ExternalDeliveryEntity
import com.itsorderkds.data.entity.OrderEntity
import com.itsorderkds.data.entity.OrderProductEntity
import com.itsorderkds.data.entity.OrderStatusEntity
import com.itsorderkds.data.entity.ShippingAddressEntity
import com.itsorderkds.data.entity.SourceEntity
import com.itsorderkds.data.model.AdditionalFee
import com.itsorderkds.data.model.Addon
import com.itsorderkds.data.model.AddonsGroup
import com.itsorderkds.data.model.Consumer
import com.itsorderkds.data.model.Coordinates
import com.itsorderkds.data.model.Courier
import com.itsorderkds.data.model.ExternalDelivery
import com.itsorderkds.data.model.Order
import com.itsorderkds.data.model.OrderProduct
import com.itsorderkds.data.model.OrderStatus
import com.itsorderkds.data.model.ShippingAddress
import com.itsorderkds.data.model.SourceOrder
import com.itsorderkds.ui.order.DeliveryStatusEnum
import com.itsorderkds.ui.order.OrderDelivery
import com.itsorderkds.ui.order.OrderStatusEnum
import com.itsorderkds.ui.order.PaymentMethod
import com.itsorderkds.ui.order.PaymentStatus
import com.itsorderkds.ui.order.SourceEnum


object OrderMapper {

    // ----- Helper do enumów -----

    private inline fun <reified T : Enum<T>> String?.toEnumOr(default: T): T =
        this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    // ----- Order → OrderEntity (do Room) -----

    fun fromOrder(order: Order): OrderEntity = OrderEntity(
        orderId = order.orderId,
        status = order.status,
        total = order.total,
        consumer = ConsumerEntity(
            name = order.consumer?.name ?: "",
            email = order.consumer?.email ?: "",
            phone = order.consumer?.phone ?: "",
            countryCode = order.consumer?.countryCode ?: "",
        ),
        orderNumber = order.orderNumber,
        orderStatus = OrderStatusEntity(
            name = order.orderStatus.name,
            sequence = order.orderStatus.sequence,
            slug = order.orderStatus.slug?.let {
                runCatching { OrderStatusEnum.valueOf(it) }.getOrNull()?.name
            } ?: OrderStatusEnum.UNKNOWN.name,
        ),
        paymentMethod = order.paymentMethod?.name ?: PaymentMethod.UNKNOWN.name,
        paymentStatus = order.paymentStatus?.name ?: PaymentStatus.UNKNOWN.name,

        // DATY jako String
        createdAt = order.createdAt.orEmpty(),

        // Bezpieczne mapowanie adresu - zabezpieczenie przed nullami w API
        shippingAddress = order.shippingAddress.let { sa ->
            // jeśli obiekt adresu jest null, użyj pustych wartości
            val street = sa?.street ?: ""
            val city = sa?.city ?: ""
            val numberHome = sa?.numberHome ?: ""
            val numberFlat = sa?.numberFlat ?: ""
            val coords = sa?.coordinates?.let { c ->
                CoordinatesEntity(
                    lat = c.lat,
                    lng = c.lng,
                )
            } ?: CoordinatesEntity(lat = null, lng = null)

            ShippingAddressEntity(
                street = street,
                city = city,
                numberHome = numberHome,
                numberFlat = numberFlat,
                coordinates = coords,
            )
        },
        amount = order.amount,
        pointsAmount = order.pointsAmount ?: 0.0,
        shippingTotal = order.shippingTotal,
        walletBalance = order.walletBalance,

        deliveryInterval = order.deliveryInterval,
        deliveryTime = order.deliveryTime,
        isAsap = order.isAsap,

        source = order.source?.let { src ->
            SourceEntity(
                // BŁĄD BYŁ TUTAJ: src.number może być null, a Entity wymaga String
                number = src.number ?: "",

                name = src.name ?: SourceEnum.UNKNOWN,

                // Warto zabezpieczyć też sourceId na przyszłość
                sourceId = src.sourceId ?: ""
            )
        } ?: SourceEntity("", SourceEnum.UNKNOWN, ""),

        courier = order.courier?.let {
            CourierEntity(
                id = it.id,
                name = it.name,
            )
        },

        note = order.note.orEmpty() ?: "",

        products = order.products.map { product ->
            OrderProductEntity(
                discount = product.discount,
                price = product.price,
                salePrice = product.salePrice,
                name = product.name,
                quantity = product.quantity,
                comment = product.comment.orEmpty(),
                note = product.note,  // List<String>? przekazujemy bezpośrednio
                addonsGroup = product.addonsGroup.map { group ->
                    AddonsGroupEntity(
                        addons = group.addons.map { addon ->
                            AddonEntity(
                                price = addon.price,
                                name = addon.name,
                            )
                        },
                    )
                },
            )
        },

        additionalFees = order.additionalFees.map {
            AdditionalFeeEntity(
                label = it.label,
                amount = it.amount,
            )
        },

        externalDelivery = order.externalDelivery?.let { ed ->
            // Tylko jeśli mamy rzeczywistego kuriera z API, tworzymy obiekt
            // Jeśli courier jest null - zwracamy null (nie ma dostawy zewnętrznej)
            ed.courier?.let { courierEnum ->
                ExternalDeliveryEntity(
                    courier = courierEnum,
                    // status - jeśli nie ma, użyj REQUESTED (bo kurier istnieje)
                    status = ed.status?.let {
                        runCatching { DeliveryStatusEnum.valueOf(it.name) }.getOrNull()
                    } ?: DeliveryStatusEnum.REQUESTED,
                    pickupEta = ed.pickupEta?.toString(),
                    dropoffEta = ed.dropoffEta?.toString(),
                )
            }
        },

        additionalFeeTotal = order.additionalFeeTotal ?: 0.0,
        couponTotalDiscount = order.couponTotalDiscount ?: 0.0,
        deliveryType = order.deliveryType?.name ?: OrderDelivery.UNKNOWN.name,

        type = order.type,
    )

    // ----- OrderEntity → Order (do UI) -----

    fun toOrder(entity: OrderEntity): Order = Order(
        orderId = entity.orderId,
        status = entity.status,
        total = entity.total,
        consumer = Consumer(
            name = entity.consumer.name,
            email = entity.consumer.email,
            phone = entity.consumer.phone,
            countryCode = entity.consumer.countryCode,
        ),
        orderNumber = entity.orderNumber,
        orderStatus = OrderStatus(
            //            id = entity.orderStatus.id,
            name = entity.orderStatus.name,
            sequence = entity.orderStatus.sequence,
            slug = entity.orderStatus.slug?.ifBlank { null } ?: OrderStatusEnum.UNKNOWN.name
            //            systemReserve = entity.orderStatus.systemReserve,
//            status = entity.orderStatus.status,
        ),

        paymentMethod = entity.paymentMethod.toEnumOr(PaymentMethod.UNKNOWN),
        paymentStatus = entity.paymentStatus.toEnumOr(PaymentStatus.UNKNOWN),

        // DATY jako String? (puste zamieniamy na null)
        createdAt = entity.createdAt?.ifBlank { null },

        shippingAddress = ShippingAddress(
            street = entity.shippingAddress.street,
            city = entity.shippingAddress.city,
            numberHome = entity.shippingAddress.numberHome,
            numberFlat = entity.shippingAddress.numberFlat,
            coordinates = entity.shippingAddress.coordinates?.let { c ->
                if (c.lat != null && c.lng != null) Coordinates(lat = c.lat, lng = c.lng) else null
            },
        ),
        amount = entity.amount,
        pointsAmount = entity.pointsAmount,
        shippingTotal = entity.shippingTotal,
        walletBalance = entity.walletBalance,

        deliveryInterval = entity.deliveryInterval?.ifBlank { null },
        deliveryTime = entity.deliveryTime?.ifBlank { null },
        isAsap = entity.isAsap,

        courier = entity.courier?.let {
            Courier(
                id = it.id,
                name = it.name,
            )
        },

        products = entity.products.map { product ->
            OrderProduct(
                discount = product.discount,
                price = product.price,
                comment = product.comment,
                note = product.note,  // List<String>? przekazujemy bezpośrednio
                salePrice = product.salePrice,
                name = product.name,
                quantity = product.quantity,
                addonsGroup = product.addonsGroup.map { group ->
                    AddonsGroup(
                        addons = group.addons.map { addon ->
                            Addon(
                                price = addon.price,
                                name = addon.name,
                            )
                        },
                    )
                },
            )
        },

        note = entity.note,

        source = SourceOrder(
            number = entity.source.number,
            name = entity.source.name,
            sourceId = entity.source.sourceId,
        ),

        deliveryType = entity.deliveryType.toEnumOr(OrderDelivery.UNKNOWN),

        additionalFeeTotal = entity.additionalFeeTotal,

        externalDelivery = entity.externalDelivery?.let { ed ->
            ExternalDelivery(
                courier = ed.courier,
                status = ed.status,
                trackingUrl = null,
                deliveryId = null,
                orderReference = null,
                // pickupEta i dropoffEta - przechowujemy jako stringi, ale w modelu mogą być Date lub null
                pickupEta = ed.pickupEta?.ifBlank { null },
                dropoffEta = ed.dropoffEta?.ifBlank { null },
                acceptedByCourier = null,
                acceptedAt = null,
                completedAt = null,
                rejected = null,
                rejectedReason = null,
                rejectedAt = null,
                handshakePin = null,
                courierLocation = null,
            )
        },

        additionalFees = entity.additionalFees.map {
            AdditionalFee(
                label = it.label,
                amount = it.amount,
            )
        },

        couponTotalDiscount = entity.couponTotalDiscount,

        // Reszta pól z API, których nie trzymasz w lokalnej bazie:
        currency = null,
        isGuest = null,
        usedPoint = null,
        updatedAt = null,
        orderKey = null,
        ip = null,
        orderStatusActivities = emptyList(),
        paymentStatusRank = null,
        taxTotal = null,
        type = entity.type,

    )
}
