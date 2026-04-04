package com.itsorderkds.data.model

data class SectionedOrders(
    val section: String,
    val orders: List<Order>
)