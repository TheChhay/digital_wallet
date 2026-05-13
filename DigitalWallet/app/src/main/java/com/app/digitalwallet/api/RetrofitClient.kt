package com.app.digitalwallet.api

import android.content.Context
import com.app.digitalwallet.api.dto.RefreshRequest
import com.app.digitalwallet.auth.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/api/v1/"
    private var tokenManager: TokenManager? = null

    /**
     * Initialize with Context to enable Secure Storage
     */
    fun init(context: Context) {
        tokenManager = TokenManager.getInstance(context)
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = tokenManager?.getAccessToken()

        val requestBuilder = originalRequest.newBuilder()
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val authenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            val refreshToken = tokenManager?.getRefreshToken() ?: return null

            synchronized(this) {
                val currentToken = tokenManager?.getAccessToken()
                val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

                // If the token in the request is already different from our current token,
                // someone else refreshed it already.
                if (requestToken != currentToken && currentToken != null) {
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                // Need to refresh
                val newTokens = runBlocking {
                    try {
                        // Separate retrofit for refresh to avoid loops
                        val refreshService = Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(OkHttpClient.Builder().addInterceptor(logging).build())
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
                    tokenManager?.saveTokens(newTokens.accessToken, newTokens.refreshToken)
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.accessToken}")
                        .build()
                } else {
                    // Logout on refresh failure
                    tokenManager?.clearTokens()
                    null
                }
            }
        }
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .authenticator(authenticator)
        .addInterceptor(logging)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
    }

    /**
     * Creates a Retrofit service implementation for the given [serviceClass].
     * Professional way to create services dynamically.
     */
    fun <T> create(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }

    /**
     * A more idiomatic Kotlin way to create services using reified types.
     * Use this in your Repositories to create services without adding them to this file.
     * Usage: val api = RetrofitClient.createService<MyApiService>()
     */
    inline fun <reified T> createService(): T {
        return create(T::class.java)
    }

    // Explicitly named services for clarity across the project
    val walletApi: WalletApiService by lazy { createService<WalletApiService>() }
    val authApi: AuthApiService by lazy { createService<AuthApiService>() }
}
