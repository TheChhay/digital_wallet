package com.app.digitalwallet.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.digitalwallet.ui.components.CreditCardItem
import com.app.digitalwallet.ui.theme.ZenAccent
import com.app.digitalwallet.ui.theme.ZenPrimary
import com.app.digitalwallet.viewmodel.AuthViewModel
import com.app.digitalwallet.viewmodel.WalletUiState
import com.app.digitalwallet.viewmodel.WalletViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    authViewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile = authViewModel.userProfile

    // Notification Permission Handling for Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(
            Manifest.permission.POST_NOTIFICATIONS
        )
        LaunchedEffect(Unit) {
            if (!notificationPermissionState.status.isGranted) {
                notificationPermissionState.launchPermissionRequest()
            }
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.getMe()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "My Wallet", 
                            color = MaterialTheme.colorScheme.onSurface, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when (val state = uiState) {
            is WalletUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ZenPrimary)
                }
            }
            is WalletUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp)
                ) {
                    item {
                        CardSection(state.walletInfo.balance)
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            "My Cards", 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    item {
                        val fullName = userProfile?.let { "${it.firstName} ${it.lastName}" } ?: "Loading..."
                        val displayPhone = userProfile?.phone?.let { 
                            if (it.length >= 4) "•••• •••• •••• ${it.takeLast(4)}" else it 
                        } ?: "•••• •••• •••• ••••"

                        CreditCardItem(
                            number = displayPhone,
                            holder = fullName,
                            expiry = "", // Expiry commented out/empty as requested
                            gradient = Brush.horizontalGradient(listOf(ZenPrimary, ZenAccent))
                        )
                    }
                }
            }
            is WalletUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun CardSection(balance: Double) {
    val formattedBalance = String.format(Locale.US, "%,.2f", balance)
    Column {
        Text("Total Balance", color = MaterialTheme.colorScheme.tertiary, fontSize = 14.sp)
        Text(
            text = "$ $formattedBalance",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
