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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vbwd.core.cart.Cart
import com.vbwd.core.cart.CheckoutContext
import com.vbwd.core.networking.ApiError
import com.vbwd.core.ui.checkout.CheckoutScreen
import com.vbwd.core.ui.checkout.CheckoutViewModel
import com.vbwd.plugin.subscription.domain.Subscription
import com.vbwd.plugin.subscription.domain.SubscriptionService
import com.vbwd.plugin.subscription.domain.TarifPlan
import com.vbwd.plugin.subscription.domain.toCartItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val PADDING = 16.dp

/** Tarif-plans listing. Port of the iOS `TarifPlansViewModel`. */
class TarifPlansViewModel(
    private val service: SubscriptionService,
    private val cart: Cart,
) {
    data class UiState(
        val isLoading: Boolean = true,
        val plans: List<TarifPlan> = emptyList(),
        val currentSubscription: Subscription? = null,
        val errorMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val currentPlanSlug: String? get() = _uiState.value.currentSubscription?.plan?.slug

    suspend fun load(currency: String = "USD") {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        try {
            coroutineScope {
                val plans = async { service.fetchPlans(currency) }
                val sub = async { service.fetchActiveSub() }
                _uiState.value = UiState(isLoading = false, plans = plans.await(), currentSubscription = sub.await())
            }
        } catch (error: ApiError) {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.message)
        }
    }

    /** Add the plan to the cart (the generic checkout takes it from there). */
    fun selectPlan(plan: TarifPlan) {
        cart.add(plan.toCartItem())
    }
}

@Composable
fun TarifPlansScreen(
    viewModel: TarifPlansViewModel,
    checkoutFactory: (CheckoutContext) -> CheckoutViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    var checkout by remember { mutableStateOf<CheckoutViewModel?>(null) }
    LaunchedEffect(Unit) { viewModel.load() }

    val active = checkout
    if (active != null) {
        CheckoutScreen(active, onDone = { checkout = null })
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(PADDING).testTag("tarif_plans_screen"),
        verticalArrangement = Arrangement.spacedBy(PADDING),
    ) {
        items(state.plans, key = { it.id }) { plan ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(PADDING)) {
                    Text(plan.name, style = MaterialTheme.typography.titleMedium)
                    Text(plan.formattedPrice)
                    Button(
                        onClick = {
                            viewModel.selectPlan(plan)
                            checkout = checkoutFactory(CheckoutContext(source = "subscription", isCart = true))
                        },
                        modifier = Modifier.testTag("subscribe_${plan.id}"),
                    ) {
                        Text("Subscribe")
                    }
                }
            }
        }
    }
}
