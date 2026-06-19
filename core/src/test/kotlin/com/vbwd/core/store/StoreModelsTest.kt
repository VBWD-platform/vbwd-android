package com.vbwd.core.store

import com.vbwd.core.cart.toCartItem
import com.vbwd.core.networking.ApiJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StoreModelsTest {
    private val json = ApiJson.instance

    @Test
    fun `token bundle decodes snake_case fields`() {
        val body = """
            {"id":"b1","name":"Starter","token_amount":600,"price":"29.99",
             "currency":"USD","is_active":true,"sort_order":1}
        """.trimIndent()
        val bundle = json.decodeFromString(TokenBundle.serializer(), body)
        assertEquals(600, bundle.tokenAmount)
        assertEquals("29.99", bundle.price)
        assertEquals(1, bundle.sortOrder)
        assertEquals(true, bundle.isActive)
    }

    @Test
    fun `token bundle maps to a generic cart item`() {
        val bundle = TokenBundle(
            id = "b1",
            name = "Starter",
            tokenAmount = 600,
            price = "29.99",
            currency = "EUR",
            slug = "starter",
        )
        val item = bundle.toCartItem()
        assertEquals("token_bundle", item.type)
        assertEquals("b1", item.id)
        assertEquals(29.99, item.price)
        assertEquals("EUR", item.currency)
        assertEquals("600", item.metadata["token_amount"])
        assertEquals("starter", item.metadata["slug"])
    }

    @Test
    fun `checkout result decodes nested invoice and surfaces accessors`() {
        val body = """
            {"invoice":{"id":"inv1","invoice_number":"2026-001","total_amount":"29.99","status":"pending"}}
        """.trimIndent()
        val result = json.decodeFromString(CheckoutResult.serializer(), body)
        assertEquals("inv1", result.invoiceId)
        assertEquals("pending", result.status)
        assertEquals("paid", result.withStatus("paid").status)
    }
}
