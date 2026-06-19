package com.vbwd.core.store

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Store & checkout models. Port of the iOS `StoreModels`: `TokenBundle` (backend
 * model) + `PaymentMethod` (settings endpoint). [CheckoutItem] keeps the
 * checkout screen source-agnostic (OCP — bundles today, plans tomorrow).
 */
@Serializable
data class TokenBundle(
    val id: String,
    val name: String,
    val slug: String? = null,
    val description: String? = null,
    @SerialName("token_amount") val tokenAmount: Int,
    val price: String,
    val currency: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
) : CheckoutItem {
    override val checkoutItemId: String get() = id
    override val checkoutItemName: String get() = name
    override val checkoutItemPrice: String get() = price
    override val checkoutItemCurrency: String get() = currency ?: "USD"
    override val checkoutItemQuantity: Int get() = 1
}

@Serializable
internal data class TokenBundlesResponse(val bundles: List<TokenBundle>? = null)

@Serializable
data class PaymentMethod(
    val id: String,
    val code: String,
    val name: String,
    val icon: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
)

@Serializable
internal data class PaymentMethodsResponse(val methods: List<PaymentMethod>? = null)

/**
 * Anything purchasable. The checkout screen renders items through this so it
 * stays source-agnostic (OCP). Port of the iOS `CheckoutItem` protocol.
 */
interface CheckoutItem {
    val checkoutItemId: String
    val checkoutItemName: String
    val checkoutItemPrice: String
    val checkoutItemCurrency: String
    val checkoutItemQuantity: Int
}
