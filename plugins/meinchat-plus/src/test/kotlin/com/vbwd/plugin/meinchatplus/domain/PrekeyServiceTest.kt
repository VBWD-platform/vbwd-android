package com.vbwd.plugin.meinchatplus.domain

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.HttpMethod
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrekeyServiceTest {
    private val client = mockk<ApiClient>(relaxed = true)
    private val service = DefaultPrekeyService(client)

    private fun stubStatus(status: PrekeyStatus) {
        coEvery { client.request<PrekeyStatus>(HttpMethod.GET, "/me/prekeys/status", any(), any()) } returns status
    }

    @Test
    fun `needsRefill honours the server low-water mark`() = runTest {
        stubStatus(PrekeyStatus(oneTimeRemaining = 3, oneTimeCapacity = 100, lowWaterMark = 5))
        assertTrue(service.needsRefill())
    }

    @Test
    fun `needsRefill is false above the mark`() = runTest {
        stubStatus(PrekeyStatus(oneTimeRemaining = 50, oneTimeCapacity = 100, lowWaterMark = 5))
        assertFalse(service.needsRefill())
    }

    @Test
    fun `needsRefill falls back to 20 percent when no mark is supplied`() = runTest {
        stubStatus(PrekeyStatus(oneTimeRemaining = 15, oneTimeCapacity = 100, lowWaterMark = null))
        assertTrue(service.needsRefill()) // 15 <= 20
    }

    @Test
    fun `fetchStatus returns the pool status`() = runTest {
        stubStatus(PrekeyStatus(oneTimeRemaining = 97, oneTimeCapacity = 100))
        assertEquals(97, service.fetchStatus().oneTimeRemaining)
    }
}
