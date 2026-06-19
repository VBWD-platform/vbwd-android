package com.vbwd.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Dashboard data models + endpoints. Shapes mirror the fields the backend
 * `invoice.to_dict()` returns (web `Invoices.vue` / `InvoiceDetail.vue`).
 */
@Serializable
data class Invoice(
    val id: String,
    @SerialName("invoice_number") val invoiceNumber: String? = null,
    @SerialName("invoiced_at") val invoicedAt: String? = null,
    val amount: String? = null,
    val subtotal: String? = null,
    @SerialName("tax_amount") val taxAmount: String? = null,
    @SerialName("total_amount") val totalAmount: String? = null,
    val currency: String? = null,
    val status: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("payment_ref") val paymentRef: String? = null,
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("line_items") val lineItems: List<InvoiceLineItem>? = null,
)

/**
 * Line item within an invoice. Returned by the detail endpoint
 * `GET /user/invoices/{id}`.
 */
@Serializable
data class InvoiceLineItem(
    val id: String? = null,
    val type: String? = null,
    val description: String? = null,
    val quantity: Int? = null,
    @SerialName("unit_price") val unitPrice: String? = null,
    val amount: String? = null,
)

@Serializable
data class TokenTransaction(
    val id: String,
    val amount: Double,
    @SerialName("transaction_type") val transactionType: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
internal data class TokenBalanceResponse(val balance: Double? = null)

@Serializable
internal data class TokenTransactionsResponse(val transactions: List<TokenTransaction>? = null)

@Serializable
internal data class InvoicesResponse(val invoices: List<Invoice>? = null)

@Serializable
internal data class InvoiceDetailResponse(val invoice: Invoice? = null)

/** Configurable dashboard endpoints (web `Dashboard.vue` paths). */
data class DashboardEndpoints(
    val tokenBalance: String = "/user/tokens/balance",
    val tokenTransactions: String = "/user/tokens/transactions?limit=7",
    val invoices: String = "/user/invoices",
)
