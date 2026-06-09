package com.app.digitalwallet.data.remote.dto

import com.app.digitalwallet.domain.model.User
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
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
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

fun UserResponse.toDomain() = User(
    id = id,
    phone = phone,
    firstName = firstName,
    lastName = lastName,
    role = role,
    status = status,
    profileImageUrl = profileImageUrl
)

@Serializable
data class UserProfileRequest (
    val phone: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null)

@Serializable
data class FcmTokenRequest(
    @SerialName("fcm_token") val fcmToken: String
)

@Serializable
data class NotificationResponse(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val amount: Float,
    @SerialName("related_tx_id") val relatedTxID: String,
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("created_at") val createdAt: String

)
