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
cache strategy, ttl=120). Feature ViewModels never call a Service directly (Repository/Store
boundary).

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
`GroupCreateApiTest`, `MemberDashboardApiTest`, `GroupDashboardApiTest`, `LoanDetailApiTest`
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
query-param default threading and success/401/404/500/malformed-JSON branches). Ktor
test deps (`ktor-client-mock`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`)
already declared in `core/network/build.gradle.kts`.

## 8. Observability

Kermit tag per Service (`CompanionAuthApi`, `GroupTypeConfigApi`, `GroupApi`, `InvitationApi`,
`GroupCreateApi`, `MemberDashboardApi`, `GroupDashboardApi`, `LoanDetailApi`) — debug on request
start, info on 2xx, error on every failure branch (status-mapped or transport/serialization
exception).

## 9. Evolution

Adding a new companion endpoint: extend the relevant `*Api` + `*ApiImpl` following the same
`requestAsNetworkResult` helper pattern; extend `idea-layer/screens/{feature}/api.yaml` first
(RULE-GAP-IDEA-FIRST-001) so the endpoint contract stays the single source of truth.
<!-- kmp-client-gen:END -->
