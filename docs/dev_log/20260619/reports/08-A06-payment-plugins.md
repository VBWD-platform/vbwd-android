# Report 08 — A06 payment plugins (token-payment, stripe, invoice)

**Date:** 2026-06-19 · **Sprint:** [A06](../sprints/A06-plugins-payment.md) ·
**Modules:** `:plugins:token-payment`, `:plugins:stripe`, `:plugins:invoice`
(each → `:core` only) · **Status:** on disk, **NOT gate-checked** (JDK 26 / no
Android SDK). Three payment-method plugins — feature-complete on disk; they prove
the A04 payment-action seam end-to-end with **zero core branching** (OCP).

## What landed (port of the three iOS payment plugins)
- **token-payment** (`com.vbwd.plugin.tokenpayment`): `TokenPaymentService`
  (`fetchQuote` + `payWithTokens`), `TokenQuoteSection` (reads the new
  `LocalCheckoutInfo`), plugin registers the `PaymentMethodToken_balance` section
  + the `token_balance` payment action (instant pay → `ShowConfirmation`).
- **stripe** (`com.vbwd.plugin.stripe`): `StripePaymentService` (`createSession` +
  `checkStatus`), `StripeRedirectSection`, plugin registers `PaymentMethodStripe`
  + the `stripe` action (create session → `OpenUrl`).
- **invoice** (`com.vbwd.plugin.invoice`): `InvoiceInfoSection`, plugin registers
  `PaymentMethodInvoice` — **no** payment action (a missing handler routes the
  generic checkout straight to confirmation).

## Core addition
- `ui/checkout/CheckoutInfo` + `LocalCheckoutInfo` CompositionLocal (port of the
  iOS `checkoutInfo` environment); `CheckoutScreen` now renders the **selected**
  method's `PaymentMethod*` detail section inside it, so a section can quote the
  order amount without referencing the view model.

## Tests (TDD)
- `TokenPaymentServiceTest` / `StripePaymentServiceTest` (MockK): quote/pay,
  create-session/status.
- Per-plugin contract tests: each `install(sdk)` registers its section under the
  right `PaymentMethod{Code}` name, the payment action under the right code
  (invoice = none), and translations.
- **`PaymentActionRoutingTest`** (in `:app`, the only module that sees all three):
  with all installed, `paymentAction("token_balance"|"stripe")` resolve,
  `paymentAction("invoice")` is null, and `supportedPaymentMethodCodes()` ==
  `{token_balance, stripe, invoice}` — three providers, zero core branching.
- Wired into the app: deps + available-plugins + `plugins.json` (`token-payment`,
  `stripe-payment`, `invoice-payment` enabled).

## Validation
**Gate NOT run** (JDK 26, no Android SDK). Turn green in Studio / CI:
```bash
./gradlew check dependencyBoundaryCheck :app:assembleDebug
```
Self-review: imports ordered, lines ≤120, no broad catches, each module → `:core`
only, payment-method-code conventions consistent with the A04 `ComponentRegistry`
(`PaymentMethodToken_balance` → `token_balance`, etc.).

## Next
A07 — `cms` plugin (WebView posts browser). A08 — `tarot` plugin.
