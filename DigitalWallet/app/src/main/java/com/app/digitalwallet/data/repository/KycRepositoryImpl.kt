package com.app.digitalwallet.data.repository

import com.app.digitalwallet.data.remote.api.KycApiService
import com.app.digitalwallet.data.remote.dto.KYCResponse
import com.app.digitalwallet.domain.model.KycData
import com.app.digitalwallet.domain.model.KycStatus
import com.app.digitalwallet.domain.repository.IKycRepository
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KycRepositoryImpl @Inject constructor(
    private val kycService: KycApiService
) : IKycRepository {

    override suspend fun submitKYC(
        fullName: String,
        dob: String,
        address: String,
        idCardImage: MultipartBody.Part,
        selfieImage: MultipartBody.Part
    ): Result<KycData> {
        return try {
            val fullNameBody = fullName.toRequestBody(MultipartBody.FORM)
            val dobBody = dob.toRequestBody(MultipartBody.FORM)
            val addressBody = address.toRequestBody(MultipartBody.FORM)

            val response = kycService.submitKYC(fullNameBody, dobBody, addressBody, idCardImage, selfieImage)
            if (response.success && response.data != null) {
                Result.success(response.data.toDomain())
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getKYC(): Result<KycData> {
        return try {
            val response = kycService.getKYC()
            if (response.success && response.data != null) {
                Result.success(response.data.toDomain())
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun KYCResponse.toDomain() = KycData(
        submitted = submitted,
        status = KycStatus.from(status),
        fullName = fullName,
        dob = dob,
        address = address,
        idCardImageUrl = idCardImageUrl,
        selfieImageUrl = selfieImageUrl,
        rejectionReason = rejectionReason,
        submittedAt = submittedAt,
        updatedAt = updatedAt,
        reviewedAt = reviewedAt
    )
}
