package com.vbwd.core.ui.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vbwd.core.store.CheckoutResult
import kotlinx.coroutines.launch

private val SCREEN_PADDING = 16.dp
private val ROW_SPACING = 12.dp
private const val FULL_WEIGHT = 1f

/**
 * Generic checkout screen driven by the selected `CheckoutSource`. Port of the
 * iOS `CheckoutView`: Form → (external) PaymentRedirect → Confirmation.
 */
@Composable
fun CheckoutScreen(viewModel: CheckoutViewModel, onDone: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.loadForContext() }

    when (val phase = state.phase) {
        CheckoutPhase.Form -> CheckoutForm(viewModel, state) { scope.launch { viewModel.submit() } }
        is CheckoutPhase.ProcessingPayment ->
            PaymentRedirectScreen(url = phase.url) { callbackUrl -> viewModel.completePayment(callbackUrl) }
        is CheckoutPhase.Confirmation -> CheckoutConfirmation(phase.result, onDone)
    }
}

@Composable
private fun CheckoutForm(
    viewModel: CheckoutViewModel,
    state: CheckoutViewModel.UiState,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SCREEN_PADDING)
            .testTag("checkout_screen"),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        Text("Checkout", style = MaterialTheme.typography.headlineSmall)

        state.lineItems.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(item.name, modifier = Modifier.weight(FULL_WEIGHT))
                Text("${item.currency} ${item.price}")
            }
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Total", modifier = Modifier.weight(FULL_WEIGHT))
            Text("${viewModel.currency()} ${viewModel.orderTotal()}")
        }

        Text("Payment method", style = MaterialTheme.typography.titleSmall)
        state.paymentMethods.forEach { method ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = state.selectedMethodId == method.code,
                        onClick = { viewModel.selectMethod(method.code) },
                    )
                    .testTag("payment_method_${method.code}"),
            ) {
                RadioButton(selected = state.selectedMethodId == method.code, onClick = null)
                Text(method.name, modifier = Modifier.padding(start = ROW_SPACING))
            }
            if (state.selectedMethodId == method.code) {
                val detail = viewModel.paymentMethodDetail(method.code)
                if (detail != null) {
                    CompositionLocalProvider(
                        LocalCheckoutInfo provides CheckoutInfo(viewModel.orderTotal(), viewModel.currency()),
                    ) {
                        detail()
                    }
                }
            }
        }

        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = onSubmit,
            enabled = viewModel.canSubmit(),
            modifier = Modifier.fillMaxWidth().testTag("checkout_submit"),
        ) {
            Text(if (state.isSubmitting) "Processing…" else "Pay")
        }
    }
}

@Composable
private fun CheckoutConfirmation(result: CheckoutResult, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(SCREEN_PADDING).testTag("checkout_confirmation"),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        Text("Order confirmed", style = MaterialTheme.typography.headlineSmall)
        result.invoice?.invoiceNumber?.let { Text("Invoice $it") }
        result.invoice?.status?.let { Text("Status: $it") }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}
