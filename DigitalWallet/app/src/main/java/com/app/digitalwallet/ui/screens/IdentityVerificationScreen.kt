// D:/mobile app project/digital_wallet/DigitalWallet/app/src/main/java/com/app/digitalwallet/ui/screens/IdentityVerificationScreen.kt

package com.app.digitalwallet.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.app.digitalwallet.ui.components.IDUploadCard
import com.app.digitalwallet.ui.components.PrimaryButton
import com.app.digitalwallet.ui.components.ZenTextField
import com.app.digitalwallet.viewmodel.KYCViewModel
import com.app.digitalwallet.viewmodel.KycUiState
import com.app.digitalwallet.viewmodel.KycUiEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ─────────────────────────────────────────────────────────────
// KYC Status enum — maps server strings to typed values
// ─────────────────────────────────────────────────────────────
enum class KycStatus(val value: String) {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    NOT_SUBMITTED("not_submitted"),
    UNKNOWN("unknown");

    companion object {
        fun from(value: String?): KycStatus =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}

// ─────────────────────────────────────────────────────────────
// Identity Verification Form Screen
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityVerificationScreen(
    onBack: () -> Unit,
    onProceed: () -> Unit,
    viewModel: KYCViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState = viewModel.kycUiState

    // If already submitted, show the status screen instead of the form
    if (uiState is KycUiState.Loaded) {
        val status = KycStatus.from(uiState.data.status)
        if (status != KycStatus.NOT_SUBMITTED) {
            IdentityVerified(
                status = status,
                fullName = uiState.data.fullName,
                rejectionReason = uiState.data.rejectionReason,
                onBack = onBack,
                onResubmit = { viewModel.resetKycState() }
            )
            return
        }
    }

    val idCardPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? -> viewModel.idCardUri = uri }

    val selfiePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? -> viewModel.selfieUri = uri }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        viewModel.dob = formatter.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Initial check for KYC status and navigation event
    LaunchedEffect(viewModel) {
        viewModel.getKYC()
        viewModel.uiEvent.collect { event ->
            when (event) {
                is KycUiEvent.VerificationSuccess -> onProceed()
                is KycUiEvent.ShowError -> {
                    // Errors are already handled by showing errorMessage from uiState
                }
            }
        }
    }

    val isLoading = uiState is KycUiState.Loading
    val errorMessage = (uiState as? KycUiState.Error)?.message

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Identity Verification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorScheme.surface,
                    titleContentColor = colorScheme.onSurface
                )
            )
        },
        containerColor = colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Identity Check",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "To comply with financial regulations, please provide clear photos of your official ID and a quick selfie.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── ID Card Upload ──
            Text(
                "Upload Documents",
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        width = 1.dp,
                        color = if (viewModel.idCardUri != null) colorScheme.primary
                        else colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                IDUploadCard(
                    title = if (viewModel.idCardUri != null) "ID Card Captured" else "ID Card Front",
                    subtitle = if (viewModel.idCardUri != null) "Tap to change image"
                    else "Tap to capture or upload",
                    icon = if (viewModel.idCardUri != null) Icons.Default.CheckCircle
                    else Icons.Outlined.Badge,
                    onClick = { idCardPicker.launch("image/*") }
                )
                if (viewModel.idCardUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(viewModel.idCardUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.2f
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Selfie Upload ──
            Text(
                "Live Selfie",
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        width = 1.dp,
                        color = if (viewModel.selfieUri != null) colorScheme.primary
                        else colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                IDUploadCard(
                    title = if (viewModel.selfieUri != null) "Selfie Captured" else "Live Selfie",
                    subtitle = if (viewModel.selfieUri != null) "Tap to change image"
                    else "Verify it's really you with a quick photo",
                    icon = if (viewModel.selfieUri != null) Icons.Default.CheckCircle
                    else Icons.Outlined.Face,
                    onClick = { selfiePicker.launch("image/*") }
                )
                if (viewModel.selfieUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(viewModel.selfieUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.2f
                    )
                }
                // Live badge
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.Red, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "LIVE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Personal Details ──
            Text(
                "Personal Details",
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            ZenTextField(
                value = viewModel.fullName,
                onValueChange = { viewModel.fullName = it },
                label = "FULL NAME (AS ON ID)",
                placeholder = "John Doe",
                isOutlined = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            ZenTextField(
                value = viewModel.dob,
                onValueChange = { viewModel.dob = it },
                label = "DATE OF BIRTH",
                placeholder = "YYYY-MM-DD",
                isOutlined = true,
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Select Date",
                            tint = colorScheme.primary
                        )
                    }
                },
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showDatePicker = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ZenTextField(
                value = viewModel.address,
                onValueChange = { viewModel.address = it },
                label = "RESIDENTIAL ADDRESS",
                placeholder = "123 Financial District, Suite 400",
                minLines = 3,
                isOutlined = true
            )

            // ── Error message (from KycUiState.Error) ──
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = it,
                                color = colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Security Notice ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (isSystemInDarkTheme()) colorScheme.surfaceVariant
                else Color(0xFFE8F5E9)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isSystemInDarkTheme()) colorScheme.primary
                        else Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Your data is encrypted with 256-bit AES protection. We never share your personal information with unauthorized third parties.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSystemInDarkTheme()) colorScheme.onSurface
                        else Color(0xFF2E7D32),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            PrimaryButton(
                text = if (isLoading) "Submitting..." else "Proceed for Verification",
                onClick = { viewModel.submitKYC() },
                enabled = !isLoading,
                icon = Icons.AutoMirrored.Filled.ArrowForward
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Identity Verified / Status Screen
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityVerified(
    status: KycStatus,
    fullName: String? = null,
    rejectionReason: String? = null,
    onBack: () -> Unit,
    onResubmit: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    // Visual config per status
    val config = when (status) {
        KycStatus.APPROVED -> StatusConfig(
            icon = Icons.Default.CheckCircle,
            iconTint = Color(0xFF2E7D32),
            iconBg = Color(0xFFE8F5E9),
            title = "Verification Approved!",
            subtitle = buildString {
                append("Congratulations")
                if (!fullName.isNullOrBlank()) append(", $fullName")
                append("! Your identity has been successfully verified. You now have full access to all wallet features.")
            },
            badge = "APPROVED",
            badgeColor = Color(0xFF2E7D32),
            badgeBg = Color(0xFFE8F5E9),
            showResubmit = false
        )
        KycStatus.PENDING -> StatusConfig(
            icon = Icons.Outlined.HourglassTop,
            iconTint = Color(0xFFB45309),
            iconBg = Color(0xFFFFF8E1),
            title = "Under Review",
            subtitle = "Your documents have been submitted and are currently being reviewed by our compliance team. This usually takes 1–2 business days. We'll notify you once the review is complete.",
            badge = "PENDING",
            badgeColor = Color(0xFFB45309),
            badgeBg = Color(0xFFFFF8E1),
            showResubmit = false
        )
        KycStatus.REJECTED -> StatusConfig(
            icon = Icons.Default.Warning,
            iconTint = colorScheme.error,
            iconBg = colorScheme.errorContainer,
            title = "Verification Failed",
            subtitle = "Unfortunately we were unable to verify your identity with the documents provided. Please review the reason below and resubmit with the correct information.",
            badge = "REJECTED",
            badgeColor = colorScheme.error,
            badgeBg = colorScheme.errorContainer,
            showResubmit = true
        )
        KycStatus.NOT_SUBMITTED -> StatusConfig(
            icon = Icons.Outlined.Badge,
            iconTint = colorScheme.primary,
            iconBg = colorScheme.primaryContainer,
            title = "Not Submitted",
            subtitle = "You haven't submitted your identity verification documents yet.",
            badge = "NOT SUBMITTED",
            badgeColor = colorScheme.primary,
            badgeBg = colorScheme.primaryContainer,
            showResubmit = true
        )
        KycStatus.UNKNOWN -> StatusConfig(
            icon = Icons.Default.Warning,
            iconTint = colorScheme.onSurfaceVariant,
            iconBg = colorScheme.surfaceVariant,
            title = "Status Unknown",
            subtitle = "We could not determine your verification status. Please try again or contact support.",
            badge = "UNKNOWN",
            badgeColor = colorScheme.onSurfaceVariant,
            badgeBg = colorScheme.surfaceVariant,
            showResubmit = false
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Verification Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        },
        containerColor = colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Status Icon ──
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(config.iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = config.icon,
                    contentDescription = null,
                    tint = config.iconTint,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Status Badge ──
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = config.badgeBg
            ) {
                Text(
                    text = config.badge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = config.badgeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Title ──
            Text(
                text = config.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Subtitle ──
            Text(
                text = config.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            // ── Rejection Reason (only for rejected) ──
            if (status == KycStatus.REJECTED && !rejectionReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.errorContainer
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Reason for Rejection",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = rejectionReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onErrorContainer,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ── What happens next (for pending) ──
            if (status == KycStatus.PENDING) {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "What happens next?",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf(
                            "Our team reviews your documents",
                            "You'll receive a push notification",
                            "Full wallet access granted on approval"
                        ).forEachIndexed { i, step ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${i + 1}",
                                        color = colorScheme.onPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // ── Actions ──
            if (config.showResubmit) {
                PrimaryButton(
                    text = "Resubmit Documents",
                    onClick = onResubmit,
                    icon = Icons.AutoMirrored.Filled.ArrowForward
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text("Back to Home")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Internal helper — keeps config off the composable
// ─────────────────────────────────────────────────────────────
private data class StatusConfig(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val title: String,
    val subtitle: String,
    val badge: String,
    val badgeColor: Color,
    val badgeBg: Color,
    val showResubmit: Boolean
)