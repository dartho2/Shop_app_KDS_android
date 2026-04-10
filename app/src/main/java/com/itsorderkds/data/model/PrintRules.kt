package com.itsorderkds.data.model

import kotlinx.serialization.Serializable

/**
 * Pojedyncza reguła drukowania: po przejściu na [status]
 * wydrukuj na drukarkach [printerIds].
 *
 * Jeśli [printerIds] jest puste → drukuj na WSZYSTKICH włączonych.
 */
@Serializable
data class PrintStatusRule(
    val id: String,                             // UUID reguły
    val enabled: Boolean = true,                // czy reguła aktywna
    val status: KdsTicketState,                 // trigger – na jaki status
    val printerIds: List<String> = emptyList(), // puste = wszystkie włączone
    val templateOverrideId: String? = null,     // null = szablon przypisany do drukarki
)


