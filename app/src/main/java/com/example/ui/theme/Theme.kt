package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkFuturisticColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = VoidBackground,
    primaryContainer = ElectricBlue.copy(alpha = 0.25f),
    onPrimaryContainer = NeonCyan,
    secondary = QuantumPurple,
    onSecondary = Color.White,
    secondaryContainer = QuantumPurple.copy(alpha = 0.2f),
    onSecondaryContainer = GlowingMagenta,
    tertiary = CyberAmber,
    onTertiary = VoidBackground,
    background = VoidBackground,
    onBackground = TextPrimary,
    surface = DeepSpaceSurface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = TextSecondary,
    outline = GlassBorderColor
)

private val LightFuturisticColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = ElectricBlue.copy(alpha = 0.12f),
    onPrimaryContainer = ElectricBlue,
    secondary = QuantumPurple,
    onSecondary = Color.White,
    secondaryContainer = QuantumPurple.copy(alpha = 0.12f),
    onSecondaryContainer = QuantumPurple,
    tertiary = CyberAmber,
    onTertiary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = GlassBorderColor
)

@Composable
fun JarvisTheme(
    themeMode: String = "Dark Futuristic",
    accentColorName: String = "Cyan",
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "Light Futuristic" -> false
        "OLED Pitch Black" -> true
        "Cyberpunk Neon" -> true
        else -> true
    }

    val baseScheme = if (isDark) DarkFuturisticColorScheme else LightFuturisticColorScheme

    val customPrimary = when (accentColorName) {
        "Magenta" -> GlowingMagenta
        "Purple" -> QuantumPurple
        "Amber" -> CyberAmber
        "Emerald" -> EmeraldGreen
        else -> NeonCyan
    }

    val finalColorScheme = baseScheme.copy(
        primary = customPrimary,
        background = if (themeMode == "OLED Pitch Black") Color.Black else baseScheme.background
    )

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = Typography,
        content = content
    )
}
