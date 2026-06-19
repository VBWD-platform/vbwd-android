package com.vbwd.core.config

import android.content.Context
import com.vbwd.core.networking.ApiJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * App-level configuration loaded from the bundled `vbwd_config.json` asset
 * (the `.dist` template is committed; the real file is gitignored so each
 * dev / CI environment points at its own backend). Port of the iOS
 * `VBWDConfig` — A01 needs only the API base URL; the CMS/web-origin keys
 * (A07) are added when a consumer needs them (no overengineering).
 */
@Serializable
data class AppConfig(
    @SerialName("api_base_url") val apiBaseUrl: String = DEFAULT_BASE_URL,
    @SerialName("tarif_plan_root_cat_slug") val tarifPlanRootCatSlug: String? = null,
    @SerialName("root_android_category_on_host") val rootCategoryOnHost: String? = null,
    @SerialName("root_android_post_type_on_host") val rootPostTypeOnHost: String? = null,
    @SerialName("web_base_url") val webBaseUrl: String? = null,
) {
    /**
     * Web origin for the CMS embed routes (A07). Prefers an explicit
     * `web_base_url` (split-host dev); otherwise derives it by stripping the
     * `/api/vN` suffix from [apiBaseUrl]. Port of the iOS `webOrigin`.
     */
    val webOrigin: String?
        get() {
            webBaseUrl?.takeIf { it.isNotEmpty() }?.let { return it }
            return apiBaseUrl.replace(Regex("/api/v[0-9]+/?$"), "").takeIf { it.isNotEmpty() }
        }

    /**
     * Archive URL the CMS Posts browser opens onto, or null when either CMS key
     * is unset (the plugin uses that to stay unregistered). Port of `cmsArchiveURL`.
     */
    val cmsArchiveUrl: String?
        get() {
            val origin = webOrigin ?: return null
            val type = rootPostTypeOnHost?.takeIf { it.isNotEmpty() } ?: return null
            val category = rootCategoryOnHost?.takeIf { it.isNotEmpty() } ?: return null
            return "$origin/cms/embed/$type/$category"
        }

    companion object {
        /**
         * Fallback base URL. `10.0.2.2` is the Android emulator's alias for the
         * host machine's `localhost` (where the dev backend runs on :5000).
         */
        const val DEFAULT_BASE_URL = "http://10.0.2.2:5000/api/v1"

        /**
         * Loads config from the named asset. Returns defaults when the asset is
         * missing or malformed — the caller never has to handle a null (the iOS
         * `load()` returns nil + a `defaultBaseURL` fallback; here it is folded
         * into one call).
         */
        fun load(context: Context, fileName: String = "vbwd_config.json"): AppConfig =
            runCatching {
                context.assets.open(fileName).bufferedReader().use { it.readText() }
            }.mapCatching {
                ApiJson.instance.decodeFromString(serializer(), it)
            }.getOrDefault(AppConfig())
    }
}
