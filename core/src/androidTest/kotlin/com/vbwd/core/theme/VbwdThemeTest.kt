package com.vbwd.core.theme

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies [VbwdTheme] publishes the selected theme via [LocalAppTheme]. */
@RunWith(AndroidJUnit4::class)
class VbwdThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun localAppThemeReflectsTheManagerSelection() {
        val manager = ThemeManager(ThemeRegistry(), InMemoryThemeStore())
        manager.selectTheme("dark-green")

        composeRule.setContent {
            VbwdTheme(manager) {
                Text(LocalAppTheme.current.id, modifier = Modifier.testTag("theme_id"))
            }
        }

        composeRule.onNodeWithTag("theme_id").assertIsDisplayed()
    }
}
