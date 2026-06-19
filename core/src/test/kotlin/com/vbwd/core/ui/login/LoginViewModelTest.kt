package com.vbwd.core.ui.login

import com.vbwd.core.domain.AuthService
import com.vbwd.core.domain.AuthUser
import com.vbwd.core.domain.Credentials
import com.vbwd.core.networking.ApiError
import com.vbwd.core.session.AuthSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private val testUser = AuthUser(id = "u1", email = "test@example.com")

private class FakeAuthService(var error: ApiError? = null) : AuthService {
    var loginCount = 0
    var lastCredentials: Credentials? = null

    override suspend fun login(credentials: Credentials): AuthUser {
        loginCount++
        lastCredentials = credentials
        error?.let { throw it }
        return testUser
    }

    override suspend fun logout() = Unit
    override fun restore(): AuthUser? = null
    override suspend fun fetchProfile(): AuthUser = testUser
    override suspend fun refreshAccessToken(): String = throw ApiError.NotImplemented("nope")
    override fun currentToken(): String? = null
}

class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(service: FakeAuthService) = LoginViewModel(AuthSession(service))

    @Test
    fun `canSubmit is false until both fields are filled`() {
        val vm = viewModel(FakeAuthService())
        assertFalse(vm.uiState.value.canSubmit)
        vm.onEmailChange("test@example.com")
        assertFalse(vm.uiState.value.canSubmit)
        vm.onPasswordChange("pw")
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `submit is guarded when input is incomplete`() = runTest(dispatcher.scheduler) {
        val service = FakeAuthService()
        val vm = viewModel(service)

        vm.submit()
        advanceUntilIdle()

        assertEquals(0, service.loginCount)
    }

    @Test
    fun `submit forwards the credentials and clears loading on success`() = runTest(dispatcher.scheduler) {
        val service = FakeAuthService()
        val vm = viewModel(service)
        vm.onEmailChange("test@example.com")
        vm.onPasswordChange("pw")

        vm.submit()
        advanceUntilIdle()

        assertEquals(1, service.loginCount)
        assertEquals(Credentials("test@example.com", "pw"), service.lastCredentials)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `submit surfaces the error message on failure`() = runTest(dispatcher.scheduler) {
        val service = FakeAuthService(error = ApiError.Http(401, "Invalid credentials"))
        val vm = viewModel(service)
        vm.onEmailChange("test@example.com")
        vm.onPasswordChange("bad")

        vm.submit()
        advanceUntilIdle()

        assertEquals("Invalid credentials", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }
}
