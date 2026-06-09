package com.app.digitalwallet.data.repository

import com.app.digitalwallet.data.remote.api.QRApiService
import com.app.digitalwallet.data.remote.dto.*
import com.app.digitalwallet.domain.model.DynamicQRData
import com.app.digitalwallet.domain.model.StaticQRData
import com.app.digitalwallet.domain.model.ValidateQRData
import com.app.digitalwallet.domain.repository.IQRRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QRRepositoryImpl @Inject constructor(
    private val qrApiService: QRApiService
) : IQRRepository {

    override suspend fun generateDynamicQR(walletId: String, amount: Double, currency: String): Result<DynamicQRData> {
        return try {
            val response = qrApiService.generateDynamicQR(GenerateQRRequest(walletId, amount, currency))
            val body = response.body()
            if (response.isSuccessful && body != null && body.data != null) {
                val data = body.data
                Result.success(DynamicQRData(
                    qrImageBase64 = data.qrImageBase64,
                    token = data.token,
                    expiresAt = data.expiresAt
                ))
            } else {
                Result.failure(Exception(body?.message ?: "Failed to generate QR"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateToken(token: String): Result<ValidateQRData> {
        return try {
            val response = qrApiService.validateToken(ValidateTokenRequest(token))
            val body = response.body()
            if (response.isSuccessful && body != null && body.data != null) {
                val data = body.data
                Result.success(ValidateQRData(
                    isValid = data.isValid,
                    message = data.message,
                    recipientName = data.recipientName,
                    recipientPhone = data.recipientPhone,
                    recipientId = data.recipientId,
                    amount = data.amount,
                    currency = data.currency
                ))
            } else {
                Result.failure(Exception(body?.message ?: "Invalid token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStaticQR(address: String): Result<StaticQRData> {
        return try {
            val response = qrApiService.getStaticQR(address)
            val body = response.body()
            if (response.isSuccessful && body != null && body.data != null) {
                val data = body.data
                Result.success(StaticQRData(
                    qrImageBase64 = data.qrImageBase64,
                    walletId = data.walletId
                ))
            } else {
                Result.failure(Exception(body?.message ?: "Failed to load static QR"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
