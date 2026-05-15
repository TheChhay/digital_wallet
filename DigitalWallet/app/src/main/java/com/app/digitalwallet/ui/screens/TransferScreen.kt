package com.app.digitalwallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.app.digitalwallet.ui.components.PrimaryButton
import com.app.digitalwallet.ui.components.ZenTextField
import com.app.digitalwallet.ui.theme.ZenPrimary
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.app.digitalwallet.auth.TokenManager
import com.app.digitalwallet.utils.PhoneNumberUtils
import com.app.digitalwallet.viewmodel.WalletUiState
import com.app.digitalwallet.viewmodel.WalletViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit,
    onNavigateToSuccess: (String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var phoneNumber by remember { mutableStateOf("") }
    
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmedRecipientName by remember { mutableStateOf("") }
    var confirmedRecipientPhone by remember { mutableStateOf("") }
    var isLookingUpRecipient by remember { mutableStateOf(false) }
    var showRecipientNotFoundError by remember { mutableStateOf(false) }
    var showSelfTransferError by remember { mutableStateOf(false) }
    var showAdminTransferError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val currentUserPhone = remember { TokenManager.getInstance(context).getUserPhone() }

    val uiState by viewModel.uiState.collectAsState()
    val transferStatus by viewModel.transferStatus.collectAsState()
    val balance = (uiState as? WalletUiState.Success)?.walletInfo?.balance ?: 0.0
    val amountDouble = amount.toDoubleOrNull() ?: 0.0
    val isInsufficientFunds = amountDouble > balance
    val isAmountValid = amountDouble > 0 && !isInsufficientFunds

    val contacts = listOf(
        Contact("New", null, Icons.Default.Add, isAction = true),
        Contact("Alex M.", "0812345678", Icons.Default.Person),
        Contact("Sarah R.", "0812345679", Icons.Default.Person),
        Contact("James D.", "0812345680", Icons.Default.Person),
        Contact("Kate L.", "0812345681", Icons.Default.Person)
    )

    // Handle Transfer Status
    LaunchedEffect(transferStatus) {
        if (transferStatus is com.app.digitalwallet.viewmodel.TransferStatus.Success) {
            // Success is handled via onNavigateToSuccess callback in the Dialog's confirm button
            // But we might want to reset it when we leave or return
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer Money", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier.padding(end = 16.dp).size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(8.dp))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                ZenTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search name, phone or email",
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    isOutlined = false
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Recent Contacts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Contacts", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "View All",
                        color = ZenPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(contacts) { contact ->
                        ContactItem(
                            contact = contact,
                            isSelected = selectedContact == contact,
                            onClick = { 
                                selectedContact = contact
                                if (!contact.isAction) {
                                    phoneNumber = ""
                                }
                            }
                        )
                    }
                }
                
                // Manual phone input
                Spacer(modifier = Modifier.height(24.dp))
                ZenTextField(
                    value = phoneNumber,
                    onValueChange = { 
                        phoneNumber = it
                        if (it.isNotEmpty()) {
                            // Select "New" to indicate manual entry when typing
                            selectedContact = contacts.firstOrNull { it.isAction }
                        }
                    },
                    placeholder = "Enter phone number",
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = ZenPrimary) },
                    isOutlined = false
                )

                // Amount Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Quick Amount Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        listOf("10", "50", "100", "500").forEach { quickAmount ->
                            SuggestionChip(
                                onClick = { amount = quickAmount },
                                label = { Text("+$quickAmount") },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isInsufficientFunds) Color.Red else ZenPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (amount == "0") "0" else amount,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isInsufficientFunds) Color.Red else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    AnimatedVisibility(
                        visible = isInsufficientFunds,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            "Insufficient balance",
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.clickable { }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Balance: $${String.format(Locale.US, "%,.2f", balance)}",
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Note Field
                ZenTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Transfer Note (Optional)",
                    placeholder = "What's this for?",
                    leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null, tint = ZenPrimary) },
                    isOutlined = true
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Fixed Bottom Section (Keypad + Button)
            Column(modifier = Modifier.background(Color(0xFFF7F7F7))) {
                NumericKeypad(
                    onKeyPress = { key ->
                        amount = updateAmount(amount, key)
                    }
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    PrimaryButton(
                        text = if (isLookingUpRecipient) "Checking..." else "Continue",
                        onClick = {
                            if (selectedContact != null && !selectedContact!!.isAction) {
                                // Known contact selected
                                val contactPhone = selectedContact!!.phone?.let { PhoneNumberUtils.normalize(it) } ?: ""
                                
                                if (contactPhone == currentUserPhone) {
                                    showSelfTransferError = true
                                    return@PrimaryButton
                                }

                                isLookingUpRecipient = true
                                viewModel.lookupRecipient(contactPhone) { result ->
                                    isLookingUpRecipient = false
                                    if (result != null) {
                                        if (result.role == "ADMIN") {
                                            showAdminTransferError = true
                                        } else {
                                            confirmedRecipientName = result.fullName
                                            confirmedRecipientPhone = contactPhone
                                            showConfirmDialog = true
                                        }
                                    } else {
                                        // Fallback if lookup fails for a recent contact
                                        confirmedRecipientName = selectedContact!!.name
                                        confirmedRecipientPhone = contactPhone
                                        showConfirmDialog = true
                                    }
                                }
                            } else {
                                // Manual phone entry
                                val normalizedInput = PhoneNumberUtils.normalize(phoneNumber)
                                
                                if (normalizedInput == currentUserPhone) {
                                    showSelfTransferError = true
                                    return@PrimaryButton
                                }

                                isLookingUpRecipient = true
                                viewModel.lookupRecipient(normalizedInput) { result ->
                                    isLookingUpRecipient = false
                                    if (result != null) {
                                        if (result.role == "ADMIN") {
                                            showAdminTransferError = true
                                        } else {
                                            confirmedRecipientName = result.fullName
                                            confirmedRecipientPhone = normalizedInput
                                            showConfirmDialog = true
                                        }
                                    } else {
                                        showRecipientNotFoundError = true
                                    }
                                }
                            }
                        },
                        icon = if (isLookingUpRecipient) null else Icons.AutoMirrored.Filled.ArrowForward,
                        enabled = !isLookingUpRecipient && isAmountValid && ( (selectedContact != null && !selectedContact!!.isAction) || phoneNumber.isNotEmpty())
                    )
                    
                    if (isLookingUpRecipient) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp)
                                .size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        val displayName = if (confirmedRecipientName.isNotEmpty() && confirmedRecipientName != "null null") confirmedRecipientName else confirmedRecipientPhone
        
        Dialog(onDismissRequest = { 
            if (transferStatus !is com.app.digitalwallet.viewmodel.TransferStatus.Loading) {
                showConfirmDialog = false 
            }
        }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Receipt Icon
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = ZenPrimary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = ZenPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Transfer Receipt", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Verify transaction details", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Receipt Content
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            ReceiptRow("To", displayName)
                            if (displayName != confirmedRecipientPhone) {
                                ReceiptRow("", confirmedRecipientPhone, isSubtext = true)
                            }
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            
                            ReceiptRow("Amount", "$${String.format(Locale.US, "%,.2f", amountDouble)}", isHighlight = true)
                            ReceiptRow("Fee", "$0.00", isSubtext = true)
                            
                            if (note.isNotBlank()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                Text("Note", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(note, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    PrimaryButton(
                        text = "Confirm & Send",
                        onClick = {
                            viewModel.sendMoney(confirmedRecipientPhone, amountDouble, note) { success ->
                                if (success) {
                                    showConfirmDialog = false
                                    onNavigateToSuccess(amount, displayName)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = transferStatus !is com.app.digitalwallet.viewmodel.TransferStatus.Loading
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { 
                            showConfirmDialog = false 
                            viewModel.resetTransferStatus()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = transferStatus !is com.app.digitalwallet.viewmodel.TransferStatus.Loading
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }

    // Recipient Not Found Dialog
    if (showRecipientNotFoundError) {
        AlertDialog(
            onDismissRequest = { showRecipientNotFoundError = false },
            title = { Text("User Not Found") },
            text = { Text("We couldn't find a user with the phone number $phoneNumber. Please check the number and try again.") },
            confirmButton = {
                TextButton(onClick = { showRecipientNotFoundError = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Self Transfer Error Dialog
    if (showSelfTransferError) {
        AlertDialog(
            onDismissRequest = { showSelfTransferError = false },
            title = { Text("Invalid Transfer") },
            text = { Text("You cannot transfer money to yourself.") },
            confirmButton = {
                TextButton(onClick = { showSelfTransferError = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Admin Transfer Error Dialog
    if (showAdminTransferError) {
        AlertDialog(
            onDismissRequest = { showAdminTransferError = false },
            title = { Text("Transfer Restricted") },
            text = { Text("Transfers to administrator accounts are not allowed for security reasons.") },
            confirmButton = {
                TextButton(onClick = { showAdminTransferError = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Error Snackbar/Toast for Transfer
    if (transferStatus is com.app.digitalwallet.viewmodel.TransferStatus.Error) {
        val errorMessage = (transferStatus as com.app.digitalwallet.viewmodel.TransferStatus.Error).message
        AlertDialog(
            onDismissRequest = { viewModel.resetTransferStatus() },
            title = { Text("Transfer Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetTransferStatus() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ContactItem(contact: Contact, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = if (contact.isAction) ZenPrimary else MaterialTheme.colorScheme.surfaceVariant,
            border = if (isSelected) BorderStroke(2.dp, ZenPrimary) else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    contact.icon,
                    contentDescription = null,
                    tint = if (contact.isAction) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            contact.name,
            fontSize = 12.sp,
            color = if (isSelected) ZenPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun NumericKeypad(onKeyPress: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "backspace")
    )

    Surface(
        color = Color(0xFFF7F7F7),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp)
        ) {
            keys.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { 
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onKeyPress(key) 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "backspace") {
                                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete", tint = ZenPrimary)
                            } else {
                                Text(
                                    text = key,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun updateAmount(current: String, key: String): String {
    if (key == "backspace") {
        return if (current.length > 1) current.dropLast(1) else "0"
    }
    if (key == ".") {
        return if (current.contains(".")) current else "$current."
    }
    return if (current == "0") key else current + key
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    isSubtext: Boolean = false,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (label.isNotEmpty()) {
            Text(
                label,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = if (isSubtext) 12.sp else 14.sp
            )
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }
        
        Text(
            value,
            fontWeight = if (isHighlight) FontWeight.ExtraBold else if (isSubtext) FontWeight.Normal else FontWeight.Bold,
            fontSize = if (isHighlight) 18.sp else if (isSubtext) 12.sp else 14.sp,
            color = if (isHighlight) ZenPrimary else if (isSubtext) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
        )
    }
}

data class Contact(
    val name: String,
    val phone: String?,
    val icon: ImageVector,
    val isAction: Boolean = false
)
