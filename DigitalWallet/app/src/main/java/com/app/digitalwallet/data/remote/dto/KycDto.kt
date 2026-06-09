package com.app.digitalwallet.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody

@Serializable
data class KYCResponse(
    val submitted: Boolean,
    val status: String,
    @SerialName("full_name") val fullName: String? = null,
    val dob: String? = null,
    val address: String? = null,
    @SerialName("id_card_image_url") val idCardImageUrl: String? = null,
    @SerialName("selfie_image_url") val selfieImageUrl: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null
)

data class KYCRequest(
    val fullName: String,
    val dob: String,
    val address: String,
    val idCardImage: MultipartBody.Part,
    val selfieImage: MultipartBody.Part
)
