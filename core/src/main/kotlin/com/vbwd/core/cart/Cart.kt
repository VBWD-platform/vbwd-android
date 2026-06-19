package com.vbwd.core.cart

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory, session-scoped cart. Port of the iOS `Cart` / web `cart.ts`: holds
 * generic [CartItem]s any plugin can add (token bundles, subscriptions, shop
 * products). Single source of truth (`StateFlow`); not persisted — lives for the
 * session. Core never switches on `CartItem.type`.
 */
class Cart {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    /** Bumped when any view requests the checkout sheet (the host observes it). */
    private val _checkoutRequests = MutableStateFlow(0)
    val checkoutRequests: StateFlow<Int> = _checkoutRequests.asStateFlow()

    fun requestCheckout() {
        _checkoutRequests.value += 1
    }

    /** Add an item, or increment quantity if one with the same id exists. */
    fun add(item: CartItem) {
        val current = _items.value
        val index = current.indexOfFirst { it.id == item.id }
        _items.value = if (index >= 0) {
            current.toMutableList().also { it[index] = it[index].copy(quantity = it[index].quantity + item.quantity) }
        } else {
            current + item
        }
    }

    fun remove(id: String) {
        _items.value = _items.value.filterNot { it.id == id }
    }

    /** Set quantity for an item; removes it when quantity <= 0. */
    fun updateQuantity(id: String, quantity: Int) {
        val current = _items.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return
        _items.value = if (quantity <= 0) {
            current.filterNot { it.id == id }
        } else {
            current.toMutableList().also { it[index] = it[index].copy(quantity = quantity) }
        }
    }

    fun clear() {
        _items.value = emptyList()
    }

    /** Current snapshot of all items. */
    fun allItems(): List<CartItem> = _items.value

    /** All items of a given type (e.g. `"token_bundle"`). */
    fun items(type: String): List<CartItem> = _items.value.filter { it.type == type }

    val total: Double get() = _items.value.sumOf { it.price * it.quantity }

    val isEmpty: Boolean get() = _items.value.isEmpty()

    val itemCount: Int get() = _items.value.sumOf { it.quantity }
}
