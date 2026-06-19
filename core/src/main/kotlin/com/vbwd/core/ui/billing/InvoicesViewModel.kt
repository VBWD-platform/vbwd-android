package com.vbwd.core.ui.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vbwd.core.domain.DashboardEndpoints
import com.vbwd.core.domain.Invoice
import com.vbwd.core.domain.InvoicesResponse
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.get
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Paginated invoice list (10/page) with local search. Port of the iOS
 * `InvoicesViewModel`: first page on [load], more via [loadMoreIfNeeded].
 */
@HiltViewModel
class InvoicesViewModel @Inject constructor(
    private val api: ApiClient,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val invoices: List<Invoice> = emptyList(),
        val allLoaded: Boolean = false,
        val searchText: String = "",
    ) {
        val filteredInvoices: List<Invoice>
            get() {
                if (searchText.isEmpty()) return invoices
                val query = searchText.lowercase()
                return invoices.filter { inv ->
                    (inv.invoiceNumber ?: "").lowercase().contains(query) ||
                        (inv.amount ?: "").lowercase().contains(query) ||
                        (inv.totalAmount ?: "").lowercase().contains(query) ||
                        (inv.status ?: "").lowercase().contains(query) ||
                        (inv.invoicedAt ?: "").lowercase().contains(query) ||
                        inv.id.lowercase().contains(query)
                }
            }
    }

    private val endpoints = DashboardEndpoints()
    private var currentOffset = 0

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun setSearch(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            currentOffset = 0
            val fetched = fetchPage(0)
            currentOffset = fetched.size
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                invoices = fetched,
                allLoaded = fetched.size < PAGE_SIZE,
            )
        }
    }

    fun loadMoreIfNeeded(current: Invoice) {
        val state = _uiState.value
        if (state.filteredInvoices.lastOrNull()?.id != current.id) return
        if (state.isLoadingMore || state.allLoaded) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            val fetched = fetchPage(currentOffset)
            currentOffset += fetched.size
            _uiState.value = _uiState.value.copy(
                isLoadingMore = false,
                invoices = _uiState.value.invoices + fetched,
                allLoaded = fetched.size < PAGE_SIZE,
            )
        }
    }

    private suspend fun fetchPage(offset: Int): List<Invoice> =
        runCatching { api.get<InvoicesResponse>("${endpoints.invoices}?limit=$PAGE_SIZE&offset=$offset") }
            .getOrNull()?.invoices ?: emptyList()

    private companion object {
        const val PAGE_SIZE = 10
    }
}
