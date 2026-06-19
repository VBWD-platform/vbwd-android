package com.vbwd.plugin.example

import com.vbwd.core.plugins.registries.MenuItem

/**
 * Menu-item factory for the Example plugin (SRP — kept out of the plugin's
 * composition root). Port of the iOS `ExampleMenuItems`.
 */
object ExampleMenuItems {
    private const val ORDER = 50

    fun all(): List<MenuItem> = listOf(
        MenuItem(
            id = "example",
            icon = "star",
            title = "Example",
            routePath = "/example",
            order = ORDER,
        ),
    )
}
