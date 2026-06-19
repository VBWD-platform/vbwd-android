package com.vbwd.core.testutil

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiError
import com.vbwd.core.networking.ApiEvent
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.HttpMethod
import kotlinx.serialization.DeserializationStrategy

/**
 * Minimal [ApiClient] fake for plugin/event tests. Returns [nextResult] (or an
 * [EmptyResponse]) for any call; set [nextError] to make the next request throw
 * (e.g. to exercise retry/fallback). [nextResult] lets a test return a typed
 * body (e.g. a `CheckoutResult`) without a real codec.
 */
class FakeApiClient(
    var nextError: ApiError? = null,
    var nextResult: Any? = null,
) : ApiClient {
    var requestCount = 0
    var lastPath: String? = null
    var lastBody: String? = null

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> request(
        method: HttpMethod,
        path: String,
        jsonBody: String?,
        deserializer: DeserializationStrategy<T>,
    ): T {
        requestCount++
        lastPath = path
        lastBody = jsonBody
        nextError?.let { throw it }
        return (nextResult ?: EmptyResponse()) as T
    }

    override fun setToken(token: String?) = Unit
    override fun on(event: ApiEvent, handler: () -> Unit) = Unit
}
