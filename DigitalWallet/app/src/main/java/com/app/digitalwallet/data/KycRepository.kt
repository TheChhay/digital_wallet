package com.app.digitalwallet.data

import com.app.digitalwallet.api.AuthApiService
import com.app.digitalwallet.api.KycApiService
import com.app.digitalwallet.api.dto.APIResponse
import com.app.digitalwallet.api.dto.KYCResponse
import com.app.digitalwallet.auth.TokenManager
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KycRepository @Inject constructor(
    private val kycService: KycApiService
) {
    suspend fun submitKYC(
        fullName: String,
        dob: String,
        address: String,
        idCardImage: MultipartBody.Part,
        selfieImage: MultipartBody.Part
    ): APIResponse<KYCResponse> {
        val fullNameBody = fullName.toRequestBody(MultipartBody.FORM)
        val dobBody = dob.toRequestBody(MultipartBody.FORM)
        val addressBody = address.toRequestBody(MultipartBody.FORM)

        return kycService.submitKYC(fullNameBody, dobBody, addressBody, idCardImage, selfieImage)
    }

    suspend fun getKYC(): APIResponse<KYCResponse> {
        return kycService.getKYC()
    }
}