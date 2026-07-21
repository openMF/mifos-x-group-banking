<!-- generated-by: kmp-screen-gen -->
# feature/group-list — API

## viewmodel

`org.mifos.groupbanking.feature.grouplist.GroupListViewModel` — extends
`BaseViewModel<GroupListState, GroupListEvent, GroupListAction>`
(`kpt.core.base.ui.viewmodel.BaseViewModel`). Full field/action/event contract documented in the
ViewModel source KDoc — this section is owned by `kmp-viewmodel-gen`.

## di

`org.mifos.groupbanking.feature.grouplist.di.GroupListModule` — Koin module,
`viewModelOf(::GroupListViewModel)`. Already included in `KoinModules.kt#featureModule`.

<!-- kmp-screen-gen:BEGIN -->
## screen

- `GroupListScreen(onNavigateToGroupDashboard: (groupId: String, viewerRole: String) -> Unit, onNavigateToCreateGroup: () -> Unit, onNavigateToJoinGroup: () -> Unit, modifier: Modifier = Modifier, viewModel: GroupListViewModel = koinViewModel())`
  — Container. Collects `viewModel.stateFlow` via `collectAsStateWithLifecycle`, routes
  `GroupListEvent.NavigateToGroupDashboard(groupId, viewerRole)` → `onNavigateToGroupDashboard(...)`,
  `NavigateToCreateGroup` → `onNavigateToCreateGroup()`, `NavigateToJoinGroup` →
  `onNavigateToJoinGroup()`, and `ShowSnackbar(messageKey)` → resolves the key to a display string
  (`messageKeyToText`) and shows it, through `EventsEffect`. Delegates render to `GroupListContent`.
- `GroupListContent(state: GroupListState, onAction: (GroupListAction) -> Unit, modifier: Modifier = Modifier, snackbarHostState: SnackbarHostState = remember { SnackbarHostState() })`
  — stateless. `KptScaffold(showNavigationIcon = false, title = ..., floatingActionButtonContent = ..., pullToRefreshState = rememberKptPullToRefreshState(isEnabled = true, isRefreshing = state.isRefreshing, onRefresh = { onAction(OnRefresh) }))`
  + `when (state.screenState)`:
  - `Loading` → `GroupListLoadingSection(state.searchQuery, onAction)` (search bar + 5×
    `GroupListCardSkeleton` + `CircularProgressIndicator`)
  - `Content` → `GroupListContentSection(state, onAction)` (search bar + paginated `LazyColumn` of
    `GroupListCard`, or an inline "no results for query" message when `filteredGroups` is empty
    but `searchQuery` is non-blank)
  - `Empty` → `GroupListEmptySection(state.searchQuery, onAction)` (illustration + "Create Group" +
    "Join with Code" CTAs)
  - `Error` → `GroupListErrorSection(message = <resolved from state.error>, onRetry = { onAction(Retry) })`
- `GroupListFabContent(fabCd: String)` — sub-composable, FAB icon+label content slot.
- `GroupListLoadingSection(query: String, onAction: (GroupListAction) -> Unit, modifier: Modifier = Modifier)` — sub-composable, Loading state.
- `GroupListContentSection(state: GroupListState, onAction: (GroupListAction) -> Unit, modifier: Modifier = Modifier)`
  — sub-composable, Content state. Each row dispatches
  `onAction(GroupListAction.OnGroupClick(group.id, group.viewerRole.name))` on tap; scroll-to-end
  dispatches `onAction(GroupListAction.OnLoadMoreTap)` via `rememberLoadMoreTrigger` (core-base/ui).
- `GroupListEmptySection(query: String, onAction: (GroupListAction) -> Unit, modifier: Modifier = Modifier)` — sub-composable, Empty state.
- `GroupListErrorSection(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier)` — sub-composable, Error state.

### components

| Component | Signature | Notes |
|---|---|---|
| `GroupListCard` | `(group: Group, onClick: () -> Unit, modifier: Modifier = Modifier, testTag: String = "")` | `AppCard`-wrapped row with `accentColor = healthIndicator.accentColor()` left stripe; name, health badge, cycle/groupType/viewerRole chips, member count, last-met date; merged-semantics touch target ≥48dp. |
| `GroupListCardSkeleton` | `(modifier: Modifier = Modifier)` | Shimmering loading placeholder row. |
| `GroupListSearchBar` | `(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier)` | 48dp `OutlinedTextField`, 28dp corner radius, leading search icon, trailing clear icon (visible only when non-empty). |
| `HealthIndicator.badgeContainerColor` / `badgeContentColor` / `accentColor` | `@Composable fun HealthIndicator.___(): Color` | Traffic-light color tokens (`components/HealthIndicatorVisuals.kt`) — GREEN/AMBER hardcoded per `SPEC.md#design-tokens`, RED/UNKNOWN themed via `MaterialTheme.colorScheme`. |

## route

`GroupListRoute.kt`:

- `@Serializable data object GroupListRoute` — `/groups`.
- `NavController.navigateToGroupList(navOptions: NavOptions? = null)`.
- `NavGraphBuilder.groupListScreen(onNavigateToGroupDashboard: (groupId: String, viewerRole: String) -> Unit, onNavigateToCreateGroup: () -> Unit, onNavigateToJoinGroup: () -> Unit)`
  — registers `GroupListScreen` via `composableWithRootPushTransitions<GroupListRoute>`. DC3
  count-assertion: 0 `on{X}: () -> Unit = {}` defaults emitted in `GroupListScreen.kt` (all 3 nav
  callbacks are required, non-default params) — 0 overrides needed, N/A rather than a violation.

`SPEC.md#navigation`: `group-list card tap → group-dashboard` (via `onNavigateToGroupDashboard`),
`group-list FAB → group-create` (via `onNavigateToCreateGroup`, resolves to the generated
`group-type-picker` route), `group-list → join-with-code` (via `onNavigateToJoinGroup`, target not
yet generated).

## tags

`GroupListTestTags` (append-only, RULE-KMP-COMPOSE-UITEST-001 CU-5):

| Constant | Value |
|---|---|
| `SCREEN` | `group_list_screen` |
| `SEARCH_FIELD` | `group_list_search_field` |
| `SEARCH_CLEAR_ICON` | `group_list_search_clear_icon` |
| `SEARCH_EMPTY_MESSAGE` | `group_list_search_empty_message` |
| `LOADING_INDICATOR` | `group_list_loading_indicator` |
| `GROUP_LIST` | `group_list_lazy_column` |
| `FAB_CREATE` | `group_list_fab_create` |
| `EMPTY_SECTION` | `group_list_empty_section` |
| `EMPTY_CREATE_BUTTON` | `group_list_empty_create_button` |
| `EMPTY_JOIN_BUTTON` | `group_list_empty_join_button` |
| `ERROR_SECTION` | `group_list_error_section` |
| `ERROR_RETRY_BUTTON` | `group_list_error_retry_button` |

Plus `fun cardTag(groupId: String): String` — resolves the stable per-card tag
(`group_list_card_$groupId`; cards render data-driven, not as N separate composables).

## preview

`GroupListScreenPreview.kt` — 9 `@Preview` functions:

- `GroupListContentPreview` (top-level, `PreviewParameterProvider<GroupListState>`, 4 variants:
  Loading / Content (2 groups from `MOCKUP.md`'s worked example) / Empty / Error).
- `GroupListLoadingSectionPreview`, `GroupListContentSectionPreview`, `GroupListEmptySectionPreview`,
  `GroupListErrorSectionPreview`, `GroupListFabContentPreview`.
- `GroupListCardPreview`, `GroupListCardSkeletonPreview`, `GroupListSearchBarPreview`
  (component-level, extra coverage beyond the CP-4 floor).

Data source: no `demo-data.yaml` exists for this project's idea-layer schema (`exports/` +
`mockups/`, not `screens/{f}/`) — the 2 seeded `Group` rows (Mwangaza Women's Group / Tumaini
Savings Circle) are hand-authored verbatim from `MOCKUP.md`'s own worked example rather than
generic placeholder literals.

## i18n

`composeResources/values/strings.xml` — 19 keys under the `screens_group_list_*` namespace:
`title`, `loading_message`, `search_{hint,icon_cd,clear_cd,empty_message}`,
`cycle_label`, `member_count`, `last_met`, `card_cd`, `fab_cd`,
`action_{create,join,retry}`, `empty_{icon_cd,title,body}`,
`error_{title,icon_cd,network_message,server_message,auth_message}`. `Group.name` / `memberCount` /
`cycleNumber` / `lastMeetingDate` / `groupType` / `viewerRole` / `healthIndicator` are intentionally
NOT resource keys — they render data-bound from the live/cached `Group` domain model (dynamic
value, not a hardcoded literal).

## permissions

None — `group-list` requires no runtime permission (no `request-permission` `on_click` in this
screen's design surface).
<!-- kmp-screen-gen:END -->
