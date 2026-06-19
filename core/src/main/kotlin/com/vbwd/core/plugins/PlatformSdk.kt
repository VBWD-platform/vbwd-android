package com.vbwd.core.plugins

import androidx.compose.runtime.Composable
import com.vbwd.core.cart.Cart
import com.vbwd.core.checkout.CheckoutSource
import com.vbwd.core.checkout.CheckoutSourceRegistry
import com.vbwd.core.events.EventBus
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiClientConfig
import com.vbwd.core.notifications.DefaultNotificationsSdk
import com.vbwd.core.notifications.NotificationsSdk
import com.vbwd.core.plugins.registries.ComponentRegistry
import com.vbwd.core.plugins.registries.LocalizationRegistry
import com.vbwd.core.plugins.registries.MenuItem
import com.vbwd.core.plugins.registries.MenuItemRegistry
import com.vbwd.core.plugins.registries.RouteRegistry
import com.vbwd.core.plugins.registries.StoreRegistry

/** A lazy Compose view a plugin registers (port of the iOS `ComponentFactory`). */
typealias ComponentFactory = @Composable () -> Unit

/** What a payment-action handler tells checkout to do after `POST /user/checkout`. */
sealed interface PaymentAction {
    /** Go straight to the confirmation page (e.g. invoice payment). */
    data object ShowConfirmation : PaymentAction

    /** Open an external URL for payment (e.g. Stripe Checkout session URL). */
    data class OpenUrl(val url: String, val sessionId: String? = null) : PaymentAction
}

/** Post-checkout handler a payment plugin registers: invoice id → next action. */
typealias PaymentActionHandler = suspend (invoiceId: String) -> PaymentAction

/**
 * A screen contributed by a plugin. Port of the iOS `PluginRoute` /
 * web `IRouteConfig` (path/name/lazy content + auth & permission meta).
 * When [matchPrefix] is true, any path starting with [path] matches.
 */
class PluginRoute(
    val path: String,
    val name: String,
    val requiresAuth: Boolean = false,
    val requiredUserPermission: String? = null,
    val matchPrefix: Boolean = false,
    val content: @Composable () -> Unit,
)

/**
 * Facade handed to a plugin's [Plugin.install]. Port of the iOS `PlatformSDK`.
 * A plugin depends on **this interface only** — never on a registry, the
 * registry manager, or the composition root (ISP/DIP). This is the single seam
 * through which the app is extended (OCP).
 */
interface PlatformSdk {
    val api: ApiClient
    val apiConfig: ApiClientConfig
    val events: EventBus
    val notifications: NotificationsSdk
    val cart: Cart
    val checkoutSources: CheckoutSourceRegistry

    fun addRoute(route: PluginRoute)
    fun getRoutes(): List<PluginRoute>

    fun addComponent(name: String, factory: ComponentFactory)
    fun removeComponent(name: String)
    fun getComponents(): Map<String, ComponentFactory>

    fun createStore(id: String, store: Any)
    fun getStores(): Map<String, Any>

    fun addTranslations(locale: String, messages: Map<String, String>)
    fun getTranslations(): Map<String, Map<String, String>>

    fun addMenuItem(item: MenuItem)
    fun removeMenuItem(id: String)
    fun getMenuItems(): List<MenuItem>

    fun addPaymentAction(code: String, handler: PaymentActionHandler)

    fun addCheckoutSource(source: CheckoutSource)
    fun removeCheckoutSource(id: String)
}

/**
 * Thin forwarder over the registries + injected `api`/`events`. Port of the iOS
 * `DefaultPlatformSDK` — no behaviour of its own (DRY/SRP). The registries are
 * exposed (not on the [PlatformSdk] interface) so the composition root / app
 * shell can read them; plugins still see only the interface.
 */
class DefaultPlatformSdk(
    override val api: ApiClient,
    override val apiConfig: ApiClientConfig,
    override val events: EventBus,
    override val cart: Cart = Cart(),
    override val checkoutSources: CheckoutSourceRegistry = CheckoutSourceRegistry(),
    override val notifications: NotificationsSdk = DefaultNotificationsSdk(),
    val routes: RouteRegistry = RouteRegistry(),
    val components: ComponentRegistry = ComponentRegistry(),
    val stores: StoreRegistry = StoreRegistry(),
    val localizations: LocalizationRegistry = LocalizationRegistry(),
    val menuItems: MenuItemRegistry = MenuItemRegistry(),
) : PlatformSdk {

    override fun addRoute(route: PluginRoute) = routes.add(route)
    override fun getRoutes(): List<PluginRoute> = routes.all()

    override fun addComponent(name: String, factory: ComponentFactory) = components.add(name, factory)
    override fun removeComponent(name: String) = components.remove(name)
    override fun getComponents(): Map<String, ComponentFactory> = components.all()

    override fun createStore(id: String, store: Any) = stores.create(id, store)
    override fun getStores(): Map<String, Any> = stores.all()

    override fun addTranslations(locale: String, messages: Map<String, String>) =
        localizations.add(locale, messages)
    override fun getTranslations(): Map<String, Map<String, String>> = localizations.all()

    override fun addMenuItem(item: MenuItem) = menuItems.add(item)
    override fun removeMenuItem(id: String) = menuItems.remove(id)
    override fun getMenuItems(): List<MenuItem> = menuItems.all()

    override fun addPaymentAction(code: String, handler: PaymentActionHandler) =
        components.addPaymentAction(code, handler)

    override fun addCheckoutSource(source: CheckoutSource) = checkoutSources.register(source)
    override fun removeCheckoutSource(id: String) = checkoutSources.unregister(id)
}
