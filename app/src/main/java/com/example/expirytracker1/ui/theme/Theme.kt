package com.example.expirytracker1.ui.theme

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
    primary = SageGreenBackground, // Brighter green for readability in dark mode
    onPrimary = Color.Black,
    secondary = DarkGreenPrimary,
    onSecondary = Color.White,
    tertiary = SuccessGreen,
    background = Color(0xFF1B1C1B),
    surface = Color(0xFF252625),
    onBackground = Color.White,
    onSurface = Color.White,
    outline = Color(0xFF3E403E),
    primaryContainer = DarkGreenPrimary,
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFF2E4D41).copy(alpha = 0.3f),
    onSecondaryContainer = SageGreenBackground
)

private val LightColorScheme = lightColorScheme(
    primary = DarkGreenPrimary,
    onPrimary = Color.White,
    secondary = SageGreenBackground,
    onSecondary = Color.Black,
    tertiary = SuccessGreen,
    background = Color(0xFFF8FAF8), // Very light sage tint
    surface = Color.White,
    onBackground = DarkGreenPrimary,
    onSurface = DarkGreenPrimary,
    outline = LightSageBorder,
    primaryContainer = SageGreenBackground.copy(alpha = 0.3f),
    onPrimaryContainer = DarkGreenPrimary,
    secondaryContainer = Color(0xFFE8F5E9),
    onSecondaryContainer = DarkGreenPrimary
)

@Composable
fun ExpiryTracker1Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled to enforce the specific green theme
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
