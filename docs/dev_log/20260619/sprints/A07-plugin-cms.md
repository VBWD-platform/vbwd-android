# Sprint A07 — `cms` plugin

**Parent:** [00-PORT-OVERVIEW](./00-PORT-OVERVIEW.md) ·
**Eng-req:** [`_engineering_requirements.md`](./_engineering_requirements.md) ·
**Module:** `:plugins:cms` (→ `:core` only) ·
**Depends on:** A02 (independent of commerce) · **Blocks:** — ·
**Source:** `vbwd-ios-plugin-cms` (591 LOC, 8 files).

## Goal

Port the CMS "Posts" browser — a config-driven WebView over the host-rendered
chrome-light CMS archive (the iOS S91 design: one web renderer ⇒ every post type
works, no per-type native code). First WebView-backed plugin.

## Deliverables (`plugins/cms/.../com/vbwd/plugin/cms/`)

```
CmsPlugin.kt                 # registers route /posts + menu item "Posts"
CmsEndpoints.kt
domain/CmsManifest.kt        # fail-loud embed-manifest contract
domain/CmsService.kt         # interface + impl (ApiClient)
ui/PostsBrowserScreen.kt (+VM)      # Android WebView (port PostsBrowserView)
ui/PostsBrowserAuthBootstrap.kt     # seeds JWT into the WebView (access-gated posts)
ui/WebViewNavigator.kt              # nav delegate (port WebViewNavigator)
```

- Device config keys (`vbwd_config.json`): `root_android_category_on_host` +
  `root_android_post_type_on_host` (port of the iOS `root_ios_*` keys) selecting
  the `category × post-type` archive to render.

## TDD checkpoints

- `CmsServiceTest` (MockK `ApiClient`): resolves the embed URL from manifest +
  config; **fail-loud** when the embed manifest is missing (raises, no silent
  blank — Liskov).
- `PostsBrowserViewModelTest`: builds the chrome-light URL from
  category/post-type config; auth-bootstrap injects the JWT header/cookie.
- `WebViewNavigatorTest`: in-archive nav allowed; external links handled per the
  iOS rules.
- `CmsPluginContractTest`: `install(sdk)` registers `/posts` + the "Posts" menu
  item + translations.

## Exit Gate

- [ ] `./gradlew :plugins:cms:check` + whole-repo `check` green.
- [ ] `:dependencyBoundaryCheck`: `:plugins:cms → :core` only.
- [ ] In the app: "Posts" opens the WebView over the configured archive;
      access-gated posts load with the seeded JWT; every post type (page/post/
      video/pdf) renders through the single web renderer.

## Principle notes

- **No overengineering:** exactly one WebView + an `embed` render mode — no
  per-post-type native screens (the explicit iOS decision).
- **Fail-loud (Liskov):** a missing embed manifest raises, never renders blank.
