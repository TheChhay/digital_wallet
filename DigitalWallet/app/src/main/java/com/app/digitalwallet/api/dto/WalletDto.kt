package com.app.digitalwallet.api.dto

import com.google.gson.annotations.SerializedName

data class WalletInfoResponse(
    @SerializedName("balance_cents") val balanceCents: Long,
    val currency: String? = "USD",
    @SerializedName("monthly_growth") val monthlyGrowth: Double? = 0.0,
    @SerializedName("recent_transactions") val recentTransactions: List<TransactionDto>? = emptyList()
)

data class TransactionsResponse(
    val items: List<TransactionDto>? = emptyList(),
    @SerializedName("has_more") val hasMore: Boolean? = false,
    @SerializedName("next_cursor") val nextCursor: String? = null
)

data class TransactionDto(
    val id: String? = null,
    val reference: String? = null,
    val type: String? = null,
    val status: String? = null,
    @SerializedName("amount_cents") val amountCents: Long? = 0,
    val description: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("merchant_name") val merchantName: String? = null,
    @SerializedName("receiver_name") val receiverName: String? = null,
    @SerializedName("receiver_phone") val receiverPhone: String? = null,
    val category: String? = null,
    val amount: Double? = null,
    val date: String? = null,
    val time: String? = null,
    val icon: String? = null,
    @SerializedName("is_positive") val isPositive: Boolean? = null,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    val tax: Double? = null
)

data class SendMoneyRequest(
    @SerializedName("receiver_phone") val receiverPhone: String,
    @SerializedName("amount_cents") val amountCents: Long,
    val description: String? = null,
    @SerializedName("idempotency_key") val idempotencyKey: String
)

data class MoneyRequest(
    @SerializedName("amount_cents") val amountCents: Long,
    val description: String? = null
)

data class TransactionResponse(
    @SerializedName("transaction_id") val transactionId: String,
    val status: String,
    val message: String
)

data class RecipientLookupDto(
    @SerializedName("full_name") val fullName: String,
    val role: String? = "USER"
)
