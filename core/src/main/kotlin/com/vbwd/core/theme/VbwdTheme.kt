package com.vbwd.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Reads the current [AppTheme] anywhere in the tree (port of the iOS
 * `Environment(\.appTheme)`). Provided by [VbwdTheme].
 */
val LocalAppTheme = staticCompositionLocalOf<AppTheme> { ClassicTheme() }

/**
 * App theme wrapper: feeds a Compose [MaterialTheme] from the selected
 * [AppTheme] and publishes it via [LocalAppTheme]. One place maps the theme's
 * data onto Compose colours (DRY); `preferredDark` overrides the system setting.
 */
@Composable
fun VbwdTheme(themeManager: ThemeManager, content: @Composable () -> Unit) {
    val theme by themeManager.currentTheme.collectAsState()
    val dark = theme.preferredDark ?: isSystemInDarkTheme()
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val colorScheme = base.copy(
        primary = theme.accent,
        background = theme.background,
        surface = theme.cardBackground,
        onBackground = theme.textPrimary,
        onSurface = theme.textPrimary,
        error = theme.destructive,
    )
    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
