<!-- generated-by: kmp-viewmodel-gen -->
# feature/login-signup — Development

## 1. Module Identity

`:feature:login-signup` — unified authentication entry screen (self-registration + credential
login + biometric re-auth + zero-group onboarding). Namespace `kpt.feature.loginsignup`.
Source of truth: `idea-layer/screens/login-signup/{ui,api,docs,flow,data-flow}.yaml`.

<!-- kmp-viewmodel-gen:BEGIN -->
## 2. Public API

**ViewModel constructor deps** (`LoginSignupViewModel`):
- `authRepository: AuthRepository` (`core/data`) — session read (`currentSession`) + `login` / `selfRegister` / `refreshSession` / `clearSession`.
- `analytics: KptAnalyticsTracker` (`core/analytics`).
- `crashReporter: CrashReporter` (`core-base/observability`).

**State fields** (`LoginSignupState`): `mode`, `name`, `emailPhone`, `password`, `pin`,
`isSubmitting`, `validationErrors`, `error` (`@Transient`), `isBiometricAvailable`,
`sessionToken`, `groupMemberships` (`@Transient`), `screenState` (`@Transient`).

**Action variants** (`LoginSignupAction`, 11 — verbatim mirror of `ui.yaml#state_model.actions.members`):
`OnModeToggle`, `OnNameChange`, `OnEmailPhoneChange`, `OnPasswordChange`, `OnPinChange`,
`OnLoginTap`, `OnSignupTap`, `OnBiometricUnlock`, `OnCreateGroupTap`, `OnJoinWithCodeTap`,
`OnForgotPassword` — plus the sanctioned `Internal` async-result sub-interface
(`SessionChecked`, `LoginResult`, `SignupResult`, `BiometricResult`).

**Event variants** (`LoginSignupEvent`): `NavigateToPersonalDashboard`, `NavigateToGroupList`,
`NavigateToGroupTypePicker`, `NavigateToJoinWithCode`, `ShowSnackbar(message)`, `PromptBiometric`.

## 3. Consumers

- `{F}Route.kt` in `cmp-navigation` (not yet generated — pending `kmp-screen-gen`).
- `feature/login-signup/src/commonTest/.../LoginSignupViewModelTest.kt` (19 tests, dispatches every Action).

## 4. Boundaries

Reads/writes exclusively via the injected `AuthRepository` (`core/data`) — no direct
`CompanionAuthApi`/`CompanionSessionStore`/DataStore/HttpClient access from this module.
No Store5 (`business_logic.kind: processor` — a write/session-read mutation flow per
RULE-IMPLEMENT-STORE5-001, so `NetworkResult` is consumed directly, not `.asScreenStream()`).

## 5. Data

See API.md#state / #actions / #events for the full field/payload schema.

## 6. Errors

`LoginSignupError` (6 variants, verbatim mirror of `ui.yaml#state_model.errors.types`):
`InvalidCredentials`, `AccountExists`, `WeakPassword`, `Network`, `Server`, `BiometricFailed`.
Every submit/refresh failure path sets `LoginSignupState.error` + `screenState = Error` and
emits a `crashReporter.recordMessage(..., CrashSeverity.Warning)` breadcrumb.

## 7. Testing

`feature/login-signup/src/commonTest/kotlin/org/mifos/groupbanking/feature/loginsignup/LoginSignupViewModelTest.kt`
— 19 `@Test` functions covering initial state, every field-change action, both submit forms
(validation-block + success + error per mode), mount-time session/biometric detection, both
biometric outcomes, both zero-groups navigate actions, forgot-password, and PIN entry.

## 8. Observability

Kermit `Logger.i/w(TAG) { ... }` (`TAG = "LoginSignupViewModel"`) — PII-safe (logs counts /
error names only, never raw `emailPhone`/`password`/`name`). `KptAnalyticsTracker.trackLogin`
fires on every login/signup/biometric attempt (method + success + errorCode).
`KptAnalyticsTracker.trackGroupOperation` fires on the two zero-groups CTA taps.
`crashReporter.setUser(session?.userId)` on every session-state emission;
`crashReporter.recordMessage` breadcrumb on mount + on every auth failure.

## 9. Evolution

- 2026-07-21 — `kmp-viewmodel-gen`: initial `LoginSignupViewModel` (MVI) + State/Event/Action +
  Koin module + 19-test suite. Flagged 3 idea-layer/training drifts in the generation report
  (see PR/task notes): (a) `KptAnalyticsTracker` ships no `trackOperation()`/`trackError()`
  despite `CORE_ANALYTICS.md` documenting them; (b) `KptAnalyticsTracker` had zero Koin
  registration anywhere in the codebase — added locally in `LoginSignupModule.kt`; (c)
  `core-base/observability`'s `observabilityModule` was never included in
  `KoinModules.allModules` — added.
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## 2b. Screen / UI layer

- `LoginSignupScreen.kt` — Container (`LoginSignupScreen`) + stateless Content
  (`LoginSignupContent`), state-driven over all 4 `LoginSignupScreenState` members.
- `LoginSignupRoute.kt` — `@Serializable data object LoginSignupRoute` (`/auth`) +
  `navigateToLoginSignup()` + `NavGraphBuilder.loginSignupScreen(...)` (4 required nav
  callbacks, no dead-clickable defaults).
- `components/` — `AuthBrandHeader`, `AuthModeToggleTabs`, `AuthTextField`,
  `AuthErrorBanner`, `AuthDividerLabeled`, `ZeroGroupsEmptyState`.
- `LoginSignupTestTags.kt` — 15 append-only test tags.
- `LoginSignupScreenPreview.kt` — 4 `@Preview` functions covering 5 state variants.
- `composeResources/values/strings.xml` — 42 `screens_login_signup_*` keys (zero hardcoded
  UI literals — RULE-IMPL-NO-HARDCODED-STRING-001).

## 3b. Consumers (screen layer)

- `LoginSignupRoute.kt#loginSignupScreen(...)` — NOT YET wired into the app-level nav graph
  (`cmp-navigation`); the unauthenticated root graph (`personal-dashboard` / `group-list` /
  `group-type-picker` / `join-with-code` destinations) does not exist yet in this codebase.
  Wiring is out of scope for `kmp-screen-gen` (feature-scoped) — tracked as an infra-layer
  follow-up once those destination features are generated.
- `feature/login-signup/src/commonMain/kotlin/.../LoginSignupScreenPreview.kt`.

## Evolution (screen layer)

- 2026-07-22 — `kmp-screen-gen`: `LoginSignupScreen.kt` (Container+Content, all 4
  `screenState` branches), 6 reusable components, `LoginSignupRoute.kt`, `LoginSignupTestTags`
  (15 tags), `LoginSignupScreenPreview.kt` (4 `@Preview`, 5 state variants), and
  `strings.xml` (42 keys, zero hardcoded literals). Resolved one design ambiguity: the
  Stitch preview's decorative tagline ("Save together. Lend together. Grow together.") is
  not declared in `ui.yaml#i18n` or `#components`, so it was omitted rather than invented.
<!-- kmp-screen-gen:END -->
