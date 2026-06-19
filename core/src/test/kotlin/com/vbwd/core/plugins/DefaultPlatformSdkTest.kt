package com.vbwd.core.plugins

import com.vbwd.core.events.DefaultEventBus
import com.vbwd.core.networking.ApiClientConfig
import com.vbwd.core.plugins.registries.MenuItem
import com.vbwd.core.plugins.registries.RegistryError
import com.vbwd.core.testutil.FakeApiClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DefaultPlatformSdkTest {
    private fun sdk(): DefaultPlatformSdk {
        val api = FakeApiClient()
        return DefaultPlatformSdk(
            api = api,
            apiConfig = ApiClientConfig(baseUrl = "http://x"),
            events = DefaultEventBus(api),
        )
    }

    @Test
    fun `route contributions forward and enforce uniqueness`() {
        val platform = sdk()
        platform.addRoute(PluginRoute(path = "/a", name = "a") {})
        assertEquals(1, platform.getRoutes().size)
        assertThrows(RegistryError.DuplicateRoutePath::class.java) {
            platform.addRoute(PluginRoute(path = "/a", name = "b") {})
        }
    }

    @Test
    fun `component, store, translation and menu contributions forward`() {
        val platform = sdk()
        platform.addComponent("DashboardX") {}
        platform.createStore("s", Any())
        platform.addTranslations("en", mapOf("k" to "v"))
        platform.addMenuItem(MenuItem(id = "m", icon = "i", title = "M"))

        assertEquals(setOf("DashboardX"), platform.getComponents().keys)
        assertEquals(setOf("s"), platform.getStores().keys)
        assertEquals("v", platform.getTranslations()["en"]?.get("k"))
        assertEquals(listOf("m"), platform.getMenuItems().map { it.id })

        platform.removeComponent("DashboardX")
        assertEquals(emptySet<String>(), platform.getComponents().keys)
        platform.removeMenuItem("m")
        assertEquals(emptyList<MenuItem>(), platform.getMenuItems())
    }

    @Test
    fun `createStore forwards the duplicate-id rejection`() {
        val platform = sdk()
        platform.createStore("s", Any())
        assertThrows(RegistryError.DuplicateStoreId::class.java) { platform.createStore("s", Any()) }
    }
}
