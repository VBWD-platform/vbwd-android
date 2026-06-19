# Report 13 — Roborazzi screenshot tests (real screens)

**Date:** 2026-06-19 · **Status:** wired on disk, **NOT run** (JDK 26 / no
Android SDK sandbox — Roborazzi builds against the Android SDK even though it
renders on the JVM). Screenshot tests render the **real** Compose screens to PNGs
with no emulator.

## Why Roborazzi (not instrumented E2E)
This sandbox has no Android SDK, no `adb`, no emulator (verified). Roborazzi
renders Compose under **Robolectric on the JVM** and writes golden PNGs — so the
screenshots run in plain unit tests / CI, no device, no flakiness. (True
instrumented E2E would need an emulator + a built APK, neither available here.)

## What was wired
- Catalog: `roborazzi` (1.32.0) libs + plugin, `junit-vintage-engine` (so the
  JUnit4/Robolectric screenshot tests run on the existing JUnit5 platform).
- Root `build.gradle.kts`: `roborazzi` plugin `apply false`.
- `:core`, `:plugins:{tarot,subscription,meinchat}`: apply the roborazzi plugin
  + the test deps (robolectric, compose-ui-test-junit4/-manifest, roborazzi*).

## The screenshots (real screens, themed via `VbwdTheme`)
- **`:core`** `CoreScreenshotTest` → `login`, `settings`, `profile`, `dashboard`,
  `checkout` — each constructs the real screen + view model with small demo
  fakes (`DemoAuthService`/`DemoProfileService`, a `PluginHost` with a demo
  `Dashboard*` widget, a `FakeCheckoutSource` line item).
- **`:plugins:tarot`** `TarotScreenshotTest` → `tarot` (the real `TarotScreen`).
- **`:plugins:subscription`** `SubscriptionScreenshotTest` → `tarif_plans`.
- **`:plugins:meinchat`** `MeinChatScreenshotTest` → `bot_cards` (the native
  `BotMetaContent`: choice buttons + a bot cart).

## How to generate them (in Studio / CI, with the SDK)
```bash
# JDK 17 + Android SDK on PATH. Record the golden PNGs:
./gradlew recordRoborazziDebug
#   → core/screenshots/{login,settings,profile,dashboard,checkout}.png
#   → plugins/tarot/screenshots/tarot.png
#   → plugins/subscription/screenshots/tarif_plans.png
#   → plugins/meinchat/screenshots/bot_cards.png

# Thereafter, fail the build on visual diffs:
./gradlew verifyRoborazziDebug      # compares against the committed goldens
```

## Caveats (must validate on first real build)
- **Compile first.** These sit on top of 10 never-compiled sprints — get
  `./gradlew check` green before recording, or compile errors surface here too.
- **Pin the Roborazzi version** to whatever's current (1.32.0 is a placeholder);
  it must match the Compose/AGP in use.
- Async-loading screens (Dashboard/Profile/Tarif/Tarot) capture after their
  `LaunchedEffect` settles under Robolectric's scheduler; if a capture looks
  empty, add an idle/advance or seed the state synchronously.
