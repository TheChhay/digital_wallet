package com.app.digitalwallet.di

import android.content.Context
import com.app.digitalwallet.api.AuthApiService
import com.app.digitalwallet.api.KycApiService
import com.app.digitalwallet.api.QRApiService
import com.app.digitalwallet.api.WalletApiService
import com.app.digitalwallet.api.dto.RefreshRequest
import com.app.digitalwallet.auth.SessionManager
import com.app.digitalwallet.auth.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Base host URL for images and other non-API resources
     */
    const val BASE_HOST = "http://192.168.1.18:8080"
    //emulator
//    const val BASE_HOST = "http://10.0.2.2:8080"
    private const val BASE_URL = "$BASE_HOST/api/v1/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val contentType = "application/json".toMediaType()

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val token = tokenManager.getAccessToken()
            
            android.util.Log.d("NetworkModule", "Interceptor: Adding token to ${originalRequest.url}: ${token?.take(10)}...")

            val requestBuilder = originalRequest.newBuilder()
            if (token != null) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            chain.proceed(requestBuilder.build())
        }
    }

    @Provides
    @Singleton
    fun provideAuthenticator(
        tokenManager: TokenManager, 
        loggingInterceptor: HttpLoggingInterceptor,
        sessionManager: SessionManager
    ): Authenticator {
        return object : Authenticator {
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
                                .baseUrl(BASE_URL)
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
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor,
        authenticator: Authenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideQRApiService(retrofit: Retrofit): QRApiService {
        return retrofit.create(QRApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideWalletApiService(retrofit: Retrofit): WalletApiService {
        return retrofit.create(WalletApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideKycApiService(retrofit: Retrofit): KycApiService {
        return retrofit.create(KycApiService::class.java)
    }
}
