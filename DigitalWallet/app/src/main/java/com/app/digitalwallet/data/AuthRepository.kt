package com.app.digitalwallet.data

import com.app.digitalwallet.api.AuthApiService
import com.app.digitalwallet.api.dto.*
import com.app.digitalwallet.auth.TokenManager
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApiService,
    private val tokenManager: TokenManager
) {

    suspend fun login(request: LoginRequest): APIResponse<AuthResponse> {
        return try {
            val response = authApi.login(request)
            if (response.success && response.data != null) {
                tokenManager.saveTokens(
                    response.data.accessToken,
                    response.data.refreshToken,
                    response.data.user.phone
                )
            }
            response
        } catch (e: Exception) {
            APIResponse(success = false, message = e.localizedMessage ?: "Login failed")
        }
    }

    suspend fun register(request: RegisterRequest): APIResponse<Unit> {
        return try {
            authApi.register(request)
        } catch (e: Exception) {
            APIResponse(success = false, message = e.localizedMessage ?: "Registration failed")
        }
    }

    suspend fun updateProfileImage(image: MultipartBody.Part): APIResponse<UserResponse> {
        return try {
            authApi.updateProfileImage(image)
        } catch (e: Exception) {
            APIResponse(success = false, message = e.localizedMessage ?: "Failed to update image")
        }
    }

    suspend fun getMe(): APIResponse<UserResponse> {
        return try {
            authApi.getMe()
        } catch (e: Exception) {
            APIResponse(success = false, message = e.localizedMessage ?: "Failed to fetch profile")
        }
    }

    suspend fun updateProfile(request: UserProfileRequest): APIResponse<UserResponse> {
        return try {
            authApi.updateProfile(request)
        } catch (e: Exception) {
            APIResponse(success = false, message = e.localizedMessage ?: "Update failed")
        }
    }

    suspend fun updateFcmToken(token: String): APIResponse<Unit> {
        return try {
            authApi.updateFcmToken(FcmTokenRequest(token))
        } catch (e: Exception) {
            APIResponse(success = false, message = e.localizedMessage ?: "Failed to update token")
        }
    }


    fun logout() {
        tokenManager.clearTokens()
    }
}
