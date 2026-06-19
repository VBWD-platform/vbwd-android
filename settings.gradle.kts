pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "vbwd-android"

// A01 ships the host app + the SDK module. A02 adds the first plugin module
// (:plugins:example, the reference implementation of the plugin contract).
include(":app", ":core")
include(":plugins:example")
include(":plugins:subscription")
include(":plugins:token-payment")
include(":plugins:stripe")
include(":plugins:invoice")
include(":plugins:cms")
include(":plugins:tarot")
include(":plugins:meinchat")
include(":plugins:meinchat-plus")
