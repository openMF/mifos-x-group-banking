<!-- generated-by: kmp-viewmodel-gen -->
# feature/settings — API

<!-- kmp-viewmodel-gen:BEGIN -->

## viewmodel

`kpt.feature.settings.SettingsViewModel` — extends
`BaseViewModel<SettingsState, SettingsEvent, SettingsAction>` (`kpt.core.base.ui.viewmodel.BaseViewModel`).
Constructor deps: `UserPreferencesRepository` (`core/datastore`), `BiometricAuthenticator`
(`core-base/security`), `ChangePinRepository` (`core/data`), `AppVersionInfo` (feature-local DI
seam), `KptAnalyticsTracker` (`core/analytics`), `CrashReporter` (`core-base/observability`).

## state

`SettingsState` (`@Serializable @Immutable`):

| Field | Type | Default |
|---|---|---|
| `selectedLanguage` | `LanguageConfig` | `LanguageConfig.ENGLISH` |
| `selectedTheme` | `DarkThemeConfig` | `DarkThemeConfig.FOLLOW_SYSTEM` |
| `isBiometricEnabled` | `Boolean` | `false` |
| `isNotificationsEnabled` | `Boolean` | `true` |
| `isBiometricAvailable` | `Boolean` | `false` |
| `appVersion` | `String` | `""` |
| `buildNumber` | `String` | `""` |
| `isChangingPin` | `Boolean` | `false` |
| `pinChangeError` | `String?` (`@Transient`) | `null` |
| `pinChangeSuccess` | `Boolean` | `false` |
| `isLoggingOut` | `Boolean` | `false` (never flipped by this VM — see class KDoc) |

Type-reuse divergence: `ui.yaml` names `AppLanguage`/`AppTheme`; this VM consumes the real
`LanguageConfig`/`DarkThemeConfig` directly (`AppTheme.SYSTEM` → `DarkThemeConfig.FOLLOW_SYSTEM`).

`SettingsScreenState` (derived via `SettingsState.deriveScreenState()`): `Content`, `ChangingPin`,
`LoggingOut`.

## actions

| Variant | Payload | Emitted Event |
|---|---|---|
| `OnLanguageSelected` | `language: LanguageConfig` | — (reactive `combine()` re-emits state) |
| `OnThemeSelected` | `theme: DarkThemeConfig` | — |
| `OnBiometricToggled` | `enabled: Boolean` | — (guarded by `shouldPersistBiometricToggle`) |
| `OnNotificationsToggled` | `enabled: Boolean` | — |
| `OnChangePinTapped` | — | — |
| `OnSubmitPinChange` | `currentPin: String, newPin: String` | — |
| `OnDismissPinDialog` | — | — |
| `OnLogoutTapped` | — | `ShowLogoutDialog` |
| `Internal.PrefsLoaded` | `snapshot: SettingsPrefsSnapshot` | — |
| `Internal.PinChangeResult` | `result: NetworkResult<ChangePinResult, NetworkError>` | `NavigateToLogin` on 401 |

## events

`SettingsEvent`: `ShowLogoutDialog`, `NavigateToLogin`, `ShowSnackbar(message: String)` (declared,
currently unused — no i18n success-copy key exists yet, see class KDoc).

## di

`kpt.feature.settings.di.SettingsModule` — Koin module,
`single { AppVersionInfo(appVersion = "1.0.0", buildNumber = "1") }` (documented app-module wiring
seam) + `viewModelOf(::SettingsViewModel)`. Included in `KoinModules.kt#featureModule` via the
aliased import `GroupBankingSettingsModule` (short-name collision guard against the legacy
`kpt.feature.settings.SettingsModule`, kept alongside, unchanged).

<!-- kmp-viewmodel-gen:END -->
