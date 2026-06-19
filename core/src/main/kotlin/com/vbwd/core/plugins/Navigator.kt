package com.vbwd.core.plugins

import com.vbwd.core.domain.PermissionEvaluator

/** Pure route-resolution outcome. Port of the iOS `RouteResolution`. */
sealed interface RouteResolution {
    data object Allow : RouteResolution
    data object RedirectToLogin : RouteResolution
    data object Forbidden : RouteResolution
    data object NotFound : RouteResolution
}

/**
 * Pure route-guard decision (auth + permission + 404-last). Port of the iOS
 * `Navigator` / the web `router/index.ts` guards. Extracted from the shell so
 * it is unit-testable without rendering Compose (SRP, like `RootRouter`).
 */
object Navigator {
    /**
     * First route matching by path wins (callers placing core routes before
     * plugin routes get core precedence — web router order). Unknown path →
     * [RouteResolution.NotFound].
     */
    @Suppress("ReturnCount")
    fun resolve(
        path: String,
        routes: List<PluginRoute>,
        isAuthenticated: Boolean,
        userPermissions: List<String>,
        evaluator: PermissionEvaluator = PermissionEvaluator(),
    ): RouteResolution {
        val route = routes.firstOrNull { it.path == path || (it.matchPrefix && path.startsWith(it.path)) }
            ?: return RouteResolution.NotFound

        if (route.requiresAuth && !isAuthenticated) return RouteResolution.RedirectToLogin

        val needed = route.requiredUserPermission
        if (needed != null && !evaluator.has(needed, userPermissions)) {
            return RouteResolution.Forbidden
        }
        return RouteResolution.Allow
    }
}
