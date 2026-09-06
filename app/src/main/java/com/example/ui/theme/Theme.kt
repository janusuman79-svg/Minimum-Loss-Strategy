package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CallGreen,
    onPrimary = Color.Black,
    primaryContainer = CallGreenContainer,
    onPrimaryContainer = CallGreenLight,
    secondary = PutRed,
    onSecondary = Color.White,
    secondaryContainer = PutRedContainer,
    onSecondaryContainer = PutRedLight,
    tertiary = BluePositional,
    onTertiary = Color.White,
    tertiaryContainer = BluePositionalContainer,
    onTertiaryContainer = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = CallGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FBE8),
    onPrimaryContainer = Color(0xFF003822),
    secondary = PutRed,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DF),
    onSecondaryContainer = Color(0xFF400010),
    tertiary = BluePositional,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6E4FF),
    onTertiaryContainer = Color(0xFF001B3E),
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceElevatedLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = SurfaceBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to pro dark trading terminal
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}

