package com.vbwd.core.plugins

import com.vbwd.core.cart.Cart
import com.vbwd.core.checkout.CheckoutSourceRegistry
import com.vbwd.core.events.AppEvents
import com.vbwd.core.events.DefaultEventBus
import com.vbwd.core.events.EventBus
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiClientConfig
import com.vbwd.core.notifications.DefaultNotificationsSdk
import com.vbwd.core.notifications.NotificationsSdk
import com.vbwd.core.plugins.registries.ComponentRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plugin composition root. Port of the iOS `PluginHost` / web `factory.ts`:
 * load manifest → register the compiled-in plugins → install the enabled ones
 * in dependency order → collect routes → activate. The only place the
 * compiled-in plugin list, the registry, and the SDK are constructed (DI).
 * A structural or per-plugin failure never blocks the shell (error isolation).
 */
class PluginHost(
    api: ApiClient,
    apiConfig: ApiClientConfig,
    private val manifestLoader: PluginManifestLoader,
    private val plugins: List<Plugin>,
    events: EventBus? = null,
    cart: Cart = Cart(),
    checkoutSources: CheckoutSourceRegistry = CheckoutSourceRegistry(),
    notifications: NotificationsSdk? = null,
) {
    val sdk: DefaultPlatformSdk = DefaultPlatformSdk(
        api = api,
        apiConfig = apiConfig,
        events = events ?: DefaultEventBus(api),
        cart = cart,
        checkoutSources = checkoutSources,
        notifications = notifications ?: DefaultNotificationsSdk(),
    )

    private val registry = PluginRegistry()

    var routes: List<PluginRoute> = emptyList()
        private set
    var manifest: PluginManifest = PluginManifest.EMPTY
        private set

    /** Current navigation path driven by menu taps; null ⇒ the home/dashboard. */
    private val _selectedRoute = MutableStateFlow<String?>(null)
    val selectedRoute: StateFlow<String?> = _selectedRoute.asStateFlow()

    /** The dashboard/profile screens read this to surface enabled plugins' widgets. */
    val components: ComponentRegistry get() = sdk.components

    fun select(path: String?) {
        _selectedRoute.value = path
    }

    suspend fun bootstrap() {
        manifest = manifestLoader.load()
        plugins.forEach { runCatching { registry.register(it) } }

        val enabled = manifest.enabledNames
        // Structural dep errors are isolated so the shell still loads.
        try {
            registry.installAll(sdk, enabled)
        } catch (error: PluginError) {
            sdk.events.emit(AppEvents.PLUGIN_ERROR, error.toString())
        }

        routes = sdk.getRoutes()

        for (plugin in plugins) {
            val name = plugin.metadata.name
            if (name in enabled && registry.status(name) == PluginStatus.Installed) {
                runCatching { registry.activate(name) }
            }
        }
    }

    fun status(name: String): PluginStatus? = registry.status(name)
}
