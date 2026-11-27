package com.afterhours.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Truth or Trouble palette: neon on midnight.
val Night = Color(0xFF0B0716)        // app background
val NightDeep = Color(0xFF070410)    // gradient floor
val Plum = Color(0xFF1B1233)         // cards / surfaces
val PlumHigh = Color(0xFF281A4A)     // raised surfaces, chips
val Magenta = Color(0xFFFF2E7E)      // primary: "Trouble"
val Amber = Color(0xFFFFB020)        // secondary: highlights, scores
val Violet = Color(0xFF8B5CF6)       // tertiary
val Cyan = Color(0xFF22E4D6)         // accent: "Truth"
val Ink = Color(0xFFF6F1FF)          // main text
val InkMuted = Color(0xFFB4A9D1)     // secondary text

/** Hot gradient used for hero text, primary buttons and the active card border. */
val HotBrush = Brush.horizontalGradient(listOf(Magenta, Amber))
val NeonBrush = Brush.linearGradient(listOf(Cyan, Violet, Magenta))
val NightBrush = Brush.verticalGradient(listOf(Plum.copy(alpha = .55f), Night, NightDeep))

private val Scheme = darkColorScheme(
    primary = Magenta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5A1038),
    onPrimaryContainer = Color(0xFFFFD9E6),
    secondary = Amber,
    onSecondary = Color(0xFF2A1800),
    secondaryContainer = Color(0xFF4A3300),
    onSecondaryContainer = Color(0xFFFFE2A8),
    tertiary = Cyan,
    onTertiary = Color(0xFF00201E),
    background = Night,
    onBackground = Ink,
    surface = Plum,
    onSurface = Ink,
    surfaceVariant = PlumHigh,
    onSurfaceVariant = InkMuted,
    outline = Color(0xFF6B5C99),
    outlineVariant = Color(0xFF3A2D63),
    error = Color(0xFFFF6B6B),
)

private val Type = Typography(
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black,
        fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-1.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.5.sp,
    ),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 15.sp, letterSpacing = 0.8.sp,
    ),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun AfterHoursTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Scheme, typography = Type, shapes = AppShapes, content = content)
