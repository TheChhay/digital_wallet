package com.app.digitalwallet.data.repository

import com.app.digitalwallet.core.session.SessionManager
import com.app.digitalwallet.core.session.TokenManager
import com.app.digitalwallet.data.remote.api.AuthApiService
import com.app.digitalwallet.data.remote.dto.*
import com.app.digitalwallet.domain.model.*
import com.app.digitalwallet.domain.repository.IAuthRepository
import okhttp3.MultipartBody
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApiService,
    private val tokenManager: TokenManager,
    private val sessionManager: SessionManager
) : IAuthRepository {

    override suspend fun login(phone: String, password: String): Result<AuthData> {
        return try {
            val response = authApi.login(LoginRequest(phone, password))
            if (response.success && response.data != null) {
                tokenManager.saveTokens(
                    response.data.accessToken,
                    response.data.refreshToken,
                    response.data.user.phone
                )
                Result.success(AuthData(
                    accessToken = response.data.accessToken,
                    refreshToken = response.data.refreshToken,
                    user = response.data.user.toDomain()
                ))
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(phone: String, password: String, firstName: String, lastName: String): Result<AuthData> {
        return try {
            val response = authApi.register(RegisterRequest(phone, password, firstName, lastName))
            if (response.success && response.data != null) {
                tokenManager.saveTokens(
                    response.data.accessToken,
                    response.data.refreshToken,
                    response.data.user.phone
                )
                Result.success(AuthData(
                    accessToken = response.data.accessToken,
                    refreshToken = response.data.refreshToken,
                    user = response.data.user.toDomain()
                ))
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfileImage(image: MultipartBody.Part): Result<User> {
        return try {
            val response = authApi.updateProfileImage(image)
            if (response.success && response.data != null) {
                Result.success(response.data.toDomain())
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMe(): Result<User> {
        return try {
            val response = authApi.getMe()
            if (response.success && response.data != null) {
                Result.success(response.data.toDomain())
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            if (e is HttpException && e.code() == 404) {
                tokenManager.clearTokens()
                sessionManager.triggerLogout()
            }
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(phone: String?, firstName: String?, lastName: String?): Result<User> {
        return try {
            val response = authApi.updateProfile(UserProfileRequest(phone, firstName, lastName))
            if (response.success && response.data != null) {
                Result.success(response.data.toDomain())
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFcmToken(token: String): Result<Unit> {
        return try {
            val response = authApi.updateFcmToken(FcmTokenRequest(token))
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNotification(): Result<Notification> {
        return try {
            val response = authApi.getNotification()
            if (response.success && response.data != null) {
                val data = response.data
                Result.success(Notification(
                    id = data.id,
                    type = data.type,
                    title = data.title,
                    message = data.message,
                    amount = data.amount,
                    relatedTxId = data.relatedTxID,
                    isRead = data.isRead,
                    createdAt = data.createdAt
                ))
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            if (e is HttpException && e.code() == 404) {
                tokenManager.clearTokens()
                sessionManager.triggerLogout()
            }
            Result.failure(e)
        }
    }

    override fun logout() {
        tokenManager.clearTokens()
    }
}
