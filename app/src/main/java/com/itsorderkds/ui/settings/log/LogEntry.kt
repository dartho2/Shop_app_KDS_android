package com.itsorderkds.ui.settings.log

import com.google.gson.annotations.SerializedName

/**
 * Reprezentuje pojedynczy wpis w logu otrzymany z API.
 * Pola są nullowalne, aby aplikacja była odporna na brakujące dane w JSON.
 */
data class LogEntry(
    @SerializedName("timestamp")
    val timestamp: String?,

    @SerializedName("level")
    val level: String?,

    @SerializedName("message")
    val message: String?

    // Jeśli potrzebujesz dostępu do pozostałych, dynamicznych pól,
    // można tu dodać mapę:
    // @JsonAnySetter
    // val additionalProperties: Map<String, Any> = mutableMapOf()
)