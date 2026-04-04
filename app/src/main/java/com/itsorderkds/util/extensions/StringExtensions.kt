package com.itsorderkds.util.extensions

import android.net.Uri
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

// Teksty
fun String?.orDash(): String = if (this.isNullOrBlank()) "—" else this
fun String?.orNA(): String = if (this.isNullOrBlank()) "N/A" else this
fun String?.trimSafe(): String = this?.trim().orEmpty()

// URL/URI
fun String?.toHttpUrlSafe(): HttpUrl? = this?.toHttpUrlOrNull()
fun String?.toUriSafe(): Uri? = try { this?.let(Uri::parse) } catch (_: Exception) { null }

// Maskowanie
fun String.maskPhone(): String {
    val digits = filter { it.isDigit() }
    if (digits.length < 4) return "***"
    val tail = digits.takeLast(4)
    return "***-***-$tail"
}

// Formatowanie
fun Int.minutesToLabel(): String = when (this) {
    15 -> "15 min"
    30 -> "30 min"
    60 -> "60 min"
    else -> "$this min"
}

// Walidacje
fun String.isEmailLike(): Boolean =
    Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matches(this)

fun String.isPhoneLike(): Boolean =
    Regex("^\\+?[0-9\\-\\s]{6,}$").matches(this)
