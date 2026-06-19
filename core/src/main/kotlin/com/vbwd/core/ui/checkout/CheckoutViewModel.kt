package com.vbwd.core.ui.checkout

import com.vbwd.core.cart.Cart
import com.vbwd.core.cart.CartItem
import com.vbwd.core.cart.CheckoutContext
import com.vbwd.core.checkout.CheckoutSource
import com.vbwd.core.checkout.CheckoutSourceRegistry
import com.vbwd.core.events.AppEvents
import com.vbwd.core.events.EventBus
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.get
import com.vbwd.core.plugins.ComponentFactory
import com.vbwd.core.plugins.PaymentAction
import com.vbwd.core.plugins.registries.ComponentRegistry
import com.vbwd.core.store.CheckoutResult
import com.vbwd.core.store.PaymentMethod
import com.vbwd.core.store.PaymentMethodsResponse
import com.vbwd.core.store.StoreEndpoints
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Checkout phase. Drives the screen: Form → ProcessingPayment → Confirmation. */
sealed interface CheckoutPhase {
    data object Form : CheckoutPhase
    data class ProcessingPayment(val url: String, val sessionId: String?) : CheckoutPhase
    data class Confirmation(val result: CheckoutResult) : CheckoutPhase
}

/**
 * Checkout orchestrator. Port of the iOS `CheckoutViewModel`: finds the matching
 * [CheckoutSource] for the context, delegates `load`/`submit` to it, manages
 * payment-method selection + phase transitions, and routes the post-submit
 * payment action. Core never names a domain — it only delegates.
 *
 * A plain class (not a `ViewModel`) because it needs a per-checkout
 * [CheckoutContext]; the screen calls its suspend methods from a coroutine.
 */
class CheckoutViewModel(
    private val api: ApiClient,
    private val context: CheckoutContext,
    private val cart: Cart,
    private val checkoutSources: CheckoutSourceRegistry,
    private val endpoints: StoreEndpoints = StoreEndpoints(),
    private val components: ComponentRegistry? = null,
    private val events: EventBus? = null,
) {
    data class UiState(
        val isLoading: Boolean = false,
        val isSubmitting: Boolean = false,
        val paymentMethods: List<PaymentMethod> = emptyList(),
        val selectedMethodId: String? = null,
        val lineItems: List<CartItem> = emptyList(),
        val phase: CheckoutPhase = CheckoutPhase.Form,
        val errorMessage: String? = null,
        val checkoutResult: CheckoutResult? = null,
    )

    private var activeSource: CheckoutSource? = null

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun checkoutComponents(): List<Pair<String, ComponentFactory>> =
        components?.checkoutComponents() ?: emptyList()

    fun paymentMethodDetail(code: String): ComponentFactory? = components?.paymentMethodDetail(code)

    fun orderTotal(): Double {
        val items = _uiState.value.lineItems
        val fromItems = items.sumOf { it.price * it.quantity }
        if (context.isCart && context.source == null) return fromItems
        return activeSource?.orderTotal() ?: fromItems
    }

    fun currency(): String = _uiState.value.lineItems.firstOrNull()?.currency ?: "USD"

    fun isZeroTotal(): Boolean = orderTotal() <= 0

    fun canSubmit(): Boolean = with(_uiState.value) {
        selectedMethodId != null && !isSubmitting && lineItems.isNotEmpty()
    }

    fun selectMethod(code: String) {
        _uiState.update { it.copy(selectedMethodId = code) }
    }

    suspend fun loadForContext() {
        _uiState.update { it.copy(isLoading = true) }
        events?.emit(AppEvents.CHECKOUT_STARTED)

        val source = checkoutSources.find(context)
        activeSource = source

        val items = resolveLineItems(source)
        _uiState.update { it.copy(lineItems = items) }

        loadPaymentMethods()
        _uiState.update { it.copy(isLoading = false) }
    }

    private suspend fun resolveLineItems(source: CheckoutSource?): List<CartItem> = when {
        context.isCart && context.source == null -> {
            source?.let { loadSourceQuietly(it) }
            cart.allItems()
        }
        source != null -> {
            try {
                source.load(context)
                source.lineItems()
            } catch (error: com.vbwd.core.networking.ApiError) {
                _uiState.update { it.copy(errorMessage = error.message) }
                emptyList()
            }
        }
        else -> cart.allItems()
    }

    private suspend fun loadSourceQuietly(source: CheckoutSource) {
        runCatching { source.load(context) }
    }

    private suspend fun loadPaymentMethods() {
        val all = runCatching { api.get<PaymentMethodsResponse>(endpoints.paymentMethods) }
            .getOrNull()?.methods ?: emptyList()

        if (isZeroTotal()) {
            val invoiceOnly = all.filter { it.code.lowercase() == "invoice" }
            _uiState.update {
                it.copy(paymentMethods = invoiceOnly, selectedMethodId = invoiceOnly.firstOrNull()?.code)
            }
            return
        }

        val billingPeriods = _uiState.value.lineItems
            .mapNotNull { it.metadata["billing_period"] }
            .filter { it.isNotEmpty() }
            .toSet()
        val mixedBillingIntervals = billingPeriods.size > 1

        val supported = components?.supportedPaymentMethodCodes()
        var filtered = if (!supported.isNullOrEmpty()) all.filter { it.code in supported } else all
        if (mixedBillingIntervals) filtered = filtered.filter { it.code.lowercase() != "stripe" }

        _uiState.update { it.copy(paymentMethods = filtered) }
    }

    // Broad catch: a third-party payment-action handler may throw anything; we
    // surface it as an error (cancellation re-thrown), never a false success.
    @Suppress("TooGenericExceptionCaught")
    suspend fun submit() {
        val methodCode = _uiState.value.selectedMethodId ?: return
        val source = activeSource ?: return

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        try {
            val result = source.submit(methodCode)
            _uiState.update { it.copy(checkoutResult = result) }

            val invoiceId = result.invoiceId
            if (invoiceId == null) {
                finishCheckout(result)
            } else {
                routePaymentAction(methodCode, invoiceId, result)
            }
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            _uiState.update { it.copy(errorMessage = error.message ?: "Checkout failed") }
            events?.emit(AppEvents.CHECKOUT_FAILED)
        }
        _uiState.update { it.copy(isSubmitting = false) }
    }

    private suspend fun routePaymentAction(methodCode: String, invoiceId: String, result: CheckoutResult) {
        val handler = components?.paymentAction(methodCode)
        if (handler == null) {
            finishCheckout(result)
            return
        }
        when (val action = handler(invoiceId)) {
            PaymentAction.ShowConfirmation -> finishCheckout(result.withStatus("paid"))
            is PaymentAction.OpenUrl ->
                _uiState.update { it.copy(phase = CheckoutPhase.ProcessingPayment(action.url, action.sessionId)) }
        }
    }

    /** Called when the user returns from an external payment page. */
    fun completePayment(callbackUrl: String?) {
        if (callbackUrl == null || callbackUrl.lowercase().contains("cancel")) {
            _uiState.update {
                it.copy(errorMessage = "Payment was cancelled.", phase = CheckoutPhase.Form, isSubmitting = false)
            }
            return
        }
        val result = _uiState.value.checkoutResult
        if (result != null) finishCheckout(result) else _uiState.update { it.copy(phase = CheckoutPhase.Form) }
    }

    private fun finishCheckout(result: CheckoutResult) {
        _uiState.update { it.copy(phase = CheckoutPhase.Confirmation(result)) }
        cart.clear()
        events?.emit(AppEvents.CHECKOUT_COMPLETED)
    }
}
