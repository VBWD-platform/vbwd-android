# Report 04 — A02 Plugin System

**Date:** 2026-06-19 · **Sprint:** [A02](../sprints/A02-plugin-system.md)
(A02.0–A02.7) · **Status:** on disk, **NOT gate-checked** (JDK 26 / no Android
SDK sandbox — same limit as reports 01–03). **A02 feature-complete on disk:** the
app bootstraps a plugin host, reads `plugins.json`, installs/activates enabled
plugins in dependency order, and renders their routes/menu/widgets through stable
seams; the `example` plugin is the living contract test.

## What landed (port of `vbwd-ios-core` `Plugins/`, `Events/`, `Composition/`, `UI/AppShell/` + `vbwd-ios-plugin-example`)

### A02.0 — Module scaffold
- `:plugins:example` Gradle module (compose + detekt/ktlint + junit5), depends on
  `:core` only. `settings.gradle.kts` includes it; `dependencyBoundaryCheck`
  auto-applies the `:core`-only allowlist (no edit needed).

### A02.1 — Contract (`core/plugins/`)
- `Plugin` (interface, default no-op `install/activate/deactivate/uninstall`),
  `PluginMetadata`, `PluginDependencies` (None/List/Constrained + `resolved`),
  `PluginStatus` (sealed), `SemanticVersion` + `VersionConstraint`
  (`^ ~ >= > <= < x-range * exact`), `PluginError` (sealed).

### A02.3 — Events (`core/events/`)
- `AppEvents` (same string constants + `localOnly`), `EventBus` interface +
  `DefaultEventBus` (lock-guarded listeners, capped history, `once/off`,
  `sendToBackend`/`flushPending` with retry, local-only exclusion). Faithful to
  iOS: `emit` is local-only synchronous; backend forwarding is explicit.

### A02.4 — Registries (`core/plugins/registries/`)
- `RouteRegistry` (unique path+name), `ComponentRegistry` (`Dashboard*`/`Profile*`
  discovery + payment actions), `StoreRegistry` (unique id), `LocalizationRegistry`
  (last-write-wins merge), `MenuItem` + `MenuItemRegistry` (order-sorted).

### A02.2 — Facade (`core/plugins/PlatformSdk.kt`) + A04 stubs
- `PlatformSdk` interface (the single plugin-facing seam) + `DefaultPlatformSdk`
  forwarder; `PluginRoute`, `ComponentFactory`, `PaymentAction(Handler)`.
- **A04 stubs** (stable contract now, behaviour later): `cart/Cart`,
  `checkout/CheckoutSource(+Registry)`, `notifications/NotificationsSdk`.

### A02.5 — Manifest + host + navigator + shell
- `PluginManifest` + loaders (`InMemory`/`Remote`/`Bundled` from `assets/plugins.json`).
- `PluginRegistry` — topological install, semver-checked deps, per-plugin error
  isolation, active-dependent-guarded deactivate, status machine.
- `PluginHost` — bootstrap: load manifest → register → installAll(enabled) →
  collect routes → activate; emits `pluginError` on structural failure.
- `Navigator.resolve → Allow|RedirectToLogin|Forbidden|NotFound` (prefix +
  permission rules).
- Compose `AppShellView` (navigation drawer + content host; Home surfaces
  `Dashboard*`/`Profile*` components; routes resolved via `Navigator`).

### A02.6 — Example plugin (`:plugins:example`)
- `ExamplePlugin` (thin composition root) + `ExampleStore` (StateFlow),
  `ExampleService` (DIP over `ApiClient`), `ExampleMenuItems`, and
  `ExampleScreen`/`ExampleDashboardWidget`/`ExampleProfileSection`. Registers a
  route, a dashboard widget, a profile section, a store, translations, a menu
  item, and an `auth:login` subscription (released in `uninstall`).

### A02.7 — Integration (`:app`)
- `CoreModule` provides `EventBus`, available-plugins `[ExamplePlugin()]`,
  `BundledPluginManifestLoader`, `PluginHost` (singletons); the 401 seam now
  also emits `AUTH_SESSION_EXPIRED` on the shared bus.
- `AppRoot` bootstraps the host then renders `AppShellView` when authenticated;
  `MainActivity` field-injects `AuthSession` + `PluginHost`; `plugins.json`
  enables `example`.

## Tests (TDD)
- `SemanticVersionTest`, `PluginContractTest`, `EventBusTest`, `RegistriesTest`,
  `DefaultPlatformSdkTest`, `PluginRegistryTest` (topo order, error isolation,
  missing-dep, gate-excluded dep, status machine, deactivate guard),
  `NavigatorTest`, `PluginManifestTest`, `PluginHostTest`. Plugin module:
  `ExamplePluginContractTest` (install registers all seams; subscription
  updates store + released on uninstall; `Dashboard*`/`Profile*` conventions).
- Shared test doubles: `testutil/FakeApiClient`, `testutil/FakePlugin`.

## Build / config changes
- **`:core` now exposes `kotlinx-serialization` + `kotlinx-coroutines-core` as
  `api`** — they leak through the public surface (`ApiClient.request(...,
  DeserializationStrategy)`, `StateFlow` returns), so plugin modules need them
  transitively. (Bug caught in self-review: were `implementation`.)
- detekt: `TooManyFunctions` thresholds → 20 (wide `PlatformSdk`/`EventBus`
  ports), `LongParameterList.constructorThreshold` → 12 (the SDK forwarder).
- `settings.gradle.kts` + app `build.gradle.kts` add `:plugins:example`.

## Validation
**Gate NOT run** (JDK 26, no Android SDK). Turn green in Studio / CI:
```bash
./gradlew check                       # ktlint + detekt + JUnit5 (:app, :core, :plugins:example)
./gradlew dependencyBoundaryCheck     # :plugins:example → :core only
./gradlew :app:assembleDebug          # boot → install example → menu → /example
```
Self-review done: import ordering (ktlint), all lines ≤120, MagicNumber-safe
(dp/semver consts hoisted), broad catches documented with `@Suppress`
(plugin-isolation/transport-retry/best-effort probe — cancellation re-thrown),
no `:plugins:* → :app` edges, no SwiftUI/iOS leftovers.

## Risks to watch on first real run (unvalidated)
- Compose @Composable lambdas in unit tests rely on the kotlin-compose plugin
  processing the test compilation (it does); if a route-construction test fails
  to compile, move those to androidTest.
- detekt may surface a `CyclomaticComplexMethod` on `VersionConstraint`/`installAll`
  if its default (15) is stricter than estimated — bump or extract if it fires.
- `hilt-navigation-compose` 1.2.0 vs Hilt 2.52 KSP compatibility.

## Next
**A02 exit gate** (run the commands above in Studio/CI). Then **A03 — Theme +
Profile + Dashboard + Settings** (the cross-cutting `:core` UI that consumes the
A02 component registries), and in parallel A07/A08 once A02 is green.
