package com.vbwd.plugin.tokenpayment

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.get
import com.vbwd.core.networking.post
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Quote response from `GET /plugins/token-payment/quote`. */
@Serializable
data class TokenQuote(
    val available: Boolean? = null,
    @SerialName("tokens_needed") val tokensNeeded: Double? = null,
    val balance: Double? = null,
    @SerialName("balance_after") val balanceAfter: Double? = null,
    val sufficient: Boolean? = null,
)

/** API calls for the token-payment backend plugin. Port of the iOS service. */
class TokenPaymentService(private val api: ApiClient) {

    suspend fun fetchQuote(amount: Double, currency: String): TokenQuote =
        api.get("/plugins/token-payment/quote?amount=$amount&currency=$currency")

    suspend fun payWithTokens(invoiceId: String) {
        api.post<EmptyBody, EmptyResponse>("/plugins/token-payment/invoices/$invoiceId/pay", EmptyBody())
    }

    @Serializable
    private class EmptyBody
}
