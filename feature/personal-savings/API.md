<!-- generated-by: kmp-viewmodel-gen -->
# feature/personal-savings — API

<!-- kmp-viewmodel-gen:BEGIN -->
## viewmodel

`org.mifos.groupbanking.feature.personalsavings.PersonalSavingsViewModel` — extends
`BaseViewModel<PersonalSavingsState, PersonalSavingsEvent, PersonalSavingsAction>`
(`kpt.core.base.ui.viewmodel.BaseViewModel`). Constructor deps: `SavingsRepository` (`core/data`,
`loadMemberSavings(...)` + `getSavingsTransactions(...)` — both plain `suspend fun`s, no Store5
wrap), `SessionManager` (`core-base/security`), `CrashReporter` (`core-base/observability`),
`KptAnalyticsTracker` (`core/analytics`), plus the `clientId: Long` / `groupLinkedSavingsId: Long`
/ `individualSavingsId: Long?` nav-args (Koin `parametersOf`, NOT DI-graph types).

## state

`PersonalSavingsState` (`@Serializable @Immutable`):

| Field | Type | Default | Transient |
|---|---|---|---|
| `clientId` | `Long` | `0L` | no |
| `groupLinkedSavingsId` | `Long` | `0L` | no |
| `individualSavingsId` | `Long?` | `null` | no |
| `selectedTab` | `SavingsTab` | `GROUP_LINKED` | no |
| `groupLinkedBalance` | `Double` | `0.0` | no |
| `individualBalance` | `Double` | `0.0` | no |
| `groupLinkedTransactions` | `List<SavingsLedgerEntry>` | `emptyList()` | yes (type corrected from ui.yaml's stale `SavingsTransactionDto` — see `DEVELOPMENT.md#9` gap (2)) |
| `individualTransactions` | `List<SavingsLedgerEntry>` | `emptyList()` | yes (same correction) |
| `contributionTarget` | `Double` | `500.0` | no (no API source — see `DEVELOPMENT.md#9` gap (3)) |
| `meetingsAttended` | `Int` | `0` | no (no API source — see `DEVELOPMENT.md#9` gap (4)) |
| `totalMeetings` | `Int` | `0` | no (same gap) |
| `isLoading` | `Boolean` | `true` | no |
| `isRefreshing` | `Boolean` | `false` | no |
| `error` | `SavingsError?` | `null` | yes |

`PersonalSavingsState.deriveScreenState()` (extension) → `PersonalSavingsScreenState` (`Loading` /
`Content` / `Error` — no `Empty` member, per `ui.yaml#state_model.screen_state.members`).
`error != null` → `Error`; `isLoading` → `Loading`; else → `Content`.

`SavingsError`: `Network` (`error_network`, retry), `Server` (`error_server`, retry),
`Unauthorized` (`error_session_expired`, non-retry — calls `sessionManager.endSession()` for
real).

**Balance derivation:** `groupLinkedBalance`/`individualBalance` = the corresponding
transaction list's `maxByOrNull { it.date }?.runningBalance ?: 0.0` — `SavingsRepository` exposes
no dedicated account-summary endpoint (see `MemberSavingsBundle` KDoc, `core/model`). Sorted by
date rather than assuming transport order.

## actions

| Action | Payload | Effect |
|---|---|---|
| `OnTabSelected` | `tab: SavingsTab` | pure state transform — `selectedTab = tab`, no re-fetch (both accounts already eagerly loaded on mount — see `DEVELOPMENT.md#9` gap (1)) |
| `OnRefresh` | — | `isRefreshing = true`, bypass-refetches ONLY the active tab's account via `getSavingsTransactions` |
| `OnRetry` | — | clears `error`, `isLoading = true`, re-fetches the active tab's account |
| `Internal.SavingsLoaded` | `result: NetworkResult<MemberSavingsBundle, NetworkError>` | routes the on_mount composite fetch outcome; not user-dispatched |
| `Internal.RefreshResult` | `tab: SavingsTab, result: NetworkResult<List<SavingsLedgerEntry>, NetworkError>` | routes the `OnRefresh`/`OnRetry` single-account fetch outcome; not user-dispatched |

## events

| Event | Payload | Trigger |
|---|---|---|
| `NavigateBack` | — | declared (`ui.yaml#events`) but never emitted — see `DEVELOPMENT.md#9` gap (5) |

## di

`org.mifos.groupbanking.feature.personalsavings.di.PersonalSavingsModule` — Koin module,
`viewModel { parameters -> PersonalSavingsViewModel(..., clientId = parameters.get(),
groupLinkedSavingsId = parameters.get(), individualSavingsId = parameters.getOrNull()) }` (not
`viewModelOf` — the 3 nav-args are not DI-graph types). Included in
`KoinModules.kt#featureModule`. `SavingsRepository` resolved from `DataModule`, `SessionManager`
from `SecurityModule`, `CrashReporter` from `observabilityModule`, `KptAnalyticsTracker` from the
single process-wide binding supplied by `LoginSignupModule`.
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## screen

`org.mifos.groupbanking.feature.personalsavings.PersonalSavingsScreen` (Container, `internal`) —
collects `viewModel.stateFlow` via `collectAsStateWithLifecycle`, consumes `PersonalSavingsEvent`
via `EventsEffect`, delegates to `PersonalSavingsContent` (Content, `internal`, stateless —
hoisted `state: PersonalSavingsState` + `onAction: (PersonalSavingsAction) -> Unit` + plain
`onNavigateBack: () -> Unit`). `SavingsTabRow` renders OUTSIDE the state `when` block (every one
of the 3 declared `ui.yaml#states` includes `savings_tab_row`). State-driven per
`PersonalSavingsState.deriveScreenState()`: `Loading` -> `PersonalSavingsLoadingSection`,
`Content` -> `PersonalSavingsContentSection`, `Error` -> `PersonalSavingsErrorSection`.
Components: `components.SavingsTabRow`, `components.SavingsBalanceHeroCard`,
`components.SavingsContributionProgressCard`, `components.SavingsTransactionRow` (+ private
`isCreditTransaction()` heuristic — see its KDoc), `components.SavingsTransactionSkeletonRow`,
`components.SavingsEmptyIndividualState`.

## route

`org.mifos.groupbanking.feature.personalsavings.PersonalSavingsRoute` — `@Serializable data class
PersonalSavingsRoute(val clientId: Long, val groupLinkedSavingsId: Long, val individualSavingsId:
Long? = null)`. `NavController.navigateToPersonalSavings(clientId, groupLinkedSavingsId,
individualSavingsId, navOptions)`. `NavGraphBuilder.personalSavingsScreen(onNavigateBack: () ->
Unit)` — 1 callback, required (no `= {}` default); matches `flow.yaml#navigates_to`
(`personal-dashboard` only).

## preview

`PersonalSavingsScreenPreview.kt` — 15 `@Preview` functions,
`org.jetbrains.compose.ui.tooling.preview` import (KMP-safe). Data source: `demo-data.yaml`
(`entries[0].items[]` `PersonalSavingsState`, `entries[1]`/`entries[2]` group-linked/individual
`SavingsTransactionDto*` series for `clientId: 101`) mapped onto `SavingsLedgerEntry`. Top-level
`PersonalSavingsContent` driven by `PreviewParameterProvider<PersonalSavingsState>` (6 variants:
Loading, Content GROUP_LINKED, Content INDIVIDUAL-with-account, Content
INDIVIDUAL-no-account-empty-promo, Error-Network, Error-Unauthorized-non-retryable). Every
non-container composable covered (CP-4).

## tags

`org.mifos.groupbanking.feature.personalsavings.PersonalSavingsTestTags` — `SCREEN`, `TAB_ROW`,
`TAB_GROUP_LINKED`, `TAB_INDIVIDUAL`, `LOADING_INDICATOR`, `TRANSACTION_LIST`, `BALANCE_CARD`,
`CONTRIBUTION_PROGRESS_CARD`, `EMPTY_INDIVIDUAL_SECTION`, `ERROR_SECTION`, `ERROR_RETRY_BUTTON`,
plus `transactionRowTag(transactionId: Long)` stable-key function. Append-only
(RULE-KMP-COMPOSE-UITEST-001 CU-5).

## permissions

None declared — `ui.yaml` has no `on_click` with `action: request-permission` for this screen.
<!-- kmp-screen-gen:END -->
