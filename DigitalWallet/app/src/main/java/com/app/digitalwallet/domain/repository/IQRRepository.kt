package com.app.digitalwallet.domain.repository

import com.app.digitalwallet.domain.model.DynamicQRData
import com.app.digitalwallet.domain.model.StaticQRData
import com.app.digitalwallet.domain.model.ValidateQRData

interface IQRRepository {
    suspend fun generateDynamicQR(walletId: String, amount: Double, currency: String): Result<DynamicQRData>
    suspend fun validateToken(token: String): Result<ValidateQRData>
    suspend fun getStaticQR(address: String): Result<StaticQRData>
}
