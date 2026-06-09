package com.app.digitalwallet.domain.usecase.auth

import com.app.digitalwallet.domain.repository.IAuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val repository: IAuthRepository
) {
    operator fun invoke() {
        repository.logout()
    }
}
