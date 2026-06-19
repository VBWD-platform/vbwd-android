package com.vbwd.plugin.subscription.domain

/** API endpoint paths for the subscription plugin. Port of the iOS enum. */
internal object SubscriptionEndpoints {
    const val PLANS = "/tarif-plans"
    fun plan(slug: String): String = "/tarif-plans/$slug"
    const val ALL_SUBSCRIPTIONS = "/user/subscriptions"
    const val ACTIVE_SUB = "/user/subscriptions/active"
    fun cancelSub(id: String): String = "/user/subscriptions/$id/cancel"
    const val ADDONS = "/addons"
    const val USER_ADDONS = "/user/addons"
    fun userAddon(id: String): String = "/user/addons/$id"
    fun cancelAddon(id: String): String = "/user/addons/$id/cancel"
    const val CHECKOUT = "/user/checkout"
}
