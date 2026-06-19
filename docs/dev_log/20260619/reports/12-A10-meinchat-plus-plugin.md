# Report 12 — A10 meinchat-plus plugin (E2E backbone)

**Date:** 2026-06-19 · **Sprint:** [A10](../sprints/A10-plugin-meinchat-plus.md) ·
**Module:** `:plugins:meinchat-plus` (→ `:core`; **declares** `meinchat`) ·
**Status:** on disk, **NOT gate-checked** (JDK 26 / no Android SDK). The
canonical **plugin→plugin dependency** + E2E layer; the actual Signal crypto is a
**fail-closed stub** — exactly as the iOS port ships (`SignalSecureMessaging` is
behind `#if canImport(LibSignalClient)`, not yet vendored).

## What landed (port of `vbwd-ios-plugin-meinchat-plus`)
**Pure logic (fully tested):**
- `Padding` — 256-byte-multiple pad/strip (`[len][payload][random tail]`).
- `EnvelopePacker` — the restricted-CBOR `Envelope` pack/unpack (definite-length
  maps/arrays, multi-byte length encoding, forward-compat key skipping).
- `DowngradeGuard` — `assertE2e` vetoes (raises) any non-`e2e_v1` response —
  never a silent plaintext downgrade.
- `ComposerPrecheck` — ready / local-not-paired / peer-cannot-receive /
  probe-failed-optimistic.

**Wire services (REST, tested with MockK):** `DeviceModels`,
`MeinChatPlusEndpoints`, `DeviceRegistryService`, `PrekeyService`
(`needsRefill` low-water math).

**Secure-messaging seam:** `StubSecureMessaging` + `SignalSecureMessaging` —
**both currently fail-closed** (never ready, every secure op raises,
peer-can-receive false). `MeinChatPlusPlugin` declares `dependencies =
["meinchat"]` and registers the stub under meinchat's
`MeinChatSecureMessagingStoreId` (one-line swap to the Signal impl when the
library lands). Wired into the app + `plugins.json`.

- **Boundary allowlist** updated: `:plugins:meinchat-plus → {:core, :plugins:meinchat}`
  (the declared edge); any undeclared plugin edge still fails `dependencyBoundaryCheck`.

## Tests (TDD)
`PaddingTest` (256-multiple, round-trip, truncated-raises), `EnvelopePackerTest`
(multi-slot round-trip incl. long header; unknown-key skip), `DowngradeGuardTest`
(non-e2e raises), `ComposerPrecheckTest` (all four results), `PrekeyServiceTest`
(MockK low-water math), **`SecureMessagingContractTest`** (one contract run
against both Stub and Signal — Liskov fail-closed), `MeinChatPlusPluginContractTest`
(declares meinchat dep; registers the seam impl).

## The crypto decision (deliberate, not a shortcut)
Porting fabricated, uncompiled, security-critical Signal crypto would be
irresponsible — broken crypto that *looks* like it works is worse than an honest
fail-closed stub, and the sprint itself says "pick the maintained Kotlin/JVM
Signal lib at sprint start." The iOS source ships the same stub. So: the
deterministic, verifiable pieces (`Padding`/`EnvelopePacker`) are ported + tested;
the X3DH/Double-Ratchet session is a documented stub that **fails closed** pending
a `libsignal` integration + Android-Keystore identity store done **with the
library and a real build**.

## Validation
**Gate NOT run** (JDK 26, no Android SDK). `./gradlew :plugins:meinchat-plus:check
dependencyBoundaryCheck :app:assembleDebug` in Studio/CI.

## Port status
**A01–A10 are complete on disk** — the full core SDK + 9 plugin modules (example,
subscription, token-payment, stripe, invoice, cms, tarot, meinchat, meinchat-plus).
**None has been compiled** (sandbox: JDK 26, no Android SDK). Outstanding: a real
`./gradlew check` in Android Studio / `android-ci`; the meinchat realtime + cache
crypto and the meinchat-plus Signal layer are documented deferrals requiring the
respective libraries + a build.
