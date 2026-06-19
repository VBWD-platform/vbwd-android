package com.vbwd.plugin.stripe

import com.vbwd.core.networking.ApiError
import com.vbwd.core.plugins.PaymentAction
import com.vbwd.core.plugins.PlatformSdk
import com.vbwd.core.plugins.Plugin
import com.vbwd.core.plugins.PluginMetadata
import com.vbwd.core.plugins.SemanticVersion
import com.vbwd.plugin.stripe.ui.StripeRedirectSection

/**
 * Stripe payment plugin: after checkout creates the invoice, the payment action
 * creates a Stripe Checkout session and returns its URL to open. Port of the iOS
 * `StripePaymentPlugin` — intentionally thin (all logic is server-side).
 */
class StripePaymentPlugin : Plugin {
    override val metadata = PluginMetadata(
        name = "stripe-payment",
        version = SemanticVersion(1, 0, 0),
        description = "Stripe payment processing — redirects to Stripe Checkout.",
        author = "VBWD",
        keywords = listOf("payment", "stripe"),
        translations = mapOf("en" to TRANSLATIONS),
    )

    override suspend fun install(sdk: PlatformSdk) {
        val api = sdk.api
        sdk.addComponent("PaymentMethodStripe") { StripeRedirectSection() }
        sdk.addPaymentAction("stripe") { invoiceId ->
            val response = StripePaymentService(api).createSession(invoiceId)
            val url = response.sessionUrl
                ?: throw ApiError.Http(status = 0, message = "No redirect URL received from Stripe")
            PaymentAction.OpenUrl(url, response.sessionId)
        }
        sdk.addTranslations("en", TRANSLATIONS)
    }

    private companion object {
        val TRANSLATIONS = mapOf(
            "stripe.payment.redirecting" to "Redirecting to Stripe…",
            "stripe.payment.error" to "Failed to create payment session",
            "stripe.success.title" to "Payment Successful",
            "stripe.cancel.title" to "Payment Cancelled",
        )
    }
}
