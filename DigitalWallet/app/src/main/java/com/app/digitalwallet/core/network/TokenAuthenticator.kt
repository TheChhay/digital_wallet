package com.app.digitalwallet.core.network

import com.app.digitalwallet.core.session.SessionManager
import com.app.digitalwallet.core.session.TokenManager
import com.app.digitalwallet.data.remote.api.AuthApiService
import com.app.digitalwallet.data.remote.dto.RefreshRequest
import com.app.digitalwallet.di.NetworkModule
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val sessionManager: SessionManager,
    private val loggingInterceptor: HttpLoggingInterceptor,
    private val json: Json
) : Authenticator {

    private val contentType = "application/json".toMediaType()

    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = tokenManager.getRefreshToken() ?: run {
            sessionManager.triggerLogout()
            return null
        }

        synchronized(this) {
            val currentToken = tokenManager.getAccessToken()
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            if (requestToken != currentToken && currentToken != null) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val newTokens = runBlocking {
                try {
                    val refreshService = Retrofit.Builder()
                        .baseUrl(NetworkModule.BASE_URL)
                        .addConverterFactory(json.asConverterFactory(contentType))
                        .client(OkHttpClient.Builder().addInterceptor(loggingInterceptor).build())
                        .build()
                        .create(AuthApiService::class.java)

                    val refreshResponse = refreshService.refresh(RefreshRequest(refreshToken))
                    if (refreshResponse.success && refreshResponse.data != null) {
                        refreshResponse.data
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            return if (newTokens != null) {
                tokenManager.saveTokens(newTokens.accessToken, newTokens.refreshToken)
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()
            } else {
                tokenManager.clearTokens()
                sessionManager.triggerLogout()
                null
            }
        }
    }
}
