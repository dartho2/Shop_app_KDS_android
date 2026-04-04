package com.itsorderkds.data.network

import okhttp3.ResponseBody

sealed class Resource<out T> {
    companion object {
        fun networkError(message: String? = null, body: ResponseBody? = null) =
            Failure(isNetworkError = true, errorMessage = message, errorBody = body)

        fun appError(message: String? = null, code: Int? = null) =
            Failure(isNetworkError = false, errorMessage = message, errorCode = code)

        fun fromException(e: Throwable) =
            Failure(isNetworkError = false, errorMessage = e.message)
    }
    // ───────────────────────── states ─────────────────────────
    data class Success<out T>(val value: T) : Resource<T>()
    data class Failure(
        val isNetworkError: Boolean,
        val errorCode: Int? = null,
        val errorBody: ResponseBody? = null,
        val errorMessage: String? = null,
        val exception: Throwable? = null
    ) : Resource<Nothing>()

    object Loading : Resource<Nothing>()

    // ─────────────────────── helpers ────────────────────────

    /** Zwraca dane albo `null` – alias dla `getOrNull()` z Kotlin `Result`. */
    fun getData(): T? = (this as? Success<T>)?.value

    /** True, gdy stan to `Success`. */
    val isSuccess get() = this is Success<T>

    /** True, gdy stan to `Failure`. */
    val isFailure get() = this is Failure

    /** True, gdy stan to `Loading`. */
    val isLoading get() = this === Loading

    /** Reaktywna transformacja wyniku – działa tylko na `Success`. */
    inline fun <R> mapSuccess(transform: (T) -> R): Resource<R> = when (this) {
        is Success -> runCatching { transform(value) }
            .fold(
                onSuccess = { Success(it) },
                onFailure = { Failure(false, null, null, it.message) }
            )
        is Failure -> this
        Loading    -> Loading
    }

    /** Składniowy sugar zamiast `when`-a. */
    inline fun fold(
        onSuccess: (T) -> Unit,
        onFailure: (Failure) -> Unit,
        onLoading: () -> Unit = {}
    ) = when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(this)
        Loading    -> onLoading()
    }
}
