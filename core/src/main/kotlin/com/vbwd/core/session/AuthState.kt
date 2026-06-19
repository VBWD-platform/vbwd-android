package com.vbwd.core.session

import com.vbwd.core.domain.AuthUser

/**
 * Reactive auth state for the UI layer (port of the iOS `AuthState` enum /
 * the web Pinia store shape). A sealed interface so `when` over it is
 * exhaustive and `RootRouter` can map it without an `else`.
 */
sealed interface AuthState {
    data object SignedOut : AuthState
    data object Authenticating : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
    data class Error(val message: String) : AuthState
}
