package com.vbwd.plugin.stripe

import com.vbwd.core.events.DefaultEventBus
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiClientConfig
import com.vbwd.core.networking.ApiEvent
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.HttpMethod
import com.vbwd.core.plugins.DefaultPlatformSdk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.DeserializationStrategy
import org.junit.jupiter.api.Assertions.assertNotNull
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

class StripePaymentPluginContractTest {
    private fun sdk() = DefaultPlatformSdk(FakeApi(), ApiClientConfig("http://x"), DefaultEventBus(FakeApi()))

    @Test
    fun `install registers the stripe section and payment action`() = runTest {
        val platform = sdk()
        StripePaymentPlugin().install(platform)
        assertTrue(platform.getComponents().containsKey("PaymentMethodStripe"))
        assertNotNull(platform.components.paymentAction("stripe"))
        assertTrue(platform.components.supportedPaymentMethodCodes().contains("stripe"))
    }
}
