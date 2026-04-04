package com.itsorderkds.data.entity

import java.io.Serializable

data class OrderStatusEntity(
//    val id: String,
    val name: String,
    val sequence: Int,
    val slug: String,
//    val systemReserve: Boolean,
//    val status: Boolean
) : Serializable
