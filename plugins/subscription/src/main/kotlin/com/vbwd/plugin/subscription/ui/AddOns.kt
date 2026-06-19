package com.vbwd.plugin.subscription.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.vbwd.core.cart.Cart
import com.vbwd.core.cart.CartItem
import com.vbwd.core.networking.ApiError
import com.vbwd.plugin.subscription.domain.AddOn
import com.vbwd.plugin.subscription.domain.Subscription
import com.vbwd.plugin.subscription.domain.SubscriptionService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val PADDING = 16.dp

/** Add-ons listing. Port of the iOS `AddOnsViewModel`. */
class AddOnsViewModel(
    private val service: SubscriptionService,
    private val cart: Cart,
) {
    data class UiState(
        val isLoading: Boolean = true,
        val addons: List<AddOn> = emptyList(),
        val currentSubscription: Subscription? = null,
        val errorMessage: String? = null,
    ) {
        val subscriptionAddons: List<AddOn> get() = addons.filter { it.isSubscriptionDependent }
        val globalAddons: List<AddOn> get() = addons.filterNot { it.isSubscriptionDependent }
        val hasActiveSub: Boolean get() = currentSubscription?.isActive == true
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    suspend fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        try {
            coroutineScope {
                val addons = async { service.fetchAddOns() }
                val sub = async { service.fetchActiveSub() }
                _uiState.value = UiState(
                    isLoading = false,
                    addons = addons.await().filter { it.isActive == true },
                    currentSubscription = sub.await(),
                )
            }
        } catch (error: ApiError) {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.message)
        }
    }

    fun addToCart(addon: AddOn) {
        cart.add(
            CartItem(
                type = "add_on",
                id = addon.id,
                name = addon.name,
                price = addon.price?.toDoubleOrNull() ?: 0.0,
                currency = addon.currency ?: "USD",
                metadata = mapOf(
                    "slug" to (addon.slug ?: ""),
                    "billing_period" to (addon.billingPeriod ?: "month"),
                ),
            ),
        )
    }
}

@Composable
fun AddOnsScreen(viewModel: AddOnsViewModel) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(PADDING).testTag("addons_screen"),
        verticalArrangement = Arrangement.spacedBy(PADDING),
    ) {
        items(state.addons, key = { it.id }) { addon ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(PADDING)) {
                    Text(addon.name, style = MaterialTheme.typography.titleMedium)
                    Text("${addon.currency ?: "USD"} ${addon.price ?: "0"}")
                    Button(
                        onClick = { viewModel.addToCart(addon) },
                        modifier = Modifier.testTag("addon_add_${addon.id}"),
                    ) {
                        Text("Add to cart")
                    }
                }
            }
        }
    }
}
