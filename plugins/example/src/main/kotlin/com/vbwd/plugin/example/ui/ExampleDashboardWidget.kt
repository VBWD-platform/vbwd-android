package com.vbwd.plugin.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vbwd.plugin.example.domain.ExampleStore

private val CARD_PADDING = 16.dp

/**
 * `Dashboard*` widget surfaced on the home screen (the convention the host's
 * `ComponentRegistry.dashboardComponents()` discovers). Port of the iOS
 * `ExampleDashboardWidget`.
 */
@Composable
fun ExampleDashboardWidget(store: ExampleStore) {
    val sawLogin by store.sawLogin.collectAsState()
    Card(modifier = Modifier.testTag("example_dashboard_widget")) {
        Column(modifier = Modifier.padding(CARD_PADDING)) {
            Text("Example widget")
            Text("Saw login: $sawLogin")
        }
    }
}
