package com.example.replybubble.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E9),
    onPrimaryContainer = Ink,
    secondary = CoralSecondary,
    onSecondary = Ink,
    secondaryContainer = Color(0xFFFFE3F0),
    onSecondaryContainer = Ink,
    tertiary = MintAccent,
    onTertiary = Ink,
    tertiaryContainer = Color(0xFFF6D8EE),
    onTertiaryContainer = Ink,
    background = MistBackground,
    onBackground = Ink,
    surface = CardIvory,
    onSurface = Ink,
    surfaceContainer = Color(0xFFFBEAF4),
    surfaceContainerHigh = Color(0xFFFFF5FB),
    surfaceContainerLowest = Color(0xFFFFFBFE),
    onSurfaceVariant = Slate,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF471126),
    primaryContainer = Color(0xFF6A2744),
    onPrimaryContainer = Color(0xFFFFD9E9),
    secondary = DarkSecondary,
    onSecondary = Color(0xFF4A1530),
    secondaryContainer = Color(0xFF5B2440),
    onSecondaryContainer = Color(0xFFFFD9EA),
    tertiary = MintAccent,
    onTertiary = Color(0xFF472039),
    tertiaryContainer = Color(0xFF653653),
    onTertiaryContainer = Color(0xFFFFD9F0),
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceContainer = DarkSurfaceAlt,
    surfaceContainerHigh = Color(0xFF3C2543),
    surfaceContainerLowest = Color(0xFF140D18),
    onSurfaceVariant = DarkMuted,
)

@Composable
fun ReplyBubbleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ReplyBubbleTypography,
        content = content,
    )
}
