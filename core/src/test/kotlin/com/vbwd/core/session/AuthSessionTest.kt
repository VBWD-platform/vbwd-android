package com.vbwd.core.session

import app.cash.turbine.test
import com.vbwd.core.domain.AuthService
import com.vbwd.core.domain.AuthUser
import com.vbwd.core.domain.Credentials
import com.vbwd.core.networking.ApiError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private val testUser = AuthUser(id = "u1", email = "test@example.com", name = "John")
private val creds = Credentials("test@example.com", "pw")

/**
 * Configurable [AuthService] fake. `loginGate` lets a test hold `login` open so
 * the transient `Authenticating` state is observable before completion.
 */
private class FakeAuthService(
    var restored: AuthUser? = null,
    var token: String? = null,
) : AuthService {
    var loginGate: CompletableDeferred<Unit>? = null
    var loginError: ApiError? = null
    var loggedOut = false

    override suspend fun login(credentials: Credentials): AuthUser {
        loginGate?.await()
        loginError?.let { throw it }
        return testUser
    }

    override suspend fun logout() {
        loggedOut = true
    }

    override fun restore(): AuthUser? = restored
    override suspend fun fetchProfile(): AuthUser = testUser
    override suspend fun refreshAccessToken(): String = throw ApiError.NotImplemented("nope")
    override fun currentToken(): String? = token
}

class AuthSessionTest {
    @Test
    fun `signIn emits SignedOut, Authenticating, Authenticated on success`() = runTest {
        val service = FakeAuthService().apply { loginGate = CompletableDeferred() }
        val session = AuthSession(service)

        session.state.test {
            assertEquals(AuthState.SignedOut, awaitItem())
            val job = launch { session.signIn(creds) }
            assertEquals(AuthState.Authenticating, awaitItem())
            service.loginGate!!.complete(Unit)
            assertEquals(AuthState.Authenticated(testUser), awaitItem())
            job.join()
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(session.isAuthenticated)
        assertEquals(testUser, session.currentUser)
    }

    @Test
    fun `signIn surfaces an Error state when login throws`() = runTest {
        val service = FakeAuthService().apply { loginError = ApiError.Http(401, "Invalid credentials") }
        val session = AuthSession(service)

        session.signIn(creds)

        assertEquals(AuthState.Error("Invalid credentials"), session.state.value)
        assertFalse(session.isAuthenticated)
        assertNull(session.currentUser)
    }

    @Test
    fun `start restores an authenticated session when a user is persisted`() {
        val session = AuthSession(FakeAuthService(restored = testUser))
        session.start()
        assertEquals(AuthState.Authenticated(testUser), session.state.value)
    }

    @Test
    fun `start lands SignedOut when nothing is persisted`() {
        val session = AuthSession(FakeAuthService(restored = null))
        session.start()
        assertEquals(AuthState.SignedOut, session.state.value)
    }

    @Test
    fun `signOut clears state and calls the service logout`() = runTest {
        val service = FakeAuthService(restored = testUser)
        val session = AuthSession(service)
        session.start()

        session.state.test {
            assertEquals(AuthState.Authenticated(testUser), awaitItem())
            session.signOut()
            assertEquals(AuthState.SignedOut, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(service.loggedOut)
    }

    @Test
    fun `accessToken delegates to the service current token`() {
        val session = AuthSession(FakeAuthService(token = "TOK"))
        assertEquals("TOK", session.accessToken)
    }
}
