package com.vbwd.core.domain

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiError
import com.vbwd.core.networking.ApiJson
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.HttpMethod
import com.vbwd.core.persistence.InMemoryTokenStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DefaultAuthServiceTest {
    private val store = InMemoryTokenStore()
    private val client = mockk<ApiClient>(relaxed = true)
    private val endpoints = AuthEndpoints()
    private val service = DefaultAuthService(client, store, endpoints)

    private val user = AuthUser(
        id = "u1",
        email = "test@example.com",
        name = "John Bach",
        isAdmin = false,
        userPermissions = listOf("user.profile.view"),
    )

    private fun stubLogin(response: LoginResponse) {
        coEvery {
            client.request<LoginResponse>(HttpMethod.POST, endpoints.login, any(), any())
        } returns response
    }

    @Test
    fun `login persists token, user and returns the user`() = runTest {
        stubLogin(LoginResponse(success = true, token = "TOK", refreshToken = "RFRSH", user = user))

        val result = service.login(Credentials("test@example.com", "pw"))

        assertEquals(user, result)
        assertEquals("TOK", store.loadToken())
        assertEquals("RFRSH", store.loadRefreshToken())
        val persisted = ApiJson.instance.decodeFromString(
            AuthUser.serializer(),
            store.loadUser()!!.decodeToString(),
        )
        assertEquals(user, persisted)
        verify { client.setToken("TOK") }
    }

    @Test
    fun `login with success=false surfaces Http 401 and persists nothing`() = runTest {
        stubLogin(LoginResponse(success = false, error = "Invalid credentials"))

        val error = runCatching { service.login(Credentials("x", "y")) }.exceptionOrNull()

        assertInstanceOf(ApiError.Http::class.java, error)
        assertEquals(401, (error as ApiError.Http).status)
        assertEquals("Invalid credentials", error.message)
        assertNull(store.loadToken())
        assertNull(store.loadUser())
    }

    @Test
    fun `login propagates a transport-level ApiError without persisting`() = runTest {
        coEvery {
            client.request<LoginResponse>(HttpMethod.POST, endpoints.login, any(), any())
        } throws ApiError.Http(401, "Unauthorized")

        val error = runCatching { service.login(Credentials("x", "y")) }.exceptionOrNull()

        assertInstanceOf(ApiError.Http::class.java, error)
        assertNull(store.loadToken())
    }

    @Test
    fun `restore returns the user and re-arms the client token when present`() = runTest {
        store.saveToken("TOK")
        store.saveUser(ApiJson.instance.encodeToString(AuthUser.serializer(), user).toByteArray())

        val restored = service.restore()

        assertEquals(user, restored)
        verify { client.setToken("TOK") }
    }

    @Test
    fun `restore returns null when no token is stored`() {
        assertNull(service.restore())
        verify(exactly = 0) { client.setToken(any()) }
    }

    @Test
    fun `restore returns null when token is present but user blob is missing`() {
        store.saveToken("TOK")
        assertNull(service.restore())
    }

    @Test
    fun `logout clears the store and the client token`() = runTest {
        store.saveToken("TOK")
        store.saveUser("x".toByteArray())

        service.logout()

        assertNull(store.loadToken())
        assertNull(store.loadUser())
        verify { client.setToken(null) }
    }

    @Test
    fun `logout still clears locally when the server logout call fails`() = runTest {
        store.saveToken("TOK")
        coEvery {
            client.request<EmptyResponse>(HttpMethod.POST, endpoints.logout, any(), any())
        } throws ApiError.Transport("offline")

        service.logout()

        assertNull(store.loadToken())
        verify { client.setToken(null) }
    }

    @Test
    fun `refreshAccessToken is not implemented in this sprint`() = runTest {
        val error = runCatching { service.refreshAccessToken() }.exceptionOrNull()
        assertInstanceOf(ApiError.NotImplemented::class.java, error)
    }

    @Test
    fun `currentToken returns the persisted access token`() {
        store.saveToken("TOK")
        assertEquals("TOK", service.currentToken())
    }

    @Test
    fun `fetchProfile delegates to the configured profile endpoint`() = runTest {
        coEvery {
            client.request<AuthUser>(HttpMethod.GET, endpoints.profile, any(), any())
        } returns user

        assertEquals(user, service.fetchProfile())
        coVerify { client.request<AuthUser>(HttpMethod.GET, endpoints.profile, any(), any()) }
    }
}
