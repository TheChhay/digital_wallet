package com.app.digitalwallet.domain.repository

import com.app.digitalwallet.domain.model.Transaction
import com.app.digitalwallet.domain.model.Wallet
import kotlinx.coroutines.flow.Flow

data class TransactionsPage(
    val transactions: List<Transaction>,
    val nextCursor: String?,
    val hasMore: Boolean
)

data class RecipientLookupResult(
    val fullName: String,
    val phone: String?,
    val id: String?
)

interface IWalletRepository {
    suspend fun getWalletInfoSync(): Wallet
    fun getWalletInfo(): Flow<Wallet>
    fun getAllTransactions(cursor: String? = null): Flow<TransactionsPage>
    suspend fun transferMoney(recipientPhone: String? = null, walletId: String? = null, amount: Double, note: String?): Boolean
    suspend fun lookupRecipient(phone: String? = null, walletId: String? = null): RecipientLookupResult?
    suspend fun deposit(amount: Double): Boolean
    suspend fun withdraw(amount: Double): Boolean
}
