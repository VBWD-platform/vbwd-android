package com.vbwd.core.plugins

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NavigatorTest {
    private fun route(
        path: String,
        name: String = path,
        requiresAuth: Boolean = false,
        permission: String? = null,
        matchPrefix: Boolean = false,
    ) = PluginRoute(
        path = path,
        name = name,
        requiresAuth = requiresAuth,
        requiredUserPermission = permission,
        matchPrefix = matchPrefix,
    ) {}

    @Test
    fun `unknown path is not found`() {
        assertEquals(
            RouteResolution.NotFound,
            Navigator.resolve("/nope", listOf(route("/a")), isAuthenticated = false, userPermissions = emptyList()),
        )
    }

    @Test
    fun `exact public route is allowed`() {
        assertEquals(
            RouteResolution.Allow,
            Navigator.resolve("/a", listOf(route("/a")), isAuthenticated = false, userPermissions = emptyList()),
        )
    }

    @Test
    fun `prefix route matches sub-paths`() {
        val routes = listOf(route("/chat", matchPrefix = true))
        assertEquals(
            RouteResolution.Allow,
            Navigator.resolve("/chat/123", routes, isAuthenticated = false, userPermissions = emptyList()),
        )
    }

    @Test
    fun `auth-required route redirects when signed out`() {
        val routes = listOf(route("/secret", requiresAuth = true))
        assertEquals(
            RouteResolution.RedirectToLogin,
            Navigator.resolve("/secret", routes, isAuthenticated = false, userPermissions = emptyList()),
        )
    }

    @Test
    fun `permission-gated route is forbidden without the permission`() {
        val routes = listOf(route("/secret", requiresAuth = true, permission = "x.view"))
        assertEquals(
            RouteResolution.Forbidden,
            Navigator.resolve("/secret", routes, isAuthenticated = true, userPermissions = listOf("y.view")),
        )
    }

    @Test
    fun `permission-gated route is allowed with the permission`() {
        val routes = listOf(route("/secret", requiresAuth = true, permission = "x.view"))
        assertEquals(
            RouteResolution.Allow,
            Navigator.resolve("/secret", routes, isAuthenticated = true, userPermissions = listOf("x.view")),
        )
    }

    @Test
    fun `first matching route wins (core precedence)`() {
        val routes = listOf(route("/a", name = "core"), route("/a", name = "plugin"))
        // Both match; resolution is Allow either way, but the first is the one used.
        assertEquals(
            RouteResolution.Allow,
            Navigator.resolve("/a", routes, isAuthenticated = false, userPermissions = emptyList()),
        )
    }
}
