package com.app.digitalwallet.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.app.digitalwallet.api.dto.RecipientLookupDto
import com.app.digitalwallet.viewmodel.QRViewModel
import com.app.digitalwallet.viewmodel.WalletViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "QRScanner"

// How long to wait before allowing another scan attempt (ms)
private const val SCAN_COOLDOWN_MS = 2000L

@ExperimentalGetImage
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraQRScannerScreen(
    viewModel: QRViewModel,
    walletViewModel: WalletViewModel,
    onNavigateToPayment: (recipientLookupDto: RecipientLookupDto) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val isScanningEnabled = remember { AtomicBoolean(true) }
    val lastScanTime = remember { AtomicLong(0L) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        when {
            cameraPermissionState.status.isGranted -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    CameraPreview(
                        lifecycleOwner = lifecycleOwner,
                        isScanningEnabled = isScanningEnabled,
                        lastScanTime = lastScanTime,
                        scope = scope,
                        viewModel = viewModel,
                        walletViewModel = walletViewModel,
                        context = context,
                        onNavigateToPayment = onNavigateToPayment
                    )
                    ScannerOverlay()
                }
            }

            cameraPermissionState.status.shouldShowRationale -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Camera access needed",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "We need your camera to scan QR codes for payments.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            Text("Allow Camera")
                        }
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Camera permission denied",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Please enable camera access in your device settings to scan QR codes.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }) {
                            Text("Open Settings")
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalGetImage
@Composable
private fun CameraPreview(
    lifecycleOwner: LifecycleOwner,
    isScanningEnabled: AtomicBoolean,
    lastScanTime: AtomicLong,
    scope: CoroutineScope,
    viewModel: QRViewModel,
    walletViewModel: WalletViewModel,
    context: android.content.Context,
    onNavigateToPayment: (RecipientLookupDto) -> Unit
) {
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val mainExecutor = ContextCompat.getMainExecutor(ctx)
            val analysisExecutor = Executors.newSingleThreadExecutor()
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val scanner = BarcodeScanning.getClient()

                val resolutionSelector = androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        androidx.camera.core.resolutionselector.ResolutionStrategy(
                            Size(1280, 720),
                            androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image

                            if (mediaImage == null || !isScanningEnabled.get()) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            val now = System.currentTimeMillis()
                            if (now - lastScanTime.get() < SCAN_COOLDOWN_MS) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    val rawValue = barcodes.firstOrNull()?.rawValue
                                    if (rawValue != null && isScanningEnabled.compareAndSet(true, false)) {
                                        lastScanTime.set(System.currentTimeMillis())
                                        scope.launch {
                                            handleScannedResult(
                                                value = rawValue,
                                                viewModel = viewModel,
                                                walletViewModel = walletViewModel,
                                                context = context,
                                                isScanningEnabled = isScanningEnabled,
                                                onNavigateToPayment = onNavigateToPayment
                                            )
                                        }
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, mainExecutor)

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

private suspend fun handleScannedResult(
    value: String,
    viewModel: QRViewModel,
    walletViewModel: WalletViewModel,
    context: android.content.Context,
    isScanningEnabled: AtomicBoolean,
    onNavigateToPayment: (RecipientLookupDto) -> Unit
) {
    // FIX: Strip BOM and any surrounding whitespace/invisible characters before any checks
    val trimmedValue = value.trim().trimStart('\uFEFF')

    Log.d(TAG, "Raw scanned value: '$trimmedValue'")
    Log.d(TAG, "Length: ${trimmedValue.length}")
    Log.d(TAG, "Starts with '{': ${trimmedValue.startsWith("{")}")
    Log.d(TAG, "Ends with '}':   ${trimmedValue.endsWith("}")}")

    val json = Json { ignoreUnknownKeys = true }

    try {
        when {
            // Case 1: JSON format — FIX: use cleaned trimmedValue for the check
            trimmedValue.startsWith("{") && trimmedValue.endsWith("}") -> {
                Log.d(TAG, "Entering JSON branch")

                val jsonObject = json.parseToJsonElement(trimmedValue).jsonObject
                Log.d(TAG, "Top-level JSON keys: ${jsonObject.keys}")

                // FIX: Handle wrapped API response { "success": true, "data": { ... } }
                // by unwrapping the "data" field if it contains the actual payload
                val rootData = jsonObject["data"]?.takeIf { it is JsonObject }?.jsonObject
                val targetObject = if (rootData != null) {
                    Log.d(TAG, "Found wrapped API response, unwrapping 'data' field")
                    Log.d(TAG, "data keys: ${rootData.keys}")
                    rootData
                } else {
                    jsonObject
                }

                val token = targetObject["token"]?.jsonPrimitive?.contentOrNull
                val address = targetObject["address"]?.jsonPrimitive?.contentOrNull
                val nestedData = targetObject["data"]?.takeIf { it is JsonObject }?.jsonObject

                // Also check if the target object itself looks like recipient data
                val directFullName = targetObject["full_name"]?.jsonPrimitive?.contentOrNull
                val directPhone = targetObject["phone"]?.jsonPrimitive?.contentOrNull
                val directId = targetObject["id"]?.jsonPrimitive?.contentOrNull

                Log.d(TAG, "token=$token, address=$address, nestedData=$nestedData")
                Log.d(TAG, "directFullName=$directFullName, directPhone=$directPhone, directId=$directId")

                when {
                    // FIX: If the unwrapped object directly contains recipient fields, use them
                    directFullName != null -> {
                        Log.d(TAG, "Navigating with direct recipient fields from unwrapped data")
                        onNavigateToPayment(
                            RecipientLookupDto(
                                fullName = directFullName,
                                phone = directPhone,
                                id = directId
                            )
                        )
                    }

                    nestedData != null -> {
                        val fullName = nestedData["full_name"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
                        val phone = nestedData["phone"]?.jsonPrimitive?.contentOrNull
                        val id = nestedData["id"]?.jsonPrimitive?.contentOrNull
                        Log.d(TAG, "Navigating with nested data: fullName=$fullName, phone=$phone, id=$id")
                        onNavigateToPayment(
                            RecipientLookupDto(
                                fullName = fullName,
                                phone = phone,
                                id = id
                            )
                        )
                    }

                    token != null -> {
                        Log.d(TAG, "Validating token: $token")
                        val response = viewModel.validateScannedTokenAsync(token)
                        if (response != null) {
                            onNavigateToPayment(
                                RecipientLookupDto(
                                    fullName = response.recipientName ?: "Unknown",
                                    phone = response.recipientPhone,
                                    id = null
                                )
                            )
                        } else {
                            Log.d(TAG, "Token validation failed")
                            Toast.makeText(context, "Invalid QR token", Toast.LENGTH_SHORT).show()
                            isScanningEnabled.set(true)
                        }
                    }

                    address != null -> {
                        Log.d(TAG, "Resolving address: $address")
                        val result = viewModel.resolveRecipient(walletId = address, walletViewModel = walletViewModel)
                        if (result != null) {
                            onNavigateToPayment(
                                RecipientLookupDto(
                                    fullName = result.fullName,
                                    phone = result.phone,
                                    id = result.id
                                )
                            )
                        } else {
                            Log.d(TAG, "Address resolve failed")
                            Toast.makeText(context, "Recipient not found", Toast.LENGTH_SHORT).show()
                            isScanningEnabled.set(true)
                        }
                    }

                    else -> {
                        Log.d(TAG, "JSON had no recognized fields")
                        Toast.makeText(context, "Unrecognized QR data format", Toast.LENGTH_SHORT).show()
                        isScanningEnabled.set(true)
                    }
                }
            }

            // Case 2: Deep link — token
            trimmedValue.startsWith("walletapp://pay?token=") -> {
                val token = trimmedValue.substringAfter("token=")
                Log.d(TAG, "Deep link token: $token")
                val response = viewModel.validateScannedTokenAsync(token)
                if (response != null) {
                    onNavigateToPayment(
                        RecipientLookupDto(
                            fullName = response.recipientName ?: "Unknown",
                            phone = response.recipientPhone,
                            id = null
                        )
                    )
                } else {
                    Toast.makeText(context, "Invalid QR token", Toast.LENGTH_SHORT).show()
                    isScanningEnabled.set(true)
                }
            }

            // Case 3: Deep link — wallet address (two formats)
            trimmedValue.startsWith("walletapp://address=") || trimmedValue.startsWith("walletapp://address?wallet=") -> {
                val address = if (trimmedValue.contains("wallet=")) {
                    trimmedValue.substringAfter("wallet=")
                } else {
                    trimmedValue.substringAfter("address=")
                }
                Log.d(TAG, "Deep link address: $address")
                val result = viewModel.resolveRecipient(walletId = address, walletViewModel = walletViewModel)
                if (result != null) {
                    onNavigateToPayment(
                        RecipientLookupDto(
                            fullName = result.fullName,
                            phone = result.phone,
                            id = result.id
                        )
                    )
                } else {
                    Toast.makeText(context, "Recipient not found", Toast.LENGTH_SHORT).show()
                    isScanningEnabled.set(true)
                }
            }

            // Case 4: Raw phone number or wallet address
            else -> {
                Log.d(TAG, "Falling through to raw phone/wallet branch")
                // Heuristic: UUIDs / wallet addresses are typically longer than 20 chars
                val result = if (trimmedValue.length > 20) {
                    viewModel.resolveRecipient(walletId = trimmedValue, walletViewModel = walletViewModel)
                } else {
                    viewModel.resolveRecipient(phone = trimmedValue, walletViewModel = walletViewModel)
                }

                if (result != null) {
                    onNavigateToPayment(
                        RecipientLookupDto(
                            fullName = result.fullName,
                            phone = result.phone ?: trimmedValue,
                            id = result.id
                        )
                    )
                } else {
                    Toast.makeText(context, "Recipient not found. Try entering details manually.", Toast.LENGTH_SHORT).show()
                    isScanningEnabled.set(true)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e(TAG, "Exception processing QR: ${e.message}")
        Toast.makeText(context, "Error processing QR code", Toast.LENGTH_SHORT).show()
        isScanningEnabled.set(true)
    }
}

@Composable
fun ScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 4.dp.toPx()
        val cornerRadius = 16.dp.toPx()
        val boxSize = 250.dp.toPx()
        val left = (size.width - boxSize) / 2
        val top = (size.height - boxSize) / 2
        val rect = Rect(left, top, left + boxSize, top + boxSize)

        val cutoutPath = Path().apply {
            addRoundRect(RoundRect(rect, CornerRadius(cornerRadius)))
        }
        clipPath(cutoutPath, clipOp = ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.6f))
        }
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(boxSize, boxSize),
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = strokeWidth)
        )
    }
}