# Sprint A06 — Payment plugins: `token-payment`, `stripe`, `invoice`

**Parent:** [00-PORT-OVERVIEW](./00-PORT-OVERVIEW.md) ·
**Eng-req:** [`_engineering_requirements.md`](./_engineering_requirements.md) ·
**Modules:** `:plugins:token-payment`, `:plugins:stripe`, `:plugins:invoice`
(each → `:core` only) ·
**Depends on:** A04, A05 · **Blocks:** — ·
**Source:** `vbwd-ios-plugin-token-payment` (260 LOC),
`vbwd-ios-plugin-stripe` (154), `vbwd-ios-plugin-invoice` (85).

## Goal

Port the three payment-method plugins. Each registers a **payment action**
(post-checkout routing) and a checkout section component, proving the A04
payment-action seam end-to-end. Small, near-identical shape — one sprint.

## Deliverables

### `:plugins:token-payment`  (`com.vbwd.plugin.tokenpayment`)
```
TokenPaymentPlugin.kt        # registers payment action "token" + section
TokenPaymentService.kt       # quote/charge via ApiClient
ui/TokenQuoteSection.kt      # checkout section component
```

### `:plugins:stripe`  (`com.vbwd.plugin.stripe`)
```
StripePaymentPlugin.kt       # registers payment action "stripe" + redirect section
StripePaymentService.kt      # create payment intent / session via ApiClient
ui/StripeRedirectSection.kt  # redirect (WebView) section
```

### `:plugins:invoice`  (`com.vbwd.plugin.invoice`)
```
InvoicePaymentPlugin.kt      # registers payment action "invoice" + info section
ui/InvoiceInfoSection.kt
```

## TDD checkpoints (per plugin)

- `…ServiceTest` (MockK `ApiClient`): quote/charge/create-session happy + error
  paths; errors surface.
- `…PluginContractTest`: `install(sdk)` registers the payment action under the
  right code and the checkout section; `uninstall` cleans up.
- A cross-plugin `PaymentActionRoutingTest`: with all three installed, the
  generic A04 checkout routes a completed checkout to the **correct** action by
  code (proves Open/Closed — three providers, zero core branching).

## Exit Gate

- [ ] `./gradlew check` green incl. all three modules.
- [ ] `:dependencyBoundaryCheck`: each → `:core` only.
- [ ] In the running app: at checkout, each method renders its section and a
      completed checkout routes to its payment action (Stripe redirect opens the
      WebView; invoice shows info; token shows the quote).

## Principle notes

- **O/C:** payment methods are added purely as plugins via `addPaymentAction` —
  the A04 checkout never names a provider.
- **Liskov:** every handler honours `PaymentActionHandler`; a provider that can't
  service a request raises rather than faking success.
- **DRY / no overengineering:** shared section/redirect patterns reuse core UI;
  no provider SDK pulled in that the thin REST flow doesn't need.
