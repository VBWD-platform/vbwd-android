package com.vbwd.core.testutil

import com.vbwd.core.cart.CartItem
import com.vbwd.core.cart.CheckoutContext
import com.vbwd.core.checkout.CheckoutSource
import com.vbwd.core.store.CheckoutInvoice
import com.vbwd.core.store.CheckoutResult

/** Configurable [CheckoutSource] test double for registry + checkout-VM tests. */
class FakeCheckoutSource(
    override val id: String,
    override val priority: Int = 0,
    private val matchesContext: Boolean = true,
    private val items: List<CartItem> = emptyList(),
    private val submitResult: CheckoutResult = CheckoutResult(invoice = CheckoutInvoice(id = "inv")),
    private val submitError: Throwable? = null,
) : CheckoutSource {
    var loaded = false

    override fun matches(context: CheckoutContext): Boolean = matchesContext
    override suspend fun load(context: CheckoutContext) {
        loaded = true
    }
    override fun lineItems(): List<CartItem> = items
    override fun orderTotal(): Double = items.sumOf { it.price * it.quantity }
    override suspend fun submit(paymentMethodCode: String?): CheckoutResult {
        submitError?.let { throw it }
        return submitResult
    }
    override fun reset() = Unit
}
