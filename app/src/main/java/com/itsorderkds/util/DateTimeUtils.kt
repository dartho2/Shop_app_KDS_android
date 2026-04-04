package com.itsorderkds.util

import timber.log.Timber
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    /**
     * Parsuje różne ISO-formaty dat/czasów zwracanych przez API do ZonedDateTime w strefie systemowej.
     * Zwraca null jeśli parsowanie nie powiodło się.
     */
    fun parseToZonedDateTime(dateString: String?): ZonedDateTime? {
        if (dateString.isNullOrBlank()) return null
        val s = dateString.trim()

        // 1. ISO_ZONED_DATE_TIME
        runCatching {
            return ZonedDateTime.parse(s, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        }

        // 2. ISO_OFFSET_DATE_TIME
        runCatching {
            val odt = OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            return odt.toZonedDateTime()
        }

        // 3. common fallback patterns
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
        )
        for (p in patterns) {
            runCatching {
                val fmt = DateTimeFormatter.ofPattern(p)
                val z = ZonedDateTime.parse(s, fmt)
                return z
            }
            runCatching {
                val odt = OffsetDateTime.parse(s, DateTimeFormatter.ofPattern(p))
                return odt.toZonedDateTime()
            }
        }

        // 4. Instant.parse (z-brakiem offsetu)
        runCatching {
            val instant = Instant.parse(s)
            return instant.atZone(ZoneId.systemDefault())
        }

        // 5. try generic OffsetDateTime
        runCatching {
            val odt = OffsetDateTime.parse(s)
            return odt.toZonedDateTime()
        }

        Timber.w("DateTimeUtils.parseToZonedDateTime: failed to parse date: %s", s)
        return null
    }

    fun formatToLocalShort(zdt: ZonedDateTime?): String {
        if (zdt == null) return "-"
        return try {
            zdt.withZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault()))
        } catch (e: Exception) {
            Timber.e(e, "DateTimeUtils.formatToLocalShort: format failed")
            "-"
        }
    }

    fun formatStringToLocalShort(dateString: String?): String {
        val z = parseToZonedDateTime(dateString)
        return formatToLocalShort(z)
    }
}

