---
title: Testing & the quality gate
---

# Testing & the quality gate

[← Back to index](index.md)

## The gate: `bin/pre-commit-check.sh`

The Android port of the backend/iOS gate. Run it from the umbrella root; it maps
the gate phases onto Gradle tasks across **all** modules (composite build) and
auto-detects a JDK 17.

```bash
bin/pre-commit-check.sh                # quick: stylecheck + unit  (the iterate loop)
bin/pre-commit-check.sh --full         # + instrumented (integration / ux / e2e)
bin/pre-commit-check.sh --unit         # unit tests only
bin/pre-commit-check.sh --lint         # stylecheck only
bin/pre-commit-check.sh --integration  # instrumented only (needs an emulator)
bin/pre-commit-check.sh --no-stylecheck # drop the stylecheck phase
bin/pre-commit-check.sh --plugin meinchat   # scope to one module
```

| Phase | Gradle tasks |
|-------|--------------|
| **stylecheck** | `ktlintCheck` + `detekt` + Android `lintDebug` + `dependencyBoundaryCheck` |
| **unit** | `testDebugUnitTest` |
| **integration** (= ux/e2e) | `connectedDebugAndroidTest` (instrumented — needs a device/emulator) |

A change is "done" only when the gate is green.

## The test stack

| Kind | Tools | Runs on |
|------|-------|---------|
| Pure-logic unit | JUnit5 + MockK + Turbine + kotlinx-coroutines-test | JVM (fast) |
| Android-coupled unit | + Robolectric | JVM (Robolectric) |
| Compose UI | `compose.ui.test` + Robolectric | JVM (Robolectric) |
| Screenshot | Roborazzi (record with `-Proborazzi.test.record=true`) | JVM |
| Instrumented | `androidx.test` | device/emulator |

## Style rules that bite

The detekt ruleset is the strict default plus a few documented deltas
(`config/detekt/detekt.yml`). Common ones:

- **No magic numbers** — extract `dp`/alpha values to `private val` tokens.
- **`MaxLineLength` 120** — wrap long lines (ktlint enforces it too).
- **`ReturnCount` ≤ 2 / `ThrowsCount` ≤ 2** — restructure to fewer returns/throws.
- **Cancellation-safe catches** — don't `if (e is CancellationException) throw e`
  (that trips `InstanceOfCheckForException`); use
  `currentCoroutineContext().ensureActive()` instead.
- **Compose functions are PascalCase** — allowed via config.

## Writing a Compose UI test

Drive a screen with a fake service and assert on `testTag`s:

```kotlin
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel5)
class MeinChatScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun `inbox shows tabs and the new-chat entry`() {
        rule.setContent {
            MeinChatScreen(/* view models backed by FakeMeinChatService */)
        }
        rule.onNodeWithTag("meinchat_tab_chats").assertExists()
        rule.onNodeWithTag("meinchat_new_chat_button").assertExists()
    }
}
```

Give your composables stable `testTag`s (the meinchat plugin tags its inbox,
conversation, tabs, input, send button, etc.) so tests — and screenshot tooling —
can target them.

## CI

`.github/workflows/android-ci.yml` runs `./gradlew check` + `dependencyBoundaryCheck`
on every push/PR. `publish.yml` publishes on a `v*` tag (see
[Consuming the SDK](consuming-the-sdk.md)).

---

Next: [MeinChat reference →](meinchat-reference.md)
