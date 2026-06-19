# Sprint A04 — Store + Billing + Checkout + Cart + Notifications

**Parent:** [00-PORT-OVERVIEW](./00-PORT-OVERVIEW.md) ·
**Eng-req:** [`_engineering_requirements.md`](./_engineering_requirements.md) ·
**Area:** `:core` (ui/store · ui/billing · ui/checkout · domain/cart ·
plugins/registries/checkoutSource · notifications/) ·
**Depends on:** A02, A03 · **Blocks:** A05 (subscription), A06 (payment plugins),
A09 (meinchat) ·
**Source:** `vbwd-ios-core` `UI/Store/*`, `UI/Billing/*`, `UI/Checkout/*`,
`Domain/Cart`, `Domain/CartModels`, `Domain/StoreModels`, `Domain/StoreEndpoints`,
`Domain/TokenBundleCheckoutSource`, `Plugins/Registries/CheckoutSourceRegistry`,
`Composition/PluginMenuItems`, `Notifications/*`.

## Goal

Port the commerce + notifications backbone the plugins plug into: the cart, the
**CheckoutSource registry** (the seam subscription/shop register into), the
generic checkout + payment-action routing, the token store/buy/billing screens,
and the notifications SDK (FCM token relay). This fills the A02 cart/checkout
stubs with their real behaviour.

## Subsprints

### A04.1 — Cart + Store domain
**Port** `Domain/Cart`, `Domain/CartModels`, `Domain/StoreModels`,
`Domain/StoreEndpoints`, `Domain/TokenBundleCheckoutSource`.

**Deliverables (`core/domain/`)**
- `Cart` — observable (`StateFlow`) line-item container; add/remove/clear/total.
  Port the iOS single-source-of-truth semantics (reset must **not** wipe the
  cart where iOS doesn't).
- `@Serializable` `CartModels` / `StoreModels`; `StoreEndpoints`.
- `TokenBundleCheckoutSource` — the built-in core `CheckoutSource`.

**TDD:** `CartTest` (add/remove/total, idempotent add, clear); JSON round-trips;
`TokenBundleCheckoutSourceTest` (builds the expected checkout payload).

### A04.2 — CheckoutSource registry + payment actions
**Port** `Plugins/Registries/CheckoutSourceRegistry`, the `PlatformSdk`
checkout/payment seams (stubbed in A02), `PaymentActionHandler`.

**Deliverables**
- `CheckoutSource` interface + `CheckoutSourceRegistry` (register/unregister by
  id, unique).
- `addPaymentAction(code, handler)` backed by `ComponentRegistry`
  (`PaymentActionHandler` = post-checkout routing closure).
- Replace the A02 `cart`/`checkoutSources`/`addCheckoutSource` stubs on
  `DefaultPlatformSdk` with the real registries.

**TDD:** registry uniqueness; payment-action dispatch by code;
**Liskov:** a source for an unsupported operation **raises**
`UnsupportedOperationError`, never returns a false success.

### A04.3 — Checkout UI
**Port** `UI/Checkout/CheckoutView`, `CheckoutViewModel`,
`CheckoutConfirmationView`, `CheckoutEnvironment`, `PaymentRedirectView`.

**Deliverables (`core/ui/checkout/`)** — generic `CheckoutScreen` driven by the
selected `CheckoutSource`; confirmation + payment-redirect (WebView) screens;
`CheckoutViewModel` orchestrating source → payment action.

**TDD:** `CheckoutViewModelTest` (source selection → charge → payment-action
routing; error paths surface, not swallow); Compose tests for confirmation +
redirect.

### A04.4 — Store + Billing UI
**Port** `UI/Store/TokensView(+VM)`, `BuyTokensView(+VM)`, `CartView`,
`UI/Billing/InvoicesView(+VM)`, `InvoiceDetailView(+VM)`.

**Deliverables (`core/ui/store`, `core/ui/billing/`)** — `TokensScreen`,
`BuyTokensScreen`, `CartScreen`, `InvoicesScreen`, `InvoiceDetailScreen` with
their ViewModels over the domain services.

**TDD:** ViewModel tests (load tokens/invoices, buy flow adds to cart, invoice
detail fetch); Compose render tests incl. empty/error states.

### A04.5 — Notifications SDK
**Port** `Notifications/NotificationsSDK`, `DefaultNotificationsSDK`.

**Deliverables (`core/notifications/`)**
- `NotificationsSdk` interface — device-token relay + badge seam.
- `DefaultNotificationsSdk` — **FCM** token registration relayed to the backend
  (port of the APNs relay); Android badge via notification channels (documented
  divergence: no app-icon badge API like iOS).
- Wire onto `PlatformSdk.notifications`.

**TDD:** `DefaultNotificationsSdkTest` (token relayed to backend endpoint;
re-registration idempotent; relay failure surfaced/retried).

## Exit Gate

- [ ] `./gradlew check` green on `:core` (+ `:plugins:example`).
- [ ] `TokenBundleCheckoutSource` checks out end-to-end (against fixtures); cart
      survives the documented nav/reset cases.
- [ ] CheckoutSource registry + payment-action dispatch proven by test.
- [ ] FCM token relay verified (instrumented or fixture).

## Principle notes

- **O/C + DIP:** plugins register `CheckoutSource`s / payment actions; core never
  names a plugin. Subscription (A05) is the first real registrant.
- **Liskov:** unsupported operations raise (the called-out iOS rule).
- **No overengineering:** checkout is generic over `CheckoutSource`; no
  per-provider branching in core — providers arrive as plugins in A05/A06.
