package com.dik.videoedit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat

val Blue = Color(0xFF2684FF)
val LightBlue = Color(0xFF63B3FF)
val Ink = Color(0xFF17233C)
val Muted = Color(0xFF77849B)
val BorderLine = Color(0xFFE7EDF7)
val PurpleTrack = Color(0xFF9B8CFF)
val TealTrack = Color(0xFF1FB5C9)
val Gold = Color(0xFFFFB800)
val Success = Color(0xFF22C55E)

private val LightColors = lightColorScheme(
    primary = Blue, onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EFFF), onPrimaryContainer = Blue,
    secondary = LightBlue, onSecondary = Color.White,
    background = Color(0xFFF3F7FF), onBackground = Ink,
    surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFEDF3FC), onSurfaceVariant = Muted,
    outline = BorderLine, outlineVariant = BorderLine,
    error = Color(0xFFE5484D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4C9BFF), onPrimary = Color.White,
    primaryContainer = Color(0xFF1C3358), onPrimaryContainer = Color(0xFFBBD9FF),
    secondary = LightBlue, onSecondary = Color.White,
    background = Color(0xFF0E1525), onBackground = Color(0xFFEAF0FB),
    surface = Color(0xFF172037), onSurface = Color(0xFFEAF0FB),
    surfaceVariant = Color(0xFF1F2A45), onSurfaceVariant = Color(0xFF9AA8C2),
    outline = Color(0xFF2A3656), outlineVariant = Color(0xFF2A3656),
    error = Color(0xFFFF6B6F),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
)

/** @param mode "light", "dark", atau "system". */
@Composable
fun VideoEditTheme(mode: String, content: @Composable () -> Unit) {
    val dark = when (mode) {
        "dark" -> true
        "system" -> isSystemInDarkTheme()
        else -> false
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, shapes = AppShapes, content = content)
}
