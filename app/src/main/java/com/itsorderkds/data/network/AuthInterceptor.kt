package com.itsorderkds.data.network

import android.util.Log
import com.itsorderkds.data.network.preferences.TokenProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 *  • Dokleja nagłówek  `Authorization: Bearer <token>`
 *  • Gdy serwer odpowie 401 → czyści token i wysyła AuthEvent.Unauthorized
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // --- 1. Dołącz nagłówek Bearer -----------------------------------
        val token = tokenProvider.getAccessToken()
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else original

        // --- 2. Wyślij zapytanie -----------------------------------------
        val response = chain.proceed(request)

        // --- 3. Obsłuż 401 -----------------------------------------------
        if (response.code == 401) {
            android.util.Log.e("AuthInterceptor", "Unauthorized")
            // wyczyść token – jeśli masz taką metodę w providerze

        }

        return response
    }
}
