package com.app.digitalwallet.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WalletInfoResponse(
    @SerialName("id") val walletId: String,
    @SerialName("balance_cents") val balanceCents: Long,
    val currency: String? = "USD",
    @SerialName("monthly_growth") val monthlyGrowth: Double? = 0.0,
    @SerialName("recent_transactions") val recentTransactions: List<TransactionDto>? = emptyList()
)

@Serializable
data class TransactionsResponse(
    val items: List<TransactionDto>? = emptyList(),
    @SerialName("has_more") val hasMore: Boolean? = false,
    @SerialName("next_cursor") val nextCursor: String? = null
)

@Serializable
data class TransactionDto(
    val id: String? = null,
    val reference: String? = null,
    val type: String? = null,
    val status: String? = null,
    @SerialName("amount_cents") val amountCents: Long? = 0,
    val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("merchant_name") val merchantName: String? = null,
    @SerialName("receiver_name") val receiverName: String? = null,
    @SerialName("receiver_phone") val receiverPhone: String? = null,
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("sender_phone") val senderPhone: String? = null,
    val category: String? = null,
    val amount: Double? = null,
    val date: String? = null,
    val time: String? = null,
    val icon: String? = null,
    @SerialName("is_positive") val isPositive: Boolean? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val tax: Double? = null
)

@Serializable
data class TransferMoneyRequest(
    @SerialName("receiver_phone") val receiverPhone: String? = null,
    @SerialName("receiver_wallet_id") val receiverWalletId: String? = null,
    @SerialName("amount_cents") val amountCents: Long,
    val description: String? = null,
    @SerialName("idempotency_key") val idempotencyKey: String
)

@Serializable
data class MoneyRequest(
    @SerialName("amount_cents") val amountCents: Long,
    val description: String? = null
)

@Serializable
data class TransactionResponse(
    @SerialName("id") val id: String? = null,
    @SerialName("reference") val reference: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("message") val message: String? = null
)

@Serializable
data class RecipientLookupDto(
    @SerialName("full_name") val fullName: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("id") val id: String? = null,
)
