package com.vbwd.core.networking

/**
 * HTTP verbs the SDK issues. Port of the iOS `HTTPMethod` / web `ApiClient`
 * surface (get/post/put/patch/delete).
 */
enum class HttpMethod(val wireName: String) {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE"),
}
