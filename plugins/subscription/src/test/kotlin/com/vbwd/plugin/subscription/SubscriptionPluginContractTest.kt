package com.vbwd.plugin.subscription

import com.vbwd.core.events.AppEvents
import com.vbwd.core.events.DefaultEventBus
import com.vbwd.core.networking.ApiClientConfig
import com.vbwd.core.plugins.DefaultPlatformSdk
import com.vbwd.plugin.subscription.testutil.FakeApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SubscriptionPluginContractTest {
    private fun sdk(): DefaultPlatformSdk {
        val api = FakeApi()
        return DefaultPlatformSdk(api, ApiClientConfig("http://x"), DefaultEventBus(api))
    }

    @Test
    fun `install registers routes, the dashboard widget, menu, translations and the checkout source`() = runTest {
        val platform = sdk()
        SubscriptionPlugin().install(platform)

        assertEquals(
            setOf("/subscription", "/subscription/plans", "/subscription/all", "/subscription/addons"),
            platform.getRoutes().map { it.path }.toSet(),
        )
        assertTrue(platform.getComponents().containsKey("DashboardSubscription"))
        assertEquals(
            listOf("subscription", "subscription-plans", "subscription-addons"),
            platform.getMenuItems().map { it.id },
        )
        assertEquals("Subscription", platform.getTranslations()["en"]?.get("nav.subscription"))
        assertNotNull(platform.checkoutSources.get("subscription"))
        assertEquals(10, platform.checkoutSources.get("subscription")?.priority)
    }

    @Test
    fun `uninstall releases the auth-login subscription`() = runTest {
        val platform = sdk()
        val plugin = SubscriptionPlugin()
        plugin.install(platform)
        assertEquals(1, platform.events.listenerCount(AppEvents.AUTH_LOGIN))
        plugin.uninstall()
        assertEquals(0, platform.events.listenerCount(AppEvents.AUTH_LOGIN))
    }
}
