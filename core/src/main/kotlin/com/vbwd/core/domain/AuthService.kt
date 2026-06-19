package com.vbwd.core.domain

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiError
import com.vbwd.core.networking.ApiJson
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.get
import com.vbwd.core.networking.post
import com.vbwd.core.persistence.TokenStore

/**
 * Auth orchestration contract. Port of the `auth.ts` actions. Session/UI depend
 * on this interface, never on the concrete service (DIP).
 */
interface AuthService {
    /** POST login; on success persists token/refresh/user and returns the user. */
    suspend fun login(credentials: Credentials): AuthUser

    /** Calls logout (errors ignored, web parity) then clears local state. */
    suspend fun logout()

    /**
     * Restores a persisted session (web `initAuth`). Returns the user if a token
     * is present and the user blob decodes, else null.
     */
    fun restore(): AuthUser?

    /** Fetches the profile from the configured endpoint. */
    suspend fun fetchProfile(): AuthUser

    /** Deferred to Sprint 02 — throws [ApiError.NotImplemented]. */
    suspend fun refreshAccessToken(): String

    /**
     * Returns the persisted access token (S91). Plugins use this when they need
     * to forward the JWT outside the normal `ApiClient` request path — e.g.
     * seeding a WebView's `localStorage` so an embedded web app shares the
     * user's session. Default returns null so pre-S91 impls degrade gracefully.
     */
    fun currentToken(): String? = null
}

/**
 * Default [AuthService]. Depends only on injected interfaces (DIP); holds no
 * UI/transport concretions (SRP).
 */
class DefaultAuthService(
    private val client: ApiClient,
    private val store: TokenStore,
    private val endpoints: AuthEndpoints = AuthEndpoints(),
) : AuthService {

    override suspend fun login(credentials: Credentials): AuthUser {
        // Invalid credentials → backend 401 → ApiClient throws here; nothing is
        // persisted because persistence happens only after this returns.
        val response: LoginResponse = client.post(endpoints.login, credentials)

        val token = response.token
        val user = response.user
        if (token.isNullOrEmpty() || response.success == false || user == null) {
            throw ApiError.Http(status = 401, message = response.error ?: "Login failed")
        }

        store.saveToken(token)
        response.refreshToken?.let { store.saveRefreshToken(it) }
        store.saveUser(ApiJson.instance.encodeToString(AuthUser.serializer(), user).toByteArray())
        client.setToken(token)
        return user
    }

    override suspend fun logout() {
        // Best-effort server logout; ignore its outcome (web `.catch(()=>{})`).
        runCatching { client.post<EmptyResponse>(endpoints.logout) }
        runCatching { store.clear() }
        client.setToken(null)
    }

    override fun restore(): AuthUser? {
        val token = runCatching { store.loadToken() }.getOrNull()
        if (token.isNullOrEmpty()) return null
        client.setToken(token)
        val blob = runCatching { store.loadUser() }.getOrNull() ?: return null
        return runCatching {
            ApiJson.instance.decodeFromString(AuthUser.serializer(), blob.decodeToString())
        }.getOrNull()
    }

    override suspend fun fetchProfile(): AuthUser = client.get(endpoints.profile)

    override suspend fun refreshAccessToken(): String =
        throw ApiError.NotImplemented("token refresh deferred to Sprint 02")

    override fun currentToken(): String? = runCatching { store.loadToken() }.getOrNull()
}
