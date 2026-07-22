<!-- generated-by: kmp-viewmodel-gen -->
# feature/member-list — Development

## 1. Module Identity

`:feature:member-list` — the paginated roster of a savings group's members
(`/groups/{groupId}/members`), reached from `group-dashboard`'s `view_members_button`. Namespace
`org.mifos.groupbanking.feature.memberlist`. Source of truth:
`idea-layer/screens/member-list/{ui,api,docs,flow,data-flow}.yaml`.

## 2. Public API

See `MemberListViewModel.kt` class/field KDoc for the full ViewModel contract (`MemberListState`,
`MemberListEvent`, `MemberListAction`, `MemberListScreenState`, `MemberListError`) — not
re-documented here to avoid drift; this section is owned by `kmp-viewmodel-gen`. Summary tables
live in `API.md`.

## 3. Consumers

- `feature/member-list/src/commonTest/.../MemberListViewModelTest.kt` (this pass).
- `MemberListModule` (Koin) — included in `cmp-navigation`'s `KoinModules.kt#featureModule`.
- No `MemberListRoute.kt` / `MemberListScreen.kt` yet — those are `kmp-screen-gen`'s scope. Once
  generated, the Route composable resolves `groupId` via `koinViewModel<MemberListViewModel> {
  parametersOf(groupId) }` per `MemberListModule`'s KDoc.

## 4. Boundaries

Reads exclusively via the injected `MemberRepository.membersPagingStream(groupId, scope)`
(`core/data`) — no direct API/DB access from this module. `business_logic.kind: crud` (read-only,
paginated) per RULE-IMPLEMENT-STORE5-001 — the offline-first `PagingScreenStream<Member>` is
consumed directly. A secondary, best-effort `GroupRepository.groupsPagingStream(scope,
fetchPolicy=CACHE_ONLY)` read resolves `groupName` for the top-bar subtitle (documented gap — see
`MemberListViewModel` class KDoc; `ui.yaml#state_model.di` declares `GroupRepository` for this
screen but the interface exposes no by-id lookup).

## 5. Data

See `API.md#state` / `#actions` / `#events` for the ViewModel field/payload schema.

## 6. Errors

`MemberListError` (3 variants): `Network`, `Server` (both retryable), `Auth` (non-retryable, no
declared `NavigateToLogin` event in `ui.yaml#state_model.events.members` despite
`data-flow.yaml#error_paths` declaring a `401 -> navigate login-signup` behavior — surfaced as an
ordinary error state + the closest declared event, `ShowSnackbar`). Flagged to the caller for an
idea-layer `ui.yaml#events` update.

## 7. Testing

`feature/member-list/src/commonTest/kotlin/org/mifos/groupbanking/feature/memberlist/MemberListViewModelTest.kt`
— 15 tests: initial state, `ScreenState` → `MemberListState` mapping (Content / Empty / Network /
Auth / Server), all 6 declared actions (`OnMemberClick`, `OnAddMember`, `OnLoadMore`, `OnRefresh`,
`Retry`, `OnBack`), `hasMorePages` exhaustion, and the `groupName` best-effort resolution
(match-found + no-match-fallback). No `commonTest` UI-test suite yet for the screen layer —
pending `kmp-compose-uitest-gen` / `kmp-journey-uitest-gen` once `kmp-screen-gen` runs.

## 8. Observability

See `MemberListViewModel.kt` KDoc (Kermit `Logger.i/d` + `CrashReporter` breadcrumbs +
`KptAnalyticsTracker.trackClientOperation(...)` on mount + every user-initiated roster operation —
`Member` maps 1:1 onto a Fineract `client`, so the existing tracker surface is reused rather than
inventing a new method).

## 9. Evolution

- 2026-07-22 — `kmp-viewmodel-gen`: `MemberListViewModel` (TDD, 15 tests first), `MemberListState`
  (`@Serializable @Immutable`, `members`/`error` `@Transient`), `MemberListAction` (6 declared +
  4 `Internal` async-routing members), `MemberListEvent` (3 declared + 1 flagged `NavigateBack`
  addition), `MemberListError` (3 variants), `MemberListModule` (Koin, nav-arg `groupId`). Feature
  module scaffolded (`build.gradle.kts`, `settings.gradle.kts` include, `cmp-navigation` dep +
  Koin wiring). Two documented idea-layer gaps (not invented silently, reported to caller):
  1. No `NavigateToLogin` event for the declared `401 -> navigate login-signup` Auth error path
     (mirrors `GroupListError.Auth`'s identical gap).
  2. No `NavigateBack` event for `OnBack` despite an unambiguous `effect: navigate` (plain nav-pop)
     — added as the one sanctioned addition-from-component, mirroring `GroupDashboardEvent
     .NavigateBack`'s identical precedent.
  3. `groupName` has no nav-param source and `GroupRepository` (the declared DI type) exposes no
     by-id lookup — resolved via a best-effort `CACHE_ONLY` scan of the cached group list instead
     of inventing a new repository method.
