package com.afterhours.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Night = Color(0xFF0F0B1E)
val Plum = Color(0xFF1E1638)
val Magenta = Color(0xFFFF3D81)
val Amber = Color(0xFFFFB347)
val Violet = Color(0xFF8B5CF6)

private val Scheme = darkColorScheme(
    primary = Magenta,
    onPrimary = Color.White,
    secondary = Amber,
    onSecondary = Color.Black,
    tertiary = Violet,
    background = Night,
    onBackground = Color(0xFFF4EFFF),
    surface = Plum,
    onSurface = Color(0xFFF4EFFF),
    surfaceVariant = Color(0xFF2A2048),
)

@Composable
fun AfterHoursTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Scheme, content = content)
