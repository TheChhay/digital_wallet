package com.app.digitalwallet.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.digitalwallet.auth.TokenManager
import com.app.digitalwallet.ui.components.PrimaryButton
import com.app.digitalwallet.ui.components.ZenTextField
import com.app.digitalwallet.ui.theme.ZenPrimary
import com.app.digitalwallet.utils.PhoneNumberUtils
import com.app.digitalwallet.viewmodel.QRViewModel
import com.app.digitalwallet.viewmodel.TransferStatus
import com.app.digitalwallet.viewmodel.WalletUiState
import com.app.digitalwallet.viewmodel.WalletViewModel
import java.util.Locale

import com.app.digitalwallet.viewmodel.WalletUiEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    qrViewModel: QRViewModel,
    viewModel: WalletViewModel,
    onBack: () -> Unit,
    onNavigateToSuccess: (String, String) -> Unit
) {
    val payment by qrViewModel.pendingPayment.collectAsState()

    var note by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmedRecipientName by remember { mutableStateOf("") }
    var confirmedRecipientPhone by remember { mutableStateOf("") }
    var isLookingUpRecipient by remember { mutableStateOf(false) }
    var showRecipientNotFoundError by remember { mutableStateOf(false) }
    var showSelfTransferError by remember { mutableStateOf(false) }
    var showAdminTransferError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val currentUserPhone = remember { TokenManager.getInstance(context).getUserPhone() }

    val uiState by viewModel.uiState.collectAsState()
    val transferStatus by viewModel.transferStatus.collectAsState()
    val balance = (uiState as? WalletUiState.Success)?.walletInfo?.balance ?: 0.0

    // --- QR auto-fill ---
    // When arriving from QR scan, pendingPayment is set.
    // amount comes from QR if present, otherwise starts at "0"
    var amount by remember {
        mutableStateOf(payment?.amount?.let {
            if (it > 0) it.toBigDecimal().stripTrailingZeros().toPlainString() else "0"
        } ?: "0")
    }

    // When coming from QR scan, recipient is already validated — no phone lookup needed.
    // The Continue button is enabled if QR payment is pending OR manual entry is valid.
    val isFromQR = payment != null
    val hasRecipient = isFromQR || phoneNumber.isNotEmpty()

    // Handle UI effects
    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is WalletUiEffect.TransferSuccess -> {
                    val displayName = when {
                        confirmedRecipientName.isNotEmpty() && confirmedRecipientName != "null null" -> confirmedRecipientName
                        isFromQR -> payment?.recipientName ?: "QR Recipient"
                        else -> confirmedRecipientPhone
                    }
                    qrViewModel.clearPendingPayment()
                    showConfirmDialog = false
                    onNavigateToSuccess(amount, displayName)
                }
                is WalletUiEffect.ShowError -> {
                    // Handled via transferStatus
                }
                else -> {} // Ignore other effects like DepositSuccess, WithdrawSuccess
            }
        }
    }

    // If QR data arrives (e.g. screen re-entered), sync amount and recipient
    LaunchedEffect(payment) {
        payment?.let { p ->
            // Pre-fill amount from QR if it carries one
            if ((p.amount ?: 0.0) > 0.0) {
                amount = p.amount!!.toBigDecimal().stripTrailingZeros().toPlainString()
            }
            // Pre-fill confirmed recipient so Continue can skip lookup
            p.recipientName?.let { name ->
                if (name.isNotBlank()) {
                    confirmedRecipientName = name
                }
            }
            // Pre-fill phone number from QR
            p.recipientPhone?.let { phone ->
                if (phone.isNotBlank()) {
                    phoneNumber = phone
                    confirmedRecipientPhone = phone
                }
            }
        } ?: run {
            // If payment is cleared (e.g. user manually cleared it), reset fields
            // but only if they were filled from QR (this might need refinement)
            // For now, we only care about filling it once
        }
    }

    val amountDouble = amount.toDoubleOrNull() ?: 0.0
    val isInsufficientFunds = amountDouble > balance
    val isAmountValid = amountDouble > 0 && !isInsufficientFunds

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer Money", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        // Clear QR state when user goes back from this screen
                        qrViewModel.clearPendingPayment()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp)
                        )
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

                // --- QR recipient banner (shown when arriving from QR scan) ---
                if (isFromQR) {
                    val displayName = payment?.recipientName?.takeIf { it.isNotBlank() } ?: "QR Recipient"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ZenPrimary.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = ZenPrimary.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = ZenPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Paying via QR",
                                    fontSize = 11.sp,
                                    color = ZenPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    displayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val subDetails = listOfNotNull(
                                    payment?.recipientPhone,
                                    payment?.currency
                                ).joinToString(" • ")
                                if (subDetails.isNotBlank()) {
                                    Text(
                                        subDetails,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            // Allow user to dismiss QR and enter manually
                            IconButton(onClick = { qrViewModel.clearPendingPayment() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove QR recipient",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    // Manual entry — show phone field
                    ZenTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        placeholder = "Enter phone number",
                        leadingIcon = {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                tint = ZenPrimary
                            )
                        },
                        isOutlined = false
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Amount Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        listOf("10", "50", "100", "500").forEach { quickAmount ->
                            SuggestionChip(
                                onClick = { amount = quickAmount },
                                label = { Text("+$$quickAmount") },
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
                            color = if (isInsufficientFunds) Color.Red
                            else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    AnimatedVisibility(visible = isInsufficientFunds, enter = fadeIn(), exit = fadeOut()) {
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
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Balance: $${String.format(Locale.US, "%,.2f", balance)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                ZenTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Transfer Note (Optional)",
                    placeholder = "What's this for?",
                    leadingIcon = {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = ZenPrimary)
                    },
                    isOutlined = true
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Fixed Bottom: Keypad + Button
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                NumericKeypad(onKeyPress = { key -> amount = updateAmount(amount, key) })

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    PrimaryButton(
                        text = if (isLookingUpRecipient) "Checking..." else "Continue",
                        onClick = {
                            when {
                                // QR path: recipient already validated, skip lookup
                                isFromQR -> {
                                    val targetPhone = payment?.recipientPhone?.let { PhoneNumberUtils.normalize(it) }
                                    if (targetPhone != null && targetPhone == currentUserPhone) {
                                        showSelfTransferError = true
                                        return@PrimaryButton
                                    }
                                    confirmedRecipientName = payment?.recipientName ?: ""
                                    confirmedRecipientPhone = payment?.recipientPhone ?: ""
                                    showConfirmDialog = true
                                }

                                // Manual phone entry
                                else -> {
                                    val normalizedInput = PhoneNumberUtils.normalize(phoneNumber)
                                    if (normalizedInput == currentUserPhone) {
                                        showSelfTransferError = true
                                        return@PrimaryButton
                                    }
                                    isLookingUpRecipient = true
                                    viewModel.lookupRecipient(normalizedInput) { result ->
                                        isLookingUpRecipient = false
                                        when {
                                            result == null -> showRecipientNotFoundError = true
                                            else -> {
                                                confirmedRecipientName = result.fullName
                                                confirmedRecipientPhone = normalizedInput
                                                showConfirmDialog = true
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        icon = if (isLookingUpRecipient) null else Icons.AutoMirrored.Filled.ArrowForward,
                        enabled = !isLookingUpRecipient && isAmountValid && hasRecipient
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

    // Confirm Dialog
    if (showConfirmDialog) {
        val displayName = when {
            confirmedRecipientName.isNotEmpty() && confirmedRecipientName != "null null" -> confirmedRecipientName
            isFromQR -> payment?.recipientName ?: "QR Recipient"
            else -> confirmedRecipientPhone
        }

        Dialog(onDismissRequest = {
            if (transferStatus !is TransferStatus.Loading) showConfirmDialog = false
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
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = ZenPrimary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = ZenPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Transfer Receipt", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(
                        "Verify transaction details",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            ReceiptRow("To", displayName)
                            if (!isFromQR && confirmedRecipientPhone.isNotEmpty() && displayName != confirmedRecipientPhone) {
                                ReceiptRow("", confirmedRecipientPhone, isSubtext = true)
                            }
                            if (isFromQR) {
                                ReceiptRow("", "via QR Code", isSubtext = true)
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            ReceiptRow(
                                "Amount",
                                "$${String.format(Locale.US, "%,.2f", amountDouble)}",
                                isHighlight = true
                            )
                            ReceiptRow("Fee", "$0.00", isSubtext = true)

                            if (note.isNotBlank()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                Text(
                                    "Note",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(note, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    PrimaryButton(
                        text = "Confirm & Send",
                        onClick = {
                            if (isFromQR) {
                                viewModel.transferMoney(
                                    phone = payment?.recipientPhone,
                                    walletId = payment?.recipientId,
                                    amount = amountDouble,
                                    note = note
                                )
                            } else {
                                viewModel.transferMoney(phone = confirmedRecipientPhone, amount = amountDouble, note = note)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = transferStatus !is TransferStatus.Loading
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            showConfirmDialog = false
                            viewModel.resetTransferStatus()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = transferStatus !is TransferStatus.Loading
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }

    if (showRecipientNotFoundError) {
        AlertDialog(
            onDismissRequest = { showRecipientNotFoundError = false },
            title = { Text("User Not Found") },
            text = { Text("We couldn't find a user with phone number $phoneNumber. Please check and try again.") },
            confirmButton = {
                TextButton(onClick = { showRecipientNotFoundError = false }) { Text("OK") }
            }
        )
    }

    if (showSelfTransferError) {
        AlertDialog(
            onDismissRequest = { showSelfTransferError = false },
            title = { Text("Invalid Transfer") },
            text = { Text("You cannot transfer money to yourself.") },
            confirmButton = {
                TextButton(onClick = { showSelfTransferError = false }) { Text("OK") }
            }
        )
    }

    if (showAdminTransferError) {
        AlertDialog(
            onDismissRequest = { showAdminTransferError = false },
            title = { Text("Transfer Restricted") },
            text = { Text("Transfers to administrator accounts are not allowed for security reasons.") },
            confirmButton = {
                TextButton(onClick = { showAdminTransferError = false }) { Text("OK") }
            }
        )
    }

    if (transferStatus is TransferStatus.Error) {
        val errorMessage = (transferStatus as TransferStatus.Error).message
        AlertDialog(
            onDismissRequest = { viewModel.resetTransferStatus() },
            title = { Text("Transfer Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetTransferStatus() }) { Text("OK") }
            }
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
        color = MaterialTheme.colorScheme.surface,
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
                                Icon(
                                    Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Delete",
                                    tint = ZenPrimary
                                )
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
    if (key == "backspace") return if (current.length > 1) current.dropLast(1) else "0"
    if (key == ".") return if (current.contains(".")) current else "$current."
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
            fontWeight = if (isHighlight) FontWeight.ExtraBold
            else if (isSubtext) FontWeight.Normal
            else FontWeight.Bold,
            fontSize = if (isHighlight) 18.sp else if (isSubtext) 12.sp else 14.sp,
            color = if (isHighlight) ZenPrimary
            else if (isSubtext) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

data class Contact(
    val name: String,
    val phone: String?,
    val icon: ImageVector,
    val isAction: Boolean = false
)
