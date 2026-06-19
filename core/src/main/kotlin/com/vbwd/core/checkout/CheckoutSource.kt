package com.vbwd.core.checkout

import com.vbwd.core.cart.CartItem
import com.vbwd.core.cart.CheckoutContext
import com.vbwd.core.plugins.ComponentFactory
import com.vbwd.core.store.CheckoutResult

/**
 * A checkout flow a plugin contributes (subscription, shop, …) plus the built-in
 * token-bundle source. Port of the iOS `CheckoutSource`: core discovers the
 * matching source via [CheckoutSourceRegistry] and delegates all domain logic to
 * it (DIP — core never names a domain).
 *
 * **Liskov:** an operation a source does not support must **raise** (e.g.
 * `UnsupportedOperationException`), never return a false success.
 */
interface CheckoutSource {
    /** Unique id (e.g. `"token_bundle"`, `"subscription"`, `"shop"`). */
    val id: String

    /** Higher priority wins when multiple sources match. Default 0. */
    val priority: Int get() = 0

    /** Does this source handle the given context? */
    fun matches(context: CheckoutContext): Boolean

    /** Load items for the context (fetch a plan, read the cart, …). */
    suspend fun load(context: CheckoutContext)

    /** Project loaded state into generic cart items for the checkout UI. */
    fun lineItems(): List<CartItem>

    /** Computed order total from the loaded items. */
    fun orderTotal(): Double

    /** Build + post the source's own request body; return the standard result. */
    suspend fun submit(paymentMethodCode: String?): CheckoutResult

    /** Clear loaded state so the source can be reused. */
    fun reset()

    /** Optional custom summary view; when non-null the checkout renders it. */
    val summaryComponent: ComponentFactory? get() = null
}

/**
 * Registry of [CheckoutSource]s. Port of the iOS `CheckoutSourceRegistry`:
 * [find] returns the highest-priority source whose [CheckoutSource.matches] is
 * true; registering an existing id replaces it.
 */
class CheckoutSourceRegistry {
    private val sources = mutableListOf<CheckoutSource>()

    fun register(source: CheckoutSource) {
        sources.removeAll { it.id == source.id }
        sources.add(source)
    }

    fun unregister(id: String) {
        sources.removeAll { it.id == id }
    }

    /** Highest-priority matching source (ties broken by registration order). */
    fun find(context: CheckoutContext): CheckoutSource? =
        sources.filter { it.matches(context) }.maxByOrNull { it.priority }

    fun get(id: String): CheckoutSource? = sources.firstOrNull { it.id == id }

    fun all(): List<CheckoutSource> = sources.toList()
}
