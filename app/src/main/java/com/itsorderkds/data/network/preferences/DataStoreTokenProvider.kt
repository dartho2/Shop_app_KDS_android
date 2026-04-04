package com.itsorderkds.data.network.preferences

import com.itsorderkds.data.preferences.AppPreferencesManager
import com.itsorderkds.data.responses.UserRole
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token provider interface dla HTTP interceptors.
 */
interface TokenProvider {
    fun getAccessToken(): String?
    val accessTokenFlow: Flow<String?>
    suspend fun getRefreshToken(): String?
    suspend fun getRefreshTokenId(): String?
    suspend fun getTenantKey(): String?
    suspend fun getRole(): UserRole?
    suspend fun getUserID(): String?
    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        refreshTokenId: String,
        tenantKey: String,
        role: UserRole?,
        userId: String?
    )
    suspend fun clearAccessTokens()
}

/**
 * Implementacja TokenProvider używająca AppPreferencesManager.
 * Adapter pattern dla kompatybilności wstecznej.
 *
 * @deprecated Używaj bezpośrednio AppPreferencesManager
 */
@Singleton
class DataStoreTokenProvider @Inject constructor(
    private val preferencesManager: AppPreferencesManager
) : TokenProvider {

    override fun getAccessToken(): String? = preferencesManager.getAccessToken()

    override val accessTokenFlow: Flow<String?> = preferencesManager.accessTokenFlow

    override suspend fun getRefreshToken(): String? = preferencesManager.getRefreshToken()

    override suspend fun getRefreshTokenId(): String? = preferencesManager.getRefreshTokenId()

    override suspend fun getTenantKey(): String? = preferencesManager.getTenantKey()

    override suspend fun getRole(): UserRole? = preferencesManager.getUserRole()

    override suspend fun getUserID(): String? = preferencesManager.getUserId()

    override suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        refreshTokenId: String,
        tenantKey: String,
        role: UserRole?,
        userId: String?
    ) {
        preferencesManager.saveAuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            refreshTokenId = refreshTokenId,
            tenantKey = tenantKey,
            role = role,
            userId = userId
        )
    }

    override suspend fun clearAccessTokens() {
        preferencesManager.clearAuthTokens()
    }
}
