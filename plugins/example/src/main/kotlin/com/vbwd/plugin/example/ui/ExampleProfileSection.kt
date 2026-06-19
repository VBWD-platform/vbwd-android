package com.vbwd.plugin.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private val SECTION_PADDING = 16.dp

/**
 * `Profile*` section injected into the profile screen (the convention the host's
 * `ComponentRegistry.profileComponents()` discovers). Port of the iOS
 * `ExampleProfileSection`.
 */
@Composable
fun ExampleProfileSection() {
    Column(modifier = Modifier.padding(SECTION_PADDING).testTag("example_profile_section")) {
        Text("Example profile section")
    }
}
