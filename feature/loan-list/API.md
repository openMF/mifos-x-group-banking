<!-- generated-by: kmp-viewmodel-gen -->
<!-- co-authored-by: kmp-screen-gen -->
# feature/loan-list — API

<!-- kmp-viewmodel-gen:BEGIN -->
## viewmodel

`kpt.feature.loanlist.LoanListViewModel` — extends
`BaseViewModel<LoanListState, LoanListEvent, LoanListAction>`
(`kpt.core.base.ui.viewmodel.BaseViewModel`). Constructor deps: `LoanRepository` (`core/data`,
`loansPagingStream(groupId, scope, fetchPolicy)`), `SessionManager` (`core-base/security`,
`endSession()` on 401), `CrashReporter` (`core-base/observability`), `KptAnalyticsTracker`
(`core/analytics`), plus the `groupId` nav-arg (Koin `parametersOf`, NOT a DI-graph type).

## state

`LoanListState` (`@Serializable @Immutable`):

| Field | Type | Default | Transient |
|---|---|---|---|
| `isLoading` | `Boolean` | `true` | no |
| `loans` | `List<LoanSummary>` | `emptyList()` | yes |
| `selectedFilter` | `LoanStatusFilter` | `LoanStatusFilter.ALL` | no |
| `filteredLoans` | `List<LoanSummary>` | `emptyList()` | yes |
| `isRefreshing` | `Boolean` | `false` | no |
| `error` | `LoanListError?` | `null` | yes |
| `groupId` | `Long` | `0L` | no |
| `canApplyLoan` | `Boolean` | `false` | no |

`LoanListState.screenState` (derived extension) → `LoanListScreenState` (`Loading` / `Content` /
`Error` / `Empty`). `error != null` → `Error`; `isLoading` → `Loading`; `loans.isEmpty()` →
`Empty`; else → `Content`.

`LoanListError`: `Network` (`error_network`, retry), `Server` (`error_server`, retry), `Auth`
(`error_auth`, non-retry — also triggers `SessionManager.endSession()`; `ui.yaml` declares
`redirect: login` but no matching `NavigateToLogin` event exists in `events.members`).

**`canApplyLoan` gap (flagged, not invented):** never set `true` anywhere in this ViewModel — the
injectable `SessionManager` carries no role field, and `ui.yaml#nav_params` for `loan-list`
declares only `groupId` (no `viewerRole` forwarded from `group-dashboard`, unlike that screen's
own `groupId`/`viewerRole` pair). The Apply-Loan FAB (`visible_when: canApplyLoan`) never renders
until this gap is closed. See `LoanListState` class KDoc.

## actions

| Action | Payload | Effect |
|---|---|---|
| `OnLoanClick` | `loanId: Long` | emits `NavigateToLoanDetail(loanId)` |
| `OnFilterChange` | `filter: LoanStatusFilter` | sets `selectedFilter`, recomputes `filteredLoans` (client-side, no network call) |
| `OnApplyLoan` | — | emits `NavigateToLoanApply(groupId)` |
| `OnRefresh` | — | `pagingStream.refresh()`; sets `isRefreshing = true` |
| `Retry` | — | `pagingStream.retry()`; sets `isLoading = true`, clears `error` |
| `OnLoadNextPage` | — | `pagingStream.loadNextPage()` — scroll-to-end pagination |
| `Internal.StreamUpdated` | `screenState: ScreenState<List<LoanSummary>>` | routes to the stream→state mapper; not user-dispatched |

**Not added (flagged idea-layer gap):** `top_bar.on_nav_click`'s `OnBack` action has a
fully-authored `action_contract` in `ui.yaml` but is NOT present in
`state_model.actions.members` (only the 7 rows above are declared, one of which is `Internal`) —
mirrors `GroupListAction`'s identical `OnOpenNotifications` gap. Reported to the caller for an
idea-layer `ui.yaml#state_model.actions.members` update rather than invented.

## events

| Event | Payload | Trigger |
|---|---|---|
| `NavigateToLoanDetail` | `loanId: Long` | `OnLoanClick` |
| `NavigateToLoanApply` | `groupId: Long` | `OnApplyLoan` |
| `ShowSnackbar` | `message: String` (messageKey) | `Unauthenticated` (401) |

## di

`kpt.feature.loanlist.di.LoanListModule` — Koin module,
`viewModel { parameters -> LoanListViewModel(..., groupId = parameters.get()) }` (not
`viewModelOf` — `groupId` is a nav-arg, not a DI-graph type). Included in
`KoinModules.kt#featureModule`. `LoanRepository` resolved from `DataModule`, `SessionManager` from
`SecurityModule`, `CrashReporter` from `observabilityModule`, `KptAnalyticsTracker` from the
single process-wide binding supplied by `LoginSignupModule`.
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## screen

`kpt.feature.loanlist.LoanListScreen` — Container: collects `LoanListViewModel
.stateFlow` via `collectAsStateWithLifecycle`, consumes `LoanListEvent`s through `EventsEffect`
(`NavigateToLoanDetail`/`NavigateToLoanApply` → typed nav callbacks; `ShowSnackbar` → resolves
`messageKey` to display text, mirrors `GroupListScreen`'s `messageKeyToText`). `groupId: Long` nav-
arg forwarded to `koinViewModel(parameters = { parametersOf(groupId) })`. `onNavigateBack: () ->
Unit` is a PLAIN callback (NOT a `LoanListAction`/`LoanListEvent`) wired straight to
`KptScaffold`'s `onNavigationIconClick` — `top_bar.on_nav_click`'s `OnBack` has no matching
`state_model.actions.members` entry (flagged gap, see `LoanListAction` KDoc); this mirrors
`GroupDashboardContent`'s "local nav/UI concern, not a ViewModel action" precedent.

`LoanListContent` — stateless, `state: LoanListState, onAction: (LoanListAction) -> Unit,
onNavigateBack: () -> Unit`. `when (state.screenState)` branches: `Loading` →
`LoanListLoadingSection` (functional filter chips + 5 `LoanListCardSkeleton` rows +
`CircularProgressIndicator`); `Content` → `LoanListContentSection` (filter chips + paginated
`LazyColumn` of `LoanListCard`, scroll-to-end dispatches `OnLoadNextPage` via
`rememberLoadMoreTrigger`); `Empty` → `LoanListEmptySection` (filter chips + icon/title/body, no
in-body CTA — the FAB is the CTA); `Error` → `LoanListErrorSection` (icon/title/message/Retry, no
filter chips). FAB (`LoanListFabContent`) renders only when `state.canApplyLoan` (currently always
`false` — see `LoanListState` KDoc gap note), dispatching `OnApplyLoan`.

## route

`kpt.feature.loanlist.LoanListRoute` — `@Serializable data class
LoanListRoute(val groupId: Long)` (`/groups/{groupId}/loans`). `navigateToLoanList(groupId,
navOptions)` extension on `NavController`. `NavGraphBuilder.loanListScreen(onNavigateToLoanDetail:
(loanId: Long) -> Unit, onNavigateToLoanApply: (groupId: Long) -> Unit, onNavigateBack: () ->
Unit)` registers via `composableWithPushTransitions<LoanListRoute>` (non-root push destination,
NOT `...RootPushTransitions`). Wired into `cmp-navigation/.../GroupBankingNavHost.kt`: group 7
(`group-dashboard`'s "Loans" quick action) now calls `navController.navigateToLoanList(groupId =
...)` for real; group 8 registers `loanListScreen(...)` with `onNavigateToLoanDetail` /
`onNavigateToLoanApply` both routing to `PlaceholderRoute` (neither `loan-detail` nor `loan-apply`
is a generated feature module yet) and `onNavigateBack = { navController.popBackStack() }`.

## preview

`LoanListScreenPreview.kt` — 10 `@Preview` functions. Top-level `LoanListContentPreview` drives 4
`LoanListState` variants (Loading/Content/Empty/Error) via `LoanListStatePreviewProvider`
(`PreviewParameterProvider<LoanListState>`), data source: `demo-data.yaml`'s 12-row `LoanSummary`
set (4 rows used, one per `LoanAccountStatus` bucket — ACTIVE/OVERDUE/CLOSED/PENDING). One preview
each for `LoanListFabContent`, `LoanListLoadingSection`, `LoanListContentSection`,
`LoanListEmptySection`, `LoanListErrorSection`, `LoanListCard` (×2 — ACTIVE + OVERDUE variants),
`LoanListCardSkeleton`, `LoanListFilterChips`. All wrapped in `KptTheme { ... }`; import is
`org.jetbrains.compose.ui.tooling.preview.*` (KMP-safe, not `androidx.compose.ui.tooling.preview`).

## tags

`kpt.feature.loanlist.LoanListTestTags` — append-only (RULE-KMP-COMPOSE-UITEST-001
CU-5): `SCREEN`, `FILTER_CHIP_ALL`, `FILTER_CHIP_ACTIVE`, `FILTER_CHIP_OVERDUE`,
`FILTER_CHIP_CLOSED`, `LOADING_INDICATOR`, `LOAN_LIST`, `FAB_APPLY`, `EMPTY_SECTION`,
`ERROR_SECTION`, `ERROR_RETRY_BUTTON` (10 constants) plus `cardTag(loanId: Long): String` (per-row
resolver, since loan cards render data-driven from `filteredLoans`, not as N separate
composables).

## permissions

Not applicable — `loan-list` declares no `request-permission` `on_click` in `ui.yaml`; no
`remember*PermissionRequester()` invocation is emitted.
<!-- kmp-screen-gen:END -->
