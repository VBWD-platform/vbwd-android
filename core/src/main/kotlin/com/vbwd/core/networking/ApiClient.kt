package com.vbwd.core.networking

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/** Transport events the client emits. Port of the web `ApiEvent`. */
enum class ApiEvent { TOKEN_EXPIRED }

/** Decode target for endpoints with no/empty body (e.g. logout). */
@Serializable
class EmptyResponse {
    override fun equals(other: Any?): Boolean = other is EmptyResponse
    override fun hashCode(): Int = javaClass.hashCode()
}

/**
 * Transport contract. Port of the iOS `APIClient` / web `ApiClient` public
 * surface. Domain code depends on this interface, never on [OkHttpApiClient]
 * (DIP).
 *
 * [request] is the single mockable seam: it performs the call (bearer injection,
 * 401 → [ApiEvent.TOKEN_EXPIRED], non-2xx → [ApiError]) and decodes the success
 * body with the supplied [deserializer]. The reified `get`/`post`/… extension
 * functions below are ergonomic sugar over it.
 */
interface ApiClient {
    suspend fun <T> request(
        method: HttpMethod,
        path: String,
        jsonBody: String?,
        deserializer: DeserializationStrategy<T>,
    ): T

    /** Raw byte download (e.g. PDF). Default impl throws (not all support it). */
    suspend fun getData(path: String): ByteArray =
        throw ApiError.Transport("getData not supported")

    /** Set/clear the bearer token (web `setToken`/`clearToken`). */
    fun setToken(token: String?)

    /** Subscribe to a transport event (web `on`). */
    fun on(event: ApiEvent, handler: () -> Unit)
}

/** Shared JSON codec. Lenient on unknown keys; encodes defaults (web parity). */
object ApiJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        // Backend may send `null` for a non-null field with a default (e.g. a
        // fresh user's profile strings). Coerce null → default, mirroring the
        // iOS `decodeIfPresent(...) ?? ""` behaviour. Malformed JSON still throws.
        coerceInputValues = true
    }
}

suspend inline fun <reified T> ApiClient.get(path: String): T =
    request(HttpMethod.GET, path, null, serializer())

suspend inline fun <reified T> ApiClient.post(path: String): T =
    request(HttpMethod.POST, path, null, serializer())

suspend inline fun <reified B, reified T> ApiClient.post(path: String, body: B): T =
    request(HttpMethod.POST, path, ApiJson.instance.encodeToString(serializer<B>(), body), serializer())

suspend inline fun <reified T> ApiClient.put(path: String): T =
    request(HttpMethod.PUT, path, null, serializer())

suspend inline fun <reified B, reified T> ApiClient.put(path: String, body: B): T =
    request(HttpMethod.PUT, path, ApiJson.instance.encodeToString(serializer<B>(), body), serializer())

suspend inline fun <reified B, reified T> ApiClient.patch(path: String, body: B): T =
    request(HttpMethod.PATCH, path, ApiJson.instance.encodeToString(serializer<B>(), body), serializer())

suspend inline fun <reified T> ApiClient.delete(path: String): T =
    request(HttpMethod.DELETE, path, null, serializer())
