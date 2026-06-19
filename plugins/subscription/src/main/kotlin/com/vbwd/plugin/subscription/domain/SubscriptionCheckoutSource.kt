package com.vbwd.plugin.subscription.domain

import com.vbwd.core.cart.Cart
import com.vbwd.core.cart.CartItem
import com.vbwd.core.cart.CheckoutContext
import com.vbwd.core.checkout.CheckoutSource
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.post
import com.vbwd.core.store.CheckoutResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Checkout source for subscription plan purchases. Registered with `priority=10`
 * (above token_bundle's 0) so it wins when the context specifies a plan. Port of
 * the iOS `SubscriptionCheckoutSource`.
 */
class SubscriptionCheckoutSource(
    private val api: ApiClient,
    private val cart: Cart,
    private val service: SubscriptionService,
) : CheckoutSource {
    override val id = "subscription"
    override val priority = 10

    private var loadedItems: List<CartItem> = emptyList()
    private var loadedPlanId: String? = null

    override fun matches(context: CheckoutContext): Boolean {
        context.source?.let { return it == "subscription" }
        if (context.planSlug != null) return true
        return cart.items("subscription").isNotEmpty() || cart.items("add_on").isNotEmpty()
    }

    override suspend fun load(context: CheckoutContext) {
        // Cart-first: selectPlan() adds the plan to the cart before checkout.
        val subItems = cart.items("subscription")
        val addOnItems = cart.items("add_on")
        if (subItems.isNotEmpty()) loadedPlanId = subItems.first().id
        if (subItems.isNotEmpty() || addOnItems.isNotEmpty()) {
            loadedItems = subItems + addOnItems
            return
        }
        // Slug-driven deep link: fetch the plan and add it to the cart.
        val slug = context.planSlug ?: return
        val plan = service.fetchPlan(slug)
        val item = plan.toCartItem()
        cart.add(item)
        loadedItems = listOf(item)
        loadedPlanId = plan.id
    }

    override fun lineItems(): List<CartItem> = loadedItems

    override fun orderTotal(): Double = loadedItems.sumOf { it.price * it.quantity }

    override suspend fun submit(paymentMethodCode: String?): CheckoutResult {
        val request = SubscriptionCheckoutRequest(
            planId = loadedPlanId,
            tokenBundleIds = cart.items("token_bundle").map { it.id },
            addOnIds = cart.items("add_on").map { it.id },
            paymentMethodCode = paymentMethodCode ?: "",
        )
        return api.post(SubscriptionEndpoints.CHECKOUT, request)
    }

    override fun reset() {
        loadedItems = emptyList()
        loadedPlanId = null
    }
}

/** Request body for `POST /user/checkout` with a subscription plan. */
@Serializable
private data class SubscriptionCheckoutRequest(
    @SerialName("plan_id") val planId: String?,
    @SerialName("token_bundle_ids") val tokenBundleIds: List<String>,
    @SerialName("add_on_ids") val addOnIds: List<String>,
    @SerialName("payment_method_code") val paymentMethodCode: String,
)
