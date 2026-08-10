<!-- generated-by: kmp-screen-gen -->
# feature/group-list — Development

## 1. Module Identity

`:feature:group-list` — the authenticated user's savings-group portfolio (`/groups`), the app's
post-login/root list screen. Namespace `kpt.feature.grouplist`. Source of
truth: `idea-layer/exports/group-management/{SPEC,MOCKUP}.md` (this project's idea-layer uses the
`exports/` + `mockups/` schema, not `screens/{f}/{ui,flow,data-flow,demo-data}.yaml` — no
`idea-layer/screens/group-list/` directory exists for this project).

## 2. Public API

See `GroupListViewModel.kt` class/field KDoc for the full ViewModel contract (`GroupListState`,
`GroupListEvent`, `GroupListAction`, `GroupListScreenState`, `GroupListError`) — not
re-documented here to avoid drift; this section is owned by `kmp-viewmodel-gen`.

## 3. Consumers

- `feature/group-list/src/commonTest/.../GroupListViewModelTest.kt` (pre-existing).
- `GroupListRoute.kt` (this pass) — not yet wired into the app-level nav graph (`cmp-navigation`);
  `group-dashboard` and `join-with-code` are not yet generated feature modules in this codebase,
  so `onNavigateToGroupDashboard` / `onNavigateToJoinGroup` are typed nav-arg contracts for the
  caller to wire once those routes exist. `onNavigateToCreateGroup` resolves to the
  already-generated `group-type-picker` route.

## 4. Boundaries

Reads exclusively via the injected `GroupRepository.groupsPagingStream(scope)` (`core/data`) — no
direct API/DB access from this module. `business_logic.kind: crud` (read-only, paginated) per
RULE-IMPLEMENT-STORE5-001 — the offline-first `PagingScreenStream<Group>` is consumed directly.

## 5. Data

See API.md#state / #actions / #events for the ViewModel field/payload schema and API.md#screen /
#route / #tags / #preview for the screen-layer contract.

## 6. Errors

`GroupListError` (3 variants): `Network`, `Server` (both retryable), `Auth` (non-retryable, no
declared `NavigateToLogin` event — surfaced as an ordinary error state + `ShowSnackbar`). The
Content layer maps each to `screens_group_list_error_{network,server,auth}_message`.

## 7. Testing

`feature/group-list/src/commonTest/kotlin/org/mifos/groupbanking/feature/grouplist/GroupListViewModelTest.kt`
(ViewModel layer, pre-existing). No `commonTest` UI-test suite yet for the screen layer — pending
`kmp-compose-uitest-gen` / `kmp-journey-uitest-gen`.

## 8. Observability

See `GroupListViewModel.kt` KDoc (Kermit `Logger.i/d` + `CrashReporter` breadcrumbs +
`KptAnalyticsTracker.trackGroupOperation(...)` on mount + every user-initiated group operation).

## 9. Evolution

<!-- kmp-screen-gen:BEGIN -->
## 2b. Screen / UI layer

- `GroupListScreen.kt` — Container (`GroupListScreen`) + stateless Content (`GroupListContent`),
  state-driven over all 4 `GroupListScreenState` members (Loading/Content/Empty/Error). Sub-sections
  `GroupListLoadingSection` / `GroupListContentSection` / `GroupListEmptySection` /
  `GroupListErrorSection` / `GroupListFabContent` (all `internal`, previewed individually).
- `GroupListRoute.kt` — `@Serializable data object GroupListRoute` (`/groups`) +
  `navigateToGroupList()` + `NavGraphBuilder.groupListScreen(onNavigateToGroupDashboard,
  onNavigateToCreateGroup, onNavigateToJoinGroup)` (3 required nav callbacks, no dead-clickable
  defaults — DC3 count-assertion N/A, 0 `= {}` defaults emitted).
- `components/` — `GroupListCard` (data-bound name/cycle/memberCount/lastMeetingDate/
  groupType/viewerRole + traffic-light health badge + accent-stripe), `GroupListCardSkeleton`
  (loading shimmer row), `GroupListSearchBar` (client-side name filter field),
  `HealthIndicatorVisuals.kt` (`badgeContainerColor` / `badgeContentColor` / `accentColor`
  per-`HealthIndicator` lookups).
- `GroupListTestTags.kt` — 12 append-only test tags (SCREEN, SEARCH_FIELD, SEARCH_CLEAR_ICON,
  SEARCH_EMPTY_MESSAGE, LOADING_INDICATOR, GROUP_LIST, FAB_CREATE, EMPTY_SECTION,
  EMPTY_CREATE_BUTTON, EMPTY_JOIN_BUTTON, ERROR_SECTION, ERROR_RETRY_BUTTON) plus the
  `cardTag(groupId)` resolver.
- `GroupListScreenPreview.kt` — 9 `@Preview` functions: 4 top-level `GroupListState` variants
  (Loading/Content/Empty/Error) plus one preview each for `GroupListLoadingSection`,
  `GroupListContentSection`, `GroupListEmptySection`, `GroupListErrorSection`,
  `GroupListFabContent`, plus 3 component-level previews (`GroupListCard`,
  `GroupListCardSkeleton`, `GroupListSearchBar`).
- `composeResources/values/strings.xml` — 19 `screens_group_list_*` keys (zero hardcoded UI
  literals — RULE-IMPL-NO-HARDCODED-STRING-001; `group.name`/`memberCount`/`cycleNumber`/
  `lastMeetingDate`/`groupType`/`viewerRole`/`healthIndicator` render data-bound, not as resource
  literals).

## 3b. Consumers (screen layer)

- `GroupListRoute.kt#groupListScreen(...)` — NOT YET wired into the app-level nav graph
  (`cmp-navigation`); mirrors `group-type-picker` and `login-signup`'s identical
  not-yet-wired-into-RootNavScreen state in this codebase. Wiring is out of scope for
  `kmp-screen-gen` (feature-scoped) — tracked as an infra-layer follow-up.
- `feature/group-list/src/commonMain/kotlin/.../GroupListScreenPreview.kt`.

## Evolution (screen layer)

- 2026-07-22 — `kmp-screen-gen`: `GroupListScreen.kt` (Container+Content, all 4 `screenState`
  branches, paginated `LazyColumn` with scroll-triggered `OnLoadMoreTap`), 4 reusable components,
  `GroupListRoute.kt`, `GroupListTestTags` (12 tags + 1 resolver fn), `GroupListScreenPreview.kt`
  (9 `@Preview`, 4 top-level state variants), and `strings.xml` (19 keys, zero hardcoded
  literals). Design ambiguities resolved (see also `API.md#screen`):
  1. `GroupListState` does not expose `hasMore`/`isLoadingMore`/`loadMoreError` paging-progress
     flags (the ViewModel's own KDoc on `handleLoadMoreTap` documents `OnLoadMoreTap` as sourced
     from `data-flow.yaml`, not a declared UI component), so the core-base `PagingScreenStream`
     is not directly exposed to this Container/Content pair (it is `private` on
     `GroupListViewModel`, by design — the ViewModel fully funnels pagination through
     `GroupListState`). The full `PagingScreenContent(pagingStream = ...)` overload could not be
     used; a `LazyColumn` + `rememberLoadMoreTrigger(hasMore = true, isLoadingMore = false)`
     scroll-trigger dispatching `OnLoadMoreTap` was used instead, relying on the underlying
     `PagingScreenStream` to no-op once genuinely exhausted.
  2. `SPEC.md`/`MOCKUP.md` (2026-05-24-era docs) predate the current `GroupListViewModel` —
     they declare no `OnJoinGroup` action, no `viewerRole`/`groupType`/`overdueRate` on `Group`,
     and no pagination. Per the dispatch brief's explicit "Empty (create + join-with-code CTAs)"
     instruction, `GroupListEmptySection` renders both a "Create Group" and a "Join with Code" CTA
     — the latter has no MOCKUP.md precedent and was placed in the Empty state only.
  3. `Group.groupType` / `Group.viewerRole` render via their raw enum `.name` (e.g. `VSLA`,
     `TREASURER`) rather than a humanized label — `GroupTypeConfig.displayName` (used by
     `group-type-picker`) is not attached to this list-row `Group` shape. Flagged as an
     idea-layer follow-up rather than invented here.
<!-- kmp-screen-gen:END -->
