package com.vbwd.core.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ThemeManagerTest {
    private val registry = ThemeRegistry()

    @Test
    fun `defaults to the registry default when nothing persisted`() {
        val manager = ThemeManager(registry, InMemoryThemeStore())
        assertEquals("classic", manager.currentTheme.value.id)
    }

    @Test
    fun `selectTheme persists and updates the current theme`() {
        val store = InMemoryThemeStore()
        val manager = ThemeManager(registry, store)
        manager.selectTheme("dark-blue")
        assertEquals("dark-blue", manager.currentTheme.value.id)
        assertEquals("dark-blue", store.selectedThemeId)
    }

    @Test
    fun `restores a persisted selection on construction`() {
        val store = InMemoryThemeStore().apply { selectedThemeId = "dark-green" }
        val manager = ThemeManager(registry, store)
        assertEquals("dark-green", manager.currentTheme.value.id)
    }

    @Test
    fun `an unknown persisted id falls back to the default`() {
        val store = InMemoryThemeStore().apply { selectedThemeId = "ghost" }
        val manager = ThemeManager(registry, store)
        assertEquals("classic", manager.currentTheme.value.id)
    }

    @Test
    fun `selecting an unknown id is a no-op`() {
        val manager = ThemeManager(registry, InMemoryThemeStore())
        manager.selectTheme("ghost")
        assertEquals("classic", manager.currentTheme.value.id)
    }
}
