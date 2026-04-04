package com.itsorderkds.data.network

interface LanguageProvider {
    fun current(): String // np. "pl", "en", "uk"
}