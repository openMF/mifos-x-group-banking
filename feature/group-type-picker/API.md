<!-- generated-by: kmp-screen-gen -->
# feature/group-type-picker — API

## viewmodel

`org.mifos.groupbanking.feature.grouptypepicker.GroupTypePickerViewModel` — extends
`BaseViewModel<GroupTypePickerState, GroupTypePickerEvent, GroupTypePickerAction>`
(`kpt.core.base.ui.viewmodel.BaseViewModel`). Full field/action/event contract documented in the
ViewModel source KDoc — this section is owned by `kmp-viewmodel-gen` on its next pass.

## di

`org.mifos.groupbanking.feature.grouptypepicker.di.GroupTypePickerModule` — Koin module,
`viewModelOf(::GroupTypePickerViewModel)`. Already included in `KoinModules.kt#featureModule`.

<!-- kmp-screen-gen:BEGIN -->
## screen

- `GroupTypePickerScreen(onNavigateToGroupCreate: (GroupTypeConfig) -> Unit, onNavigateBack: () -> Unit, modifier: Modifier = Modifier, viewModel: GroupTypePickerViewModel = koinViewModel())`
  — Container. Collects `viewModel.stateFlow` via `collectAsStateWithLifecycle`, routes
  `GroupTypePickerEvent.NavigateToGroupCreate(typeConfig)` → `onNavigateToGroupCreate(typeConfig)`
  and `GroupTypePickerEvent.NavigateBack` → `onNavigateBack()` through `EventsEffect`, delegates
  render to `GroupTypePickerContent`.
- `GroupTypePickerContent(state: GroupTypePickerState, onAction: (GroupTypePickerAction) -> Unit, modifier: Modifier = Modifier)`
  — stateless. `KptScaffold(onNavigationIconClick = { onAction(OnBack) }, title = ...)` +
  `when (state.screenState)`:
  - `Loading` → `GroupTypeLoadingSection()` (4× `GroupTypeCardSkeleton` + `CircularProgressIndicator`)
  - `Content` → `GroupTypeListSection(state.typeConfigs, onAction)` (`LazyColumn` of `GroupTypeCard`)
  - `Error` → `GroupTypeErrorSection(message = <resolved from state.error>, onRetry = { onAction(OnRetry) })`
- `GroupTypeLoadingSection(modifier: Modifier = Modifier)` — sub-composable, Loading state.
- `GroupTypeListSection(typeConfigs: List<GroupTypeConfig>, onAction: (GroupTypePickerAction) -> Unit, modifier: Modifier = Modifier)`
  — sub-composable, Content state. Each row dispatches
  `onAction(GroupTypePickerAction.OnTypeCardTap(config.typeSlug.name))` on tap.

### components

| Component | Signature | Notes |
|---|---|---|
| `GroupTypeCard` | `(config: GroupTypeConfig, icon: ImageVector, features: List<String>, onClick: () -> Unit, modifier: Modifier = Modifier, testTag: String = "")` | `AppCard`-wrapped row; `config.displayName`/`config.tagline` data-bound; merged-semantics touch target ≥48dp labelled with `config.displayName`; leading icon + trailing chevron decorative (`contentDescription = null`). |
| `GroupTypeCardSkeleton` | `(modifier: Modifier = Modifier)` | Shimmering loading placeholder row, mirrors `preview/loading.html`. |
| `GroupTypeErrorSection` | `(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier)` | Full-screen error surface, mirrors `preview/error.html`; `RETRY_BUTTON` test tag. |
| `groupTypeIcon` | `(slug: GroupTypeSlug): ImageVector` | Static per-type leading-icon lookup (`components/GroupTypeVisuals.kt`). |
| `groupTypeFeatureChips` | `@Composable (slug: GroupTypeSlug): List<String>` | Static per-type feature-chip label lookup, resolved from `strings.xml`. |

## route

`GroupTypePickerRoute.kt`:

- `@Serializable data object GroupTypePickerRoute` — `/groups/create/type`.
- `NavController.navigateToGroupTypePicker(navOptions: NavOptions? = null)`.
- `NavGraphBuilder.groupTypePickerScreen(onNavigateToGroupCreate: (GroupTypeConfig) -> Unit, onNavigateBack: () -> Unit)`
  — registers `GroupTypePickerScreen` via `composableWithRootPushTransitions<GroupTypePickerRoute>`.
  DC3 count-assertion: 0 `on{X}: () -> Unit = {}` defaults emitted in `GroupTypePickerScreen.kt`
  (both nav callbacks are required, non-default params) — 0 overrides needed, N/A rather than a
  violation.

`flow.yaml#navigates_to`: `group-create` (via `onNavigateToGroupCreate`, carrying the resolved
`GroupTypeConfig`), `group-list` (via `onNavigateBack`).

## tags

`GroupTypePickerTestTags` (append-only, RULE-KMP-COMPOSE-UITEST-001 CU-5):

| Constant | Value |
|---|---|
| `SCREEN` | `group_type_picker_screen` |
| `LOADING_INDICATOR` | `group_type_picker_loading_indicator` |
| `ERROR_BANNER` | `group_type_picker_error_banner` |
| `RETRY_BUTTON` | `group_type_picker_retry_button` |
| `TYPE_LIST` | `group_type_picker_type_list` |
| `TYPE_CARD_VSLA` | `group_type_picker_card_vsla` |
| `TYPE_CARD_ROSCA` | `group_type_picker_card_rosca` |
| `TYPE_CARD_ASCA` | `group_type_picker_card_asca` |
| `TYPE_CARD_SILC` | `group_type_picker_card_silc` |
| `TYPE_CARD_SHG` | `group_type_picker_card_shg` |
| `TYPE_CARD_SACCO` | `group_type_picker_card_sacco` |
| `TYPE_CARD_CBO` | `group_type_picker_card_cbo` |
| `TYPE_CARD_BURIAL` | `group_type_picker_card_burial` |
| `TYPE_CARD_JLG` | `group_type_picker_card_jlg` |
| `TYPE_CARD_UNKNOWN` | `group_type_picker_card_unknown` |

Plus `fun cardTag(slug: GroupTypeSlug): String` — resolves the stable constant for a given slug
(cards render data-driven, not as 9 separate composables).

## preview

`GroupTypePickerScreenPreview.kt` — 3 `@Preview` functions:

- `GroupTypePickerScreenPreview` (top-level, `PreviewParameterProvider<GroupTypePickerState>`,
  3 variants: Loading / Content (full 9-row `demo-data.yaml` catalogue) / Error).
- `GroupTypeLoadingSectionPreview`.
- `GroupTypeListSectionPreview` (9-row catalogue).

Data source: `demo-data.yaml#entries[0].items` (9 seeded `GroupTypeConfig` rows, verbatim field
values — no placeholder literals).

## i18n

`composeResources/values/strings.xml` — 32 keys under the `screens_group_type_picker_*` namespace:
`title`, `loading_message`, `action_retry`, `error_icon_cd`, `error_{network,server,auth}_message`,
and 25 `{slug}_feature_{n}` chip labels (3 per type except `rosca`/`burial`, which have 2). Card
`displayName`/`tagline` are intentionally NOT resource keys — they are data-bound from the live/
cached `GroupTypeConfig` returned by COMP-DT-003 (dynamic value, not a hardcoded literal).

## permissions

None — `group-type-picker` requires no runtime permission (`ui.yaml` declares no
`request-permission` `on_click`).
<!-- kmp-screen-gen:END -->
