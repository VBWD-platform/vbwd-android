# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`vbwd-android` is a **Kotlin + Jetpack Compose port of `vbwd-ios`** — a plugin-host
platform. A single SDK module (`:core`) plus feature plugin modules (`:plugins:*`)
that extend it through stable seams. The host app (`:app`) wires the enabled plugins
from a `plugins.json` manifest at boot. The port preserves the **exact public
contracts** of the iOS/web SDK (the `Plugin` interface, the `PlatformSdk` facade,
event names, manifest shape, route/permission semantics) so a plugin author with an
iOS/web mental model maps 1:1.

This repo is one piece of the larger `vbwd-sdk` (see `../CLAUDE.md` for the
Python/Flask backend + Vue frontends). The Android app talks to that backend's
`/api/v1/` endpoints. The backend is the **single writer** of plugin enable/disable
state; Android reads the manifest only.

## Toolchain (non-obvious, will bite you)

- **JDK 17 is required.** Gradle 8.x + AGP 8.9 do **not** run on JDK 23+. Point
  `JAVA_HOME` at a Temurin 17.
- **Android SDK** — copy `local.properties.dist` → `local.properties` with your
  `sdk.dir`, or set `ANDROID_HOME`. `compileSdk`/`targetSdk` = 36, `minSdk` = 26.
- Dependency versions live **only** in `gradle/libs.versions.toml` (version catalog,
  DRY). Don't hardcode a version in a module `build.gradle.kts`.

## Commands (the quality gate)

Plugins and `:core` are composite `includeBuild(...)`s, so a module task is
addressed `:<included-build>:<module>:<task>` (e.g. `:core:core:test`,
`:tarot:tarot:test`) — **not** `:plugins:tarot:test`. `:app` is the only real
subproject.

```bash
./gradlew check                  # ktlint + detekt + unit tests + Android lint (ALL modules)
./gradlew dependencyBoundaryCheck    # plugins depend on :core (+ declared peers) ONLY
./gradlew :core:core:test        # quick loop on one module
./gradlew :tarot:tarot:test      # quick loop on one plugin (composite path)
./gradlew :app:assembleDebug     # build the APK
```

`check` + `dependencyBoundaryCheck` are exactly what CI runs
(`.github/workflows/android-ci.yml`) and are the Android translation of the
backend/iOS `bin/pre-commit-check.sh --full` gate. **Both must be green** before any
change is done. Detekt config: `config/detekt/detekt.yml`.

Run a single test class/method via Gradle's filter:
```bash
./gradlew :core:core:test --tests "com.vbwd.core.plugins.PluginRegistryTest"
./gradlew :core:core:test --tests "*.PluginRegistryTest.installs in topological order"
```

## Architecture

### Module boundaries (CI-enforced)

```
:app          host application — @HiltAndroidApp, MainActivity, AppRoot, the
              composition root (di/CoreModule.kt), assets/plugins.json + vbwd_config.json
:core         the SDK (com.vbwd.core) — networking, domain, session, plugin system, shared Compose UI
:plugins:*    feature plugins (com.vbwd.plugin.*) — depend on :core ONLY
```

The **dependency-boundary rule** (core-agnosticism) is enforced by the
`dependencyBoundaryCheck` Gradle task in the root `build.gradle.kts`: a `:plugins:*`
module may depend on `:core` and **nothing else** — except a peer plugin it
*declares*. The only declared plugin→plugin edge today is
`:plugins:meinchat-plus → :plugins:meinchat` (mirrors
`PluginMetadata.dependencies = ["meinchat"]`). A plugin depending on `:app`, or an
undeclared `:plugins:a → :plugins:b` edge, **fails the build**. To add a declared
peer edge you must update `declaredPeerDependencies` in the root build file *and* the
plugin's `PluginMetadata.dependencies`.

### The plugin contract (the heart of the system)

Read these together — they define the extension surface; everything else is a
consumer of them:

- `core/.../plugins/Plugin.kt` — the `Plugin` interface (`install`/`activate`/
  `deactivate`/`uninstall`, all `suspend`, all but `install` default to no-op) and
  `PluginMetadata` (name, semver, `dependencies`, translations).
- `core/.../plugins/PlatformSdk.kt` — the **facade handed to `Plugin.install`**. A
  plugin depends on **this interface only** — never on a registry, the registry
  manager, or the composition root. This is the single seam through which the app is
  extended (OCP/ISP/DIP). Seams: `addRoute`, `addComponent`, `createStore`,
  `addTranslations`, `addMenuItem`, `addPaymentAction`, `addCheckoutSource`, plus
  injected `api`/`events`/`cart`/`notifications`/`checkoutSources`.
- `core/.../plugins/PluginHost.kt` — the plugin composition root. `bootstrap()`:
  load manifest → register compiled-in plugins → install the **enabled** ones in
  dependency order → collect routes → activate. A structural or per-plugin failure
  is **isolated** and never blocks the shell.
- `core/.../plugins/PluginRegistry.kt` — lifecycle manager: register → install
  (topological sort, semver-checked deps) → activate/deactivate (active-dependent
  guarded) → uninstall, with **per-plugin error isolation** (a plugin failing a hook
  becomes `PluginStatus.Error` without aborting peers).
- `core/.../plugins/PluginManifest.kt` — the `plugins.json` shape + loaders.
  `BundledPluginManifestLoader` (offline-first, from assets) is what the app uses;
  loaders **never throw** (return a fallback on any error — web parity).

### Component naming conventions (how plugins surface UI)

Plugins register Compose factories by name via `sdk.addComponent(name) { ... }`. The
shell discovers them by prefix:
- `Dashboard*` → rendered as a dashboard widget (e.g. `"DashboardExample"`).
- `Profile*` → rendered as a profile section (e.g. `"ProfileExample"`).

`:plugins:example` (`ExamplePlugin.kt`) is the **reference implementation** — it
exercises every `PlatformSdk` seam and is the canonical template for a new plugin.

### Composition root (DI)

`app/.../di/CoreModule.kt` is the **only** place concrete adapters are named
(`OkHttpApiClient`, `EncryptedTokenStore`, `BundledPluginManifestLoader`, the
`provideAvailablePlugins` list). Everything downstream is constructor-injected by
Hilt against interfaces. All singletons → one shared `ApiClient`/`TokenStore`/
`AuthSession`/`Cart` app-wide. Dynamic plugin instances live **outside** the Hilt
graph (the `List<Plugin>` is provided here but bootstrapped by `PluginHost`).

To **add a new plugin** to the host: create the `:plugins:<name>` module, add it to
`settings.gradle.kts` + `:app`'s `dependencies`, add the instance to
`provideAvailablePlugins`, and add its entry to `app/src/main/assets/plugins.json`
(enable/disable gate).

### iOS → Kotlin mapping (locked decisions)

ViewModel/`StateFlow` ≈ `ObservableObject`/`@Published`; `suspend`/Flow ≈
`async/await`; kotlinx.serialization ≈ `Codable`; OkHttp behind the `ApiClient` port
(verb methods, **not** Retrofit typed endpoints — ISP); `EncryptedSharedPreferences`
≈ `KeychainTokenStore`. Full rationale: `docs/dev_log/20260619/sprints/00-PORT-OVERVIEW.md`.

## Config

- `app/src/main/assets/vbwd_config.json` (copy from `.dist`) — `api_base_url`
  (use `http://10.0.2.2:5000/...` to reach the host's localhost from the emulator),
  CMS post-type/category keys, web origin. Loaded by `core/.../config/AppConfig.kt`.
- `app/src/main/assets/plugins.json` — plugin enable/disable manifest (offline
  default; the backend manifest endpoint is the runtime single-writer).

## Engineering rules (binding — from `docs/_engineering_requirements.md`)

- **TDD-first.** Every change starts with a failing test (Red → Green → Refactor).
  No code lands without a test. Stack: JUnit5 + MockK + Turbine +
  kotlinx-coroutines-test + Robolectric; Compose UI via `compose.ui.test`; JVM
  Roborazzi screenshot tests via Robolectric (record with
  `-Proborazzi.test.record=true`). The JUnit-vintage engine runs the JUnit4/
  Robolectric tests on the JUnit5 platform.
- **SOLID / DI / DRY.** Extend via the `PlatformSdk` seams; **never modify `:core`
  for a plugin's feature**. Depend on abstractions, not concrete plugin modules.
- **No over-engineering** — the narrowest change that satisfies the requirement.
- **Liskov for the disabled-plugin path** — a disabled/absent plugin must not break
  callers; null defaults and test fakes honour the same contract.
- **Do not commit** unless explicitly instructed.
- Clean code: full readable names, functions < 50 LOC, classes < 300 LOC, no bare
  `catch (e: Exception) {}` (the broad catches in the plugin registry/loaders are
  deliberate error-isolation and are annotated as such).

## Publishing (polyrepo)

`:core` publishes an AAR to GitHub Packages as `com.vbwd:vbwd-android-core` so future
standalone plugin/template repos consume it via Maven:
```bash
GITHUB_ACTOR=... GITHUB_TOKEN=<PAT with write:packages> ./gradlew :core:publish
```
Split-plan + repo generators: `docs/dev_log/20260619/reports/14-polyrepo-split-plan.md`,
`tools/create-android-repos.sh`, `tools/make-umbrella-submodules.sh`.

## Where to read more

`docs/dev_log/20260619/sprints/` (A01–A10 sprint plans, one per module/feature) and
`docs/dev_log/20260619/reports/` (per-sprint completion reports). Start with
`sprints/00-PORT-OVERVIEW.md`.
