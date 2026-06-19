package com.vbwd.plugin.cms.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PostsBrowserAuthBootstrapTest {
    @Test
    fun `no token and no user yields an empty (no-op) script`() {
        assertEquals("", PostsBrowserAuthBootstrap.javaScriptSource(token = null, userJson = null))
        assertEquals("", PostsBrowserAuthBootstrap.javaScriptSource(token = "", userJson = ""))
    }

    @Test
    fun `a token seeds the token and isAuthenticated keys`() {
        val script = PostsBrowserAuthBootstrap.javaScriptSource(token = "abc", userJson = null)
        assertTrue(script.contains("localStorage.setItem('token', 'abc');"))
        assertTrue(script.contains("localStorage.setItem('isAuthenticated', 'true');"))
    }

    @Test
    fun `jsLiteral escapes single quotes and backslashes`() {
        assertEquals("'a\\'b'", PostsBrowserAuthBootstrap.jsLiteral("a'b"))
        assertEquals("'a\\\\b'", PostsBrowserAuthBootstrap.jsLiteral("a\\b"))
    }
}
