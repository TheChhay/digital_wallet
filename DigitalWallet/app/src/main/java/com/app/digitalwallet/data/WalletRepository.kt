package com.app.digitalwallet.data

import com.app.digitalwallet.api.WalletApiService
import com.app.digitalwallet.api.dto.MoneyRequest
import com.app.digitalwallet.api.dto.TransferMoneyRequest
import com.app.digitalwallet.api.dto.TransactionDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

data class WalletInfo(
    val balance: Double,
    val walletId: String,
    val currency: String = "USD",
    val monthlyGrowth: Double = 2.5,
    val recentTransactions: List<Transaction> = emptyList()
)

@Singleton
class WalletRepository @Inject constructor(private val apiService: WalletApiService) {

    fun getWalletInfo(): Flow<WalletInfo> = flow {
        val response = apiService.getWalletInfo()
        if (response.success && response.data != null) {
            val dto = response.data
            
            emit(WalletInfo(
                balance = dto.balanceCents / 100.0,
                walletId = dto.walletId,
                currency = dto.currency ?: "USD",
                monthlyGrowth = dto.monthlyGrowth ?: 0.0,
                recentTransactions = emptyList() // Now fetched via getAllTransactions
            ))
        } else {
            throw Exception(response.message)
        }
    }

    data class TransactionsPage(
        val transactions: List<Transaction>,
        val nextCursor: String?,
        val hasMore: Boolean
    )

    fun getAllTransactions(cursor: String? = null): Flow<TransactionsPage> = flow {
        val response = apiService.getAllTransactions(cursor)
        if (response.success && response.data != null) {
            val dto = response.data
            val list = dto.items?.map { it.toDomain() } ?: emptyList()
            emit(TransactionsPage(
                transactions = list,
                nextCursor = dto.nextCursor,
                hasMore = dto.hasMore ?: false
            ))
        } else {
            emit(TransactionsPage(emptyList(), null, false))
        }
    }

    suspend fun transferMoney(recipientPhone: String? = null, walletId: String? = null, amount: Double, note: String?): Boolean {
        val amountCents = (amount * 100).toLong()
        val idempotencyKey = UUID.randomUUID().toString()
        val response = apiService.transferMoney(TransferMoneyRequest(
            receiverPhone = recipientPhone,
            receiverWalletId = walletId,
            amountCents = amountCents,
            description = note,
            idempotencyKey = idempotencyKey
        ))
        return response.success
    }

    suspend fun findUserByWalletId(phone: String? = null, walletId: String? = null): RecipientLookupResult? {
        return try {
            val response = if (walletId != null) {
                apiService.findUserByWalletId(walletId)
            } else {
                apiService.lookupRecipient(phone ?: "")
            }

            if (response.success && response.data != null) {
                RecipientLookupResult(
                    fullName = response.data.fullName,
                    phone = response.data.phone,
                    id = response.data.id

                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    data class RecipientLookupResult(
        val fullName: String,
        val phone: String?,
        val id: String?
    )

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

        val transactionType = type?.lowercase()
        val fallbackName = when (transactionType) {
            "transfer" -> "Money Transfer"
            "deposit" -> "Deposit"
            "withdrawal" -> "Withdrawal"
            else -> "Transaction"
        }

        // Mock logic for missing receiver name in transfers
        val effectiveReceiverName = if (transactionType == "transfer" && receiverName.isNullOrBlank()) {
            "Unknown Recipient" 
        } else {
            receiverName
        }

        return Transaction(
            id = id ?: "",
            merchantName = merchantName.takeIf { !it.isNullOrBlank() } 
                ?: description.takeIf { !it.isNullOrBlank() } 
                ?: fallbackName,
            receiverName = effectiveReceiverName.takeIf { !it.isNullOrBlank() },
            receiverPhone = receiverPhone.takeIf { !it.isNullOrBlank() },
            reference = reference,
            category = category ?: type?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "General",
            amount = finalAmount,
            date = displayDate,
            time = displayTime,
            status = TransactionStatus.fromString(status ?: "SUCCESS"),
            icon = Transaction.getIconForName(icon ?: type ?: "default"),
            isPositive = isPositive ?: (transactionType == "deposit"),
            paymentMethod = paymentMethod ?: "Balance",
            tax = tax ?: 0.0,
            createdAt = createdAt ?: ""
        )
    }
}
