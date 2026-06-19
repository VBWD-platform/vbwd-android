# Sprint A05 — `subscription` plugin

**Parent:** [00-PORT-OVERVIEW](./00-PORT-OVERVIEW.md) ·
**Eng-req:** [`_engineering_requirements.md`](./_engineering_requirements.md) ·
**Module:** `:plugins:subscription` (→ `:core` only) ·
**Depends on:** A04 · **Blocks:** A06 (payment plugins consume it), A10 ·
**Source:** `vbwd-ios-plugin-subscription` (2,650 LOC, 20 files).

## Goal

Port the subscription plugin — the first **CheckoutSource** registrant and the
largest commerce plugin. It contributes tarif-plan + add-on browsing, the
subscription overview, a dashboard widget, and a `SubscriptionCheckoutSource`
into the A04 registry.

## Deliverables (`plugins/subscription/.../com/vbwd/plugin/subscription/`)

```
SubscriptionPlugin.kt              # registration only (SRP)
SubscriptionMenuItems.kt
domain/
  SubscriptionEndpoints.kt         # configurable paths
  SubscriptionModels.kt            # @Serializable plan/addon/subscription
  SubscriptionService.kt           # interface + impl (DIP over ApiClient)
  SubscriptionCheckoutSource.kt    # registers into CheckoutSourceRegistry (A04)
ui/
  TarifPlansScreen.kt (+VM) · PlanDetailScreen.kt (+VM)
  AddOnsScreen.kt (+VM) · AddOnDetailScreen.kt (+VM) · AddOnCatalogDetailScreen.kt
  SubscriptionOverviewScreen.kt (+VM) · AllSubscriptionsScreen.kt (+VM)
  DashboardSubscriptionWidget.kt   # Dashboard* component
```

## TDD checkpoints (Red → Green)

- `SubscriptionServiceTest` (MockK `ApiClient`): list plans/add-ons, fetch
  detail, create subscription; errors surface (no false success).
- `SubscriptionModelsTest`: JSON round-trip vs the backend contract fixtures.
- `SubscriptionCheckoutSourceTest`: registered with a unique id; builds the
  correct checkout payload; an unsupported op **raises** (Liskov).
- `SubscriptionPluginContractTest`: after `install(sdk)` the routes
  (`/tarifs`, `/addons`, `/subscriptions`, …), the `DashboardSubscription`
  widget, the menu items, translations and store are present; `uninstall`
  unsubscribes.
- ViewModel tests for each list/detail screen (load, select, error states).
- Compose render tests for the catalog + overview screens incl. empty states.

## Exit Gate

- [ ] `./gradlew :plugins:subscription:check` + whole-repo `check` green.
- [ ] `:dependencyBoundaryCheck`: `:plugins:subscription → :core` only.
- [ ] In the running app: menu shows the subscription items; plan list →
      detail → checkout via `SubscriptionCheckoutSource` reaches the A04 generic
      checkout; dashboard widget renders.

## Principle notes

- **O/C + DIP:** extends the app only via `PlatformSdk` (route/component/menu/
  checkout-source seams); imports `:core` only.
- **Liskov:** `SubscriptionCheckoutSource` honours the `CheckoutSource` contract;
  unsupported operations raise.
- **No overengineering:** one `SubscriptionService` interface with exactly the
  methods the screens call.
