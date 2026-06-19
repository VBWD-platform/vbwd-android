# Report 05 — A03 Theme + Profile + Dashboard + Settings

**Date:** 2026-06-19 · **Sprint:** [A03](../sprints/A03-theme-profile-dashboard.md)
(A03.1–A03.4) · **Status:** on disk, **NOT gate-checked** (JDK 26 / no Android
SDK sandbox). **A03 feature-complete on disk:** the shell is themed, the
Dashboard/Profile screens render plugin `Dashboard*`/`Profile*` components via the
A02 registries, and Settings switches the theme live.

## What landed (port of `vbwd-ios-core` `Plugins/Theme/*`, `UI/Dashboard/*`, `UI/Profile/*`, `UI/AppShell/SettingsScreen`)

### A03.1 — Theme system (`core/theme/`)
- `AppTheme` interface (colour palette 1:1 with iOS) + `ClassicTheme`
  (system-adaptive), `DarkBlueTheme`, `DarkGreenTheme` (exact RGB).
- `ThemeRegistry` (OCP — built-ins + plugin registration; register replaces by
  id, per iOS).
- `ThemeManager` over a `ThemeStore` seam (`InMemoryThemeStore` +
  `SharedPrefsThemeStore`); `currentTheme: StateFlow`; unknown/absent id →
  default → Classic (Liskov).
- `LocalAppTheme` CompositionLocal + `VbwdTheme { }` wrapper feeding a Compose
  `MaterialTheme` from the selected theme (`preferredDark` overrides the system).

### A03.2 — Profile (`core/ui/profile/`)
- `ProfileViewModel` (`@HiltViewModel` over `ProfileService`): `form`/`password`
  flows, load/save/change-password + validation, dirty-tracking (save gated on
  `isDirty`), errors from `ApiError`.
- `ProfileEditScreen` — core detail fields + the plugin `Profile*` sections
  (passed in as `profileSections`, so `:core` stays decoupled from the host).

### A03.3 — Dashboard (`core/ui/dashboard/`)
- `DashboardWidgetLayout` — pure grid arithmetic (`rowCount`, `COLUMN_COUNT=2`) +
  `DashboardWidgetGrid` (chunked-row 2-column themed cards).
- `DashboardViewModel` (`@HiltViewModel`): profile summary (name/initials/email
  from the session user), permission-gated token/invoice cards via
  `PermissionEvaluator`, concurrent best-effort fetches, plugin `Dashboard*`
  widgets from the registry.
- `DashboardScreen` — header + gated cards + widget grid (empty state when none).

### A03.4 — Settings + integration
- `SettingsScreen` — theme picker bound to `ThemeManager`/`ThemeRegistry`
  (tap → persist + live re-theme).
- `AppShellView` reworked: built-in Dashboard/Profile/Settings drawer entries
  (rendered via slots) + plugin menu items; `Navigator`-resolved plugin routes.
- `AppRoot` wraps the app in `VbwdTheme`; supplies the three shell slots
  (`DashboardScreen`/`ProfileEditScreen`/`SettingsScreen` via `hiltViewModel()`).
- DI: `ThemeRegistry` + `ThemeManager` (SharedPrefs-backed) singletons;
  `MainActivity` field-injects `ThemeManager`.

## Tests (TDD)
- Unit: `ThemeRegistryTest`, `ThemeManagerTest` (persist/restore/fallback),
  `DashboardWidgetLayoutTest` (grid arithmetic), `ProfileViewModelTest`
  (load/dirty/save/validation), `DashboardViewModelTest` (initials, permission
  gating, plugin widgets, best-effort load).
- Instrumented (Compose, `connectedCheck`): `SettingsScreenTest` (theme switch
  persists + live), `VbwdThemeTest` (`LocalAppTheme` propagation).

## Validation
**Gate NOT run** (JDK 26, no Android SDK). Turn green in Studio / CI:
```bash
./gradlew check                       # :app, :core, :plugins:example
./gradlew :core:connectedCheck        # theme + settings Compose tests
./gradlew dependencyBoundaryCheck
./gradlew :app:assembleDebug          # boot → dashboard widget + profile section render; theme switch
```
Self-review: imports ordered, lines ≤120, MagicNumber-safe (dp/weight/limits
hoisted to consts), `ApiError`-specific catches (no broad catches added), the
example plugin's `DashboardExample`/`ProfileExample` now render in the real
grid/form.

## Divergences (intentional)
- Read-only `ProfileScreen` not added — no consumer (the shell uses the edit
  screen, which already satisfies "render core fields + `Profile*` sections");
  added when something needs it (no overengineering).
- `ThemeRegistry.register` **replaces** a same-id theme (iOS behaviour / plugin
  override) rather than rejecting — diverges from the sprint's "duplicate id
  rejected" wording; the iOS source is the binding reference.

## Next
**A03 exit gate** in Studio/CI, then **A04 — Store + Billing + Checkout + Cart +
Notifications** (fills the A02 stubs), and A07/A08 (cms/tarot) are unblocked.
