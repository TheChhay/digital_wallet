package com.app.digitalwallet.domain.usecase.auth

import com.app.digitalwallet.domain.model.User
import com.app.digitalwallet.domain.repository.IAuthRepository
import javax.inject.Inject

class GetMeUseCase @Inject constructor(
    private val repository: IAuthRepository
) {
    suspend operator fun invoke(): Result<User> {
        return repository.getMe()
    }
}
