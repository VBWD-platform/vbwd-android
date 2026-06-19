package com.vbwd.core.networking

/**
 * Where the bearer token comes from. Segregated from the client (ISP): the
 * transport asks for a token, it does not own auth state. Port of the iOS
 * `AuthTokenProvider`.
 */
interface AuthTokenProvider {
    var token: String?
}

/** Default mutable provider. `AuthService` writes it via `ApiClient.setToken`. */
class MutableTokenProvider(@Volatile override var token: String? = null) : AuthTokenProvider
