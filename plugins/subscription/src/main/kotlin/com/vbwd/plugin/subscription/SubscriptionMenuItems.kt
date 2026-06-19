package com.vbwd.plugin.subscription

import com.vbwd.core.plugins.registries.MenuItem

/**
 * Menu-item factory for the subscription plugin (SRP). Port of the iOS
 * `SubscriptionMenuItems`: a top-level "Subscription" plus "Tarif Plans" and
 * "Add-Ons" under the Store section.
 */
object SubscriptionMenuItems {
    private const val SUBSCRIPTION_ORDER = 30
    private const val PLANS_ORDER = 200
    private const val ADDONS_ORDER = 201

    fun all(): List<MenuItem> = listOf(
        MenuItem(
            id = "subscription",
            icon = "creditcard",
            title = "Subscription",
            routePath = "/subscription",
            order = SUBSCRIPTION_ORDER,
            section = "top",
        ),
        MenuItem(
            id = "subscription-plans",
            icon = "list",
            title = "Tarif Plans",
            routePath = "/subscription/plans",
            requiredPermission = "subscription.plans.view",
            order = PLANS_ORDER,
            section = "store",
        ),
        MenuItem(
            id = "subscription-addons",
            icon = "extension",
            title = "Add-Ons",
            routePath = "/subscription/addons",
            requiredPermission = "subscription.plans.view",
            order = ADDONS_ORDER,
            section = "store",
        ),
    )
}
