package com.vbwd.core.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vbwd.core.theme.InMemoryThemeStore
import com.vbwd.core.theme.ThemeManager
import com.vbwd.core.theme.ThemeRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A03.4 exit gate: selecting a theme in Settings persists it and re-themes live
 * (the manager's `StateFlow` updates synchronously). Instrumented Compose test.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingAThemePersistsAndUpdatesTheCurrentTheme() {
        val store = InMemoryThemeStore()
        val manager = ThemeManager(ThemeRegistry(), store)

        composeRule.setContent {
            MaterialTheme { SettingsScreen(themeManager = manager) }
        }

        composeRule.onNodeWithTag("theme_option_dark-blue").assertIsDisplayed()
        composeRule.onNodeWithTag("theme_option_dark-blue").performClick()
        composeRule.waitForIdle()

        assertEquals("dark-blue", manager.currentTheme.value.id)
        assertEquals("dark-blue", store.selectedThemeId)
    }
}
