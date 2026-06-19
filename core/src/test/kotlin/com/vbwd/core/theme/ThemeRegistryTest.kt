package com.vbwd.core.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ThemeRegistryTest {
    @Test
    fun `built-in themes are registered and the default resolves`() {
        val registry = ThemeRegistry()
        assertNotNull(registry.theme("classic"))
        assertNotNull(registry.theme("dark-blue"))
        assertNotNull(registry.theme("dark-green"))
        assertNotNull(registry.theme(registry.defaultThemeId))
    }

    @Test
    fun `themes are listed sorted by display name`() {
        val names = ThemeRegistry().themes.map { it.displayName }
        assertEquals(names.sorted(), names)
    }

    @Test
    fun `registering an existing id overrides it (plugin override)`() {
        val registry = ThemeRegistry()
        val custom = object : AppTheme by ClassicTheme() {
            override val id = "classic"
            override val displayName = "Custom Classic"
        }
        registry.register(custom)
        assertEquals("Custom Classic", registry.theme("classic")?.displayName)
    }

    @Test
    fun `unknown id resolves to null`() {
        assertNull(ThemeRegistry().theme("nope"))
    }
}
