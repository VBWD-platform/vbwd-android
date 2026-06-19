package com.vbwd.plugin.subscription.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.vbwd.core.cart.Cart
import com.vbwd.core.checkout.CheckoutSourceRegistry
import com.vbwd.core.theme.InMemoryThemeStore
import com.vbwd.core.theme.ThemeManager
import com.vbwd.core.theme.ThemeRegistry
import com.vbwd.core.theme.VbwdTheme
import com.vbwd.core.ui.checkout.CheckoutViewModel
import com.vbwd.plugin.subscription.domain.TarifPlan
import com.vbwd.plugin.subscription.testutil.FakeApi
import com.vbwd.plugin.subscription.testutil.FakeSubscriptionService
import com.vbwd.plugin.subscription.ui.TarifPlansScreen
import com.vbwd.plugin.subscription.ui.TarifPlansViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class SubscriptionScreenshotTest {
    private val theme = ThemeManager(ThemeRegistry(), InMemoryThemeStore())

    @Test
    fun tarifPlans() {
        val plan = TarifPlan(
            id = "p1",
            name = "Pro",
            displayPrice = 29.99,
            displayCurrency = "USD",
            billingPeriod = "month",
            features = listOf("Unlimited projects", "Priority support"),
        )
        val viewModel = TarifPlansViewModel(FakeSubscriptionService(plan = plan), Cart())
        captureRoboImage("screenshots/tarif_plans.png") {
            VbwdTheme(theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TarifPlansScreen(
                        viewModel = viewModel,
                        checkoutFactory = { context ->
                            CheckoutViewModel(FakeApi(), context, Cart(), CheckoutSourceRegistry())
                        },
                    )
                }
            }
        }
    }
}
