package com.vbwd.core.plugins

import android.content.Context
import com.vbwd.core.networking.ApiClient
import com.vbwd.core.networking.ApiJson
import com.vbwd.core.networking.get
import kotlinx.serialization.Serializable

/**
 * Runtime plugin enable/disable manifest. Exact shape of the backend
 * `/admin/frontend-plugins/<app>` response and the bundled `plugins.json`:
 * `{ plugins: { name: { enabled, version, installedAt?, source } } }`.
 * Port of the iOS `PluginManifest`.
 */
@Serializable
data class PluginManifest(val plugins: Map<String, Entry> = emptyMap()) {

    @Serializable
    data class Entry(
        val enabled: Boolean,
        val version: String,
        val installedAt: String? = null,
        val source: String = "local",
    )

    /** Names whose entry is `enabled` — the gate the registry/host honours. */
    val enabledNames: Set<String>
        get() = plugins.filterValues { it.enabled }.keys

    fun isEnabled(name: String): Boolean = plugins[name]?.enabled ?: false

    companion object {
        val EMPTY = PluginManifest()
    }
}

/**
 * Reads the manifest. Android is **read-only** (the backend is the single
 * writer). Port of the iOS loader contract: `load()` never throws — on any
 * failure it returns a fallback (web parity).
 */
interface PluginManifestLoader {
    suspend fun load(): PluginManifest
}

/** Deterministic loader for tests/previews and the bundled-default path. */
class InMemoryPluginManifestLoader(private val manifest: PluginManifest) : PluginManifestLoader {
    override suspend fun load(): PluginManifest = manifest
}

/** Fetches the manifest from a backend endpoint; falls back on any error. */
class RemotePluginManifestLoader(
    private val api: ApiClient,
    private val path: String,
    private val fallback: PluginManifest = PluginManifest.EMPTY,
) : PluginManifestLoader {
    // Broad catch is intentional: the loader never throws (web parity) — any
    // failure yields the fallback; cancellation is re-thrown.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun load(): PluginManifest =
        try {
            api.get(path)
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            fallback
        }
}

/**
 * Loads the manifest from a bundled `plugins.json` asset (offline-safe, no
 * backend dependency). Port of the iOS `BundledPluginManifestLoader`. Returns
 * [PluginManifest.EMPTY] when the asset is missing or malformed.
 */
class BundledPluginManifestLoader(
    private val context: Context,
    private val fileName: String = "plugins.json",
) : PluginManifestLoader {
    override suspend fun load(): PluginManifest =
        runCatching {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        }.mapCatching {
            ApiJson.instance.decodeFromString(PluginManifest.serializer(), it)
        }.getOrDefault(PluginManifest.EMPTY)
}
