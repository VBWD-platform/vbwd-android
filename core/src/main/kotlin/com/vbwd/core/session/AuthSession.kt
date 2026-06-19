package com.vbwd.core.session

import com.vbwd.core.domain.AuthService
import com.vbwd.core.domain.AuthUser
import com.vbwd.core.domain.Credentials
import com.vbwd.core.networking.ApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Observable auth state machine. Port of the iOS `AuthSession` (the `auth.ts`
 * reactive store). Depends only on [AuthService] (ISP/DIP) — it knows nothing
 * of transport or persistence.
 *
 * The EventBus the iOS session emits `authLogin`/`authLogout` on arrives in A02;
 * A01 keeps the session event-free (no premature event layer). Auto sign-out on
 * a 401 is wired by the composition root via the `ApiClient` 401 callback seam.
 */
class AuthSession(private val service: AuthService) {
    private val _state = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /** Web `isAuthenticated`: an authenticated state. */
    val isAuthenticated: Boolean
        get() = _state.value is AuthState.Authenticated

    val currentUser: AuthUser?
        get() = (_state.value as? AuthState.Authenticated)?.user

    /** Current access token, if any (S91 — plugins forwarding the raw JWT). */
    val accessToken: String?
        get() = service.currentToken()

    /** Web `initAuth`: restore a persisted session on app start. */
    fun start() {
        _state.value = service.restore()
            ?.let { AuthState.Authenticated(it) }
            ?: AuthState.SignedOut
    }

    suspend fun signIn(credentials: Credentials) {
        _state.value = AuthState.Authenticating
        _state.value = try {
            AuthState.Authenticated(service.login(credentials))
        } catch (error: ApiError) {
            AuthState.Error(error.message)
        }
    }

    suspend fun signOut() {
        service.logout()
        _state.value = AuthState.SignedOut
    }
}
