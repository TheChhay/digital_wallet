package com.app.digitalwallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.digitalwallet.ui.components.IDUploadCard
import com.app.digitalwallet.ui.components.PrimaryButton
import com.app.digitalwallet.ui.components.ZenTextField
import com.app.digitalwallet.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityVerificationScreen(onBack: () -> Unit, onProceed: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("ZenWallet", color = ZenBlue, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ZenBlue)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help", tint = ZenGray)
                    }
                    Surface(
                        modifier = Modifier.padding(end = 8.dp).size(32.dp),
                        shape = CircleShape,
                        color = Color.LightGray
                    ) {
                        // Placeholder for profile image
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFBFBFE)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Step Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "STEP 2 OF 3",
                        color = ZenBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { 0.66f },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = ZenBlue,
                        trackColor = Color(0xFFE0E0E0)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Identity Verification",
                    color = ZenGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Identity Check",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "To comply with financial regulations, please provide clear photos of your official ID and a quick selfie.",
                fontSize = 14.sp,
                color = ZenGray,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ID Front
            IDUploadCard(
                title = "ID Card Front",
                subtitle = "Tap to capture or upload",
                icon = Icons.Outlined.Badge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ID Back
            IDUploadCard(
                title = "ID Card Back",
                subtitle = "Clear view of barcode/text",
                icon = Icons.Outlined.AccountBox
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Live Selfie Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = ZenPrimary.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Face, contentDescription = null, tint = ZenPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Live Selfie", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Verify it's really you with a quick photo.", color = ZenGray, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.LightGray)
                    ) {
                        // Placeholder for selfie preview
                        Box(
                            modifier = Modifier
                                .padding(12.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Blue.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retake Photo", color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Personal Details Form
            Text("Personal Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ZenGray)
            Spacer(modifier = Modifier.height(16.dp))

            ZenTextField(
                value = "",
                onValueChange = {},
                label = "FULL NAME (AS ON ID)",
                placeholder = "John Doe",
                isOutlined = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    ZenTextField(
                        value = "",
                        onValueChange = {},
                        label = "DATE OF BIRTH",
                        placeholder = "DD/MM/YYYY",
                        isOutlined = true
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ZenTextField(
                        value = "",
                        onValueChange = {},
                        label = "ID NUMBER",
                        placeholder = "0-12345678",
                        isOutlined = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ZenTextField(
                value = "",
                onValueChange = {},
                label = "RESIDENTIAL ADDRESS",
                placeholder = "123 Financial District, Suite 400",
                minLines = 3,
                isOutlined = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Security Notice
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE8F5E9)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Your data is encrypted with 256-bit AES protection. We never share your personal information with unauthorized third parties.",
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = "Proceed for Verification",
                onClick = onProceed,
                icon = Icons.AutoMirrored.Filled.ArrowForward
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
