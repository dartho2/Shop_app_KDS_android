package com.itsorderkds.data.interfaces

import com.itsorderkds.data.model.Order

interface OrderActionListener {
    fun onAccept(order: Order)
    fun onSend(order: Order)
    fun onDetails(order: Order)
}