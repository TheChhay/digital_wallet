package com.app.digitalwallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.digitalwallet.ui.theme.ZenBlue
import com.app.digitalwallet.ui.theme.ZenGray
import com.app.digitalwallet.ui.theme.ZenPrimary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.digitalwallet.viewmodel.WalletViewModel
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.digitalwallet.navigation.Screen
import com.app.digitalwallet.ui.screens.HistoryScreen
import com.app.digitalwallet.ui.screens.HomeScreen
import com.app.digitalwallet.ui.screens.IdentityVerificationScreen
import com.app.digitalwallet.ui.screens.LoginScreen
import com.app.digitalwallet.ui.screens.ProfileScreen
import com.app.digitalwallet.ui.screens.SplashScreen
import com.app.digitalwallet.ui.screens.TransactionDetailScreen
import com.app.digitalwallet.ui.screens.TransferScreen
import com.app.digitalwallet.ui.screens.TransferSuccessScreen
import com.app.digitalwallet.ui.screens.WalletScreen
import com.app.digitalwallet.ui.theme.DigitalWalletTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize networking
        com.app.digitalwallet.api.RetrofitClient.init(this)

        enableEdgeToEdge()
        setContent {
            DigitalWalletTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Home,
        Screen.Wallet,
        Screen.History,
        Screen.Profile
    )

    // Get current destination to decide whether to show the BottomBar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Logic: Only show bottom bar if we are on one of the 4 main tab routes
    val showBottomBar = items.any { it.route == currentDestination?.route }
    
    val walletViewModel: WalletViewModel = viewModel()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    items.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    imageVector = screen.icon!!, 
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            label = { 
                                Text(
                                    text = screen.label!!,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToHome = {
                        walletViewModel.refresh()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        walletViewModel.refresh()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    }
                )
            }
            composable(Screen.Register.route) {
                com.app.digitalwallet.ui.screens.RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Home.route) { 
                HomeScreen(
                    viewModel = walletViewModel,
                    onNavigateToDetail = { transactionId -> 
                        navController.navigate(Screen.TransactionDetail.createRoute(transactionId)) 
                    },
                    onNavigateToTransfer = { navController.navigate(Screen.Transfer.route) },
                    onNavigateToAllTransactions = { navController.navigate(Screen.History.route) }
                ) 
            }
            composable(Screen.Wallet.route) { WalletScreen(viewModel = walletViewModel) }
            composable(Screen.History.route) { HistoryScreen(viewModel = walletViewModel) }
            composable(Screen.Profile.route) { ProfileScreen(navController = navController) }
            composable(
                route = Screen.TransactionDetail.route,
                arguments = listOf(
                    androidx.navigation.navArgument("transactionId") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
                TransactionDetailScreen(
                    transactionId = transactionId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Transfer.route) {
                TransferScreen(
                    viewModel = walletViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToSuccess = { amount, recipient ->
                        walletViewModel.refresh()
                        navController.navigate(Screen.TransferSuccess.createRoute(amount, recipient)) {
                            popUpTo(Screen.Transfer.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Screen.TransferSuccess.route,
                arguments = listOf(
                    androidx.navigation.navArgument("amount") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("recipient") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val amount = backStackEntry.arguments?.getString("amount") ?: ""
                val recipient = backStackEntry.arguments?.getString("recipient") ?: ""
                TransferSuccessScreen(
                    amount = amount,
                    recipientName = recipient,
                    onDone = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
            
            // Profile related screens
            composable(Screen.PersonalInfo.route) { 
                PlaceholderScreen("Personal Information", onBack = { navController.popBackStack() }) 
            }
            composable(Screen.PaymentMethods.route) { 
                PlaceholderScreen("Payment Methods", onBack = { navController.popBackStack() }) 
            }
            composable(Screen.Notifications.route) { 
                PlaceholderScreen("Notifications", onBack = { navController.popBackStack() }) 
            }
            composable(Screen.Security.route) { 
                PlaceholderScreen("Security", onBack = { navController.popBackStack() }) 
            }
            composable(Screen.HelpCenter.route) { 
                PlaceholderScreen("Help Center", onBack = { navController.popBackStack() }) 
            }
            composable(Screen.IdentityVerification.route) { 
                IdentityVerificationScreen(
                    onBack = { navController.popBackStack() },
                    onProceed = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("$title Screen Content")
        }
    }
}
