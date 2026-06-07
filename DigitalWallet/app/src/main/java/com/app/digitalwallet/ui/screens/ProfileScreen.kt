package com.app.digitalwallet.ui.screens

import android.content.ClipData
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.app.digitalwallet.api.dto.UserResponse
import com.app.digitalwallet.auth.TokenManager
import com.app.digitalwallet.di.NetworkModule
import com.app.digitalwallet.navigation.Screen
import com.app.digitalwallet.ui.components.ProfileOption
import com.app.digitalwallet.ui.theme.ZenPrimary
import com.app.digitalwallet.viewmodel.AuthViewModel
import com.app.digitalwallet.viewmodel.WalletUiState
import com.app.digitalwallet.viewmodel.WalletViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

import com.app.digitalwallet.viewmodel.AuthUiEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
    walletViewModel: WalletViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val walletState by walletViewModel.uiState.collectAsState()
    val uploadState = authViewModel.uploadProfileImageUiState
    val userProfile = authViewModel.userProfile
    val userPhone = remember { TokenManager.getInstance(context).getUserPhone() ?: "User" }

    // Handle UI effects
    LaunchedEffect(authViewModel) {
        authViewModel.uiEffect.collect { effect ->
            when (effect) {
                is AuthUiEffect.ProfileUpdated -> {
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    val fullImageUrl = remember(userProfile?.profileImageUrl) {
        val path = userProfile?.profileImageUrl
        if (path != null && path.startsWith("/")) {
            "${NetworkModule.BASE_HOST}$path"
        } else {
            path
        }
    }
    
    var showLogoutDialog by remember { mutableStateOf(false) }

    val walletId = (walletState as? WalletUiState.Success)?.walletInfo?.walletId ?: "..."

    LaunchedEffect(Unit) {
        authViewModel.getMe()
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val file = File(context.cacheDir, "profile_upload.jpg")
                    context.contentResolver.openInputStream(it)?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
                    
                    authViewModel.updateProfileImage(body)
                } catch (e: Exception) {
                    Toast.makeText(context, "Error processing image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile", 
                        color = MaterialTheme.colorScheme.onSurface, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 20.sp
                    )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Profile Picture & Name
            Box(
                modifier = Modifier.clickable { photoLauncher.launch("image/*") }
            ) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (uploadState.isLoading) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = ZenPrimary)
                        }
                    } else if (!fullImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = fullImageUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .padding(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.BottomEnd),
                    shape = CircleShape,
                    color = ZenPrimary,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Change Photo",
                        modifier = Modifier.padding(6.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            val displayName = if (userProfile != null) {
                "${userProfile.firstName} ${userProfile.lastName}"
            } else {
                userPhone
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    displayName,
                    fontSize = 22.sp, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Text(
                "Digital Wallet User", 
                color = MaterialTheme.colorScheme.secondary, 
                fontSize = 14.sp
            )

            if (uploadState.error != null) {
                Text(
                    text = uploadState.error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Wallet ID Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Your Wallet ID",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            walletId,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { 
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Wallet ID", walletId)))
                        }
                    }) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy ID",
                            modifier = Modifier.size(20.dp),
                            tint = ZenPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Profile Sections
            ProfileSectionHeader("Account")
            ProfileOption(
                icon = Icons.Outlined.Person,
                title = "Personal Information",
                onClick = { navController.navigate(Screen.PersonalInfo.route) }
            )
            ProfileOption(
                icon = Icons.Outlined.AccountBalanceWallet,
                title = "Payment Methods",
                onClick = { navController.navigate(Screen.PaymentMethods.route) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            ProfileSectionHeader("Preferences")
            ProfileOption(
                icon = Icons.Outlined.Notifications,
                title = "Notifications",
                onClick = { navController.navigate(Screen.Notifications.route) }
            )
            ProfileOption(
                icon = Icons.Outlined.Security,
                title = "Security & Privacy",
                onClick = { navController.navigate(Screen.Security.route) }
            )
            ProfileOption(
                icon = Icons.Outlined.VerifiedUser,
                title = "Identity Verification",
                onClick = { navController.navigate(Screen.IdentityVerification.route) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            ProfileSectionHeader("Support")
            ProfileOption(
                icon = Icons.Default.Info,
                title = "Help Center",
                onClick = { navController.navigate(Screen.HelpCenter.route) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 4. Profile Options (Logout)
            Spacer(modifier = Modifier.height(16.dp))
            ProfileOption(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                title = "Logout",
                onClick = { showLogoutDialog = true },
                showArrow = false
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out of your account?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Logout", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileSectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ZenPrimary,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
