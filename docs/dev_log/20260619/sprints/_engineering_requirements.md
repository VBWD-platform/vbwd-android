# Engineering requirements — BINDING for every Android-port sprint here

This anchor inherits the canonical, binding statement at
[`../../../_engineering_requirements.md`](../../../_engineering_requirements.md)
(TDD-first · DevOps-first · SOLID · DI · DRY · Liskov · clean code ·
**NO OVERENGINEERING**, guarded by the quality gate). Every sprint file in this
directory links back to this anchor and must satisfy it in full.

## Android translation of the gate

The iOS/backend gate `bin/pre-commit-check.sh --full` maps to the Android gate:

```
./gradlew check        # = ktlint + detekt + unit tests (JVM/Robolectric) + lint, ALL modules
./gradlew :module:test # quick loop while iterating on one module
```

Done = TDD'd · `./gradlew check` green on every touched module · the slice's
oracle assertion green · SOLID/DI/DRY/Liskov/clean · not over-engineered ·
no commits unless explicitly instructed.

- No silenced findings: no unreviewed `@Suppress`, no blanket detekt baseline
  growth. Fix the root cause or ask first.
- Plugin modules depend on **`:core` only** — enforced by the dependency-boundary
  check (the Android port of the iOS `boundary-lint.sh`). A plugin importing
  another plugin without a declared `PluginMetadata.dependencies` entry fails CI.
