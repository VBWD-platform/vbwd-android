package com.vbwd.plugin.example

import com.vbwd.core.events.AppEvents
import com.vbwd.core.events.DefaultEventBus
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiClientConfig
import com.vbwd.core.networking.ApiEvent
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.HttpMethod
import com.vbwd.core.plugins.DefaultPlatformSdk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.DeserializationStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Liskov/contract test for the reference plugin: after `install(sdk)` every
 * seam it advertises is present; `uninstall` releases the event subscription;
 * its widget/section names satisfy the `Dashboard*`/`Profile*` conventions.
 */
class ExamplePluginContractTest {
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

    private fun sdk(): DefaultPlatformSdk {
        val api = FakeApi()
        return DefaultPlatformSdk(api, ApiClientConfig("http://x"), DefaultEventBus(api))
    }

    @Test
    fun `install registers routes, components, store, translations and menu`() = runTest {
        val platform = sdk()
        ExamplePlugin().install(platform)

        assertEquals(setOf("/example", "/example/secret"), platform.getRoutes().map { it.path }.toSet())
        assertTrue(platform.getComponents().keys.containsAll(setOf("DashboardExample", "ProfileExample")))
        assertTrue(platform.getStores().containsKey("exampleStore"))
        assertEquals("Example", platform.getTranslations()["en"]?.get("example.title"))
        assertEquals("Beispiel", platform.getTranslations()["de"]?.get("example.title"))
        assertEquals(listOf("example"), platform.getMenuItems().map { it.id })
    }

    @Test
    fun `the auth-login subscription updates the store and is released on uninstall`() = runTest {
        val platform = sdk()
        val plugin = ExamplePlugin()
        plugin.install(platform)
        assertEquals(1, platform.events.listenerCount(AppEvents.AUTH_LOGIN))

        platform.events.emit(AppEvents.AUTH_LOGIN)
        assertTrue(plugin.store.sawLogin.value)

        plugin.uninstall()
        assertEquals(0, platform.events.listenerCount(AppEvents.AUTH_LOGIN))
    }

    @Test
    fun `widget and section names satisfy the discovery conventions`() = runTest {
        val platform = sdk()
        ExamplePlugin().install(platform)
        assertTrue(platform.components.dashboardComponents().map { it.first }.contains("DashboardExample"))
        assertTrue(platform.components.profileComponents().map { it.first }.contains("ProfileExample"))
    }

    @Test
    fun `activate and deactivate toggle the store active flag`() = runTest {
        val plugin = ExamplePlugin()
        plugin.activate()
        assertTrue(plugin.store.active.value)
        plugin.deactivate()
        assertTrue(!plugin.store.active.value)
    }
}
