package com.app.digitalwallet.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String? = null, val icon: ImageVector? = null) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Wallet : Screen("wallet", "Wallet", Icons.Default.Wallet)
    object History : Screen("history", "History", Icons.Default.History)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object TransactionDetail : Screen("detail/{transactionId}") {
        fun createRoute(transactionId: String) = "detail/$transactionId"
    }
    object Transfer : Screen("transfer")
    object TransferSuccess : Screen("transfer_success/{amount}/{recipient}") {
        fun createRoute(amount: String, recipient: String) = "transfer_success/$amount/$recipient"
    }
    
    // Profile related screens
    object PersonalInfo : Screen("personal_info")
    object PaymentMethods : Screen("payment_methods")
    object Notifications : Screen("notifications")
    object Security : Screen("security")
    object HelpCenter : Screen("help_center")
    object IdentityVerification : Screen("identity_verification")
    
    // QR related screens
    object StaticQR : Screen("static_qr/{address}") {
        fun createRoute(address: String) = "static_qr/$address"
    }
    object DynamicQR : Screen("dynamic_qr")
    object QRScanner : Screen("qr_scanner")

}
