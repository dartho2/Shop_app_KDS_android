package com.itsorderkds.data.network

import android.util.Log
import com.itsorderkds.data.network.preferences.TokenProvider
import com.itsorderkds.data.util.TokenRefreshHelper
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenProvider: TokenProvider,
    private val authApi: AuthApi
) : Authenticator {

    companion object { private const val TAG = "TokenAuth" }

    override fun authenticate(route: Route?, response: Response): Request? {

        /* ① unikamy nieskończonej pętli */
        if (response.wasRetried()) return null

        /* ② jeśli ktoś już odświeżył token – użyj go */
        val cachedToken   = tokenProvider.getAccessToken()
        val requestHeader = response.request.header("Authorization")
        if (!cachedToken.isNullOrBlank() && requestHeader != "Bearer $cachedToken") {
            Timber.d(TAG, "Re-using fresh token from cache")
            return response.request.withAuth(cachedToken)
        }

        /* ③ spróbuj odświeżyć */
        return when (val r = TokenRefreshHelper.refreshBlocking(tokenProvider, authApi)) {
            is TokenRefreshHelper.RefreshResult.Success -> {
                Timber.d(TAG, "Token refreshed")
                response.request.withAuth(r.newAccessToken)
            }
            else -> {
                Timber.d(TAG, "Refresh failed → passing 401 up")
                null
            }
        }
    }

    /* ---------- helpers ---------- */

    /** maks. 1 retry – liczymy łańcuch priorResponse */
    private fun Response.wasRetried(): Boolean = retryCount() > 0
    private fun Response.retryCount(): Int {
        var count = 0; var p = priorResponse
        while (p != null) { count++; p = p.priorResponse }
        return count
    }

    /** dokleja Bearera w sposób nie-modyfikujący reszty nagłówków */
    private fun Request.withAuth(token: String): Request =
        newBuilder().header("Authorization", "Bearer $token").build()
}
