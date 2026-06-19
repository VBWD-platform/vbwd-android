package com.vbwd.plugin.subscription.domain

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiError
import com.vbwd.core.networking.HttpMethod
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class SubscriptionServiceTest {
    private val client = mockk<ApiClient>(relaxed = true)
    private val service = DefaultSubscriptionService(client)

    @Test
    fun `fetchPlans appends the currency query`() = runTest {
        coEvery { client.request<PlansResponse>(HttpMethod.GET, any(), any(), any()) } returns
            PlansResponse(plans = listOf(TarifPlan(id = "p1", name = "Pro")))

        assertEquals(1, service.fetchPlans("USD").size)
        coVerify { client.request<PlansResponse>(HttpMethod.GET, "/tarif-plans?currency=USD", any(), any()) }
    }

    @Test
    fun `fetchActiveSub unwraps the subscription`() = runTest {
        coEvery {
            client.request<SubscriptionResponse>(HttpMethod.GET, "/user/subscriptions/active", any(), any())
        } returns SubscriptionResponse(subscription = Subscription(id = "s1"))
        assertEquals("s1", service.fetchActiveSub()?.id)
    }

    @Test
    fun `fetchPlan reads the plan from the top level`() = runTest {
        coEvery { client.request<TarifPlan>(HttpMethod.GET, "/tarif-plans/pro", any(), any()) } returns
            TarifPlan(id = "p1", name = "Pro")
        assertEquals("p1", service.fetchPlan("pro").id)
    }

    @Test
    fun `fetchUserAddOn raises when the body is empty (no false success)`() = runTest {
        coEvery { client.request<UserAddOnResponse>(HttpMethod.GET, "/user/addons/a1", any(), any()) } returns
            UserAddOnResponse(addonSubscription = null)
        val error = runCatching { service.fetchUserAddOn("a1") }.exceptionOrNull()
        assertInstanceOf(ApiError.Http::class.java, error)
        assertEquals(404, (error as ApiError.Http).status)
    }

    @Test
    fun `cancelSubscription posts to the cancel endpoint`() = runTest {
        coEvery {
            client.request<CancelResponse>(HttpMethod.POST, "/user/subscriptions/s1/cancel", any(), any())
        } returns CancelResponse()
        service.cancelSubscription("s1")
        coVerify { client.request<CancelResponse>(HttpMethod.POST, "/user/subscriptions/s1/cancel", any(), any()) }
    }
}
