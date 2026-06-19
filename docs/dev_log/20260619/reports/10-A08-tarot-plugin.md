# Report 10 — A08 tarot plugin

**Date:** 2026-06-19 · **Sprint:** [A08](../sprints/A08-plugin-tarot.md) ·
**Module:** `:plugins:tarot` (→ `:core` only) · **Status:** on disk, **NOT
gate-checked** (JDK 26 / no Android SDK). A self-contained feature plugin —
tarot card reading with AI interpretations.

## What landed (port of `vbwd-ios-plugin-tarot`)
- `domain/TarotModels` — `@Serializable` `DailyLimits`, `Arcana` (with
  `isMajorArcana`/`suitName` helpers), `TaroCard`, `TaroSession` + the
  `CardPosition`/`CardOrientation`/`TaroSessionStatus` enums, response wrappers.
- `domain/TarotEndpoints`, `domain/TarotService` (interface + impl): daily
  limits, create session, submit situation — each surfaces a backend
  `success=false` as an error (no false success).
- `ui/Tarot` — `TarotViewModel` (load limits → create session → submit
  situation) + `TarotScreen` (limits, start-reading, cards, situation input,
  interpretation).
- `TarotPlugin` — registers `/tarot` (auth-required) + the "Tarot" menu item +
  translations. Wired into the app + `plugins.json` (`tarot`).

## Tests (TDD)
- `TarotServiceTest` (MockK): limits success + `success=false` surfaces an error;
  submit situation returns the interpretation.
- `TarotModelsTest`: snake_case + enum + nested-card JSON; arcana-type helpers.
- `TarotViewModelTest`: load → limits; create session → submit → interpretation.
- `TarotPluginContractTest`: install registers `/tarot` + menu + translations.

## Validation
**Gate NOT run** (JDK 26, no Android SDK). `./gradlew :plugins:tarot:check
dependencyBoundaryCheck :app:assembleDebug` in Studio/CI. Self-review: imports
ordered, lines ≤120, `ApiError`-specific catches, the logical-failure `Http(200)`
status hoisted to a const, boundary = `:core` only.

## Divergence / scope
- Follow-up / history / card-explanation endpoints declared in `TarotEndpoints`
  but only the three screen-driving verbs (limits/session/situation) are in the
  service interface (no overengineering — the screen's calls only).

## Where the port stands
A01–A08 are now complete on disk: the full core SDK + 8 plugin modules
(example, subscription, token-payment, stripe, invoice, cms, tarot — A09/A10
meinchat remain). **None of it has been compiled** (sandbox: JDK 26, no Android
SDK). Validation in Android Studio / the `android-ci` workflow is the
outstanding gate before relying on any of it.
