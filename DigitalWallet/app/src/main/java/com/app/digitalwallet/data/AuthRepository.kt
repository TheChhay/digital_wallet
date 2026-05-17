package com.app.digitalwallet.data

import android.content.Context
import com.app.digitalwallet.api.AuthApiService
import com.app.digitalwallet.api.dto.*
import com.app.digitalwallet.auth.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApiService,
    private val tokenManager: TokenManager
) {

    suspend fun login(request: LoginRequest): APIResponse<AuthResponse> {
        val response = authApi.login(request)
        if (response.success && response.data != null) {
            tokenManager.saveTokens(
                response.data.accessToken,
                response.data.refreshToken,
                response.data.user.phone
            )
        }
        return response
    }

    suspend fun register(request: RegisterRequest): APIResponse<Unit> {
        return authApi.register(request)
    }

    fun logout() {
        tokenManager.clearTokens()
    }

    fun isLoggedIn(): Boolean {
        return tokenManager.getAccessToken() != null
    }
}
