package com.app.digitalwallet.domain.usecase.auth

import com.app.digitalwallet.domain.model.AuthData
import com.app.digitalwallet.domain.repository.IAuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: IAuthRepository
) {
    suspend operator fun invoke(phone: String, password: String, firstName: String, lastName: String): Result<AuthData> {
        return repository.register(phone, password, firstName, lastName)
    }
}
