package com.itsorderkds.data.repository
import android.content.Context
import com.itsorderkds.data.network.AuthApi
import com.itsorderkds.data.network.LoginRequest
import com.itsorderkds.data.network.preferences.TokenProvider
import com.itsorderkds.data.responses.UserRole
import com.itsorderkds.ui.theme.GlobalMessageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenProvider: TokenProvider,
    @ApplicationContext private val context: Context,
    messageManager: GlobalMessageManager
) : BaseRepository(messageManager) {

    suspend fun login(email: String, password: String) = safeApiCall {
        api.login(LoginRequest(email, password))
    }

    suspend fun saveAuthToken(
        access: String,
        refresh: String,
        refreshId: String,
        tenantKey: String,
        role: UserRole?,
        userId: String?
    ) {
        tokenProvider.saveTokens(access, refresh, refreshId, tenantKey, role, userId )
        // możesz tu użyć context do DataStore jeśli chcesz, ale polecam wyciągać przez TokenProvider
    }
}
