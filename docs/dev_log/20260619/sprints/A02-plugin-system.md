# Sprint A02 — Plugin System

**Parent:** [00-PORT-OVERVIEW](./00-PORT-OVERVIEW.md) ·
**Eng-req:** [`_engineering_requirements.md`](./_engineering_requirements.md) ·
**Area:** `:core` (plugins/) + `:app` + `:plugins:example` ·
**Depends on:** A01 · **Blocks:** every plugin sprint ·
**Source:** `vbwd-ios-core` `Plugins/`, `Composition/`, `UI/AppShell/`,
`Events/`, and `vbwd-ios-plugin-example`.

## Goal

Port the **plugin host platform** — the heart of the SDK. After A02 the app
boots its `AppShell`, reads `plugins.json`, installs/activates enabled plugins,
and renders their routes, menu items, dashboard/profile components and
translations through stable seams. The `example` plugin is the reference
implementation and the living test of the contract.

## Subsprints (mirrors iOS sprint-02.x)

### A02.0 — Plugin module scaffolding
- `:plugins:example` Gradle module (depends on `:core` only).
- Boundary check allowlist updated; `:plugins:example → :core` is the only edge.
- **Green:** `./gradlew :plugins:example:check`.

### A02.1 — Plugin contract
**Port** `Plugin.swift`, `SemanticVersion.swift`, `PluginError.swift`.

**Deliverables (`core/plugins/`)**
- `Plugin` interface — `val metadata: PluginMetadata`;
  `suspend fun install(sdk: PlatformSdk)`, `activate()`, `deactivate()`,
  `uninstall()` with **default no-op** implementations (interface default
  methods — port of the Swift protocol extension).
- `data class PluginMetadata(name, version, description?, author?, homepage?,
  keywords, dependencies, translations)`.
- `sealed interface PluginDependencies`: `None`, `List(names)`,
  `Constrained(map)` with a `resolved: List<Pair<name, VersionConstraint>>`.
- `enum PluginStatus`: `Registered, Installed, Active, Inactive, Error(msg)`.
- `data class SemanticVersion(major, minor, patch)` + `VersionConstraint("*"|…)`
  with `satisfies(version)`.
- `PluginError` (sealed).

**TDD:** `SemanticVersionTest` (compare, constraint satisfy incl. `*`);
`PluginDependenciesTest` (`resolved` for none/list/constrained);
`PluginMetadataTest` (equality, defaults).

### A02.2 — PlatformSdk facade
**Port** `PlatformSDK.swift`.

**Deliverables**
- `PlatformSdk` interface — the **only** type a plugin depends on (ISP/DIP):
  `api`, `apiConfig`, `events`, `notifications`; `addRoute/getRoutes`;
  `addComponent/removeComponent/getComponents`; `createStore/getStores`;
  `addTranslations/getTranslations`; `addMenuItem/removeMenuItem/getMenuItems`;
  `addPaymentAction`; `cart`, `checkoutSources`, `addCheckoutSource/remove…`.
- `PluginRoute` data class — `path, name, requiresAuth, requiredUserPermission,
  matchPrefix, content: @Composable () -> Unit`.
- `typealias ComponentFactory = @Composable () -> Unit`.
- `DefaultPlatformSdk` — thin forwarder over the registries (no behaviour of its
  own; DRY/SRP).

**TDD:** `DefaultPlatformSdkTest` — each method forwards to its registry; routes
with duplicate path/name throw; the facade exposes nothing beyond the interface.

> **Note on cart/checkout/payment seams:** in A02 these are declared on the
> interface but back onto **minimal stubs**; their full behaviour lands in A04
> (`Cart`, `CheckoutSourceRegistry`, payment actions). This keeps the plugin
> contract stable from A02 while not over-building UI the example plugin doesn't
> need yet (**no overengineering**).

### A02.3 — EventBus
**Port** `Events/EventBus.swift`, `Events/AppEvents.swift`.

**Deliverables (`core/events/`)**
- `EventBus` interface — `on(event, callback): Unsubscribe`,
  `emit(event, payload?)`, `flushPending()`.
- `DefaultEventBus` — `Mutex`-guarded listeners + history (cap `maxHistory=100`),
  fire-and-forget `emit`, non-local events forwarded to the backend
  (`POST /events`), failed sends retained + retried on `flushPending`. The 6
  UI-local events never hit the backend.
- `object AppEvents` — **same string constants** as iOS (authLogin, authLogout,
  authTokenRefreshed, authSessionExpired, pluginError, userUpdated,
  subscriptionCreated, paymentSucceeded, …).
- Wire the A01 `401` seam to `emit(authSessionExpired)`.

**TDD** (Turbine + coroutines-test): `on/emit` delivers; `unsubscribe` stops
delivery; history caps at `maxHistory`; UI-local events are **not** forwarded;
backend-forwarded event retried on `flushPending` after a failed send.

### A02.4 — Registries
**Port** `Plugins/Registries/*` + `AppShellMenuItem*`.

**Deliverables (`core/plugins/registries/`)**
- `RouteRegistry` — unique path **and** name enforced (throws on dup).
- `ComponentRegistry` — named factories; `dashboardComponents()` /
  `profileComponents()` discovery by the `Dashboard*` / `Profile*` prefix
  convention; `addPaymentAction`.
- `StoreRegistry` — unique id enforced.
- `LocalizationRegistry` — locale-keyed merge of translation maps.
- `MenuItemRegistry` + `MenuItem` data class (id, icon, title, routePath?,
  action?, order).

**TDD:** uniqueness violations throw; prefix discovery returns only matching
components; localization merge is order-independent and last-write-wins per key;
menu items sort by `order`.

### A02.5 — App Shell + composition + manifest
**Port** `Composition/PluginHost.swift`, `Composition/SDKContainer.swift`,
`Plugins/PluginManifest.swift`, `Plugins/PluginRegistry.swift`,
`UI/AppShell/*`, `Plugins/SideMenu`, `BurgerMenuContainer`, `MenuHeader`,
`MenuItemButton`, `Navigator`, `MenuToolbar`, `RootRouter`.

**Deliverables**
- `PluginManifest` + `BundledPluginManifestLoader` — reads `assets/plugins.json`
  (same shape: `{plugins: {name: {enabled, version, source}}}`).
- `PluginRegistry` (main-dispatcher confined) — `register`,
  `installAll(sdk, enabled)` with **topological sort by dependencies** + **per-
  plugin error isolation** (one failing → `Error`, others continue), `activate`,
  `deactivate` (guarded by active dependents), `uninstall`. Status transitions
  exactly as iOS.
- `PluginHost` — bootstrap: take the host-provided available `Plugin` list →
  filter by manifest → register → installAll → activate. Emits `pluginError` on
  failures.
- `Navigator.resolve(path, routes, isAuthenticated, userPermissions)` →
  `Allow | RedirectToLogin | Forbidden | NotFound` (port the exact rules incl.
  `matchPrefix`).
- Compose: `AppShellView` (nav host), `SideMenu`/`BurgerMenuContainer`,
  `MenuHeader`, `MenuItemButton`, `MenuToolbar`, `AppRoot` (full app with
  plugins).
- Hilt: `PlatformSdk`, `EventBus`, `PluginRegistry`, `PluginHost` provided as
  singletons; the available-plugins list bound in `:app`.

**TDD:** `PluginRegistryTest` — topological install order; a plugin whose
dependency is missing/disabled → isolated `Error`, peers still install;
`deactivate` blocked by an active dependent; status machine transitions.
`NavigatorTest` — every resolution branch incl. prefix match + permission
forbidden. `PluginManifestLoaderTest` — parses, respects `enabled`.

### A02.6 — Example plugin (reference)
**Port** `vbwd-ios-plugin-example` to `:plugins:example`.

**Deliverables** — the canonical plugin shape (the template in
`docs/llm/PLUGIN-DEVELOPMENT.md`, Kotlin-ised):
```
plugins/example/src/main/kotlin/com/vbwd/plugin/example/
├── ExamplePlugin.kt          # Plugin impl — registration only (SRP)
├── ExampleMenuItems.kt       # menu factory
├── domain/ExampleStore.kt    # StateFlow store, nonisolated-equivalent ctor
├── domain/ExampleService.kt  # interface + impl (DIP: depends on ApiClient)
└── ui/ExampleScreen.kt · ExampleDashboardWidget.kt · ExampleProfileSection.kt
```
- Registers a route `/example`, a `DashboardExample` widget, a `ProfileExample`
  section, a store, translations, a menu item, and an event subscription
  (unsubscribed in `uninstall`).

**TDD:** `ExamplePluginContractTest` — after `install(sdk)` the route/component/
store/menu/translation are present; `uninstall` removes the subscription;
widget/section names satisfy the `Dashboard*`/`Profile*` convention.

### A02.7 — Integration
- `AppRoot` wired in `:app` with `[ExamplePlugin()]` available and
  `example: {enabled:true}` in `plugins.json`; shell renders the side menu,
  navigates to `/example`, shows the dashboard widget + profile section.
- **Verification:** Robolectric/Compose end-to-end — boot → install → menu shows
  "Example" → navigate → screen renders; toggling `enabled:false` hides it.

## Exit Gate (before any plugin sprint)

- [ ] `./gradlew check` green on `:app`, `:core`, `:plugins:example`.
- [ ] `:dependencyBoundaryCheck` green — `:plugins:example → :core` only.
- [ ] Manifest-driven install/activate works; per-plugin error isolation proven
      by test; Navigator resolution table green.
- [ ] Example plugin renders route + dashboard widget + profile section + menu
      item live in the running app.

## Principle notes

- **O/C:** the app is extended only through `PlatformSdk` seams; `:core` is never
  edited to add a plugin feature (the Android port of the core-agnosticism rule).
- **ISP/DIP:** a plugin imports `:core` and sees only `PlatformSdk` — never a
  registry or `PluginRegistry`.
- **Liskov:** every `Plugin` impl passes `ExamplePluginContractTest`-shaped
  checks; a disabled/erroring plugin must not break the host.
- **No overengineering:** registries expose exactly the methods the example
  plugin + A03/A04 consumers need; cart/checkout seams stub now, fill in A04.
