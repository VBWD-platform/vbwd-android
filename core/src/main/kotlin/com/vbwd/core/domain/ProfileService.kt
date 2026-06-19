package com.vbwd.core.domain

import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiError
import com.vbwd.core.networking.get
import com.vbwd.core.networking.post
import com.vbwd.core.networking.put
import kotlinx.serialization.Serializable

/**
 * Profile orchestration contract. Port of the web `profile.ts` store actions.
 * ViewModels depend on this interface, never on the concrete service (DIP).
 */
interface ProfileService {
    /** GET /user/profile — fetches the full profile. */
    suspend fun fetchProfile(): UserProfile

    /** PUT /user/details — updates personal info + address fields. */
    suspend fun updateDetails(profile: UserProfile): UserProfile

    /** POST /user/change-password — changes the user password. */
    suspend fun changePassword(current: String, new: String)
}

/** Default [ProfileService]. Depends only on injected interfaces (DIP). */
class DefaultProfileService(
    private val client: ApiClient,
    private val endpoints: ProfileEndpoints = ProfileEndpoints(),
) : ProfileService {

    @Serializable
    private data class ChangePasswordBody(
        val currentPassword: String,
        val newPassword: String,
    )

    override suspend fun fetchProfile(): UserProfile {
        val response: ProfileResponse = client.get(endpoints.profile)
        return response.details
            ?: throw ApiError.Decoding("Missing details in profile response")
    }

    override suspend fun updateDetails(profile: UserProfile): UserProfile {
        val response: UpdateDetailsResponse = client.put(endpoints.updateDetails, profile)
        return response.details
            ?: throw ApiError.Decoding("Missing details in update response")
    }

    override suspend fun changePassword(current: String, new: String) {
        client.post<ChangePasswordBody, ChangePasswordResponse>(
            endpoints.changePassword,
            ChangePasswordBody(currentPassword = current, newPassword = new),
        )
    }
}
