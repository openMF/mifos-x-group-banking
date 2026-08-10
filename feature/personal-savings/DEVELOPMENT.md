<!-- generated-by: kmp-viewmodel-gen -->
# feature/personal-savings — Development

## 1. Module Identity

`:feature:personal-savings` — the member's own tabbed savings ledger (`/savings`), entered from
`personal-dashboard`'s `user_taps_savings_card` entry point. Namespace
`kpt.feature.personalsavings`. Source of truth:
`idea-layer/screens/personal-savings/{ui,data-flow}.yaml`.

<!-- kmp-viewmodel-gen:BEGIN -->
## 2. Public API

See `PersonalSavingsViewModel.kt` class/field KDoc for the full ViewModel contract
(`PersonalSavingsState`, `PersonalSavingsEvent`, `PersonalSavingsAction`,
`PersonalSavingsScreenState`, `SavingsError`) — not re-documented here to avoid drift; see
`API.md#viewmodel` / `#state` / `#actions` / `#events` / `#di`.

## 3. Consumers

- `feature/personal-savings/src/commonTest/.../PersonalSavingsViewModelTest.kt`.
- `cmp-navigation` — `PersonalSavingsModule` included in `KoinModules.kt#featureModule`;
  `implementation(projects.feature.personalSavings)` added to `cmp-navigation/build.gradle.kts`.
  `PersonalSavingsScreen.kt` / `PersonalSavingsRoute.kt` are `kmp-screen-gen` scope (not generated
  this pass).

## 4. Boundaries

Reads exclusively via the injected `SavingsRepository` (`core/data`) —
`loadMemberSavings(groupLinkedSavingsId, individualSavingsId)` on mount,
`getSavingsTransactions(activeTabSavingsId)` on `OnRefresh`/`OnRetry` — no direct Ktor/SQLDelight
access from this module. `business_logic.kind: crud` per ui.yaml — the legacy direct-`NetworkResult`
template path (both repository methods are plain `suspend fun`s, no Store5 wrap; see
`SavingsRepository` KDoc "Store5 branch" note). `SessionManager` (`core-base/security`) is injected
and used for its one real capability — `endSession()` on a 401. Tab switching is a pure in-memory
`PersonalSavingsState` transition with no network call (both accounts are already eagerly loaded
together on mount — see gap (1) below).

## 5. Data

See `API.md#state` / `#actions` / `#events` for the ViewModel field/payload schema.

## 6. Errors

`SavingsError` (3 variants): `Network`/`Server` (`retry: true`), `Unauthorized` (`retry: false`,
calls `sessionManager.endSession()` for real). `NetworkError.BAD_REQUEST`/`NOT_FOUND`/`SERVER`/
`SERIALIZATION`/`UNKNOWN` all fold into `SavingsError.Server`; `TOO_MANY_REQUESTS` folds onto
`Unauthorized` — no dedicated `RateLimited` bucket declared in `ui.yaml#state_model.errors.types`.

## 7. Testing

`feature/personal-savings/src/commonTest/kotlin/org/mifos/groupbanking/feature/personalsavings/PersonalSavingsViewModelTest.kt`
(13 tests — initial-state nav-arg seeding, on_mount balance-from-newest-runningBalance derivation
(deliberately out-of-order fixture list to prove sort-by-date, not `firstOrNull`), on_mount with
null `individualSavingsId` → individual side stays empty/zero, on_mount zero-transactions →
balance zero, on_mount SERVER → `Error` screen state, on_mount UNAUTHORIZED → real
`sessionManager.endSession()`, `OnTabSelected` pure transform with zero re-fetch, `OnRefresh` on
GROUP_LINKED re-fetches only that account, `OnRefresh` on INDIVIDUAL re-fetches only that account,
`OnRetry` error recovery, `OnRetry` NETWORK error mapping). No `commonTest` UI-test suite yet for
the screen layer — pending `kmp-compose-uitest-gen` / `kmp-journey-uitest-gen` (screen not
generated this pass).

## 8. Observability

See `PersonalSavingsViewModel.kt` KDoc (Kermit `Logger.i/d` + `CrashReporter` breadcrumbs +
`KptAnalyticsTracker.trackSavingsOperation(...)` on mount and refresh).

## 9. Evolution

- 2026-07-22 — `kmp-viewmodel-gen`: `PersonalSavingsViewModel.kt` (state/action/event/error sealed
  types, composite on_mount fetch via `loadMemberSavings`, single-account bypass fetch on
  `OnRefresh`/`OnRetry` via `getSavingsTransactions`, pure-transform tab switch, balance-from-
  newest-runningBalance derivation, 401 → real `sessionManager.endSession()`),
  `PersonalSavingsModule.kt` (Koin, `clientId`/`groupLinkedSavingsId`/`individualSavingsId`
  nav-args via `parametersOf`), feature module scaffold (`build.gradle.kts`,
  `settings.gradle.kts` include, `cmp-navigation` dependency + `KoinModules.kt` registration).
  Flagged idea-layer gaps (not invented): (1) `data-flow.yaml#entries[on_interact:
  tab_switch_to_individual]` describes the individual account as lazy-loaded on first tab open,
  but `SavingsRepository.loadMemberSavings`'s own KDoc fetches BOTH accounts concurrently
  whenever `individualSavingsId != null` — i.e. on THIS screen's initial mount, not on tab
  switch; `OnTabSelected` is therefore a pure `transform_state` action with no re-fetch branch —
  flagged for RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1; (2) `ui.yaml#state_model.state.fields`
  declares `groupLinkedTransactions`/`individualTransactions` as `List<SavingsTransactionDto>`,
  but `SavingsTransactionDto` (`core/network/model`) is a DIFFERENT, incompatible
  `personal-dashboard`-companion-card shape — this ViewModel uses the real domain type returned
  by `SavingsRepository`, `SavingsLedgerEntry` (`core/model`), instead — flagged for
  RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1 (`ui.yaml` should reference `SavingsLedgerEntry`
  directly); (3) `contributionTarget` (default `500.0`) has no API source on this screen's
  declared DI graph (`SavingsRepository`, `SessionManager` only) — kept as a real literal default,
  never fabricated as a fetched value — flagged for RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1;
  (4) `meetingsAttended`/`totalMeetings` (both default `0`) have no meetings/attendance API
  reachable from this screen — kept honestly at `0`/`0` rather than invented numbers — flagged for
  RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1; (5) `ui.yaml#components.top_bar.on_navigation_click.
  action: NavigateBack` has no matching entry in `ui.yaml#state_model.actions.members` (the
  3-member list `PersonalSavingsAction` mirrors verbatim) — same drift class as
  `personal-loans`'s documented top-bar gap — flagged for RULE-IDEA-ACTION-CONTRACT-001.
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## 10. Screen Layer

`PersonalSavingsScreen.kt` / `PersonalSavingsScreenPreview.kt` / `PersonalSavingsTestTags.kt` /
`PersonalSavingsRoute.kt` — generated by `kmp-screen-gen` (2026-07-22). Container
(`PersonalSavingsScreen`, `koinViewModel(parameters = { parametersOf(clientId,
groupLinkedSavingsId, individualSavingsId) })`) + stateless Content (`PersonalSavingsContent`),
state-driven per `PersonalSavingsState.deriveScreenState()` (Loading/Content/Error, all 3
branches handled — `PersonalSavingsLoadingSection` / `PersonalSavingsContentSection` /
`PersonalSavingsErrorSection`). `SavingsTabRow` renders outside the state `when` — every declared
`ui.yaml#states` entry includes it.

- **Tab row** (`components/SavingsTabRow.kt`) — Material3 `TabRow` (not a literal chip-group as
  `ui.yaml#components.savings_tab_row.type: chip-group` styles it — built-in a11y/indicator
  traded for the pill visual, flagged not silently dropped). The INDIVIDUAL tab is omitted
  entirely (not merely disabled) when `individualSavingsId == null`
  (`ui.yaml#components.savings_tab_row.chips[1].visible_when`).
- **Balance hero card** (`components/SavingsBalanceHeroCard.kt`) — active-tab balance (KES,
  grouped via `formatGrouped(0)`) + account number, on a `primary`-toned `Card`
  (`ui.yaml#components.balance_hero_card.style.background: primary`).
- **Contribution progress card** (`components/SavingsContributionProgressCard.kt`) — visible only
  when `selectedTab == GROUP_LINKED`; meetings-attended `LinearProgressIndicator` (division-by-zero
  guarded when `totalMeetings == 0`) + current-cycle balance line. `meetingsAttended`/
  `totalMeetings` render the ViewModel's honest `0`/`0` default when no meetings API is reachable
  (see `DEVELOPMENT.md#9` gap (4) — not re-invented here).
- **Transaction row** (`components/SavingsTransactionRow.kt`) — leading circular deposit/withdrawal
  icon, headline (Fineract `transactionType.description`, `i18n:skip`), supporting date
  (`transaction.date.toString()`, ISO-8601 — no shared date-format utility exists in this
  codebase, so the raw `kotlinx.datetime.LocalDate` string is used rather than fabricating one),
  trailing signed amount + running balance. **Documented heuristic** — `SavingsLedgerTransactionType`
  is a raw untyped Fineract `{value, code, description}` mirror (no invented enum, Hard Rule 4),
  so credit-vs-debit coloring is derived from `code` containing `"deposit"`/`"interestposting"`
  (private `isCreditTransaction()`) — flagged for RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1 if a
  dedicated boolean is later added.
- **Empty-individual promo** (`components/SavingsEmptyIndividualState.kt`) —
  `ui.yaml#components.empty_individual_promo`, rendered when `selectedTab == INDIVIDUAL &&
  individualSavingsId == null`. In practice unreachable via tap (the tab itself is hidden in that
  case — see `SavingsTabRow` above) but implemented for real per the ui.yaml `visible_when`
  contract rather than silently dropped.
- **Loading skeleton** (`components/SavingsTransactionSkeletonRow.kt`) — 6 shimmering rows
  (`ui.yaml#components.shimmer_loading.count: 6`), mirrors `PersonalLoansCardSkeleton`'s identical
  infinite-transition-alpha convention.
- **Pull-to-refresh** — `rememberKptPullToRefreshState(isEnabled = screenState == Content, ...)`
  dispatches `PersonalSavingsAction.OnRefresh`.

**Stitch mockup drift (flagged, not followed):** `idea-layer/mockups/personal-savings/stitch/
01-personal-savings-content/code.html` renders an entirely different screen shape (no tabs, a
3-way Group/Emergency/Personal breakdown grid, a FAB, and a bottom nav bar) that does not match
`ui.yaml#components`/`state_model` at all (no `SavingsTab.GROUP_LINKED`/`INDIVIDUAL` concept, no
`meetingsAttended`/`contributionTarget` fields). Per `UI_SOURCE_PRIORITY.md` the Stitch render
normally outranks `ui.yaml`, but this render is stale drift against a screen that has since been
re-specified around the tabbed group-linked/individual ledger this ViewModel actually implements
— `ui.yaml` (which matches `PersonalSavingsViewModel`'s real fields 1:1) was followed instead.
Flagged for `idea-store-assets-generate`/Stitch regeneration.

### Route

`PersonalSavingsRoute.kt` — `@Serializable data class PersonalSavingsRoute(val clientId: Long, val
groupLinkedSavingsId: Long, val individualSavingsId: Long? = null)` + `navigateToPersonalSavings(
clientId, groupLinkedSavingsId, individualSavingsId)` + `NavGraphBuilder.personalSavingsScreen(
onNavigateBack)`. Only 1 callback — matches `flow.yaml#navigates_to` (`personal-dashboard` only,
`condition: user_taps_back`). DC3 count-assertion: 1 default / 1 override / 0 suppressed. NOT yet
wired into `cmp-navigation`'s nav graph builder (separate cross-feature task).

### Preview

`PersonalSavingsScreenPreview.kt` — 15 `@Preview` functions. Top-level `PersonalSavingsContent`
driven by a `PreviewParameterProvider<PersonalSavingsState>` with 6 variants (Loading, Content
GROUP_LINKED, Content INDIVIDUAL-with-account, Content INDIVIDUAL-no-account-empty-promo,
Error-Network, Error-Unauthorized-non-retryable) sourced from `demo-data.yaml#entries[0..2]`
(`PersonalSavingsState` + both `SavingsTransactionDto*` series, `clientId: 101`) mapped onto
`SavingsLedgerEntry`. Every non-container composable in `PersonalSavingsScreen.kt`
(`PersonalSavingsContent`, `PersonalSavingsLoadingSection`, `PersonalSavingsContentSection` × 2
variants, `PersonalSavingsErrorSection` × 2 variants incl. non-retryable `Unauthorized`) plus every
`components/` composable (`SavingsTabRow` × 2, `SavingsBalanceHeroCard`,
`SavingsContributionProgressCard`, `SavingsTransactionRow`, `SavingsTransactionSkeletonRow`,
`SavingsEmptyIndividualState`) is covered.

### TestTags

`PersonalSavingsTestTags.kt` — 11 `const val` + 1 stable-key function
(`transactionRowTag(transactionId)`). Append-only per RULE-KMP-COMPOSE-UITEST-001 CU-5.

### i18n

30 keys in `commonMain/composeResources/values/strings.xml`, namespace
`screens_personal_savings_*`. Zero hardcoded `Text("…")` / `contentDescription = "…"` literals.
`transaction.type.description` and `transaction.date.toString()` (dynamic Fineract-sourced values)
are the only `// i18n:skip` lines, mirroring `personal-loans`'s `loan.status.name` convention.

### Permissions

None — `ui.yaml` declares no `on_click` with `action: request-permission` for this screen; no
`remember*PermissionRequester()` invocation was emitted.
<!-- kmp-screen-gen:END -->
