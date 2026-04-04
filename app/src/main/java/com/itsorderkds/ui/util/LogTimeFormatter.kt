package com.itsorderkds.ui.util


import java.time.*
import java.time.format.DateTimeFormatter

object LogTimeFormatter {
    private val deviceZone: ZoneId = ZoneId.systemDefault()
    private val outFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /** Sformatuj timestamp (UTC) do czasu urządzenia. */
    fun formatDevice(utcString: String?): String {
        val instant = parseToInstant(utcString) ?: return "Brak czasu"
        return instant.atZone(deviceZone).format(outFormatter)
    }

    /** Spróbuj z kilku wzorców -> Instant (UTC). */
    fun parseToInstant(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return runCatching { Instant.parse(raw) }.getOrElse {
            runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrElse {
                runCatching {
                    // fallback na stary format bez strefy — traktujemy jako UTC
                    LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .toInstant(ZoneOffset.UTC)
                }.getOrNull()
            }
        }
    }
}
