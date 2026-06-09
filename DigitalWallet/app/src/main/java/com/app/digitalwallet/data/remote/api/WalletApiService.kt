package com.app.digitalwallet.data.remote.api

import com.app.digitalwallet.data.remote.dto.APIResponse
import com.app.digitalwallet.data.remote.dto.MoneyRequest
import com.app.digitalwallet.data.remote.dto.RecipientLookupDto
import com.app.digitalwallet.data.remote.dto.TransactionResponse
import com.app.digitalwallet.data.remote.dto.WalletInfoResponse
import com.app.digitalwallet.data.remote.dto.TransactionsResponse
import com.app.digitalwallet.data.remote.dto.TransferMoneyRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface WalletApiService {
    @GET("wallet")
    suspend fun getWalletInfo(): APIResponse<WalletInfoResponse>

    @GET("wallet/transactions")
    suspend fun getAllTransactions(@Query("cursor") cursor: String? = null): APIResponse<TransactionsResponse>
    
    @GET("wallet/lookup-recipient")
    suspend fun lookupRecipient(
        @Query("wallet_id") walletId: String? = null,
        @Query("phone") phone: String? = null
    ): APIResponse<RecipientLookupDto>

    @POST("wallet/transfer")
    suspend fun transferMoney(@Body request: TransferMoneyRequest): APIResponse<TransactionResponse>

    @POST("wallet/deposit")
    suspend fun deposit(@Body request: MoneyRequest): APIResponse<TransactionResponse>

    @POST("wallet/withdraw")
    suspend fun withdraw(@Body request: MoneyRequest): APIResponse<TransactionResponse>
}
