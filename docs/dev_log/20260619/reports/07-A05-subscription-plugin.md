# Report 07 — A05 subscription plugin

**Date:** 2026-06-19 · **Sprint:** [A05](../sprints/A05-plugin-subscription.md) ·
**Module:** `:plugins:subscription` (→ `:core` only) · **Status:** on disk,
**NOT gate-checked** (JDK 26 / no Android SDK sandbox). The subscription plugin —
the first real `CheckoutSource` registrant — is feature-complete on disk.

## Design decision
The subscription **ViewModels are plain classes** built with `sdk.api` inside the
plugin's route closures (mirroring the iOS `makeViewModel` pattern), so the module
needs **no Hilt** — same build as `:plugins:example` (compose + serialization +
`:core`). This keeps the plugin's only dependency on the platform the `PlatformSdk`
facade.

## What landed (port of `vbwd-ios-plugin-subscription`)

### Domain (`domain/`)
- `SubscriptionModels` — `TarifPlan` (with a custom `features` serializer that
  accepts **either** `["a","b"]` **or** `{"a":…}` → sorted keys, the iOS
  array|dict case), `Subscription`, `AddOn`, `AddOnInfo`, `AddonSubscription`,
  response wrappers, `TarifPlan.toCartItem()`.
- `SubscriptionEndpoints`, `SubscriptionService` (interface + `Default` impl over
  `ApiClient`; `fetchUserAddOn` raises `Http(404)` rather than a false success).
- `SubscriptionCheckoutSource` (`priority=10` — wins over token-bundle's 0):
  cart-first load + slug-driven deep-link load, builds the `plan_id` checkout
  payload.

### Plugin + UI
- `SubscriptionPlugin` (composition root) registers routes `/subscription`,
  `/subscription/plans`, `/subscription/all`, `/subscription/addons`, a
  `DashboardSubscription` widget, the `SubscriptionCheckoutSource`, menu items,
  translations, and an `auth:login` subscription (released on `uninstall`).
- `SubscriptionMenuItems`; VMs+screens: `TarifPlans` (subscribe → cart → the A04
  generic `CheckoutScreen` via a `checkoutFactory`), `SubscriptionOverview`
  (active sub + cancel), `AddOns` (add to cart), `AllSubscriptions`,
  `DashboardSubscriptionWidget`.
- Wired into the app: `:plugins:subscription` dependency, `SubscriptionPlugin()`
  in the available-plugins list, `subscription: {enabled:true}` in `plugins.json`.

## Tests (TDD)
- `SubscriptionServiceTest` (MockK): plans query-building, top-level plan,
  `fetchUserAddOn` 404-raises, cancel posts.
- `SubscriptionModelsTest`: snake_case + features array **and** object, `toCartItem`,
  status helpers.
- `SubscriptionCheckoutSourceTest`: matches (hint/slug/cart), cart-first +
  slug-driven load, `plan_id` submit payload.
- `SubscriptionPluginContractTest`: install registers the 4 routes + widget + menu
  + translations + the priority-10 checkout source; `uninstall` unsubscribes.
- `AddOnsViewModelTest`: add-to-cart line item + subscription/global partition.
- Shared `testutil` fakes (`FakeApi`, `FakeSubscriptionService`).

## Validation
**Gate NOT run** (JDK 26, no Android SDK). Turn green in Studio / CI:
```bash
./gradlew :plugins:subscription:check
./gradlew check dependencyBoundaryCheck   # :plugins:subscription → :core only
./gradlew :app:assembleDebug
```
Self-review: imports ordered, lines ≤120, MagicNumber-safe (404/orders/priority
hoisted), locale-stable price formatting (`Locale.US`), `ApiError`-specific
catches (no broad catches), `install` is `@Suppress("LongMethod")` with a reason
(single wiring point), boundary = `:core` only.

## Divergences (intentional)
- `PlanDetail` / `AddOnDetail` / `AddOnCatalogDetail` screens deferred — in iOS
  they are reached by *push* navigation inside the list screens; the Android
  flat-route shell goes plan-list → cart → generic checkout directly. Added when
  a nested-nav consumer needs them (no overengineering).
- `fetchPlans` currency/category query is built by string concatenation (no
  `URLComponents`); category slug is left unset here (the `vbwd_config.json`
  `tarif_plan_root_cat_slug` read can be wired when the catalog needs it).

## Next
**A05 exit gate** in Studio/CI, then **A06 — payment plugins**
(`token-payment`/`stripe`/`invoice`) which register the first `PaymentAction`
handlers and consume the subscription checkout. A07/A08 (cms/tarot) also ready.
