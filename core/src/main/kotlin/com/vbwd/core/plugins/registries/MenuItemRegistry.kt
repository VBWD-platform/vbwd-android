package com.vbwd.core.plugins.registries

/**
 * A menu item contributed by a plugin. Port of the iOS `MenuItem` (badge /
 * badgeProvider are S67.2 — deferred; `icon` is a free-form string the shell
 * maps to an icon). `action` runs on tap; `routePath` (when set) navigates.
 */
data class MenuItem(
    val id: String,
    val icon: String,
    val title: String,
    val routePath: String? = null,
    val action: () -> Unit = {},
    val requiredPermission: String? = null,
    val order: Int = DEFAULT_ORDER,
    val section: String? = null,
) {
    companion object {
        const val DEFAULT_ORDER = 100
    }
}

/**
 * Registry for plugin-contributed menu items. Port of the iOS
 * `MenuItemRegistry`: stored by id, returned sorted by `order`.
 */
class MenuItemRegistry {
    private val items = LinkedHashMap<String, MenuItem>()

    fun add(item: MenuItem) {
        items[item.id] = item
    }

    fun remove(id: String) {
        items.remove(id)
    }

    fun get(id: String): MenuItem? = items[id]

    fun all(): List<MenuItem> = items.values.sortedBy { it.order }

    /** Items in a named section, sorted by order. */
    fun items(section: String): List<MenuItem> =
        items.values.filter { it.section == section }.sortedBy { it.order }

    /** Items with no section (the generic plugin-items area), sorted by order. */
    fun unsectionedItems(): List<MenuItem> =
        items.values.filter { it.section == null }.sortedBy { it.order }
}
