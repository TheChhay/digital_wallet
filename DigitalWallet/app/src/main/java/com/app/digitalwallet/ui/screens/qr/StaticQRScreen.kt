package com.app.digitalwallet.ui.screens.qr

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.digitalwallet.data.remote.dto.StaticQRResponse
import com.app.digitalwallet.ui.theme.ZenPrimary
import com.app.digitalwallet.ui.screens.qr.QRViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaticQRScreen(
    viewModel: QRViewModel,
    walletAddress: String,
    onBack: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(walletAddress) {
        viewModel.getStaticQR(walletAddress)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My QR Code") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .size(280.dp)
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (val state = uiState) {
                        is QRUiState.Loading -> {
                            CircularProgressIndicator(color = ZenPrimary)
                        }
                        is QRUiState.Success<*> -> {
                            val data = state.data as? StaticQRResponse
                            if (data != null) {
                                val qrBitmap = remember(data.qrImageBase64) {
                                    try {
                                        val bytes = Base64.decode(data.qrImageBase64, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap,
                                        contentDescription = "Static QR Code",
                                        modifier = Modifier.size(240.dp)
                                    )
                                } else {
                                    Text("Failed to decode QR", color = MaterialTheme.colorScheme.error)
                                }
                            } else {
                                Text("No QR Data Found", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        is QRUiState.Error -> {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        else -> {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Your Wallet Address",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = walletAddress,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(walletAddress))
                    // Show snackbar or toast
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Address")
            }
        }
    }
}
