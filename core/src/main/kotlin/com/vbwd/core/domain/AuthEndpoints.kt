package com.vbwd.core.domain

/**
 * Auth endpoint paths. Defaults match the web `auth.ts` configuration, except
 * `profile`: the live backend has **no `/auth/me`** (404); the real profile
 * route is `/user/profile` with a `{details,user}` shape, so the dashboard user
 * is sourced from the login response. All paths are overridable (OCP).
 */
data class AuthEndpoints(
    val login: String = "/auth/login",
    val logout: String = "/auth/logout",
    val refresh: String = "/auth/refresh",
    val profile: String = "/user/profile",
)
