package com.vbwd.plugin.subscription.domain

import com.vbwd.core.cart.Cart
import com.vbwd.core.cart.CartItem
import com.vbwd.core.cart.CheckoutContext
import com.vbwd.core.store.CheckoutInvoice
import com.vbwd.core.store.CheckoutResult
import com.vbwd.plugin.subscription.testutil.FakeApi
import com.vbwd.plugin.subscription.testutil.FakeSubscriptionService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SubscriptionCheckoutSourceTest {
    private fun subItem(id: String, price: Double = 29.99) =
        CartItem(type = "subscription", id = id, name = id, price = price)

    @Test
    fun `matches the source hint, a plan slug, and subscription cart items`() {
        val cart = Cart()
        val source = SubscriptionCheckoutSource(FakeApi(), cart, FakeSubscriptionService())
        assertTrue(source.matches(CheckoutContext(source = "subscription")))
        assertFalse(source.matches(CheckoutContext(source = "shop")))
        assertTrue(source.matches(CheckoutContext(planSlug = "pro")))
        assertFalse(source.matches(CheckoutContext()))
        cart.add(subItem("p1"))
        assertTrue(source.matches(CheckoutContext()))
    }

    @Test
    fun `load is cart-first and computes the total`() = runTest {
        val cart = Cart()
        cart.add(subItem("p1", price = 10.0))
        val source = SubscriptionCheckoutSource(FakeApi(), cart, FakeSubscriptionService())
        source.load(CheckoutContext(source = "subscription"))
        assertEquals(listOf("p1"), source.lineItems().map { it.id })
        assertEquals(10.0, source.orderTotal())
    }

    @Test
    fun `slug-driven load fetches the plan and adds it to the cart`() = runTest {
        val cart = Cart()
        val service = FakeSubscriptionService(
            plan = TarifPlan(id = "p1", name = "Pro", slug = "pro", displayPrice = 5.0),
        )
        val source = SubscriptionCheckoutSource(FakeApi(), cart, service)
        source.load(CheckoutContext(planSlug = "pro", isCart = false))
        assertEquals(listOf("p1"), source.lineItems().map { it.id })
        assertEquals(1, cart.allItems().size)
    }

    @Test
    fun `submit posts a plan_id payload and returns the result`() = runTest {
        val cart = Cart()
        cart.add(subItem("p1"))
        val api = FakeApi(nextResult = CheckoutResult(invoice = CheckoutInvoice(id = "inv1")))
        val source = SubscriptionCheckoutSource(api, cart, FakeSubscriptionService())
        source.load(CheckoutContext(source = "subscription"))

        val result = source.submit("invoice")

        assertEquals("inv1", result.invoiceId)
        assertEquals("/user/checkout", api.lastPath)
        assertTrue(api.lastBody?.contains("plan_id") == true)
    }
}
