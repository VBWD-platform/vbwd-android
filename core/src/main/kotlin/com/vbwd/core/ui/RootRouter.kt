package com.vbwd.core.ui

import com.vbwd.core.domain.AuthUser
import com.vbwd.core.session.AuthState

/**
 * Where the app should be for a given [AuthState]. Extracted from the root
 * composable so the auth-guard decision (port of the web router guard) is
 * unit-testable without rendering Compose (SRP). Port of the iOS `RootRoute`.
 */
sealed interface RootRoute {
    data object Login : RootRoute
    data object Loading : RootRoute
    data class Authenticated(val user: AuthUser) : RootRoute
}

/**
 * Pure auth-guard mapping. `SignedOut`/`Error` → login (error keeps the user on
 * login with a banner); `Authenticating` → loading; `Authenticated` → the
 * authenticated root. Port of the iOS `RootRouter.route(for:)`.
 */
object RootRouter {
    fun route(state: AuthState): RootRoute = when (state) {
        AuthState.SignedOut, is AuthState.Error -> RootRoute.Login
        AuthState.Authenticating -> RootRoute.Loading
        is AuthState.Authenticated -> RootRoute.Authenticated(state.user)
    }
}
