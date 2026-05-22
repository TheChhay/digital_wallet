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
        return try {
            val fullNameBody = fullName.toRequestBody(MultipartBody.FORM)
            val dobBody = dob.toRequestBody(MultipartBody.FORM)
            val addressBody = address.toRequestBody(MultipartBody.FORM)

            kycService.submitKYC(fullNameBody, dobBody, addressBody, idCardImage, selfieImage)
        } catch (e: Exception) {
            APIResponse(success = false, message = e.localizedMessage ?: "KYC submission failed")
        }
    }

    suspend fun getKYC(): APIResponse<KYCResponse> {
        return try {
            kycService.getKYC()
        } catch (e: Exception) {
            APIResponse(success = false, message = e.localizedMessage ?: "Failed to fetch KYC status")
        }
    }
}