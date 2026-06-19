package com.vbwd.core.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vbwd.core.domain.DashboardEndpoints
import com.vbwd.core.domain.TokenBalanceResponse
import com.vbwd.core.domain.TokenTransaction
import com.vbwd.core.domain.TokenTransactionsResponse
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.get
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Token balance + full transaction history (the dashboard token card's dedicated
 * screen, no limit). Port of the iOS `TokensViewModel`: concurrent fetch,
 * per-call failure tolerance.
 */
@HiltViewModel
class TokensViewModel @Inject constructor(
    private val api: ApiClient,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val balance: Double = 0.0,
        val transactions: List<TokenTransaction> = emptyList(),
    )

    private val endpoints = DashboardEndpoints()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val balance = async { fetchBalance() }
            val transactions = async { fetchTransactions() }
            awaitAll(balance, transactions)
            _uiState.value = UiState(
                isLoading = false,
                balance = balance.await(),
                transactions = transactions.await(),
            )
        }
    }

    private suspend fun fetchBalance(): Double =
        runCatching { api.get<TokenBalanceResponse>(endpoints.tokenBalance) }.getOrNull()?.balance ?: 0.0

    private suspend fun fetchTransactions(): List<TokenTransaction> {
        val base = endpoints.tokenTransactions.substringBefore("?")
        return runCatching { api.get<TokenTransactionsResponse>("$base?limit=$HISTORY_LIMIT") }
            .getOrNull()?.transactions ?: emptyList()
    }

    private companion object {
        const val HISTORY_LIMIT = 33
    }
}
