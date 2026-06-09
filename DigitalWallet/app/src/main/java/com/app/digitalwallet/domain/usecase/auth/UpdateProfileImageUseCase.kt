package com.app.digitalwallet.domain.usecase.auth

import com.app.digitalwallet.domain.model.User
import com.app.digitalwallet.domain.repository.IAuthRepository
import okhttp3.MultipartBody
import javax.inject.Inject

class UpdateProfileImageUseCase @Inject constructor(
    private val repository: IAuthRepository
) {
    suspend operator fun invoke(image: MultipartBody.Part): Result<User> {
        return repository.updateProfileImage(image)
    }
}
