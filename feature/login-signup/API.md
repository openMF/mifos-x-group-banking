<!-- generated-by: kmp-viewmodel-gen -->
# feature/login-signup — API

<!-- kmp-viewmodel-gen:BEGIN -->
## viewmodel

`kpt.feature.loginsignup.LoginSignupViewModel` — extends
`BaseViewModel<LoginSignupState, LoginSignupEvent, LoginSignupAction>`
(`kpt.core.base.ui.viewmodel.BaseViewModel`).

## state

`LoginSignupState` — `@Serializable @Immutable data class`.

| Field | Type | Default |
|---|---|---|
| `mode` | `AuthMode` | `AuthMode.Login` |
| `name` | `String` | `""` |
| `emailPhone` | `String` | `""` |
| `password` | `String` | `""` |
| `pin` | `String` | `""` |
| `isSubmitting` | `Boolean` | `false` |
| `validationErrors` | `Map<String, String>` | `emptyMap()` |
| `error` (`@Transient`) | `LoginSignupError?` | `null` |
| `isBiometricAvailable` | `Boolean` | `false` |
| `sessionToken` | `String?` | `null` |
| `groupMemberships` (`@Transient`) | `List<GroupMembership>` | `emptyList()` |
| `screenState` (`@Transient`) | `LoginSignupScreenState` | `Content` |

`LoginSignupScreenState`: `Content` · `Loading` · `Error` · `ZeroGroups`.
`LoginSignupError`: `InvalidCredentials` · `AccountExists` · `WeakPassword` · `Network` ·
`Server` · `BiometricFailed` — each carries `retry: Boolean` + `messageKey: String`.

## actions

| Action | Payload | Emitted Event | `handleAction` effect |
|---|---|---|---|
| `OnModeToggle` | `mode: AuthMode` | — | resets form fields, clears errors, switches mode |
| `OnNameChange` | `value: String` | — | `state.name` transform + clears `validationErrors["name"]` |
| `OnEmailPhoneChange` | `value: String` | — | `state.emailPhone` transform + clears its error |
| `OnPasswordChange` | `value: String` | — | `state.password` transform + clears its error |
| `OnPinChange` | `value: String` | — | digit-filtered `state.pin` transform |
| `OnLoginTap` | — | `NavigateToPersonalDashboard` \| `NavigateToGroupList` \| none (→ZeroGroups) | validates, calls `authRepository.login`, routes on `groupMemberships` |
| `OnSignupTap` | — | same set as `OnLoginTap` | validates, calls `authRepository.selfRegister` |
| `OnBiometricUnlock` | — | none (state error) if no token, else routes like `OnLoginTap` | calls `authRepository.refreshSession(token)` |
| `OnCreateGroupTap` | — | `NavigateToGroupTypePicker` | pure navigate |
| `OnJoinWithCodeTap` | — | `NavigateToJoinWithCode` | pure navigate |
| `OnForgotPassword` | — | `ShowSnackbar` | pure event |

`Internal` (async-result routing, never UI-dispatched): `SessionChecked(session)`,
`LoginResult(result)`, `SignupResult(result)`, `BiometricResult(result)`.

## events

`NavigateToPersonalDashboard` · `NavigateToGroupList` · `NavigateToGroupTypePicker` ·
`NavigateToJoinWithCode` · `ShowSnackbar(message: String)` · `PromptBiometric`.

## di

`kpt.feature.loginsignup.di.LoginSignupModule` — Koin module.
Included via `cmp-navigation/.../KoinModules.kt#featureModule.includes(...)`.
Declares `single { KptAnalyticsTracker(analyticsHelper = get()) }` (the tracker had no
Koin registration anywhere in the codebase prior to this generation) plus
`viewModelOf(::LoginSignupViewModel)`. `AuthRepository` resolves from `DataModule`/
`RepositoryModule`; `CrashReporter` resolves from `core-base/observability`'s
`observabilityModule` (newly added to `KoinModules.allModules`).
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## screen

`LoginSignupScreen.kt` (`kpt.feature.loginsignup`) — Container + Content split:

- **Container** `internal fun LoginSignupScreen(onNavigateToPersonalDashboard, onNavigateToGroupList, onNavigateToGroupTypePicker, onNavigateToJoinWithCode, modifier, viewModel = koinViewModel())` — collects `viewModel.stateFlow` via `collectAsStateWithLifecycle`, consumes `LoginSignupEvent` via `EventsEffect` (4 nav branches + `ShowSnackbar` resolved to a `stringResource` + `PromptBiometric` re-dispatched as `OnBiometricUnlock`), delegates to Content.
- **Content** `internal fun LoginSignupContent(state: LoginSignupState, onAction: (LoginSignupAction) -> Unit, modifier, snackbarHostState)` — hoisted state, stateless. `when (state.screenState)` renders all 4 `LoginSignupScreenState` members: `Loading`/`Content`/`Error` → `AuthFormSection(isLoading = …)`; `ZeroGroups` → `ZeroGroupsSection`.
- **Sub-composables** (`internal`, previewed): `AuthFormSection` (mode toggle + fields + submit + biometric), `ZeroGroupsSection` (post-auth empty state).
- **Reusable components** (`kpt.feature.loginsignup.components`, public): `AuthBrandHeader`, `AuthModeToggleTabs`, `AuthTextField`, `AuthErrorBanner`, `AuthDividerLabeled`, `ZeroGroupsEmptyState`.
- Every `on_click` in `ui.yaml#components[]` dispatches a typed `LoginSignupAction` member (RULE-IMPL-DEAD-CLICKABLE-001 Rule 1/2) — no free `on{X}: () -> Unit` callbacks, no dead clickables.

## route

`LoginSignupRoute.kt` — `@Serializable data object LoginSignupRoute` (`ui.yaml#route: /auth`).
`NavController.navigateToLoginSignup(navOptions?)`. `NavGraphBuilder.loginSignupScreen(onNavigateToPersonalDashboard, onNavigateToGroupList, onNavigateToGroupTypePicker, onNavigateToJoinWithCode)` registers via `composableWithRootPushTransitions<LoginSignupRoute>` (entry-point screen, no back stack). All 4 nav callbacks are **required** parameters (no `= {}` defaults) — DC3 count-assertion trivially satisfied (0 Screen.kt bare defaults). All 4 targets appear in `flow.yaml#navigates_to[]` (`personal-dashboard`, `group-list`, `group-type-picker`, `join-with-code`); `group-create` is declared in `flow.yaml` but has no current UI trigger, so it is not wired here.

## preview

`LoginSignupScreenPreview.kt` — 4 `@Preview` functions: `LoginSignupScreenPreview` (top-level, `PreviewParameterProvider<LoginSignupState>` with 5 variants: content/login, content/signup, loading, error, zero_groups — sourced from `demo-data.yaml` personas Grace Wanjiku / Amina Otieno), `AuthFormSectionContentPreview`, `AuthFormSectionSignupErrorPreview`, `ZeroGroupsSectionPreview`. Data source: demo-data.yaml. Every non-container composable in `LoginSignupScreen.kt` (`LoginSignupContent`, `AuthFormSection`, `ZeroGroupsSection`) is invoked by at least one preview (CP-4).

## tags

`LoginSignupTestTags` (`kpt.feature.loginsignup`) — append-only, 15 constants: `SCREEN`, `AUTH_CARD`, `TAB_LOGIN`, `TAB_SIGNUP`, `FIELD_NAME`, `FIELD_EMAIL_PHONE`, `FIELD_PASSWORD`, `FORGOT_PASSWORD_LINK`, `LOGIN_BUTTON`, `SIGNUP_BUTTON`, `BIOMETRIC_BUTTON`, `ERROR_BANNER`, `ZERO_GROUPS_ILLUSTRATION`, `CREATE_GROUP_BUTTON`, `JOIN_WITH_CODE_BUTTON`.

## permissions

No `request-permission` canonical `on_click` is declared in `ui.yaml` for this screen — `biometric_unlock_button.on_click.action = OnBiometricUnlock` with `effect: call_api`, handled entirely inside `AuthRepository`/`LoginSignupViewModel` (no OS permission prompt at the Screen layer). No requester composable emitted; RULE-PERMISSION-CODEGEN-001 is not applicable here.
<!-- kmp-screen-gen:END -->
