package com.vbwd.core.ui.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private val PADDING = 16.dp
private const val FULL_WEIGHT = 1f

/** Invoice detail with line items. Port of the iOS `InvoiceDetailView`. */
@Composable
fun InvoiceDetailScreen(viewModel: InvoiceDetailViewModel) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel.invoiceId) { viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PADDING)
            .testTag("invoice_detail_screen"),
        verticalArrangement = Arrangement.spacedBy(PADDING),
    ) {
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.invoice?.let { invoice ->
            Text(invoice.invoiceNumber ?: invoice.id, style = MaterialTheme.typography.headlineSmall)
            Text("Status: ${invoice.status ?: "—"}")
            Text("Total: ${invoice.totalAmount ?: invoice.amount ?: "—"} ${invoice.currency ?: ""}")
            HorizontalDivider()
            invoice.lineItems.orEmpty().forEach { line ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(line.description ?: "", modifier = Modifier.weight(FULL_WEIGHT))
                    Text(line.amount ?: "")
                }
            }
        }
    }
}
