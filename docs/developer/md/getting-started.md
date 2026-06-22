---
title: Getting started
---

# Getting started

[← Back to index](index.md)

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| **JDK** | **17** | Gradle 8.x / AGP do **not** run on JDK 23+. Use Temurin/OpenJDK 17. |
| Android SDK | `compileSdk` 36, `minSdk` 26 | Set `ANDROID_HOME` or `local.properties` (`sdk.dir`). |
| Gradle | 8.13 (wrapper) | Bundled — use `./gradlew`. |

### Make `./gradlew` use JDK 17

If your shell defaults to a newer JDK, point Gradle at 17 without changing
`JAVA_HOME` for other tools — add to `~/.gradle/gradle.properties`:

```properties
org.gradle.java.home=/path/to/jdk-17
```

(On macOS with Homebrew: `brew install openjdk@17`, then the path is
`/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`.)

## Two ways to work

The platform is a **polyrepo**: `vbwd-android-core` and each plugin live in their
own GitHub repo and publish to GitHub Packages. The `vbwd-android` **umbrella**
repo composes them as git submodules + Gradle composite builds for end-to-end
development.

- **Consume the SDK** (build a solution): add the GitHub Packages repos and depend
  on `com.vbwd:vbwd-android-*`. See [Consuming the SDK](consuming-the-sdk.md).
- **Develop the platform** (work across core + plugins): clone the umbrella with
  submodules and build everything from source.

### Develop with the umbrella

```bash
git clone --recurse-submodules https://github.com/vbwd-platform/vbwd-android.git
cd vbwd-android
./gradlew :app:installDebug      # build + install the example host app
```

Because plugins/core are composite `includeBuild(...)`s, a module task is
addressed `:<included-build>:<module>:<task>`:

```bash
./gradlew :core:core:test         # core unit tests
./gradlew :meinchat:meinchat:test # one plugin's tests
./gradlew :app:assembleDebug      # the host APK
```

`:app` is the only ordinary subproject.

## The quality gate

One script runs the whole gate (the Android port of the backend/iOS
`pre-commit-check.sh`):

```bash
./bin/pre-commit-check.sh              # stylecheck + unit  (quick loop)
./bin/pre-commit-check.sh --full       # + instrumented (needs an emulator)
./bin/pre-commit-check.sh --plugin meinchat   # scope to one module
```

It auto-detects JDK 17. See [Testing & the quality gate](testing.md) for all flags.

## Configure the app

Two JSON assets drive the example host app:

- `app/src/main/assets/vbwd_config.json` — `api_base_url` (use
  `http://10.0.2.2:5000/...` to reach the host's localhost from the emulator),
  CMS keys, web origin.
- `app/src/main/assets/plugins.json` — the enable/disable manifest.

See [The host app](host-app.md) for details.

---

Next: [Architecture →](architecture.md)
