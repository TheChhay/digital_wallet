package com.app.digitalwallet.data

import com.app.digitalwallet.api.QRApiService
import com.app.digitalwallet.api.dto.GenerateQRRequest
import com.app.digitalwallet.api.dto.GenerateQRResponse
import com.app.digitalwallet.api.dto.StaticQRResponse
import com.app.digitalwallet.api.dto.ValidateTokenRequest
import com.app.digitalwallet.api.dto.ValidateTokenResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QRRepository @Inject constructor(
    private val qrApiService: QRApiService
) {
    suspend fun generateDynamicQR(walletId: String, amount: Double, currency: String): GenerateQRResponse? {
        return try {
            val response = qrApiService.generateDynamicQR(GenerateQRRequest(walletId, amount, currency))
            if (response.isSuccessful) response.body()?.data else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun validateToken(token: String): ValidateTokenResponse? {
        return try {
            val response = qrApiService.validateToken(ValidateTokenRequest(token))
            if (response.isSuccessful) response.body()?.data else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getStaticQR(address: String): StaticQRResponse? {
        return try {
            val response = qrApiService.getStaticQR(address)
            if (response.isSuccessful) response.body()?.data else null
        } catch (e: Exception) {
            null
        }
    }
}
