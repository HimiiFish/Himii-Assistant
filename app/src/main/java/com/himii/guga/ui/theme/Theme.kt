package com.himii.guga.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF5B6CFF),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF6B7280),
    tertiary = Color(0xFF8B9CF7),
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFEEF0F4),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFD1D5DB)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9AA6FF),
    onPrimary = Color(0xFF111827),
    secondary = Color(0xFF9CA3AF),
    tertiary = Color(0xFFB4C0FF),
    background = Color(0xFF111827),
    surface = Color(0xFF1F2937),
    onBackground = Color(0xFFF9FAFB),
    onSurface = Color(0xFFF9FAFB),
    surfaceVariant = Color(0xFF374151),
    onSurfaceVariant = Color(0xFFD1D5DB),
    outline = Color(0xFF4B5563)
)

@Composable
fun GugaTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
