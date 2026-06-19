package com.vbwd.core.ui.checkout

import androidx.compose.runtime.compositionLocalOf

/**
 * Order amount/currency exposed to plugin-provided payment-method detail
 * sections via [LocalCheckoutInfo] (port of the iOS `checkoutInfo` environment).
 * A token-payment plugin reads this to quote the cost without referencing the
 * [CheckoutViewModel] directly.
 */
data class CheckoutInfo(val amount: Double = 0.0, val currency: String = "USD")

/** The current checkout's [CheckoutInfo]; provided by `CheckoutScreen`. */
val LocalCheckoutInfo = compositionLocalOf { CheckoutInfo() }
