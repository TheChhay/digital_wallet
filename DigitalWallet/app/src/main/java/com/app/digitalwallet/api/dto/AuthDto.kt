package com.app.digitalwallet.api.dto

import com.google.gson.annotations.SerializedName

data class APIResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val errors: Any? = null
)

data class LoginRequest(
    val phone: String,
    val password: String
)

data class RegisterRequest(
    val phone: String,
    val password: String,
    @SerializedName("full_name") val fullName: String
)

data class RefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    val user: UserResponse
)

data class UserResponse(
    val id: String,
    val phone: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    val role: String,
    val status: String,
    @SerializedName("profile_image_url") val profileImageUrl: String? = null
)
