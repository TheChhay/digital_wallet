package com.app.digitalwallet.domain.usecase.kyc

import com.app.digitalwallet.domain.model.KycData
import com.app.digitalwallet.domain.repository.IKycRepository
import okhttp3.MultipartBody
import javax.inject.Inject

class SubmitKycUseCase @Inject constructor(
    private val repository: IKycRepository
) {
    suspend operator fun invoke(
        fullName: String,
        dob: String,
        address: String,
        idCardImage: MultipartBody.Part,
        selfieImage: MultipartBody.Part
    ): Result<KycData> {
        return repository.submitKYC(fullName, dob, address, idCardImage, selfieImage)
    }
}
