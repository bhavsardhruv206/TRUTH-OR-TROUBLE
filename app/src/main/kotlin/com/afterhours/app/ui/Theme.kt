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
import com.afterhours.core.Intensity

// Truth or Trouble palette: a late-night lounge. Charcoal, warm ivory, champagne and wine.
val Charcoal = Color(0xFF0F0F11)      // app background
val CharcoalDeep = Color(0xFF09090A)  // gradient floor
val Graphite = Color(0xFF18181B)      // cards / surfaces
val GraphiteHigh = Color(0xFF222226)  // raised surfaces
val Champagne = Color(0xFFC8A97E)     // primary accent
val ChampagneDim = Color(0xFF8E7856)  // pressed / secondary accent
val Wine = Color(0xFF9A4555)          // wild intensity, destructive
val Ivory = Color(0xFFECE7DF)         // main text
val Stone = Color(0xFF9A958D)         // secondary text
val Hairline = Color(0xFF2E2E33)      // dividers, quiet borders
val HairlineStrong = Color(0xFF45454C)

val BackdropBrush = Brush.verticalGradient(listOf(Color(0xFF16151A), Charcoal, CharcoalDeep))

/** Tag colour for an intensity. */
fun intensityColor(i: Intensity): Color = when (i) {
    Intensity.CHILL -> Stone
    Intensity.BOLD -> Champagne
    Intensity.WILD -> Wine
}

private val Scheme = darkColorScheme(
    primary = Champagne,
    onPrimary = Color(0xFF1A140B),
    primaryContainer = Color(0xFF3A3022),
    onPrimaryContainer = Color(0xFFF1E3CC),
    secondary = ChampagneDim,
    onSecondary = Ivory,
    tertiary = Wine,
    onTertiary = Ivory,
    background = Charcoal,
    onBackground = Ivory,
    surface = Graphite,
    onSurface = Ivory,
    surfaceVariant = GraphiteHigh,
    onSurfaceVariant = Stone,
    outline = HairlineStrong,
    outlineVariant = Hairline,
    error = Color(0xFFD0707E),
)

private val Serif = FontFamily.Serif
private val Sans = FontFamily.SansSerif

private val Type = Typography(
    displayMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 42.sp, lineHeight = 46.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 1.2.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.6.sp),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
fun AfterHoursTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Scheme, typography = Type, shapes = AppShapes, content = content)
