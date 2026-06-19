package com.vbwd.core.networking

/**
 * Transport configuration. Port of the iOS `APIClientConfig` / web
 * `ApiClientConfig`: `baseUrl`, `timeout` (default 30s — web used 30000ms), and
 * headers with a default `Content-Type: application/json` that callers may
 * override.
 */
data class ApiClientConfig(
    val baseUrl: String,
    val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    val logging: ApiTrafficLogger = ApiTrafficLogger.default(),
    private val extraHeaders: Map<String, String> = emptyMap(),
) {
    /** Default first, caller-supplied headers override (web spread order). */
    val headers: Map<String, String> =
        buildMap {
            put("Content-Type", "application/json")
            put("X-Client-Platform", "android")
            putAll(extraHeaders)
        }

    companion object {
        /** Web parity: `ApiClient` default timeout was 30000ms. */
        const val DEFAULT_TIMEOUT_MILLIS: Long = 30_000
    }
}
