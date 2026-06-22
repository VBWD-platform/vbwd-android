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
// Post-polyrepo, plugins are composite `includeBuild(...)`s — NOT root
// subprojects — and declare their peers as substituted Maven coordinates
// (`com.vbwd:vbwd-android-<module>`, see settings.gradle.kts). The project model
// of an included build is not reachable from here, so we statically scan each
// plugin module's build script for those coordinates (mirrors the iOS shell
// lint). This also keeps the task configuration-cache compatible: it captures
// only Files and plain data — never `subprojects`/`configurations` at execution.
// ---------------------------------------------------------------------------
val declaredPeerDependencies: Map<String, Set<String>> = mapOf(
    // meinchat-plus consumes meinchat's secure-messaging seam (the only declared
    // plugin→plugin edge; mirrors PluginMetadata.dependencies = ["meinchat"]).
    ":plugins:meinchat-plus" to setOf(":core", ":plugins:meinchat"),
)

// Each plugin lives at plugins/<name>/<name>/build.gradle.kts. Resolved at
// configuration time to plain Files so the cache can serialize the task.
val pluginModuleBuildFiles: List<File> =
    (rootDir.resolve("plugins").listFiles { file -> file.isDirectory }?.toList() ?: emptyList())
        .map { dir -> dir.resolve("${dir.name}/build.gradle.kts") }
        .filter { it.isFile }
        .sortedBy { it.path }

tasks.register("dependencyBoundaryCheck") {
    group = "verification"
    description = "Plugin modules may depend on :core only (plus declared peer deps)."
    val allowMap = declaredPeerDependencies
    val buildFiles = pluginModuleBuildFiles
    inputs.files(buildFiles)
    doLast {
        val coordRegex = Regex("""com\.vbwd:vbwd-android-([\w-]+):""")
        val violations = mutableListOf<String>()
        buildFiles.forEach { file ->
            val pluginPath = ":plugins:${file.parentFile.name}"
            val allowed = allowMap[pluginPath] ?: setOf(":core")
            file.readLines()
                .filterNot { it.trimStart().startsWith("//") }
                .forEach { line ->
                    coordRegex.findAll(line).forEach { match ->
                        val artifact = match.groupValues[1]
                        val dep = if (artifact == "core") ":core" else ":plugins:$artifact"
                        if (dep != pluginPath && dep !in allowed) {
                            violations += "$pluginPath -> $dep (allowed: $allowed)"
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
        logger.lifecycle("dependencyBoundaryCheck: OK (${buildFiles.size} plugin module(s) checked)")
    }
}
