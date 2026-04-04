package com.itsorderkds.data.network

import okhttp3.Interceptor
import okhttp3.Response

class LanguageInterceptor(
    private val langProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Jeżeli już ręcznie ustawisz nagłówek w konkretnym wywołaniu,
        // to INTERCEPTOR go nie nadpisze (szanujemy request).
        if (original.header("Accept-Language") != null) {
            return chain.proceed(original)
        }

        val lang = langProvider()?.takeIf { it.isNotBlank() }
            ?: java.util.Locale.getDefault().toLanguageTag() // np. "pl-PL"

        val newReq = original.newBuilder()
            .header("Accept-Language", lang)
            .build()

        return chain.proceed(newReq)
    }
}