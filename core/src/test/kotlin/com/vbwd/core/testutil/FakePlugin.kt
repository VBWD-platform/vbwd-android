package com.vbwd.core.testutil

import com.vbwd.core.plugins.Plugin
import com.vbwd.core.plugins.PluginDependencies
import com.vbwd.core.plugins.PluginMetadata
import com.vbwd.core.plugins.PlatformSdk
import com.vbwd.core.plugins.SemanticVersion

/** Configurable [Plugin] test double — records lifecycle hook calls. */
class FakePlugin(
    name: String,
    version: SemanticVersion = SemanticVersion(1, 0, 0),
    dependencies: PluginDependencies = PluginDependencies.None,
    private val failInstall: Boolean = false,
    private val installRecorder: MutableList<String>? = null,
) : Plugin {
    override val metadata = PluginMetadata(name = name, version = version, dependencies = dependencies)

    var installed = false
    var activated = false
    var deactivated = false
    var uninstalled = false

    override suspend fun install(sdk: PlatformSdk) {
        if (failInstall) throw IllegalStateException("boom")
        installRecorder?.add(metadata.name)
        installed = true
    }

    override suspend fun activate() {
        activated = true
    }

    override suspend fun deactivate() {
        deactivated = true
    }

    override suspend fun uninstall() {
        uninstalled = true
    }
}
