package com.app.digitalwallet.data

import android.content.Context
import com.app.digitalwallet.api.AuthApiService
import com.app.digitalwallet.api.RetrofitClient
import com.app.digitalwallet.api.dto.*
import com.app.digitalwallet.auth.TokenManager

class AuthRepository(
    private val authApi: AuthApiService = RetrofitClient.authApi,
    context: Context
) {
    private val tokenManager = TokenManager.getInstance(context)

    suspend fun login(request: LoginRequest): APIResponse<AuthResponse> {
        val response = authApi.login(request)
        if (response.success && response.data != null) {
            tokenManager.saveTokens(
                response.data.accessToken,
                response.data.refreshToken
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
