package com.vbwd.plugin.tokenpayment

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.EmptyResponse
import com.vbwd.core.networking.HttpMethod
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TokenPaymentServiceTest {
    private val client = mockk<ApiClient>(relaxed = true)
    private val service = TokenPaymentService(client)

    @Test
    fun `fetchQuote decodes the quote`() = runTest {
        coEvery { client.request<TokenQuote>(HttpMethod.GET, any(), any(), any()) } returns
            TokenQuote(available = true, tokensNeeded = 600.0, balance = 1000.0)
        val quote = service.fetchQuote(29.99, "USD")
        assertEquals(true, quote.available)
        assertEquals(600.0, quote.tokensNeeded)
    }

    @Test
    fun `payWithTokens posts to the pay endpoint`() = runTest {
        coEvery {
            client.request<EmptyResponse>(HttpMethod.POST, "/plugins/token-payment/invoices/inv1/pay", any(), any())
        } returns EmptyResponse()
        service.payWithTokens("inv1")
        coVerify {
            client.request<EmptyResponse>(HttpMethod.POST, "/plugins/token-payment/invoices/inv1/pay", any(), any())
        }
    }
}
