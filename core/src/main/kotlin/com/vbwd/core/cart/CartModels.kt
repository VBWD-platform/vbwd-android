package com.vbwd.core.cart

import com.vbwd.core.store.TokenBundle

/**
 * Generic cart item. Port of the iOS `CartItem` / web `ICartItem`. The [type]
 * discriminator is an opaque string — core never switches on it; plugins set
 * domain types (`"token_bundle"`, `"subscription"`, `"shop_product"`) so the
 * checkout-source registry can match items to the right handler.
 */
data class CartItem(
    val type: String,
    val id: String,
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val currency: String = "USD",
    val metadata: Map<String, String> = emptyMap(),
)

/**
 * Context passed when navigating to checkout. Port of the iOS `CheckoutContext`
 * (the web's `?tarif_plan_id=…&source=…` query params, passed programmatically).
 */
data class CheckoutContext(
    val source: String? = null,
    val planSlug: String? = null,
    val isCart: Boolean = true,
    val extras: Map<String, String> = emptyMap(),
)

/** Convert a token bundle to a generic cart item. Port of `TokenBundle.toCartItem`. */
fun TokenBundle.toCartItem(): CartItem = CartItem(
    type = "token_bundle",
    id = id,
    name = name,
    price = price.toDoubleOrNull() ?: 0.0,
    quantity = 1,
    currency = currency ?: "USD",
    metadata = mapOf(
        "token_amount" to tokenAmount.toString(),
        "slug" to (slug ?: ""),
    ),
)
