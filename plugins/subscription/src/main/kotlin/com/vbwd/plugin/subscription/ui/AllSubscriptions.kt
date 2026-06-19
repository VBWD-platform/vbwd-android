package com.vbwd.plugin.subscription.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vbwd.core.networking.ApiError
import com.vbwd.plugin.subscription.domain.Subscription
import com.vbwd.plugin.subscription.domain.SubscriptionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val PADDING = 16.dp

/** Full subscription history. Port of the iOS `AllSubscriptionsViewModel`. */
class AllSubscriptionsViewModel(private val service: SubscriptionService) {
    data class UiState(
        val isLoading: Boolean = true,
        val subscriptions: List<Subscription> = emptyList(),
        val errorMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    suspend fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        try {
            _uiState.value = UiState(isLoading = false, subscriptions = service.fetchAllSubscriptions())
        } catch (error: ApiError) {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.message)
        }
    }
}

@Composable
fun AllSubscriptionsScreen(viewModel: AllSubscriptionsViewModel) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(PADDING).testTag("all_subscriptions_screen"),
        verticalArrangement = Arrangement.spacedBy(PADDING),
    ) {
        items(state.subscriptions, key = { it.id }) { subscription ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(PADDING)) {
                    Text(subscription.plan?.name ?: subscription.id, style = MaterialTheme.typography.titleSmall)
                    Text("Status: ${subscription.statusLabel}")
                }
            }
        }
    }
}
