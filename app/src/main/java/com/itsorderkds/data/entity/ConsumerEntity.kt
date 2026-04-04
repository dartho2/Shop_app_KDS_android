package com.itsorderkds.data.entity

import java.io.Serializable

data class ConsumerEntity(
    val name: String,
    val email: String,
    val phone: String,
    val countryCode: String
) : Serializable
