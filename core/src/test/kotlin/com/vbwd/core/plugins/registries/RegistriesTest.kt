package com.vbwd.core.plugins.registries

import com.vbwd.core.plugins.PluginRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegistriesTest {
    private fun route(path: String, name: String) = PluginRoute(path = path, name = name) {}

    @Test
    fun `route registry rejects duplicate path and name`() {
        val registry = RouteRegistry()
        registry.add(route("/a", "a"))
        assertThrows(RegistryError.DuplicateRoutePath::class.java) { registry.add(route("/a", "b")) }
        assertThrows(RegistryError.DuplicateRouteName::class.java) { registry.add(route("/c", "a")) }
        assertEquals(1, registry.all().size)
    }

    @Test
    fun `component registry discovers Dashboard and Profile in registration order`() {
        val registry = ComponentRegistry()
        registry.add("DashboardA") {}
        registry.add("Other") {}
        registry.add("ProfileX") {}
        registry.add("DashboardB") {}

        assertEquals(listOf("DashboardA", "DashboardB"), registry.dashboardComponents().map { it.first })
        assertEquals(listOf("ProfileX"), registry.profileComponents().map { it.first })
    }

    @Test
    fun `component registry payment action lookup is case-insensitive`() {
        val registry = ComponentRegistry()
        val handler: com.vbwd.core.plugins.PaymentActionHandler =
            { com.vbwd.core.plugins.PaymentAction.ShowConfirmation }
        registry.addPaymentAction("Stripe", handler)
        assertTrue(registry.paymentAction("stripe") != null)
        assertTrue(registry.paymentAction("STRIPE") != null)
        assertTrue(registry.paymentAction("paypal") == null)
    }

    @Test
    fun `store registry rejects duplicate ids`() {
        val registry = StoreRegistry()
        registry.create("s", Any())
        assertThrows(RegistryError.DuplicateStoreId::class.java) { registry.create("s", Any()) }
    }

    @Test
    fun `localization merge is last-write-wins per key`() {
        val registry = LocalizationRegistry()
        registry.add("en", mapOf("k1" to "a", "k2" to "x"))
        registry.add("en", mapOf("k1" to "b"))
        assertEquals("b", registry.t("k1", "en"))
        assertEquals("x", registry.t("k2", "en"))
        assertEquals("missing", registry.t("missing", "en")) // fallback to key
    }

    @Test
    fun `menu items sort by order`() {
        val registry = MenuItemRegistry()
        registry.add(MenuItem(id = "b", icon = "i", title = "B", order = 20))
        registry.add(MenuItem(id = "a", icon = "i", title = "A", order = 10))
        assertEquals(listOf("a", "b"), registry.all().map { it.id })
    }
}
