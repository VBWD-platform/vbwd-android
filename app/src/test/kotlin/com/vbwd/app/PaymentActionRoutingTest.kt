package com.vbwd.app

import com.vbwd.core.events.DefaultEventBus
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiClientConfig
import com.vbwd.core.networking.ApiEvent
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.HttpMethod
import com.vbwd.core.plugins.DefaultPlatformSdk
import com.vbwd.plugin.invoice.InvoicePaymentPlugin
import com.vbwd.plugin.stripe.StripePaymentPlugin
import com.vbwd.plugin.tokenpayment.TokenPaymentPlugin
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.DeserializationStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

private class FakeApi : ApiClient {
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> request(
        method: HttpMethod,
        path: String,
        jsonBody: String?,
        deserializer: DeserializationStrategy<T>,
    ): T = EmptyResponse() as T
    override fun setToken(token: String?) = Unit
    override fun on(event: ApiEvent, handler: () -> Unit) = Unit
}

/**
 * Cross-plugin proof of Open/Closed: with all three payment plugins installed,
 * the generic A04 checkout routes by code to the **correct** payment action with
 * zero core branching (token + stripe register handlers; invoice has none →
 * straight to confirmation). Lives in `:app` (the only module that sees all three).
 */
class PaymentActionRoutingTest {
    @Test
    fun `each payment method resolves to its own action by code`() = runTest {
        val platform = DefaultPlatformSdk(FakeApi(), ApiClientConfig("http://x"), DefaultEventBus(FakeApi()))

        TokenPaymentPlugin().install(platform)
        StripePaymentPlugin().install(platform)
        InvoicePaymentPlugin().install(platform)

        assertNotNull(platform.components.paymentAction("token_balance"))
        assertNotNull(platform.components.paymentAction("stripe"))
        assertNull(platform.components.paymentAction("invoice"))

        assertEquals(
            setOf("token_balance", "stripe", "invoice"),
            platform.components.supportedPaymentMethodCodes(),
        )
    }
}
