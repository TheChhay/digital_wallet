package com.app.digitalwallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import com.app.digitalwallet.core.session.TokenManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SplashScreen(onNavigateToHome: () -> Unit, onNavigateToLogin: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager.getInstance(context) }

    // Navigate to Home or Login after a delay
    LaunchedEffect(key1 = true) {
        delay(2500.milliseconds) // 2.5 seconds splash
        if (tokenManager.getAccessToken() != null) {
            onNavigateToHome()
        } else {
            onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Eye Logo Icon with Gradient Background (Matching your image)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RemoveRedEye,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brand Name
            Text(
                text = "ZenWallet",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            // Slogan
            Text(
                text = "COOL VISION",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 14.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Three loading dots (simplified animation)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                    )
                }
            }
        }

        // Bottom Footer Text
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SECURE DIGITAL ASSET MANAGEMENT",
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = "AES-256 ENCRYPTION",
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                fontSize = 9.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
