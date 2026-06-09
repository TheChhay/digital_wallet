package com.app.digitalwallet.data.remote.api

import com.app.digitalwallet.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface QRApiService {
    @POST("qr/generate")
    suspend fun generateDynamicQR(@Body request: GenerateQRRequest): Response<APIResponse<GenerateQRResponse>>

    @POST("qr/validate")
    suspend fun validateToken(@Body request: ValidateTokenRequest): Response<APIResponse<ValidateTokenResponse>>

    @GET("qr/static")
    suspend fun getStaticQR(@Query("address") address: String): Response<APIResponse<StaticQRResponse>>
}
