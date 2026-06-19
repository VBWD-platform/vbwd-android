package com.vbwd.plugin.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vbwd.plugin.example.domain.ExampleStore

private val SCREEN_PADDING = 16.dp
private val ITEM_SPACING = 12.dp

/** Standalone `/example` screen. Port of the iOS `ExampleScreen`. */
@Composable
fun ExampleScreen(store: ExampleStore) {
    val count by store.count.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(SCREEN_PADDING).testTag("example_screen"),
        verticalArrangement = Arrangement.spacedBy(ITEM_SPACING),
    ) {
        Text("Example", style = MaterialTheme.typography.headlineSmall)
        Text("Count: $count")
        Button(onClick = { store.incrementCounter() }) {
            Text("Increment")
        }
    }
}
