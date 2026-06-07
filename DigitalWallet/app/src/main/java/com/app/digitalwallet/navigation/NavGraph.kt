package com.app.digitalwallet.navigation

import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.app.digitalwallet.ui.screens.*
import com.app.digitalwallet.viewmodel.*

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun MainNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    val walletViewModel: WalletViewModel = hiltViewModel()
    val qrViewModel: QRViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Screens that should not have bottom padding (full screen screens)
    val fullScreenRoutes = listOf(
        Screen.Splash.route,
        Screen.Login.route,
        Screen.Register.route,
        Screen.QRScanner.route
    )
    
    val isFullScreen = fullScreenRoutes.contains(currentRoute)

    LaunchedEffect(Unit) {
        authViewModel.logoutEvent.collect {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = if (isFullScreen) Modifier else Modifier.padding(bottom = innerPadding.calculateBottomPadding())
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
                viewModel = authViewModel,
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
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    walletViewModel.refresh()
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
                onNavigateToTransfer = { navController.navigate(Screen.Transfer.route) },
                onNavigateToScan = { navController.navigate(Screen.QRScanner.route) },
                onNavigateToMyQR = { address -> 
                    navController.navigate(Screen.StaticQR.createRoute(address)) 
                },
                onNavigateToRequest = {
                    navController.navigate(Screen.DynamicQR.route)
                },
                onNavigateToAllTransactions = { navController.navigate(Screen.History.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            ) 
        }
        composable(Screen.Wallet.route) { 
            WalletScreen(
                viewModel = walletViewModel,
                authViewModel = authViewModel
            ) 
        }
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
                qrViewModel = qrViewModel,
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
        
        // QR related screens
        composable(
            route = Screen.StaticQR.route,
            arguments = listOf(
                androidx.navigation.navArgument("address") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: ""
            StaticQRScreen(
                viewModel = qrViewModel,
                walletAddress = address,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.DynamicQR.route) {
            val walletState by walletViewModel.uiState.collectAsState()
            val walletId = (walletState as? WalletUiState.Success)?.walletInfo?.walletId ?: ""
            
            DynamicQRScreen(
                viewModel = qrViewModel,
                walletId = walletId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.QRScanner.route) {
            CameraQRScannerScreen(
                viewModel = qrViewModel,
                walletViewModel = walletViewModel,
                onNavigateToPayment = { request ->
                    qrViewModel.setPendingPayment(
                        com.app.digitalwallet.api.dto.ValidateTokenResponse(
                            isValid = true,
                            message = null,
                            recipientName = request.fullName,
                            recipientPhone = request.phone,
                            recipientId = request.id,
                            amount = null,
                            currency = "USD"
                        )
                    )
                    navController.navigate(Screen.Transfer.route)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("$title coming soon...")
        }
    }
}
