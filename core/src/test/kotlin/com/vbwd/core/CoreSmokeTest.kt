package com.vbwd.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A01.0 smoke test — its only job is to prove the JUnit5 test runner compiles
 * and runs for `:core`, making the Red/Green loop real from day one.
 */
class CoreSmokeTest {
    @Test
    fun `test runner works`() {
        assertTrue(true)
    }

    @Test
    fun `api contract version is v1`() {
        assertEquals("v1", Vbwd.API_CONTRACT_VERSION)
    }
}
