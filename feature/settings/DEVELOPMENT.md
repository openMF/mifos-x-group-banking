<!-- generated-by: kmp-viewmodel-gen -->
# feature/settings — Development

<!-- kmp-viewmodel-gen:BEGIN -->

## 1. Module Identity

`:feature:settings` — the group-banking `settings-screen` (language, appearance, security,
notifications, about, logout). Namespace `kpt.feature.settings`. Source of
truth: `idea-layer/screens/settings/{ui,data-flow}.yaml`.

**Coexistence note:** this Gradle module ALSO still contains the legacy, unmigrated
`kpt.feature.settings` package (a generic Money-Toolkit template settings screen — theme-brand/
dynamic-color picker only, still wired into `KoinModules.featureModule` as `SettingsModule`). The
two packages are independent and additive; see `SettingsViewModel.kt` class KDoc for the full
"idea-layer truth over source modules" rationale. The legacy Screen/Route migration is out of this
ViewModel-only generation pass's scope.

## 2. Public API

See `SettingsViewModel.kt` class/field KDoc for the full ViewModel contract (`SettingsState`,
`SettingsEvent`, `SettingsAction`, `SettingsScreenState`, `AppVersionInfo`) — not re-documented here
to avoid drift. See API.md#viewmodel / #state / #actions / #events / #di.

## 3. Consumers

- `feature/settings/src/commonTest/.../SettingsViewModelTest.kt`.
- `SettingsRoute.kt` / `SettingsScreen.kt` — NOT YET generated for this package (pending
  `kmp-screen-gen`); the legacy `kpt.feature.settings.SettingsRoute`/`SettingsScreen` still serve
  the app-level nav graph today.

## 4. Boundaries

Reads/writes exclusively via injected `UserPreferencesRepository` (`core/datastore`),
`BiometricAuthenticator` (`core-base/security`), and `ChangePinRepository` (`core/data`) — no
direct `Settings`/DataStore/HTTP access from this module. `business_logic.kind: crud` per
`ui.yaml` — preference reads/writes are plain `multiplatform-settings`-backed flows; PIN change is
the only network write (`NetworkResult`-wrapped, no Store5).

## 5. Data

See API.md#state / #actions / #events for the ViewModel field/payload schema.

## 6. Errors

No `SettingsError` sealed taxonomy — `ui.yaml#state_model.errors` declares 2 flat message keys
(`error_pin_change`, `error_auth`) surfaced directly via `SettingsState.pinChangeError` (a plain
`String?`) and `SettingsEvent.NavigateToLogin` (session-expiry redirect) respectively.

## 7. Testing

`feature/settings/src/commonTest/kotlin/org/mifos/groupbanking/feature/settings/SettingsViewModelTest.kt`
— 14 tests (init/combine fan-in, language/theme persistence, biometric guard [+2 pure-predicate
tests documenting the `BiometricAuthenticator` test-seam gap], notifications persistence, PIN-change
dialog lifecycle incl. 400/401/success, logout event emission).

## 8. Observability

See `SettingsViewModel.kt` KDoc (Kermit `Logger.i/w` + `CrashReporter` breadcrumbs on mount and on
PIN-change failure + `KptAnalyticsTracker.trackLogin(method = "pin_change", ...)` reuse on every
PIN-change result).

## 9. Evolution

- 2026-07-22 — `kmp-viewmodel-gen`: `SettingsViewModel.kt` (State/ScreenState/Event/Action/VM,
  `AppVersionInfo` DI seam, `shouldPersistBiometricToggle` pure guard predicate),
  `di/SettingsModule.kt`, `SettingsViewModelTest.kt` (14 tests, TDD-first). Additive
  `UserPreferencesRepository.observeNotificationsEnabled`/`setNotificationsEnabled` +
  `UserData.enableNotifications` (`core/datastore`, `core/model`) — no existing signature changed.
  `KoinModules.kt` patched: `GroupBankingSettingsModule` (aliased import) added to
  `featureModule.includes` alongside the legacy `SettingsModule`, which is kept as-is. Flagged gaps:
  `AppVersionInfo` app-module wiring seam, legacy-shell Screen/Route migration, `isLoggingOut`
  cross-feature bridging with `settings-logout-dialog`, `BiometricAuthenticator` test-seam (all
  RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1).

<!-- kmp-viewmodel-gen:END -->
