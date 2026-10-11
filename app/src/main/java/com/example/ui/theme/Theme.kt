package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LivingDarkColorScheme = darkColorScheme(
    primary = BioCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003830),
    onPrimaryContainer = BioCyan,
    secondary = BioViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1065),
    onSecondaryContainer = BioVioletLight,
    tertiary = BioAmber,
    onTertiary = Color.Black,
    background = CharcoalBlack,
    onBackground = BioTextPrimary,
    surface = CharcoalSurface,
    onSurface = BioTextPrimary,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = BioTextSecondary,
    outline = GlassCardBorder
)

@Composable
fun LivingPhoneTheme(
    content: @Composable () -> Unit
) {
    // Strictly Dark Mode with Luxurious Charcoal Black
    MaterialTheme(
        colorScheme = LivingDarkColorScheme,
        typography = Typography,
        content = content
    )
}

// Backwards compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    LivingPhoneTheme(content = content)
}
