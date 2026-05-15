package com.app.digitalwallet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.digitalwallet.data.Transaction
import com.app.digitalwallet.ui.components.TransactionItem
import com.app.digitalwallet.ui.components.ZenTextField
import com.app.digitalwallet.ui.components.TransactionDetailContent
import java.util.Locale

import androidx.compose.runtime.collectAsState
import com.app.digitalwallet.viewmodel.WalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: WalletViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    val transactions by viewModel.transactions.collectAsState()

    val filteredTransactions = transactions.filter {
        (selectedFilter == "All" || it.status.name.lowercase(Locale.US) == selectedFilter.lowercase(Locale.US)) &&
        (searchQuery.isEmpty() || it.merchantName.contains(searchQuery, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "History", 
                            color = MaterialTheme.colorScheme.onSurface, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            ZenTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                placeholder = "Search transactions...",
                leadingIcon = { 
                    Icon(
                        Icons.Outlined.Search, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                isOutlined = true
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Success", "Pending", "Failed").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == filter,
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent,
                            borderWidth = 0.dp
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                items(filteredTransactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        subtitle = "${transaction.date}, ${transaction.time}"
                    ) {
                        selectedTransaction = transaction
                        showSheet = true
                    }
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }

        if (showSheet && selectedTransaction != null) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                TransactionDetailContent(
                    transaction = selectedTransaction!!,
                    onClose = { showSheet = false }
                )
            }
        }
    }
}
