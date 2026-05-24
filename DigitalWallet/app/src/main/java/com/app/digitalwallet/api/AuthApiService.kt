package com.app.digitalwallet.api

import com.app.digitalwallet.api.dto.*
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): APIResponse<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): APIResponse<Unit>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): APIResponse<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): APIResponse<Unit>

    @Multipart
    @PUT("me/profile-image")
    suspend fun updateProfileImage(@Part image: MultipartBody.Part): APIResponse<UserResponse>

    @GET("me")
    suspend fun getMe(): APIResponse<UserResponse>

    @PUT("me")
    suspend fun updateProfile(@Body request: UserProfileRequest): APIResponse<UserResponse>

    @POST("me/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): APIResponse<Unit>

    @GET("me/notifications")
    suspend fun getNotification(): APIResponse<NotificationResponse>
}
