---
title: Published artifacts
---

# Published artifacts

[← Back to index](index.md)

All artifacts are published to **GitHub Packages** under the `vbwd-platform` org,
group `com.vbwd`. Reads require a `read:packages` token — see
[Consuming the SDK](consuming-the-sdk.md).

| Artifact | Version | Repo | What it is |
|----------|---------|------|------------|
| `vbwd-android-core` | 0.1.0 | [vbwd-android-core](https://github.com/vbwd-platform/vbwd-android-core) | The SDK: networking, session, plugin system, shared UI. |
| `vbwd-android-example` | 1.0.0 | [vbwd-android-example](https://github.com/vbwd-platform/vbwd-android-example) | Reference plugin (every seam). |
| `vbwd-android-subscription` | 1.0.0 | [vbwd-android-subscription](https://github.com/vbwd-platform/vbwd-android-subscription) | Plans, subscriptions, add-ons, checkout source. |
| `vbwd-android-token-payment` | 1.0.0 | [vbwd-android-token-payment](https://github.com/vbwd-platform/vbwd-android-token-payment) | Pay from the token balance. |
| `vbwd-android-stripe` | 1.0.0 | [vbwd-android-stripe](https://github.com/vbwd-platform/vbwd-android-stripe) | Stripe Checkout redirect. |
| `vbwd-android-invoice` | 1.0.0 | [vbwd-android-invoice](https://github.com/vbwd-platform/vbwd-android-invoice) | Pay by emailed invoice. |
| `vbwd-android-cms` | 0.1.0 | [vbwd-android-cms](https://github.com/vbwd-platform/vbwd-android-cms) | Config-driven Posts browser. |
| `vbwd-android-tarot` | 0.1.0 | [vbwd-android-tarot](https://github.com/vbwd-platform/vbwd-android-tarot) | Tarot reading. |
| `vbwd-android-meinchat` | 1.1.0 | [vbwd-android-meinchat](https://github.com/vbwd-platform/vbwd-android-meinchat) | Messaging, rooms, transfers, attachments. |
| `vbwd-android-meinchat-plus` | 0.2.0 | [vbwd-android-meinchat-plus](https://github.com/vbwd-platform/vbwd-android-meinchat-plus) | E2E layer over meinchat (declared peer dep). |

The host umbrella ([vbwd-android](https://github.com/vbwd-platform/vbwd-android))
composes all of these as submodules; the example app is
[vbwd-android-app-example](https://github.com/vbwd-platform/vbwd-android-app-example).

## Gradle snippet

```kotlin
dependencies {
    implementation("com.vbwd:vbwd-android-core:0.1.0")
    implementation("com.vbwd:vbwd-android-meinchat:1.1.0")
    implementation("com.vbwd:vbwd-android-meinchat-plus:0.2.0") // also needs meinchat
}
```

## Versioning

These are fixed (non-SNAPSHOT) versions. Re-publishing a version **overwrites** it
on GitHub Packages — bump the module's `version` for a new release, or push a
`v*` tag to publish via `publish.yml`.

---

[← Back to index](index.md)
