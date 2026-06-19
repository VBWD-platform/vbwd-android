package com.vbwd.core.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistence seam for the selected theme id (DIP). The in-memory impl is the
 * testable Liskov twin of [SharedPrefsThemeStore] (mirrors the `TokenStore`
 * pattern).
 */
interface ThemeStore {
    var selectedThemeId: String?
}

/** In-memory store for tests/previews. */
class InMemoryThemeStore : ThemeStore {
    override var selectedThemeId: String? = null
}

/** SharedPreferences-backed store — the production persistence (port of `UserDefaults`). */
class SharedPrefsThemeStore(context: Context) : ThemeStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var selectedThemeId: String?
        get() = prefs.getString(KEY, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY) else putString(KEY, value)
            }.apply()
        }

    private companion object {
        const val PREFS_NAME = "vbwd_theme"
        const val KEY = "selectedThemeID"
    }
}

/**
 * Observable theme state. Persists the selected id via [ThemeStore]; views
 * observe [currentTheme]. Port of the iOS `ThemeManager` — an unknown/absent id
 * falls back to the registry default, then [ClassicTheme] (Liskov).
 */
class ThemeManager(
    private val registry: ThemeRegistry,
    private val store: ThemeStore = InMemoryThemeStore(),
) {
    private val _currentTheme = MutableStateFlow(resolveInitial())
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    val availableThemes: List<AppTheme> get() = registry.themes

    /** Select a theme by id; persists. No-op if the id is unknown. */
    fun selectTheme(id: String) {
        val theme = registry.theme(id) ?: return
        store.selectedThemeId = id
        _currentTheme.value = theme
    }

    private fun resolveInitial(): AppTheme =
        store.selectedThemeId?.let { registry.theme(it) }
            ?: registry.theme(registry.defaultThemeId)
            ?: ClassicTheme()
}
