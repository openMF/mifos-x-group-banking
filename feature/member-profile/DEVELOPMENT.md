<!-- generated-by: kmp-viewmodel-gen -->
# feature/member-profile — Development

## 1. Module Identity

`:feature:member-profile` — a single member's profile (`/groups/{groupId}/members/{memberId}`),
reached from `member-list`'s `member_row_tap`. Namespace
`kpt.feature.memberprofile`. Source of truth:
`idea-layer/screens/member-profile/{ui,api,docs,flow,data-flow}.yaml`.

## 2. Public API

See `MemberProfileViewModel.kt` class/field KDoc for the full ViewModel contract
(`MemberProfileState`, `MemberProfileEvent`, `MemberProfileAction`, `MemberProfileScreenState`,
`MemberProfileError`) — not re-documented here to avoid drift; this section is owned by
`kmp-viewmodel-gen`. Summary tables live in `API.md`.

## 3. Consumers

- `feature/member-profile/src/commonTest/.../MemberProfileViewModelTest.kt` (this pass).
- `MemberProfileModule` (Koin) — included in `cmp-navigation`'s `KoinModules.kt#featureModule`.
- No `MemberProfileRoute.kt` / `MemberProfileScreen.kt` yet — those are `kmp-screen-gen`'s scope.
  Once generated, the Route composable resolves `memberId`/`groupId` via
  `koinViewModel<MemberProfileViewModel> { parametersOf(memberId, groupId) }` per
  `MemberProfileModule`'s KDoc. `member-list`'s `MemberListEvent.NavigateToMemberProfile(memberId,
  groupId)` is the sole caller-side entry point (`MemberListRoute.kt`'s `onMemberClick` callback).

## 4. Boundaries

Reads exclusively via the injected `MemberProfileRepository.memberProfileStream(clientId, scope)`
(`core/data`) — a single composite fan-in of 3 companion reads (`get_client` +
`get_client_accounts` + `get_member_role`), NOT the 2 separately-named repositories
(`MemberRepository`/`GroupRepository`) `ui.yaml#state_model.di` documents — flagged
documentation-vs-implementation drift, not re-created here (mirrors
`GroupDashboardViewModel`'s / `MemberListViewModel`'s identical precedent).
`business_logic.kind: aggregator` per RULE-IMPLEMENT-STORE5-001. The single write —
`MemberProfileRepository.updateMemberRole(clientId, request)` — invalidates the store's cache on
success so the still-active read stream re-fetches (`data-flow.yaml#cache.strategy: invalidate`);
`SessionManager.endSession()` (`core-base/security`) is called for real on
`ScreenState.Unauthenticated`.

## 5. Data

See `API.md#state` / `#actions` / `#events` for the ViewModel field/payload schema.

## 6. Errors

`MemberProfileError` (5 variants): `Network`, `Server` (both retryable — composite READ path),
`NotFound` (non-retry, currently unreachable in production — same `MemberProfileFetchException`
message-shape reachability gap as `GroupDashboardError.NotFound`), `Auth` (non-retry, triggers
`sessionManager.endSession()` on the read path; also settable on a `401` role-update WRITE
failure), `RoleUpdateFailed` (retryable, WRITE-path only — deliberately excluded from
`MemberProfileState.screenState`'s derivation so a role-update failure never hides the
already-loaded profile content behind the full-screen `error_state`).

## 7. Testing

`feature/member-profile/src/commonTest/kotlin/org/mifos/groupbanking/feature/memberprofile/MemberProfileViewModelTest.kt`
— 20 tests: initial state, nav-arg stream request, `ScreenState` → `MemberProfileState` mapping
(Content role-resolution + no-match default / NoNetwork / Unauthenticated / generic Error), the
pure `computeAttendanceRate` / `resolveCurrentRole` formulas, chairperson-gated `OnEditRoleTap`
(defensive-ignore branch), the full role-edit flow (`OnRoleSelected` → `OnConfirmRoleChange`
no-selection guard / unchanged-role dismiss / success-invalidates-and-refetches /
`RoleUpdateFailed` snackbar / `401`-Auth snackbar), `OnDismissRoleEdit`, `OnViewSavings`, `OnBack`,
and `Retry`. No `commonTest` UI-test suite yet for the screen layer — pending
`kmp-compose-uitest-gen` / `kmp-journey-uitest-gen` once `kmp-screen-gen` runs.

## 8. Observability

See `MemberProfileViewModel.kt` KDoc (Kermit `Logger.i/w/e` + `CrashReporter` breadcrumbs +
`KptAnalyticsTracker.trackClientOperation(...)` on mount and every user-initiated action —
`MemberProfile` maps 1:1 onto a Fineract `client`, so the existing tracker surface is reused
rather than inventing a new method, mirrors `MemberListViewModel`'s identical precedent).

## 9. Evolution

- 2026-07-22 — `kmp-viewmodel-gen`: `MemberProfileViewModel` (TDD, 20 tests first),
  `MemberProfileState` (`@Serializable @Immutable`, `member`/`accounts`/`error` `@Transient`),
  `MemberProfileAction` (7 declared members verbatim from `ui.yaml#state_model.actions.members` +
  2 `Internal` async-routing members), `MemberProfileEvent` (3 declared members, no additions
  needed — `OnBack`'s target maps onto the already-declared `NavigateToMemberList`),
  `MemberProfileError` (5 variants), `MemberProfileModule` (Koin, nav-args `memberId` + `groupId`,
  declaration order). Feature module scaffolded (`build.gradle.kts`, `settings.gradle.kts`
  include, `cmp-navigation` dep + Koin wiring). Documented idea-layer gaps (not invented silently,
  reported to caller):
  1. `isCurrentUserChairperson` has no wire source anywhere in the consumable prior layers —
     `ui.yaml#state_model.di`'s `GroupRepository.isCurrentUserChairperson(groupId): Flow<Boolean>`
     never materialized on the shipped `GroupRepository` (only `groupsPagingStream` exists), and no
     nav-param/injectable carries the current viewer's identity/role (`ui.yaml#nav_params` is
     `memberId`+`groupId` only, unlike `group-dashboard`'s `groupId`+`viewerRole` pair). Defaults
     `false` always — same documented-gap class as `GroupDashboardState.isCorpusInsufficient` /
     `isCycleEnd`. `OnEditRoleTap` still ships a real defensive unauthorized-tap guard so the
     handler is correct the instant the gap is closed.
  2. `meetingsAttended`/`totalMeetings`/`attendanceRate` — `api.yaml#dtos.AttendanceSummary` is
     declared but no endpoint returns it, and `MemberProfileDetail`'s own KDoc confirms attendance
     is not part of the composite. `computeAttendanceRate` is a fully real, independently-tested
     pure formula; both counters default `0` (attendanceRate `0.0`) until a real endpoint lands.
  3. `401` on the `update_member_role` write has no declared `NavigateToLogin` event
     (`ui.yaml#events.members` is `NavigateToMemberList`/`NavigateToSavingsDetail`/`ShowSnackbar`
     only) despite `data-flow.yaml` declaring `behavior: navigate, nav_target: login` — folded onto
     `ShowSnackbar` + `state.error = Auth`, same documented gap class as
     `GroupDashboardViewModel`'s / `GroupCreateViewModel`'s identical Auth-redirect gaps.
  4. HTTP `403` (`update_member_role`'s "caller is not chairperson" case) has no dedicated
     `NetworkError` bucket in this codebase (`MemberProfileApiImpl`'s status-code mapper folds it
     into `NetworkError.UNKNOWN`) — correctly routed to the `RoleUpdateFailed` snackbar regardless,
     documented rather than silently assumed correct.
