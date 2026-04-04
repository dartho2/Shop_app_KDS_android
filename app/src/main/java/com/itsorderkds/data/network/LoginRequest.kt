package com.itsorderkds.data.network

data class LoginRequest(
    val email: String,
    val password: String
)

data class RefreshTokenRequest(
    val refresh_token: String,
    val refresh_token_id: String,
)