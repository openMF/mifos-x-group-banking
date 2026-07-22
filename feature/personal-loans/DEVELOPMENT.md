<!-- generated-by: kmp-viewmodel-gen -->
# feature/personal-loans — Development

## 1. Module Identity

`:feature:personal-loans` — the member's own loan list (`/loans`), entered from
`personal-dashboard`'s `user_taps_loan_card` entry point. Namespace
`org.mifos.groupbanking.feature.personalloans`. Source of truth:
`idea-layer/screens/personal-loans/{ui,data-flow}.yaml`.

<!-- kmp-viewmodel-gen:BEGIN -->
## 2. Public API

See `PersonalLoansViewModel.kt` class/field KDoc for the full ViewModel contract
(`PersonalLoansState`, `PersonalLoansEvent`, `PersonalLoansAction`, `PersonalLoansScreenState`,
`LoanError`) — not re-documented here to avoid drift; see `API.md#viewmodel` / `#state` /
`#actions` / `#events` / `#di`.

## 3. Consumers

- `feature/personal-loans/src/commonTest/.../PersonalLoansViewModelTest.kt`.
- `cmp-navigation` — `PersonalLoansModule` included in `KoinModules.kt#featureModule`;
  `implementation(projects.feature.personalLoans)` added to `cmp-navigation/build.gradle.kts`.
  `PersonalLoansScreen.kt` / `PersonalLoansRoute.kt` are `kmp-screen-gen` scope (not generated
  this pass).

## 4. Boundaries

Reads exclusively via the injected `LoanRepository.getLoansForClient(clientId)` (`core/data`) — no
direct Ktor/SQLDelight access from this module. `business_logic.kind: crud` per ui.yaml — the
legacy direct-`NetworkResult` template path (a plain `suspend fun`, not a Store5 stream; see
`LoanRepository.getLoansForClient` KDoc "Store5-free branch" note). `SessionManager`
(`core-base/security`) is injected and used for its one real capability — `endSession()` on a 401.
Status-filter chip taps and repayment-card expand/collapse are pure in-memory `PersonalLoansState`
transitions with no network call.

## 5. Data

See `API.md#state` / `#actions` / `#events` for the ViewModel field/payload schema.

## 6. Errors

`LoanError` (3 variants): `Network`/`Server` (`retry: true`), `Unauthorized` (`retry: false`,
calls `sessionManager.endSession()` for real). `NetworkError.BAD_REQUEST`/`NOT_FOUND`/`SERVER`/
`SERIALIZATION`/`UNKNOWN` all fold into `LoanError.Server` — this screen has no user-editable form,
so there is no `Validation` bucket to distinguish `BAD_REQUEST` the way
`LoanRequestViewModel.SubmitError.Validation` does.

## 7. Testing

`feature/personal-loans/src/commonTest/kotlin/org/mifos/groupbanking/feature/personalloans/PersonalLoansViewModelTest.kt`
(13 tests — initial-state nav-arg seeding, on_mount success → Content, on_mount empty → Empty
screen state, on_mount SERVER → Error screen state, on_mount UNAUTHORIZED → real
`sessionManager.endSession()`, `OnFilterChange` ACTIVE narrowing + no re-fetch, `OnFilterChange`
CLOSED→ALL round-trip + no re-fetch, `OnLoanExpand` toggle expand/collapse, `OnRefresh` real
re-fetch + `isRefreshing` clear, `OnRetry` error recovery, `OnRequestLoanClick` →
`NavigateToLoanRequest`). No `commonTest` UI-test suite yet for the screen layer — pending
`kmp-compose-uitest-gen` / `kmp-journey-uitest-gen`.

## 8. Observability

See `PersonalLoansViewModel.kt` KDoc (Kermit `Logger.i/d` + `CrashReporter` breadcrumbs +
`KptAnalyticsTracker.trackLoanOperation(...)` on mount, refresh, and request-loan CTA).

## 9. Evolution

- 2026-07-22 — `kmp-viewmodel-gen`: `PersonalLoansViewModel.kt` (state/action/event/error sealed
  types, plain-`suspend`-fetch on_mount + pull-to-refresh + retry, client-side status filter,
  loan-card expand/collapse toggle, 401 → real `sessionManager.endSession()`), `PersonalLoansModule.kt`
  (Koin, `clientId` nav-arg via `parametersOf`), feature module scaffold (`build.gradle.kts`,
  `settings.gradle.kts` include, `cmp-navigation` dependency + `KoinModules.kt` registration).
  Flagged idea-layer gaps (not invented): (1) `NavigateToLoanRequest` is emitted parameterless
  (verbatim `ui.yaml#events.members` mirror — no `params` key declared), but the destination
  `loan-request` screen's `LoanRequestViewModel` constructor requires `clientId` **and**
  `savingsBalance` **and** `loanMultiplier` — `savingsBalance` is reachable from neither
  `PersonalLoansState` nor this ViewModel's declared DI graph (`LoanRepository`, `SessionManager`
  only); no placeholder balance is fabricated here — flagged for
  RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1; (2) `ui.yaml#components.top_bar.on_navigation_click.action:
  NavigateBack` has no matching entry in `ui.yaml#state_model.actions.members` (the 5-member list
  `PersonalLoansAction` mirrors verbatim) — same drift class as `loan-list`'s/`loan-request`'s
  documented top-bar gap — flagged for RULE-IDEA-ACTION-CONTRACT-001; (3)
  `ui.yaml#components.loan_card.content.repayment_schedule_section` reads
  `loan.repaymentSchedule.periods`, but the canonical `core/model` `LoanSummary` domain type
  (shared with `loan-list`/`loan-detail`) carries no `repaymentSchedule` field — the
  expand/collapse toggle (`selectedLoanId`) is implemented for real, but the Screen layer has
  nothing to render inside the expanded panel until `LoanSummary` gains this field — flagged for
  RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1; (4) `NetworkError.TOO_MANY_REQUESTS` folds onto
  `LoanError.Unauthorized` (no dedicated `RateLimited` bucket is declared in
  `ui.yaml#state_model.errors.types`).
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## 10. Screen Layer

`PersonalLoansScreen.kt` / `PersonalLoansScreenPreview.kt` / `PersonalLoansTestTags.kt` /
`PersonalLoansRoute.kt` — generated by `kmp-screen-gen` (2026-07-22). Container (`PersonalLoansScreen`,
`koinViewModel(parameters = { parametersOf(clientId) })`) + stateless Content
(`PersonalLoansContent`), state-driven per `PersonalLoansState.deriveScreenState()`
(Loading/Content/Empty/Error, all 4 branches handled — `PersonalLoansLoadingSection` /
`PersonalLoansContentSection` / `PersonalLoansEmptySection` / `PersonalLoansErrorSection`).

- **Filter chips** (`components/PersonalLoansFilterChips.kt`) — 3 chips (All/Active/Closed, per
  `ui.yaml#components.filter_chips_row.chips` — no Overdue chip on this screen, unlike
  `loan-list`'s 4-chip row). Duplicated (not imported) from `loan-list`'s equivalent — no
  feature→feature dependency.
- **Loan card** (`components/PersonalLoansCard.kt`) — product name + colour-coded
  `LoanAccountStatus` badge, outstanding balance, next-repayment line (ACTIVE + non-null date
  only), overdue warning icon, expand/collapse chevron. Tap dispatches
  `PersonalLoansAction.OnLoanExpand(loan.id)` (in-place expand, NOT a navigation event —
  `ui.yaml#components.loan_card.on_click.action`). Colour tokens duplicated as
  `components/PersonalLoansStatusVisuals.kt` (`personalLoansBadgeContainerColor` /
  `personalLoansBadgeContentColor`), mapped onto `MaterialTheme.colorScheme.*` per
  `ui.yaml#components.loan_card.content.status_chip.style` semantic token names.
- **Expand panel** — `PersonalLoansCardDetails` (private, in `PersonalLoansCard.kt`) renders an
  honest "Loan Details" heading (deliberately NOT `ui.yaml`'s literal "Repayment Schedule" title)
  over the `LoanSummary` fields that actually exist (principal, outstanding, status, next-due
  date, overdue amount) — see `PersonalLoansCardDetails` KDoc for the full
  `repaymentSchedule`-field-gap rationale (same gap `PersonalLoansViewModel.kt`'s own KDoc
  flags — not re-invented here, just rendered honestly).
- **FAB** — `+ Request Loan`, visible only in the `Content` screenState (mirrors
  `ui.yaml#components.request_loan_fab.visible: loans.isNotEmpty()`), dispatches
  `PersonalLoansAction.OnRequestLoanClick`. The shared `FloatingActionButtonContent` contract has
  no `containerColor` override, so `ui.yaml`'s `background: secondary` swap is not reachable
  without touching `core/ui` — flagged, not worked around.
- **Pull-to-refresh** — `rememberKptPullToRefreshState(isEnabled = screenState == Content, ...)`
  dispatches `PersonalLoansAction.OnRefresh`.

### Route

`PersonalLoansRoute.kt` — `@Serializable data class PersonalLoansRoute(val clientId: Long)` +
`navigateToPersonalLoans(clientId)` + `NavGraphBuilder.personalLoansScreen(onNavigateToLoanRequest,
onNavigateBack)`. Only 2 callbacks — **no `onNavigateToLoanDetail`**: `ui.yaml#components.loan_card
.on_click` is `OnLoanExpand` (not navigation) and `flow.yaml#navigates_to` lists only
`loan-request` + `personal-dashboard` (back); inventing an unwired detail callback would fail the
generated-nav-vs-flow completion gate. DC3 count-assertion: 2 defaults / 2 overrides / 0
suppressed. NOT yet wired into `cmp-navigation`'s nav graph builder (that wiring, plus
`loan-request`'s `savingsBalance`/`loanMultiplier` supply, is a separate cross-feature task per
RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1).

### Preview

`PersonalLoansScreenPreview.kt` — 12 `@Preview` functions. Top-level `PersonalLoansContent` driven
by a `PreviewParameterProvider<PersonalLoansState>` with 5 variants (Loading, Content-all,
Content-ACTIVE-filtered-with-expanded-card, Empty, Error-Network) sourced from
`demo-data.yaml#entries[0].items[]` (3 `LoanDto` rows, `clientId: 101`) mapped onto `LoanSummary`
— member identity (`memberName: "Amina Wanjiru"`) borrowed from `personal-dashboard/demo-data.yaml`
since this screen is the signed-in member's own loans. Every non-container composable in
`PersonalLoansScreen.kt` (`PersonalLoansContent`, `PersonalLoansFabContent`,
`PersonalLoansLoadingSection`, `PersonalLoansContentSection`, `PersonalLoansEmptySection`,
`PersonalLoansErrorSection` × 2 variants incl. non-retryable `Unauthorized`) plus every
`components/` composable (`PersonalLoansCard` × 3 variants, `PersonalLoansCardSkeleton`,
`PersonalLoansFilterChips`) is covered.

### TestTags

`PersonalLoansTestTags.kt` — 11 `const val` + 2 stable-key functions (`cardTag(loanId)`,
`cardDetailsTag(loanId)`). Append-only per RULE-KMP-COMPOSE-UITEST-001 CU-5.

### i18n

30 keys in `commonMain/composeResources/values/strings.xml`, namespace
`screens_personal_loans_*`. Zero hardcoded `Text("…")` / `contentDescription = "…"` literals.
`loan.status.name` (dynamic enum binding) is the only `// i18n:skip` line, mirroring `loan-list`'s
identical convention.

### Permissions

None — `ui.yaml` declares no `on_click` with `action: request-permission` for this screen; no
`remember*PermissionRequester()` invocation was emitted.
<!-- kmp-screen-gen:END -->
