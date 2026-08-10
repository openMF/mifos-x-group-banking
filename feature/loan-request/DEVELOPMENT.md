<!-- generated-by: kmp-viewmodel-gen -->
# feature/loan-request — Development

## 1. Module Identity

`:feature:loan-request` — the member-side loan application form (`/loan-request`), entered from
`personal-dashboard`'s "Request Loan" CTA or `personal-loans`'s FAB. Namespace
`kpt.feature.loanrequest`. Source of truth:
`idea-layer/screens/loan-request/{ui,data-flow}.yaml`.

<!-- kmp-viewmodel-gen:BEGIN -->
## 2. Public API

See `LoanRequestViewModel.kt` class/field KDoc for the full ViewModel contract
(`LoanRequestState`, `LoanRequestEvent`, `LoanRequestAction`, `LoanRequestScreenState`,
`SubmitError`) — not re-documented here to avoid drift; see `API.md#viewmodel` / `#state` /
`#actions` / `#events` / `#di`.

## 3. Consumers

- `feature/loan-request/src/commonTest/.../LoanRequestViewModelTest.kt`.
- `cmp-navigation` — `LoanRequestModule` included in `KoinModules.kt#featureModule`;
  `implementation(projects.feature.loanRequest)` added to `cmp-navigation/build.gradle.kts`.
  `LoanRequestScreen.kt` / `LoanRequestRoute.kt` are `kmp-screen-gen` scope (not generated this
  pass).

## 4. Boundaries

Reads/writes exclusively via the injected `LoanRequestRepository` (`core/data`) — no direct
Ktor/SQLDelight access from this module. `business_logic.kind: crud` per ui.yaml — the legacy
direct-`NetworkResult` template path (no Store5 read-stream to back; every read
(`clientId`/`savingsBalance`/`loanMultiplier`) arrives via nav-arg). `NetworkMonitor`
(`cmp-network-monitor`) is injected directly (not composed inside a Store) and drives both a
reactive `isOfflineMode` banner and a synchronous pre-flight before every submit attempt.
Transport-level `Network`/`Server` submit failures retry-enqueue to
`LoanRequestRepository.enqueueOffline` rather than surfacing an inline error — see
`LoanRequestViewModel.kt`'s `SubmitError` KDoc for the full rationale + flagged gap
(`NetworkError` has one `SERVER` bucket for both 500 and 503).

## 5. Data

See `API.md#state` / `#actions` / `#events` for the ViewModel field/payload schema.

## 6. Errors

`SubmitError` (4 variants): `Network`/`Server` (both `retry: true`, but never persisted to state —
routed straight into a retry-enqueue instead, see `API.md#state`), `Validation`/`Unauthorized`
(both `retry: false`, persisted + drive the real `error_snackbar`). `Unauthorized`'s declared
`401 -> navigate login-signup` contract has no dedicated nav event (`ui.yaml#events` declares only
3 members) — same documented gap class as `MemberAddError.Auth`/`LoanApplyError.Auth`.

## 7. Testing

`feature/loan-request/src/commonTest/kotlin/org/mifos/groupbanking/feature/loanrequest/LoanRequestViewModelTest.kt`
(16 tests — nav-arg seeding + `maxLoanAmount` derivation, reactive connectivity, amount validation
(non-numeric / below-floor / above-ceiling / valid), purpose selection + `isFormValid` transition,
duration-driven `repaymentEstimate` recomputation, blank-form submit-block, online submit success,
offline submit enqueue, `SERVER`/`REQUEST_TIMEOUT` transport-error retry-enqueue, `UNAUTHORIZED`
inline error, `OnRetry` re-submission, and success-dialog dismiss). No `commonTest` UI-test suite
yet for the screen layer — pending `kmp-compose-uitest-gen` / `kmp-journey-uitest-gen`.

## 8. Observability

See `LoanRequestViewModel.kt` KDoc (Kermit `Logger.i/d/w` + `CrashReporter` breadcrumbs +
`KptAnalyticsTracker.trackLoanOperation(...)` / `.trackOfflineOperation(...)` on mount and every
submit/enqueue transition).

## 9. Evolution

- 2026-07-22 — `kmp-viewmodel-gen`: `LoanRequestViewModel.kt` (state/action/event/error sealed
  types, local amount/purpose/duration validation + `repaymentEstimate` derivation, connectivity-
  gated submit with proactive offline-enqueue + reactive `Internal.ConnectivityChanged`, transport-
  error retry-enqueue for `Network`/`Server`, inline `SubmitError` for `Validation`/`Unauthorized`),
  `LoanRequestModule.kt` (Koin, `clientId`/`savingsBalance`/`loanMultiplier` nav-args via
  `parametersOf`), feature module scaffold (`build.gradle.kts`, `settings.gradle.kts` include,
  `cmp-navigation` dependency + `KoinModules.kt` registration). Flagged idea-layer gaps (not
  invented): (1) `ui.yaml#components.top_bar.on_click.action: NavigateBack` has no matching entry
  in `ui.yaml#state_model.actions.members` (the 6-member list `LoanRequestAction` mirrors
  verbatim) — same drift class as `loan-list`/`group-list`'s documented top-bar gap — flagged for
  RULE-IDEA-ACTION-CONTRACT-001; (2) `repaymentEstimate`'s group interest rate has no declared
  numeric source anywhere in `ui.yaml`/`api.yaml` — a documented flat 10% placeholder
  (`GROUP_INTEREST_RATE`) is used, flagged for RULE-IMPLEMENT-CROSS-FEATURE-FIT-001 CFF1; (3)
  `NetworkError` exposes one `SERVER` bucket for both HTTP 500 and 503, so `data-flow.yaml`'s
  differentiated `500 -> show_retry` / `503 -> enqueue_sync` rows collapse into a single
  retry-enqueue outcome per this generation step's explicit brief — flagged for the same CFF1
  station; (4) HTTP 409 ("duplicate pending request") has no dedicated `SubmitError` member —
  `NetworkError` carries no `CONFLICT` value, currently folds into `Server`.
<!-- kmp-viewmodel-gen:END -->

<!-- kmp-screen-gen:BEGIN -->
## Screen layer (kmp-screen-gen)

Adds to **§2 Public API**: `LoanRequestScreen.kt` (Container -- collects `LoanRequestState` via
`collectAsStateWithLifecycle`, seeds Koin via `parametersOf(clientId, savingsBalance,
loanMultiplier)`, consumes `LoanRequestEvent` via `EventsEffect`; the top app bar's back icon
invokes `onNavigateBack` directly -- no `LoanRequestAction` dispatch, see `API.md#screen`) +
`LoanRequestContent`/`LoanRequestFormFields`/`LoanRequestSubmitButton` (stateless, state-driven
per `LoanRequestScreenState`) + 8 `components/` (`LoanRequestDropdownField`,
`LoanRequestAmountField`, `LoanRequestOfflineBanner`, `LoanRequestSavingsLimitCard`,
`LoanRequestDurationSelector`, `LoanRequestRepaymentSummaryCard`, `LoanRequestSuccessDialog`,
`LoanRequestErrorCard`) + `LoanRequestRoute.kt` (nav destination) + `LoanRequestTestTags.kt` +
`LoanRequestScreenPreview.kt`.

Adds to **§3 Consumers**: `cmp-navigation` additionally consumes `LoanRequestRoute` /
`navigateToLoanRequest(...)` / `NavGraphBuilder.loanRequestScreen(...)` once a cross-feature nav
graph wires `personal-dashboard`/`personal-loans` entry points (not yet wired in this pass --
matches `feature/loan-apply`'s identical not-yet-wired precedent).

Adds to **§7 Testing**: no `commonTest` Compose UI-test suite yet for
`LoanRequestScreen.kt`/`components/` -- pending `kmp-compose-uitest-gen` /
`kmp-journey-uitest-gen` (same gap noted for the ViewModel layer in §7 above).

Adds to **§9 Evolution**:
- 2026-07-22 -- `kmp-screen-gen`: `LoanRequestScreen.kt` (Container + Content +
  `LoanRequestFormFields` + `LoanRequestSubmitButton`), 8 `components/*.kt`, `LoanRequestRoute.kt`,
  `LoanRequestTestTags.kt` (14 tags), `LoanRequestScreenPreview.kt` (11 `@Preview` functions), and
  49 `strings.xml` keys covering every `ui.yaml#i18n.en` entry (zero hardcoded UI strings). Money
  values route through a single shared `screens_loan_request_kes_amount_format` ("KES %1$s")
  template rather than Kotlin string interpolation, keeping the `KES` prefix localizable.
<!-- kmp-screen-gen:END -->
