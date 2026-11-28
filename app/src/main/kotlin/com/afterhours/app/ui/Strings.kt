package com.afterhours.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.afterhours.core.i18n.EnglishStrings
import com.afterhours.core.i18n.UiStrings

/** The app text for the chosen language. Read it with `LocalStrings.current`. */
val LocalStrings = staticCompositionLocalOf<UiStrings> { EnglishStrings }

fun UiStrings.themeLabel(m: com.afterhours.app.data.ThemeMode): String = when (m) {
    com.afterhours.app.data.ThemeMode.SYSTEM -> themeAuto
    com.afterhours.app.data.ThemeMode.LIGHT -> themeLight
    com.afterhours.app.data.ThemeMode.DARK -> themeDark
}
