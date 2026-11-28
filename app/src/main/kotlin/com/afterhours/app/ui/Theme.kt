package com.afterhours.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afterhours.app.data.ThemeMode
import com.afterhours.core.Intensity

/**
 * Truth or Trouble palette. Dark is a late-night lounge (charcoal, warm ivory, champagne, wine);
 * light is the same mood on warm paper, with the accents deepened so they stay readable.
 * Screens use the role names below; each one reads from whichever palette is active.
 */
@Immutable
data class Palette(
    val background: Color,     // app background
    val backgroundDeep: Color, // gradient floor
    val backdropTop: Color,    // gradient top
    val surface: Color,        // cards / surfaces
    val surfaceHigh: Color,    // raised surfaces
    val accent: Color,         // primary accent
    val accentDim: Color,      // pressed / secondary accent
    val wine: Color,           // wild intensity, destructive
    val text: Color,           // main text
    val textMuted: Color,      // secondary text
    val hairline: Color,       // dividers, quiet borders
    val hairlineStrong: Color,
)

val DarkPalette = Palette(
    background = Color(0xFF0F0F11),
    backgroundDeep = Color(0xFF09090A),
    backdropTop = Color(0xFF16151A),
    surface = Color(0xFF18181B),
    surfaceHigh = Color(0xFF222226),
    accent = Color(0xFFC8A97E),
    accentDim = Color(0xFF8E7856),
    wine = Color(0xFF9A4555),
    text = Color(0xFFECE7DF),
    textMuted = Color(0xFF9A958D),
    hairline = Color(0xFF2E2E33),
    hairlineStrong = Color(0xFF45454C),
)

val LightPalette = Palette(
    background = Color(0xFFF6F2EC),
    backgroundDeep = Color(0xFFEEE8DF),
    backdropTop = Color(0xFFFBF9F5),
    surface = Color(0xFFFFFDFA),
    surfaceHigh = Color(0xFFF0EAE1),
    accent = Color(0xFF7D5E33),
    accentDim = Color(0xFFB09470),
    wine = Color(0xFF9A3647),
    text = Color(0xFF1D1B18),
    textMuted = Color(0xFF6B655C),
    hairline = Color(0xFFE3DCD1),
    hairlineStrong = Color(0xFFC5BBAC),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

// Role names used across the screens. Kept from the original dark-only palette so call sites stay the same.
val Charcoal: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.background
val CharcoalDeep: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.backgroundDeep
val Graphite: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface
val GraphiteHigh: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surfaceHigh
val Champagne: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accent
val ChampagneDim: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accentDim
val Wine: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.wine
val Ivory: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.text
val Stone: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.textMuted
val Hairline: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.hairline
val HairlineStrong: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.hairlineStrong

val BackdropBrush: Brush
    @Composable get() {
        val p = LocalPalette.current
        return remember(p) { Brush.verticalGradient(listOf(p.backdropTop, p.background, p.backgroundDeep)) }
    }

/** Tag colour for an intensity. */
@Composable
@ReadOnlyComposable
fun intensityColor(i: Intensity): Color = when (i) {
    Intensity.CHILL -> Stone
    Intensity.BOLD -> Champagne
    Intensity.WILD -> Wine
}

private fun darkScheme(p: Palette) = darkColorScheme(
    primary = p.accent,
    onPrimary = Color(0xFF1A140B),
    primaryContainer = Color(0xFF3A3022),
    onPrimaryContainer = Color(0xFFF1E3CC),
    secondary = p.accentDim,
    onSecondary = p.text,
    tertiary = p.wine,
    onTertiary = p.text,
    background = p.background,
    onBackground = p.text,
    surface = p.surface,
    onSurface = p.text,
    surfaceVariant = p.surfaceHigh,
    onSurfaceVariant = p.textMuted,
    outline = p.hairlineStrong,
    outlineVariant = p.hairline,
    error = Color(0xFFD0707E),
)

private fun lightScheme(p: Palette) = lightColorScheme(
    primary = p.accent,
    onPrimary = Color(0xFFFFFBF5),
    primaryContainer = Color(0xFFF1E3CC),
    onPrimaryContainer = Color(0xFF2E2414),
    secondary = p.accentDim,
    onSecondary = Color(0xFFFFFBF5),
    tertiary = p.wine,
    onTertiary = Color(0xFFFFFBF5),
    background = p.background,
    onBackground = p.text,
    surface = p.surface,
    onSurface = p.text,
    surfaceVariant = p.surfaceHigh,
    onSurfaceVariant = p.textMuted,
    outline = p.hairlineStrong,
    outlineVariant = p.hairline,
    error = Color(0xFFB3384B),
)

private val DarkScheme = darkScheme(DarkPalette)
private val LightScheme = lightScheme(LightPalette)

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

/** Whether the app should be dark, given the player's choice and the phone's setting. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun AfterHoursTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = Type,
            shapes = AppShapes,
            content = content,
        )
    }
}
