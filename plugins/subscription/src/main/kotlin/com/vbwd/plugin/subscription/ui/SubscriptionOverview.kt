package com.vbwd.plugin.subscription.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vbwd.core.networking.ApiError
import com.vbwd.plugin.subscription.domain.AddonSubscription
import com.vbwd.plugin.subscription.domain.Subscription
import com.vbwd.plugin.subscription.domain.SubscriptionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val PADDING = 16.dp

/** Subscription overview. Port of the iOS `SubscriptionOverviewViewModel`. */
class SubscriptionOverviewViewModel(private val service: SubscriptionService) {
    data class UiState(
        val isLoading: Boolean = true,
        val subscription: Subscription? = null,
        val addons: List<AddonSubscription> = emptyList(),
        val errorMessage: String? = null,
        val cancelSuccess: String? = null,
    ) {
        val activeAddons: List<AddonSubscription> get() = addons.filter { it.isActive }
        val previousAddons: List<AddonSubscription> get() = addons.filterNot { it.isActive }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    suspend fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        var error: String? = null
        val subscription = try {
            service.fetchActiveSub()
        } catch (e: ApiError) {
            error = e.message
            null
        }
        val addons = try {
            service.fetchUserAddOns()
        } catch (e: ApiError) {
            if (error == null) error = e.message
            emptyList()
        }
        _uiState.value = UiState(isLoading = false, subscription = subscription, addons = addons, errorMessage = error)
    }

    suspend fun cancel() {
        val subId = _uiState.value.subscription?.id ?: return
        try {
            service.cancelSubscription(subId)
            load()
            _uiState.value = _uiState.value.copy(cancelSuccess = "Subscription cancelled successfully.")
        } catch (e: ApiError) {
            _uiState.value = _uiState.value.copy(errorMessage = e.message)
        }
    }
}

@Composable
fun SubscriptionOverviewScreen(viewModel: SubscriptionOverviewViewModel) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PADDING)
            .testTag("subscription_overview_screen"),
        verticalArrangement = Arrangement.spacedBy(PADDING),
    ) {
        Text("Subscription", style = MaterialTheme.typography.headlineSmall)
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.cancelSuccess?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        val subscription = state.subscription
        if (subscription == null) {
            Text("No active subscription")
        } else {
            Text(subscription.plan?.name ?: "Plan", style = MaterialTheme.typography.titleMedium)
            Text("Status: ${subscription.statusLabel}")
            Button(
                onClick = { scope.launch { viewModel.cancel() } },
                modifier = Modifier.fillMaxWidth().testTag("cancel_subscription"),
            ) {
                Text("Cancel subscription")
            }
        }
        state.activeAddons.forEach { addon ->
            Text("Add-on: ${addon.addon?.name ?: addon.id} · ${addon.statusLabel}")
        }
    }
}
