<!-- generated-by: kmp-viewmodel-gen -->
<!-- co-authored-by: kmp-screen-gen -->
# feature/group-create — Development

## 1. Module Identity

`:feature:group-create` — 4-step, type-adaptive group creation wizard (Identity -> Rules ->
Members -> Review/Submit). Namespace `org.mifos.groupbanking.feature.groupcreate`.
Source of truth: `idea-layer/screens/group-create/{ui,api,docs,flow,data-flow}.yaml`.

<!-- kmp-viewmodel-gen:BEGIN -->
## 2. Public API

**ViewModel constructor deps** (`GroupCreateViewModel`):
- `initialTypeConfig: GroupTypeConfig` — nav-arg forwarded from `group-type-picker` (NOT a
  DI-graph type; supplied via Koin `parametersOf(typeConfig)`).
- `groupCreateRepository: GroupCreateRepository` (`core/data`) — `getOffices` / `createGroup`.
- `authRepository: AuthRepository` (`core/data`) — session read (`currentSession`), source of
  `userId` (substituted for the idea-layer's declared `SessionManager` — see class KDoc gap note).
- `networkMonitor: NetworkMonitor` (`io.github.mobilebytelabs.kmptoolkit.networkmonitor`,
  resolved via `DataModule`'s `single<NetworkMonitor>`) — proactive offline detection at submit.
- `analytics: KptAnalyticsTracker` (`core/analytics`).
- `crashReporter: CrashReporter` (`core-base/observability`).

**State fields** (`GroupCreateState`, verbatim mirror of `ui.yaml#state_model.state.fields`):
`currentStep`, `totalSteps`, `typeConfig` (`@Transient`), `groupTypeName`, `groupName`,
`officeId`, `officeName`, `currency`, `meetingDay`, `meetingTime`, `shareValue`, `shareMin`,
`shareMax`, `contributionAmount`, `payoutOrderMethod`, `loanMultiplier`, `interestRate`,
`cycleLengthMonths`, `fineAmount`, `socialFundEnabled`, `socialFundPercent`, `maxMembers`,
`inviteCode`, `isSubmitting`, `isSubmitSuccess`, `officeList` (`@Transient`),
`validationErrors`, `error` (`@Transient`), `isOffline`. `GroupCreateScreenState` is **derived**
(`GroupCreateState.deriveScreenState()`), not a stored field.

**Action variants** (`GroupCreateAction`, 21 — verbatim mirror of
`ui.yaml#state_model.actions.members`): `OnNameChange`, `OnOfficeSelect`, `OnCurrencyChange`,
`OnMeetingDaySelect`, `OnMeetingTimeSelect`, `OnShareValueChange`, `OnShareMinChange`,
`OnShareMaxChange`, `OnContributionAmountChange`, `OnPayoutOrderChange`,
`OnLoanMultiplierChange`, `OnInterestRateChange`, `OnCycleLengthChange`, `OnFineAmountChange`,
`OnSocialFundToggle`, `OnSocialFundPercentChange`, `OnMaxMembersChange`, `OnNextStep`,
`OnPreviousStep`, `OnSubmit`, `OnBack` — plus the sanctioned `Internal` async-result
sub-interface (`SessionChecked`, `ConnectivityChanged`, `OfficesLoaded`, `SubmitResult`).

**Event variants** (`GroupCreateEvent`): `NavigateToGroupDashboard(groupId)`, `NavigateBack`,
`ShowOfflineSyncDialog`, `ShowSnackbar(message)`.

## 3. Consumers

- `GroupCreateScreen.kt` / `GroupCreateRoute.kt` (co-located in this module, not
  `cmp-navigation` — same convention as `LoginSignupRoute.kt`/`JoinWithCodeRoute.kt`); generated
  by `kmp-screen-gen`. `groupCreateScreen(...)` is not yet wired into the app-wide
  `NavGraphBuilder` host — see API.md#route.
- `feature/group-create/src/commonTest/.../GroupCreateViewModelTest.kt` (13 tests, dispatches
  every Action, covers both type-adaptive branches).

## 4. Boundaries

Reads/writes exclusively via the injected `GroupCreateRepository`/`AuthRepository` (`core/data`)
— no direct `GroupCreateApi`/`CompanionSessionStore`/HttpClient access from this module. No
Store5 (`business_logic.kind: complex`, Step 4 is a write-only mutation orchestration flow per
RULE-IMPLEMENT-STORE5-001, so `NetworkResult` is consumed directly, not `.asScreenStream()`).

## 5. Data

See API.md#state / #actions / #events for the full field/payload schema.

## 6. Errors

`GroupCreateError` (4 variants, verbatim mirror of `ui.yaml#state_model.errors.types`):
`Validation`, `Network` (-> `ShowOfflineSyncDialog`), `Server`, `Auth` (-> `ShowSnackbar`, KNOWN
GAP: no dedicated `NavigateToLogin` event declared for the `redirect: login` contract). Every
submit failure sets `GroupCreateState.error` + emits a `crashReporter.recordMessage(...,
CrashSeverity.Warning)` breadcrumb.

## 7. Testing

`feature/group-create/src/commonTest/kotlin/org/mifos/groupbanking/feature/groupcreate/GroupCreateViewModelTest.kt`
— 13 `@Test` functions: nav-arg seeding, office load, field-change + validation-error clearing,
Step 1 validation block/pass, previous-step retains fields, `OnBack` always navigates back, the
two type-adaptive Step 2 branches (VSLA share-based vs. ROSCA fixed+rotating-payout), submit
success/offline/no-session/server-error.

## 8. Observability

Kermit `Logger.i/w/e(TAG) { ... }` (`TAG = "GroupCreateViewModel"`) — PII-safe (group/office
identifiers and counts only). `KptAnalyticsTracker.trackGroupOperation(operation = "create", ...)`
fires on every submit attempt (start + success/failure). `crashReporter.setUser(session?.userId)`
on every session emission; `crashReporter.recordMessage` breadcrumb on mount, offline submit
attempts, office-load failures, and submit failures.

## 9. Evolution

- 2026-07-22 — `kmp-viewmodel-gen`: initial `GroupCreateViewModel` (MVI, 4-step type-adaptive
  wizard) + State/Event/Action + Koin module + 13-test suite. Flagged 3 idea-layer/source drifts
  in the generation report: (a) `ui.yaml#state_model.di` names `SessionManager` for `userId`, but
  the real class has no such concept — substituted `AuthRepository.currentSession`; (b)
  `ui.yaml#nav_params.typeConfig` documents `shareout_formula`/`share_value`/etc. on the nav-arg
  that the actual `GroupTypeConfig` model doesn't carry — defaulted `shareoutFormula` to `NONE`;
  (c) no `SyncQueueRepository`/offline-outbox DI exists yet (`OutboxQualifiers` has zero entries)
  — submission falls back to a proactive-offline-check + manual-retry seam rather than a
  half-built `DraftSubmitHandler` wiring; `CreateGroupRequest`/`CreateGroupTypeConfig` marked
  `@Serializable` in `core/model` as the forward-compat step.
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## UI Surface (Screen / Route / Preview / TestTags)

**Container + Content** (`GroupCreateScreen.kt`) — `GroupCreateScreen` (Container, collects
`stateFlow`, consumes `GroupCreateEvent` via `EventsEffect`, manages local `showOfflineDialog`
state) + `GroupCreateContent` (stateless, `state.deriveScreenState()`-driven `when`). 10 further
composables: `GroupCreateWizardSection`, `GroupIdentityStepSection`, `GroupRulesStepSection`,
`GroupMembersStepSection`, `GroupReviewStepSection`, `GroupCreateSubmittingSection`,
`GroupCreateErrorSection`, `GroupCreateSuccessSection`, `OfflineSyncDialog`,
`GroupCreateReviewCardBody`. 7 reusable
components under `components/`: `GroupTypeBanner`, `WizardStepIndicator`, `GroupCreateTextField`,
`GroupCreateDropdownField`, `ReviewSection`, `OfflineNoticeBanner`, `GroupCreateErrorBanner`. See
API.md#screen.

**Route** (`GroupCreateRoute.kt`) — `/groups/create`, co-located in this module. `GroupTypeConfig`
is flattened into primitive `@Serializable` route args (it is not itself `@Serializable`) via
`toGroupCreateRoute()`/`toGroupTypeConfig()`. Pure pass-through — DC3 count-assertion 0/0/0. See
API.md#route.

**Preview** (`GroupCreateScreenPreview.kt`) — 12 `@Preview` functions (top-level
`PreviewParameterProvider` with 8 `GroupCreateState` variants + 11 sub-composable previews), data
sourced from `demo-data.yaml#entries`. See API.md#preview.

**TestTags** (`GroupCreateTestTags.kt`) — 30 append-only constants (`NEXT_BUTTON`/`BACK_BUTTON`/
`SUBMIT_BUTTON` reused across steps — only one is ever composed at a time). See API.md#tags.

**i18n** — ~100 string keys under `screens_group_create_*` appended to
`src/commonMain/composeResources/values/strings.xml` (en only — other locales are a
translation-team follow-up per `training-layer/instructions/stream-first/latest/CORE_I18N.md`).

- 2026-07-22 — `kmp-screen-gen`: `GroupCreateScreen.kt` (Container+Content+10 sections) + 7
  components + `GroupCreateRoute.kt` + `GroupCreateTestTags.kt` (30 tags) +
  `GroupCreateScreenPreview.kt` (12 previews) + `strings.xml`. Flagged follow-ups: (a)
  `ui.yaml#components.review_card.content.review_rules_section.rows` hardcodes `"KES {{value}}"`
  templates — resolved dynamically from `GroupCreateState.currency` instead (see API.md#screen);
  (b) `groupCreateScreen(...)` is not yet wired into the app-wide `NavGraphBuilder` host — same
  pending gap as `login-signup`/`join-with-code`; (c) `GroupTypeConfig` not being
  `@Serializable` required flattening it into `GroupCreateRoute`'s primitive fields rather than
  passing the domain type directly through Navigation — flagged for a possible future
  `@Serializable` annotation on `GroupTypeConfig` in `core/model`; (d) meeting-time entry is a
  formatted text field (`"09:00"` placeholder), not a native platform time-picker dialog — no
  cross-platform (Android/iOS/Desktop/Web) Compose Multiplatform time-picker primitive exists in
  this codebase yet.
<!-- kmp-screen-gen:END -->
