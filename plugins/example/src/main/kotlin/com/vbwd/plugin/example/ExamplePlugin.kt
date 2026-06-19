package com.vbwd.plugin.example

import androidx.compose.material3.Text
import com.vbwd.core.events.AppEvents
import com.vbwd.core.events.Unsubscribe
import com.vbwd.core.plugins.PlatformSdk
import com.vbwd.core.plugins.Plugin
import com.vbwd.core.plugins.PluginMetadata
import com.vbwd.core.plugins.PluginRoute
import com.vbwd.core.plugins.SemanticVersion
import com.vbwd.plugin.example.domain.DefaultExampleService
import com.vbwd.plugin.example.domain.ExampleStore
import com.vbwd.plugin.example.ui.ExampleDashboardWidget
import com.vbwd.plugin.example.ui.ExampleProfileSection
import com.vbwd.plugin.example.ui.ExampleScreen

/**
 * Reference plugin demonstrating SOLID architecture on top of `:core`. This is
 * the **thin composition root** — it wires the domain (store, service), views,
 * and menu items that live in their own files. It depends ONLY on the public
 * `:core` module, proving a third-party can extend the app without touching SDK
 * internals (OCP). Port of the iOS `ExamplePlugin`.
 *
 * - **S** — each file has one responsibility
 * - **O** — extends the app via [PlatformSdk] seams, no core modification
 * - **L** — passes the same [Plugin] contract as any other plugin
 * - **I** — `ExampleService` exposes only what this plugin needs
 * - **D** — depends on `ApiClient`, not a concrete HTTP client
 */
class ExamplePlugin : Plugin {
    val store = ExampleStore()
    private var unsubscribe: Unsubscribe? = null

    override val metadata = PluginMetadata(
        name = "example",
        version = SemanticVersion(1, 0, 0),
        description = "Reference plugin exercising every PlatformSdk seam.",
        author = "VBWD",
        keywords = listOf("example", "reference"),
        translations = mapOf("en" to mapOf("example.title" to "Example")),
    )

    override suspend fun install(sdk: PlatformSdk) {
        // Routes
        sdk.addRoute(PluginRoute(path = "/example", name = "example") { ExampleScreen(store) })
        sdk.addRoute(
            PluginRoute(
                path = "/example/secret",
                name = "example-secret",
                requiresAuth = true,
                requiredUserPermission = "user.profile.view",
            ) { Text("secret") },
        )

        // Dashboard widget + profile section (Dashboard*/Profile* conventions)
        sdk.addComponent("DashboardExample") { ExampleDashboardWidget(store) }
        sdk.addComponent("ProfileExample") { ExampleProfileSection() }

        // Store + translations
        sdk.createStore("exampleStore", store)
        sdk.addTranslations("en", mapOf("example.title" to "Example"))
        sdk.addTranslations("de", mapOf("example.title" to "Beispiel"))

        // Event subscription (decoupled core ↔ plugin via the bus)
        unsubscribe = sdk.events.on(AppEvents.AUTH_LOGIN) { store.markLoginSeen() }

        // Backend interaction via the injected client (DIP)
        DefaultExampleService(sdk.api).fetchPlans()

        // Menu items (delegated to the factory — SRP)
        ExampleMenuItems.all().forEach { sdk.addMenuItem(it) }
    }

    override suspend fun activate() {
        store.setActive(true)
    }

    override suspend fun deactivate() {
        store.setActive(false)
    }

    override suspend fun uninstall() {
        unsubscribe?.invoke()
        unsubscribe = null
        store.setActive(false)
    }
}
