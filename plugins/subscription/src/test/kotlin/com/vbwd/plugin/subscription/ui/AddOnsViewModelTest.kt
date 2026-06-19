package com.vbwd.plugin.subscription.ui

import com.vbwd.core.cart.Cart
import com.vbwd.plugin.subscription.domain.AddOn
import com.vbwd.plugin.subscription.testutil.FakeSubscriptionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AddOnsViewModelTest {
    @Test
    fun `addToCart adds an add_on line item with metadata`() {
        val cart = Cart()
        val vm = AddOnsViewModel(FakeSubscriptionService(), cart)
        vm.addToCart(
            AddOn(id = "a1", name = "Extra", price = "4.99", currency = "EUR", slug = "extra", billingPeriod = "month"),
        )

        val item = cart.allItems().single()
        assertEquals("add_on", item.type)
        assertEquals("a1", item.id)
        assertEquals(4.99, item.price)
        assertEquals("EUR", item.currency)
        assertEquals("month", item.metadata["billing_period"])
    }

    @Test
    fun `ui state partitions add-ons into subscription-dependent and global`() {
        val state = AddOnsViewModel.UiState(
            addons = listOf(
                AddOn(id = "a", name = "Tied", tarifPlanIds = listOf("p1")),
                AddOn(id = "b", name = "Global"),
            ),
        )
        assertEquals(listOf("a"), state.subscriptionAddons.map { it.id })
        assertEquals(listOf("b"), state.globalAddons.map { it.id })
    }
}
