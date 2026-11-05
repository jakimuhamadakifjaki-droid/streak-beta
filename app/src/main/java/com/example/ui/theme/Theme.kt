package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FlareDarkColorScheme = darkColorScheme(
    primary = FlareOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D1600),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = FlareCrimson,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF400A15),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = FlarePurple,
    onTertiary = Color.White,
    background = FlareDarkBackground,
    onBackground = FlareTextPrimary,
    surface = FlareDarkSurface,
    onSurface = FlareTextPrimary,
    surfaceVariant = FlareDarkSurfaceVariant,
    onSurfaceVariant = FlareTextSecondary,
    surfaceContainer = FlareDarkSurfaceContainer,
    outline = FlareDarkCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to Dark mode as requested
    dynamicColor: Boolean = false, // Keep Flare's distinct neon palette
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FlareDarkColorScheme,
        typography = Typography,
        content = content
    )
}
