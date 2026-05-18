package com.app.digitalwallet.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class APIResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val errors: JsonElement? = null
)

@Serializable
data class LoginRequest(
    val phone: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val phone: String,
    val password: String,
    @SerialName("full_name") val fullName: String
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    val user: UserResponse
)

@Serializable
data class UserResponse(
    val id: String,
    val phone: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val role: String,
    val status: String,
    @SerialName("profile_image_url") val profileImageUrl: String? = null
)

@Serializable
data class UserProfileRequest (
    val phone: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null)


