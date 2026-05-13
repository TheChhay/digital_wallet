package com.app.digitalwallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.digitalwallet.ui.theme.ZenAccent
import com.app.digitalwallet.ui.theme.ZenBlue
import com.app.digitalwallet.ui.theme.ZenGray
import com.app.digitalwallet.ui.theme.ZenPrimary
import com.app.digitalwallet.ui.components.TransactionItem
import com.app.digitalwallet.ui.components.ActionButton
import com.app.digitalwallet.ui.components.BalanceCard
import com.app.digitalwallet.ui.components.RewardsCard

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.app.digitalwallet.data.Transaction
import com.app.digitalwallet.data.WalletInfo
import com.app.digitalwallet.viewmodel.WalletUiState
import com.app.digitalwallet.viewmodel.WalletViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: WalletViewModel, onNavigateToDetail: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "ZenWallet",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* Profile */ }) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Icon(
                                Icons.Default.Person, 
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* Notifications */ }) {
                        Icon(
                            Icons.Outlined.Notifications, 
                            contentDescription = "Notifications", 
                            tint = MaterialTheme.colorScheme.onSurface
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
                HomeContent(
                    modifier = Modifier.padding(innerPadding),
                    walletInfo = state.walletInfo,
                    onNavigateToDetail = onNavigateToDetail
                )
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
fun HomeContent(
    modifier: Modifier = Modifier,
    walletInfo: WalletInfo,
    onNavigateToDetail: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 1. Total Balance Card
        BalanceCard(walletInfo.balance, walletInfo.monthlyGrowth)

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ActionButton(icon = Icons.Default.Send, label = "Send", onClick = { /* Send */ })
            ActionButton(icon = Icons.Default.AddCard, label = "Deposit", onClick = { /* Deposit */ })
            ActionButton(icon = Icons.Default.Payments, label = "Withdraw", onClick = { /* Withdraw */ })
            ActionButton(icon = Icons.Outlined.QrCodeScanner, label = "Scan QR", onClick = { /* Scan */ })
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 3. Recent Transactions Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Transactions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "See All",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { /* See All */ }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Transactions List
        walletInfo.recentTransactions.forEach { transaction ->
            TransactionItem(
                transaction = transaction,
                onClick = onNavigateToDetail
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. Rewards Card
        RewardsCard(
            title = "New Rewards!",
            subtitle = "You've earned a cashback boost.",
            buttonText = "Claim Now",
            onButtonClick = { /* Claim */ }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}


