package com.vbwd.core.ui

import com.vbwd.core.domain.AuthUser
import com.vbwd.core.session.AuthState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RootRouterTest {
    private val user = AuthUser(id = "u1", email = "test@example.com")

    @Test
    fun `signed out routes to login`() {
        assertEquals(RootRoute.Login, RootRouter.route(AuthState.SignedOut))
    }

    @Test
    fun `error routes to login so the banner shows on the login screen`() {
        assertEquals(RootRoute.Login, RootRouter.route(AuthState.Error("boom")))
    }

    @Test
    fun `authenticating routes to loading`() {
        assertEquals(RootRoute.Loading, RootRouter.route(AuthState.Authenticating))
    }

    @Test
    fun `authenticated routes to the authenticated root carrying the user`() {
        assertEquals(
            RootRoute.Authenticated(user),
            RootRouter.route(AuthState.Authenticated(user)),
        )
    }
}
