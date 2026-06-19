package com.vbwd.core.networking

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ApiErrorTest {
    @Test
    fun `fromResponse prefers the body 'error' field`() {
        val error = ApiError.fromResponse(400, """{"error":"bad input"}""", "Bad Request")
        assertEquals(400, error.status)
        assertEquals("bad input", error.message)
    }

    @Test
    fun `fromResponse falls back to 'message' then status text`() {
        assertEquals("nope", ApiError.fromResponse(400, """{"message":"nope"}""", "Bad Request").message)
        assertEquals("Bad Request", ApiError.fromResponse(400, """{}""", "Bad Request").message)
        assertEquals("Bad Request", ApiError.fromResponse(400, "", "Bad Request").message)
    }

    @Test
    fun `messageFromBody returns null for non-json or empty bodies`() {
        assertNull(ApiError.messageFromBody(""))
        assertNull(ApiError.messageFromBody("not json"))
        assertNull(ApiError.messageFromBody("""{"other":"x"}"""))
    }
}
