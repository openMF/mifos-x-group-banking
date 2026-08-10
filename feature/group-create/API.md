<!-- generated-by: kmp-viewmodel-gen -->
<!-- co-authored-by: kmp-screen-gen -->
# feature/group-create — API

<!-- kmp-viewmodel-gen:BEGIN -->
## viewmodel

`kpt.feature.groupcreate.GroupCreateViewModel` — extends
`BaseViewModel<GroupCreateState, GroupCreateEvent, GroupCreateAction>`
(`kpt.core.base.ui.viewmodel.BaseViewModel`).

## state

`GroupCreateState` — `@Serializable @Immutable data class`.

| Field | Type | Default |
|---|---|---|
| `currentStep` | `Int` | `1` |
| `totalSteps` | `Int` | `4` |
| `typeConfig` (`@Transient`) | `GroupTypeConfig?` | `null` |
| `groupTypeName` | `String` | `""` |
| `groupName` | `String` | `""` |
| `officeId` | `Long?` | `null` |
| `officeName` | `String` | `""` |
| `currency` | `String` | `"KES"` |
| `meetingDay` | `String` | `""` |
| `meetingTime` | `String` | `""` |
| `shareValue` | `String` | `""` |
| `shareMin` | `String` | `"1"` |
| `shareMax` | `String` | `""` |
| `contributionAmount` | `String` | `""` |
| `payoutOrderMethod` | `String` | `"FIXED_ORDER"` |
| `loanMultiplier` | `String` | `"3"` |
| `interestRate` | `String` | `"10"` |
| `cycleLengthMonths` | `String` | `"12"` |
| `fineAmount` | `String` | `""` |
| `socialFundEnabled` | `Boolean` | `false` |
| `socialFundPercent` | `String` | `"5"` |
| `maxMembers` | `String` | `"30"` |
| `inviteCode` | `String?` | `null` |
| `isSubmitting` | `Boolean` | `false` |
| `isSubmitSuccess` | `Boolean` | `false` |
| `officeList` (`@Transient`) | `List<Office>` | `emptyList()` |
| `validationErrors` | `Map<String, String>` | `emptyMap()` |
| `error` (`@Transient`) | `GroupCreateError?` | `null` |
| `isOffline` | `Boolean` | `false` |

On construction, `initialTypeConfig` seeds `typeConfig`, `groupTypeName`, `loanMultiplier`,
`interestRate`, `cycleLengthMonths`, `maxMembers`, `socialFundEnabled` from the nav-arg's
per-type defaults. `GroupCreateScreenState` (`Content`/`Submitting`/`Success`/`Error`) is
**derived** via `GroupCreateState.deriveScreenState()`, not stored.
`GroupCreateError`: `Validation` · `Network` · `Server` · `Auth` — each carries `retry: Boolean`
+ `messageKey: String`.

## actions

| Action | Payload | Emitted Event | `handleAction` effect |
|---|---|---|---|
| `OnNameChange` | `value: String` | — | `groupName` transform + clears its validation error |
| `OnOfficeSelect` | `officeId: Long, officeName: String` | — | `officeId`/`officeName` transform |
| `OnCurrencyChange` | `value: String` | — | `currency` transform |
| `OnMeetingDaySelect` | `day: String` | — | `meetingDay` transform |
| `OnMeetingTimeSelect` | `time: String` | — | `meetingTime` transform |
| `OnShareValueChange`/`OnShareMinChange`/`OnShareMaxChange` | `value: String` | — | Step 2 share-based field transform |
| `OnContributionAmountChange` | `value: String` | — | Step 2 fixed-contribution field transform |
| `OnPayoutOrderChange` | `method: String` | — | Step 2 ROSCA payout-order transform |
| `OnLoanMultiplierChange`/`OnInterestRateChange`/`OnCycleLengthChange`/`OnFineAmountChange` | `value: String` | — | Step 2 shared-rule field transform |
| `OnSocialFundToggle` | `enabled: Boolean` | — | toggles `socialFundEnabled` |
| `OnSocialFundPercentChange` | `value: String` | — | `socialFundPercent` transform |
| `OnMaxMembersChange` | `value: String` | — | Step 3 `maxMembers` transform |
| `OnNextStep` | — | — | validates current step; advances `currentStep` on pass, else sets `validationErrors` |
| `OnPreviousStep` | — | — | decrements `currentStep` (no validation, no field clearing) |
| `OnSubmit` | — | `NavigateToGroupDashboard` \| `ShowOfflineSyncDialog` \| `ShowSnackbar` | validates, resolves `userId`, offline-checks, calls `groupCreateRepository.createGroup` |
| `OnBack` | — | `NavigateBack` | pure navigate, always fires regardless of step |

`Internal` (async-result routing, never UI-dispatched): `SessionChecked(session)`,
`ConnectivityChanged(online)`, `OfficesLoaded(result)`, `SubmitResult(result)`.

## events

`NavigateToGroupDashboard(groupId: String)` · `NavigateBack` · `ShowOfflineSyncDialog` ·
`ShowSnackbar(message: String)`.

## di

`kpt.feature.groupcreate.di.GroupCreateModule` — Koin module. Included via
`cmp-navigation/.../KoinModules.kt#featureModule.includes(...)`. Registers
`GroupCreateViewModel` with the `viewModel { parameters -> ... }` builder (`initialTypeConfig`
resolved via `parameters.get()` — a nav-arg, not a DI-graph type) plus
`single { KptAnalyticsTracker(analyticsHelper = get()) }` (no shared registration exists yet,
same flagged follow-up as `LoginSignupModule`/`JoinWithCodeModule`). `GroupCreateRepository` /
`AuthRepository` / `NetworkMonitor` resolve from `DataModule`; `CrashReporter` resolves from
`core-base/observability`'s `observabilityModule`.
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## screen

`kpt.feature.groupcreate.GroupCreateScreen` (Container, `internal`) —
collects `GroupCreateViewModel.stateFlow` via `collectAsStateWithLifecycle`, forwards the
required `typeConfig: GroupTypeConfig` nav-arg to Koin via `parametersOf(typeConfig)`, consumes
`GroupCreateEvent`s through `EventsEffect` (`NavigateToGroupDashboard` / `NavigateBack` /
`ShowOfflineSyncDialog` → local `showOfflineDialog` state → `OfflineSyncDialog` / `ShowSnackbar`
→ message-key-matched localized string), and delegates to the stateless `GroupCreateContent`.
2 required nav callbacks (`onNavigateToGroupDashboard`, `onNavigateBack` — no `= {}` defaults).

`GroupCreateContent` (stateless, `internal`) — `state: GroupCreateState, onAction:
(GroupCreateAction) -> Unit` (single typed dispatcher, RULE-IMPL-DEAD-CLICKABLE-001 Rule 3).
Top app bar close icon hides on `Submitting`/`Success` (mirrors `submitting.html`'s
`visibility:hidden`). Renders via `state.deriveScreenState()`:

| `GroupCreateScreenState` | Section composable | Notes |
|---|---|---|
| `Content` | `GroupCreateWizardSection` | banner + step indicator + step-router (1→`GroupIdentityStepSection`, 2→`GroupRulesStepSection`, 3→`GroupMembersStepSection`, 4→`GroupReviewStepSection(isSubmitting=false)`) |
| `Submitting` | `GroupCreateSubmittingSection` | banner + step indicator (frozen step 4) + `GroupReviewStepSection(isSubmitting=true)` — spinner replaces "Create Group" label, both buttons disabled |
| `Error` | `GroupCreateErrorSection` | banner + step indicator + inline error banner + conditional offline banner + review card + "Retry" (re-dispatches `OnSubmit`) + Back |
| `Success` | `GroupCreateSuccessSection` | transient — no interactive CTA (`NavigateToGroupDashboard` fires the same frame); checkmark + invite code + "Opening group dashboard…" spinner row |

Type-adaptive Step 2 (`GroupRulesStepSection`): `isShareBasedContribution` gates
`share_value`/`share_min`/`share_max` vs. `contribution_amount`; `isRotatingPayout` additionally
shows `payout_order_dropdown`. Social-fund toggle always renders; `social_fund_percent_field`
gates on `socialFundEnabled`.

Reusable components (`feature/group-create/.../components/`): `GroupTypeBanner` (chip badge),
`WizardStepIndicator` (dot+label+connector stepper, `labels` param backs
`ui.yaml#components.step_indicator.labels`), `GroupCreateTextField` (label/placeholder/helper/
error `OutlinedTextField` wrapper), `GroupCreateDropdownField` (`ExposedDropdownMenuBox` wrapper
— office/currency/meeting-day/payout-order selects), `ReviewSection` (labeled key-value block),
`OfflineNoticeBanner`, `GroupCreateErrorBanner`.

**Resolved idea-layer template gap:** `ui.yaml#components.review_card.content
.review_rules_section.rows` hardcodes `"KES {{shareValue}}"` / `"KES {{contributionAmount}}"` /
`"KES {{fineAmount}}"` literals. Since Step 1's `currency_dropdown` lets the user pick
USD/UGX/TZS, the Screen resolves these rows from the live `GroupCreateState.currency` field
instead (`rulesReviewRows()` in `GroupCreateScreen.kt`) — a hardcoded "KES" would misreport a
non-KES group. Flagged in the generation report for idea-layer template correction.

## route

`GroupCreateRoute` — `@Serializable data class`, path `/groups/create`. `GroupTypeConfig`
(`core/model`) is a plain, non-`@Serializable` domain model (per its own KDoc), so
`GroupCreateRoute` flattens every `GroupTypeConfig` field into primitive route args;
`GroupTypeConfig.toGroupCreateRoute()` / `GroupCreateRoute.toGroupTypeConfig()` round-trip
losslessly (enum fields resolve via `valueOf(...)`, falling back to each enum's `UNKNOWN` member
on an unrecognized string). `NavController.navigateToGroupCreate(typeConfig, navOptions)` +
`NavGraphBuilder.groupCreateScreen(onNavigateToGroupDashboard, onNavigateBack)`. Pure
pass-through (0 defaults / 0 overrides / 0 suppressed — RULE-PROTO-COMPOSE-DEAD-CLICK-001 DC3
count-assertion). `flow.yaml#navigates_to` declares both targets this Route closes:
`group-dashboard`, `group-list` (the `OnBack`/pop target — `flow.yaml` notes `OnBack` pops
through `group-type-picker` back onto `group-list`, so `onNavigateBack` stays a generic
pass-through rather than a hardcoded destination).

## preview

`GroupCreateScreenPreview.kt` — 12 `@Preview` functions: 1 top-level
`PreviewParameterProvider<GroupCreateState>` (8 state variants: Step 1 Identity, Step 2 Rules
VSLA share-based+social-fund, Step 2 Rules ROSCA fixed+rotating-payout, Step 3 Members, Step 4
Review, Submitting, Error (offline/Network), Success) + 11 sub-composable previews
(`GroupCreateWizardSection`, `GroupIdentityStepSection`, `GroupRulesStepSection` × 2 (share-based
+ fixed-rotating), `GroupMembersStepSection`, `GroupReviewStepSection`,
`GroupCreateSubmittingSection`, `GroupCreateErrorSection`, `GroupCreateSuccessSection`,
`OfflineSyncDialog`, `GroupCreateReviewCardBody`). Data source: `demo-data.yaml#entries` (`Office` rows 10/11/12,
`GroupTypeConfig` VSLA/ROSCA, `CreateGroupOrchestrationRequest` Mwangaza Women's Group / Kilimani
Merry-Go-Round, `CreateGroupOrchestrationResponse` invite code `MWG001`).

## tags

`GroupCreateTestTags` (append-only, RULE-KMP-COMPOSE-UITEST-001 CU-5) — `SCREEN`, `TOP_BAR`,
`TYPE_BANNER`, `STEP_INDICATOR`, `FIELD_GROUP_NAME`, `DROPDOWN_OFFICE`, `DROPDOWN_CURRENCY`,
`DROPDOWN_MEETING_DAY`, `FIELD_MEETING_TIME`, `FIELD_SHARE_VALUE`, `FIELD_SHARE_MIN`,
`FIELD_SHARE_MAX`, `FIELD_CONTRIBUTION_AMOUNT`, `DROPDOWN_PAYOUT_ORDER`,
`FIELD_LOAN_MULTIPLIER`, `FIELD_INTEREST_RATE`, `FIELD_CYCLE_LENGTH`, `FIELD_FINE_AMOUNT`,
`SWITCH_SOCIAL_FUND`, `FIELD_SOCIAL_FUND_PERCENT`, `FIELD_MAX_MEMBERS`, `REVIEW_CARD`,
`NEXT_BUTTON`, `BACK_BUTTON`, `SUBMIT_BUTTON`, `ERROR_BANNER`, `OFFLINE_NOTICE_BANNER`,
`OFFLINE_SYNC_DIALOG`, `OFFLINE_SYNC_DIALOG_CONFIRM`, `SUCCESS_SECTION` (30 total).
`NEXT_BUTTON`/`BACK_BUTTON`/`SUBMIT_BUTTON` are reused across steps — `ui.yaml#components`
declares exactly one `next_button`/`back_step_button`/`submit_button` component id, and only one
is ever composed on screen at a time (`currentStep`-gated visibility).

## permissions

Not applicable — `ui.yaml` declares no `request-permission` `on_click.action` for this feature.
<!-- kmp-screen-gen:END -->
