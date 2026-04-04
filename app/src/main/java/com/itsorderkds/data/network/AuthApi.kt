package com.itsorderkds.data.network

import com.itsorderkds.data.responses.LoginResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("client/v3/api/mobile/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("client/v3/api/mobile/refresh")
    fun refreshSync(@Body refreshTokenRequest: RefreshTokenRequest): Call<LoginResponse>
}
