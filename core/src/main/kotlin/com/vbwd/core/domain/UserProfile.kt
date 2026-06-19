package com.vbwd.core.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User profile detail fields. Matches the backend `GET /user/profile`
 * `details` object shape (snake_case JSON keys).
 *
 * The backend may return `null` for any detail field (e.g. a freshly created
 * user has no `tax_number`). Each field has an empty-string default and the
 * shared codec coerces null → default (port of the iOS `decodeIfPresent ?? ""`).
 */
@Serializable
data class UserProfile(
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    val company: String = "",
    @SerialName("tax_number") val taxNumber: String = "",
    val phone: String = "",
    @SerialName("address_line_1") val addressLine1: String = "",
    @SerialName("address_line_2") val addressLine2: String = "",
    val city: String = "",
    @SerialName("postal_code") val postalCode: String = "",
    val country: String = "",
)

/** Response shape for `GET /user/profile`. */
@Serializable
data class ProfileResponse(
    val user: ProfileResponseUser? = null,
    val details: UserProfile? = null,
)

/** Minimal user info returned within the profile response. */
@Serializable
data class ProfileResponseUser(
    val id: String,
    val email: String,
)

/** Response shape for `PUT /user/details`. */
@Serializable
data class UpdateDetailsResponse(
    val details: UserProfile? = null,
)

/** Response shape for `POST /user/change-password`. */
@Serializable
data class ChangePasswordResponse(
    val success: Boolean? = null,
    val error: String? = null,
)
