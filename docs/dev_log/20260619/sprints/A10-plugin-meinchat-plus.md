# Sprint A10 — `meinchat-plus` plugin (E2E encryption)

**Parent:** [00-PORT-OVERVIEW](./00-PORT-OVERVIEW.md) ·
**Eng-req:** [`_engineering_requirements.md`](./_engineering_requirements.md) ·
**Module:** `:plugins:meinchat-plus` (→ `:core`; **declares deps**
`meinchat`, `subscription`) ·
**Depends on:** A05, A09 · **Blocks:** — ·
**Source:** `vbwd-ios-plugin-meinchat-plus` (1,884 LOC, 20 files).

## Goal

Port the E2E-encryption upgrade for meinchat: device registration, Signal-style
pairing/prekeys, the real `SecureMessaging` implementation that replaces the
A09 plain-path stub, per-recipient envelope fan-out for rooms, and a downgrade
guard. This is the canonical **plugin → plugin dependency** port (declares
`meinchat` + `subscription` in `PluginMetadata.dependencies`).

## Deliverables (`plugins/meinchat-plus/.../com/vbwd/plugin/meinchatplus/`)

```
MeinChatPlusPlugin.kt              # declares deps; swaps in SignalSecureMessaging
domain/
  DeviceModels.kt · DeviceRegistryService.kt
  PrekeyService.kt · SignalPairingService.kt
  EnvelopePacker.kt · Padding.kt · ComposerPrecheck.kt
  DowngradeGuard.kt                # vetoes plain-send when E2E required (raises)
  SignalSecureMessaging.kt         # real impl of the A09 SecureMessaging seam
  StubSecureMessaging.kt           # Liskov fake / disabled-path default
  MeinChatPlusEndpoints.kt · PushRegistrationService.kt
storage/
  KeychainIdentityStore.kt → AndroidKeystoreIdentityStore.kt   # Keystore-backed
  SignalProtocolStores.kt          # session/prekey stores
notifications/
  MeinChatPlusNotificationPermissions.kt · MeinChatPlusTokenSink.kt
ui/
  PairingSheet.kt · PrekeyStatusRow.kt · RevokeDeviceButton.kt
```

- **Crypto library decision:** port the Signal protocol via a maintained Kotlin/
  JVM Signal library (e.g. `libsignal`); identity keys stored in the **Android
  Keystore** (port of `KeychainIdentityStore`). Keep the crypto behind
  `SecureMessaging` so the rest of the app is unchanged.

## TDD checkpoints

- `EnvelopePackerTest` + `PaddingTest`: deterministic pack/unpack; padding bounds.
- `DeviceRegistryServiceTest` / `PrekeyServiceTest` / `SignalPairingServiceTest`
  (MockK `ApiClient`): register device, publish/consume prekeys, pair; errors
  surface.
- `SecureMessagingContractTest` — **one contract** run against both
  `SignalSecureMessaging` and `StubSecureMessaging` (Liskov substitutability).
- `DowngradeGuardTest`: when E2E is required and a recipient lacks device keys,
  the send is **vetoed (raises)** — never silently downgraded to plain.
- Per-recipient **fan-out** test: a room message encrypts once per recipient
  device over existing pairwise sessions (port the iOS no-new-group-crypto model).
- `MeinChatPlusPluginContractTest`: declares `meinchat` + `subscription` deps;
  on install **replaces** the meinchat `SecureMessaging` seam with the Signal
  impl; `uninstall` restores the stub.

## Exit Gate

- [ ] `./gradlew :plugins:meinchat-plus:check` + whole-repo `check` green.
- [ ] `:dependencyBoundaryCheck`: the `meinchat`/`subscription` edges are
      **allowed because declared** in `PluginMetadata.dependencies`; an undeclared
      edge would fail.
- [ ] In the app: pair a device → prekeys published → a meinchat message between
      paired devices is E2E-encrypted; a recipient without keys triggers the
      downgrade guard (no silent plaintext); revoke device works.

## Principle notes

- **Liskov (twice):** `SignalSecureMessaging` and `StubSecureMessaging` satisfy
  the same contract; the `DowngradeGuard` **raises** rather than faking success
  — the exact called-out rule.
- **DIP + declared deps:** the only sanctioned plugin→plugin import path; it goes
  through `meinchat`'s public seam, not its internals.
- **No overengineering:** reuse the existing pairwise Signal sessions for room
  fan-out — no bespoke group-crypto.
