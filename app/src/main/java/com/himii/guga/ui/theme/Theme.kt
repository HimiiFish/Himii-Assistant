package com.himii.guga.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7A5CFF),
    secondary = Color(0xFFFF7AB6),
    tertiary = Color(0xFF5CC8FF),
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB9A6FF),
    secondary = Color(0xFFFFB0D2),
    tertiary = Color(0xFF8AD7FF),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F)
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
