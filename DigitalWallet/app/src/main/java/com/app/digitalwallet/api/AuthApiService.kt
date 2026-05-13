package com.app.digitalwallet.api

import com.app.digitalwallet.api.dto.*
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): APIResponse<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): APIResponse<Unit>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): APIResponse<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): APIResponse<Unit>
}
