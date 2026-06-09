package com.app.digitalwallet.domain.usecase.auth

import com.app.digitalwallet.domain.model.User
import com.app.digitalwallet.domain.repository.IAuthRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val repository: IAuthRepository
) {
    suspend operator fun invoke(phone: String?, firstName: String?, lastName: String?): Result<User> {
        return repository.updateProfile(phone, firstName, lastName)
    }
}
