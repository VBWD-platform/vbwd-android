---
title: vbwd-android SDK — Developer Guide
---

# vbwd-android SDK — Developer Guide

Build feature plugins and host apps on the **vbwd-android** platform — the
Kotlin · Jetpack Compose · Hilt port of the vbwd-ios plugin-host SDK.

This guide is for developers building **solutions** on top of the SDK: writing
plugins, consuming the published artifacts, and assembling a host app.

## Contents

1. [Getting started](getting-started.md) — prerequisites, install, first build.
2. [Architecture](architecture.md) — modules, the plugin host, the boundary rule.
3. [The plugin contract](plugin-contract.md) — `Plugin`, `PlatformSdk`, every seam.
4. [Writing a plugin](writing-a-plugin.md) — a step-by-step plugin from scratch.
5. [Consuming the SDK](consuming-the-sdk.md) — GitHub Packages, Gradle setup.
6. [The host app](host-app.md) — wiring plugins, config, the manifest.
7. [Testing & the quality gate](testing.md) — `bin/pre-commit-check.sh`, Compose tests.
8. [MeinChat reference](meinchat-reference.md) — a full-featured plugin, with screenshots.
9. [Published artifacts](artifacts.md) — coordinates and versions.

## The one-paragraph mental model

The SDK is a **single core module** (`com.vbwd:vbwd-android-core`) plus any number
of **feature plugins**. The core owns networking, auth/session, the shared Compose
UI shell, and the **plugin host**. A plugin implements one interface (`Plugin`) and
extends the app **only** through a facade (`PlatformSdk`) — it never touches core
internals. The host app declares which plugins to bundle and a `plugins.json`
manifest decides which are enabled at boot. Every public contract (the `Plugin`
interface, the `PlatformSdk` seams, event names, manifest shape, route/permission
semantics) is identical to the iOS/web SDK, so the mental model ports 1:1.

```
┌─────────────────────────────────────────────┐
│  host app (:app)                             │
│   • names concrete adapters (DI)             │
│   • provides the available-plugins list      │
│   • ships plugins.json (enable/disable)      │
├─────────────────────────────────────────────┤
│  core SDK (vbwd-android-core)                │
│   • networking · session · shared UI         │
│   • PluginHost → PluginRegistry              │
│   • PlatformSdk facade  ◄── the only seam    │
├─────────────────────────────────────────────┤
│  plugins (vbwd-android-*)                    │
│   • depend on :core ONLY                     │
│   • install(sdk): routes, components, …      │
└─────────────────────────────────────────────┘
```

> New here? Start with [Getting started](getting-started.md), then read
> [The plugin contract](plugin-contract.md) and follow
> [Writing a plugin](writing-a-plugin.md).
