<!-- generated-by: kmp-viewmodel-gen -->
# feature/member-profile — API

## viewmodel

`kpt.feature.memberprofile.MemberProfileViewModel` — extends
`BaseViewModel<MemberProfileState, MemberProfileEvent, MemberProfileAction>`
(`kpt.core.base.ui.viewmodel.BaseViewModel`). Constructor deps: `MemberProfileRepository`
(`core/data`, composite fan-in of `get_client` + `get_client_accounts` + `get_member_role`, plus
the `update_member_role` write), `SessionManager` (`core-base/security`), `CrashReporter`
(`core-base/observability`), `KptAnalyticsTracker` (`core/analytics`), plus the `memberId` /
`groupId` nav-args (Koin `parametersOf`, NOT DI-graph types).

## state

`MemberProfileState` (`@Serializable @Immutable`):

| Field | Type | Default | Transient |
|---|---|---|---|
| `isLoading` | `Boolean` | `true` | no |
| `member` | `MemberProfile?` | `null` | yes |
| `accounts` | `MemberAccounts?` | `null` | yes |
| `role` | `MemberRole` | `MemberRole.MEMBER` | no (enum, native kotlinx.serialization support) |
| `isCurrentUserChairperson` | `Boolean` | `false` | no — always `false` in production, see `DEVELOPMENT.md#9-evolution` gap 1 |
| `isEditingRole` | `Boolean` | `false` | no |
| `selectedRole` | `MemberRole?` | `null` | no |
| `isUpdatingRole` | `Boolean` | `false` | no |
| `meetingsAttended` | `Int` | `0` | no — no wire source, see gap 2 |
| `totalMeetings` | `Int` | `0` | no — no wire source, see gap 2 |
| `attendanceRate` | `Double` | `0.0` | no — `computeAttendanceRate(meetingsAttended, totalMeetings)` |
| `error` | `MemberProfileError?` | `null` | yes |

`MemberProfileState.screenState` (derived extension) → `MemberProfileScreenState`
(`Loading` / `Content` / `Error` — no `Empty` variant; a single-member composite is never empty
once present). `error != null && error != MemberProfileError.RoleUpdateFailed` → `Error`;
`isLoading` → `Loading`; else → `Content`. `RoleUpdateFailed` is a WRITE-path-only, snackbar-only
error that never flips the screen away from the already-loaded profile.

`MemberProfileError`: `Network` (`error_network`, retry), `Server` (`error_server`, retry),
`NotFound` (`error_not_found`, non-retry — currently unreachable, same reachability-gap class as
`GroupDashboardError.NotFound`), `Auth` (`error_auth`, non-retry — read-path 401 triggers
`SessionManager.endSession()`; also settable on a write-path 401), `RoleUpdateFailed`
(`error_role_update`, retry — write-path only).

## actions

`MemberProfileAction` — 7 declared members (verbatim mirror of
`ui.yaml#state_model.actions.members`) + 2 `Internal` async-routing members.

| Variant | Payload | Effect |
|---|---|---|
| `OnEditRoleTap` | — | chairperson-gated (defensive-ignore if `!isCurrentUserChairperson`); sets `isEditingRole=true`, seeds `selectedRole=role` |
| `OnRoleSelected` | `role: MemberRole` | `selectedRole = role` |
| `OnConfirmRoleChange` | — | no-op if `selectedRole==null`; dismiss-only if `selectedRole==role` (unchanged); else calls `repository.updateMemberRole(memberId, request)` |
| `OnDismissRoleEdit` | — | `isEditingRole=false, selectedRole=null` |
| `OnViewSavings` | — | emits `NavigateToSavingsDetail(memberId, groupId)` |
| `Retry` | — | `profileStream.retry()`; sets `isLoading=true`, clears `error` |
| `OnBack` | — | emits `NavigateToMemberList(groupId)` |
| `Internal.StreamUpdated` | `screenState: ScreenState<MemberProfileDetail>` | maps the composite stream onto `MemberProfileState` |
| `Internal.RoleUpdateResult` | `requestedRole: MemberRole, result: NetworkResult<UpdateMemberRoleResult, NetworkError>` | success: updates `role` locally + closes sheet + `ShowSnackbar("role_updated")`; error: `ShowSnackbar` + (401 only) `error=Auth` |

## events

`MemberProfileEvent`:

| Variant | Payload | Trigger |
|---|---|---|
| `NavigateToMemberList` | `groupId: String` | `OnBack` |
| `NavigateToSavingsDetail` | `memberId: String, groupId: String` | `OnViewSavings` |
| `ShowSnackbar` | `message: String` (messageKey) | `Unauthenticated` (401 read), `Internal.RoleUpdateResult` (success + every error branch) |

## di

`kpt.feature.memberprofile.di.MemberProfileModule` — Koin module,
`viewModel { parameters -> MemberProfileViewModel(..., memberId = parameters.get(), groupId =
parameters.get()) }` (not `viewModelOf` — `memberId`/`groupId` are nav-args, not DI-graph types).
Included in `KoinModules.kt#featureModule`. `MemberProfileRepository` resolved from `DataModule`,
`SessionManager` from `SecurityModule`, `CrashReporter` from `observabilityModule`,
`KptAnalyticsTracker` from the single process-wide binding supplied by `LoginSignupModule`.
