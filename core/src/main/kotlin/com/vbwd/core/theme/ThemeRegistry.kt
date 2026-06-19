package com.vbwd.core.theme

/**
 * Holds all registered [AppTheme]s. Extensible via [register] — new themes are
 * added without modifying existing code (OCP; plugins can register their own).
 * Port of the iOS `ThemeRegistry`: registering an existing id **replaces** it
 * (so a plugin can override a built-in).
 */
class ThemeRegistry {
    private val storage = LinkedHashMap<String, AppTheme>()
    var defaultThemeId: String = "classic"

    init {
        register(ClassicTheme())
        register(DarkGreenTheme())
        register(DarkBlueTheme())
    }

    /** All registered themes, sorted by display name. */
    val themes: List<AppTheme>
        get() = storage.values.sortedBy { it.displayName }

    /** Register a theme; replaces any existing theme with the same id. */
    fun register(theme: AppTheme) {
        storage[theme.id] = theme
    }

    /** Look up a theme by id, or null. */
    fun theme(id: String): AppTheme? = storage[id]
}
