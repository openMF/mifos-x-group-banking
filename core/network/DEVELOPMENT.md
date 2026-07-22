<!-- generated-by: kmp-client-gen -->
<!-- kmp-client-gen:BEGIN -->
# core/network — DEVELOPMENT.md

## 1. Module Identity

`core/network` is the Ktor client + Service layer of the KMP module-placement roster
(`core/registries/KMP_MODULE_PLACEMENT.yaml` → `KtorService`/`Api`/`Dto`). It owns every
`*Service.kt`/`*Api.kt` implementation and the wire DTOs under `core/network/model`. It consumes
`core-base/network` (`HttpClient` factory, `NetworkResult<T, NetworkError>`, `AppErrorMapper`)
as a library — never edits it.

## 2. Public API

- `CompanionAuthApi` / `CompanionAuthApiImpl` (`.../service/loginsignup/`) — self-register,
  login, biometric-refresh `me` (COMP-AUTH-001/002/003). See API.md#services.
- `GroupTypeConfigApi` / `GroupTypeConfigApiImpl` (`.../service/grouptypepicker/`) — fetch the
  seeded group-type catalogue (COMP-DT-003). See API.md#services.
- `GroupApi` / `GroupApiImpl` (`.../service/grouplist/`) — fetch the authenticated user's
  offset-paginated group list (COMP-GRP-001). See API.md#services.
- `InvitationApi` / `InvitationApiImpl` (`.../service/joinwithcode/`) — validate an invite
  token, fetch the group-preview card, associate the invitee to the group, and mark the
  invitation datatable row as accepted (COMP-DT-004 + COMP-GRP-003). See API.md#services.
- `GroupCreateApi` / `GroupCreateApiImpl` (`.../service/groupcreate/`) — fetch the office
  dropdown list (raw Fineract `/offices` passthrough) and submit the group-create wizard's
  companion orchestration call (COMP-GRP-001). See API.md#services.
- `MemberDashboardApi` / `MemberDashboardApiImpl` (`.../service/personaldashboard/`) — fetch the
  unified member dashboard (COMP-DASH-001). See API.md#services.
- `GroupDashboardApi` / `GroupDashboardApiImpl` (`.../service/groupdashboard/`) — the FOUR
  independent group-dashboard companion reads fired in parallel on mount/refresh/retry:
  `getGroup` (identity + embedded `typeConfig`), `getViewerRole` (authenticated user's role),
  `getGroupCorpus` (balance/rotation state), `getGroupAccounts` (savings/loan summary +
  recent-activity feed) (COMP-GRP-001). See API.md#services.
- `LoanDetailApi` / `LoanDetailApiImpl` (`.../service/loandetail/`) — `getLoanDetail`, the single
  composite read bundling the loan header, repayment schedule, and transaction history
  (`get_loan_detail`/`get_loan`). See API.md#services.
- `LoanApplyApi` / `LoanApplyApiImpl` (`.../service/loanapply/`) — the loan-apply form's 7
  endpoints: `getGroupMembers`, `getLoanProducts`, `getLoanTemplate`, `getMemberSavings`,
  `getGroupCorpus`, `getGroupLoanConfig`, `applyLoan`. See API.md#services.
- `BatchSyncApi` / `BatchSyncApiImpl` (`.../service/batchsync/`) — the sync-status feature's
  single endpoint: `batchSync`, submitting the entire `SyncQueueRepository` pending backlog as
  one Fineract Batch API request (`POST /fineract-provider/api/v1/batches`). Response is a
  top-level JSON array, decoded via Ktor's reified-generic content negotiation. See
  API.md#services.
- `SupabaseConfigClient` (`kpt.core.base.network`, wired here) — dynamic server config, inert by
  default.
- `CompanionAuthApiConfig` — Koin-injectable base-URL config for the companion backend.
- `GroupTypeConfigApiConfig` — Koin-injectable base-URL config for the group-type-config
  endpoint (override-surface symmetry; the implementation reuses the shared `HttpClient` today).
- `GroupApiConfig` — Koin-injectable base-URL config for the group-list endpoint
  (override-surface symmetry; the implementation reuses the shared `HttpClient` today).
- `InvitationApiConfig` — Koin-injectable base-URL config for the join-with-code endpoints
  (override-surface symmetry; the implementation reuses the shared `HttpClient` today).
- `GroupCreateApiConfig` — Koin-injectable base-URL config for the group-create wizard endpoints
  (override-surface symmetry; the implementation reuses the shared `HttpClient` today).
- `MemberDashboardApiConfig` — Koin-injectable base-URL config for the personal-dashboard
  endpoint (override-surface symmetry; the implementation reuses the shared `HttpClient` today).
- `GroupDashboardApiConfig` — Koin-injectable base-URL config for the group-dashboard endpoints
  (override-surface symmetry; the implementation reuses the shared `HttpClient` today).
- `LoanDetailApiConfig` — Koin-injectable base-URL config for the loan-detail endpoint
  (override-surface symmetry; the implementation reuses the shared `HttpClient` today).
- `LoanApplyApiConfig` — Koin-injectable base-URL config for the loan-apply endpoints
  (override-surface symmetry; the implementation reuses the shared `HttpClient` today).
- `BatchSyncApiConfig` — Koin-injectable base-URL config for the sync-status batch-drain
  endpoint (override-surface symmetry; the implementation reuses the shared `HttpClient` today).
- `ChangePinApi` / `ChangePinApiImpl` (`.../service/changepin/`) — the settings screen's single
  endpoint: `changePin` (`PUT /fineract-provider/api/v1/self/user/updatePassword`, BasicAuth).
  See API.md#services.
- `ChangePinApiConfig` — Koin-injectable base-URL config for the change-PIN endpoint
  (override-surface symmetry; the implementation reuses the shared `HttpClient` today).
- `SavingsApi` / `SavingsApiImpl` (`.../service/savings/`) — the shared savings client stack for
  personal-savings/member-savings-detail/savings-dashboard: `getSavingsTransactions` (reused for
  BOTH the group-linked and individual accounts), `getMemberSavingsDetail`,
  `getGroupSavingsSummary`, `getIndividualSavingsSummary`. See API.md#services.
- `SavingsApiConfig` — Koin-injectable base-URL config for the savings endpoints
  (override-surface symmetry; the implementation reuses the shared `HttpClient` today).

## 3. Consumers

`core/data` Repositories / `core/store` Store5 wrappers are the only consumers of `core/network`
Services — e.g. `AuthRepositoryImpl` (`core/data`) calls `CompanionAuthApi`; the
group-type-picker feature's Store5 store (downstream `kmp-store-gen` generation step) calls
`GroupTypeConfigApi`; the group-list feature's Store5 store (downstream `kmp-store-gen`
generation step) will call `GroupApi` and surface it via `.asPagingScreenStream()` (COMP-GRP-001
is offset-paginated); `InvitationRepositoryImpl` (`core/data`) calls `InvitationApi` directly
(Store5-free — no read-stream to cache, see API.md#services). `GroupCreateRepositoryImpl`
(`core/data`) calls `GroupCreateApi` directly for `createGroup` (Store5-free mutation
orchestration); `getOffices` is likewise a direct pass-through today pending a future
`kmp-store-gen` `OfficeStore` for its declared stale-while-revalidate cache strategy (see
API.md#services). The personal-dashboard feature's Store5 store (downstream `kmp-store-gen`
generation step) will call `MemberDashboardApi`. The group-dashboard feature's Store5 store
(downstream `kmp-store-gen` generation step) will call all 4 `GroupDashboardApi` methods in
parallel and fan them into the composite dashboard model, surfaced via `.asScreenStream()`. The
loan-detail feature's Store5 store (downstream `kmp-store-gen` generation step) will call
`LoanDetailApi.getLoanDetail` and surface it via `.asScreenStream()` (`stale_while_revalidate`
cache strategy, ttl=120). `LoanApplyRepositoryImpl` (`core/data`) calls all 7 `LoanApplyApi`
methods directly — `getGroupMembers` is a standalone read; `getLoanProducts`/`getLoanTemplate`/
`getMemberSavings`/`getGroupCorpus`/`getGroupLoanConfig` fire in parallel
(`kotlinx.coroutines.coroutineScope`+`async`) and fan into the composite `LoanApplyTemplate`
domain model; `applyLoan` submits the form (Store5-free — no `AppStoreRegistry.LoanApply` entry
exists yet). `SyncManagerImpl` (`core/data`) calls `BatchSyncApi.batchSync` directly —
Store5-free (`sync-status`'s `data-flow.yaml` declares `cache.strategy: no_cache` on every
entry, no read-stream to cache). `ChangePinRepositoryImpl` (`core/data`) calls
`ChangePinApi.changePin` directly — Store5-free (`business_logic.kind: crud`, no read-stream to
cache; change-PIN is a pure fire-and-forget write), same branch as `InvitationRepositoryImpl`/
`GroupCreateRepositoryImpl`. `SavingsRepositoryImpl` (`core/data`) calls all 4 `SavingsApi`
methods directly — `getSavingsTransactions`/`getMemberSavingsDetail`/`getGroupSavingsSummary`/
`getIndividualSavingsSummary` are standalone reads; `getSavingsTransactions`+`getSavingsTransactions`
(twice, per account) and `getGroupSavingsSummary`+`getIndividualSavingsSummary` also fire in
parallel (`kotlinx.coroutines.coroutineScope`+`async`) inside the `loadMemberSavings`/
`loadSavingsDashboard` composites — Store5-free (no `AppStoreRegistry.Savings` entry exists yet).
Feature ViewModels never call a Service directly (Repository/Store boundary).

## 4. Boundaries

- Every Service method returns `NetworkResult<T, NetworkError>` — never a raw thrown exception,
  never a `Result<T>` envelope.
- The single client-wide `Json` (installed on the `HttpClient` in `NetworkModule`) sets
  `ignoreUnknownKeys = true` + `coerceInputValues = true` (EC30 client-side half).
- `HttpTimeout` + `HttpRequestRetry` + `exponentialDelay` live on the shared `HttpClient` factory
  in `core-base/network` (SC3) — Service implementations do not re-implement resilience.
- Status-code → `NetworkError` mapping is the single table mirrored by
  `CompanionAuthApiImpl`, `GroupTypeConfigApiImpl`, `GroupApiImpl`, and `core-base/network`'s
  `ResultSuspendConverterFactory` (400/401/404/408/429/5xx/else→UNKNOWN) — kept in lock-step
  deliberately.

## 5. Data

Wire DTOs live in `core/network/model` (see that module's own `API.md`/`DEVELOPMENT.md`).
`core/network` itself holds no persisted state — it is a pure request/response boundary.

## 6. Errors

4xx/5xx map to `NetworkError` (`BAD_REQUEST`/`UNAUTHORIZED`/`NOT_FOUND`/`REQUEST_TIMEOUT`/
`TOO_MANY_REQUESTS`/`SERVER`); deserialization failures map to `NetworkError.SERIALIZATION`;
transport failures (timeouts, no connectivity) map to `NetworkError.UNKNOWN`. Every branch is
Kermit-logged (`Logger.d` on start, `Logger.i` on success, `Logger.e` on failure) before
returning — no silent failures.

## 7. Testing

`commonTest` — `CompanionAuthApiTest`, `GroupTypeConfigApiTest`, `GroupApiTest`, `InvitationApiTest`,
`GroupCreateApiTest`, `MemberDashboardApiTest`, `GroupDashboardApiTest`, `LoanDetailApiTest`,
`LoanApplyApiTest` (24 MockEngine tests, all green — success + ≥2 error branches per method
across all 7 `LoanApplyApi` methods, incl. `applyLoan`'s 400/403(→UNKNOWN)/500/malformed-JSON
matrix; malformed-JSON coverage required catching `io.ktor.serialization.ContentConvertException`
in this Service's local `requestAsNetworkResult` helper — a gap the `LoanApi`/`MemberAddApi`
precedent's copy of the same helper does NOT close, flagged for a future upstream fix)
(MockEngine-backed; ≥3 cases per method: success + at least two distinct error-status branches;
`GroupApiTest` also covers default/explicit `paged`/`limit`/`offset` query-param threading;
`InvitationApiTest` covers all 4 `InvitationApi` methods incl. path templating for
`{code}`/`{groupId}`/`{code}/{rowId}` and the `associateClientToGroup`/`markInvitationAccepted`
request-body wiring; `GroupCreateApiTest` covers `getOffices` default/explicit `orderBy`
query-param threading + missing-`externalId` mapping, and `createGroup` success + the full
400/401/403/409/500 status-mapping matrix incl. the two else-branch-to-UNKNOWN cases;
`MemberDashboardApiTest` covers `selectedGroupId` present/null query-param threading;
`GroupDashboardApiTest` covers all 4 `GroupDashboardApi` methods (`getGroup`/`getViewerRole`/
`getGroupCorpus`/`getGroupAccounts`) incl. path templating for `{groupId}` +
`/my-role`/`/corpus`/`/accounts` suffixes and success/401/404/500/malformed-JSON branches;
`LoanDetailApiTest` covers `getLoanDetail`'s single composite read incl. the `associations`
query-param default threading and success/401/404/500/malformed-JSON branches).
`BatchSyncApiTest` (7 MockEngine tests, all green) covers `batchSync`'s success/mixed-status/
empty-array/400/401/500/malformed-body branches, incl. a non-array-JSON-object 2xx body mapping
to `SERIALIZATION` (this endpoint's response is array-shaped, not object-shaped). Ktor
test deps (`ktor-client-mock`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`)
already declared in `core/network/build.gradle.kts`. `ChangePinApiTest` (6 MockEngine tests, all
green) covers `changePin`'s success (PUT verb + path assertions) + 400/401/500 status-mapping +
malformed-JSON→SERIALIZATION + the password/repeatPassword-carry-the-same-new-PIN-value
assertion. `SavingsApiTest` (24 MockEngine tests, all green) covers all 4 `SavingsApi` methods —
`getSavingsTransactions`'s default/explicit `limit`/`offset` threading and reuse across both
group-linked and individual `savingsId` values, `getMemberSavingsDetail`'s pagination-param
threading, `getGroupSavingsSummary`/`getIndividualSavingsSummary`'s path assertions — each with
success + empty-list + ≥3 distinct error-status branches (401/403→UNKNOWN/404/500/503→SERVER) +
malformed-JSON→SERIALIZATION.

## 8. Observability

Kermit tag per Service (`CompanionAuthApi`, `GroupTypeConfigApi`, `GroupApi`, `InvitationApi`,
`GroupCreateApi`, `MemberDashboardApi`, `GroupDashboardApi`, `LoanDetailApi`, `LoanApplyApi`,
`BatchSyncApi`, `ChangePinApi`, `SavingsApi`) — debug on request start (incl. request-row count for
`BatchSyncApi`), info on 2xx, error on every failure branch (status-mapped or
transport/serialization exception).

## 9. Evolution

Adding a new companion endpoint: extend the relevant `*Api` + `*ApiImpl` following the same
`requestAsNetworkResult` helper pattern; extend `idea-layer/screens/{feature}/api.yaml` first
(RULE-GAP-IDEA-FIRST-001) so the endpoint contract stays the single source of truth.
<!-- kmp-client-gen:END -->
