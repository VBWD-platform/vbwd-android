package com.vbwd.core.ui.store

import com.vbwd.core.cart.Cart
import com.vbwd.core.store.TokenBundle
import com.vbwd.core.testutil.FakeApiClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BuyTokensViewModelTest {
    @Test
    fun `addToCart adds the bundle as a token_bundle line item`() {
        val cart = Cart()
        val vm = BuyTokensViewModel(FakeApiClient(), cart)
        vm.addToCart(TokenBundle(id = "b1", name = "Starter", tokenAmount = 600, price = "29.99"))
        assertEquals(1, cart.allItems().size)
        assertEquals("token_bundle", cart.allItems().first().type)
        assertEquals("b1", cart.allItems().first().id)
    }
}
