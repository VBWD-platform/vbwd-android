# Sprint A01 — Core Foundation

**Parent:** [00-PORT-OVERVIEW](./00-PORT-OVERVIEW.md) ·
**Eng-req:** [`_engineering_requirements.md`](./_engineering_requirements.md) ·
**Area:** `:app` + `:core` (networking · persistence · domain · session · login) ·
**Depends on:** nothing · **Blocks:** A02 ·
**Source:** `vbwd-ios-core` `Networking/`, `Persistence/`, `Domain/`, `Session/`,
`UI/Login`, `UI/RootRouter`, `UI/RootView`.

## Goal

Stand up a buildable Kotlin/Compose/Gradle/Hilt skeleton and port the
**non-plugin SDK foundation**: the HTTP client, the secure token store, the auth
+ profile domain, the auth session state machine, and a working login screen.
**No plugin system yet** (A02). At the end of A01 the app boots, shows the login
screen, authenticates against the backend (or fixtures), and lands on a
placeholder authenticated root.

## Subsprints

### A01.0 — Gradle / Hilt / CI scaffolding *(0.5d, blocks all)*

**Deliverables**
```
settings.gradle.kts · build.gradle.kts (root) · gradle/libs.versions.toml
build-logic/                       # convention plugin: com.vbwd.android.module
app/  (@HiltAndroidApp VbwdApplication, MainActivity, empty AppRoot)
core/ (com.vbwd.core namespace, empty)
.github/workflows/android-ci.yml   # ./gradlew check
config/detekt.yml · .editorconfig (ktlint)
gradle task :dependencyBoundaryCheck  # port of boundary-lint.sh (allowlist empty)
```
**Tasks (TDD where applicable)**
- T0.1 — Root build, version catalog, convention plugin applying ktlint + detekt
  + JUnit5 + Kotlin/Compose to every module. **Green:** `./gradlew help` + `check`
  succeed on empty modules.
- T0.2 — Smoke test `CoreSmokeTest.kt` (`assertTrue(true)`) proving the JVM test
  runner compiles & runs. **Red→Green.**
- T0.3 — `:app` launches an empty `AppRoot` composable; Hilt `@HiltAndroidApp`
  wired. **Manual:** `./gradlew :app:assembleDebug` succeeds; app launches.
- T0.4 — CI workflow runs `./gradlew check`; `:dependencyBoundaryCheck` Gradle
  task exists (reads module deps, fails on illegal edges; allowlist empty).

**Exit gate:** `./gradlew check` + `:app:assembleDebug` green locally and in CI;
boundary task green.

### A01.1 — Networking + Persistence

**Port** `Networking/*` and `Persistence/*`.

**Deliverables (`core/networking/`, `core/persistence/`)**
- `HttpMethod` (enum), `ApiError` (sealed: `network`, `decoding`,
  `http(status, body)`, `unauthorized`, …), `ApiClientConfig` (data class:
  baseUrl, timeouts, default headers), `EmptyResponse` (`@Serializable object`).
- `ApiClient` interface — `suspend fun get/post/put/delete`, generic
  `reified` decode helpers via kotlinx.serialization (ISP: verbs only).
- `OkHttpApiClient` — production impl: injects bearer token from
  `AuthTokenProvider`; on `401` emits `AppEvents.authSessionExpired` (wired in
  A02 once EventBus exists — A01 uses a `TokenExpiredHandler` callback seam, DIP).
- `ApiTrafficLogger` — debug-only request/response logger (port of
  `APITrafficLogger`).
- `TokenStore` interface (`save/load/clear` access + refresh tokens) ·
  `EncryptedTokenStore` (EncryptedSharedPreferences) · `InMemoryTokenStore`
  (tests).

**TDD tasks**
- `OkHttpApiClientTest` against a `MockWebServer`: GET/POST decode happy path;
  `401 → ApiError.unauthorized` + handler fired; non-2xx → `ApiError.http`;
  malformed JSON → `ApiError.decoding`; bearer header present when token set.
- `TokenStoreContractTest` — **one parametrised contract** run against both
  `InMemoryTokenStore` and `EncryptedTokenStore` (Robolectric) → proves Liskov
  substitutability. round-trip save/load/clear; load-empty → null.

**Parity:** `URLSession` → OkHttp; `Codable` → kotlinx; `Keychain` →
EncryptedSharedPreferences. **DRY:** decode helper lives once on `ApiClient`.

### A01.2 — Domain

**Port** `Domain/*`: `AuthService`/`DefaultAuthService`,
`ProfileService`/`DefaultProfileService`, models (`AuthUser`, `UserProfile`,
`Credentials`, `LoginResponse`), `PermissionEvaluator`, configurable endpoint
structs (`AuthEndpoints`, `ProfileEndpoints`), `DashboardModels`.

**Deliverables (`core/domain/`)**
- `@Serializable` data classes for every model (field-for-field with the iOS
  Codable shapes — the backend contract is shared).
- `AuthService` interface + `DefaultAuthService(api, tokenStore, endpoints)` —
  `login(Credentials)`, `logout()`, `refresh()`, `restore()`.
- `ProfileService` interface + default impl.
- `PermissionEvaluator` — wildcard matching (`a.*` matches `a.b`); **port the
  exact iOS rules**.
- `AuthEndpoints` / `ProfileEndpoints` data classes (configurable paths,
  defaults from `vbwd_config.json`).

**TDD tasks**
- `PermissionEvaluatorTest` — table of (granted, required, expected) cases
  mirrored from the iOS suite (exact-match, wildcard, deny, empty).
- `DefaultAuthServiceTest` (MockK `ApiClient` + `InMemoryTokenStore`):
  login persists tokens + returns `AuthUser`; failed login surfaces `ApiError`
  (not a false success — Liskov); `restore()` with stored token → user; logout
  clears store.
- JSON round-trip tests for each model against the `Fixtures/*.json` copied from
  `vbwd-ios-core/Fixtures/` (auth_user, login_success, login_error).

### A01.3 — Session + Login UI

**Port** `Session/AuthState`, `Session/AuthSession`, `UI/Login/*`,
`UI/RootRouter`, `UI/RootView`.

**Deliverables (`core/session/`, `core/ui/login/`, `core/ui/`)**
- `AuthState` `sealed interface`: `SignedOut`, `Authenticating`,
  `Authenticated(user)`, `Error(message)`.
- `AuthSession` — holds `StateFlow<AuthState>`; `signIn(Credentials)`,
  `signOut()`, `start()/restore()`. State machine identical to iOS
  (launch → restore → authenticated|signedOut).
- `RootRouter` — pure function `AuthState → RootRoute` (testable, no Compose).
- `LoginViewModel` (Hilt `@HiltViewModel`) over `AuthSession`.
- `LoginScreen` (Compose) + `RootView` (Compose) switching on `AuthState`.

**TDD tasks**
- `AuthSessionTest` (Turbine + coroutines-test): emits
  `SignedOut → Authenticating → Authenticated` on success;
  `… → Error` on failure; `signOut → SignedOut`; `restore` paths.
- `RootRouterTest` — every `AuthState` maps to the right `RootRoute`.
- `LoginViewModelTest` — invalid input guarded; submits credentials; surfaces
  error message.
- `LoginScreenTest` (Compose UI test) — fields render; submit disabled when
  empty; error text shows on `Error`.

### A01.4 — Integration

**Deliverables**
- `AppRoot` shows `LoginScreen` when signed out, a placeholder
  `AuthenticatedRootPlaceholder` when authenticated (real shell arrives in A02).
- Hilt graph wires `ApiClient`, `TokenStore`, `AuthService`, `ProfileService`,
  `AuthSession` as singletons (the composition root — port of `SDKContainer`).
- `vbwd_config.json` + `vbwd_config.json.dist` + fixtures in `app/src/main/assets/`.

**TDD/verification**
- `AppBootTest` (Robolectric) — cold start → `SignedOut` → `LoginScreen`.
- Manual: real login against the backend lands on the placeholder root; relaunch
  restores the session (token persisted).

## Exit Gate (all must hold before A02)

- [ ] `./gradlew check` green on `:app` + `:core` (ktlint + detekt + unit +
      Robolectric + Compose tests + Android lint).
- [ ] `:dependencyBoundaryCheck` green.
- [ ] App boots → login → authenticate (backend or fixtures) → placeholder root;
      session restores on relaunch.
- [ ] Every ported type has a test; no `@Suppress` without a written reason.

## Principle notes

- **DIP/DI:** core depends on `ApiClient`/`TokenStore`/`AuthService` interfaces;
  Hilt modules are the only place naming concrete types (the `SDKContainer`
  port). The `401` handler is a callback seam in A01, replaced by the EventBus in
  A02 — no premature event layer (**no overengineering**).
- **Liskov:** the one `TokenStoreContractTest` runs against both impls.
- **SRP:** Service = API, ViewModel = mediate, Screen = render, Session = state.
