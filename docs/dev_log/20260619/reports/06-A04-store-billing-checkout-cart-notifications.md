# Report 06 — A04 Store + Billing + Checkout + Cart + Notifications

**Date:** 2026-06-19 · **Sprint:** [A04](../sprints/A04-store-billing-checkout-cart-notifications.md)
(A04.1–A04.5) · **Status:** on disk, **NOT gate-checked** (JDK 26 / no Android
SDK sandbox). **A04 feature-complete on disk:** the commerce + notifications
backbone the plugins plug into — it fills the A02 cart/checkout/notifications
stubs with their real behaviour.

## What landed (port of `vbwd-ios-core` `Domain/{Cart,CartModels,StoreModels,StoreEndpoints,TokenBundleCheckoutSource}`, `Plugins/Registries/CheckoutSourceRegistry`, `UI/{Checkout,Store,Billing}/*`, `Notifications/*`)

### A04.1 — Cart + Store domain
- `cart/Cart` (replaces the A02 stub) — `StateFlow` line-item container:
  add/increment, remove, updateQuantity (0 ⇒ remove), clear, `items(type)`,
  total/itemCount; session-scoped single source of truth.
- `cart/CartModels` — `CartItem`, `CheckoutContext`, `TokenBundle.toCartItem()`.
- `store/StoreModels` (`TokenBundle`+`CheckoutItem`, `PaymentMethod`),
  `store/CheckoutModels` (`CheckoutResult`/`CheckoutInvoice`/`CheckoutLineItem`),
  `store/StoreEndpoints`, `store/TokenBundleCheckoutSource` (the built-in source).

### A04.2 — CheckoutSource registry + payment actions
- `checkout/CheckoutSource` (replaces the A02 stub) — the rich interface
  (matches/load/lineItems/orderTotal/submit/reset/priority/summaryComponent) +
  `CheckoutSourceRegistry` (`find` = highest-priority match; register replaces
  by id). **Liskov:** a source raises for an unsupported op (tested), never a
  false success. `PlatformSdk` cart/checkout seams now back onto the real types.
- `ComponentRegistry` gained `checkoutComponents()`, `supportedPaymentMethodCodes()`,
  `paymentMethodDetail(code)` (the `Checkout*` / `PaymentMethod*` conventions).

### A04.3 — Checkout UI
- `ui/checkout/CheckoutViewModel` — finds the source, delegates load/submit,
  filters payment methods (zero-total ⇒ invoice-only; mixed billing intervals ⇒
  exclude stripe), routes the post-submit `PaymentAction`
  (ShowConfirmation / OpenUrl), `finishCheckout` clears the cart + emits the
  event. Errors surface (cancellation re-thrown), never swallowed.
- `CheckoutScreen` (Form → Confirmation), `PaymentRedirectScreen` (WebView
  intercepting the `vbwd://` callback → `completePayment`).

### A04.4 — Store + Billing UI
- ViewModels: `TokensViewModel`, `BuyTokensViewModel` (buy ⇒ add to cart),
  `InvoicesViewModel` (10/page pagination + local search), `InvoiceDetailViewModel`.
- Screens: `TokensScreen`, `BuyTokensScreen`, `CartScreen`, `InvoicesScreen`,
  `InvoiceDetailScreen`.

### A04.5 — Notifications SDK
- `notifications/NotificationsSdk` (replaces the A02 stub) + `DefaultNotificationsSdk`
  — buffers the FCM token, forwards to every `DeviceTokenSink`, replays to
  late-registering sinks; badge clamps to ≥0. Agnostic seam: **plugins** own the
  backend wire contract (a sink), core never POSTs — honouring core-agnosticism.
- DI: shared singletons `Cart`, `CheckoutSourceRegistry` (seeded with
  `TokenBundleCheckoutSource`), `NotificationsSdk`, all passed into `PluginHost`.

## Tests (TDD)
- `CartTest`, `StoreModelsTest` (JSON + `toCartItem`), `TokenBundleCheckoutSourceTest`
  (matches/load/total + submit payload to `/user/checkout`),
  `CheckoutSourceRegistryTest` (priority/replace/unregister),
  `CheckoutViewModelTest` (load → submit → no-handler-confirm / open-url /
  error paths + cart cleared), `DefaultNotificationsSdkTest` (forward/replay/clamp),
  `BuyTokensViewModelTest` (add to cart), `InvoicesViewModelTest` (search filter).
- Shared doubles: `testutil/FakeApiClient` (now returns a configurable typed
  body) + new `testutil/FakeCheckoutSource`.

## Validation
**Gate NOT run** (JDK 26, no Android SDK). Turn green in Studio / CI:
```bash
./gradlew check                       # :app, :core, :plugins:example
./gradlew dependencyBoundaryCheck
./gradlew :app:assembleDebug
```
Self-review: imports ordered (incl. CoreModule fix), lines ≤120, MagicNumber-safe
(dp/weight/limits hoisted), broad catch only in `CheckoutViewModel.submit`
(third-party payment handler — `@Suppress` + cancellation re-thrown), A02 stubs
fully replaced with no dangling references, `:core` exposes serialization/coroutines
as `api` (from A02) so the store models resolve in plugin modules.

## Divergences (intentional)
- Notifications: ported the iOS **sink** seam (core agnostic) rather than having
  core POST the token — the sprint's "relay to backend" is a plugin sink's job
  (verified via a test sink). Android app-icon badge is a no-op default
  (documented — Android has no iOS-style badge API; use notification channels).
- Stripe session-status **polling** in `completePayment` is deferred to the A06
  stripe plugin (the poll endpoint is stripe-specific); the redirect/cancel/
  success transitions are ported.
- Invoice PDF download (`getData` byte path) deferred — no screen consumes it yet.
- Store/Billing screens are not yet wired into the shell drawer (no consumer
  route until subscription/shop plugins arrive); reachable when A05+ navigate to
  them.

## Next
**A04 exit gate** in Studio/CI, then **A05 — subscription plugin** (the first
real `CheckoutSource` registrant) and **A06 — payment plugins** (the first
`PaymentAction` handlers). A07/A08 (cms/tarot) also unblocked.
