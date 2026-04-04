package com.itsorderkds.data.util

import android.util.Log
import com.itsorderkds.data.network.AuthApi
import com.itsorderkds.data.network.RefreshTokenRequest
import com.itsorderkds.data.network.preferences.TokenProvider
import kotlinx.coroutines.runBlocking
import java.io.IOException

object TokenRefreshLock {
    val lock = Any()
}

object TokenRefreshHelper {
    @Volatile
    private var refreshInProgress = false

    fun refreshBlocking(tokenProvider: TokenProvider, authApi: AuthApi): RefreshResult {
        synchronized(TokenRefreshLock.lock) {
            Log.d("AuthRefresh", "🔄  START refresh (thread=${Thread.currentThread().name})")
            if (refreshInProgress) {
                repeat(10) {
                    Thread.sleep(100)
                    val nowToken = runBlocking { tokenProvider.getAccessToken() }
                    if (!nowToken.isNullOrBlank()) return RefreshResult.Success(nowToken)
                }
                return RefreshResult.Failed
            }

            refreshInProgress = true
            try {
                val refreshToken = runBlocking { tokenProvider.getRefreshToken() }
                val refreshTokenId = runBlocking { tokenProvider.getRefreshTokenId() }

                if (refreshToken.isNullOrBlank() || refreshTokenId.isNullOrBlank()) return RefreshResult.Failed

                val refreshResp = try {
                    Log.e("AuthRefresh", "🔄  REFRESH (thread=${Thread.currentThread().name})")
                    authApi.refreshSync(RefreshTokenRequest(refreshToken, refreshTokenId)).execute()
                } catch (_: IOException) {
                    // Błąd sieciowy (brak neta) -> Zwykły Failed (można ponowić później)
                    return RefreshResult.Failed
                }

                // --- TU JEST KLUCZOWA ZMIANA ---
                if (refreshResp.code() == 401) {
                    Log.e("AuthRefresh", "⛔ Session expired (401 on refresh)")
                    return RefreshResult.SessionExpired
                }
                // -------------------------------

                if (!refreshResp.isSuccessful) return RefreshResult.Failed
                val body = refreshResp.body() ?: return RefreshResult.Failed

                runBlocking {
                    tokenProvider.saveTokens(
                        body.accessToken,
                        body.refreshToken,
                        body.refreshTokenId,
                        body.tenantKey,
                        body.role,
                        body.sub
                    )
                }
                return RefreshResult.Success(body.accessToken)
            } finally {
                refreshInProgress = false
            }
        }
    }

    sealed class RefreshResult {
        data class Success(val newAccessToken: String): RefreshResult()
        object Failed: RefreshResult()          // Błąd techniczny/sieci
        object SessionExpired: RefreshResult()  // Krytyczny błąd autoryzacji (401)
    }
}