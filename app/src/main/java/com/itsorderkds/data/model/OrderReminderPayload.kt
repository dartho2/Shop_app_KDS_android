package com.itsorderkds.data.model

data class OrderReminderPayload(
    val orderId: String,
    val reminderNumber: Int
)