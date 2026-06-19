package com.vbwd.plugin.subscription.domain

import com.vbwd.core.cart.CartItem
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * The backend `features` field is **either** `["a","b"]` **or** `{"a":true,...}`.
 * This serializer accepts both (object ⇒ sorted keys), mirroring the iOS custom
 * decoder, so a dict payload never fails the whole `TarifPlan` decode.
 */
internal object FeaturesAsListSerializer : KSerializer<List<String>?> {
    override val descriptor: SerialDescriptor = ListSerializer(String.serializer()).descriptor

    override fun deserialize(decoder: Decoder): List<String>? {
        val input = decoder as? JsonDecoder ?: return null
        return when (val element = input.decodeJsonElement()) {
            is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            is JsonObject -> element.keys.sorted()
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>?) {
        val list = value ?: emptyList()
        encoder.encodeSerializableValue(ListSerializer(String.serializer()), list)
    }
}

@Serializable
data class TarifPlan(
    val id: String,
    val name: String,
    val slug: String? = null,
    val description: String? = null,
    @SerialName("display_price") val displayPrice: Double? = null,
    @SerialName("display_currency") val displayCurrency: String? = null,
    @SerialName("billing_period") val billingPeriod: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("net_price") val netPrice: Double? = null,
    @SerialName("tax_amount") val taxAmount: Double? = null,
    @SerialName("gross_price") val grossPrice: Double? = null,
    @SerialName("tax_rate") val taxRate: Double? = null,
    @Serializable(with = FeaturesAsListSerializer::class) val features: List<String>? = null,
    val popular: Boolean? = null,
) {
    /** Display price, e.g. "USD 29.99 / month". */
    val formattedPrice: String
        get() {
            val price = displayPrice ?: grossPrice ?: 0.0
            return String.format(
                java.util.Locale.US,
                "%s %.2f / %s",
                displayCurrency ?: "USD",
                price,
                billingPeriod ?: "month",
            )
        }
}

@Serializable
data class Subscription(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("tarif_plan_id") val tarifPlanId: String? = null,
    @SerialName("pending_plan_id") val pendingPlanId: String? = null,
    val status: String? = null,
    @SerialName("is_valid") val isValid: Boolean? = null,
    @SerialName("days_remaining") val daysRemaining: Int? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("cancelled_at") val cancelledAt: String? = null,
    @SerialName("paused_at") val pausedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val plan: TarifPlan? = null,
    @SerialName("pending_plan") val pendingPlan: TarifPlan? = null,
) {
    val statusLabel: String get() = status?.uppercase() ?: "UNKNOWN"
    val isActive: Boolean get() = status?.uppercase() == "ACTIVE"
}

@Serializable
data class AddOn(
    val id: String,
    val name: String,
    val slug: String? = null,
    val description: String? = null,
    val price: String? = null,
    val currency: String? = null,
    @SerialName("billing_period") val billingPeriod: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("tarif_plan_ids") val tarifPlanIds: List<String>? = null,
) {
    val isSubscriptionDependent: Boolean get() = tarifPlanIds?.isNotEmpty() == true
}

/** Lightweight add-on data nested in addon-subscription responses (no catalog id). */
@Serializable
data class AddOnInfo(
    val name: String? = null,
    val slug: String? = null,
    val description: String? = null,
    val price: String? = null,
    val currency: String? = null,
    @SerialName("billing_period") val billingPeriod: String? = null,
)

@Serializable
data class AddonSubscription(
    val id: String,
    @SerialName("addon_id") val addonId: String? = null,
    val status: String? = null,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("cancelled_at") val cancelledAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val addon: AddOnInfo? = null,
) {
    val statusLabel: String get() = status?.uppercase() ?: "UNKNOWN"
    val isActive: Boolean get() = status?.uppercase() == "ACTIVE"
}

// --- API response wrappers (internal — the service unwraps them) ---

@Serializable
internal data class PlansResponse(val plans: List<TarifPlan>? = null, val currency: String? = null)

@Serializable
internal data class SubscriptionResponse(val subscription: Subscription? = null)

@Serializable
internal data class SubscriptionsListResponse(val subscriptions: List<Subscription>? = null)

@Serializable
internal data class AddOnsResponse(val addons: List<AddOn>? = null)

@Serializable
internal data class UserAddOnsResponse(
    @SerialName("addon_subscriptions") val addonSubscriptions: List<AddonSubscription>? = null,
)

@Serializable
internal data class UserAddOnResponse(
    @SerialName("addon_subscription") val addonSubscription: AddonSubscription? = null,
)

@Serializable
internal data class CancelResponse(
    val subscription: Subscription? = null,
    val message: String? = null,
)

/** Convert a tarif plan to a generic cart item for checkout. */
fun TarifPlan.toCartItem(): CartItem = CartItem(
    type = "subscription",
    id = id,
    name = name,
    price = displayPrice ?: grossPrice ?: 0.0,
    quantity = 1,
    currency = displayCurrency ?: "USD",
    metadata = mapOf(
        "slug" to (slug ?: ""),
        "billing_period" to (billingPeriod ?: "month"),
    ),
)
