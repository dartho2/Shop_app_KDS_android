package com.itsorderkds.data.responses

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("refresh_token")
    val refreshToken: String,

    @SerializedName("refresh_token_id")
    val refreshTokenId: String,

    @SerializedName("tenantKey")
    val tenantKey: String,

    @SerializedName("role")
    val role: UserRole,

    @SerializedName("sub")
    val sub: String
)


enum class UserRole {
    ADMIN,
    USER,
    MANAGER,
    STAFF,
    COURIER
}