package com.itsorderkds.ui.settings.print

enum class PrinterType { STANDARD, KITCHEN }

data class PrinterSettings(
    val type: PrinterType,
    val deviceId: String?,
    val profileId: String,
    val encoding: String,
    val codepage: Int?,
    val templateId: String,
    val autoCut: Boolean,
    val enabled: Boolean
)

