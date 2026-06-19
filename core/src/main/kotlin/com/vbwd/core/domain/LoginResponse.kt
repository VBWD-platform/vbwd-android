package com.vbwd.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `/auth/login` response. Matches the live backend shape:
 * `{ error, success, token, user, user_id }` (web `LoginResponse` + extras).
 */
@Serializable
data class LoginResponse(
    val success: Boolean? = null,
    val token: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: AuthUser? = null,
    @SerialName("user_id") val userId: String? = null,
    val error: String? = null,
)
