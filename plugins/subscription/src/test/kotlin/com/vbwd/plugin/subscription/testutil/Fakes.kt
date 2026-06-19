package com.vbwd.plugin.subscription.testutil

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiError
import com.vbwd.core.networking.ApiEvent
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.HttpMethod
import com.vbwd.plugin.subscription.domain.AddOn
import com.vbwd.plugin.subscription.domain.AddonSubscription
import com.vbwd.plugin.subscription.domain.Subscription
import com.vbwd.plugin.subscription.domain.SubscriptionService
import com.vbwd.plugin.subscription.domain.TarifPlan
import kotlinx.serialization.DeserializationStrategy

/** ApiClient fake returning a configurable typed body. */
class FakeApi(
    var nextResult: Any? = null,
    var nextError: ApiError? = null,
) : ApiClient {
    var lastPath: String? = null
    var lastBody: String? = null

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> request(
        method: HttpMethod,
        path: String,
        jsonBody: String?,
        deserializer: DeserializationStrategy<T>,
    ): T {
        lastPath = path
        lastBody = jsonBody
        nextError?.let { throw it }
        return (nextResult ?: EmptyResponse()) as T
    }

    override fun setToken(token: String?) = Unit
    override fun on(event: ApiEvent, handler: () -> Unit) = Unit
}

/** Configurable [SubscriptionService] double. */
class FakeSubscriptionService(
    var plan: TarifPlan? = null,
    var activeSub: Subscription? = null,
    var allSubs: List<Subscription> = emptyList(),
    var addOns: List<AddOn> = emptyList(),
    var userAddOns: List<AddonSubscription> = emptyList(),
) : SubscriptionService {
    override suspend fun fetchAllSubscriptions(): List<Subscription> = allSubs
    override suspend fun fetchActiveSub(): Subscription? = activeSub
    override suspend fun fetchPlans(currency: String?): List<TarifPlan> = plan?.let { listOf(it) } ?: emptyList()
    override suspend fun fetchPlan(slug: String): TarifPlan = plan ?: error("no plan")
    override suspend fun cancelSubscription(id: String) = Unit
    override suspend fun fetchAddOns(): List<AddOn> = addOns
    override suspend fun fetchUserAddOns(): List<AddonSubscription> = userAddOns
    override suspend fun fetchUserAddOn(id: String): AddonSubscription = userAddOns.first()
    override suspend fun cancelAddOn(id: String) = Unit
}
