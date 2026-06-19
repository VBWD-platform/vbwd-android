package com.vbwd.core.cart

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CartTest {
    private fun item(id: String, type: String = "token_bundle", price: Double = 10.0, qty: Int = 1) =
        CartItem(type = type, id = id, name = id, price = price, quantity = qty)

    @Test
    fun `add appends new items and increments existing ones`() {
        val cart = Cart()
        cart.add(item("a", qty = 1))
        cart.add(item("a", qty = 2))
        cart.add(item("b"))
        assertEquals(2, cart.allItems().size)
        assertEquals(3, cart.allItems().first { it.id == "a" }.quantity)
    }

    @Test
    fun `total sums price times quantity`() {
        val cart = Cart()
        cart.add(item("a", price = 10.0, qty = 2))
        cart.add(item("b", price = 5.0, qty = 1))
        assertEquals(25.0, cart.total)
        assertEquals(3, cart.itemCount)
    }

    @Test
    fun `updateQuantity to zero removes the item`() {
        val cart = Cart()
        cart.add(item("a"))
        cart.updateQuantity("a", 0)
        assertTrue(cart.isEmpty)
    }

    @Test
    fun `remove and clear`() {
        val cart = Cart()
        cart.add(item("a"))
        cart.add(item("b"))
        cart.remove("a")
        assertEquals(listOf("b"), cart.allItems().map { it.id })
        cart.clear()
        assertTrue(cart.isEmpty)
    }

    @Test
    fun `items filters by type`() {
        val cart = Cart()
        cart.add(item("a", type = "token_bundle"))
        cart.add(item("b", type = "add_on"))
        assertEquals(listOf("a"), cart.items("token_bundle").map { it.id })
        assertFalse(cart.items("add_on").isEmpty())
    }
}
