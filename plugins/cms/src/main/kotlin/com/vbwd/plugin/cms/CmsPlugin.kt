package com.vbwd.plugin.cms

import androidx.compose.runtime.remember
import com.vbwd.core.plugins.PlatformSdk
import com.vbwd.core.plugins.Plugin
import com.vbwd.core.plugins.PluginMetadata
import com.vbwd.core.plugins.PluginRoute
import com.vbwd.core.plugins.SemanticVersion
import com.vbwd.core.plugins.registries.MenuItem
import com.vbwd.plugin.cms.domain.DefaultCmsService
import com.vbwd.plugin.cms.ui.PostsBrowserScreen
import com.vbwd.plugin.cms.ui.PostsBrowserViewModel

/**
 * Config-driven "Posts" browser over the host CMS embed archive. Port of the iOS
 * `CmsPlugin`. Config comes via the constructor (the app reads `AppConfig`); when
 * either CMS key is unset the plugin registers **nothing** — the menu stays clean
 * (Liskov: absent config ⇒ invisible, never a crash).
 */
class CmsPlugin(
    private val postType: String?,
    private val category: String?,
    private val webOrigin: String?,
    private val archiveUrl: String?,
    private val tokenProvider: () -> String? = { null },
) : Plugin {
    override val metadata = PluginMetadata(
        name = "cms",
        version = SemanticVersion(0, 1, 0),
        description = "Config-driven Posts browser pointing at the host CMS.",
        author = "VBWD",
        keywords = listOf("cms", "posts", "browser", "embed"),
        translations = mapOf("en" to mapOf("cms.posts.title" to "Posts")),
    )

    override suspend fun install(sdk: PlatformSdk) {
        val type = postType
        val cat = category
        if (type.isNullOrEmpty() || cat.isNullOrEmpty()) return

        val service = DefaultCmsService(sdk.api)
        sdk.addRoute(PluginRoute(path = "/posts", name = "posts") {
            val viewModel = remember {
                PostsBrowserViewModel(
                    type = type,
                    category = cat,
                    archiveUrlFallback = archiveUrl,
                    webOrigin = webOrigin,
                    service = service,
                )
            }
            PostsBrowserScreen(viewModel, tokenProvider)
        })

        sdk.addMenuItem(
            MenuItem(
                id = "cms-posts",
                icon = "article",
                title = "Posts",
                routePath = "/posts",
                order = MENU_ORDER,
                section = "top",
            ),
        )
        sdk.addTranslations("en", mapOf("cms.posts.title" to "Posts"))
    }

    private companion object {
        const val MENU_ORDER = 60
    }
}
