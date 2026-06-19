package com.vbwd.plugin.subscription

import androidx.compose.runtime.remember
import com.vbwd.core.events.AppEvents
import com.vbwd.core.events.Unsubscribe
import com.vbwd.core.plugins.DefaultPlatformSdk
import com.vbwd.core.plugins.PlatformSdk
import com.vbwd.core.plugins.Plugin
import com.vbwd.core.plugins.PluginMetadata
import com.vbwd.core.plugins.PluginRoute
import com.vbwd.core.plugins.SemanticVersion
import com.vbwd.core.ui.checkout.CheckoutViewModel
import com.vbwd.plugin.subscription.domain.DefaultSubscriptionService
import com.vbwd.plugin.subscription.domain.SubscriptionCheckoutSource
import com.vbwd.plugin.subscription.domain.SubscriptionService
import com.vbwd.plugin.subscription.ui.AddOnsScreen
import com.vbwd.plugin.subscription.ui.AddOnsViewModel
import com.vbwd.plugin.subscription.ui.AllSubscriptionsScreen
import com.vbwd.plugin.subscription.ui.AllSubscriptionsViewModel
import com.vbwd.plugin.subscription.ui.DashboardSubscriptionWidget
import com.vbwd.plugin.subscription.ui.SubscriptionOverviewScreen
import com.vbwd.plugin.subscription.ui.SubscriptionOverviewViewModel
import com.vbwd.plugin.subscription.ui.TarifPlansScreen
import com.vbwd.plugin.subscription.ui.TarifPlansViewModel

/**
 * Subscription management plugin — tarif plans, subscription overview, add-ons,
 * a dashboard widget, and a `SubscriptionCheckoutSource` into the A04 registry.
 * Port of the iOS `SubscriptionPlugin`. Depends on `:core` only; builds its view
 * models with `sdk.api` inside the route closures (no Hilt in the plugin).
 */
class SubscriptionPlugin : Plugin {
    private var unsubscribe: Unsubscribe? = null

    override val metadata = PluginMetadata(
        name = "subscription",
        version = SemanticVersion(1, 0, 0),
        description = "Subscription management — plans, subscriptions, add-ons, checkout.",
        author = "VBWD",
        keywords = listOf("subscription", "plans", "addons", "checkout"),
        translations = mapOf("en" to TRANSLATIONS),
    )

    // Composition root: registers 4 routes + a widget + the checkout source +
    // menu items + translations. Long by nature (mirrors the iOS install); the
    // alternative — splitting it — would scatter the plugin's single wiring point.
    @Suppress("LongMethod")
    override suspend fun install(sdk: PlatformSdk) {
        val service: SubscriptionService = DefaultSubscriptionService(sdk.api)

        sdk.addRoute(
            PluginRoute(
                path = "/subscription",
                name = "subscription-overview",
                requiresAuth = true,
                requiredUserPermission = PERMISSION,
            ) {
                val viewModel = remember { SubscriptionOverviewViewModel(service) }
                SubscriptionOverviewScreen(viewModel)
            },
        )

        sdk.addRoute(
            PluginRoute(
                path = "/subscription/plans",
                name = "subscription-plans",
                requiresAuth = true,
                requiredUserPermission = PERMISSION,
            ) {
                val viewModel = remember { TarifPlansViewModel(service, sdk.cart) }
                TarifPlansScreen(
                    viewModel = viewModel,
                    checkoutFactory = { context ->
                        CheckoutViewModel(
                            api = sdk.api,
                            context = context,
                            cart = sdk.cart,
                            checkoutSources = sdk.checkoutSources,
                            components = (sdk as? DefaultPlatformSdk)?.components,
                            events = sdk.events,
                        )
                    },
                )
            },
        )

        sdk.addRoute(
            PluginRoute(
                path = "/subscription/all",
                name = "subscription-all",
                requiresAuth = true,
                requiredUserPermission = PERMISSION,
            ) {
                val viewModel = remember { AllSubscriptionsViewModel(service) }
                AllSubscriptionsScreen(viewModel)
            },
        )

        sdk.addRoute(
            PluginRoute(
                path = "/subscription/addons",
                name = "subscription-addons",
                requiresAuth = true,
                requiredUserPermission = PERMISSION,
            ) {
                val viewModel = remember { AddOnsViewModel(service, sdk.cart) }
                AddOnsScreen(viewModel)
            },
        )

        sdk.addComponent("DashboardSubscription") { DashboardSubscriptionWidget(service) }

        // Priority 10 — wins over the built-in token-bundle source (0).
        sdk.addCheckoutSource(SubscriptionCheckoutSource(sdk.api, sdk.cart, service))

        SubscriptionMenuItems.all().forEach { sdk.addMenuItem(it) }
        sdk.addTranslations("en", TRANSLATIONS)

        unsubscribe = sdk.events.on(AppEvents.AUTH_LOGIN) {
            // Reserved: plugins may refresh subscription data on login.
        }
    }

    override suspend fun uninstall() {
        unsubscribe?.invoke()
        unsubscribe = null
    }

    private companion object {
        const val PERMISSION = "subscription.plans.view"

        val TRANSLATIONS = mapOf(
            "subscription.title" to "Subscription",
            "subscription.plans.title" to "Tarif Plans",
            "subscription.addons.title" to "Add-Ons",
            "subscription.overview.title" to "Subscription Overview",
            "subscription.no_active" to "No active subscription",
            "subscription.subscribe" to "Subscribe",
            "nav.subscription" to "Subscription",
            "nav.plans" to "Tarif Plans",
            "nav.addons" to "Add-Ons",
        )
    }
}
