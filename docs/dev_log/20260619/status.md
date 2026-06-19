# 2026-06-19 — vbwd-android dev day status

> Forward backlog for the **Android port of vbwd-ios**. Plan authored this day.
> Binding eng-req: [`sprints/_engineering_requirements.md`](sprints/_engineering_requirements.md)
> → [`../../_engineering_requirements.md`](../../_engineering_requirements.md).

## Theme

The Android app is a greenfield port of the Swift/SwiftUI `vbwd-ios` plugin-host
platform to **Kotlin + Jetpack Compose + Gradle multi-module + Hilt**. Today we
planned the full sprint chain, then built **A01 end-to-end on disk** (A01.0–A01.4:
scaffold → networking/persistence → domain → session+login UI → integration).
The port mirrors the proven iOS build sequence: core foundation → plugin system →
cross-cutting UI → plugins in dependency order. SDK pinned to the latest stable
platform (API 36 / Android 16, AGP 8.9.1). Next rock: **A02** (plugin system) —
after the A01 gate is validated in Studio/CI (no Android SDK in this sandbox).

## Master plan

[`sprints/00-PORT-OVERVIEW.md`](sprints/00-PORT-OVERVIEW.md) — stack decisions,
module/dependency map, Swift→Kotlin parity table, full sprint chain.

## Sprints

| # | Sprint | Area | Status |
|---|--------|------|--------|
| A01 | [Core foundation](sprints/A01-core-foundation.md) | `:app` + `:core` (net/persist/domain/session/login) | 🟢 **A01 feature-complete on disk (A01.0–A01.4)** ([reports 01](reports/01-A01.0-A01.1-scaffold-and-networking-persistence.md)/[02](reports/02-A01.2-domain.md)/[03](reports/03-A01.3-A01.4-session-login-integration.md)). A01.0 scaffold; A01.1 networking+persistence; A01.2 domain (auth/profile models+services, `PermissionEvaluator`, endpoints); A01.3 session+login UI (`AuthState`/`AuthSession` StateFlow, `RootRouter`, `LoginViewModel` @HiltViewModel, `LoginScreen`/`RootView` Compose — `:core` now Compose+Hilt); A01.4 integration (`AppConfig` asset loader, `CoreModule` composition root w/ 401→signout seam, `AppRoot` wired). **Latest SDK: compileSdk/targetSdk 36 (Android 16), AGP 8.9.1.** **Build gate NOT yet run** (sandbox: JDK 26, no Android SDK → validate via Studio/CI: `./gradlew check connectedCheck dependencyBoundaryCheck :app:assembleDebug`). **Next: A02** (plugin system). |
| A02 | [Plugin system](sprints/A02-plugin-system.md) | `:core` plugins/ + `:plugins:example` | 🟢 **A02 feature-complete on disk (A02.0–A02.7)** ([report 04](reports/04-A02-plugin-system.md)). Contract (`Plugin`/`SemanticVersion`/`VersionConstraint`/`PluginError`); `EventBus`+`AppEvents`; registries (route/component/store/localization/menu); `PlatformSdk` facade + A04 stubs (Cart/CheckoutSource/NotificationsSdk); `PluginManifest`+loaders, `PluginRegistry` (topo+isolation), `PluginHost`, `Navigator`; Compose `AppShellView`; `:plugins:example` reference plugin + contract test; wired into `AppRoot`/`CoreModule` w/ `plugins.json`. `:core` serialization+coroutines promoted to `api`. **Build gate NOT yet run** (JDK 26/no SDK → Studio/CI: `./gradlew check dependencyBoundaryCheck :app:assembleDebug`). **Next: A03.** |
| A03 | [Theme + Profile + Dashboard + Settings](sprints/A03-theme-profile-dashboard.md) | `:core` | 🟢 **A03 feature-complete on disk (A03.1–A03.4)** ([report 05](reports/05-A03-theme-profile-dashboard-settings.md)). Theme system (`AppTheme`+3 themes, `ThemeRegistry`, `ThemeManager`/`ThemeStore`, `LocalAppTheme`/`VbwdTheme`); `ProfileViewModel`+`ProfileEditScreen` (renders `Profile*` sections); `DashboardWidgetLayout`+`DashboardViewModel`+`DashboardScreen` (renders `Dashboard*` widgets); `SettingsScreen` theme picker; `AppShellView` reworked w/ built-in Dashboard/Profile/Settings slots; `AppRoot` wrapped in `VbwdTheme`; DI for theme. Unit + Compose tests. **Build gate NOT yet run** (JDK 26/no SDK → Studio/CI). **Next: A04** (+ A07/A08 unblocked). |
| A04 | [Store + Billing + Checkout + Cart + Notifications](sprints/A04-store-billing-checkout-cart-notifications.md) | `:core` | 🟢 **A04 feature-complete on disk (A04.1–A04.5)** ([report 06](reports/06-A04-store-billing-checkout-cart-notifications.md)). Real `Cart` (StateFlow) + `CartItem`/`CheckoutContext`; `StoreModels`/`CheckoutModels`/`StoreEndpoints` + `TokenBundleCheckoutSource`; rich `CheckoutSource`+`CheckoutSourceRegistry` (find/priority); `CheckoutViewModel` (source→submit→payment action) + `CheckoutScreen`/`PaymentRedirect`; Tokens/BuyTokens/Cart/Invoices/InvoiceDetail VMs+screens; `NotificationsSdk`+`DefaultNotificationsSdk` (token sink/badge). A02 stubs replaced; DI singletons for cart/registry/notifications. TDD across domain/registry/VM/notifications. **Build gate NOT yet run** (JDK 26/no SDK → Studio/CI). **Next: A05/A06 (+ A07/A08).** |
| A05 | [`subscription` plugin](sprints/A05-plugin-subscription.md) | `:plugins:subscription` | 🟢 **A05 feature-complete on disk** ([report 07](reports/07-A05-subscription-plugin.md)). `:plugins:subscription` (→ :core only, no Hilt — VMs built with sdk.api in route closures). Domain (TarifPlan w/ features array|dict serializer, Subscription, AddOn, AddonSubscription, service, `SubscriptionCheckoutSource` priority 10); plugin registers /subscription[/plans|/all|/addons] + `DashboardSubscription` widget + checkout source + menu + translations; TarifPlans/Overview/AddOns/AllSubscriptions VMs+screens; wired into app + plugins.json. TDD: service (MockK), models JSON, checkout source, plugin contract, VM. **Build gate NOT yet run** (JDK 26/no SDK → Studio/CI). **Next: A06 (+ A07/A08).** |
| A06 | [Payment plugins (token/stripe/invoice)](sprints/A06-plugins-payment.md) | `:plugins:{token-payment,stripe,invoice}` | 🟢 **A06 feature-complete on disk** ([report 08](reports/08-A06-payment-plugins.md)). 3 modules (→ :core only): token-payment (`token_balance` action + quote section), stripe (`stripe` action + redirect section), invoice (section only, no action). Core gains `LocalCheckoutInfo`; `CheckoutScreen` renders the selected method's `PaymentMethod*` section. TDD: per-plugin service + contract tests + cross-plugin `PaymentActionRoutingTest` (3 providers, 0 core branching). Wired into app + plugins.json. **Build gate NOT yet run.** **Next: A07/A08.** |
| A07 | [`cms` plugin](sprints/A07-plugin-cms.md) | `:plugins:cms` | 🟢 **A07 feature-complete on disk** ([report 09](reports/09-A07-cms-plugin.md)). `:plugins:cms` (→ :core): `CmsManifest`/`CmsService` (404→fail-loud `NotFound`), `PostsBrowserViewModel` (LoadState + URL resolve), `PostsBrowserAuthBootstrap` (pure JS seed, tested), `PostsBrowserScreen` (Android WebView), `CmsPlugin` (registers `/posts`+menu only when configured). `AppConfig` gains CMS keys + `webOrigin`/`cmsArchiveUrl`. TDD across service/VM/bootstrap/contract. **Gate NOT run.** |
| A08 | [`tarot` plugin](sprints/A08-plugin-tarot.md) | `:plugins:tarot` | 🟢 **A08 feature-complete on disk** ([report 10](reports/10-A08-tarot-plugin.md)). `:plugins:tarot` (→ :core): models (limits/arcana/card/session+enums), service (limits/session/situation, surfaces `success=false`), `TarotViewModel`+`TarotScreen`, `TarotPlugin` (`/tarot`+menu). Wired into app+plugins.json. TDD: service (MockK), models JSON, VM, contract. **Gate NOT run.** |
| A09 | [`meinchat` plugin](sprints/A09-plugin-meinchat.md) | `:plugins:meinchat` | 🟢 **A09 backbone on disk** ([report 11](reports/11-A09-meinchat-plugin.md)). Domain (incl. bot `meta` vocab serializer), service, limits, retention math, in-memory cache, secure-messaging seam, token sink, bot-meta UI + inbox/conversation screens, plugin. TDD across models/limits/retention/cache/service/sink/contract. **Deferred (documented):** SSE streaming, encrypted at-rest cache, sheet UIs. **Gate NOT run.** |
| A10 | [`meinchat-plus` plugin (E2E)](sprints/A10-plugin-meinchat-plus.md) | `:plugins:meinchat-plus` | 🟢 **A10 backbone on disk** ([report 12](reports/12-A10-meinchat-plus-plugin.md)). `:plugins:meinchat-plus` (→ :core + declared `meinchat`). Pure logic tested: `Padding`, `EnvelopePacker` (CBOR), `DowngradeGuard`, `ComposerPrecheck`; device/prekey REST services; `Stub`+`Signal` secure messaging **fail-closed** (Signal crypto deferred to a real libsignal+Keystore integration, as in iOS). Boundary allowlist updated. TDD incl. one Liskov contract over both impls. **Gate NOT run.** |

**Build order (BLOCKING):** A01 → A02 → A03 → A04, then A05 → A06, and A09 → A10.
A07/A08 are independent after A02.

## Carry-forward / open

- **Decisions locked (2026-06-19):** Kotlin+Compose · Gradle multi-module · Hilt
  · kotlinx.serialization · OkHttp · Coroutines/Flow · EncryptedSharedPreferences
  · JUnit5/MockK/Turbine/Robolectric · ktlint+detekt. See overview §2.
- **Crypto library (A10):** pick the maintained Kotlin/JVM Signal lib at sprint
  start; identity keys in the Android Keystore.
- **Backend parity:** models are `@Serializable` field-for-field with the iOS
  Codable shapes against the shared backend contract — reuse the iOS
  `Fixtures/*.json` as test fixtures.
