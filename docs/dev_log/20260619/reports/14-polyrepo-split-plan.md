# Report 14 — Polyrepo split plan (core + per-plugin + template)

**Date:** 2026-06-19 · **Status:** plan + groundwork on disk. The physical split
is **gated on a green `./gradlew check`** (your decision). Dependency mechanism:
**GitHub Packages (Maven)** (your decision).

## Decisions
- **Core as a dep:** GitHub Packages Maven — `:core` publishes
  `com.vbwd:vbwd-android-core:<ver>`; plugins + template consume the artifact.
- **Sequencing:** split only **after** the monorepo compiles green (splitting
  11 never-compiled modules into version-coordinated repos compounds risk — one
  core API change then ripples across 11 repos + version bumps).
- **Repo creation/push is yours to run:** the safety layer hard-blocks me from
  creating/pushing to new external GitHub remotes (public or private). The
  `tools/create-android-repos.sh` script below performs the split + push; **you**
  run it (or grant a Bash permission rule).

## Target repos (all public, `dantweb/…`)
| Repo | Module | Maven coordinate | Depends on |
|------|--------|------------------|-----------|
| `vbwd-android-core` | `:core` | `com.vbwd:vbwd-android-core:0.1.0` | — |
| `vbwd-android-example` | `:plugins:example` | `…-example:1.0.0` | core |
| `vbwd-android-subscription` | `:plugins:subscription` | `…-subscription:1.0.0` | core |
| `vbwd-android-token-payment` | `:plugins:token-payment` | `…-token-payment:1.0.0` | core |
| `vbwd-android-stripe` | `:plugins:stripe` | `…-stripe:1.0.0` | core |
| `vbwd-android-invoice` | `:plugins:invoice` | `…-invoice:1.0.0` | core |
| `vbwd-android-cms` | `:plugins:cms` | `…-cms:0.1.0` | core |
| `vbwd-android-tarot` | `:plugins:tarot` | `…-tarot:0.1.0` | core |
| `vbwd-android-meinchat` | `:plugins:meinchat` | `…-meinchat:1.1.0` | core |
| `vbwd-android-meinchat-plus` | `:plugins:meinchat-plus` | `…-meinchat-plus:0.2.0` | core, **meinchat** |
| `vbwd-android-template` | `:app` | (app, not published) | core + all plugins |

Plugin source lives **only** in its own repo — excluded from core and template.

## Groundwork already on disk (carries into the split)
- `:core/build.gradle.kts` now has **`maven-publish` → GitHub Packages**
  (`com.vbwd:vbwd-android-core`, `release` variant + sources). Publish with
  `GITHUB_ACTOR` + `GITHUB_TOKEN` (PAT, `write:packages`):
  `./gradlew :core:publish`.

## Per-repo standard layout (the script generates it)
```
<repo>/
├── settings.gradle.kts          # single module; dependencyResolutionManagement adds the
│                                #   GitHub Packages maven{} for com.vbwd artifacts
├── build.gradle.kts (root)      # plugin aliases apply false
├── gradle/ (wrapper + libs.versions.toml)   # copied from the monorepo (DRY catalog)
├── gradlew · gradlew.bat
├── config/detekt/detekt.yml     # copied
├── <module>/                    # the module's src + its build.gradle.kts, with
│                                #   project(":core") rewritten to the Maven coordinate
├── README.md                    # generated (purpose, install, build, test)
├── docs/                        # generated skeleton (+ the module's own docs if any)
└── .github/workflows/ci.yml     # JDK 17 + Android SDK → ./gradlew check (unit tests)
```

## Publish/consume order (CI)
1. `vbwd-android-core` CI: `./gradlew check` then (on tag) `:core:publish`.
2. `vbwd-android-meinchat` publishes (needed by meinchat-plus).
3. Every other plugin + template: CI `./gradlew check`, resolving core (and
   meinchat for meinchat-plus) from GitHub Packages — CI provides `GITHUB_TOKEN`.

## Run it (post-green)
```bash
# 1) Get the monorepo green first:
./gradlew check
# 2) Then split + create + push every repo (creates ../vbwd-android-* dirs):
tools/create-android-repos.sh
# 3) Publish core + meinchat so dependent CI can resolve them:
#    (in vbwd-android-core, on a release tag) ./gradlew :core:publish
```

## Caveats
- The script's `project(":core")` → Maven-coordinate rewrite is a `sed` over each
  module's `build.gradle.kts`; review each generated build once.
- Consumer repos need `GITHUB_TOKEN` (read:packages) to resolve core — CI sets it;
  local dev needs `gpr.user`/`gpr.key` in `~/.gradle/gradle.properties`.
- Versions are independent per repo; bump core + re-publish before bumping a
  consumer that needs the new API.
