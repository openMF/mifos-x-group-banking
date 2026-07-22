<!-- generated-by: kmp-viewmodel-gen -->
# feature/loan-request — API

<!-- kmp-viewmodel-gen:BEGIN -->
## viewmodel

`org.mifos.groupbanking.feature.loanrequest.LoanRequestViewModel` — extends
`BaseViewModel<LoanRequestState, LoanRequestEvent, LoanRequestAction>`
(`kpt.core.base.ui.viewmodel.BaseViewModel`). Constructor deps: `LoanRequestRepository`
(`core/data`, `submit(payload)` / `enqueueOffline(payload)`), `NetworkMonitor`
(`cmp-network-monitor`), `CrashReporter` (`core-base/observability`), `KptAnalyticsTracker`
(`core/analytics`), plus the `clientId: Long` / `savingsBalance: Double` /
`loanMultiplier: Double = 3.0` nav-args (Koin `parametersOf`, NOT DI-graph types).

## state

`LoanRequestState` (`@Serializable @Immutable`):

| Field | Type | Default | Transient |
|---|---|---|---|
| `clientId` | `Long` | `0L` | no |
| `savingsBalance` | `Double` | `0.0` | no |
| `loanMultiplier` | `Double` | `3.0` | no |
| `maxLoanAmount` | `Double` | `0.0` (computed `savingsBalance * loanMultiplier` in `initialState`) | no |
| `requestedAmount` | `String` | `""` | no |
| `requestedAmountError` | `String?` | `null` | no |
| `purpose` | `LoanPurpose?` | `null` | no |
| `purposeError` | `String?` | `null` | no |
| `durationWeeks` | `Int` | `12` | no |
| `repaymentEstimate` | `Double` | `0.0` | no |
| `isOfflineMode` | `Boolean` | `false` | no |
| `isSubmitting` | `Boolean` | `false` | no |
| `isFormValid` | `Boolean` | `false` | no |
| `submitError` | `SubmitError?` | `null` | yes |
| `successDialogVisible` | `Boolean` | `false` | no |

`LoanRequestState.deriveScreenState()` (extension) → `LoanRequestScreenState` (`Content` /
`Submitting` / `SubmitSuccess` / `SubmitError` / `OfflineQueued`). `submitError != null` → `SubmitError`;
`isOfflineMode && successDialogVisible` → `OfflineQueued`; `successDialogVisible` → `SubmitSuccess`;
`isSubmitting` → `Submitting`; else → `Content`.

`SubmitError`: `Network` (`error_network_queued`, retry — never persisted, always retry-enqueued),
`Server` (`error_server`, retry — never persisted, always retry-enqueued), `Validation`
(`error_validation`, non-retry, persisted + inline `error_snackbar`), `Unauthorized`
(`error_session_expired`, non-retry, persisted + inline `error_snackbar` — no dedicated nav event,
see `DEVELOPMENT.md#6`).

**Validation formulas:**
- `requestedAmountError`: `null` while blank; `"error_validation"` for non-numeric, `< KES 500`, or
  `> maxLoanAmount` (single shared message key — `ui.yaml` declares no per-case key).
- `isFormValid`: `requestedAmount` parses to a `Double` AND `requestedAmountError == null` AND
  `purpose != null` AND `!isSubmitting`.
- `repaymentEstimate`: `requestedAmount * (1 + GROUP_INTEREST_RATE) / durationWeeks` where
  `GROUP_INTEREST_RATE = 0.10` — a documented, flagged placeholder (see `DEVELOPMENT.md#9`); no
  numeric group-rate field is declared anywhere in `ui.yaml`/`api.yaml`.

## actions

| Action | Payload | Effect |
|---|---|---|
| `OnAmountChange` | `value: String` | validates + recomputes `repaymentEstimate`/`isFormValid` (pure) |
| `OnPurposeSelected` | `purpose: LoanPurpose` | sets `purpose`, clears `purposeError`, recomputes `isFormValid` (pure) |
| `OnDurationChanged` | `weeks: Int` | sets `durationWeeks`, recomputes `repaymentEstimate` (pure) |
| `OnSubmitClick` | — | final validation gate; on pass, submits (online) or enqueues (offline) |
| `OnSuccessDialogDismiss` | — | clears `successDialogVisible`, emits `NavigateToDashboardAfterSuccess` |
| `OnRetry` | — | re-submits the unchanged payload, no validation gate |
| `Internal.ConnectivityChanged` | `online: Boolean` | sets `isOfflineMode`; not user-dispatched |
| `Internal.SubmitResult` | `payload, result: NetworkResult<LoanRequestResult, NetworkError>` | routes submit outcome; not user-dispatched |
| `Internal.EnqueueResult` | `queueId: Long` | routes enqueue outcome → `OfflineQueued`; not user-dispatched |

## events

| Event | Payload | Trigger |
|---|---|---|
| `NavigateToDashboardAfterSuccess` | — | `OnSuccessDialogDismiss` |
| `NavigateBack` | — | declared (`ui.yaml#events`) but never emitted — see `DEVELOPMENT.md#9` gap (1) |
| `ShowOfflineQueuedConfirmation` | — | `Internal.EnqueueResult` (offline-direct OR transport-error retry-enqueue) |

## di

`org.mifos.groupbanking.feature.loanrequest.di.LoanRequestModule` — Koin module,
`viewModel { parameters -> LoanRequestViewModel(..., clientId = parameters.get(), savingsBalance =
parameters.get(), loanMultiplier = parameters.getOrNull() ?: 3.0) }` (not `viewModelOf` —
nav-args, not DI-graph types). Included in `KoinModules.kt#featureModule`. `LoanRequestRepository`
resolved from `DataModule`, `NetworkMonitor` from `DataModule`'s `NetworkMonitorProvider.install()`
binding, `CrashReporter` from `observabilityModule`, `KptAnalyticsTracker` from the single
process-wide binding supplied by `LoginSignupModule`.
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## screen

`org.mifos.groupbanking.feature.loanrequest.LoanRequestScreen` (Container) --
`LoanRequestScreen(clientId, savingsBalance, loanMultiplier, onNavigateToDashboard,
onNavigateBack, modifier, viewModel = koinViewModel(parameters = { parametersOf(clientId,
savingsBalance, loanMultiplier) }))`. Collects `stateFlow` via `collectAsStateWithLifecycle`,
handles `LoanRequestEvent` via `EventsEffect` (`NavigateToDashboardAfterSuccess` ->
`onNavigateToDashboard()`; `NavigateBack` -> `onNavigateBack()`, defensive/unreachable per
`DEVELOPMENT.md#9`; `ShowOfflineQueuedConfirmation` -> no-op, the success dialog already reflects
the offline-queued outcome). Delegates to stateless `LoanRequestContent(state, onAction,
onNavigateBack)`.

`LoanRequestContent` -- `KptScaffold` + `KptTopAppBar` (back icon wired straight to
`onNavigateBack`, NOT an `onAction` dispatch, since `ui.yaml#state_model.actions.members` has no
matching `OnBack` member) wrapping `LoanRequestFormFields` (shared scrollable body across every
`LoanRequestScreenState`) + an overlay `LoanRequestSuccessDialog` when `state.successDialogVisible`
(title/body pick online vs offline copy by `screenState == LoanRequestScreenState.OfflineQueued`).

`LoanRequestFormFields(state, onAction, screenState)` -- offline banner (if `isOfflineMode`) +
savings/limit card + amount field + purpose dropdown + duration slider + repayment summary card
(if `requestedAmount.isNotBlank() && requestedAmountError == null`) + a trailing
`when(screenState)`: `Submitting` -> `CircularProgressIndicator`; `SubmitError` -> submit button +
`LoanRequestErrorCard` (inline, not a transient `Snackbar`, per `ui.yaml`'s `type: card`
declaration); `Content`/`SubmitSuccess`/`OfflineQueued` -> submit button only.

## route

`org.mifos.groupbanking.feature.loanrequest.LoanRequestRoute` -- `@Serializable data class
LoanRequestRoute(val clientId: Long, val savingsBalance: Double, val loanMultiplier: Double =
3.0)`. `NavController.navigateToLoanRequest(clientId, savingsBalance, loanMultiplier = 3.0,
navOptions = null)`. `NavGraphBuilder.loanRequestScreen(onNavigateToDashboard, onNavigateBack)` --
both params required (no `= {}` default); DC3 count-assertion: 0 defaults / 0 overrides / 0
suppressed. Not yet wired into `cmp-navigation`'s nav graph (matches `feature/loan-apply`'s
identical not-yet-wired precedent -- see `DEVELOPMENT.md#3`).

## preview

`LoanRequestScreenPreview.kt` -- 11 `@Preview` functions. Data source: hand-authored stubs shaped
after `ui.yaml#states.*` (no `demo-data.yaml` resolved this pass). `LoanRequestContentPreview`
uses a `PreviewParameterProvider<LoanRequestState>` with 7 variants (content-empty,
content-filled, offline, submitting, submit-error, submit-success, offline-queued). Every other
`@Composable` in `LoanRequestScreen.kt` (`LoanRequestFormFields` x5 state variants,
`LoanRequestSubmitButton` x2 enabled/disabled) also gets a dedicated `@Preview`. All wrapped in
`KptTheme { ... }`; all action lambdas stubbed `{}`.

## tags

`org.mifos.groupbanking.feature.loanrequest.LoanRequestTestTags` (append-only, RULE-KMP-COMPOSE-UITEST-001
CU-5) -- `SCREEN`, `TOP_BAR`, `OFFLINE_BANNER`, `SAVINGS_LIMIT_CARD`, `FIELD_AMOUNT`,
`DROPDOWN_PURPOSE`, `DURATION_SLIDER`, `REPAYMENT_SUMMARY_CARD`, `SUBMIT_BUTTON`,
`SUBMITTING_INDICATOR`, `SUCCESS_DIALOG`, `SUCCESS_DIALOG_OK_BUTTON`, `ERROR_CARD`,
`ERROR_CARD_RETRY_BUTTON` (14 tags).

## permissions

None -- `ui.yaml` declares no `request-permission` `on_click` action for this screen; no
`remember*PermissionRequester()` invocation was emitted.
<!-- kmp-screen-gen:END -->
