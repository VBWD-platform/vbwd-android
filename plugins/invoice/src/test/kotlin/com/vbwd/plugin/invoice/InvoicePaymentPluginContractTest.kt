package com.vbwd.plugin.invoice

import com.vbwd.core.events.DefaultEventBus
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiClientConfig
import com.vbwd.core.networking.ApiEvent
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.HttpMethod
import com.vbwd.core.plugins.DefaultPlatformSdk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.DeserializationStrategy
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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

class InvoicePaymentPluginContractTest {
    private fun sdk() = DefaultPlatformSdk(FakeApi(), ApiClientConfig("http://x"), DefaultEventBus(FakeApi()))

    @Test
    fun `install registers the invoice section with no payment action (goes straight to confirmation)`() = runTest {
        val platform = sdk()
        InvoicePaymentPlugin().install(platform)
        assertTrue(platform.getComponents().containsKey("PaymentMethodInvoice"))
        assertTrue(platform.components.supportedPaymentMethodCodes().contains("invoice"))
        assertNull(platform.components.paymentAction("invoice"))
    }
}
