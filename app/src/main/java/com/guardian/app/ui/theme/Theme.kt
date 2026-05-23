package com.guardian.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val OliveGreen = Color(0xFF6B7B5C)
val OliveGreenDark = Color(0xFF4A5E3A)
val OliveLight = Color(0xFF6B7F56)
val Gold = Color(0xFFC9A84C)
val GoldLight = Color(0xFFE8C97A)
val OffWhite = Color(0xFFF7F5F0)
val DarkSurface = Color(0xFF0D1117)
val DarkBackground = Color(0xFF111820)

private val LightColors = lightColorScheme(
    primary = OliveGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0E8B8),
    secondary = Gold,
    background = OffWhite,
    surface = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A)
)

private val DarkColors = darkColorScheme(
    primary = OliveLight,
    secondary = Gold,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = OffWhite,
    onSurface = OffWhite
)

@Composable
fun GuardianTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
