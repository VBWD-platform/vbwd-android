package com.vbwd.core.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vbwd.core.cart.Cart
import com.vbwd.core.cart.toCartItem
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.get
import com.vbwd.core.store.StoreEndpoints
import com.vbwd.core.store.TokenBundle
import com.vbwd.core.store.TokenBundlesResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Token-bundle storefront. Port of the iOS `BuyTokensViewModel` (SRP — separate
 * from the balance/history screen). The buy flow adds a bundle to the [Cart].
 */
@HiltViewModel
class BuyTokensViewModel @Inject constructor(
    private val api: ApiClient,
    private val cart: Cart,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val bundles: List<TokenBundle> = emptyList(),
    )

    private val endpoints = StoreEndpoints()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val bundles = runCatching { api.get<TokenBundlesResponse>(endpoints.tokenBundles) }
                .getOrNull()?.bundles.orEmpty()
                .sortedBy { it.sortOrder ?: Int.MAX_VALUE }
            _uiState.value = UiState(isLoading = false, bundles = bundles)
        }
    }

    /** Buy flow: add the bundle to the cart as a generic line item. */
    fun addToCart(bundle: TokenBundle) {
        cart.add(bundle.toCartItem())
    }
}
