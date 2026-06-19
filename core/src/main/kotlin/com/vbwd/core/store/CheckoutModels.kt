package com.vbwd.core.store

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from `POST /user/checkout`. Port of the iOS `CheckoutResult`: the
 * backend returns a nested `invoice`; convenience accessors are surfaced here.
 */
@Serializable
data class CheckoutResult(
    val invoice: CheckoutInvoice? = null,
    val message: String? = null,
) {
    val invoiceId: String? get() = invoice?.id
    val status: String? get() = invoice?.status

    /** Returns a copy with the invoice status overridden. */
    fun withStatus(newStatus: String): CheckoutResult =
        invoice?.let { copy(invoice = it.copy(status = newStatus)) } ?: this
}

/** Subset of the invoice payload from `POST /user/checkout`. Port of `CheckoutInvoice`. */
@Serializable
data class CheckoutInvoice(
    val id: String,
    @SerialName("invoice_number") val invoiceNumber: String? = null,
    val amount: String? = null,
    @SerialName("total_amount") val totalAmount: String? = null,
    val currency: String? = null,
    val status: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("line_items") val lineItems: List<CheckoutLineItem>? = null,
)

@Serializable
data class CheckoutLineItem(
    val id: String? = null,
    val description: String? = null,
    val quantity: Int? = null,
    @SerialName("unit_price") val unitPrice: String? = null,
    val amount: String? = null,
)
