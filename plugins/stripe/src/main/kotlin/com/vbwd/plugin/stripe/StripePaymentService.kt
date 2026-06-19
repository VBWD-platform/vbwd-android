package com.vbwd.plugin.stripe

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.get
import com.vbwd.core.networking.post
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response from `POST /plugins/stripe/create-session`. */
@Serializable
data class StripeSessionResponse(
    @SerialName("session_url") val sessionUrl: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
)

/** Response from `GET /plugins/stripe/session-status/{id}`. */
@Serializable
data class StripeStatusResponse(
    val status: String? = null,
    @SerialName("invoice_id") val invoiceId: String? = null,
)

/** Thin service over the Stripe backend endpoints. Port of the iOS service. */
class StripePaymentService(
    private val api: ApiClient,
    private val baseEndpoint: String = "/plugins/stripe",
) {
    suspend fun createSession(invoiceId: String): StripeSessionResponse =
        api.post("$baseEndpoint/create-session", CreateSessionBody(invoiceId))

    suspend fun checkStatus(sessionId: String): StripeStatusResponse =
        api.get("$baseEndpoint/session-status/$sessionId")

    @Serializable
    private data class CreateSessionBody(@SerialName("invoice_id") val invoiceId: String)
}
