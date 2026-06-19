# vbwd-android — Port of vbwd-ios to Kotlin/Compose · Master Plan

**Created:** 2026-06-19 · **Source:** `vbwd-ios` (Swift 6 + SwiftUI, SPM) ·
**Target:** `vbwd-android` (Kotlin + Jetpack Compose + Gradle multi-module + Hilt)
**Binding eng-req:** [`_engineering_requirements.md`](./_engineering_requirements.md)

## 1. What we are porting

The iOS app is a **plugin-host platform**: a single SDK package
(`vbwd-ios-core` / `VBWDCore`, ~7,400 LOC) plus 9 plugin packages that extend it
through stable seams (routes, components, stores, i18n, menu items, events,
checkout sources, payment actions, themes). The host app wires enabled plugins
from a `plugins.json` manifest at boot.

We port **the architecture, not a literal transliteration** — every Swift idiom
maps to its idiomatic Kotlin/Compose equivalent (table §4) while preserving the
exact public contracts (the `Plugin` interface, the `PlatformSdk` facade, event
names, manifest shape, route/permission semantics) so plugin authors who know the
iOS/web SDK find a 1:1 mental model.

### Source inventory (LOC = Swift `Sources/` only)

| Package | LOC | Port target |
|---|---|---|
| `vbwd-ios-core` (VBWDCore) | 7,424 | `:core` module — Sprints **A01–A04** |
| `vbwd-ios-plugin-example` | 368 | `:plugins:example` — A02 (reference plugin) |
| `vbwd-ios-plugin-subscription` | 2,650 | `:plugins:subscription` — **A05** |
| `vbwd-ios-plugin-token-payment` | 260 | `:plugins:token-payment` — A06 |
| `vbwd-ios-plugin-stripe` | 154 | `:plugins:stripe` — A06 |
| `vbwd-ios-plugin-invoice` | 85 | `:plugins:invoice` — A06 |
| `vbwd-ios-plugin-cms` | 591 | `:plugins:cms` — A07 |
| `vbwd-ios-plugin-tarot` | 766 | `:plugins:tarot` — A08 |
| `vbwd-ios-plugin-meinchat` | 5,369 | `:plugins:meinchat` — **A09** |
| `vbwd-ios-plugin-meinchat-plus` | 1,884 | `:plugins:meinchat-plus` — A10 |

## 2. Locked technical decisions

| # | Decision | Rationale |
|---|---|---|
| D1 | **Kotlin + Jetpack Compose** | Idiomatic declarative map of SwiftUI; ViewModel/StateFlow ≈ ObservableObject/@Published. |
| D2 | **Gradle multi-module** (`:core`, `:plugins:*`, `:app`) | One Gradle module per SPM package — same boundary, same dependency arrows, CI-enforceable. |
| D3 | **Hilt** for DI | The composition root (iOS `SDKContainer`) becomes Hilt `@Module`s providing the core singletons; constructor injection everywhere. Dynamic plugin instances stay outside the Hilt graph (bootstrapped by `PluginHost`, §3). |
| D4 | **kotlinx.serialization** | Port of `Codable`; `@Serializable` data classes. |
| D5 | **OkHttp** behind the `ApiClient` port | Minimal faithful port of raw `URLSession`; no Retrofit annotations needed since the SDK exposes verb methods, not typed endpoints (ISP). |
| D6 | **Coroutines + Flow** | `async/await` → `suspend`; `@Published`/ObservableObject → `StateFlow`; the event-bus `actor` → `Mutex`-guarded state on a `CoroutineScope`. |
| D7 | **EncryptedSharedPreferences** (androidx.security.crypto) | Port of `KeychainTokenStore`; `InMemoryTokenStore` unchanged for tests. |
| D8 | **JUnit5 + MockK + Turbine + kotlinx-coroutines-test + Robolectric** | TDD stack; Compose UI via `compose.ui.test`. Pure-logic units run on the JVM (fast Red/Green), Android-coupled units via Robolectric. |
| D9 | **ktlint + detekt** as the style/lint gate | Enforces clean-code rules; the dependency-boundary check is a Gradle task (port of `boundary-lint.sh`). |
| D10 | **Version catalog** (`gradle/libs.versions.toml`) | One home for dependency versions (DRY). |

## 3. Module & dependency map

```
vbwd-android/
├── settings.gradle.kts            # includes :app, :core, :plugins:*
├── gradle/libs.versions.toml      # version catalog (DRY)
├── build-logic/                   # convention plugins (shared module config)
├── app/                           # @HiltAndroidApp host — MainActivity, AppRoot,
│   └── src/main/assets/plugins.json, vbwd_config.json
├── core/                          # :core  (com.vbwd.core) — the SDK
│   └── src/main/kotlin/com/vbwd/core/
│       ├── networking/  persistence/  domain/  session/  events/
│       ├── plugins/ (Plugin, PlatformSdk, PluginRegistry, registries/, theme/)
│       ├── composition/ (Hilt modules + PluginHost)
│       └── ui/ (Compose: appshell, login, dashboard, profile, store, checkout, billing)
└── plugins/
    ├── example/  subscription/  token-payment/  stripe/  invoice/
    └── cms/  tarot/  meinchat/  meinchat-plus/
```

**Dependency arrows (enforced by the boundary check):**
`:app → :core, :plugins:*` · `:plugins:* → :core` only ·
`:core → nothing in this repo`. A plugin→plugin edge is allowed **only** when the
importer declares the dependency in its `PluginMetadata.dependencies`
(e.g. `meinchat-plus → meinchat`, `subscription`).

**Hilt vs. dynamic plugins.** Hilt provides the *core* singletons
(`ApiClient`, `TokenStore`, `EventBus`, `PlatformSdk`, `PluginRegistry`,
`ThemeManager`, …). The host `:app` module compile-time-links every plugin module
and hands `PluginHost` the list of available `Plugin` instances; `PluginHost`
filters by the manifest's enabled set, topologically sorts by `dependencies`,
then `install → activate`. This mirrors iOS `AppRoot(plugins:)` exactly — plugins
are data to the host, not Hilt bindings.

## 4. Swift → Kotlin parity table (authoritative)

| iOS (Swift) | Android (Kotlin) |
|---|---|
| SPM package | Gradle module |
| `Plugin` protocol | `Plugin` interface (`suspend` lifecycle hooks) |
| `PluginMetadata` struct | `data class PluginMetadata` |
| `PlatformSDK` protocol | `PlatformSdk` interface |
| `PluginRegistry` (`@MainActor`) | `PluginRegistry` (main-dispatcher confined) |
| `PluginRoute` (`view: () -> AnyView`) | `PluginRoute` (`content: @Composable () -> Unit`) |
| `ComponentFactory = () -> AnyView` | `ComponentFactory = @Composable () -> Unit` |
| `ObservableObject` + `@Published` store | class exposing `StateFlow` (or `ViewModel`) |
| `createStore(id, AnyObject)` | `createStore(id, Any)` |
| `EventBus` `actor` state | `EventBus` + `Mutex`-guarded state on a scope |
| `AppEvents` constants | `object AppEvents` constants (same string values) |
| `AuthState` enum | `sealed interface AuthState` |
| `AuthSession` (`@MainActor`) | `AuthSession` exposing `StateFlow<AuthState>` |
| `APIClient` protocol | `ApiClient` interface (`suspend get/post/put/delete`) |
| `URLSessionAPIClient` | `OkHttpApiClient` |
| `Codable` | `@Serializable` (kotlinx) |
| `TokenStore` / `KeychainTokenStore` | `TokenStore` / `EncryptedTokenStore` |
| `InMemoryTokenStore` | `InMemoryTokenStore` (unchanged role) |
| `PermissionEvaluator` (wildcard) | `PermissionEvaluator` (same wildcard rules) |
| `AppTheme` protocol + `Environment(\.appTheme)` | `AppTheme` interface + `LocalAppTheme` CompositionLocal |
| `ThemeManager` (UserDefaults) | `ThemeManager` (DataStore/SharedPreferences) |
| `NotificationsSDK` (APNs token + badge) | `NotificationsSdk` (FCM token; badge via channels) |
| `plugins.json` (app bundle) | `plugins.json` (app `assets/`) — same shape |
| `vbwd_config.json` | `vbwd_config.json` (app `assets/`) — same shape |
| `swift run …TestsRunner` / XCTest | `./gradlew check` (JUnit5 + Robolectric + Compose test) |
| `boundary-lint.sh` | Gradle dependency-boundary task |

## 5. Sprint chain & build order (BLOCKING where noted)

Mirrors the **proven iOS sequence** (core foundation → plugin system → cross-cutting
UI → plugins in dependency order). Each sprint is TDD'd and gate-green before the
next starts.

| Sprint | Title | Depends on | Blocks |
|---|---|---|---|
| [A01](./A01-core-foundation.md) | Core foundation: Gradle/Hilt/CI scaffold + networking + persistence + domain + session + login | — | A02 |
| [A02](./A02-plugin-system.md) | Plugin system: contract + PlatformSdk + EventBus + registries + AppShell + example plugin | A01 | all plugins |
| [A03](./A03-theme-profile-dashboard.md) | Theme system + Profile + Dashboard + Settings | A02 | A04, plugins |
| [A04](./A04-store-billing-checkout-cart-notifications.md) | Store/Tokens + Invoices/Billing + Checkout + Cart + CheckoutSource registry + Notifications | A02, A03 | A05, A06 |
| [A05](./A05-plugin-subscription.md) | `subscription` plugin (CheckoutSource + lifecycle) | A04 | A06, A10 |
| [A06](./A06-plugins-payment.md) | Payment plugins: `token-payment`, `stripe`, `invoice` (payment actions) | A04, A05 | — |
| [A07](./A07-plugin-cms.md) | `cms` plugin | A02 | — |
| [A08](./A08-plugin-tarot.md) | `tarot` plugin | A02 | — |
| [A09](./A09-plugin-meinchat.md) | `meinchat` plugin (largest; rooms, SSE, native rich choices) | A04 | A10 |
| [A10](./A10-plugin-meinchat-plus.md) | `meinchat-plus` plugin (E2E crypto; deps meinchat + subscription) | A05, A09 | — |

**Blocking order:** `A01 → A02 → A03 → A04`, then `A05 → A06`, and
`A09 → A10`. `A07`/`A08` are independent after A02 and can run in parallel.

## 6. Cross-cutting principles (every sprint)

- **TDD-first** — failing test (characterisation of the iOS behaviour, or oracle
  for the new Kotlin contract) before code. Red → Green → Refactor.
- **SOLID/DI** — plugins see only the `PlatformSdk` interface; core depends on
  abstractions; Hilt is the single composition root for core singletons.
- **Liskov** — `InMemoryTokenStore` substitutes `EncryptedTokenStore`; a disabled
  plugin must not break callers; fakes honour the same contract; a structurally
  unsupported operation **raises**, never silently returns a false success.
- **DRY** — one home per behaviour (version catalog, `BaseModel`-equivalent
  serialization, a single composition root).
- **NO OVERENGINEERING** — narrowest faithful port; a port interface gets exactly
  the methods today's callers use; no KMP, no Retrofit typed-API layer, no extra
  abstraction the iOS source doesn't justify.
