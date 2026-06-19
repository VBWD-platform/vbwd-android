package com.vbwd.plugin.subscription.domain

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiError
import com.vbwd.core.networking.get
import com.vbwd.core.networking.post
import kotlinx.serialization.Serializable

/** Subscription API operations (DIP — testable). Port of the iOS protocol. */
interface SubscriptionService {
    suspend fun fetchAllSubscriptions(): List<Subscription>
    suspend fun fetchActiveSub(): Subscription?
    suspend fun fetchPlans(currency: String?): List<TarifPlan>
    suspend fun fetchPlan(slug: String): TarifPlan
    suspend fun cancelSubscription(id: String)
    suspend fun fetchAddOns(): List<AddOn>
    suspend fun fetchUserAddOns(): List<AddonSubscription>
    suspend fun fetchUserAddOn(id: String): AddonSubscription
    suspend fun cancelAddOn(id: String)
}

/** Default impl backed by the SDK's [ApiClient]. Port of `DefaultSubscriptionService`. */
class DefaultSubscriptionService(
    private val api: ApiClient,
    private val rootCategorySlug: String? = null,
) : SubscriptionService {

    override suspend fun fetchAllSubscriptions(): List<Subscription> =
        api.get<SubscriptionsListResponse>(SubscriptionEndpoints.ALL_SUBSCRIPTIONS).subscriptions ?: emptyList()

    override suspend fun fetchActiveSub(): Subscription? =
        api.get<SubscriptionResponse>(SubscriptionEndpoints.ACTIVE_SUB).subscription

    override suspend fun fetchPlans(currency: String?): List<TarifPlan> {
        val params = buildList {
            currency?.takeIf { it.isNotEmpty() }?.let { add("currency=$it") }
            rootCategorySlug?.takeIf { it.isNotEmpty() }?.let { add("category=$it") }
        }
        val path = if (params.isEmpty()) {
            SubscriptionEndpoints.PLANS
        } else {
            "${SubscriptionEndpoints.PLANS}?${params.joinToString("&")}"
        }
        return api.get<PlansResponse>(path).plans ?: emptyList()
    }

    // Backend returns the plan at the top level (not wrapped in a "plan" key).
    override suspend fun fetchPlan(slug: String): TarifPlan =
        api.get(SubscriptionEndpoints.plan(slug))

    override suspend fun cancelSubscription(id: String) {
        api.post<EmptyBody, CancelResponse>(SubscriptionEndpoints.cancelSub(id), EmptyBody())
    }

    override suspend fun fetchAddOns(): List<AddOn> =
        api.get<AddOnsResponse>(SubscriptionEndpoints.ADDONS).addons ?: emptyList()

    override suspend fun fetchUserAddOns(): List<AddonSubscription> =
        api.get<UserAddOnsResponse>(SubscriptionEndpoints.USER_ADDONS).addonSubscriptions ?: emptyList()

    override suspend fun fetchUserAddOn(id: String): AddonSubscription =
        api.get<UserAddOnResponse>(SubscriptionEndpoints.userAddon(id)).addonSubscription
            ?: throw ApiError.Http(status = HTTP_NOT_FOUND, message = "Add-on subscription not found")

    override suspend fun cancelAddOn(id: String) {
        api.post<EmptyBody, CancelResponse>(SubscriptionEndpoints.cancelAddon(id), EmptyBody())
    }

    @Serializable
    private class EmptyBody

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
