package com.vbwd.core.plugins

/**
 * Plugin dependency declaration. Port of the web `dependencies?: string[] |
 * Record<string,string>` — a bare list (any version) or version-constrained.
 */
sealed interface PluginDependencies {
    data object None : PluginDependencies
    data class List(val names: kotlin.collections.List<String>) : PluginDependencies
    data class Constrained(val map: Map<String, String>) : PluginDependencies

    /** Normalised `(name, constraint)` pairs; bare list ⇒ `*` (any version). */
    val resolved: kotlin.collections.List<Pair<String, VersionConstraint>>
        get() = when (this) {
            None -> emptyList()
            is List -> names.map { it to VersionConstraint("*") }
            is Constrained -> map.map { (name, constraint) -> name to VersionConstraint(constraint) }
        }
}

/** Plugin manifest/metadata. Port of the iOS `PluginMetadata` / web `IPlugin`. */
data class PluginMetadata(
    val name: String,
    val version: SemanticVersion,
    val description: String? = null,
    val author: String? = null,
    val homepage: String? = null,
    val keywords: List<String> = emptyList(),
    val dependencies: PluginDependencies = PluginDependencies.None,
    val translations: Map<String, Map<String, String>> = emptyMap(),
)

/** Plugin lifecycle state. Port of the iOS `PluginStatus`. */
sealed interface PluginStatus {
    data object Registered : PluginStatus
    data object Installed : PluginStatus
    data object Active : PluginStatus
    data object Inactive : PluginStatus
    data class Error(val message: String) : PluginStatus
}

/**
 * The contract a plugin implements. Port of the iOS `Plugin` protocol: `install`
 * receives the [PlatformSdk]; the other hooks default to no-op (Kotlin interface
 * default methods — the port of the Swift protocol extension), so a plugin
 * overrides only what it needs.
 */
interface Plugin {
    val metadata: PluginMetadata

    suspend fun install(sdk: PlatformSdk) {}
    suspend fun activate() {}
    suspend fun deactivate() {}
    suspend fun uninstall() {}
}
