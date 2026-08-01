<!-- generated-by: kmp-viewmodel-gen -->
# feature/join-with-code — API

<!-- kmp-viewmodel-gen:BEGIN -->
## viewmodel

`org.mifos.groupbanking.feature.joinwithcode.JoinWithCodeViewModel` — extends
`BaseViewModel<JoinWithCodeState, JoinWithCodeEvent, JoinWithCodeAction>`
(`kpt.core.base.ui.viewmodel.BaseViewModel`). Constructor: `invitationRepository:
InvitationRepository`, `authRepository: AuthRepository`, `analytics: KptAnalyticsTracker`,
`crashReporter: CrashReporter`, `inviteCode: String? = null` (deep-link nav-arg, resolved via
Koin `parametersOf(...)` — see `#di`).

## state

`JoinWithCodeState` — `@Serializable @Immutable data class`.

| Field | Type | Default |
|---|---|---|
| `inviteCode` | `String` | `""` |
| `inviteStatus` | `InviteStatus` | `InviteStatus.IDLE` |
| `groupPreview` | `GroupPreview?` | `null` |
| `isJoining` | `Boolean` | `false` |
| `error` (`@Transient`) | `JoinError?` | `null` |

`InviteStatus`: `IDLE` · `VALIDATING` · `VALID` · `INVALID`.

`JoinWithCodeScreenState` (derived — `JoinWithCodeState.deriveScreenState()`, not a stored
field): `Initial` · `Validating` · `Preview` · `Joining` · `Success` · `ErrorInvalidCode` ·
`ErrorExpired` · `ErrorAlreadyMember` · `ErrorNetwork`. `Success` is never returned by
`deriveScreenState()` — per `ui.yaml#states.success` it is transient, superseded in the same
frame by the `NavigateToPersonalDashboard` event; the member exists for Screen-layer `when`
exhaustiveness only.

`JoinError`: `InvalidCode` · `ExpiredCode` · `AlreadyMember` · `Network` · `Auth` — each carries
`retry: Boolean` + `messageKey: String` (verbatim mirror of `ui.yaml#state_model.errors.types`).

## actions

| Action | Payload | Emitted Event | `handleAction` effect |
|---|---|---|---|
| `OnCodeChange` | `value: String` | — (auto-validates at 6 chars) | uppercases/truncates `inviteCode`, clears `inviteStatus`/`error`/`groupPreview`; dispatches validate at 6 chars |
| `OnValidateCode` | — | `NavigateToLoginSignup` on 401 | `validateCode` → expiry/already-used check → `fetchGroupPreview` → `Preview` |
| `OnConfirmJoin` | — | `NavigateToLoginSignup` (unauth or non-numeric clientId) \| `NavigateToPersonalDashboard` (success) | resolves `clientId` from `AuthRepository.currentSession`, calls `joinGroup` |
| `OnBack` | — | `NavigateBack` | pure navigate |
| `OnRetry` | — | — | no-op unless `error.retry==true`; clears error, re-dispatches validate |

`Internal` (async-result routing, never UI-dispatched): `SessionChecked(session)`,
`ValidateFailed(error)`, `ValidateExpired`, `ValidateAlreadyMember`, `PreviewLoaded(preview)`,
`JoinSucceeded(result)`, `JoinFailed(error)`.

## events

`NavigateToPersonalDashboard` (F5 — no params, personal-dashboard nav_params:{}) ·
`NavigateToLoginSignup(pendingInviteCode: String)` · `NavigateBack` · `ShowSnackbar(message: String)`.

## di

`org.mifos.groupbanking.feature.joinwithcode.di.JoinWithCodeModule` — Koin module. Included via
`cmp-navigation/.../KoinModules.kt#featureModule.includes(...)`. Declares
`single { KptAnalyticsTracker(analyticsHelper = get()) }` (second occurrence of the follow-up
flagged in `LoginSignupModule.kt` — see its KDoc) plus `viewModel { parameters ->
JoinWithCodeViewModel(..., inviteCode = parameters.getOrNull()) }` (`org.koin.compose.viewmodel.dsl.viewModel`,
not `viewModelOf`, because `inviteCode` is a nav-arg supplied via `parametersOf(...)`, not a
DI-graph type). `InvitationRepository`/`AuthRepository` resolve from `DataModule`; `CrashReporter`
resolves from `core-base/observability`'s `observabilityModule` (already in `KoinModules.allModules`).
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## screen

`org.mifos.groupbanking.feature.joinwithcode.JoinWithCodeScreen` (Container, `internal`) —
collects `JoinWithCodeViewModel.stateFlow` via `collectAsStateWithLifecycle`, consumes
`JoinWithCodeEvent`s through `EventsEffect`, and delegates to the stateless
`JoinWithCodeContent`. Takes `inviteCode: String?` (deep-link nav-arg forwarded to the
ViewModel via Koin `parametersOf(inviteCode)`) plus 3 required nav callbacks
(`onNavigateToPersonalDashboard`, `onNavigateToLoginSignup`, `onNavigateBack` — no `= {}`
defaults).

`JoinWithCodeContent` (stateless, `internal`) — `state: JoinWithCodeState, onAction:
(JoinWithCodeAction) -> Unit` (single typed dispatcher, RULE-IMPL-DEAD-CLICKABLE-001 Rule 3).
Renders via `state.deriveScreenState()`:

| `JoinWithCodeScreenState` | Section composable | Components (`ui.yaml#states.*.components`) |
|---|---|---|
| `Initial` | `JoinCodeEntrySection(isValidating=false)` | hero icon, invite-code field, Validate button |
| `Validating` | `JoinCodeEntrySection(isValidating=true)` | hero icon, invite-code field, spinner + label |
| `Preview` | `GroupPreviewSection(isJoining=false)` | hero icon, invite-code field (disabled), preview card, Join Group button |
| `Joining` | `GroupPreviewSection(isJoining=true)` | same as `Preview`, Join Group button shows inline spinner |
| `Success` | `GroupPreviewSection(isJoining=true)` | transient/unreachable — see `JoinWithCodeScreenState.Success` KDoc; rendered as `Joining` to avoid a blank frame |
| `ErrorInvalidCode` / `ErrorExpired` / `ErrorAlreadyMember` / `ErrorNetwork` | `JoinCodeErrorSection` | hero icon, invite-code field, Validate button, error banner (+ Retry when `error.retry`) |

Reusable components (`feature/join-with-code/.../components/`): `JoinHeroIcon` (decorative
circular icon, `contentDescription` on the icon), `InviteCodeField` (centered/letter-spaced/
uppercase `OutlinedTextField`, sanitization happens in the ViewModel), `GroupPreviewCard`
(name/type/organiser/member-count/role-chip — `groupType`/`roleToAssign` render via raw enum
`.name`, same flagged idea-layer follow-up as `feature/group-list/.../GroupListCard.kt`),
`JoinCodeErrorBanner` (message + conditional expired-code hint + conditional Retry).

## route

`JoinWithCodeRoute(val inviteCode: String? = null)` — `@Serializable data class`, path
`/groups/join`. `NavController.navigateToJoinWithCode(inviteCode, navOptions)` +
`NavGraphBuilder.joinWithCodeScreen(onNavigateToPersonalDashboard, onNavigateToLoginSignup,
onNavigateBack)`. Pure pass-through (0 defaults / 0 overrides / 0 suppressed —
RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3 count-assertion), identical shape to
`LoginSignupRoute.kt` / `GroupTypePickerRoute.kt`. `flow.yaml#navigates_to` declares the
targets this Route closes: `personal-dashboard` (F5 post-join landing) + `login-signup`;
`onNavigateBack` is the `OnBack`/pop target. The `app://groups/join?code={inviteCode}` deep-link URI itself is not
yet registered as a platform `NavDeepLink`/intent-filter — flagged as an infra follow-up, the
same as `GroupTypePickerRoute.kt`'s `group-create` nav-arg gap.

## preview

`JoinWithCodeScreenPreview.kt` — 6 `@Preview` functions: 1 top-level
`PreviewParameterProvider<JoinWithCodeState>` (8 state variants: initial, validating, preview,
joining, error_invalid_code, error_expired, error_already_member, error_network) + 5
sub-composable previews (`JoinCodeEntrySection` × 2, `GroupPreviewSection`,
`JoinCodeErrorSection`). Data source: `demo-data.yaml#entries` (`InvitationRow` tokens
MWG7X2/EXP001/USED88 + `GroupPreview` dto for groupId 301) — `GroupTypeSlug` is resolved to
`VSLA` since the wire DTO's `groupType` field is a display string, not the domain enum.

## tags

`JoinWithCodeTestTags` (append-only, RULE-KMP-COMPOSE-UITEST-001 CU-5) — `SCREEN`,
`BACK_BUTTON` (tags the whole top app bar container — its only interactive element, since
`KptTopAppBar`'s internal `IconButton` has no per-button testTag slot and `core-base` is not
modifiable), `INVITE_CODE_FIELD`, `VALIDATE_BUTTON`, `VALIDATING_INDICATOR`, `PREVIEW_CARD`,
`CONFIRM_JOIN_BUTTON`, `ERROR_CARD`, `RETRY_BUTTON` (9 total).

## permissions

Not applicable — `ui.yaml` declares no `request-permission` `on_click.action` for this
feature.
<!-- kmp-screen-gen:END -->
