<!-- generated-by: kmp-viewmodel-gen -->
# feature/loan-detail — Development

## 1. Module Identity

`:feature:loan-detail` — the tabbed single-loan detail screen (`/loans/{loanId}`), entered from
`loan-list`'s "Loan card tap". Namespace `org.mifos.groupbanking.feature.loandetail`. Source of
truth: `idea-layer/screens/loan-detail/{ui,flow,data-flow,docs}.yaml`.

<!-- kmp-viewmodel-gen:BEGIN -->
## 2. Public API

See `LoanDetailViewModel.kt` class/field KDoc for the full ViewModel contract
(`LoanDetailState`, `LoanDetailEvent`, `LoanDetailAction`, `LoanDetailScreenState`,
`LoanDetailError`) — not re-documented here to avoid drift; see `API.md#viewmodel` / `#state` /
`#actions` / `#events` / `#di`.

## 3. Consumers

- `feature/loan-detail/src/commonTest/.../LoanDetailViewModelTest.kt`.
- `cmp-navigation` — `LoanDetailModule` included in `KoinModules.kt#featureModule`;
  `implementation(projects.feature.loanDetail)` added to `cmp-navigation/build.gradle.kts`.
  `LoanDetailScreen.kt` / `LoanDetailRoute.kt` are `kmp-screen-gen` scope (not generated this
  pass).

## 4. Boundaries

Reads exclusively via the injected `LoanDetailRepository.loanDetailStream(loanId, scope)`
(`core/data`) — no direct API/DB access from this module. `business_logic.kind: crud` (read-only,
single-key composite) per RULE-IMPLEMENT-STORE5-001 — the offline-first
`ScreenDataStream<LoanDetailResponse>` is consumed directly. `SessionManager.endSession()`
(`core-base/security`) is called for real on `ScreenState.Unauthenticated` (mirrors
`GroupDashboardViewModel` / `LoanListViewModel`'s identical precedent); `NetworkMonitor` is
composed internally by `LoanDetailStore`, not re-injected here. `OnRecordRepayment` /
`OnMarkDefaulted` only OPEN their respective dialogs (`ShowRepaymentDialog` /
`ShowDefaultConfirmDialog`) — the actual `LoanRepository.recordRepayment` /
`.markDefaulted` mutation calls are declared under `api.yaml#dependencies.repositories` but have
no generated repository method yet (`data-flow.yaml` NOTE), so the mutation submit flow is a
separate, not-yet-generated dialog component/ViewModel — out of this ViewModel's scope.

## 5. Data

See `API.md#state` / `#actions` / `#events` for the ViewModel field/payload schema.

## 6. Errors

`LoanDetailError` (4 variants): `Network`, `Server` (both retryable), `NotFound`, `Auth` (both
non-retryable). `NotFound` has a documented reachability gap — `LoanDetailFetchException`'s
message carries no parseable HTTP status code, so it surfaces as `Server` in production today
(future-proofed mapping, see `LoanDetailError` KDoc).

## 7. Testing

`feature/loan-detail/src/commonTest/kotlin/org/mifos/groupbanking/feature/loandetail/LoanDetailViewModelTest.kt`
(13 tests — initial state, stream→state mapping for all 6 `ScreenState` members, all 6 declared
actions, the tab-switch pure transform, and the documented `canRecordRepayment`/`canMarkDefaulted`
no-role-source gap). No `commonTest` UI-test suite yet for the screen layer — pending
`kmp-compose-uitest-gen` / `kmp-journey-uitest-gen`.

## 8. Observability

See `LoanDetailViewModel.kt` KDoc (Kermit `Logger.i/d` + `CrashReporter` breadcrumbs +
`KptAnalyticsTracker.trackLoanOperation(...)` on mount and every user-initiated loan operation).

## 9. Evolution

- 2026-07-22 — `kmp-viewmodel-gen`: `LoanDetailViewModel.kt` (state/action/event sealed types,
  `ScreenState<LoanDetailResponse>` → `LoanDetailState` mapping, pure tab-switch transform, real
  `SessionManager.endSession()` on 401, dialog-opening `OnRecordRepayment`/`OnMarkDefaulted`
  handlers), `LoanDetailModule.kt` (Koin, `loanId` nav-arg via `parametersOf`), feature module
  scaffold (`build.gradle.kts`, `settings.gradle.kts` include, `cmp-navigation` dependency +
  `KoinModules.kt` registration). Flagged idea-layer gaps (not invented): (1)
  `LoanDetailState.canRecordRepayment` / `canMarkDefaulted` have no reachable role/permission
  source — the injectable `SessionManager` carries no role field and `ui.yaml#nav_params` declares
  only `loanId` (no `viewerRole` forwarded from `loan-list`, unlike `group-dashboard`'s
  `groupId`/`viewerRole` pair) — both stay at their conservative `false` default end-to-end; (2)
  `LoanRepository.recordRepayment` / `.markDefaulted` are declared under
  `api.yaml#dependencies.repositories` but have no generated repository method or first-class
  `api[]` operation — `OnRecordRepayment`/`OnMarkDefaulted` only open their dialogs for real, the
  mutation submit flow is a separate not-yet-generated component; (3) `top_bar.on_nav_click`'s
  `OnBack` wiring in `ui.yaml` IS present in `state_model.actions.members` (unlike the
  `loan-list`/`group-list` precedent gap) so no drift to flag there.
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## 2a. Screen / Route / Preview / Tags (kmp-screen-gen)

- `LoanDetailScreen.kt` — Container (`LoanDetailScreen`) + stateless Content
  (`LoanDetailContent`), state-driven per `LoanDetailScreenState` (Loading/Content/Error) with a
  single `LazyColumn` body (no nested scrollables) for the SCHEDULE/HISTORY tab-scoped rows.
  `loan-repayment-dialog` / `loan-mark-defaulted-dialog` are not yet generated — their events
  surface an in-screen "coming soon" `Snackbar` (intentional, documented stub).
- `LoanDetailRoute.kt` — `@Serializable LoanDetailRoute(val loanId: Long)`,
  `/loans/{loanId}`; registered for real on `cmp-navigation`'s `GroupBankingNavHost.kt`
  (`loan-list` → `loan-detail`, replacing the prior `PlaceholderRoute`).
- `LoanDetailScreenPreview.kt` — 5 top-level state variants + full sub-composable + component
  coverage, demo-data-sourced (`demo_data_resolved = true`).
- `LoanDetailTestTags.kt` — append-only test-tag catalog consumed by Compose UI tests + the
  Maestro flow generator.
- i18n: every user-facing string routes through `stringResource(Res.string.screens_loan_detail_*)`
  (`feature/loan-detail/src/commonMain/composeResources/values/strings.xml`); no permission
  requester codegen applies to this screen (no `request-permission` `on_click` in `ui.yaml`).

See `API.md#screen` / `#route` / `#preview` / `#tags`.
<!-- kmp-screen-gen:END -->
