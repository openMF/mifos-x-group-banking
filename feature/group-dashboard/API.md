<!-- generated-by: kmp-screen-gen -->
# feature/group-dashboard — API

<!-- kmp-viewmodel-gen:BEGIN -->
## viewmodel

`org.mifos.groupbanking.feature.groupdashboard.GroupDashboardViewModel` — extends
`BaseViewModel<GroupDashboardState, GroupDashboardEvent, GroupDashboardAction>`
(`kpt.core.base.ui.viewmodel.BaseViewModel`). Constructor deps: `GroupDashboardRepository`
(`core/data`, composite fan-in of `get_group` + `get_viewer_role` + `get_group_corpus` +
`get_group_accounts`), `SessionManager` (`core-base/security`), `CrashReporter`
(`core-base/observability`), `KptAnalyticsTracker` (`core/analytics`), plus the `groupId` /
`viewerRole` nav-args (Koin `parametersOf`, NOT DI-graph types).

## state

`GroupDashboardState` (`@Serializable @Immutable`):

| Field | Type | Default | Transient |
|---|---|---|---|
| `isLoading` | `Boolean` | `true` | no |
| `group` | `GroupDetail?` | `null` | yes |
| `corpus` | `GroupCorpus?` | `null` | yes |
| `config` | `GroupConfig?` | `null` | yes |
| `typeConfig` | `GroupInstanceConfig?` | `null` | yes |
| `groupTypeName` | `String` | `""` | no |
| `viewerRole` | `String` (`ORGANIZER`/`MEMBER`/`TREASURER`/`CHAIRPERSON`) | `"MEMBER"` | no |
| `accounts` | `GroupAccounts?` | `null` | yes |
| `recentActivity` | `List<ActivityItem>` | `emptyList()` | yes |
| `isCorpusInsufficient` | `Boolean` | `false` | no |
| `isCycleEnd` | `Boolean` | `false` | no |
| `rotationPosition` | `Int?` | `null` | no |
| `nextRecipientName` | `String?` | `null` | no |
| `nextRecipientPosition` | `Int?` | `null` | no |
| `shareOutProjection` | `Double?` | `null` | no |
| `error` | `GroupDashboardError?` | `null` | yes |

`GroupDashboardState.screenState` (derived extension) → `GroupDashboardScreenState`
(`Loading` / `Content` / `Error` — no `Empty` variant; a single-group composite is never empty
once present). `error != null` → `Error`; `isLoading` → `Loading`; else → `Content`.

`GroupDashboardError`: `Network` (`error_network`, retry), `Server` (`error_server`, retry),
`NotFound` (`error_not_found`, non-retry — see class KDoc "reachability gap": currently
unreachable in production, falls through to `Server` today), `Auth` (`error_auth`, non-retry —
also triggers `SessionManager.endSession()`).

## actions

| Action | Payload | Effect |
|---|---|---|
| `OnStartMeeting` | — | management role: corpus-sufficiency check + `ShowCorpusBlockedDialog` if insufficient, then navigate; member role: navigate only (no check) |
| `OnViewMembers` | — | emits `NavigateToMemberList(groupId)` |
| `OnViewLoans` | — | emits `NavigateToLoanList(groupId)` (shared by both role grids) |
| `OnShareOut` | — | ORGANIZER/TREASURER + `isCycleEnd` only; else `ShowSnackbar(share_out_not_available)`; unauthorized-role dispatch is defensively logged + ignored |
| `OnViewSavings` | — | member role only; emits `NavigateToMemberSavingsDetail(groupId)` |
| `OnRefresh` | — | `dashboardStream.refreshFresh()`; sets `isLoading = true` |
| `Retry` | — | `dashboardStream.retry()`; sets `isLoading = true`, clears `error` |
| `OnBack` | — | emits `NavigateBack` |
| `Internal.StreamUpdated` | `screenState: ScreenState<GroupDashboard>` | routes to the pool-model-adaptive stream→state mapper; not user-dispatched |

## events

| Event | Payload | Trigger |
|---|---|---|
| `NavigateToMeetingCalendar` | `groupId: String` | `OnStartMeeting` |
| `NavigateToMemberList` | `groupId: String` | `OnViewMembers` |
| `NavigateToLoanList` | `groupId: String` | `OnViewLoans` |
| `NavigateToShareOut` | `groupId: String, distributionStrategy: String` | `OnShareOut` (cycle-end + authorized role) |
| `ShowCorpusBlockedDialog` | — | `OnStartMeeting` when `isCorpusInsufficient` (management role) |
| `ShowSnackbar` | `message: String` (messageKey) | `OnShareOut` (not cycle-end), `Unauthenticated` (401) |
| `NavigateBack` | — | `OnBack` — flagged addition, `ui.yaml#events.members` doesn't declare it but `top_bar.on_navigation_click` wires `OnBack` with a full `action_contract` |
| `NavigateToMemberSavingsDetail` | `groupId: String` | `OnViewSavings` — flagged addition, same class of gap as `NavigateBack` |

## di

`org.mifos.groupbanking.feature.groupdashboard.di.GroupDashboardModule` — Koin module,
`viewModel { parameters -> GroupDashboardViewModel(..., groupId = parameters.get(), viewerRole =
parameters.get()) }` (not `viewModelOf` — `groupId`/`viewerRole` are nav-args, not DI-graph
types). Included in `KoinModules.kt#featureModule`. `GroupDashboardRepository` resolved from
`DataModule`, `SessionManager` from `SecurityModule`, `CrashReporter` from `observabilityModule`,
`KptAnalyticsTracker` from the single process-wide binding supplied by `LoginSignupModule`.
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## screen

**Container** — `GroupDashboardScreen(groupId, viewerRole, onNavigateToMeetingCalendar,
onNavigateToMemberList, onNavigateToLoanList, onNavigateToShareOut,
onNavigateToMemberSavingsDetail, onNavigateBack, modifier, viewModel)`. Collects
`GroupDashboardViewModel.stateFlow` via `collectAsStateWithLifecycle`, consumes
`GroupDashboardEvent`s through `EventsEffect` (6 nav branches + `ShowCorpusBlockedDialog` → local
dialog state + `ShowSnackbar` → resolved snackbar), delegates rendering to the stateless
`GroupDashboardContent`. `groupId`/`viewerRole` are forwarded to `koinViewModel(parameters = {
parametersOf(groupId, viewerRole) })`.

**Content** — `GroupDashboardContent(state: GroupDashboardState, onAction: (GroupDashboardAction)
-> Unit, modifier, snackbarHostState)`. State-driven per `GroupDashboardState.screenState`
(Loading / Content / Error). Pull-to-refresh wired at the `KptScaffold` level, dispatching
`GroupDashboardAction.OnRefresh` (`isRefreshing` is a constant `false` — `GroupDashboardState`
carries no distinct refresh-in-progress flag; `OnRefresh` swaps the whole screen to the Loading
skeleton). Top-bar overflow icon toggles a local `DropdownMenu` (pure Compose UI state, no
ViewModel action — no menu items declared anywhere in `ui.yaml`, none invented).

Sub-composables (all `internal`, previewed individually):
- `GroupDashboardLoadingSection` / `GroupDashboardSkeletonBlock` — Loading state, 4 shimmer blocks.
- `GroupDashboardContentSection` — header + pool-model-adaptive metric card + role-gated quick
  actions + savings summary + activity feed, inside one `LazyColumn`.
- `GroupDashboardErrorSection` — error state with conditional Retry CTA (`GroupDashboardError.retry`).

Reusable components (`components/` package): `GroupHeaderCard`, `CorpusMetricCard`
(ACCUMULATING), `RotationMetricCard` (ROTATING_PAYOUT), `QuickActionsSection` (role-gated
management/member grids), `GroupSavingsSummaryCard`, `ActivityFeedSection` + `ActivityRow`,
`CorpusBlockedDialog`.

**Flagged idea-layer gaps (not invented)**:
1. `top_bar.actions[0]` (`OnMoreOptions`, overflow icon) has no matching `GroupDashboardAction`
   member and no declared menu-item set for `group-dashboard-more-menu` — implemented as a real
   local Compose toggle (not a dead click, `effect: transform_state` per its own
   `action_contract`) with an empty `DropdownMenu` body.
2. `flow.yaml#navigates_to[]` lists `meeting-calendar` / `member-list` / `loan-list` /
   `share-out-preview` — it does **not** list `member-savings-detail`, even though
   `ui.yaml#components.view_savings_button.on_click.target` declares it and
   `GroupDashboardEvent.NavigateToMemberSavingsDetail` is wired end-to-end. This is a genuine
   `flow.yaml` completeness gap (RULE-UI-SOURCE-001 US4 nav-check) surfaced here, not silently
   patched around — `flow.yaml#navigates_to[]` needs `member-savings-detail` appended.

## route

`GroupDashboardRoute` (`@Serializable data class(groupId: String, viewerRole: String)`) — path
`/groups/{groupId}`. `NavController.navigateToGroupDashboard(groupId, viewerRole, navOptions)`.
`NavGraphBuilder.groupDashboardScreen(onNavigateToMeetingCalendar, onNavigateToMemberList,
onNavigateToLoanList, onNavigateToShareOut, onNavigateToMemberSavingsDetail, onNavigateBack)`
registers the composable destination via `composableWithRootPushTransitions`.

Nav-arg contract — all 6 callbacks close a `GroupDashboardEvent` branch, none carries a `= {}`
default (DC3 count-assertion: 0 defaults / 0 overrides / 0 suppressed). `meeting-calendar` /
`member-list` / `loan-list` / `share-out-preview` / `member-savings-detail` are not yet generated
feature modules in this codebase — typed callbacks for the host to wire later.

## preview

`GroupDashboardScreenPreview.kt` — 16 `@Preview` functions. Data source: `demo-data.yaml`
(`demo_data_resolved = true`) — Mwangaza Women's Group / VSLA / ACCUMULATING / ORGANIZER and
Jiunge ROSCA Circle / ROSCA / ROTATING_PAYOUT / MEMBER seeded rows. Covers Loading/Content
(both pool-model variants)/Error top-level states plus every sub-composable and reusable
component (both role-gated `QuickActionsSection` variants, corpus-sufficient and
corpus-insufficient `CorpusMetricCard`).

## tags

`GroupDashboardTestTags` (append-only, RULE-KMP-COMPOSE-UITEST-001 CU-5): `SCREEN`,
`MORE_OPTIONS_BUTTON`, `HEADER_CARD`, `GROUP_TYPE_CHIP`, `VIEWER_ROLE_CHIP`,
`MEMBER_COUNT_CHIP`, `OVERDUE_LOANS_CHIP`, `CORPUS_CARD`, `CORPUS_BLOCKED_BANNER`,
`ROTATION_CARD`, `QUICK_ACTIONS_SECTION`, `START_MEETING_BUTTON`, `VIEW_MEMBERS_BUTTON`,
`VIEW_LOANS_BUTTON`, `SHARE_OUT_BUTTON`, `VIEW_SAVINGS_BUTTON`, `VIEW_LOANS_MEMBER_BUTTON`,
`VIEW_MEETINGS_BUTTON`, `VIEW_MEMBERS_MEMBER_BUTTON`, `SAVINGS_SUMMARY_CARD`,
`ACTIVITY_FEED_SECTION`, `CONTENT_LIST`, `LOADING_SECTION`, `ERROR_SECTION`,
`ERROR_RETRY_BUTTON`, `CORPUS_BLOCKED_DIALOG`, `CORPUS_BLOCKED_DIALOG_CONFIRM` (27 constants),
plus `activityRowTag(activityId)` resolver function.

## permissions

Not applicable — this screen requires no runtime permission (`ui.yaml` declares no
`request-permission` `on_click`).
<!-- kmp-screen-gen:END -->
