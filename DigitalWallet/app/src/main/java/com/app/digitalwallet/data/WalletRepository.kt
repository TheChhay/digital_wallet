package com.app.digitalwallet.data

import com.app.digitalwallet.api.RetrofitClient
import com.app.digitalwallet.api.WalletApiService
import com.app.digitalwallet.api.dto.MoneyRequest
import com.app.digitalwallet.api.dto.SendMoneyRequest
import com.app.digitalwallet.api.dto.TransactionDto
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class WalletInfo(
    val balance: Double,
    val currency: String = "USD",
    val monthlyGrowth: Double = 2.5,
    val recentTransactions: List<Transaction> = emptyList()
)

class WalletRepository(private val apiService: WalletApiService = RetrofitClient.walletApi) {

    fun getWalletInfo(): Flow<WalletInfo> = flow {
        val response = apiService.getWalletInfo()
        if (response.success && response.data != null) {
            val dto = response.data
            emit(WalletInfo(
                balance = dto.balanceCents / 100.0,
                currency = dto.currency ?: "USD",
                monthlyGrowth = dto.monthlyGrowth ?: 0.0,
                recentTransactions = dto.recentTransactions?.map { it.toDomain() } ?: emptyList()
            ))
        } else {
            throw Exception(response.message)
        }
    }

    fun getAllTransactions(): Flow<List<Transaction>> = flow {
        val response = apiService.getAllTransactions()
        if (response.success && response.data != null) {
            val list = response.data.items?.map { it.toDomain() } ?: emptyList()
            emit(list)
        } else {
            emit(emptyList())
        }
    }

    suspend fun sendMoney(recipientPhone: String, amount: Double, note: String?): Boolean {
        val amountCents = (amount * 100).toLong()
        val idempotencyKey = UUID.randomUUID().toString()
        val response = apiService.sendMoney(SendMoneyRequest(
            receiverPhone = recipientPhone,
            amountCents = amountCents,
            description = note,
            idempotencyKey = idempotencyKey
        ))
        return response.success
    }

    suspend fun deposit(amount: Double): Boolean {
        val amountCents = (amount * 100).toLong()
        val response = apiService.deposit(MoneyRequest(amountCents = amountCents))
        return response.success
    }

    suspend fun withdraw(amount: Double): Boolean {
        val amountCents = (amount * 100).toLong()
        val response = apiService.withdraw(MoneyRequest(amountCents = amountCents))
        return response.success
    }

    private fun TransactionDto.toDomain(): Transaction {
        val finalAmount = amount ?: ((amountCents ?: 0L) / 100.0)
        
        // Handle possible missing date/time from backend
        val displayDate = date ?: createdAt?.split("T")?.getOrNull(0) ?: "Today"
        val displayTime = time ?: createdAt?.split("T")?.getOrNull(1)?.take(5) ?: "00:00"

        return Transaction(
            id = id ?: "",
            merchantName = merchantName ?: description ?: "Transaction",
            category = category ?: type ?: "General",
            amount = finalAmount,
            date = displayDate,
            time = displayTime,
            status = TransactionStatus.fromString(status ?: "SUCCESS"),
            icon = Transaction.getIconForName(icon ?: type ?: "default"),
            isPositive = isPositive ?: (type == "deposit" || type == "transfer" && amountCents?.let { it > 0 } == true),
            paymentMethod = paymentMethod ?: "Balance",
            tax = tax ?: 0.0
        )
    }
}
