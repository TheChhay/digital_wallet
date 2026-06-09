package com.app.digitalwallet.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerateQRRequest(
    @SerialName("wallet_id") val walletId: String,
    @SerialName("amount") val amount: Double,
    @SerialName("currency") val currency: String
)

@Serializable
data class GenerateQRResponse(
    @SerialName("qr_image_base64") val qrImageBase64: String,
    @SerialName("token") val token: String,
    @SerialName("expires_at") val expiresAt: Long // Epoch seconds
)

@Serializable
data class ValidateTokenRequest(
    @SerialName("token") val token: String
)

@Serializable
data class ValidateTokenResponse(
    @SerialName("is_valid") val isValid: Boolean,
    @SerialName("message") val message: String?,
    @SerialName("recipient_name") val recipientName: String?,
    @SerialName("recipient_phone") val recipientPhone: String? = null,
    @SerialName("recipient_id") val recipientId: String? = null,
    @SerialName("amount") val amount: Double?,
    @SerialName("currency") val currency: String?
)

@Serializable
data class StaticQRResponse(
    @SerialName("qr_image_base64") val qrImageBase64: String,
    @SerialName("wallet_id") val walletId: String
)
