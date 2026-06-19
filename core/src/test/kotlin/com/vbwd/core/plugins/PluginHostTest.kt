package com.vbwd.core.plugins

import com.vbwd.core.networking.ApiClientConfig
import com.vbwd.core.testutil.FakeApiClient
import com.vbwd.core.testutil.FakePlugin
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PluginHostTest {
    private class RoutePlugin(name: String) : Plugin {
        override val metadata = PluginMetadata(name = name, version = SemanticVersion(1, 0, 0))
        override suspend fun install(sdk: PlatformSdk) {
            sdk.addRoute(PluginRoute(path = "/$name", name = name) {})
        }
    }

    private fun host(plugins: List<Plugin>, enabled: Set<String>): PluginHost {
        val api = FakeApiClient()
        val manifest = PluginManifest(
            enabled.associateWith { PluginManifest.Entry(enabled = true, version = "1.0.0") },
        )
        return PluginHost(
            api = api,
            apiConfig = ApiClientConfig("http://x"),
            manifestLoader = InMemoryPluginManifestLoader(manifest),
            plugins = plugins,
        )
    }

    @Test
    fun `bootstrap installs and activates enabled plugins and collects routes`() = runTest {
        val pluginHost = host(listOf(RoutePlugin("alpha")), enabled = setOf("alpha"))
        pluginHost.bootstrap()
        assertEquals(PluginStatus.Active, pluginHost.status("alpha"))
        assertEquals(listOf("/alpha"), pluginHost.routes.map { it.path })
    }

    @Test
    fun `bootstrap leaves disabled plugins uninstalled`() = runTest {
        val pluginHost = host(listOf(FakePlugin("beta")), enabled = emptySet())
        pluginHost.bootstrap()
        assertEquals(PluginStatus.Registered, pluginHost.status("beta"))
        assertTrue(pluginHost.routes.isEmpty())
    }
}
