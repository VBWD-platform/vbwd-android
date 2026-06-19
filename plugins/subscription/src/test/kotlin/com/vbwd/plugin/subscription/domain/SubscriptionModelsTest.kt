package com.vbwd.plugin.subscription.domain

import com.vbwd.core.networking.ApiJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SubscriptionModelsTest {
    private val json = ApiJson.instance

    @Test
    fun `tarif plan decodes snake_case and a features array`() {
        val body = """
            {"id":"p1","name":"Pro","display_price":29.99,"display_currency":"USD",
             "billing_period":"month","features":["a","b"]}
        """.trimIndent()
        val plan = json.decodeFromString(TarifPlan.serializer(), body)
        assertEquals(29.99, plan.displayPrice)
        assertEquals("month", plan.billingPeriod)
        assertEquals(listOf("a", "b"), plan.features)
        assertEquals("USD 29.99 / month", plan.formattedPrice)
    }

    @Test
    fun `tarif plan accepts features as an object and yields sorted keys`() {
        val body = """{"id":"p1","name":"Pro","features":{"zeta":true,"alpha":1}}"""
        val plan = json.decodeFromString(TarifPlan.serializer(), body)
        assertEquals(listOf("alpha", "zeta"), plan.features)
    }

    @Test
    fun `plan maps to a subscription cart item`() {
        val plan = TarifPlan(id = "p1", name = "Pro", slug = "pro", displayPrice = 29.99, billingPeriod = "month")
        val item = plan.toCartItem()
        assertEquals("subscription", item.type)
        assertEquals("p1", item.id)
        assertEquals(29.99, item.price)
        assertEquals("month", item.metadata["billing_period"])
        assertEquals("pro", item.metadata["slug"])
    }

    @Test
    fun `subscription status helpers`() {
        val active = Subscription(id = "s1", status = "active")
        assertEquals("ACTIVE", active.statusLabel)
        assertEquals(true, active.isActive)
        assertEquals(false, Subscription(id = "s2", status = "cancelled").isActive)
    }
}
