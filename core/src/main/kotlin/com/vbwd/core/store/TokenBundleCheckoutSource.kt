package com.vbwd.core.store

import com.vbwd.core.cart.Cart
import com.vbwd.core.cart.CartItem
import com.vbwd.core.cart.CheckoutContext
import com.vbwd.core.checkout.CheckoutSource
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.post
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Built-in checkout source for token bundles — the only domain-specific source
 * in `:core` (token bundles are a core backend feature, not a plugin). Plugin
 * sources (subscription, shop) register via `PlatformSdk.addCheckoutSource`.
 * Port of the iOS `TokenBundleCheckoutSource`.
 */
class TokenBundleCheckoutSource(
    private val api: ApiClient,
    private val cart: Cart,
    private val endpoints: StoreEndpoints = StoreEndpoints(),
) : CheckoutSource {
    override val id = "token_bundle"
    override val priority = 0

    private var loadedItems: List<CartItem> = emptyList()

    override fun matches(context: CheckoutContext): Boolean =
        context.source?.let { it == "token_bundle" }
            ?: cart.items("token_bundle").isNotEmpty()

    override suspend fun load(context: CheckoutContext) {
        loadedItems = cart.items("token_bundle")
    }

    override fun lineItems(): List<CartItem> = loadedItems

    override fun orderTotal(): Double = loadedItems.sumOf { it.price * it.quantity }

    override suspend fun submit(paymentMethodCode: String?): CheckoutResult {
        val request = TokenBundleCheckoutRequest(
            tokenBundleIds = loadedItems.map { it.id },
            addOnIds = cart.items("add_on").map { it.id },
            currency = loadedItems.firstOrNull()?.currency ?: "USD",
            paymentMethodCode = paymentMethodCode ?: "",
        )
        return api.post(endpoints.checkout, request)
    }

    override fun reset() {
        loadedItems = emptyList()
    }
}

/** Request body for `POST /user/checkout` with token-bundle ids. Source-owned. */
@Serializable
private data class TokenBundleCheckoutRequest(
    @SerialName("token_bundle_ids") val tokenBundleIds: List<String>,
    @SerialName("add_on_ids") val addOnIds: List<String>,
    val currency: String,
    @SerialName("payment_method_code") val paymentMethodCode: String,
)
