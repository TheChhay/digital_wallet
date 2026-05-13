package com.app.digitalwallet.api

import com.app.digitalwallet.api.dto.APIResponse
import com.app.digitalwallet.api.dto.MoneyRequest
import com.app.digitalwallet.api.dto.SendMoneyRequest
import com.app.digitalwallet.api.dto.TransactionResponse
import com.app.digitalwallet.api.dto.WalletInfoResponse
import com.app.digitalwallet.api.dto.TransactionsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// WalletApiService.kt

interface WalletApiService {
    // Change "wallet/info" to "wallet"
    @GET("wallet")
    suspend fun getWalletInfo(): APIResponse<WalletInfoResponse>

    @GET("wallet/transactions")
    suspend fun getAllTransactions(): APIResponse<TransactionsResponse>

    // Change "wallet/send" to "wallet/transfer"
    @POST("wallet/transfer")
    suspend fun sendMoney(@Body request: SendMoneyRequest): APIResponse<TransactionResponse>

    @POST("wallet/deposit")
    suspend fun deposit(@Body request: MoneyRequest): APIResponse<TransactionResponse>

    @POST("wallet/withdraw")
    suspend fun withdraw(@Body request: MoneyRequest): APIResponse<TransactionResponse>
}
