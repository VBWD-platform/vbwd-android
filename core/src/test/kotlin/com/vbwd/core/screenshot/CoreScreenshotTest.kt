package com.vbwd.core.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.vbwd.core.cart.Cart
import com.vbwd.core.cart.CartItem
import com.vbwd.core.cart.CheckoutContext
import com.vbwd.core.checkout.CheckoutSourceRegistry
import com.vbwd.core.domain.AuthService
import com.vbwd.core.domain.AuthUser
import com.vbwd.core.domain.Credentials
import com.vbwd.core.domain.ProfileService
import com.vbwd.core.domain.UserProfile
import com.vbwd.core.networking.ApiClientConfig
import com.vbwd.core.networking.ApiError
import com.vbwd.core.plugins.InMemoryPluginManifestLoader
import com.vbwd.core.plugins.PluginHost
import com.vbwd.core.plugins.PluginManifest
import com.vbwd.core.session.AuthSession
import com.vbwd.core.testutil.FakeApiClient
import com.vbwd.core.testutil.FakeCheckoutSource
import com.vbwd.core.theme.InMemoryThemeStore
import com.vbwd.core.theme.ThemeManager
import com.vbwd.core.theme.ThemeRegistry
import com.vbwd.core.theme.VbwdTheme
import com.vbwd.core.ui.checkout.CheckoutScreen
import com.vbwd.core.ui.checkout.CheckoutViewModel
import com.vbwd.core.ui.dashboard.DashboardScreen
import com.vbwd.core.ui.dashboard.DashboardViewModel
import com.vbwd.core.ui.login.LoginScreen
import com.vbwd.core.ui.login.LoginViewModel
import com.vbwd.core.ui.profile.ProfileEditScreen
import com.vbwd.core.ui.profile.ProfileViewModel
import com.vbwd.core.ui.settings.SettingsScreen
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val demoUser = AuthUser(
    id = "u1",
    email = "ada@example.com",
    name = "Ada Lovelace",
    userPermissions = listOf("subscription.tokens.view", "subscription.invoices.view"),
)

private class DemoAuthService(private val user: AuthUser?) : AuthService {
    override suspend fun login(credentials: Credentials): AuthUser = user!!
    override suspend fun logout() = Unit
    override fun restore(): AuthUser? = user
    override suspend fun fetchProfile(): AuthUser = user!!
    override suspend fun refreshAccessToken(): String = throw ApiError.NotImplemented("nope")
    override fun currentToken(): String? = "TOKEN"
}

private class DemoProfileService : ProfileService {
    override suspend fun fetchProfile() = UserProfile(
        firstName = "Ada",
        lastName = "Lovelace",
        company = "Analytical Engines Ltd",
        city = "London",
        country = "UK",
    )
    override suspend fun updateDetails(profile: UserProfile) = profile
    override suspend fun changePassword(current: String, new: String) = Unit
}

/**
 * JVM (Robolectric) screenshot tests of the real core screens via Roborazzi —
 * no emulator. Record with `./gradlew :core:testDebugUnitTest -Proborazzi.test.record=true`
 * (or `recordRoborazziDebug`); PNGs land in `core/screenshots/`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class CoreScreenshotTest {
    private val themeManager = ThemeManager(ThemeRegistry(), InMemoryThemeStore())

    private fun capture(name: String, content: @Composable () -> Unit) {
        captureRoboImage("screenshots/$name.png") {
            VbwdTheme(themeManager) {
                Surface(modifier = Modifier.fillMaxSize()) { content() }
            }
        }
    }

    @Test
    fun login() = capture("login") {
        LoginScreen(LoginViewModel(AuthSession(DemoAuthService(demoUser))))
    }

    @Test
    fun settings() = capture("settings") {
        SettingsScreen(themeManager)
    }

    @Test
    fun profile() = capture("profile") {
        ProfileEditScreen(ProfileViewModel(DemoProfileService()))
    }

    @Test
    fun dashboard() = capture("dashboard") {
        val api = FakeApiClient()
        val host = PluginHost(
            api = api,
            apiConfig = ApiClientConfig("http://x"),
            manifestLoader = InMemoryPluginManifestLoader(PluginManifest.EMPTY),
            plugins = emptyList(),
        )
        host.sdk.addComponent("DashboardDemo") {
            Box(modifier = Modifier.padding(16.dp)) { Text("1,250 tokens") }
        }
        val session = AuthSession(DemoAuthService(demoUser)).apply { start() }
        DashboardScreen(DashboardViewModel(api, session, host))
    }

    @Test
    fun checkout() = capture("checkout") {
        val item = CartItem(type = "token_bundle", id = "b1", name = "Starter — 600 tokens", price = 29.99)
        val cart = Cart().apply { add(item) }
        val registry = CheckoutSourceRegistry().apply {
            register(FakeCheckoutSource("token_bundle", items = listOf(item)))
        }
        val viewModel = CheckoutViewModel(
            api = FakeApiClient(),
            context = CheckoutContext(source = "token_bundle", isCart = false),
            cart = cart,
            checkoutSources = registry,
        )
        CheckoutScreen(viewModel)
    }
}
