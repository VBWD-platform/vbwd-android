package com.vbwd.core.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vbwd.core.domain.AuthUser
import com.vbwd.core.domain.DashboardEndpoints
import com.vbwd.core.domain.Invoice
import com.vbwd.core.domain.InvoicesResponse
import com.vbwd.core.domain.PermissionEvaluator
import com.vbwd.core.domain.TokenBalanceResponse
import com.vbwd.core.domain.TokenTransaction
import com.vbwd.core.domain.TokenTransactionsResponse
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.get
import com.vbwd.core.plugins.ComponentFactory
import com.vbwd.core.plugins.PluginHost
import com.vbwd.core.session.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Generic user dashboard logic. Port of the iOS `DashboardViewModel` /
 * `Dashboard.vue`: profile summary always; token/invoice cards gated by
 * `user_permissions` via [PermissionEvaluator] (same rule as the rest of the
 * SDK); per-card data fetched concurrently, individual failures tolerated.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val api: ApiClient,
    session: AuthSession,
    host: PluginHost,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val tokenBalance: Double = 0.0,
        val invoices: List<Invoice> = emptyList(),
        val tokenTransactions: List<TokenTransaction> = emptyList(),
    ) {
        val recentInvoices: List<Invoice> get() = invoices.take(RECENT_INVOICE_LIMIT)
    }

    private val endpoints = DashboardEndpoints()
    private val evaluator = PermissionEvaluator()
    private val components = host.components
    private val user: AuthUser? = session.currentUser

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val userName: String get() = user?.name ?: "User"
    val userEmail: String get() = user?.email ?: ""

    val userInitials: String
        get() {
            val parts = userName.split(" ").filter { it.isNotEmpty() }
            return if (parts.size >= 2) {
                (parts[0].take(1) + parts[1].take(1)).uppercase()
            } else {
                userName.take(2).uppercase()
            }
        }

    private val grantedPermissions: List<String> get() = user?.userPermissions ?: emptyList()
    val showTokenCard: Boolean get() = evaluator.has("subscription.tokens.view", grantedPermissions)
    val showInvoicesCard: Boolean get() = evaluator.has("subscription.invoices.view", grantedPermissions)

    /** Plugin-contributed `Dashboard*` widgets, in registration order. */
    val pluginWidgets: List<Pair<String, ComponentFactory>> get() = components.dashboardComponents()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val invoices = async { fetchInvoices() }
            val balance = async { fetchBalance() }
            val transactions = async { fetchTransactions() }
            awaitAll(invoices, balance, transactions)
            _uiState.value = UiState(
                isLoading = false,
                tokenBalance = balance.await(),
                invoices = invoices.await(),
                tokenTransactions = transactions.await(),
            )
        }
    }

    // Per-card fetches never throw — a failed card is empty, the screen stays
    // (web `.catch(() => …)`). runCatching also tolerates the absent A04 endpoints.
    private suspend fun fetchInvoices(): List<Invoice> =
        runCatching { api.get<InvoicesResponse>(endpoints.invoices) }.getOrNull()?.invoices ?: emptyList()

    private suspend fun fetchBalance(): Double =
        runCatching { api.get<TokenBalanceResponse>(endpoints.tokenBalance) }.getOrNull()?.balance ?: 0.0

    private suspend fun fetchTransactions(): List<TokenTransaction> =
        runCatching { api.get<TokenTransactionsResponse>(endpoints.tokenTransactions) }
            .getOrNull()?.transactions ?: emptyList()

    private companion object {
        const val RECENT_INVOICE_LIMIT = 5
    }
}
