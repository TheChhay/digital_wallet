package com.app.digitalwallet.domain.usecase.auth

import com.app.digitalwallet.domain.repository.IAuthRepository
import javax.inject.Inject

class UpdateFcmTokenUseCase @Inject constructor(
    private val repository: IAuthRepository
) {
    suspend operator fun invoke(token: String): Result<Unit> {
        return repository.updateFcmToken(token)
    }
}
