package com.vbwd.core.domain

import kotlinx.serialization.Serializable

/** Login input. Encodes `email` / `password` (web `LoginCredentials`). */
@Serializable
data class Credentials(
    val email: String,
    val password: String,
)
