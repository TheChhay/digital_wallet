package com.app.digitalwallet.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.digitalwallet.viewmodel.WalletUiState
import com.app.digitalwallet.viewmodel.WalletViewModel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.app.digitalwallet.ui.components.CreditCardItem
import com.app.digitalwallet.ui.theme.ZenAccent
import com.app.digitalwallet.ui.theme.ZenPrimary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(viewModel: WalletViewModel) {
    val uiState by viewModel.uiState.collectAsState()

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
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    
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
                        CreditCardItem(
                            number = "•••• •••• •••• 8812",
                            holder = "Alex Johnson",
                            expiry = "12/26",
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
    Column {
        Text("Total Balance", color = MaterialTheme.colorScheme.tertiary, fontSize = 14.sp)
        Text(
            "$${String.format(Locale.US, "%,.2f", balance)}",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

