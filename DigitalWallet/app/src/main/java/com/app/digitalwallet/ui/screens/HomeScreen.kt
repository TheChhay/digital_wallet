package com.app.digitalwallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.app.digitalwallet.ui.components.TransactionDetailContent

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.app.digitalwallet.data.Transaction
import com.app.digitalwallet.data.WalletInfo
import com.app.digitalwallet.viewmodel.WalletUiState
import com.app.digitalwallet.viewmodel.WalletViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: WalletViewModel, 
    onNavigateToDetail: (String) -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToAllTransactions: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val todayTransactions by viewModel.todayTransactions.collectAsState()
    
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

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
                    viewModel = viewModel,
                    walletInfo = state.walletInfo,
                    todayTransactions = todayTransactions,
                    onTransactionClick = { transaction ->
                        selectedTransaction = transaction
                        showBottomSheet = true
                    },
                    onNavigateToTransfer = onNavigateToTransfer,
                    onNavigateToAllTransactions = onNavigateToAllTransactions
                )
            }
            is WalletUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = Color.Red)
                }
            }
        }
        
        if (showBottomSheet && selectedTransaction != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                TransactionDetailContent(
                    transaction = selectedTransaction!!,
                    onClose = { showBottomSheet = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel,
    walletInfo: WalletInfo,
    todayTransactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToAllTransactions: () -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Reset isRefreshing when uiState changes from Loading
    androidx.compose.runtime.LaunchedEffect(viewModel.uiState.collectAsState().value) {
        if (viewModel.uiState.value !is WalletUiState.Loading) {
            isRefreshing = false
        }
    }
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.refresh()
        },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Total Balance Card
            BalanceCard(walletInfo.balance)

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(icon = Icons.AutoMirrored.Filled.Send, label = "Send", onClick = onNavigateToTransfer)
                ActionButton(icon = Icons.Outlined.QrCodeScanner, label = "Scan QR", onClick = { /* Scan */ })
                ActionButton(icon = Icons.Outlined.QrCodeScanner, label = "My QR", onClick = { /* Scan */ })

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
                    modifier = Modifier.clickable { onNavigateToAllTransactions() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Transactions List
            if (todayTransactions.isEmpty()) {
                EmptyTransactionsState()
            } else {
                todayTransactions.forEach { transaction ->
                    TransactionItem(
                        transaction = transaction, 
                        onClick = { onTransactionClick(transaction) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun EmptyTransactionsState() {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ){
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.ReceiptLong, null, Modifier.size(40.dp), MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No transactions yet",
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Your recent activity will appear here",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}


