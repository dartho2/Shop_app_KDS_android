package com.itsorderkds.data.core.lang

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LanguageStore {
    private val _code = MutableStateFlow<String?>(null)
    val code: StateFlow<String?> = _code

    fun set(code: String?) { _code.value = code }
    fun get(): String? = _code.value
}