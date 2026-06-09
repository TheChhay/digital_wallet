package com.app.digitalwallet.domain.repository

import com.app.digitalwallet.domain.model.AuthData
import com.app.digitalwallet.domain.model.Notification
import com.app.digitalwallet.domain.model.User
import okhttp3.MultipartBody

interface IAuthRepository {
    suspend fun login(phone: String, password: String): Result<AuthData>
    suspend fun register(phone: String, password: String, firstName: String, lastName: String): Result<AuthData>
    suspend fun updateProfileImage(image: MultipartBody.Part): Result<User>
    suspend fun getMe(): Result<User>
    suspend fun updateProfile(phone: String?, firstName: String?, lastName: String?): Result<User>
    suspend fun updateFcmToken(token: String): Result<Unit>
    suspend fun getNotification(): Result<Notification>
    fun logout()
}
