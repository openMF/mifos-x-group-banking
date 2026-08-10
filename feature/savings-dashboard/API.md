<!-- generated-by: kmp-viewmodel-gen -->
# feature/savings-dashboard — API

<!-- kmp-viewmodel-gen:BEGIN -->
## viewmodel

`kpt.feature.savingsdashboard.SavingsDashboardViewModel` — extends
`BaseViewModel<SavingsDashboardState, SavingsDashboardEvent, SavingsDashboardAction>`
(`kpt.core.base.ui.viewmodel.BaseViewModel`). Constructor deps: `SavingsRepository` (`core/data`,
`loadSavingsDashboard(groupId)` — a single composite `suspend fun`, no Store5 wrap), `NetworkMonitor`
(`cmp-network-monitor`), `SessionManager` (`core-base/security`), `CrashReporter`
(`core-base/observability`), `KptAnalyticsTracker` (`core/analytics`), plus the `groupId: String` /
`typeConfig: GroupTypeConfig` nav-args (Koin `parametersOf`, NOT DI-graph types).

## state

`SavingsDashboardState` (`@Serializable @Immutable`):

| Field | Type | Default | Transient |
|---|---|---|---|
| `selectedTab` | `SavingsDashboardTab` | `GROUP` | no |
| `groupId` | `String` | `""` | no |
| `typeConfig` | `GroupTypeConfig?` | `null` | yes |
| `contributionModel` | `String` | `""` | no (derived from `typeConfig.contributionMode.name` — see `DEVELOPMENT.md#9` gap (2)) |
| `groupSavingsSummary` | `GroupSavingsSummary?` | `null` | yes |
| `individualSavingsSummary` | `IndividualSavingsSummary?` | `null` | yes |
| `weeklyTrend` | `List<WeeklyContributionPoint>` | `emptyList()` | yes (from `group.weeklyTrend`, falls back to `individual.weeklyTrend`) |
| `isLoading` | `Boolean` | `true` | no |
| `isRefreshing` | `Boolean` | `false` | no |
| `error` | `String?` | `null` | yes (message key, see `DEVELOPMENT.md#6`) |
| `lastSyncAt` | `String?` | `null` | no (client-observed — see `DEVELOPMENT.md#9` gap (3)) |
| `cycleTarget` | `Long` | `0L` | no |
| `cycleCollected` | `Long` | `0L` | no |

`SavingsDashboardState.deriveScreenState()` (extension) -> `SavingsDashboardScreenState`
(`Loading` / `Content` / `Empty` / `Error`). `isLoading` -> `Loading`; `error != null` with both
summaries `null` -> `Error`; both tabs' `memberRows` empty -> `Empty`; else -> `Content` (with
`error_banner` rendered conditionally when `error != null` — no dedicated `ContentWithError`
member).

## actions

| Action | Payload | Effect |
|---|---|---|
| `LoadDashboard` | — | on_mount — dispatched by the Screen's `LaunchedEffect(Unit)`; parallel group+individual composite fetch via `loadSavingsDashboard` |
| `RefreshDashboard` | — | pull-to-refresh AND the error-banner retry button (same action, dual trigger); gates on `NetworkMonitor.isOnline` before re-fetching |
| `SelectTab` | `tab: SavingsDashboardTab` | pure state transform — `selectedTab = tab`, no re-fetch (both tabs already loaded together) |
| `OpenMemberDetail` | `memberId: String` | emits `NavigateToMemberDetail(memberId, groupId, typeConfig)` |
| `Internal.DashboardLoaded` | `result: NetworkResult<SavingsDashboardSummary, NetworkError>` | routes the `LoadDashboard` fetch outcome; not user-dispatched |
| `Internal.RefreshResult` | `result: NetworkResult<SavingsDashboardSummary, NetworkError>` | routes the `RefreshDashboard` fetch outcome; not user-dispatched |

## events

| Event | Payload | Trigger |
|---|---|---|
| `NavigateToMemberDetail` | `memberId: String, groupId: String, typeConfig: GroupTypeConfig` | `OpenMemberDetail` |
| `ShowError` | `message: String` | any `LoadDashboard`/`RefreshDashboard` failure (offline, 401, 404, 500) |

## di

`kpt.feature.savingsdashboard.di.SavingsDashboardModule` — Koin module,
`viewModel { parameters -> SavingsDashboardViewModel(..., groupId = parameters.get(), typeConfig =
parameters.get()) }` (not `viewModelOf` — the 2 nav-args are not DI-graph types). Included in
`KoinModules.kt#featureModule`. `SavingsRepository` resolved from `DataModule`, `NetworkMonitor`
from `DataModule`'s `NetworkMonitorProvider.install()` single, `SessionManager` from
`SecurityModule`, `CrashReporter` from `observabilityModule`, `KptAnalyticsTracker` from the single
process-wide binding supplied by `LoginSignupModule`.
<!-- kmp-viewmodel-gen:END -->
