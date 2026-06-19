package com.vbwd.core.ui.dashboard

import com.vbwd.core.domain.AuthService
import com.vbwd.core.domain.AuthUser
import com.vbwd.core.domain.Credentials
import com.vbwd.core.networking.ApiClientConfig
import com.vbwd.core.networking.ApiError
import com.vbwd.core.plugins.InMemoryPluginManifestLoader
import com.vbwd.core.plugins.PluginHost
import com.vbwd.core.plugins.PluginManifest
import com.vbwd.core.session.AuthSession
import com.vbwd.core.testutil.FakeApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class StubAuthService(private val user: AuthUser?) : AuthService {
    override suspend fun login(credentials: Credentials): AuthUser = user!!
    override suspend fun logout() = Unit
    override fun restore(): AuthUser? = user
    override suspend fun fetchProfile(): AuthUser = user!!
    override suspend fun refreshAccessToken(): String = throw ApiError.NotImplemented("nope")
    override fun currentToken(): String? = null
}

class DashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun host(): PluginHost = PluginHost(
        api = FakeApiClient(),
        apiConfig = ApiClientConfig("http://x"),
        manifestLoader = InMemoryPluginManifestLoader(PluginManifest.EMPTY),
        plugins = emptyList(),
    )

    private fun session(user: AuthUser?): AuthSession =
        AuthSession(StubAuthService(user)).apply { start() }

    private val user = AuthUser(
        id = "u1",
        email = "test@example.com",
        name = "John Bach",
        userPermissions = listOf("subscription.tokens.view"),
    )

    @Test
    fun `derives name, email and initials from the user`() {
        val vm = DashboardViewModel(FakeApiClient(), session(user), host())
        assertEquals("John Bach", vm.userName)
        assertEquals("test@example.com", vm.userEmail)
        assertEquals("JB", vm.userInitials)
    }

    @Test
    fun `permission gates the token and invoice cards`() {
        val vm = DashboardViewModel(FakeApiClient(), session(user), host())
        assertTrue(vm.showTokenCard)
        assertFalse(vm.showInvoicesCard)
    }

    @Test
    fun `plugin Dashboard widgets are surfaced from the registry`() {
        val pluginHost = host()
        pluginHost.sdk.addComponent("DashboardX") {}
        pluginHost.sdk.addComponent("Other") {}
        val vm = DashboardViewModel(FakeApiClient(), session(user), pluginHost)
        assertEquals(listOf("DashboardX"), vm.pluginWidgets.map { it.first })
    }

    @Test
    fun `load tolerates failing endpoints and ends not-loading with empty cards`() =
        runTest(dispatcher.scheduler) {
            val vm = DashboardViewModel(FakeApiClient(), session(user), host())
            vm.load()
            advanceUntilIdle()
            assertFalse(vm.uiState.value.isLoading)
            assertTrue(vm.uiState.value.invoices.isEmpty())
        }
}
