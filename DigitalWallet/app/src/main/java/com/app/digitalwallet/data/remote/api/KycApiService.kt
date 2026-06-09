package com.app.digitalwallet.data.remote.api

import com.app.digitalwallet.data.remote.dto.APIResponse
import com.app.digitalwallet.data.remote.dto.KYCResponse
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface KycApiService {
    @Multipart
    @POST("me/kyc")
    suspend fun submitKYC(
        @Part("full_name") fullName: okhttp3.RequestBody,
        @Part("dob") dob: okhttp3.RequestBody,
        @Part("address") address: okhttp3.RequestBody,
        @Part idCardImage: MultipartBody.Part,
        @Part selfieImage: MultipartBody.Part
    ): APIResponse<KYCResponse>

    @GET("me/kyc")
    suspend fun getKYC(): APIResponse<KYCResponse>
}
