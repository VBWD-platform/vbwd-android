package com.vbwd.core.networking

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * The only OkHttp site in the SDK (the Android port of the iOS
 * `URLSessionAPIClient`, enforced by the boundary check by convention).
 * Bearer injection, JSON bodies, non-2xx → mapped [ApiError], 401 →
 * [ApiEvent.TOKEN_EXPIRED].
 */
class OkHttpApiClient(
    private val config: ApiClientConfig,
    private val tokenProvider: AuthTokenProvider = MutableTokenProvider(),
    httpClient: OkHttpClient? = null,
) : ApiClient {

    private val client: OkHttpClient =
        (httpClient ?: OkHttpClient()).newBuilder()
            .callTimeout(config.timeoutMillis, TimeUnit.MILLISECONDS)
            .build()

    private val handlers = ConcurrentHashMap<ApiEvent, CopyOnWriteArrayList<() -> Unit>>()

    override suspend fun <T> request(
        method: HttpMethod,
        path: String,
        jsonBody: String?,
        deserializer: DeserializationStrategy<T>,
    ): T = withContext(Dispatchers.IO) {
        val url = makeUrl(path)
        val requestBody = when (method) {
            HttpMethod.GET, HttpMethod.DELETE -> null
            else -> (jsonBody ?: "").toRequestBody(JSON_MEDIA_TYPE)
        }
        val builder = Request.Builder().url(url).method(method.wireName, requestBody)
        config.headers.forEach { (name, value) -> builder.header(name, value) }
        tokenProvider.token?.let { builder.header("Authorization", "Bearer $it") }

        config.logging.logRequest(method.wireName, url, jsonBody)

        val (statusCode, responseBody) = try {
            client.newCall(builder.build()).execute().use { response ->
                response.code to response.body?.string().orEmpty()
            }
        } catch (error: java.io.IOException) {
            config.logging.logTransportError(method.wireName, url, error)
            throw ApiError.fromTransport(error)
        }

        config.logging.logResponse(method.wireName, url, statusCode, responseBody)

        if (statusCode == HTTP_UNAUTHORIZED) emit(ApiEvent.TOKEN_EXPIRED)

        if (statusCode !in SUCCESS_RANGE) {
            throw ApiError.fromResponse(statusCode, responseBody, "HTTP $statusCode")
        }

        decode(responseBody, deserializer)
    }

    override fun setToken(token: String?) {
        tokenProvider.token = token
    }

    override fun on(event: ApiEvent, handler: () -> Unit) {
        handlers.getOrPut(event) { CopyOnWriteArrayList() }.add(handler)
    }

    private fun emit(event: ApiEvent) {
        handlers[event]?.forEach { it() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> decode(body: String, deserializer: DeserializationStrategy<T>): T {
        if (deserializer.descriptor == EmptyResponse.serializer().descriptor) {
            return EmptyResponse() as T
        }
        return try {
            ApiJson.instance.decodeFromString(deserializer, body)
        } catch (error: kotlinx.serialization.SerializationException) {
            throw ApiError.fromDecoding(error)
        } catch (error: IllegalArgumentException) {
            throw ApiError.fromDecoding(error)
        }
    }

    private fun makeUrl(path: String): String {
        val isAbsolute = path.startsWith("http://") || path.startsWith("https://")
        return if (isAbsolute) path else config.baseUrl + path
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        val SUCCESS_RANGE = 200..299
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
