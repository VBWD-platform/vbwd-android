package com.vbwd.core.ui.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private val PADDING = 16.dp
private const val FULL_WEIGHT = 1f

/** Token-bundle storefront. Port of the iOS `BuyTokensView`. */
@Composable
fun BuyTokensScreen(viewModel: BuyTokensViewModel) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(PADDING).testTag("buy_tokens_screen"),
        verticalArrangement = Arrangement.spacedBy(PADDING),
    ) {
        items(state.bundles, key = { it.id }) { bundle ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(FULL_WEIGHT)) {
                    Text(bundle.name, style = MaterialTheme.typography.titleSmall)
                    Text("${bundle.tokenAmount} tokens · ${bundle.currency ?: "USD"} ${bundle.price}")
                }
                Button(onClick = { viewModel.addToCart(bundle) }) { Text("Add") }
            }
        }
    }
}
