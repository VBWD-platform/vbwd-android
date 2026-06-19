package com.vbwd.core.store

import com.vbwd.core.cart.Cart
import com.vbwd.core.cart.CartItem
import com.vbwd.core.cart.CheckoutContext
import com.vbwd.core.testutil.FakeApiClient
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TokenBundleCheckoutSourceTest {
    private fun bundleItem(id: String, price: Double = 29.99) =
        CartItem(type = "token_bundle", id = id, name = id, price = price, currency = "USD")

    @Test
    fun `matches an explicit source hint and a cart with bundles`() {
        val cart = Cart()
        val source = TokenBundleCheckoutSource(FakeApiClient(), cart)
        assertTrue(source.matches(CheckoutContext(source = "token_bundle")))
        assertFalse(source.matches(CheckoutContext(source = "shop")))

        assertFalse(source.matches(CheckoutContext(source = null)))
        cart.add(bundleItem("b1"))
        assertTrue(source.matches(CheckoutContext(source = null)))
    }

    @Test
    fun `load projects cart bundles into line items and total`() = runTest {
        val cart = Cart()
        cart.add(bundleItem("b1", price = 10.0))
        cart.add(bundleItem("b2", price = 5.0))
        val source = TokenBundleCheckoutSource(FakeApiClient(), cart)

        source.load(CheckoutContext(source = "token_bundle"))

        assertEquals(listOf("b1", "b2"), source.lineItems().map { it.id })
        assertEquals(15.0, source.orderTotal())
    }

    @Test
    fun `submit posts to the checkout endpoint and returns the result`() = runTest {
        val cart = Cart()
        cart.add(bundleItem("b1"))
        val api = FakeApiClient(nextResult = CheckoutResult(invoice = CheckoutInvoice(id = "inv1")))
        val source = TokenBundleCheckoutSource(api, cart)
        source.load(CheckoutContext(source = "token_bundle"))

        val result = source.submit(paymentMethodCode = "invoice")

        assertEquals("inv1", result.invoiceId)
        assertEquals("/user/checkout", api.lastPath)
        assertTrue(api.lastBody?.contains("token_bundle_ids") == true)
    }
}
