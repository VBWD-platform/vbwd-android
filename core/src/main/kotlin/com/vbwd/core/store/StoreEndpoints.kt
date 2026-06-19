package com.vbwd.core.store

/**
 * Configurable store & checkout endpoints (same pattern as `AuthEndpoints` etc).
 * Port of the iOS `StoreEndpoints`.
 */
data class StoreEndpoints(
    val tokenBundles: String = "/token-bundles",
    val paymentMethods: String = "/settings/payment-methods",
    val checkout: String = "/user/checkout",
)
