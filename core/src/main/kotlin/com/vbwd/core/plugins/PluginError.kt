package com.vbwd.core.plugins

/**
 * Plugin lifecycle errors. Port of the iOS `PluginError` — mirrors the failure
 * modes of the web `PluginRegistry.ts`.
 */
sealed class PluginError(message: String) : Exception(message) {
    data class InvalidVersion(val value: String) : PluginError("invalid version: $value")
    data class Duplicate(val name: String) : PluginError("duplicate plugin: $name")
    data class MissingDependency(val plugin: String, val dependency: String) :
        PluginError("plugin '$plugin' is missing dependency '$dependency'")
    data class UnsatisfiedVersion(
        val plugin: String,
        val dependency: String,
        val constraint: String,
    ) : PluginError("plugin '$plugin' needs '$dependency' $constraint")
    data class CircularDependency(val cycle: List<String>) :
        PluginError("circular dependency: ${cycle.joinToString(" -> ")}")
    data class InvalidState(val plugin: String, val reason: String) :
        PluginError("plugin '$plugin': $reason")
    data class InstallFailed(val plugin: String, val reason: String) :
        PluginError("plugin '$plugin' install failed: $reason")
}
