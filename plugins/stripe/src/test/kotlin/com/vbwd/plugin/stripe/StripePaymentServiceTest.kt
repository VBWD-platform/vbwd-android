package com.vbwd.plugin.stripe

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.HttpMethod
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StripePaymentServiceTest {
    private val client = mockk<ApiClient>(relaxed = true)
    private val service = StripePaymentService(client)

    @Test
    fun `createSession returns the session url and id`() = runTest {
        coEvery {
            client.request<StripeSessionResponse>(HttpMethod.POST, "/plugins/stripe/create-session", any(), any())
        } returns StripeSessionResponse(sessionUrl = "https://pay/x", sessionId = "cs_1")
        val response = service.createSession("inv1")
        assertEquals("https://pay/x", response.sessionUrl)
        assertEquals("cs_1", response.sessionId)
    }

    @Test
    fun `checkStatus reads the session status`() = runTest {
        coEvery {
            client.request<StripeStatusResponse>(HttpMethod.GET, "/plugins/stripe/session-status/cs_1", any(), any())
        } returns StripeStatusResponse(status = "paid")
        assertEquals("paid", service.checkStatus("cs_1").status)
    }
}
