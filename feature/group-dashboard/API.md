<!-- generated-by: kmp-screen-gen -->
# feature/group-dashboard — API

<!-- kmp-viewmodel-gen:BEGIN -->
## viewmodel

`kpt.feature.groupdashboard.GroupDashboardViewModel` — extends
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
| `OnViewSavings` | — | member role only; emits `NavigateToSavingsDashboard(groupId)` (G9 — self-scoped to the group's savings dashboard) |
| `OnMoreOptions` | — | G13 — toggles `isMoreMenuExpanded` (top-bar overflow dropdown) |
| `OnGroupSettings` | — | G13 — closes the menu + emits `NavigateToSettings` |
| `OnSyncStatus` | — | G13 — closes the menu + emits `NavigateToSyncStatus` |
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
| `NavigateToSavingsDashboard` | `groupId: String` | `OnViewSavings` (G9) — MEMBER "My Savings" self-scoped to the group's savings-dashboard (replaces the prior `member-savings-detail` mis-route); `typeConfig` nav-param degrades to default at this seam (dashboard holds `GroupInstanceConfig`, not the catalogue `GroupTypeConfig`) |
| `NavigateToSettings` | — | `OnGroupSettings` (G13) — overflow menu → shared settings screen |
| `NavigateToSyncStatus` | — | `OnSyncStatus` (G13) — overflow menu → shared sync-status dashboard |
| `NavigateBack` | — | `OnBack` — flagged addition, `ui.yaml#events.members` doesn't declare it but `top_bar.on_navigation_click` wires `OnBack` with a full `action_contract` |

## di

`kpt.feature.groupdashboard.di.GroupDashboardModule` — Koin module,
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
onNavigateToSavingsDashboard, onNavigateToSettings, onNavigateToSyncStatus, onNavigateBack,
modifier, viewModel)`. Collects `GroupDashboardViewModel.stateFlow` via
`collectAsStateWithLifecycle`, consumes `GroupDashboardEvent`s through `EventsEffect` (7 nav
branches + `ShowCorpusBlockedDialog` → local dialog state + `ShowSnackbar` → resolved snackbar),
delegates rendering to the stateless `GroupDashboardContent`. `groupId`/`viewerRole` are forwarded
to `koinViewModel(parameters = { parametersOf(groupId, viewerRole) })`.

**Content** — `GroupDashboardContent(state: GroupDashboardState, onAction: (GroupDashboardAction)
-> Unit, modifier, snackbarHostState)`. State-driven per `GroupDashboardState.screenState`
(Loading / Content / Error). Pull-to-refresh wired at the `KptScaffold` level, dispatching
`GroupDashboardAction.OnRefresh` (`isRefreshing` is a constant `false` — `GroupDashboardState`
carries no distinct refresh-in-progress flag; `OnRefresh` swaps the whole screen to the Loading
skeleton). Top-bar overflow icon dispatches `OnMoreOptions`, toggling `state.isMoreMenuExpanded`
(G13 — real ViewModel state); the `DropdownMenu` renders two declared items — Settings (→ settings)
and Sync Status (→ sync-status), each dispatching its typed action.

Sub-composables (all `internal`, previewed individually):
- `GroupDashboardLoadingSection` / `GroupDashboardSkeletonBlock` — Loading state, 4 shimmer blocks.
- `GroupDashboardContentSection` — header + pool-model-adaptive metric card + role-gated quick
  actions + savings summary + activity feed, inside one `LazyColumn`.
- `GroupDashboardErrorSection` — error state with conditional Retry CTA (`GroupDashboardError.retry`).

Reusable components (`components/` package): `GroupHeaderCard`, `CorpusMetricCard`
(ACCUMULATING), `RotationMetricCard` (ROTATING_PAYOUT), `QuickActionsSection` (role-gated
management/member grids), `GroupSavingsSummaryCard`, `ActivityFeedSection` + `ActivityRow`,
`CorpusBlockedDialog`.

**Resolved (was a flagged idea-layer gap)**:
1. `top_bar.actions[0]` (`OnMoreOptions`, overflow icon) is now a declared `GroupDashboardAction`
   member with a declared menu-item set (Settings, Sync Status) — `effect: transform_state` toggles
   `isMoreMenuExpanded` and the two items dispatch `OnGroupSettings` / `OnSyncStatus`.
2. `flow.yaml#navigates_to[]` now lists `savings-dashboard` (G9), `settings` + `sync-status` (G13);
   `OnViewSavings` self-scopes MEMBER "My Savings" to `savings-dashboard` (replacing the prior
   `member-savings-detail` mis-route that needed a `memberId` this dashboard does not carry).

## route

`GroupDashboardRoute` (`@Serializable data class(groupId: String, viewerRole: String)`) — path
`/groups/{groupId}`. `NavController.navigateToGroupDashboard(groupId, viewerRole, navOptions)`.
`NavGraphBuilder.groupDashboardScreen(onNavigateToMeetingCalendar, onNavigateToMemberList,
onNavigateToLoanList, onNavigateToShareOut, onNavigateToSavingsDashboard, onNavigateToSettings,
onNavigateToSyncStatus, onNavigateBack)` registers the composable destination via
`composableWithRootPushTransitions`.

Nav-arg contract — all 8 callbacks close a `GroupDashboardEvent` branch, none carries a `= {}`
default (DC3 count-assertion: 8 defaults / 8 overrides / 0 suppressed). Targets `meeting-calendar` /
`member-list` / `loan-list` / `share-out-preview` / `savings-dashboard` / `settings` / `sync-status`
are all wired at the NavHost seam.

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
