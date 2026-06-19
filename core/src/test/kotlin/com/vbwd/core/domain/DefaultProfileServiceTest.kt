package com.vbwd.core.domain

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

class DefaultProfileServiceTest {
    private val client = mockk<ApiClient>(relaxed = true)
    private val endpoints = ProfileEndpoints()
    private val service = DefaultProfileService(client, endpoints)

    private val profile = UserProfile(firstName = "Ada", lastName = "Lovelace", city = "London")

    @Test
    fun `fetchProfile unwraps the details from the profile response`() = runTest {
        coEvery {
            client.request<ProfileResponse>(HttpMethod.GET, endpoints.profile, any(), any())
        } returns ProfileResponse(details = profile)

        assertEquals(profile, service.fetchProfile())
    }

    @Test
    fun `fetchProfile throws Decoding when details are missing`() = runTest {
        coEvery {
            client.request<ProfileResponse>(HttpMethod.GET, endpoints.profile, any(), any())
        } returns ProfileResponse(details = null)

        val error = runCatching { service.fetchProfile() }.exceptionOrNull()
        assertInstanceOf(ApiError.Decoding::class.java, error)
    }

    @Test
    fun `updateDetails returns the updated details`() = runTest {
        val updated = profile.copy(city = "Paris")
        coEvery {
            client.request<UpdateDetailsResponse>(HttpMethod.PUT, endpoints.updateDetails, any(), any())
        } returns UpdateDetailsResponse(details = updated)

        assertEquals(updated, service.updateDetails(profile))
    }

    @Test
    fun `updateDetails throws Decoding when details are missing`() = runTest {
        coEvery {
            client.request<UpdateDetailsResponse>(HttpMethod.PUT, endpoints.updateDetails, any(), any())
        } returns UpdateDetailsResponse(details = null)

        val error = runCatching { service.updateDetails(profile) }.exceptionOrNull()
        assertInstanceOf(ApiError.Decoding::class.java, error)
    }

    @Test
    fun `changePassword posts to the change-password endpoint`() = runTest {
        coEvery {
            client.request<ChangePasswordResponse>(HttpMethod.POST, endpoints.changePassword, any(), any())
        } returns ChangePasswordResponse(success = true)

        service.changePassword(current = "old", new = "new")

        coVerify {
            client.request<ChangePasswordResponse>(HttpMethod.POST, endpoints.changePassword, any(), any())
        }
    }
}
