<!-- generated-by: kmp-screen-gen -->
# feature/member-savings-detail — Development

## 1. Module Identity

`:feature:member-savings-detail` — contribution-model-aware member savings detail: paginated
transaction statement with a 6-month sparkline trend and All/Deposits/Withdrawals filter chips.
Namespace `kpt.feature.membersavingsdetail`. Source of truth:
`idea-layer/screens/member-savings-detail/{ui,flow,data-flow}.yaml`.

## 2. Public API

See `MemberSavingsDetailViewModel.kt` class/field KDoc for the full ViewModel contract
(`MemberSavingsDetailState`, `MemberSavingsDetailEvent`, `MemberSavingsDetailAction`,
`MemberSavingsDetailScreenState`, `MemberSavingsError`) — not re-documented here to avoid drift;
this section is owned by `kmp-viewmodel-gen` on its next pass over this feature.

## 3. Consumers

- `feature/member-savings-detail/src/commonTest/.../MemberSavingsDetailViewModelTest.kt`.
- `MemberSavingsDetailRoute.kt` (this pass) — entry points per `ui.yaml#entry_points`:
  `savings-dashboard`'s `tap_member_row`, `member-profile`'s `view_full_history_button`. NOT yet
  wired into the app-level nav graph (`cmp-navigation`) — same as `member-profile`/
  `personal-savings`, whose Route registrations are also not yet wired; app-level NavHost wiring is
  an infra-layer follow-up out of scope for `kmp-screen-gen` (feature-scoped).

## 4. Boundaries

Reads exclusively via the injected `SavingsRepository` (`core/data`) — no direct API/DB access from
this module. `business_logic.kind: crud` per RULE-IMPLEMENT-STORE5-001 AC-03i — offset-based
pagination is driven by hand (`currentOffset`/`hasNextPage` in state), not a `PagingScreenStream`
(`SavingsRepository` has no Store5 branch registered yet — same documented gap as
`PersonalSavingsViewModel`/`LoanApplyViewModel`).

## 5. Data

See API.md#state / #actions / #events for the ViewModel field/payload schema and API.md#screen /
#route / #tags / #preview for the screen-layer contract.

## 6. Errors

`MemberSavingsError` (4 variants): `Network`, `Server` (both retryable), `NotFound`, `Auth` (both
non-retryable). The Content layer maps each to
`screens_member_savings_detail_error_{network,server,not_found,auth}`.

## 7. Testing

`feature/member-savings-detail/src/commonTest/kotlin/org/mifos/groupbanking/feature/membersavingsdetail/MemberSavingsDetailViewModelTest.kt`
(ViewModel layer, pre-existing). No `commonTest` UI-test suite yet for the screen layer — pending
`kmp-compose-uitest-gen` / `kmp-journey-uitest-gen`.

## 8. Observability

See `MemberSavingsDetailViewModel.kt` KDoc (Kermit `Logger.d/i/w` + `CrashReporter` breadcrumbs +
`KptAnalyticsTracker.trackSavingsOperation("view"/"refresh", accountId = memberId)`).

## 9. Evolution

<!-- kmp-screen-gen:BEGIN -->
## 2b. Screen / UI layer

- `MemberSavingsDetailScreen.kt` — Container (`MemberSavingsDetailScreen`) + stateless Content
  (`MemberSavingsDetailContent`), state-driven over all 4 `MemberSavingsDetailScreenState` members
  (Loading/Content/Empty/Error). Shared `MemberSavingsDetailHeaderSection` (header + sparkline +
  filters) reused by Content and Empty per `ui.yaml`'s identical per-state component lists.
- `MemberSavingsDetailRoute.kt` — `@Serializable data class MemberSavingsDetailRoute` (flattens
  `GroupTypeConfig` field-for-field, mirroring `GroupCreateRoute.kt`'s convention exactly, since
  `GroupTypeConfig` is not itself `@Serializable`) + `navigateToMemberSavingsDetail(...)` +
  `NavGraphBuilder.memberSavingsDetailScreen(onNavigateBack)` (1 required nav callback, no
  dead-clickable defaults — DC3 count-assertion: 1 default / 1 override / 0 suppressed).
- `components/` — `MemberSavingsHeaderCard` (contribution-model-adaptive balance display),
  `SavingsSparklineCard` (`Canvas` bar-chart, no external chart lib), `SavingsFilterChips`
  (All/Deposits/Withdrawals `FilterChip` row), `SavingsStatementRow` (colour-coded transaction row
  + in-place expand).
- `MemberSavingsDetailTestTags.kt` — 15 append-only test tags (SCREEN, HEADER_CARD, AVATAR,
  SPARKLINE_CARD, SPARKLINE, FILTER_CHIPS_ROW + 3 chip tags, TRANSACTION_LIST,
  LOAD_MORE_INDICATOR, LOADING_SECTION, EMPTY_SECTION, ERROR_SECTION, ERROR_RETRY_BUTTON) plus 2
  per-row resolver functions (`transactionRowTag`, `transactionExpandedTag`).
- `MemberSavingsDetailScreenPreview.kt` — 13 `@Preview` functions covering 7 state variants on the
  top-level Content preview (Loading / Content-SHARE_BASED_VARIABLE / Content-FIXED /
  Content-loading-next-page / Empty-filtered / Error-Network / Error-NotFound) plus 6 sub-composable
  and 4 component-level previews.
- `composeResources/values/strings.xml` — 34 `screens_member_savings_detail_*` keys (zero
  hardcoded UI literals — RULE-IMPL-NO-HARDCODED-STRING-001).

## 3b. Consumers (screen layer)

- `MemberSavingsDetailRoute.kt#memberSavingsDetailScreen(...)` — NOT YET wired into the app-level
  nav graph (`cmp-navigation`); wiring is out of scope for `kmp-screen-gen` (feature-scoped),
  tracked as an infra-layer follow-up alongside `member-profile`/`personal-savings`'s identical
  not-yet-wired Route registrations.
- `feature/member-savings-detail/src/commonMain/kotlin/.../MemberSavingsDetailScreenPreview.kt`.

## Evolution (screen layer)

- 2026-07-22 — `kmp-screen-gen`: `MemberSavingsDetailScreen.kt` (Container+Content, all 4
  `screenState` branches, shared header section, real hand-rolled pagination via
  `rememberLoadMoreTrigger` driven by the REAL `hasNextPage`/`isLoadingNextPage` flags), 4 reusable
  components, `MemberSavingsDetailRoute.kt` (GroupTypeConfig flattened per the `GroupCreateRoute.kt`
  convention), `MemberSavingsDetailTestTags` (15 tags + 2 resolver fns),
  `MemberSavingsDetailScreenPreview.kt` (13 `@Preview`, 7 top-level state variants sourced from
  `ui.yaml#states.content.demo_data`), and `strings.xml` (34 keys, zero hardcoded literals).
  Resolved two documented idea-layer drifts without silently reproducing them: (1)
  `ui.yaml#components.member_header_card.content.savings_balance_total.value` literally
  re-interpolates the per-share `shareValue` for the "= KES {total} total" line — this component
  instead computes the real total (`sharesHeld * shareValue`); (2)
  `ui.yaml#components.transaction_card.on_click`'s expanded-receipt description names a "meeting
  reference" field that `SavingsStatementEntry` does not carry — the expanded block surfaces only
  the real fields (transaction id, posting date, running balance). Both flagged for the
  cross-feature repair station (CFF1) per the ViewModel's own established "flag, don't silently
  invent" convention.
<!-- kmp-screen-gen:END -->
