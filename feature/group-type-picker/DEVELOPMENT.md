<!-- generated-by: kmp-screen-gen -->
# feature/group-type-picker — Development

## 1. Module Identity

`:feature:group-type-picker` — step 1a of the group-create wizard; presents the 9 seeded group
type cards (VSLA, ROSCA, ASCA, SILC, SHG, SACCO, CBO/Village-Bank, Burial/Welfare, JLG). Namespace
`kpt.feature.grouptypepicker`. Source of truth:
`idea-layer/screens/group-type-picker/{ui,api,docs,flow,data-flow,demo-data}.yaml`.

## 2. Public API

See `GroupTypePickerViewModel.kt` class/field KDoc for the full ViewModel contract
(`GroupTypePickerState`, `GroupTypePickerEvent`, `GroupTypePickerAction`,
`GroupTypePickerScreenState`, `GroupTypePickerError`) — not re-documented here to avoid drift; this
section is owned by `kmp-viewmodel-gen` on its next pass over this feature.

## 3. Consumers

- `feature/group-type-picker/src/commonTest/.../GroupTypePickerViewModelTest.kt`.
- `GroupTypePickerRoute.kt` (this pass) — not yet wired into the app-level nav graph
  (`cmp-navigation`); `group-create` is not yet a generated feature module in this codebase, so
  `onNavigateToGroupCreate` is a typed nav-arg contract for the caller to wire once that route
  exists.

## 4. Boundaries

Reads exclusively via the injected `GroupTypeConfigRepository` (`core/data`) — no direct API/DB
access from this module. `business_logic.kind: crud` (read-only catalogue) per
RULE-IMPLEMENT-STORE5-001 — `repository.groupTypeConfigsStream(scope)` is consumed directly as an
offline-first `ScreenDataStream`.

## 5. Data

See API.md#state / #actions / #events for the ViewModel field/payload schema and API.md#screen /
#route / #tags / #preview for the screen-layer contract.

## 6. Errors

`GroupTypePickerError` (3 variants): `Network`, `Server` (both retryable), `Auth` (non-retryable).
The Content layer maps each to `screens_group_type_picker_error_{network,server,auth}_message`.

## 7. Testing

`feature/group-type-picker/src/commonTest/kotlin/org/mifos/groupbanking/feature/grouptypepicker/GroupTypePickerViewModelTest.kt`
(ViewModel layer, pre-existing). No `commonTest` UI-test suite yet for the screen layer — pending
`kmp-compose-uitest-gen` / `kmp-journey-uitest-gen`.

## 8. Observability

See `GroupTypePickerViewModel.kt` KDoc (Kermit `Logger.i/w` + `CrashReporter` breadcrumbs +
`KptAnalyticsTracker.trackGroupOperation("select_type", ...)` on card tap).

## 9. Evolution

<!-- kmp-screen-gen:BEGIN -->
## 2b. Screen / UI layer

- `GroupTypePickerScreen.kt` — Container (`GroupTypePickerScreen`) + stateless Content
  (`GroupTypePickerContent`), state-driven over all 3 `GroupTypePickerScreenState` members
  (Loading/Content/Error).
- `GroupTypePickerRoute.kt` — `@Serializable data object GroupTypePickerRoute`
  (`/groups/create/type`) + `navigateToGroupTypePicker()` +
  `NavGraphBuilder.groupTypePickerScreen(onNavigateToGroupCreate, onNavigateBack)` (2 required nav
  callbacks, no dead-clickable defaults — DC3 count-assertion N/A, 0 `= {}` defaults emitted).
- `components/` — `GroupTypeCard` (data-bound name/tagline + static icon/feature-chip catalogue),
  `GroupTypeCardSkeleton` (loading shimmer row), `GroupTypeErrorSection` (full-screen error +
  retry), `GroupTypeVisuals.kt` (`groupTypeIcon` / `groupTypeFeatureChips` per-slug lookups).
- `GroupTypePickerTestTags.kt` — 15 append-only test tags (SCREEN, LOADING_INDICATOR,
  ERROR_BANNER, RETRY_BUTTON, TYPE_LIST, 9× `TYPE_CARD_*` + `TYPE_CARD_UNKNOWN` fallback, plus the
  `cardTag(slug)` resolver).
- `GroupTypePickerScreenPreview.kt` — 3 `@Preview` functions covering 3 state variants
  (Loading/Content/Error), Content populated with the full 9-row demo-data.yaml catalogue.
- `composeResources/values/strings.xml` — 32 `screens_group_type_picker_*` keys (zero hardcoded
  UI literals — RULE-IMPL-NO-HARDCODED-STRING-001; card `displayName`/`tagline` are intentionally
  data-bound from `GroupTypeConfig`, not resource literals, per the resolved design note below).

## 3b. Consumers (screen layer)

- `GroupTypePickerRoute.kt#groupTypePickerScreen(...)` — NOT YET wired into the app-level nav graph
  (`cmp-navigation`); `group-create` (the forward destination) does not exist yet in this codebase.
  Wiring is out of scope for `kmp-screen-gen` (feature-scoped) — tracked as an infra-layer
  follow-up once `group-create` is generated.
- `feature/group-type-picker/src/commonMain/kotlin/.../GroupTypePickerScreenPreview.kt`.

## Evolution (screen layer)

- 2026-07-22 — `kmp-screen-gen`: `GroupTypePickerScreen.kt` (Container+Content, all 3
  `screenState` branches), 4 reusable components, `GroupTypePickerRoute.kt`,
  `GroupTypePickerTestTags` (15 tags + 1 resolver fn), `GroupTypePickerScreenPreview.kt` (3
  `@Preview`, 3 state variants sourced from `demo-data.yaml`'s full 9-row catalogue), and
  `strings.xml` (32 keys, zero hardcoded literals). Resolved one design ambiguity: the Stitch
  preview's decorative lede ("Pick the model your group runs on. You can fine-tune the rules
  next.") is not declared in `ui.yaml#i18n` or `#components`, so it was omitted rather than
  invented — mirrors the same resolution precedent set on `login-signup`. A second design note:
  `GroupTypeConfig` (`core/model`) carries no `features` field, so the 2–3 feature-chip labels
  per card are a static per-`GroupTypeSlug` catalogue (`groupTypeFeatureChips`) verbatim from
  `ui.yaml#i18n`, while `displayName`/`tagline` are rendered data-bound from the live/cached
  `GroupTypeConfig` (dynamic — not a hardcoded literal, so not wrapped in `stringResource`).
<!-- kmp-screen-gen:END -->
