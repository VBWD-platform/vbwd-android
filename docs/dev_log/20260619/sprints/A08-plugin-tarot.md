# Sprint A08 — `tarot` plugin

**Parent:** [00-PORT-OVERVIEW](./00-PORT-OVERVIEW.md) ·
**Eng-req:** [`_engineering_requirements.md`](./_engineering_requirements.md) ·
**Module:** `:plugins:tarot` (→ `:core` only) ·
**Depends on:** A02 · **Blocks:** — ·
**Source:** `vbwd-ios-plugin-tarot` (766 LOC, 6 files).

## Goal

Port the tarot plugin — a self-contained feature screen with its own
service/models/store. Independent of commerce; can run in parallel with A07.

## Deliverables (`plugins/tarot/.../com/vbwd/plugin/tarot/`)

```
TarotPlugin.kt               # registers route /tarot + menu item
domain/TarotEndpoints.kt
domain/TarotModels.kt        # @Serializable
domain/TarotService.kt       # interface + impl (ApiClient)
ui/TarotScreen.kt (+VM)
```

## TDD checkpoints

- `TarotServiceTest` (MockK `ApiClient`): draw/reading request; errors surface.
- `TarotModelsTest`: JSON round-trip vs backend fixtures.
- `TarotViewModelTest`: load/draw flow, loading + error states.
- `TarotPluginContractTest`: `install(sdk)` registers `/tarot`, the menu item and
  translations; `uninstall` cleans up subscriptions.
- `TarotScreenTest` (Compose): renders a reading; empty/error states.

## Exit Gate

- [ ] `./gradlew :plugins:tarot:check` + whole-repo `check` green.
- [ ] `:dependencyBoundaryCheck`: `:plugins:tarot → :core` only.
- [ ] In the app: menu → "/tarot" → draw → reading renders.

## Principle notes

- **SRP:** `TarotPlugin` only wires; logic lives in service/store/VM.
- **No overengineering:** one service interface scoped to the screen's calls.
