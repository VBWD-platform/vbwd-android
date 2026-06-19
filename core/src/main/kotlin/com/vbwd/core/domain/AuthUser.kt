package com.vbwd.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Access level (web `AccessLevel` / `UserAccessLevel` — same shape). */
@Serializable
data class AccessLevel(
    val id: String,
    val slug: String,
    val name: String,
)

/**
 * Authenticated user. Port of the web/iOS `AuthUser` with snake_case mapping,
 * matching the live backend `/auth/login` `user` object.
 */
@Serializable
data class AuthUser(
    val id: String,
    val email: String,
    val name: String? = null,
    val role: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean? = null,
    @SerialName("access_levels") val accessLevels: List<AccessLevel>? = null,
    val permissions: List<String>? = null,
    @SerialName("user_access_levels") val userAccessLevels: List<AccessLevel>? = null,
    @SerialName("user_permissions") val userPermissions: List<String>? = null,
)
