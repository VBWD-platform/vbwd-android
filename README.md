# vbwd-android

Kotlin + Jetpack Compose port of the `vbwd-ios` plugin-host platform.
Architecture, decisions and the full sprint chain:
[`docs/dev_log/20260619/sprints/00-PORT-OVERVIEW.md`](docs/dev_log/20260619/sprints/00-PORT-OVERVIEW.md).

## Modules

| Module | Type | Purpose |
|---|---|---|
| `:app` | Android application | Host app — Hilt root, `MainActivity`, `plugins.json` manifest |
| `:core` | Android library | The SDK (`com.vbwd.core`) — networking, domain, session, plugin system, UI |
| `:plugins:*` | Android libraries | Feature plugins (added A02+); depend on `:core` only |

## Toolchain prerequisites

- **JDK 17** (Gradle 8.13 + AGP 8.7 do **not** run on JDK 23+/26).
  Install Temurin 17 and point `JAVA_HOME` at it.
- **Android SDK** (compileSdk 35). Set `ANDROID_HOME`/`ANDROID_SDK_ROOT` or copy
  `local.properties.dist` → `local.properties` with your `sdk.dir`.

```bash
# one-time SDK bits (if using the command-line tools)
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

## Build & test (the quality gate)

```bash
./gradlew check                 # ktlint + detekt + unit tests + Android lint (ALL modules)
./gradlew dependencyBoundaryCheck   # plugins depend on :core (+ declared peers) only
./gradlew :app:assembleDebug    # build the APK
./gradlew :core:test            # quick loop on one module
```

`check` + `dependencyBoundaryCheck` are what CI runs
([`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml)) and the
Android translation of the iOS/backend `bin/pre-commit-check.sh --full` gate.

## Config

`app/src/main/assets/vbwd_config.json` (copy from `.dist`) — API base URL
(`10.0.2.2:5000` = host localhost from the emulator), CMS Posts keys, web origin.
`app/src/main/assets/plugins.json` — plugin enable/disable manifest.
