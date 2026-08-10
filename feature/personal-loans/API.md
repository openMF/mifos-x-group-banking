<!-- generated-by: kmp-viewmodel-gen -->
# feature/personal-loans — API

<!-- kmp-viewmodel-gen:BEGIN -->
## viewmodel

`kpt.feature.personalloans.PersonalLoansViewModel` — extends
`BaseViewModel<PersonalLoansState, PersonalLoansEvent, PersonalLoansAction>`
(`kpt.core.base.ui.viewmodel.BaseViewModel`). Constructor deps: `LoanRepository` (`core/data`,
`getLoansForClient(clientId)` — plain `suspend fun`, no Store5 wrap), `SessionManager`
(`core-base/security`), `CrashReporter` (`core-base/observability`), `KptAnalyticsTracker`
(`core/analytics`), plus the `clientId: Long` nav-arg (Koin `parametersOf`, NOT a DI-graph type).

## state

`PersonalLoansState` (`@Serializable @Immutable`):

| Field | Type | Default | Transient |
|---|---|---|---|
| `clientId` | `Long` | `0L` | no |
| `loans` | `List<LoanSummary>` | `emptyList()` | yes |
| `selectedLoanId` | `Long?` | `null` | no |
| `filterStatus` | `LoanStatusFilter` | `ALL` | no |
| `filteredLoans` | `List<LoanSummary>` | `emptyList()` | yes (additive, mirrors `LoanListState.filteredLoans`) |
| `isLoading` | `Boolean` | `true` | no |
| `isRefreshing` | `Boolean` | `false` | no |
| `error` | `LoanError?` | `null` | yes |

`PersonalLoansState.deriveScreenState()` (extension) → `PersonalLoansScreenState` (`Loading` /
`Content` / `Empty` / `Error`). `error != null` → `Error`; `isLoading` → `Loading`;
`loans.isEmpty()` → `Empty`; else → `Content`.

`LoanError`: `Network` (`error_network`, retry), `Server` (`error_server`, retry), `Unauthorized`
(`error_session_expired`, non-retry — calls `sessionManager.endSession()` for real).

## actions

| Action | Payload | Effect |
|---|---|---|
| `OnRefresh` | — | `isRefreshing = true`, re-fetches `getLoansForClient` |
| `OnRetry` | — | clears `error`, `isLoading = true`, re-fetches |
| `OnLoanExpand` | `loanId: Long` | toggles `selectedLoanId` (tap same id again collapses) |
| `OnFilterChange` | `status: LoanStatusFilter` | client-side, recomputes `filteredLoans`, no re-fetch |
| `OnRequestLoanClick` | — | emits `NavigateToLoanRequest` (parameterless, see gap below) |
| `Internal.LoansLoaded` | `result: NetworkResult<List<LoanSummary>, NetworkError>, isRefresh: Boolean` | routes fetch outcome; not user-dispatched |

## events

| Event | Payload | Trigger |
|---|---|---|
| `NavigateToLoanRequest` | — | `OnRequestLoanClick` — see `DEVELOPMENT.md#9` gap (1), `savingsBalance` not forwardable |
| `NavigateBack` | — | declared (`ui.yaml#events`) but never emitted — see `DEVELOPMENT.md#9` gap (2) |

## di

`kpt.feature.personalloans.di.PersonalLoansModule` — Koin module,
`viewModel { parameters -> PersonalLoansViewModel(..., clientId = parameters.get()) }` (not
`viewModelOf` — `clientId` is a nav-arg, not a DI-graph type). Included in
`KoinModules.kt#featureModule`. `LoanRepository` resolved from `DataModule`, `SessionManager` from
`SecurityModule`, `CrashReporter` from `observabilityModule`, `KptAnalyticsTracker` from the single
process-wide binding supplied by `LoginSignupModule`.
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## screen

`kpt.feature.personalloans.PersonalLoansScreen` (Container, `internal`) — collects
`viewModel.stateFlow` via `collectAsStateWithLifecycle`, consumes `PersonalLoansEvent` via
`EventsEffect`, delegates to `PersonalLoansContent` (Content, `internal`, stateless — hoisted
`state: PersonalLoansState` + `onAction: (PersonalLoansAction) -> Unit` + plain `onNavigateBack: ()
-> Unit`). State-driven per `PersonalLoansState.deriveScreenState()`: `Loading` ->
`PersonalLoansLoadingSection`, `Content` -> `PersonalLoansContentSection`, `Empty` ->
`PersonalLoansEmptySection`, `Error` -> `PersonalLoansErrorSection`. Components:
`components.PersonalLoansCard` (+ private `PersonalLoansCardDetails` expand panel),
`components.PersonalLoansCardSkeleton`, `components.PersonalLoansFilterChips`,
`components.PersonalLoansStatusVisuals` (`personalLoansBadgeContainerColor` /
`personalLoansBadgeContentColor` extensions on `LoanAccountStatus`).

## route

`kpt.feature.personalloans.PersonalLoansRoute` — `@Serializable data class
PersonalLoansRoute(val clientId: Long)`. `NavController.navigateToPersonalLoans(clientId,
navOptions)`. `NavGraphBuilder.personalLoansScreen(onNavigateToLoanRequest: (clientId: Long) ->
Unit, onNavigateBack: () -> Unit)` — 2 callbacks, both required (no `= {}` defaults); no
`onNavigateToLoanDetail` (see `DEVELOPMENT.md#10` Route rationale — `flow.yaml#navigates_to` does
not list `loan-detail`).

## preview

`PersonalLoansScreenPreview.kt` — 12 `@Preview` functions, `org.jetbrains.compose.ui.tooling.preview`
import (KMP-safe). Data source: `demo-data.yaml` (`entries[0].items[]`, 3 `LoanDto` rows for
`clientId: 101`, mapped onto `LoanSummary`). Top-level `PersonalLoansContent` driven by
`PreviewParameterProvider<PersonalLoansState>` (5 variants: Loading, Content, Content-filtered+
expanded, Empty, Error). Every non-container composable covered (CP-4).

## tags

`kpt.feature.personalloans.PersonalLoansTestTags` — `SCREEN`,
`FILTER_CHIP_ALL`/`FILTER_CHIP_ACTIVE`/`FILTER_CHIP_CLOSED`, `LOADING_INDICATOR`, `LOAN_LIST`,
`FAB_REQUEST_LOAN`, `EMPTY_SECTION`, `EMPTY_ACTION_BUTTON`, `ERROR_SECTION`,
`ERROR_RETRY_BUTTON`, plus `cardTag(loanId: Long)` / `cardDetailsTag(loanId: Long)` stable-key
functions. Append-only (RULE-KMP-COMPOSE-UITEST-001 CU-5).

## permissions

None declared — `ui.yaml` has no `on_click` with `action: request-permission` for this screen.
<!-- kmp-screen-gen:END -->
