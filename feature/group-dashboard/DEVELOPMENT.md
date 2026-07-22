<!-- generated-by: kmp-screen-gen -->
# feature/group-dashboard — Development

## 1. Module Identity

`:feature:group-dashboard` — the single-group dashboard (`/groups/{groupId}`), entered from
`group-list` / `group-create` with `groupId` + `viewerRole` nav-params. Namespace
`org.mifos.groupbanking.feature.groupdashboard`. Source of truth:
`idea-layer/screens/group-dashboard/{ui,flow,data-flow,docs}.yaml`.

<!-- kmp-viewmodel-gen:BEGIN -->
## 2. Public API

See `GroupDashboardViewModel.kt` class/field KDoc for the full ViewModel contract
(`GroupDashboardState`, `GroupDashboardEvent`, `GroupDashboardAction`,
`GroupDashboardScreenState`, `GroupDashboardError`) — not re-documented here to avoid drift; see
`API.md#viewmodel` / `#state` / `#actions` / `#events` / `#di`.

## 3. Consumers

- `feature/group-dashboard/src/commonTest/.../GroupDashboardViewModelTest.kt`.
- `cmp-navigation` — `GroupDashboardModule` included in `KoinModules.kt#featureModule`.
  `GroupDashboardScreen.kt` / `GroupDashboardRoute.kt` are `kmp-screen-gen` scope (see §3b).

## 4. Boundaries

Reads exclusively via the injected `GroupDashboardRepository.groupDashboardStream(groupId,
scope)` (`core/data`) — a single composite fan-in of 4 companion reads (`get_group` +
`get_viewer_role` + `get_group_corpus` + `get_group_accounts`), NOT the 3 separately-named
repositories (`GroupRepository`/`CorpusRepository`/`RoleRepository`) `ui.yaml#state_model.di`
documents — flagged documentation-vs-implementation drift, not re-created here.
`business_logic.kind: composite` per RULE-IMPLEMENT-STORE5-001. `SessionManager.endSession()`
(`core-base/security`) is called for real on `ScreenState.Unauthenticated`.

## 5. Data

See `API.md#state` / `#actions` / `#events` for the ViewModel field/payload schema.

## 6. Errors

`GroupDashboardError` (4 variants): `Network`, `Server` (both retryable), `NotFound` (non-retry —
currently unreachable in production, see class KDoc "reachability gap"), `Auth` (non-retry,
triggers `sessionManager.endSession()`).

## 7. Testing

`feature/group-dashboard/src/commonTest/kotlin/org/mifos/groupbanking/feature/groupdashboard/GroupDashboardViewModelTest.kt`.
No `commonTest` UI-test suite yet for the screen layer — pending `kmp-compose-uitest-gen` /
`kmp-journey-uitest-gen`.

## 8. Observability

See `GroupDashboardViewModel.kt` KDoc (Kermit `Logger.i/w` + `CrashReporter` breadcrumbs +
`KptAnalyticsTracker.trackGroupOperation(...)` on mount and every user-initiated action).

## 9. Evolution

- 2026-07-17 — `kmp-viewmodel-gen`: `GroupDashboardViewModel.kt` (state/action/event sealed types +
  pool-model-adaptive Content mapping + role-gated corpus-check on Start Meeting + real
  `SessionManager.endSession()` on 401), `GroupDashboardModule.kt` (Koin), feature module
  scaffold. Flagged idea-layer gaps (not invented): (1) `GroupDashboardEvent.NavigateBack` +
  `NavigateToMemberSavingsDetail` declared as additions beyond `ui.yaml#events.members`; (2)
  `GroupDashboardState.isCycleEnd` has no backing wire field (defaults `false`); (3)
  `GroupConfig.shareMin`/`shareMax`/`minimumDisbursementThreshold` have no wire source (always
  `null`), so `isCorpusInsufficient` is defensively `false` until the backend adds
  `minimumDisbursementThreshold`.
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## 2b. Public API — screen layer

`GroupDashboardScreen.kt` (Container `GroupDashboardScreen` + stateless `GroupDashboardContent` +
`internal` sub-composables), `GroupDashboardRoute.kt` (`GroupDashboardRoute` +
`navigateToGroupDashboard` + `groupDashboardScreen` NavGraphBuilder extension),
`GroupDashboardTestTags.kt`, `GroupDashboardScreenPreview.kt`, and
`components/{GroupHeaderCard,CorpusMetricCard,RotationMetricCard,QuickActionsSection,
GroupSavingsSummaryCard,ActivityFeedSection,CorpusBlockedDialog}.kt` now exist — see
`API.md#screen` / `#route` / `#preview` / `#tags`.

## 3b. Consumers — screen layer

`GroupDashboardRoute.kt`'s `groupDashboardScreen(...)` NavGraphBuilder extension is ready for the
`cmp-navigation` host to register — not yet wired into the app's root nav graph (out of this
pass's scope; mirrors the existing project state for other not-yet-wired screens in this
codebase, e.g. `group-list-screen`).

## 7b. Testing — screen layer

No `commonTest` Compose UI-test suite yet for `GroupDashboardScreen.kt` — pending
`kmp-compose-uitest-gen` / `kmp-journey-uitest-gen`.

## 9b. Evolution — screen layer

- 2026-07-22 — `kmp-screen-gen`: `GroupDashboardScreen.kt` (Container+Content, state-driven
  Loading/Content/Error rendering, pool-model-adaptive metric card, role-gated
  `QuickActionsSection`, `KptScaffold` pull-to-refresh + snackbar + corpus-blocked dialog),
  `GroupDashboardRoute.kt`, `GroupDashboardTestTags.kt` (27 tags + 1 resolver function),
  `GroupDashboardScreenPreview.kt` (16 `@Preview` functions, demo-data-sourced), 7 reusable
  `components/*.kt` files, 43 i18n string resources (`strings.xml`). Flagged idea-layer gaps (not
  invented): (1) `top_bar.actions[0]` (`OnMoreOptions`) has no matching `GroupDashboardAction`
  member or declared menu-item set — implemented as a real local Compose `DropdownMenu` toggle,
  no items invented; (2) `flow.yaml#navigates_to[]` is missing `member-savings-detail` even
  though `ui.yaml` + the ViewModel fully wire `NavigateToMemberSavingsDetail` — needs an
  idea-layer `flow.yaml` update (RULE-UI-SOURCE-001 US4 nav-check); (3)
  `GroupConfig.shareMin`/`shareMax` have no wire source, so `GroupSavingsSummaryCard`'s
  SHARE_BASED_VARIABLE line renders "Share-based: KES 200 / share" (no range) rather than
  inventing the mockup's static "1–5 shares / meeting" text.
<!-- kmp-screen-gen:END -->
