package com.vbwd.plugin.tokenpayment

import com.vbwd.core.plugins.PaymentAction
import com.vbwd.core.plugins.PlatformSdk
import com.vbwd.core.plugins.Plugin
import com.vbwd.core.plugins.PluginMetadata
import com.vbwd.core.plugins.SemanticVersion
import com.vbwd.plugin.tokenpayment.ui.TokenQuoteSection

/**
 * Token-balance payment plugin: pay an order from the token balance — instant,
 * no redirect. Port of the iOS `TokenPaymentPlugin`. Registers the
 * `PaymentMethodToken_balance` section + the `token_balance` payment action.
 */
class TokenPaymentPlugin : Plugin {
    override val metadata = PluginMetadata(
        name = "token-payment",
        version = SemanticVersion(1, 0, 0),
        description = "Pay with your token balance — instant, no redirect.",
        author = "VBWD",
        keywords = listOf("payment", "tokens"),
        translations = mapOf("en" to TRANSLATIONS),
    )

    override suspend fun install(sdk: PlatformSdk) {
        val api = sdk.api
        sdk.addComponent("PaymentMethodToken_balance") { TokenQuoteSection(api) }
        sdk.addPaymentAction("token_balance") { invoiceId ->
            TokenPaymentService(api).payWithTokens(invoiceId)
            PaymentAction.ShowConfirmation
        }
        sdk.addTranslations("en", TRANSLATIONS)
    }

    private companion object {
        val TRANSLATIONS = mapOf(
            "token_payment.balance" to "Token balance now",
            "token_payment.cost" to "Tokens to pay",
            "token_payment.after" to "Balance after payment",
            "token_payment.insufficient" to "Not enough tokens to pay this order.",
        )
    }
}
