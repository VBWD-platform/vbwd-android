# Sprint A03 — Theme system + Profile + Dashboard + Settings

**Parent:** [00-PORT-OVERVIEW](./00-PORT-OVERVIEW.md) ·
**Eng-req:** [`_engineering_requirements.md`](./_engineering_requirements.md) ·
**Area:** `:core` (theme/ · ui/profile · ui/dashboard · ui/appshell/settings) ·
**Depends on:** A02 · **Blocks:** A04 + plugin dashboard/profile components ·
**Source:** `vbwd-ios-core` `Plugins/Theme/*`, `UI/Profile/*`,
`UI/Dashboard/*`, `UI/AppShell/SettingsScreen`, `ProfileScreen`,
`Domain/ProfileService`, `Domain/PermissionEvaluator` (already in A01).

## Goal

Port the cross-cutting UI seams that plugins extend (dashboard widgets, profile
sections) and the theme system that styles them, plus the user-facing Profile and
Settings screens. This unlocks plugins contributing `Dashboard*`/`Profile*`
components against a real, themed shell.

## Subsprints

### A03.1 — Theme system
**Port** `Theme/AppTheme`, `ThemeRegistry`, `ThemeManager`, `ThemeEnvironment`,
`ClassicTheme`, `DarkBlueTheme`, `DarkGreenTheme`.

**Deliverables (`core/plugins/theme/`)**
- `AppTheme` interface — colour/typography properties (port the iOS property
  set 1:1).
- `ClassicTheme` (system-adaptive), `DarkBlueTheme`, `DarkGreenTheme`.
- `ThemeRegistry` — OCP extension point (plugins register custom themes).
- `ThemeManager` — persisted selection via DataStore/SharedPreferences (port of
  the `UserDefaults` impl).
- `LocalAppTheme` `CompositionLocal` (port of `Environment(\.appTheme)`) + a
  `VbwdTheme { }` composable wrapper feeding a Compose `MaterialTheme` from the
  selected `AppTheme`.

**TDD:** `ThemeRegistryTest` (register/lookup, default present, duplicate id
rejected); `ThemeManagerTest` (persists + restores selection; unknown id falls
back to Classic — Liskov); a Compose test asserting `LocalAppTheme` propagates.

### A03.2 — Profile
**Port** `UI/Profile/ProfileEditView` + `ProfileViewModel`,
`UI/AppShell/ProfileScreen`.

**Deliverables (`core/ui/profile/`)**
- `ProfileViewModel` (`@HiltViewModel`) over `ProfileService` (A01).
- `ProfileEditScreen` (Compose `Form`-equivalent) rendering core fields **plus**
  `ComponentRegistry.profileComponents()` (the `Profile*` plugin sections) at the
  bottom — the extension seam.
- Read-only `ProfileScreen`.

**TDD:** `ProfileViewModelTest` (load → edit → save via service; error surfaced);
Compose test: plugin `Profile*` sections render below core fields; save disabled
while pristine.

### A03.3 — Dashboard
**Port** `UI/Dashboard/DashboardView`, `DashboardViewModel`,
`DashboardWidgetLayout`.

**Deliverables (`core/ui/dashboard/`)**
- `DashboardViewModel` (loads `DashboardModels` data via the domain services).
- `DashboardScreen` rendering `ComponentRegistry.dashboardComponents()` (the
  `Dashboard*` plugin widgets) in the 2-column grid layout (port
  `DashboardWidgetLayout`'s sizing).

**TDD:** `DashboardWidgetLayoutTest` (grid arithmetic — columns/height);
Compose test: registered `Dashboard*` widgets appear in the grid; none → empty
state.

### A03.4 — Settings
**Port** `UI/AppShell/SettingsScreen`.

**Deliverables** — `SettingsScreen` with the theme picker bound to
`ThemeManager` + `ThemeRegistry`. **TDD:** selecting a theme persists and
re-themes the shell live (Compose test).

## Exit Gate

- [ ] `./gradlew check` green on `:core` (+ `:plugins:example` still green; its
      `DashboardExample`/`ProfileExample` now render in the real grid/form).
- [ ] Theme switch persists across relaunch; all 3 themes apply.
- [ ] Dashboard + Profile render plugin-contributed components via the registries.

## Principle notes

- **O/C:** Dashboard/Profile are extended only via `ComponentRegistry` prefix
  discovery — no core edit to add a plugin widget.
- **DRY:** one `VbwdTheme` wrapper feeds Compose; themes are data.
- **No overengineering:** `AppTheme` carries exactly the properties the views
  read — no speculative design-token layer.
