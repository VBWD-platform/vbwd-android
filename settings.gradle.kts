pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}

rootProject.name = "vbwd-android"

// The umbrella's host app (consumes core + plugins by Maven coordinate;
// includeBuild below substitutes those with the local submodule builds).
include(":app")

// Composite builds — each submodule is a standalone Gradle build. Their
// published `com.vbwd:vbwd-android-*` coordinates are auto-substituted with the
// local source, so the whole tree builds + tests from source.
includeBuild("core")
includeBuild("plugins/example")
includeBuild("plugins/subscription")
includeBuild("plugins/token-payment")
includeBuild("plugins/stripe")
includeBuild("plugins/invoice")
includeBuild("plugins/cms")
includeBuild("plugins/tarot")
includeBuild("plugins/meinchat")
includeBuild("plugins/meinchat-plus")
