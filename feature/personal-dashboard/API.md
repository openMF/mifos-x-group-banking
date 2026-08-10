<!-- generated-by: kmp-viewmodel-gen -->
# feature/personal-dashboard — API

<!-- kmp-viewmodel-gen:BEGIN -->
## viewmodel

`kpt.feature.personaldashboard.PersonalDashboardViewModel` — extends
`BaseViewModel<PersonalDashboardState, PersonalDashboardEvent, PersonalDashboardAction>`
(`kpt.core.base.ui.viewmodel.BaseViewModel`). Constructor deps: `MemberDashboardRepository`
(`core/data`), `SessionManager` (`core-base/security`), `CrashReporter` (`core-base/observability`),
`KptAnalyticsTracker` (`core/analytics`). No `FieldEncryptor` — zero PII persisted by this screen.

## state

`PersonalDashboardState` (`@Serializable @Immutable`):

| Field | Type | Default | Transient |
|---|---|---|---|
| `memberName` | `String` | `""` | no |
| `myGroups` | `List<GroupSummary>` | `emptyList()` | yes |
| `selectedGroup` | `GroupSummary?` | `null` | yes |
| `poolModel` | `String` (`SavingsMechanism.name`) | `""` | no |
| `groupLinkedSavingsBalance` | `Double` | `0.0` | no |
| `individualSavingsBalance` | `Double` | `0.0` | no |
| `shareOutProjection` | `Double` | `0.0` | no |
| `rotationPosition` | `Int?` | `null` | no |
| `nextRecipientEta` | `String?` | `null` | no |
| `recentTransactions` | `List<SavingsTransaction>` | `emptyList()` | yes |
| `isRefreshing` | `Boolean` | `false` | no |
| `isLoading` | `Boolean` | `true` | no |
| `error` | `DashboardError?` | `null` | yes |

`PersonalDashboardState.screenState` (derived extension) → `PersonalDashboardScreenState`
(`Loading` / `Content` / `Error` / `Empty`): `error != null` → `Error`; `isLoading` → `Loading`;
`myGroups.isEmpty()` → `Empty`; else → `Content`.

`DashboardError`: `Network` (`error_network`, retry), `Server` (`error_server`, retry),
`Unauthorized` (`error_session_expired`, non-retry — also triggers `SessionManager.endSession()`).

## actions

| Action | Payload | Effect |
|---|---|---|
| `OnRefresh` | — | `dashboardStream.refreshFresh()` (bypass-and-refresh); sets `isRefreshing = true` |
| `OnRetry` | — | `dashboardStream.retry()` (SWR); sets `isLoading = true`, clears `error` |
| `OnSavingsCardClick` | — | emits `NavigateToSavings(groupId, poolModel)`; defensive no-op + warn log if `selectedGroup == null` |
| `OnSelectGroup` | `groupId: String` | re-requests `memberDashboardStream(groupId, ...)`, re-subscribes, sets `isLoading = true` |
| `Internal.StreamUpdated` | `screenState: ScreenState<MemberDashboard>` | routes to the stream→state mapper; not user-dispatched |

## events

| Event | Payload | Trigger |
|---|---|---|
| `NavigateToSavings` | `groupId: String, poolModel: String` | `OnSavingsCardClick` |
| `NavigateToGroupList` | — | declared per ui.yaml; **no wired trigger yet** — flagged idea-layer gap |

## di

`kpt.feature.personaldashboard.di.PersonalDashboardModule` — Koin module,
`viewModelOf(::PersonalDashboardViewModel)`. Included in `KoinModules.kt#featureModule`.
`MemberDashboardRepository` resolved from `DataModule`, `SessionManager` from `SecurityModule`,
`CrashReporter` from `observabilityModule`, `KptAnalyticsTracker` from the single process-wide
binding supplied by `LoginSignupModule` (not re-registered here).
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## screen

**Container** — `PersonalDashboardScreen(onNavigateToSavings, onNavigateToGroupList, modifier, viewModel)`.
Collects `PersonalDashboardViewModel.stateFlow` via `collectAsStateWithLifecycle`, consumes
`PersonalDashboardEvent`s through `EventsEffect`, delegates rendering to the stateless
`PersonalDashboardContent`.

**Content** — `PersonalDashboardContent(state: PersonalDashboardState, onAction: (PersonalDashboardAction) -> Unit, modifier)`.
State-driven per `PersonalDashboardState.screenState` (Loading / Content / Error / Empty). Pull-to-refresh
wired at the `KptScaffold` level across every screenState, dispatching `PersonalDashboardAction.OnRefresh`.

Sub-composables (all `internal`, previewed individually):
- `PersonalDashboardTopSection` — shared primary-colored greeting + group banner + optional
  group-selector chip row (`myGroups.size > 1`).
- `PersonalDashboardLoadingSection` / `PersonalDashboardSkeletonBlock` — Loading state, 4 shimmer blocks.
- `PersonalDashboardContentSection` — overlapping `SavingsSummaryCard` + `ShareoutProjectionCard`
  (pool-model-adaptive) + recent-activity rows.
- `PersonalDashboardEmptySection` — zero-group illustration state (no CTA — see gap note below).
- `PersonalDashboardErrorSection` — network/server error state with Retry CTA.

Reusable components (`components/` package): `GroupSelectorChipRow`, `SavingsSummaryCard`,
`ShareoutProjectionCard`, `RecentActivityRow`.

**Flagged idea-layer gap**: `preview/empty.html` shows "Browse Groups" / "Create a Group" CTA
buttons, but neither `PersonalDashboardAction` nor `ui.yaml#states.empty.components` declare a
matching action/component. Per RULE-IMPL-DEAD-CLICKABLE-001 Rule 1 no action was invented — the
Empty state renders illustration + title + body only. Needs an idea-layer update (new
`OnBrowseGroups`/`OnCreateGroup` action + event members) before the CTA can be wired.

## route

`PersonalDashboardRoute` (`@Serializable data object`) — path `/dashboard/member`.
`NavController.navigateToPersonalDashboard(navOptions)`.
`NavGraphBuilder.personalDashboardScreen(onNavigateToSavings, onNavigateToGroupList)` registers
the composable destination via `composableWithRootPushTransitions`.

Nav-arg contract:
- `onNavigateToSavings: (groupId: String, poolModel: String) -> Unit` — closes
  `PersonalDashboardEvent.NavigateToSavings`; target feature module (`personal-savings` /
  `savings-dashboard`) not yet generated — typed callback for the host to wire later.
- `onNavigateToGroupList: () -> Unit` — closes `PersonalDashboardEvent.NavigateToGroupList`;
  currently unreachable from any wired `on_click` (see screen gap note above).

## preview

`PersonalDashboardScreenPreview.kt` — 13 `@Preview` functions. Data source: `demo-data.yaml`
(`demo_data_resolved = true`) — Amina Wanjiru / ACCUMULATING (Mwangaza Women's Group) and Joseph
Kamau / ROTATING_PAYOUT (Tumaini ROSCA) seeded rows. Covers both pool-model variants plus
Loading/Content/Empty/Error top-level states and every sub-composable + component.

## tags

`PersonalDashboardTestTags` (append-only, RULE-KMP-COMPOSE-UITEST-001 CU-5): `SCREEN`,
`NOTIFICATION_ICON`, `GROUP_BANNER`, `GROUP_SELECTOR_ROW`, `SAVINGS_CARD`, `SHAREOUT_CARD`,
`RECENT_ACTIVITY_HEADER`, `RECENT_ACTIVITY_LIST`, `LOADING_SECTION`, `EMPTY_SECTION`,
`ERROR_SECTION`, `ERROR_RETRY_BUTTON`, plus `groupChipTag(groupId)` and
`transactionTag(transactionId)` resolver functions.

## permissions

Not applicable — this screen requires no runtime permission (`ui.yaml` declares no
`request-permission` `on_click`).
<!-- kmp-screen-gen:END -->
