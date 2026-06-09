package com.app.digitalwallet.ui.screens.qr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.digitalwallet.data.remote.dto.GenerateQRResponse
import com.app.digitalwallet.ui.theme.ZenPrimary
import com.app.digitalwallet.ui.screens.qr.QRUiState
import com.app.digitalwallet.ui.screens.qr.QRViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicQRScreen(
    viewModel: QRViewModel,
    walletId: String,
    onBack: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val timerProgress by viewModel.timerProgress.collectAsState()

    // Stop QR generation loop when the screen is disposed (navigating back or away)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopQRGeneration()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Payment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Enter Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("$") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { 
                    if (amount.isNotEmpty()) {
                        viewModel.generateDynamicQR(walletId, amount.toDouble(), "USD")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.isNotEmpty() && uiState !is QRUiState.Loading
            ) {
                Text("Generate QR Code")
            }

            Spacer(modifier = Modifier.height(32.dp))

            when (val state = uiState) {
                is QRUiState.Loading -> {
                    CircularProgressIndicator(color = ZenPrimary)
                }
                is QRUiState.Success<*> -> {
                    val data = state.data as? GenerateQRResponse
                    if (data != null) {
                        // Decode Base64 to Bitmap for reliable display
                        val bitmap = remember(data.qrImageBase64) {
                            try {
                                // Strip potential header if present (e.g. data:image/png;base64,)
                                val pureBase64 = data.qrImageBase64.substringAfter(",")
                                val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (bitmap != null) {
                            QRDisplay(
                                bitmap = bitmap,
                                timerProgress = timerProgress,
                                onRefresh = { viewModel.generateDynamicQR(walletId, amount.toDouble(), "USD") }
                            )
                        } else {
                            Text(text = "Failed to decode QR image", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                is QRUiState.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
                else -> {}
            }
        }
    }
}

@Composable
fun QRDisplay(
    bitmap: Bitmap,
    timerProgress: Float,
    onRefresh: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.size(280.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Dynamic QR Code",
                    modifier = Modifier.size(240.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Expires in: ${(timerProgress * 60).toInt()}s",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { timerProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = ZenPrimary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh Manually")
        }
    }
}
