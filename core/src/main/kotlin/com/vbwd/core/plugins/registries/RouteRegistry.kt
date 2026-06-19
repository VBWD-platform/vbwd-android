package com.vbwd.core.plugins.registries

import com.vbwd.core.plugins.PluginRoute

/** Registry error for duplicate contributions (route path/name, store id). */
sealed class RegistryError(message: String) : Exception(message) {
    data class DuplicateRoutePath(val path: String) :
        RegistryError("duplicate route path: $path")
    data class DuplicateRouteName(val name: String) :
        RegistryError("duplicate route name: $name")
    data class DuplicateStoreId(val id: String) :
        RegistryError("duplicate store id: $id")
}

/**
 * Collects plugin-contributed routes. Port of the iOS `RouteRegistry`: storage
 * only (SRP), unique path **and** name enforced.
 */
class RouteRegistry {
    private val routes = mutableListOf<PluginRoute>()

    fun add(route: PluginRoute) {
        if (routes.any { it.path == route.path }) {
            throw RegistryError.DuplicateRoutePath(route.path)
        }
        if (routes.any { it.name == route.name }) {
            throw RegistryError.DuplicateRouteName(route.name)
        }
        routes.add(route)
    }

    fun all(): List<PluginRoute> = routes.toList()
}
