<!-- generated-by: kmp-viewmodel-gen -->
# feature/loan-detail — API

<!-- kmp-viewmodel-gen:BEGIN -->
## viewmodel

`org.mifos.groupbanking.feature.loandetail.LoanDetailViewModel` — extends
`BaseViewModel<LoanDetailState, LoanDetailEvent, LoanDetailAction>`
(`kpt.core.base.ui.viewmodel.BaseViewModel`). Constructor deps: `LoanDetailRepository`
(`core/data`, `loanDetailStream(loanId, scope, fetchPolicy)`), `SessionManager`
(`core-base/security`, `endSession()` on 401), `CrashReporter` (`core-base/observability`),
`KptAnalyticsTracker` (`core/analytics`), plus the `loanId` nav-arg (Koin `parametersOf`, NOT a
DI-graph type).

## state

`LoanDetailState` (`@Serializable @Immutable`):

| Field | Type | Default | Transient |
|---|---|---|---|
| `isLoading` | `Boolean` | `true` | no |
| `loan` | `LoanDetail?` | `null` | yes |
| `repaymentSchedule` | `List<RepaymentScheduleRow>` | `emptyList()` | yes |
| `repaymentHistory` | `List<RepaymentTransaction>` | `emptyList()` | yes |
| `selectedTab` | `LoanDetailTab` | `LoanDetailTab.SCHEDULE` | no |
| `error` | `LoanDetailError?` | `null` | yes |
| `isRecordingRepayment` | `Boolean` | `false` | no |
| `canRecordRepayment` | `Boolean` | `false` | no |
| `canMarkDefaulted` | `Boolean` | `false` | no |

`LoanDetailState.screenState` (derived extension) → `LoanDetailScreenState` (`Loading` /
`Content` / `Error`). `error != null` → `Error`; `isLoading` → `Loading`; else → `Content`.

`LoanDetailError`: `Network` (`error_network`, retry), `Server` (`error_server`, retry),
`NotFound` (`error_not_found`, non-retry — documented reachability gap, currently unreachable in
production, surfaces as `Server`), `Auth` (`error_auth`, non-retry — also triggers
`SessionManager.endSession()`).

**`canRecordRepayment` / `canMarkDefaulted` gap (flagged, not invented):** never set `true`
anywhere in this ViewModel — the injectable `SessionManager` carries no role field, and
`ui.yaml#nav_params` for `loan-detail` declares only `loanId` (no `viewerRole` forwarded from
`loan-list`, unlike `group-dashboard`'s own `groupId`/`viewerRole` pair). The Record-Repayment /
Mark-Defaulted buttons (`visible_when: canRecordRepayment && loan.status == ACTIVE` /
`canMarkDefaulted && loan.status == OVERDUE`) never render until this gap is closed. See
`LoanDetailState` class KDoc.

## actions

| Action | Payload | Effect |
|---|---|---|
| `OnTabChange` | `tab: LoanDetailTab` | sets `selectedTab` (client-side, no network call) |
| `OnRecordRepayment` | — | emits `ShowRepaymentDialog` (mutation itself out of scope — see Boundaries) |
| `OnMarkDefaulted` | — | emits `ShowDefaultConfirmDialog` (mutation itself out of scope — see Boundaries) |
| `OnBack` | — | emits `NavigateBack` |
| `Retry` | — | `detailStream.retry()`; sets `isLoading = true`, clears `error` |
| `OnRefresh` | — | `detailStream.refreshFresh()`; sets `isLoading = true` |
| `Internal.StreamUpdated` | `screenState: ScreenState<LoanDetailResponse>` | routes to the stream→state mapper; not user-dispatched |

## events

| Event | Payload | Trigger |
|---|---|---|
| `NavigateBack` | — | `OnBack` |
| `ShowRepaymentDialog` | — | `OnRecordRepayment` |
| `ShowDefaultConfirmDialog` | — | `OnMarkDefaulted` |
| `ShowSnackbar` | `message: String` (messageKey) | `Unauthenticated` (401) |

## di

`org.mifos.groupbanking.feature.loandetail.di.LoanDetailModule` — Koin module,
`viewModel { parameters -> LoanDetailViewModel(..., loanId = parameters.get()) }` (not
`viewModelOf` — `loanId` is a nav-arg, not a DI-graph type). Included in
`KoinModules.kt#featureModule`. `LoanDetailRepository` resolved from `DataModule`,
`SessionManager` from `SecurityModule`, `CrashReporter` from `observabilityModule`,
`KptAnalyticsTracker` from the single process-wide binding supplied by `LoginSignupModule`.
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## screen

`org.mifos.groupbanking.feature.loandetail.LoanDetailScreen` — Container+Content split.
`LoanDetailScreen(loanId, onNavigateBack, viewModel = koinViewModel(parameters = {
parametersOf(loanId) }))` collects `viewModel.stateFlow`, consumes `LoanDetailEvent`s via
`EventsEffect` (`NavigateBack` → `onNavigateBack()`; `ShowRepaymentDialog` /
`ShowDefaultConfirmDialog` → an in-screen "coming soon" snackbar — `loan-repayment-dialog` /
`loan-mark-defaulted-dialog` are not yet generated feature components; `ShowSnackbar` → resolved
error-message snackbar), and delegates to stateless `LoanDetailContent(state, onAction)`.
`LoanDetailContent` wraps `KptScaffold` (title, refresh top-bar action, pull-to-refresh — both
dispatch `OnRefresh`) and state-driven renders `LoanDetailScreenState.{Loading,Content,Error}` via
`LoanDetailLoadingSection` / `LoanDetailContentSection` / `LoanDetailErrorSection`.
`LoanDetailContentSection` is a single `LazyColumn` (`LoanHeaderCard` → `LoanOutstandingSummaryRow`
→ `LoanDetailTabs` → tab-scoped `RepaymentScheduleRowItem` / `RepaymentTransactionRow` rows →
`LoanActionButtonsRow`) — no nested scrollables. Components live under
`feature/loan-detail/.../components/`.

## route

`org.mifos.groupbanking.feature.loandetail.LoanDetailRoute` — `@Serializable data class
LoanDetailRoute(val loanId: Long)`, route `/loans/{loanId}` (`ui.yaml#route`). `NavController
.navigateToLoanDetail(loanId, navOptions)` + `NavGraphBuilder.loanDetailScreen(onNavigateBack)`.
Registered on `cmp-navigation`'s `GroupBankingNavHost.kt` (`loan-list` → `loan-detail` wired for
real, replacing the prior `PlaceholderRoute`). DC3 count-assertion: 1 default / 1 override / 0
suppressed.

## preview

`LoanDetailScreenPreview.kt` — data source: `idea-layer/screens/loan-detail/demo-data.yaml`
(`demo_data_resolved = true`, Grace Akinyi's KES 8,000 Group Solidarity Loan). 5 top-level
`LoanDetailState` variants (Loading, Content/Schedule, Content/History, Content/OVERDUE
synthesized via `.copy()`, Error) + one `@Preview` per `LoanDetailScreen.kt` sub-composable +
every standalone component in `components/`.

## tags

`LoanDetailTestTags` (append-only, `feature/loan-detail/.../LoanDetailTestTags.kt`) — `SCREEN`,
`HEADER_CARD`, `STATUS_BADGE`, `OUTSTANDING_SUMMARY_ROW`, `OUTSTANDING_CHIP`, `OVERDUE_CHIP`,
`TABS`, `TAB_SCHEDULE`, `TAB_HISTORY`, `SCHEDULE_TABLE`, `HISTORY_LIST`, `HISTORY_EMPTY`,
`ACTION_BUTTONS_ROW`, `RECORD_REPAYMENT_BUTTON`, `MARK_DEFAULTED_BUTTON`, `CONTENT_LIST`,
`LOADING_SECTION`, `ERROR_SECTION`, `ERROR_RETRY_BUTTON`, plus `scheduleRowTag(weekNumber)` /
`historyRowTag(transactionId)` per-row functions (data-driven rows, not N fixed constants).
<!-- kmp-screen-gen:END -->
