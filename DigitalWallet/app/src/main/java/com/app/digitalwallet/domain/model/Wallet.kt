package com.app.digitalwallet.domain.model

data class Wallet(
    val balance: Double,
    val walletId: String,
    val currency: String = "USD",
    val monthlyGrowth: Double = 2.5,
    val recentTransactions: List<Transaction> = emptyList()
)
