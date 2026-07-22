<!-- generated-by: kmp-viewmodel-gen -->
<!-- co-authored-by: kmp-screen-gen -->
# feature/loan-list — Development

## 1. Module Identity

`:feature:loan-list` — the filterable, paginated list of a savings group's loan accounts
(`/groups/{groupId}/loans`), entered from `group-dashboard`'s "Loans" quick action or
`bottom_nav`. Namespace `org.mifos.groupbanking.feature.loanlist`. Source of truth:
`idea-layer/screens/loan-list/{ui,flow,data-flow,docs}.yaml`.

<!-- kmp-viewmodel-gen:BEGIN -->
## 2. Public API

See `LoanListViewModel.kt` class/field KDoc for the full ViewModel contract (`LoanListState`,
`LoanListEvent`, `LoanListAction`, `LoanListScreenState`, `LoanListError`) — not re-documented
here to avoid drift; see `API.md#viewmodel` / `#state` / `#actions` / `#events` / `#di`.

## 3. Consumers

- `feature/loan-list/src/commonTest/.../LoanListViewModelTest.kt`.
- `cmp-navigation` — `LoanListModule` included in `KoinModules.kt#featureModule`;
  `implementation(projects.feature.loanList)` added to `cmp-navigation/build.gradle.kts`.
  `LoanListScreen.kt` / `LoanListRoute.kt` are `kmp-screen-gen` scope (not generated this pass).

## 4. Boundaries

Reads exclusively via the injected `LoanRepository.loansPagingStream(groupId, scope)`
(`core/data`) — no direct API/DB access from this module. `business_logic.kind: crud` (read-only,
paginated) per RULE-IMPLEMENT-STORE5-001 — the offline-first `PagingScreenStream<LoanSummary>` is
consumed directly. `SessionManager.endSession()` (`core-base/security`) is called for real on
`ScreenState.Unauthenticated` (mirrors `GroupDashboardViewModel`'s identical precedent);
`NetworkMonitor` is composed internally by `LoanRepositoryImpl`, not re-injected here.

## 5. Data

See `API.md#state` / `#actions` / `#events` for the ViewModel field/payload schema.

## 6. Errors

`LoanListError` (3 variants): `Network`, `Server` (both retryable), `Auth` (non-retryable, no
declared `NavigateToLogin` event — surfaced as an ordinary error state + `ShowSnackbar`, plus a
real `sessionManager.endSession()` call).

## 7. Testing

`feature/loan-list/src/commonTest/kotlin/org/mifos/groupbanking/feature/loanlist/LoanListViewModelTest.kt`
(16 tests — initial state, stream→state mapping for all 6 `ScreenState` members, all 6 declared
actions, filter recompute-on-refresh, load-more append). No `commonTest` UI-test suite yet for the
screen layer — pending `kmp-compose-uitest-gen` / `kmp-journey-uitest-gen`.

## 8. Observability

See `LoanListViewModel.kt` KDoc (Kermit `Logger.i/d` + `CrashReporter` breadcrumbs +
`KptAnalyticsTracker.trackLoanOperation(...)` on mount and every user-initiated loan operation).

## 9. Evolution

- 2026-07-22 — `kmp-viewmodel-gen`: `LoanListViewModel.kt` (state/action/event sealed types,
  client-side status filter recomputed on every `Content` emission, real
  `SessionManager.endSession()` on 401), `LoanListModule.kt` (Koin, `groupId` nav-arg via
  `parametersOf`), feature module scaffold (`build.gradle.kts`, `settings.gradle.kts` include,
  `cmp-navigation` dependency + `KoinModules.kt` registration). Flagged idea-layer gaps (not
  invented): (1) `LoanListState.canApplyLoan` has no reachable role source — the injectable
  `SessionManager` carries no role field and `ui.yaml#nav_params` declares only `groupId` (no
  `viewerRole` forwarded from `group-dashboard`, unlike that screen's own `groupId`/`viewerRole`
  pair) — stays at its conservative `false` default end-to-end; (2) `top_bar.on_nav_click`'s
  `OnBack` action is NOT present in `state_model.actions.members` (only 6 members declared) — not
  added to `LoanListAction` silently, mirrors `GroupListAction`'s identical `OnOpenNotifications`
  gap precedent.
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## 2b. Screen / UI layer

- `LoanListScreen.kt` — Container (`LoanListScreen`, `groupId: Long` nav-arg forwarded to
  `koinViewModel(parameters = { parametersOf(groupId) })`) + stateless Content
  (`LoanListContent`), state-driven over all 4 `LoanListScreenState` members
  (Loading/Content/Empty/Error). Sub-sections `LoanListLoadingSection` / `LoanListContentSection`
  / `LoanListEmptySection` / `LoanListErrorSection` / `LoanListFabContent` (all `internal`,
  previewed individually). `onNavigateBack` is threaded as a PLAIN callback (not a `LoanListAction`
  dispatch) — see design-ambiguity note 1 below.
- `LoanListRoute.kt` — `@Serializable data class LoanListRoute(val groupId: Long)`
  (`/groups/{groupId}/loans`) + `navigateToLoanList(groupId)` +
  `NavGraphBuilder.loanListScreen(onNavigateToLoanDetail, onNavigateToLoanApply,
  onNavigateBack)` (3 required nav callbacks, no dead-clickable defaults —
  `composableWithPushTransitions`, non-root push destination). Wired for real into
  `cmp-navigation/.../GroupBankingNavHost.kt` (`group-dashboard`'s "Loans" quick action now
  navigates here instead of the `PlaceholderRoute`); `loan-detail` / `loan-apply` are still
  not-yet-generated feature modules, so their callbacks route to `PlaceholderRoute` at the
  NavHost level.
- `components/` — `LoanListCard` (data-bound memberName/loanProductName/principalAmount/
  outstandingBalance/nextRepaymentDate/overdueAmount + colour-coded `LoanAccountStatus` badge +
  initials avatar), `LoanListCardSkeleton` (loading shimmer row), `LoanListFilterChips`
  (4-chip `LoanStatusFilter` row — All/Active/Overdue/Closed), `LoanStatusVisuals.kt`
  (`badgeContainerColor` / `badgeContentColor` per-`LoanAccountStatus` lookups).
- `LoanListTestTags.kt` — 10 append-only test tags (SCREEN, FILTER_CHIP_ALL, FILTER_CHIP_ACTIVE,
  FILTER_CHIP_OVERDUE, FILTER_CHIP_CLOSED, LOADING_INDICATOR, LOAN_LIST, FAB_APPLY, EMPTY_SECTION,
  ERROR_SECTION, ERROR_RETRY_BUTTON) plus the `cardTag(loanId)` resolver.
- `LoanListScreenPreview.kt` — 10 `@Preview` functions: 4 top-level `LoanListState` variants
  (Loading/Content/Empty/Error, sourced from `demo-data.yaml`'s 12-row `LoanSummary` set) plus one
  preview each for `LoanListFabContent`, `LoanListLoadingSection`, `LoanListContentSection`,
  `LoanListEmptySection`, `LoanListErrorSection`, plus 3 component-level previews (`LoanListCard`
  ×2 — ACTIVE + OVERDUE — `LoanListCardSkeleton`, `LoanListFilterChips`).
- `composeResources/values/strings.xml` — 21 `screens_loan_list_*` keys (zero hardcoded UI
  literals — RULE-IMPL-NO-HARDCODED-STRING-001; `loan.memberName`/`loanProductName`/`status.name`
  render data-bound, not as resource literals).

## 3b. Consumers (screen layer)

- `LoanListRoute.kt#loanListScreen(...)` — wired into `cmp-navigation/.../GroupBankingNavHost.kt`
  (real navigation, replacing the prior `PlaceholderRoute("Loans")` target from `group-dashboard`).
- `feature/loan-list/src/commonMain/kotlin/.../LoanListScreenPreview.kt`.

## Evolution (screen layer)

- 2026-07-22 — `kmp-screen-gen`: `LoanListScreen.kt` (Container+Content, all 4 `screenState`
  branches, paginated `LazyColumn` with scroll-triggered `OnLoadNextPage`), 3 reusable component
  files, `LoanListRoute.kt` (wired into `GroupBankingNavHost.kt`), `LoanListTestTags` (10 tags + 1
  resolver fn), `LoanListScreenPreview.kt` (10 `@Preview`, 4 top-level state variants), and
  `strings.xml` (21 keys, zero hardcoded literals). Design ambiguities resolved (see also
  `API.md#screen`):
  1. `top_bar.on_nav_click`'s `OnBack` action is documented on `LoanListAction`'s own class KDoc
     as an idea-layer gap (not present in `state_model.actions.members`) — rather than inventing
     an unlisted action member, the back icon is wired as a PLAIN `onNavigateBack: () -> Unit`
     callback threaded straight through to `KptScaffold`'s `onNavigationIconClick`, bypassing
     `LoanListAction`/`LoanListEvent` entirely. Mirrors `GroupDashboardContent`'s identical
     "local nav/UI concern, not a ViewModel action" precedent for its top-bar overflow menu.
  2. `LoanListState.canApplyLoan` stays `false` end-to-end (see `LoanListViewModel`'s own KDoc gap
     note) — the Apply-Loan FAB is coded to `visible_when: canApplyLoan` per `ui.yaml`
     (`floatingActionButtonContent = if (state.canApplyLoan) ... else null`), so it does not
     currently render in this build; verified structurally correct against a preview variant with
     `canApplyLoan = true`.
  3. `ui.yaml#components.loan_card.content.loan_amount_text` templates ONLY `KES
     {{principalAmount}}`, but the resolved preview HTML (`preview/content.html`, higher priority
     per `UI_SOURCE_PRIORITY.md`) shows `KES {amount} · {productName}` combined on one line —
     `screens_loan_list_amount_product_label` follows the resolved preview, appending
     `loan.loanProductName` (i18n:skip, dynamic binding) to the templated amount.
<!-- kmp-screen-gen:END -->
