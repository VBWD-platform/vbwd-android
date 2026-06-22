pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}

rootProject.name = "vbwd-android"

// The umbrella's host app (consumes core + plugins by Maven coordinate;
// the includeBuild substitutions below resolve those to the local submodules).
include(":app")

// Composite builds — each submodule is a standalone Gradle build whose project
// is named after its directory (:core, :example, …), NOT its published
// `com.vbwd:vbwd-android-*` artifact. Automatic substitution can't match those,
// so each includeBuild declares an explicit substitution from the Maven
// coordinate to the submodule's project, letting the tree build + test from
// source (this also covers meinchat-plus's dependency on meinchat).
includeBuild("core") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-core")).using(project(":core"))
    }
}
includeBuild("plugins/example") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-example")).using(project(":example"))
    }
}
includeBuild("plugins/subscription") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-subscription")).using(project(":subscription"))
    }
}
includeBuild("plugins/token-payment") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-token-payment")).using(project(":token-payment"))
    }
}
includeBuild("plugins/stripe") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-stripe")).using(project(":stripe"))
    }
}
includeBuild("plugins/invoice") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-invoice")).using(project(":invoice"))
    }
}
includeBuild("plugins/cms") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-cms")).using(project(":cms"))
    }
}
includeBuild("plugins/tarot") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-tarot")).using(project(":tarot"))
    }
}
includeBuild("plugins/meinchat") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-meinchat")).using(project(":meinchat"))
    }
}
includeBuild("plugins/meinchat-plus") {
    dependencySubstitution {
        substitute(module("com.vbwd:vbwd-android-meinchat-plus")).using(project(":meinchat-plus"))
    }
}
