package com.vbwd.plugin.meinchatplus

import com.vbwd.core.events.DefaultEventBus
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiClientConfig
import com.vbwd.core.networking.ApiEvent
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.HttpMethod
import com.vbwd.core.plugins.DefaultPlatformSdk
import com.vbwd.core.plugins.PluginDependencies
import com.vbwd.plugin.meinchat.domain.MeinChatSecureMessaging
import com.vbwd.plugin.meinchat.domain.MeinChatSecureMessagingStoreId
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.DeserializationStrategy
import org.junit.jupiter.api.Assertions.assertEquals
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

class MeinChatPlusPluginContractTest {
    private fun sdk() = DefaultPlatformSdk(FakeApi(), ApiClientConfig("http://x"), DefaultEventBus(FakeApi()))

    @Test
    fun `declares the meinchat plugin dependency (the only sanctioned plugin-to-plugin edge)`() {
        assertEquals(
            PluginDependencies.List(listOf("meinchat")),
            MeinChatPlusPlugin().metadata.dependencies,
        )
    }

    @Test
    fun `install registers a fail-closed secure-messaging impl under the meinchat seam`() = runTest {
        val platform = sdk()
        MeinChatPlusPlugin().install(platform)
        val registered = platform.getStores()[MeinChatSecureMessagingStoreId]
        assertTrue(registered is MeinChatSecureMessaging)
    }
}
