package com.vbwd.core.networking

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApiClientConfigTest {
    @Test
    fun `default headers include json content-type and android platform`() {
        val config = ApiClientConfig(baseUrl = "http://x/api")
        assertEquals("application/json", config.headers["Content-Type"])
        assertEquals("android", config.headers["X-Client-Platform"])
    }

    @Test
    fun `caller headers override the defaults`() {
        val config = ApiClientConfig(
            baseUrl = "http://x/api",
            extraHeaders = mapOf("Content-Type" to "text/plain", "X-Extra" to "1"),
        )
        assertEquals("text/plain", config.headers["Content-Type"])
        assertEquals("1", config.headers["X-Extra"])
    }

    @Test
    fun `default timeout matches web parity`() {
        assertEquals(30_000L, ApiClientConfig(baseUrl = "http://x").timeoutMillis)
    }
}
