package com.vbwd.core.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vbwd.core.theme.ThemeManager

private val SCREEN_PADDING = 16.dp
private val ROW_SPACING = 12.dp
private val SWATCH_SIZE = 24.dp
private const val FULL_WEIGHT = 1f

/**
 * Settings screen with a live theme picker bound to [ThemeManager]. Port of the
 * iOS `SettingsScreen` Appearance section: selecting a theme persists it and
 * re-themes the shell immediately (the manager's `StateFlow` drives `VbwdTheme`).
 */
@Composable
fun SettingsScreen(themeManager: ThemeManager) {
    val current by themeManager.currentTheme.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(SCREEN_PADDING).testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Column(modifier = Modifier.testTag("theme_picker")) {
            themeManager.availableThemes.forEach { theme ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ROW_SPACING),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { themeManager.selectTheme(theme.id) }
                        .padding(vertical = ROW_SPACING)
                        .testTag("theme_option_${theme.id}"),
                ) {
                    Box(
                        modifier = Modifier
                            .size(SWATCH_SIZE)
                            .clip(CircleShape)
                            .background(theme.accent),
                    )
                    Text(theme.displayName)
                    Spacer(modifier = Modifier.weight(FULL_WEIGHT))
                    if (current.id == theme.id) {
                        Text("✓", modifier = Modifier.testTag("theme_selected_indicator"))
                    }
                }
            }
        }
    }
}
