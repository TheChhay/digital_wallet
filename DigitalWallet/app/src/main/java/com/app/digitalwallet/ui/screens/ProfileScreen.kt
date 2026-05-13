package com.app.digitalwallet.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.app.digitalwallet.navigation.Screen
import com.app.digitalwallet.ui.components.ProfileOption
import com.app.digitalwallet.ui.theme.ZenBlue
import com.app.digitalwallet.ui.theme.ZenGray
import com.app.digitalwallet.ui.theme.ZenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Profile", 
                            color = MaterialTheme.colorScheme.onSurface, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            Icons.Outlined.Edit, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Profile Picture
            Box {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).padding(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.BottomEnd),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.padding(6.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Alex Johnson", 
                fontSize = 22.sp, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "alex.johnson@example.com", 
                color = MaterialTheme.colorScheme.tertiary, 
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Profile Options
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
            ProfileOption(
                icon = Icons.Outlined.Notifications,
                title = "Notifications",
                onClick = { navController.navigate(Screen.Notifications.route) }
            )
            ProfileOption(
                icon = Icons.Outlined.Security,
                title = "Security",
                onClick = { navController.navigate(Screen.Security.route) }
            )
            ProfileOption(
                icon = Icons.Outlined.VerifiedUser,
                title = "Verify Account",
                onClick = { navController.navigate(Screen.IdentityVerification.route) }
            )
            ProfileOption(
                icon = Icons.Default.Info,
                title = "Help Center",
                onClick = { navController.navigate(Screen.HelpCenter.route) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ProfileOption(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                title = "Logout",
                titleColor = Color.Red,
                showArrow = false,
                onClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
