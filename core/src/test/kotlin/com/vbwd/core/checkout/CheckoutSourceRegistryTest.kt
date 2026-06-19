package com.vbwd.core.checkout

import com.vbwd.core.cart.CheckoutContext
import com.vbwd.core.testutil.FakeCheckoutSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CheckoutSourceRegistryTest {
    private val ctx = CheckoutContext()

    @Test
    fun `find returns the highest-priority matching source`() {
        val registry = CheckoutSourceRegistry()
        registry.register(FakeCheckoutSource("low", priority = 0))
        registry.register(FakeCheckoutSource("high", priority = 10))
        assertEquals("high", registry.find(ctx)?.id)
    }

    @Test
    fun `find skips non-matching sources`() {
        val registry = CheckoutSourceRegistry()
        registry.register(FakeCheckoutSource("no", priority = 10, matchesContext = false))
        registry.register(FakeCheckoutSource("yes", priority = 0))
        assertEquals("yes", registry.find(ctx)?.id)
    }

    @Test
    fun `register replaces a same-id source and unregister removes it`() {
        val registry = CheckoutSourceRegistry()
        registry.register(FakeCheckoutSource("s", priority = 0))
        registry.register(FakeCheckoutSource("s", priority = 5))
        assertEquals(1, registry.all().size)
        assertEquals(5, registry.get("s")?.priority)
        registry.unregister("s")
        assertNull(registry.get("s"))
    }
}
