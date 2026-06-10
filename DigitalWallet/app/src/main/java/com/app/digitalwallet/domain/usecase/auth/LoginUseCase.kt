package com.app.digitalwallet.domain.usecase.auth

import com.app.digitalwallet.domain.model.AuthData
import com.app.digitalwallet.domain.repository.IAuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: IAuthRepository
) {
    //when use operator with invoke we can use loginUseCase() instead of loginUseCase.invoke()
    suspend operator fun invoke(phone: String, password: String): Result<AuthData> {
        return repository.login(phone, password)
    }
}
