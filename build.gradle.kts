// Root build. Plugins are declared here (applied in modules) so the version
// catalog resolves them once. The composition root (Hilt) lives in :app/:core.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.android.junit5) apply false
    alias(libs.plugins.roborazzi) apply false
}

// ---------------------------------------------------------------------------
// Dependency-boundary check — Android port of the iOS `boundary-lint.sh`.
//
// Rule (eng-req / core-agnosticism): a plugin module (`:plugins:*`) may depend
// on `:core` ONLY, plus any peer plugin it DECLARES (mirrors
// PluginMetadata.dependencies). An undeclared `:plugins:a -> :plugins:b` edge,
// or a plugin depending on `:app`, fails the build.
//
// In A01.0 there are no plugin modules yet, so this task is vacuously green;
// it grows its allow-map as plugins land (A02+).
// ---------------------------------------------------------------------------
val declaredPeerDependencies: Map<String, Set<String>> = mapOf(
    // meinchat-plus consumes meinchat's secure-messaging seam (the only declared
    // plugin→plugin edge; mirrors PluginMetadata.dependencies = ["meinchat"]).
    ":plugins:meinchat-plus" to setOf(":core", ":plugins:meinchat"),
)

tasks.register("dependencyBoundaryCheck") {
    group = "verification"
    description = "Plugin modules may depend on :core only (plus declared peer deps)."
    doLast {
        val violations = mutableListOf<String>()
        subprojects
            .filter { it.path.startsWith(":plugins:") }
            .forEach { pluginProject ->
                val allowed = declaredPeerDependencies[pluginProject.path] ?: setOf(":core")
                pluginProject.configurations.forEach { configuration ->
                    configuration.dependencies
                        .withType(ProjectDependency::class.java)
                        .forEach { dependency ->
                            if (dependency.path !in allowed) {
                                violations += "${pluginProject.path} -> ${dependency.path} " +
                                    "(allowed: $allowed)"
                            }
                        }
                }
            }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Dependency boundary violations (plugins may import :core + declared peers only):\n" +
                    violations.joinToString("\n") { "  - $it" },
            )
        }
        logger.lifecycle("dependencyBoundaryCheck: OK (${subprojects.count { it.path.startsWith(":plugins:") }} plugin module(s) checked)")
    }
}
