package com.vbwd.core.networking

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Transport/error model. Port of the iOS `APIError` / web `ApiError`:
 * non-2xx → [Http], network failure → [Transport], body decode failure →
 * [Decoding]. [NotImplemented] backs deferred features (e.g. token refresh).
 */
sealed class ApiError : Exception() {
    abstract override val message: String

    data class Http(val status: Int, override val message: String) : ApiError()
    data class Transport(override val message: String) : ApiError()
    data class Decoding(override val message: String) : ApiError()
    data class NotImplemented(override val message: String) : ApiError()

    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        /**
         * Builds an [Http] error, extracting the message the way the web client
         * did: response body `error`, then `message`, else the HTTP status text.
         */
        fun fromResponse(status: Int, body: String, statusText: String): Http =
            Http(status, messageFromBody(body) ?: statusText)

        fun fromTransport(error: Throwable): Transport =
            Transport(error.message ?: error.toString())

        fun fromDecoding(error: Throwable): Decoding =
            Decoding(error.message ?: error.toString())

        internal fun messageFromBody(body: String): String? {
            if (body.isBlank()) return null
            val obj = runCatching { parser.parseToJsonElement(body).jsonObject }.getOrNull()
                ?: return null
            fun field(name: String): String? =
                runCatching { obj[name]?.jsonPrimitive?.content }.getOrNull()?.takeIf(String::isNotEmpty)
            return field("error") ?: field("message")
        }
    }
}
