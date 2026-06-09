package com.app.digitalwallet.domain.usecase.kyc

import com.app.digitalwallet.domain.model.KycData
import com.app.digitalwallet.domain.repository.IKycRepository
import javax.inject.Inject

class GetKycUseCase @Inject constructor(
    private val repository: IKycRepository
) {
    suspend operator fun invoke(): Result<KycData> {
        return repository.getKYC()
    }
}
