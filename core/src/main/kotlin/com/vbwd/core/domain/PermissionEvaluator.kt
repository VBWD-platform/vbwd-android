package com.vbwd.core.domain

/**
 * Permission wildcard matcher. Exact port of the web `hasUserPermission` /
 * `hasAnyUserPermission` rules from `auth.ts`:
 *   - `"*"`       grants everything
 *   - exact match grants
 *   - `"shop.*"`  grants any permission with prefix `"shop."`
 *
 * Single definition of the rule (DRY); takes `List<String>`, not `AuthUser`
 * (ISP).
 */
class PermissionEvaluator {
    fun has(permission: String, granted: List<String>): Boolean {
        if (granted.contains("*")) return true
        if (granted.contains(permission)) return true
        return granted.any { g ->
            g.endsWith(".*") && permission.startsWith(g.dropLast(1))
        }
    }

    fun hasAny(permissions: List<String>, granted: List<String>): Boolean {
        if (granted.contains("*")) return true
        return permissions.any { has(it, granted) }
    }
}
