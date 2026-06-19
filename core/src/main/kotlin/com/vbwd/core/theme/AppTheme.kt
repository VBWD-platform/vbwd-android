package com.vbwd.core.theme

import androidx.compose.ui.graphics.Color

/**
 * A colour palette for the whole app. Port of the iOS `AppTheme` protocol (the
 * property set 1:1). To add a theme: implement this and register it with
 * [ThemeRegistry] (OCP). [preferredDark] is the port of `preferredColorScheme`:
 * `null` = follow system, `true`/`false` = force dark/light.
 */
interface AppTheme {
    val id: String
    val displayName: String

    // Core palette
    val accent: Color
    val background: Color
    val cardBackground: Color
    val textPrimary: Color
    val textSecondary: Color

    // Semantic
    val destructive: Color
    val success: Color

    // Chrome
    val separator: Color
    val menuBackground: Color
    val avatarBackground: Color

    // Scheme preference
    val preferredDark: Boolean?
}

/** Default look (follows the system light/dark setting). Port of `ClassicTheme`. */
class ClassicTheme : AppTheme {
    override val id = "classic"
    override val displayName = "Classic"

    override val accent = Color(0xFF007AFF)
    override val background = Color(0xFFFFFFFF)
    override val cardBackground = Color(0x14808080)
    override val textPrimary = Color(0xFF000000)
    override val textSecondary = Color(0xFF6B6B6B)

    override val destructive = Color(0xFFFF3B30)
    override val success = Color(0xFF34C759)

    override val separator = Color(0xFFC6C6C8)
    override val menuBackground = Color(0xFFFFFFFF)
    override val avatarBackground = Color(0xFF007AFF).copy(alpha = 0.2f)

    override val preferredDark: Boolean? = null
}

/** Deep navy with dodger-blue accents. Port of `DarkBlueTheme` (same RGB). */
class DarkBlueTheme : AppTheme {
    override val id = "dark-blue"
    override val displayName = "Dark Blue"

    override val accent = Color(red = 0.20f, green = 0.60f, blue = 0.86f)
    override val background = Color(red = 0.05f, green = 0.11f, blue = 0.16f)
    override val cardBackground = Color(red = 0.09f, green = 0.15f, blue = 0.22f)
    override val textPrimary = Color.White
    override val textSecondary = Color(red = 0.7f, green = 0.7f, blue = 0.7f)

    override val destructive = Color(red = 0.91f, green = 0.30f, blue = 0.24f)
    override val success = Color(red = 0.18f, green = 0.80f, blue = 0.44f)

    override val separator = Color(red = 0.25f, green = 0.25f, blue = 0.25f)
    override val menuBackground = Color(red = 0.04f, green = 0.08f, blue = 0.13f)
    override val avatarBackground = Color(red = 0.20f, green = 0.60f, blue = 0.86f).copy(alpha = 0.2f)

    override val preferredDark: Boolean? = true
}

/** Dark background with emerald-green accents. Port of `DarkGreenTheme` (same RGB). */
class DarkGreenTheme : AppTheme {
    override val id = "dark-green"
    override val displayName = "Dark Green"

    override val accent = Color(red = 0.18f, green = 0.80f, blue = 0.44f)
    override val background = Color(red = 0.10f, green = 0.10f, blue = 0.18f)
    override val cardBackground = Color(red = 0.14f, green = 0.14f, blue = 0.22f)
    override val textPrimary = Color.White
    override val textSecondary = Color(red = 0.7f, green = 0.7f, blue = 0.7f)

    override val destructive = Color(red = 0.91f, green = 0.30f, blue = 0.24f)
    override val success = Color(red = 0.18f, green = 0.80f, blue = 0.44f)

    override val separator = Color(red = 0.25f, green = 0.25f, blue = 0.25f)
    override val menuBackground = Color(red = 0.08f, green = 0.08f, blue = 0.15f)
    override val avatarBackground = Color(red = 0.18f, green = 0.80f, blue = 0.44f).copy(alpha = 0.2f)

    override val preferredDark: Boolean? = true
}
