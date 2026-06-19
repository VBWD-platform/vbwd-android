package com.vbwd.core.domain

/**
 * Profile endpoint paths. Matches the web `Profile.vue` API calls.
 * All paths are overridable (OCP).
 */
data class ProfileEndpoints(
    val profile: String = "/user/profile",
    val updateDetails: String = "/user/details",
    val changePassword: String = "/user/change-password",
)
