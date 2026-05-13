package com.app.digitalwallet.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ZenPrimary,
    secondary = ZenAccent,
    tertiary = ZenGray,
    background = DarkBluePurple,
    surface = DeepPurple,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2D2D44),
    onSurfaceVariant = Color.White,
    outlineVariant = Color(0xFF3F3F55)
)

private val LightColorScheme = lightColorScheme(
    primary = ZenPrimary,
    secondary = ZenAccent,
    tertiary = ZenGray,
    background = Color(0xFFF8F9FF),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = ZenBlue,
    onBackground = ZenBlue,
    onSurface = ZenBlue,
    surfaceVariant = Color(0xFFF0F2F8),
    onSurfaceVariant = ZenGray,
    outlineVariant = Color.LightGray
)

@Composable
fun DigitalWalletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
