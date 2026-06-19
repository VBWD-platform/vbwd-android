package com.vbwd.core.ui.billing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private val PADDING = 16.dp

/** Paginated, searchable invoice list. Port of the iOS `InvoicesView`. */
@Composable
fun InvoicesScreen(viewModel: InvoicesViewModel, onOpen: (String) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().padding(PADDING).testTag("invoices_screen")) {
        OutlinedTextField(
            value = state.searchText,
            onValueChange = viewModel::setSearch,
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        val invoices = state.filteredInvoices
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(invoices, key = { it.id }) { invoice ->
                LaunchedEffect(invoice.id) { viewModel.loadMoreIfNeeded(invoice) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(invoice.id) }
                        .padding(vertical = PADDING),
                ) {
                    Text(invoice.invoiceNumber ?: invoice.id, style = MaterialTheme.typography.titleSmall)
                    Text("${invoice.totalAmount ?: invoice.amount ?: ""} · ${invoice.status ?: ""}")
                }
            }
        }
    }
}
