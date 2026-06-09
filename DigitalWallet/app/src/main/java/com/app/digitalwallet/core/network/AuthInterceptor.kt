package com.app.digitalwallet.core.network

import com.app.digitalwallet.core.session.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()
        
        if (request.url.host == "10.0.2.2") {
            requestBuilder.header("Host", "localhost:${request.url.port}")
        }

        val token = tokenManager.getAccessToken()
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
