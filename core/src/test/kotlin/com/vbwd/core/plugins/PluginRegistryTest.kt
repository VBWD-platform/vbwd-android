package com.vbwd.core.plugins

import com.vbwd.core.events.DefaultEventBus
import com.vbwd.core.networking.ApiClientConfig
import com.vbwd.core.testutil.FakeApiClient
import com.vbwd.core.testutil.FakePlugin
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PluginRegistryTest {
    private fun sdk(): PlatformSdk {
        val api = FakeApiClient()
        return DefaultPlatformSdk(api, ApiClientConfig("http://x"), DefaultEventBus(api))
    }

    @Test
    fun `register rejects duplicates`() {
        val registry = PluginRegistry()
        registry.register(FakePlugin("a"))
        assertThrows(PluginError.Duplicate::class.java) { registry.register(FakePlugin("a")) }
    }

    @Test
    fun `installAll installs dependencies before dependents`() = runTest {
        val order = mutableListOf<String>()
        val registry = PluginRegistry()
        // Register dependent first to prove the topo sort reorders.
        registry.register(
            FakePlugin("b", dependencies = PluginDependencies.List(listOf("a")), installRecorder = order),
        )
        registry.register(FakePlugin("a", installRecorder = order))

        registry.installAll(sdk())

        assertEquals(listOf("a", "b"), order)
        assertEquals(PluginStatus.Installed, registry.status("a"))
        assertEquals(PluginStatus.Installed, registry.status("b"))
    }

    @Test
    fun `a failing plugin is isolated as Error while peers still install`() = runTest {
        val registry = PluginRegistry()
        registry.register(FakePlugin("bad", failInstall = true))
        registry.register(FakePlugin("good"))

        registry.installAll(sdk())

        assertInstanceOf(PluginStatus.Error::class.java, registry.status("bad"))
        assertEquals(PluginStatus.Installed, registry.status("good"))
    }

    @Test
    fun `a missing dependency throws up front`() = runTest {
        val registry = PluginRegistry()
        registry.register(FakePlugin("b", dependencies = PluginDependencies.List(listOf("a"))))
        assertThrows(PluginError.MissingDependency::class.java) { registry.installAll(sdk()) }
    }

    @Test
    fun `enabled gate excluding a dependency surfaces as missing`() = runTest {
        val registry = PluginRegistry()
        registry.register(FakePlugin("a"))
        registry.register(FakePlugin("b", dependencies = PluginDependencies.List(listOf("a"))))
        // Only "b" enabled — its dependency "a" is gated out.
        assertThrows(PluginError.MissingDependency::class.java) {
            registry.installAll(sdk(), enabled = setOf("b"))
        }
    }

    @Test
    fun `status machine transitions register install activate deactivate uninstall`() = runTest {
        val registry = PluginRegistry()
        registry.register(FakePlugin("a"))
        assertEquals(PluginStatus.Registered, registry.status("a"))
        registry.installAll(sdk())
        assertEquals(PluginStatus.Installed, registry.status("a"))
        registry.activate("a")
        assertEquals(PluginStatus.Active, registry.status("a"))
        registry.deactivate("a")
        assertEquals(PluginStatus.Inactive, registry.status("a"))
        registry.uninstall("a")
        assertEquals(PluginStatus.Registered, registry.status("a"))
    }

    @Test
    fun `deactivate is blocked by an active dependent`() = runTest {
        val registry = PluginRegistry()
        registry.register(FakePlugin("a"))
        registry.register(FakePlugin("b", dependencies = PluginDependencies.List(listOf("a"))))
        registry.installAll(sdk())
        registry.activate("a")
        registry.activate("b")

        assertThrows(PluginError.InvalidState::class.java) { registry.deactivate("a") }
    }
}
