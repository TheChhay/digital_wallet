package com.app.digitalwallet.domain.model

data class DynamicQRData(
    val qrImageBase64: String,
    val token: String,
    val expiresAt: Long
)

data class ValidateQRData(
    val isValid: Boolean,
    val message: String?,
    val recipientName: String?,
    val recipientPhone: String? = null,
    val recipientId: String? = null,
    val amount: Double?,
    val currency: String?
)

data class StaticQRData(
    val qrImageBase64: String,
    val walletId: String
)
