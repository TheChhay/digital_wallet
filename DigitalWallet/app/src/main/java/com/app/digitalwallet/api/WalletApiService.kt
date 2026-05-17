package com.app.digitalwallet.api

import com.app.digitalwallet.api.dto.APIResponse
import com.app.digitalwallet.api.dto.MoneyRequest
import com.app.digitalwallet.api.dto.RecipientLookupDto
import com.app.digitalwallet.api.dto.TransactionResponse
import com.app.digitalwallet.api.dto.WalletInfoResponse
import com.app.digitalwallet.api.dto.TransactionsResponse
import com.app.digitalwallet.api.dto.TransferMoneyRequest
import com.app.digitalwallet.api.dto.UserResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// WalletApiService.kt

interface WalletApiService {
    @GET("wallet")
    suspend fun getWalletInfo(): APIResponse<WalletInfoResponse>

    @GET("wallet/transactions")
    suspend fun getAllTransactions(@Query("cursor") cursor: String? = null): APIResponse<TransactionsResponse>

    @GET("wallet/recipient-lookup")
    suspend fun lookupRecipient(@Query("phone") phone: String): APIResponse<RecipientLookupDto>

    @GET("wallet/get-userinfo-by-wallet-id")
    suspend fun findUserByWalletId(@Query("wallet_id") walletId: String): APIResponse<RecipientLookupDto>

    @POST("wallet/transfer")
    suspend fun transferMoney(@Body request: TransferMoneyRequest): APIResponse<TransactionResponse>

    @POST("wallet/deposit")
    suspend fun deposit(@Body request: MoneyRequest): APIResponse<TransactionResponse>

    @POST("wallet/withdraw")
    suspend fun withdraw(@Body request: MoneyRequest): APIResponse<TransactionResponse>
}
