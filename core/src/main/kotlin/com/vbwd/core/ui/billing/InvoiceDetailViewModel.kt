package com.vbwd.core.ui.billing

import com.vbwd.core.domain.Invoice
import com.vbwd.core.domain.InvoiceDetailResponse
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Invoice detail (with line items) from `GET /user/invoices/{id}`. Port of the
 * iOS `InvoiceDetailViewModel`. A plain class (needs a per-invoice id); the
 * screen calls [load] from a coroutine. PDF download is deferred (no consumer
 * yet — the `getData` byte path lands when a screen needs it).
 */
class InvoiceDetailViewModel(
    private val api: ApiClient,
    val invoiceId: String,
) {
    data class UiState(
        val isLoading: Boolean = false,
        val invoice: Invoice? = null,
        val errorMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    suspend fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        val invoice = runCatching { api.get<InvoiceDetailResponse>("/user/invoices/$invoiceId") }
            .getOrNull()?.invoice
        _uiState.value = UiState(
            isLoading = false,
            invoice = invoice,
            errorMessage = if (invoice == null) "Invoice not found." else null,
        )
    }
}
