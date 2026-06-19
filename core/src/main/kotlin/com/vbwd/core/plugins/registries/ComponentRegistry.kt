package com.vbwd.core.plugins.registries

import com.vbwd.core.plugins.ComponentFactory
import com.vbwd.core.plugins.PaymentActionHandler

/**
 * Named component registry. Port of the iOS `ComponentRegistry`:
 * `add`/`remove`/`get`/`all`, registration-ordered discovery by the
 * `Dashboard*` / `Profile*` prefix convention (DRY — the dashboard/profile
 * screens reuse it), and post-checkout payment-action handlers.
 *
 * Insertion order is preserved by [LinkedHashMap]; re-adding an existing name
 * updates the factory without changing its position (iOS appended only on first
 * add).
 */
class ComponentRegistry {
    private val components = LinkedHashMap<String, ComponentFactory>()
    private val paymentHandlers = mutableMapOf<String, PaymentActionHandler>()

    fun add(name: String, factory: ComponentFactory) {
        components[name] = factory
    }

    fun remove(name: String) {
        components.remove(name)
    }

    fun get(name: String): ComponentFactory? = components[name]

    fun all(): Map<String, ComponentFactory> = components.toMap()

    /** Names in registration order (deterministic widget ordering). */
    fun names(): List<String> = components.keys.toList()

    /** `Dashboard*` widgets, in registration order. */
    fun dashboardComponents(): List<Pair<String, ComponentFactory>> =
        components.entries.filter { it.key.startsWith("Dashboard") }.map { it.key to it.value }

    /** `Profile*` sections, in registration order. */
    fun profileComponents(): List<Pair<String, ComponentFactory>> =
        components.entries.filter { it.key.startsWith("Profile") }.map { it.key to it.value }

    /** `Checkout*` sections injected into the checkout screen, in registration order. */
    fun checkoutComponents(): List<Pair<String, ComponentFactory>> =
        components.entries.filter { it.key.startsWith("Checkout") }.map { it.key to it.value }

    /**
     * Payment-method codes a plugin has installed: a `PaymentMethod{Code}`
     * component declares support for `code` (the suffix, lower-cased). E.g.
     * `PaymentMethodStripe` → `"stripe"`.
     */
    fun supportedPaymentMethodCodes(): Set<String> =
        components.keys
            .filter { it.startsWith(PAYMENT_METHOD_PREFIX) }
            .map { it.removePrefix(PAYMENT_METHOD_PREFIX).lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

    /** Detail view for a payment-method code (`PaymentMethod{Capitalized}`). */
    fun paymentMethodDetail(code: String): ComponentFactory? =
        components[PAYMENT_METHOD_PREFIX + code.replaceFirstChar { it.uppercase() }]

    /** Register a post-checkout handler for a payment-method code. */
    fun addPaymentAction(code: String, handler: PaymentActionHandler) {
        paymentHandlers[code.lowercase()] = handler
    }

    /** Look up the payment-action handler for a method code (null ⇒ confirm). */
    fun paymentAction(code: String): PaymentActionHandler? = paymentHandlers[code.lowercase()]

    private companion object {
        const val PAYMENT_METHOD_PREFIX = "PaymentMethod"
    }
}
