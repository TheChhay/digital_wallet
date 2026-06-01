package com.app.digitalwallet.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class TransactionStatus {
    SUCCESS, PENDING, FAILED;

    companion object {
        fun fromString(value: String): TransactionStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: SUCCESS
        }
    }
}

data class Transaction(
    val id: String = "",
    val merchantName: String = "Unknown",
    val receiverName: String? = null,
    val receiverPhone: String? = null,
    val senderPhone: String? = null,
    val senderName: String? = null,
    val reference: String? = null,
    val category: String = "General",
    val amount: Double = 0.0,
    val date: String = "",
    val time: String = "",
    val status: TransactionStatus = TransactionStatus.SUCCESS,
    val icon: ImageVector = Icons.Default.Payments,
    val isPositive: Boolean = false,
    val paymentMethod: String = "Balance",
    val tax: Double = 0.0,
    val createdAt: String = "" // For sorting
) {
    companion object {
        fun getIconForName(iconName: String): ImageVector {
            return when (iconName.lowercase()) {
                "shopping_bag", "shopping" -> Icons.Default.ShoppingBag
                "restaurant", "food" -> Icons.Default.Restaurant
                "directions_car", "transport" -> Icons.Default.DirectionsCar
                "movie", "entertainment" -> Icons.Default.Movie
                "account_balance_wallet", "wallet" -> Icons.Default.AccountBalanceWallet
                "trending_up", "investment" -> Icons.AutoMirrored.Filled.TrendingUp
                "phone_android", "mobile" -> Icons.Default.PhoneAndroid
                "electric_bolt", "utilities" -> Icons.Default.ElectricBolt
                "person", "transfer" -> Icons.Default.Person
                else -> Icons.Default.Payments
            }
        }
    }
}
