<!-- generated-by: kmp-viewmodel-gen -->
# feature/savings-dashboard — Development

## 1. Module Identity

`:feature:savings-dashboard` — the contribution-model-aware, group/individual tabbed savings
dashboard (`/groups/{groupId}/savings`), entered from `group-dashboard`'s `OnViewSavings` entry
point. Namespace `kpt.feature.savingsdashboard`. Source of truth:
`idea-layer/screens/savings-dashboard/{ui,data-flow}.yaml`.

<!-- kmp-viewmodel-gen:BEGIN -->
## 2. Public API

See `SavingsDashboardViewModel.kt` class/field KDoc for the full ViewModel contract
(`SavingsDashboardState`, `SavingsDashboardEvent`, `SavingsDashboardAction`,
`SavingsDashboardScreenState`) — not re-documented here to avoid drift; see `API.md#viewmodel` /
`#state` / `#actions` / `#events` / `#di`.

## 3. Consumers

- `feature/savings-dashboard/src/commonTest/.../SavingsDashboardViewModelTest.kt`.
- `cmp-navigation` — `SavingsDashboardModule` included in `KoinModules.kt#featureModule`;
  `implementation(projects.feature.savingsDashboard)` added to `cmp-navigation/build.gradle.kts`.
  `SavingsDashboardScreen.kt` / `SavingsDashboardRoute.kt` are `kmp-screen-gen` scope (not
  generated this pass).

## 4. Boundaries

Reads exclusively via the injected `SavingsRepository` (`core/data`) —
`loadSavingsDashboard(groupId)` on `LoadDashboard` (on_mount) AND `RefreshDashboard`
(pull-to-refresh / error-banner retry), a single composite call that fetches the group and
individual summaries concurrently — no direct Ktor/SQLDelight access from this module.
`business_logic.kind: composite` per ui.yaml — the legacy direct-`NetworkResult` template path
(the repository method is a plain `suspend fun`, no Store5 wrap; see `SavingsRepository` KDoc
"Store5 branch" note). `SessionManager` (`core-base/security`) is injected and used for its one
real capability — `endSession()` on a 401. `NetworkMonitor` (`cmp-network-monitor`) gates
`RefreshDashboard` before re-fetching, matching the error-banner's declared
`action_contract.library_refs`. Tab switching (`SelectTab`) is a pure in-memory
`SavingsDashboardState` transition with no network call — both tabs are already loaded together
by the composite fetch.

## 5. Data

See `API.md#state` / `#actions` / `#events` for the ViewModel field/payload schema.

## 6. Errors

Flat `error: String?` message-key field (`ui.yaml` declares no `errors.types` taxonomy for this
screen, unlike `MemberSavingsDetailError`'s sealed hierarchy) — mapped from `NetworkError` 1:1
against `data-flow.yaml#entries[on_mount].error_paths`: `NOT_FOUND -> error_group_not_found`,
`UNAUTHORIZED`/`TOO_MANY_REQUESTS -> error_auth` (calls `sessionManager.endSession()` for real),
`REQUEST_TIMEOUT -> error_offline`, everything else -> `error_server`. When cached summaries
already exist (a refresh failure, not the first load), the screen state stays `Content` with
`error` non-null — the Screen layer renders `error_banner` conditionally, matching
`ui.yaml#states.content_with_error`.

## 7. Testing

`feature/savings-dashboard/src/commonTest/kotlin/org/mifos/groupbanking/feature/savingsdashboard/SavingsDashboardViewModelTest.kt`
(12 tests — initial-state nav-arg seeding + contribution-model derivation before any fetch,
`LoadDashboard` success seeding both summaries/cycle-progress/weeklyTrend, FIXED contribution
model derivation, zero-member-rows -> `Empty`, `NOT_FOUND` -> `Error` screen state + `ShowError`,
`UNAUTHORIZED` -> real `sessionManager.endSession()` + `ShowError`, `SelectTab` pure transform
with zero re-fetch, `OpenMemberDetail` -> `NavigateToMemberDetail` with `memberId`/`groupId`/
`typeConfig`, `RefreshDashboard` offline -> `error_offline` + `ShowError` without calling the
repository, `RefreshDashboard` online -> re-fetch that clears a prior error, `RefreshDashboard`
success stamps `lastSyncAt`). No `commonTest` UI-test suite yet for the screen layer — pending
`kmp-compose-uitest-gen` / `kmp-journey-uitest-gen` (screen not generated this pass).

## 8. Observability

See `SavingsDashboardViewModel.kt` KDoc (Kermit `Logger.i/d/w` + `CrashReporter` breadcrumbs +
`KptAnalyticsTracker.trackSavingsOperation(...)` on view/refresh/member-row-tap).

## 9. Evolution

- 2026-07-22 — `kmp-viewmodel-gen`: `SavingsDashboardViewModel.kt` (state/action/event sealed
  types, composite `LoadDashboard`/`RefreshDashboard` fetch via `loadSavingsDashboard`, pure
  client-side `SelectTab`, `OpenMemberDetail` -> `NavigateToMemberDetail`, connectivity-gated
  retry, 401 -> real `sessionManager.endSession()`, client-observed `lastSyncAt` stamp via
  `kotlin.time.Clock`), `SavingsDashboardModule.kt` (Koin, `groupId`/`typeConfig` nav-args via
  `parametersOf`), feature module scaffold (`build.gradle.kts`, `settings.gradle.kts` include,
  `cmp-navigation` dependency + `KoinModules.kt` registration). Flagged idea-layer gaps (not
  invented): (1) `ui.yaml#state.fields.selectedTab` declares a bare `SavingsTab` type, but that
  name is TAKEN by personal-savings' own GROUP_LINKED/INDIVIDUAL enum in `core/model/Savings.kt`
  — this screen's own GROUP/INDIVIDUAL value-set is exposed under the already-generated
  `SavingsDashboardTab` instead (`api.yaml#dtos.SavingsTab`) — flagged for
  RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1 (`ui.yaml` should reference `SavingsDashboardTab`
  directly); (2) `contributionModel` (`ui.yaml` comment: `SHARE_BASED_VARIABLE | FIXED_AMOUNT |
  FIXED_NEGOTIATED`) is derived from the REAL `ContributionMode` registry (`SHARE_BASED_VARIABLE |
  FIXED | MINIMAL | UNKNOWN`) via `typeConfig.contributionMode.name` — same documented correction
  `MemberSavingsDetailState.contributionModel` makes — flagged for CFF1; (3) `lastSyncAt` has no
  server-provided source on `GroupSavingsSummary`/`IndividualSavingsSummary` — stamped as an
  honest client-observed "last successful sync" wall-clock time (`kotlin.time.Clock`) on every
  `LoadDashboard`/`RefreshDashboard` success, never a fabricated server value — flagged for CFF1;
  (4) `ui.yaml#state_model.di` declares `[SavingsRepository, NavigationManager,
  ConnectivityObserver, LocalSavingsDao]` — none except `SavingsRepository` are real symbols;
  navigation flows through `sendEvent` (no `NavigationManager`), `ConnectivityObserver` is the
  real `NetworkMonitor` (`cmp-network-monitor`), `LocalSavingsDao` is not directly injected (no
  Store5/offline-cache wiring yet per `SavingsRepository`'s own KDoc) — `SessionManager` is ALSO
  injected (undeclared in `ui.yaml#state_model.di`) purely for its real `endSession()` 401 path —
  flagged for CFF1; (5) `individual_member_row.on_click.params` additionally passes `savings_type:
  INDIVIDUAL`, but neither `state_model.actions.members[OpenMemberDetail].params` nor
  `state_model.events.members[NavigateToMemberDetail].params` declare a `savingsType` field —
  mirrored verbatim (memberId only) per RULE-IMPL-DEAD-CLICKABLE-001 Rule 1 — flagged for
  RULE-IDEA-ACTION-CONTRACT-001 if `member-savings-detail` later needs the tab-of-origin signal.
<!-- kmp-viewmodel-gen:END -->
