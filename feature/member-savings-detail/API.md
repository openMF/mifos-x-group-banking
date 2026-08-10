<!-- generated-by: kmp-screen-gen -->
# feature/member-savings-detail — API

## viewmodel

`kpt.feature.membersavingsdetail.MemberSavingsDetailViewModel` — extends
`BaseViewModel<MemberSavingsDetailState, MemberSavingsDetailEvent, MemberSavingsDetailAction>`
(`kpt.core.base.ui.viewmodel.BaseViewModel`). Full field/action/event contract documented in the
ViewModel source KDoc — this section is owned by `kmp-viewmodel-gen` on its next pass.

## di

`kpt.feature.membersavingsdetail.di.MemberSavingsDetailModule` — Koin module,
`viewModel { parameters -> MemberSavingsDetailViewModel(..., memberId = parameters.get(), groupId = parameters.get(), typeConfig = parameters.get()) }`
(nav-param constructor args, not a plain `viewModelOf` binding). Already included in
`KoinModules.kt#featureModule`.

<!-- kmp-screen-gen:BEGIN -->
## screen

- `MemberSavingsDetailScreen(memberId: String, groupId: String, typeConfig: GroupTypeConfig, onNavigateBack: () -> Unit, modifier: Modifier = Modifier, viewModel: MemberSavingsDetailViewModel = koinViewModel(parameters = { parametersOf(memberId, groupId, typeConfig) }))`
  — Container. Collects `viewModel.stateFlow` via `collectAsStateWithLifecycle`, routes
  `MemberSavingsDetailEvent.NavigateBack` → `onNavigateBack()` and
  `MemberSavingsDetailEvent.ShowSnackbar(message)` → resolved snackbar text through `EventsEffect`,
  delegates render to `MemberSavingsDetailContent`.
- `MemberSavingsDetailContent(state: MemberSavingsDetailState, onAction: (MemberSavingsDetailAction) -> Unit, modifier: Modifier = Modifier, snackbarHostState: SnackbarHostState = remember { SnackbarHostState() })`
  — stateless. `KptScaffold(showNavigationIcon = true, onNavigationIconClick = { onAction(OnBack) }, title = ..., pullToRefreshState = ...)` +
  `when (state.deriveScreenState())`:
  - `Loading` → `MemberSavingsDetailLoadingSection()` (6× shimmer block)
  - `Content` → `MemberSavingsDetailContentSection(state, onAction)` (paginated `LazyColumn`)
  - `Empty` → `MemberSavingsDetailEmptySection(state, onAction)` (header/sparkline/filters + empty illustration)
  - `Error` → `MemberSavingsDetailErrorSection(state, onAction)` (full-screen error + conditional Retry)
- `MemberSavingsDetailHeaderSection(state, onAction, modifier)` — shared header block
  (`MemberSavingsHeaderCard` + conditional `SavingsSparklineCard` + `SavingsFilterChips`), reused by
  both `Content` and `Empty` (`ui.yaml#states.content.components`/`#states.empty.components` both
  list the same 3 components).
- `MemberSavingsDetailLoadingSection` / `MemberSavingsDetailSkeletonBlock` — Loading sub-composables.
- `MemberSavingsDetailContentSection` — paginated `LazyColumn`; scroll-to-end dispatches `OnLoadMore`
  via `rememberLoadMoreTrigger(hasMore = state.hasNextPage, isLoadingMore = state.isLoadingNextPage)`
  (REAL paging-progress flags, unlike `loan-list`'s `hasMore = true` approximation).
- `MemberSavingsDetailEmptySection` / `MemberSavingsDetailErrorSection` — terminal-state sub-composables.

### components

| Component | Signature | Notes |
|---|---|---|
| `MemberSavingsHeaderCard` | `(member: SavingsMember, contributionModel: String, sharesHeld: Int?, shareValue: Long?, savingsBalance: Double, savingsAccountNo: String, modifier: Modifier = Modifier)` | Avatar (initials fallback) + name + contribution-model-adaptive balance (SHARE_BASED_VARIABLE shows "X shares @ KES Y/share" + computed total; else plain savings balance) + account number. |
| `SavingsSparklineCard` | `(points: List<SavingsDataPoint>, modifier: Modifier = Modifier)` | `Canvas` bar-chart sparkline, no external chart lib; only rendered when `points.isNotEmpty()`. |
| `SavingsFilterChips` | `(selectedFilter: SavingsTransactionFilter, onFilterSelected: (SavingsTransactionFilter) -> Unit, modifier: Modifier = Modifier)` | 3 `FilterChip`s (All/Deposits/Withdrawals), `LazyRow`, client-side transform only. |
| `SavingsStatementRow` | `(entry: SavingsStatementEntry, isExpanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, testTag: String = "", expandedTestTag: String = "")` | Colour-coded leading icon/amount by `SavingsTransactionType`, `REVERSED` badge, in-place expand reveals transaction id/date/running balance (meeting reference has no `SavingsStatementEntry` field — documented gap, not fabricated). |

## route

`MemberSavingsDetailRoute.kt`:

- `@Serializable data class MemberSavingsDetailRoute(memberId, groupId, typeSlug, displayName, tagline, savingsMechanism, contributionMode, lendingEnabled, hasSocialFund, hasBankLinkage, welfareOnlyMode, formallyRegistered, defaultLoanMultiplier, defaultInterestRatePct, defaultCycleLengthMonths, maxMembers, minMembers)`
  — `/groups/{groupId}/members/{memberId}/savings`. `GroupTypeConfig` (`core/model`) is a plain,
  non-`@Serializable` domain `data class`, so every field is flattened into the route — IDENTICAL
  convention to `GroupCreateRoute.kt`'s `toGroupCreateRoute()`/`toGroupTypeConfig()` round-trip.
- `GroupTypeConfig.toMemberSavingsDetailRoute(memberId, groupId)` / `MemberSavingsDetailRoute.toGroupTypeConfig()`
  — lossless field-for-field round-trip.
- `NavController.navigateToMemberSavingsDetail(memberId: String, groupId: String, typeConfig: GroupTypeConfig, navOptions: NavOptions? = null)`.
- `NavGraphBuilder.memberSavingsDetailScreen(onNavigateBack: () -> Unit)` — registers
  `MemberSavingsDetailScreen` via `composableWithRootPushTransitions<MemberSavingsDetailRoute>`.
  DC3 count-assertion: 1 `onNavigateBack: () -> Unit` param (required, no `= {}` default) — 1
  override wired in the Route registration — 0 suppressed.

`flow.yaml#navigates_to`: `[]` — `member-savings-detail` is a terminal read-only leaf screen; every
interactive element besides the back arrow is an in-place filter/expand/refresh client-side
transform, not a navigation.

## tags

`MemberSavingsDetailTestTags` (append-only, RULE-KMP-COMPOSE-UITEST-001 CU-5):

| Constant | Value |
|---|---|
| `SCREEN` | `member_savings_detail_screen` |
| `HEADER_CARD` | `member_savings_detail_header_card` |
| `AVATAR` | `member_savings_detail_avatar` |
| `SPARKLINE_CARD` | `member_savings_detail_sparkline_card` |
| `SPARKLINE` | `member_savings_detail_sparkline` |
| `FILTER_CHIPS_ROW` | `member_savings_detail_filter_chips_row` |
| `FILTER_CHIP_ALL` | `member_savings_detail_filter_chip_all` |
| `FILTER_CHIP_DEPOSITS` | `member_savings_detail_filter_chip_deposits` |
| `FILTER_CHIP_WITHDRAWALS` | `member_savings_detail_filter_chip_withdrawals` |
| `TRANSACTION_LIST` | `member_savings_detail_transaction_list` |
| `LOAD_MORE_INDICATOR` | `member_savings_detail_load_more_indicator` |
| `LOADING_SECTION` | `member_savings_detail_loading_section` |
| `EMPTY_SECTION` | `member_savings_detail_empty_section` |
| `ERROR_SECTION` | `member_savings_detail_error_section` |
| `ERROR_RETRY_BUTTON` | `member_savings_detail_error_retry_button` |

Plus `fun transactionRowTag(transactionId: String): String` and
`fun transactionExpandedTag(transactionId: String): String` — resolve the stable per-row constants
(rows render data-driven from `filteredTransactions`, not as N separate composables).

## preview

`MemberSavingsDetailScreenPreview.kt` — 13 `@Preview` functions:

- `MemberSavingsDetailContentPreview` (top-level, `PreviewParameterProvider<MemberSavingsDetailState>`,
  7 variants: Loading / Content-SHARE_BASED_VARIABLE / Content-FIXED / Content-loading-next-page /
  Empty-filtered / Error-Network / Error-NotFound).
- `MemberSavingsDetailLoadingSectionPreview`, `MemberSavingsDetailSkeletonBlockPreview`,
  `MemberSavingsDetailHeaderSectionPreview`, `MemberSavingsDetailContentSectionPreview`,
  `MemberSavingsDetailEmptySectionPreview`, `MemberSavingsDetailErrorSectionPreview`.
- `MemberSavingsHeaderCardShareBasedPreview`, `MemberSavingsHeaderCardFixedPreview`,
  `SavingsSparklineCardPreview`, `SavingsFilterChipsPreview`, `SavingsStatementRowDepositPreview`,
  `SavingsStatementRowExpandedPreview`.

Data source: `idea-layer/screens/member-savings-detail/ui.yaml#states.content.demo_data` (Amina
Wanjiru, `grp-001`, SHARE_BASED_VARIABLE — `sharesHeld: 20`, `shareValue: 1000` per-share from
`typeConfig.share_value`, computed total `20000`) — the top-level `demo_data.shareValue: 20000`
field is a documented ui.yaml unit-confusion (see `MemberSavingsHeaderCard.kt` KDoc), so the
per-share `typeConfig.share_value: 1000` was used instead of blindly re-copying the ambiguous
top-level literal.

## i18n

`composeResources/values/strings.xml` — 34 keys under the `screens_member_savings_detail_*`
namespace: `screen_title`, `avatar_cd_format`, `balance_label_{shares,savings}`,
`{shares_at,balance_amount,shares_total,account_no}_format`, `trend_label`, `sparkline_cd_format`,
`filter_{all,deposits,withdrawals}`, `txn_type_{deposit,withdrawal,interest,fee,transfer,unknown}`,
`amount_{deposit,withdrawal,default}_format`, `running_balance_format`, `reversed_label`,
`txn_row_cd_format`, `txn_expanded_{id,date,balance}_format`, `load_more_cd`, `shimmer_cd`,
`empty_{icon_cd,title,body}`, `error_icon_cd`, `error_title`, `action_retry`,
`error_{network,server,not_found,auth}`. Zero hardcoded UI literals
(RULE-IMPL-NO-HARDCODED-STRING-001) — `entry.date`/`entry.type.description`-style dynamic values
carry `// i18n:skip` per `ui.yaml`'s own `i18n: skip` markers.

## permissions

None — `member-savings-detail` requires no runtime permission (`ui.yaml` declares no
`request-permission` `on_click`).
<!-- kmp-screen-gen:END -->
