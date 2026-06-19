package com.vbwd.core.ui.profile

import com.vbwd.core.domain.ProfileService
import com.vbwd.core.domain.UserProfile
import com.vbwd.core.networking.ApiError
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

private class FakeProfileService(
    var profile: UserProfile = UserProfile(firstName = "Ada", lastName = "Lovelace"),
    var saveError: ApiError? = null,
    var changeError: ApiError? = null,
) : ProfileService {
    var changedTo: Pair<String, String>? = null
    override suspend fun fetchProfile(): UserProfile = profile
    override suspend fun updateDetails(profile: UserProfile): UserProfile {
        saveError?.let { throw it }
        this.profile = profile
        return profile
    }
    override suspend fun changePassword(current: String, new: String) {
        changeError?.let { throw it }
        changedTo = current to new
    }
}

class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `load populates the form and leaves it pristine`() = runTest(dispatcher.scheduler) {
        val vm = ProfileViewModel(FakeProfileService())
        vm.load()
        advanceUntilIdle()
        assertEquals("Ada", vm.form.value.firstName)
        assertFalse(vm.uiState.value.isDirty)
    }

    @Test
    fun `editing marks dirty and save persists then clears dirty`() = runTest(dispatcher.scheduler) {
        val service = FakeProfileService()
        val vm = ProfileViewModel(service)
        vm.load()
        advanceUntilIdle()

        vm.updateForm { it.copy(city = "London") }
        assertTrue(vm.uiState.value.isDirty)

        vm.save()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isDirty)
        assertEquals("London", service.profile.city)
        assertEquals("Profile updated successfully", vm.uiState.value.successMessage)
    }

    @Test
    fun `save error surfaces a message`() = runTest(dispatcher.scheduler) {
        val vm = ProfileViewModel(FakeProfileService(saveError = ApiError.Http(400, "nope")))
        vm.load()
        advanceUntilIdle()
        vm.updateForm { it.copy(city = "x") }
        vm.save()
        advanceUntilIdle()
        assertEquals("nope", vm.uiState.value.errorMessage)
    }

    @Test
    fun `changePassword rejects a mismatch`() = runTest(dispatcher.scheduler) {
        val service = FakeProfileService()
        val vm = ProfileViewModel(service)
        vm.updatePassword { it.copy(currentPassword = "old", newPassword = "abcd1234", confirmPassword = "zzzz") }
        vm.changePassword()
        advanceUntilIdle()
        assertEquals("New passwords do not match", vm.uiState.value.passwordError)
        assertEquals(null, service.changedTo)
    }

    @Test
    fun `changePassword succeeds and clears the fields`() = runTest(dispatcher.scheduler) {
        val service = FakeProfileService()
        val vm = ProfileViewModel(service)
        vm.updatePassword { it.copy(currentPassword = "old", newPassword = "abcd1234", confirmPassword = "abcd1234") }
        vm.changePassword()
        advanceUntilIdle()
        assertEquals("old" to "abcd1234", service.changedTo)
        assertEquals("", vm.password.value.newPassword)
        assertEquals("Password changed successfully", vm.uiState.value.passwordSuccess)
    }
}
