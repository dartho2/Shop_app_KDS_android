package com.itsorderkds.data.network

import com.itsorderkds.util.AppPrefs
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Ten Interceptor dynamicznie podmienia bazowy adres URL dla każdego wychodzącego zapytania.
 * Pobiera najnowszy URL z AppPrefs, co pozwala na jego zmianę w trakcie działania aplikacji
 * bez potrzeby jej restartowania.
 */
class BaseUrlInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Pobierz najnowszy base URL z AppPrefs.
        // Ta metoda jest wywoływana przy każdym zapytaniu, więc URL zawsze będzie aktualny.
        val newBaseUrlString = AppPrefs.getBaseUrl()
        val newBaseUrl = newBaseUrlString.toHttpUrl()

        // Zbuduj nowy URL, zachowując ścieżkę i parametry z oryginalnego zapytania,
        // ale podmieniając hosta, schemat i port na te z nowego base URL.
        val newUrl = originalRequest.url.newBuilder()
            .scheme(newBaseUrl.scheme)
            .host(newBaseUrl.host)
            .port(newBaseUrl.port)
            .build()

        // Zbuduj nowe zapytanie ze zmodyfikowanym URL-em.
        val request = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(request)
    }
}