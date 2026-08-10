<!-- generated-by: kmp-viewmodel-gen -->
# feature/member-list — API

## viewmodel

`kpt.feature.memberlist.MemberListViewModel` — extends
`BaseViewModel<MemberListState, MemberListEvent, MemberListAction>`
(`kpt.core.base.ui.viewmodel.BaseViewModel`). Constructor deps: `MemberRepository`,
`GroupRepository`, `CrashReporter`, `KptAnalyticsTracker`, `groupId: String` (nav-arg, resolved
via Koin `parametersOf`).

## state

`MemberListState` (`@Serializable @Immutable`):

| Field | Type | Default | Notes |
|---|---|---|---|
| `isLoading` | `Boolean` | `true` | |
| `members` | `List<Member>` | `emptyList()` | `@Transient` — `Member` is not `@Serializable`; re-derived from the paged stream. |
| `groupId` | `String` | `""` | Seeded from the nav-arg constructor param. |
| `groupName` | `String` | `""` | Best-effort `GroupRepository` cache lookup — see `DEVELOPMENT.md#4-boundaries`. |
| `isLoadingMore` | `Boolean` | `false` | Mirrors `PagingScreenStream.isLoadingMore`. |
| `hasMorePages` | `Boolean` | `true` | Mirrors `PagingScreenStream.hasMore`. |
| `currentOffset` | `Int` | `0` | Derived from `members.size` on every stream update. |
| `error` | `MemberListError?` | `null` | `@Transient`. `Network` / `Server` / `Auth`. |
| `isRefreshing` | `Boolean` | `false` | |

Derived `MemberListState.screenState: MemberListScreenState` — `Loading` / `Content` / `Error` /
`Empty`, single source of truth (error > loading > empty > content precedence).

## actions

`MemberListAction` — 6 declared members (verbatim mirror of
`ui.yaml#state_model.actions.members`) + 4 `Internal` async-routing members.

| Variant | Payload | Emitted Event / Effect |
|---|---|---|
| `OnMemberClick` | `memberId: String` | `NavigateToMemberProfile(memberId, groupId)` |
| `OnAddMember` | — | `NavigateToAddMember(groupId)` |
| `OnLoadMore` | — | `pagingStream.loadNextPage()` |
| `OnRefresh` | — | `isRefreshing=true` + `pagingStream.refresh()` |
| `Retry` | — | `error=null, isLoading=true` + `pagingStream.retry()` |
| `OnBack` | — | `NavigateBack` (flagged addition — see `DEVELOPMENT.md#9-evolution`) |
| `Internal.StreamUpdated` | `ScreenState<List<Member>>` | Maps the paged stream onto `MemberListState`. |
| `Internal.HasMoreUpdated` | `Boolean` | `hasMorePages = ...` |
| `Internal.LoadingMoreUpdated` | `Boolean` | `isLoadingMore = ...` |
| `Internal.GroupNameResolved` | `String` | `groupName = ...` |

## events

`MemberListEvent`:

| Variant | Payload |
|---|---|
| `NavigateToMemberProfile` | `memberId: String, groupId: String` |
| `NavigateToAddMember` | `groupId: String` |
| `ShowSnackbar` | `message: String` |
| `NavigateBack` | — (flagged addition, not in `ui.yaml#state_model.events.members`) |

## di

`kpt.feature.memberlist.di.MemberListModule` — Koin module,
`viewModel { parameters -> MemberListViewModel(..., groupId = parameters.get<String>()) }`
(nav-arg qualifier position 1). Included in `cmp-navigation`'s `KoinModules.kt#featureModule`.
