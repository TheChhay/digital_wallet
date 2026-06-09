package com.app.digitalwallet.domain.repository

import com.app.digitalwallet.domain.model.KycData
import okhttp3.MultipartBody

interface IKycRepository {
    suspend fun submitKYC(
        fullName: String,
        dob: String,
        address: String,
        idCardImage: MultipartBody.Part,
        selfieImage: MultipartBody.Part
    ): Result<KycData>

    suspend fun getKYC(): Result<KycData>
}
