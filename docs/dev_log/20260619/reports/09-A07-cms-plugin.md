# Report 09 — A07 cms plugin (WebView posts browser)

**Date:** 2026-06-19 · **Sprint:** [A07](../sprints/A07-plugin-cms.md) ·
**Module:** `:plugins:cms` (→ `:core` only) · **Status:** on disk, **NOT
gate-checked** (JDK 26 / no Android SDK). The first WebView-backed plugin — a
config-driven "Posts" browser over the host CMS embed archive.

## What landed (port of `vbwd-ios-plugin-cms`)
- `CmsEndpoints` (`embed-manifest`, URL-encoded), `domain/CmsManifest`,
  `domain/CmsService` (interface + impl; a 404 becomes a **fail-loud**
  `CmsServiceError.NotFound` — never a silent blank).
- `ui/PostsBrowserViewModel` — `LoadState` machine (Idle/Validating/Ready/
  NotConfigured/Error); resolves the archive URL (absolute verbatim, relative
  against the web origin) with a manifest/fallback chain.
- `ui/PostsBrowserAuthBootstrap` — **pure** JS string builder that seeds the
  WebView `localStorage` (`token`/`isAuthenticated`/`user`) with correct JS
  escaping (unit-tested without WebKit).
- `ui/PostsBrowserScreen` — Android `WebView` (`AndroidView`) with JS + DOM
  storage enabled; seeds the auth script on `onPageStarted`.
- `CmsPlugin` — registers `/posts` + the "Posts" menu item + translations, but
  **only when both CMS config keys are set** (Liskov: absent config ⇒ no
  registration, the menu stays clean).

## Core addition
- `AppConfig` gained `root_android_category_on_host` / `root_android_post_type_on_host`
  / `web_base_url` / `tarif_plan_root_cat_slug` + computed `webOrigin` (prefers
  `web_base_url`, else strips `/api/vN` from the API base) and `cmsArchiveUrl`.
- `CmsPlugin` is constructed by `CoreModule` from `AppConfig` (+ a token provider
  reading `AuthSession.accessToken`) — the plugin reads no Context itself.

## Tests (TDD)
- `CmsServiceTest` (MockK): manifest decode + 404 → fail-loud `NotFound`.
- `PostsBrowserViewModelTest`: missing-config → NotConfigured; relative/absolute
  URL resolution; no-archive fallback; not-ok → Error; service `NotFound` →
  NotConfigured; `resolve()` table.
- `PostsBrowserAuthBootstrapTest`: empty no-op script; token seeding; JS escaping.
- `CmsPluginContractTest`: configured → registers `/posts` + menu + translation;
  absent config → registers nothing.

## Validation
**Gate NOT run** (JDK 26, no Android SDK). `./gradlew :plugins:cms:check
dependencyBoundaryCheck :app:assembleDebug` in Studio/CI. Self-review: imports
ordered, lines ≤120, `ApiError`-specific catches, boundary = `:core` only, the
exotic-char JS escapes use `ch.code` (no literal control chars in source).

## Divergence
- `WebViewNavigator` (iOS back-button seam) not ported — Android `WebView`
  exposes `canGoBack()`/`goBack()` natively; the system back gesture suffices.

## Next: A08 (tarot).
