package com.app.digitalwallet.domain.model

data class KycData(
    val submitted: Boolean,
    val status: KycStatus,
    val fullName: String? = null,
    val dob: String? = null,
    val address: String? = null,
    val idCardImageUrl: String? = null,
    val selfieImageUrl: String? = null,
    val rejectionReason: String? = null,
    val submittedAt: String? = null,
    val updatedAt: String? = null,
    val reviewedAt: String? = null
)
