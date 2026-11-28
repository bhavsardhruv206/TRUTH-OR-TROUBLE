package com.afterhours.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.afterhours.core.i18n.EnglishStrings
import com.afterhours.core.i18n.UiStrings

/** The app text for the chosen language. Read it with `LocalStrings.current`. */
val LocalStrings = staticCompositionLocalOf<UiStrings> { EnglishStrings }
