---
title: Consuming the SDK
---

# Consuming the SDK

[← Back to index](index.md)

Core and every plugin are published to **GitHub Packages** under the
`vbwd-platform` org. This page shows how to depend on them and how to publish your
own.

## Authenticate to GitHub Packages

Maven packages on GitHub **always require authentication** — even public ones. Use
a Personal Access Token (classic) with `read:packages` (add `write:packages` to
publish). Provide it as env vars or Gradle properties:

```bash
export GITHUB_ACTOR=<your-username>
export GITHUB_TOKEN=<PAT with read:packages>
```

or in `~/.gradle/gradle.properties`:

```properties
gpr.user=<your-username>
gpr.key=<PAT with read:packages>
```

## Add the repositories

In `settings.gradle.kts`, add one Maven repo per artifact you consume (the path is
the **repo** that hosts the package):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/vbwd-platform/vbwd-android-core")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orNull
            }
        }
        // …add maven{} blocks for each plugin you consume (vbwd-android-meinchat, …)
    }
}
```

## Depend on the artifacts

```kotlin
dependencies {
    implementation("com.vbwd:vbwd-android-core:0.1.0")
    implementation("com.vbwd:vbwd-android-meinchat:1.1.0")
    // …
}
```

See [Published artifacts](artifacts.md) for the full coordinate/version list.

## Publishing your own plugin

Each plugin module carries a `maven-publish` block (mirroring `:core`):

```kotlin
plugins { /* … */ `maven-publish` }

group = "com.vbwd"
version = "1.0.0"

android {
    /* … */
    publishing { singleVariant("release") { withSourcesJar() } }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.vbwd"
            artifactId = "vbwd-android-hello"
            version = project.version.toString()
            afterEvaluate { from(components["release"]) }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/vbwd-platform/vbwd-android-hello")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orNull
            }
        }
    }
}
```

Publish locally:

```bash
GITHUB_ACTOR=<user> GITHUB_TOKEN=<PAT write:packages> ./gradlew :hello:publish
```

Or via CI — each repo has a `publish.yml` that runs on a `v*` tag:

```bash
git tag v1.0.0 && git push origin v1.0.0
```

> **CI note:** a plugin's publish job must *resolve* its upstreams
> (`vbwd-android-core`, and for `meinchat-plus` also `vbwd-android-meinchat`) from
> sibling repos. The default workflow `GITHUB_TOKEN` cannot read packages
> cross-repo, so the workflows prefer an org secret `PACKAGES_TOKEN`
> (a PAT with `read:packages` + `write:packages`). Set it once:
> `gh secret set PACKAGES_TOKEN --org vbwd-platform --visibility all`.
>
> Re-publishing the same version overwrites it — bump `version` for a new release.

---

Next: [The host app →](host-app.md)
