---
title: Architecture
---

# Architecture

[← Back to index](index.md)

## Modules

| Module | Artifact | Responsibility |
|--------|----------|----------------|
| `:app` | (the host) | `@HiltAndroidApp`, `MainActivity`, composition root, `plugins.json`. |
| `:core` | `com.vbwd:vbwd-android-core` | Networking, domain, session/auth, the plugin system, the shared Compose UI shell. |
| `:plugins:*` | `com.vbwd:vbwd-android-<name>` | Feature plugins — depend on `:core` **only**. |

## The dependency-boundary rule (core-agnosticism)

A plugin may depend on **`:core` and nothing else** — except a peer plugin it
*declares*. This is enforced by the `dependencyBoundaryCheck` Gradle task and
mirrors `PluginMetadata.dependencies`. The only declared plugin→plugin edge today
is `meinchat-plus → meinchat`.

Why it matters: plugins extend the app exclusively through the `PlatformSdk`
facade (Open/Closed). They never import a registry, the host, or another plugin's
models. This keeps the core **agnostic** of any feature and lets plugins ship,
version, and be enabled/disabled independently.

```
plugin  ──depends on──►  :core        ✅ always allowed
plugin  ──depends on──►  another plugin   ❌ unless declared in dependencies
plugin  ──depends on──►  :app          ❌ never
```

## Boot sequence (the plugin host)

`PluginHost.bootstrap()` (in core) runs once at app start:

1. **Load the manifest** — `plugins.json` (offline-first) or the backend endpoint.
2. **Register** the compiled-in plugins with the `PluginRegistry`.
3. **Install** the *enabled* ones in **dependency order** (topological sort, with
   semver-checked dependencies). Each `Plugin.install(sdk)` contributes its routes,
   components, stores, etc.
4. **Collect routes** from the SDK.
5. **Activate** the installed plugins.

Robustness is built in:

- A **structural** error (missing/unsatisfied/circular dependency) is isolated —
  the shell still loads.
- A **per-plugin** hook failure marks that plugin `PluginStatus.Error` and skips
  it; peers continue. A broken third-party plugin can never take down the app.

```
plugins.json ─► PluginHost.bootstrap()
                   │ register → installAll(enabled) → activate
                   ▼
              PluginRegistry  (topological, semver, error-isolated)
                   │ install(sdk)
                   ▼
              PlatformSdk  (routes / components / stores / i18n / menu / payment / checkout)
                   │
                   ▼
              AppShellView renders Dashboard / Profile / Settings + plugin routes
```

## The shared UI shell

Core ships the host UI so plugins focus on features, not chrome:

- `RootView` — switches between the login screen (signed out) and the app shell.
- `AppShellView` — the drawer + Dashboard / Profile / Settings, and it surfaces
  plugin **routes**, **dashboard widgets** (`Dashboard*` components) and **profile
  sections** (`Profile*` components).
- A theme system (`VbwdTheme`, `ThemeManager`) — plugins can register themes.

## Composition root (DI)

The host's `di/CoreModule.kt` (a Hilt module) is the **only** place concrete
adapters are named (`OkHttpApiClient`, `EncryptedTokenStore`, the manifest loader,
the available-plugins list). Everything downstream is constructor-injected against
interfaces. Plugins are **not** in the Hilt graph — they are plain instances the
host lists and the `PluginHost` bootstraps. See [The host app](host-app.md).

## iOS → Kotlin mapping

| iOS / web | Android |
|-----------|---------|
| `ObservableObject` / `@Published` | ViewModel + `StateFlow` |
| `async/await` | `suspend` / Flow |
| `Codable` | kotlinx.serialization |
| `URLSession` behind a port | OkHttp behind `ApiClient` (verb methods, not Retrofit — ISP) |
| `KeychainTokenStore` | `EncryptedSharedPreferences` |

---

Next: [The plugin contract →](plugin-contract.md)
