package com.vbwd.core.plugins

/**
 * Plugin lifecycle manager. Port of the iOS `PluginRegistry`:
 * register → install (topological, semver-checked) → activate/deactivate
 * (active-dependent guarded) → uninstall, with **per-plugin error isolation** —
 * a plugin failing its hook becomes [PluginStatus.Error] without aborting the
 * rest (web-faithful robustness). Driven sequentially from the host bootstrap
 * coroutine.
 */
class PluginRegistry {
    private class Record(val plugin: Plugin) {
        var status: PluginStatus = PluginStatus.Registered
    }

    private val records = LinkedHashMap<String, Record>()
    private val registrationOrder = mutableListOf<String>()

    fun register(plugin: Plugin) {
        val name = plugin.metadata.name
        if (records.containsKey(name)) throw PluginError.Duplicate(name)
        records[name] = Record(plugin)
        registrationOrder.add(name)
    }

    fun status(name: String): PluginStatus? = records[name]?.status

    fun all(): List<Pair<String, PluginStatus>> =
        registrationOrder.mapNotNull { name -> records[name]?.let { name to it.status } }

    /**
     * Installs registered plugins, restricted to [enabled] (null ⇒ all).
     * Structural problems (missing/unsatisfied/circular dependencies) throw up
     * front (web parity). A plugin whose `install` hook throws is marked
     * [PluginStatus.Error] and skipped — peers continue (isolation).
     */
    // Broad catch is the point: a third-party plugin may throw anything from
    // install; we isolate it as Error and continue (cancellation re-thrown).
    @Suppress("TooGenericExceptionCaught")
    suspend fun installAll(sdk: PlatformSdk, enabled: Set<String>? = null) {
        val active = registrationOrder.filter { enabled?.contains(it) ?: true }
        val activeSet = active.toSet()

        for (name in active) {
            val meta = records.getValue(name).plugin.metadata
            for ((depName, constraint) in meta.dependencies.resolved) {
                if (depName !in activeSet) {
                    throw PluginError.MissingDependency(plugin = name, dependency = depName)
                }
                val depVersion = records.getValue(depName).plugin.metadata.version
                if (!constraint.isSatisfiedBy(depVersion)) {
                    throw PluginError.UnsatisfiedVersion(
                        plugin = name,
                        dependency = depName,
                        constraint = depVersion.toString(),
                    )
                }
            }
        }

        for (name in topologicalOrder(active)) {
            val record = records.getValue(name)
            try {
                record.plugin.install(sdk)
                record.status = PluginStatus.Installed
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                record.status = PluginStatus.Error(error.toString()) // isolation: continue
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun install(name: String, sdk: PlatformSdk) {
        val record = records[name]
            ?: throw PluginError.MissingDependency(plugin = name, dependency = name)
        try {
            record.plugin.install(sdk)
            record.status = PluginStatus.Installed
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            record.status = PluginStatus.Error(error.toString())
            throw PluginError.InstallFailed(plugin = name, reason = error.toString())
        }
    }

    suspend fun activate(name: String) {
        val record = records[name]
            ?: throw PluginError.InvalidState(plugin = name, reason = "not registered")
        when (record.status) {
            PluginStatus.Installed, PluginStatus.Inactive -> Unit
            else -> throw PluginError.InvalidState(
                plugin = name,
                reason = "must be installed or inactive to activate",
            )
        }
        record.plugin.activate()
        record.status = PluginStatus.Active
    }

    suspend fun deactivate(name: String) {
        val record = records[name]
            ?: throw PluginError.InvalidState(plugin = name, reason = "not registered")
        // Block if an active plugin still depends on this one (web guard).
        for ((other, otherRecord) in records) {
            if (other != name && otherRecord.status == PluginStatus.Active &&
                otherRecord.plugin.metadata.dependencies.resolved.any { it.first == name }
            ) {
                throw PluginError.InvalidState(
                    plugin = name,
                    reason = "active dependent '$other' prevents deactivation",
                )
            }
        }
        record.plugin.deactivate()
        record.status = PluginStatus.Inactive
    }

    suspend fun uninstall(name: String) {
        val record = records[name]
            ?: throw PluginError.InvalidState(plugin = name, reason = "not registered")
        record.plugin.uninstall()
        record.status = PluginStatus.Registered
    }

    // Topological sort (DFS, on-stack cycle detection — web parity).
    private fun topologicalOrder(names: List<String>): List<String> {
        val result = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val onStack = mutableListOf<String>()

        fun visit(name: String) {
            if (name in visited) return
            if (name in onStack) throw PluginError.CircularDependency(onStack + name)
            onStack.add(name)
            val meta = records.getValue(name).plugin.metadata
            for ((depName, _) in meta.dependencies.resolved) {
                if (depName in names) visit(depName)
            }
            onStack.removeAt(onStack.lastIndex)
            visited.add(name)
            result.add(name)
        }

        names.forEach(::visit)
        return result
    }
}
