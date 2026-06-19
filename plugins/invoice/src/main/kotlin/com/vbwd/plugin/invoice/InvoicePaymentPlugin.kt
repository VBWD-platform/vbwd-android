package com.vbwd.plugin.invoice

import com.vbwd.core.plugins.PlatformSdk
import com.vbwd.core.plugins.Plugin
import com.vbwd.core.plugins.PluginMetadata
import com.vbwd.core.plugins.SemanticVersion
import com.vbwd.plugin.invoice.ui.InvoiceInfoSection

/**
 * Invoice payment plugin: the simplest method — the backend creates an invoice
 * at checkout and emails it; no redirect, no payment action (a missing handler
 * makes the generic checkout go straight to confirmation). Port of the iOS
 * `InvoicePaymentPlugin`.
 */
class InvoicePaymentPlugin : Plugin {
    override val metadata = PluginMetadata(
        name = "invoice-payment",
        version = SemanticVersion(1, 0, 0),
        description = "Invoice payment — pay by bank transfer after receiving an invoice.",
        author = "VBWD",
        keywords = listOf("payment", "invoice"),
        translations = mapOf("en" to TRANSLATIONS),
    )

    override suspend fun install(sdk: PlatformSdk) {
        sdk.addComponent("PaymentMethodInvoice") { InvoiceInfoSection() }
        sdk.addTranslations("en", TRANSLATIONS)
    }

    private companion object {
        val TRANSLATIONS = mapOf(
            "invoice.payment.title" to "Invoice Payment",
            "invoice.payment.info" to "An invoice will be sent to your email address.",
            "invoice.payment.instructions" to "You will receive payment instructions by email after checkout.",
        )
    }
}
