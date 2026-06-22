---
title: Writing a plugin
---

# Writing a plugin

[← Back to index](index.md)

This walks through a minimal plugin. The reference implementation is
`vbwd-android-example` (`ExamplePlugin`), which exercises **every** seam — copy it
when you start.

## 1. Module setup

A plugin is an Android **library** module that depends on `:core` only.

`build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.android.junit5)
}

android {
    namespace = "com.vbwd.plugin.hello"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.vbwd:vbwd-android-core:0.1.0")  // :core ONLY
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
}
```

(To add publishing, mirror the `maven-publish` block from any plugin — see
[Consuming the SDK](consuming-the-sdk.md).)

## 2. Implement `Plugin`

Keep the plugin class a **thin composition root**: it wires domain + UI that live
in their own files (Single Responsibility).

```kotlin
package com.vbwd.plugin.hello

import androidx.compose.material3.Text
import com.vbwd.core.events.AppEvents
import com.vbwd.core.events.Unsubscribe
import com.vbwd.core.plugins.*
import com.vbwd.core.plugins.registries.MenuItem

class HelloPlugin : Plugin {
    private var unsubscribe: Unsubscribe? = null

    override val metadata = PluginMetadata(
        name = "hello",
        version = SemanticVersion(1, 0, 0),
        description = "A minimal example plugin.",
        translations = mapOf("en" to mapOf("hello.title" to "Hello")),
    )

    override suspend fun install(sdk: PlatformSdk) {
        // a screen
        sdk.addRoute(PluginRoute(path = "/hello", name = "hello") { HelloScreen() })

        // a dashboard widget + profile section (name-prefix convention)
        sdk.addComponent("DashboardHello") { Text("Hello widget") }
        sdk.addComponent("ProfileHello")   { Text("Hello profile section") }

        // i18n
        sdk.addTranslations("de", mapOf("hello.title" to "Hallo"))

        // side-menu entry
        sdk.addMenuItem(
            MenuItem(id = "hello", icon = "star", title = "Hello",
                routePath = "/hello", order = 50, section = "top"),
        )

        // react to app events
        unsubscribe = sdk.events.on(AppEvents.AUTH_LOGIN) { /* … */ }

        // call the backend through the injected client (DIP)
        runCatching { sdk.api.get<Unit>("/some/endpoint") }
    }

    override suspend fun uninstall() {
        unsubscribe?.invoke()
        unsubscribe = null
    }
}
```

## 3. Register it with the host

In the host app's `di/CoreModule.kt`, add an instance to the available-plugins
list, then enable it in `plugins.json`:

```kotlin
fun provideAvailablePlugins(/* … */): List<Plugin> = listOf(
    ExamplePlugin(),
    HelloPlugin(),       // ← add
    /* … */
)
```

```json
{ "plugins": { "hello": { "enabled": true, "version": "1.0.0", "source": "local" } } }
```

See [The host app](host-app.md) for the full wiring.

## 4. SOLID checklist (what reviewers look for)

- **S** — one responsibility per file; the plugin class only *wires*.
- **O** — extend via `PlatformSdk` seams; never modify core.
- **L** — your plugin honours the same `Plugin` contract as any other; a disabled
  plugin must not break callers.
- **I** — depend on narrow ports (`ApiClient`, not a concrete HTTP client).
- **D** — depend on abstractions injected via `sdk`, never on a concrete plugin.

## 5. Test it

Write a contract test that installs the plugin into a fake `PlatformSdk` and
asserts your routes/components/menu items registered. For UI, use Robolectric
Compose tests with `testTag`s. See [Testing & the quality gate](testing.md).

## Common patterns

- **Domain via the API client**: `sdk.api.get<MyDto>(path)` — suspend, throws
  `ApiError` on failure. Wrap broad catches with
  `currentCoroutineContext().ensureActive()` so cancellation propagates.
- **Shared state**: `sdk.createStore("myStore", MyStore())` then read it where
  needed; or just hold it in your plugin instance.
- **Auth-gated screens**: `PluginRoute(requiresAuth = true, requiredUserPermission = "x")`.

---

Next: [Consuming the SDK →](consuming-the-sdk.md)
