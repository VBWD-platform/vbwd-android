package com.vbwd.core.networking

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OkHttpApiClientTest {
    @Serializable
    data class Foo(val id: String)

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    private fun newClient(tokenProvider: AuthTokenProvider = MutableTokenProvider()) =
        OkHttpApiClient(
            config = ApiClientConfig(
                baseUrl = server.url("/").toString().trimEnd('/'),
                logging = ApiTrafficLogger.off(),
            ),
            tokenProvider = tokenProvider,
        )

    @Test
    fun `get decodes a 200 json body and hits the right path`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"x"}"""))
        val result: Foo = newClient().get("/foo")
        assertEquals("x", result.id)
        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/foo", recorded.path)
    }

    @Test
    fun `bearer token is injected when present`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"x"}"""))
        newClient(MutableTokenProvider("abc")).get<Foo>("/foo")
        assertEquals("Bearer abc", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `401 emits tokenExpired and throws Http`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"expired"}"""))
        var expired = false
        val client = newClient().apply { on(ApiEvent.TOKEN_EXPIRED) { expired = true } }
        val error = runCatching { client.get<Foo>("/foo") }.exceptionOrNull()
        assertInstanceOf(ApiError.Http::class.java, error)
        assertEquals(401, (error as ApiError.Http).status)
        assertTrue(expired)
    }

    @Test
    fun `non-2xx maps to Http with the body message`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"boom"}"""))
        val error = runCatching { newClient().get<Foo>("/foo") }.exceptionOrNull()
        assertInstanceOf(ApiError.Http::class.java, error)
        assertEquals(500, (error as ApiError.Http).status)
        assertEquals("boom", error.message)
    }

    @Test
    fun `malformed json maps to Decoding`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{not json"))
        val error = runCatching { newClient().get<Foo>("/foo") }.exceptionOrNull()
        assertInstanceOf(ApiError.Decoding::class.java, error)
    }

    @Test
    fun `EmptyResponse endpoints need no body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))
        val result: EmptyResponse = newClient().post("/logout")
        assertEquals(EmptyResponse(), result)
    }
}
