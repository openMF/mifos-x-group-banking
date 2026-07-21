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
- `SupabaseConfigClient` (`kpt.core.base.network`, wired here) — dynamic server config, inert by
  default.
- `CompanionAuthApiConfig` — Koin-injectable base-URL config for the companion backend.
- `GroupTypeConfigApiConfig` — Koin-injectable base-URL config for the group-type-config
  endpoint (override-surface symmetry; the implementation reuses the shared `HttpClient` today).

## 3. Consumers

`core/data` Repositories / `core/store` Store5 wrappers are the only consumers of `core/network`
Services — e.g. `AuthRepositoryImpl` (`core/data`) calls `CompanionAuthApi`; the
group-type-picker feature's Store5 store (downstream `kmp-store-gen` generation step) calls
`GroupTypeConfigApi`. Feature ViewModels never call a Service directly (Repository/Store
boundary).

## 4. Boundaries

- Every Service method returns `NetworkResult<T, NetworkError>` — never a raw thrown exception,
  never a `Result<T>` envelope.
- The single client-wide `Json` (installed on the `HttpClient` in `NetworkModule`) sets
  `ignoreUnknownKeys = true` + `coerceInputValues = true` (EC30 client-side half).
- `HttpTimeout` + `HttpRequestRetry` + `exponentialDelay` live on the shared `HttpClient` factory
  in `core-base/network` (SC3) — Service implementations do not re-implement resilience.
- Status-code → `NetworkError` mapping is the single table mirrored by
  `CompanionAuthApiImpl`, `GroupTypeConfigApiImpl`, and `core-base/network`'s
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

`commonTest` — `CompanionAuthApiTest`, `GroupTypeConfigApiTest` (MockEngine-backed; ≥3 cases per
method: success + at least two distinct error-status branches). Ktor test deps
(`ktor-client-mock`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`)
already declared in `core/network/build.gradle.kts`.

## 8. Observability

Kermit tag per Service (`CompanionAuthApi`, `GroupTypeConfigApi`) — debug on request start, info
on 2xx, error on every failure branch (status-mapped or transport/serialization exception).

## 9. Evolution

Adding a new companion endpoint: extend the relevant `*Api` + `*ApiImpl` following the same
`requestAsNetworkResult` helper pattern; extend `idea-layer/screens/{feature}/api.yaml` first
(RULE-GAP-IDEA-FIRST-001) so the endpoint contract stays the single source of truth.
<!-- kmp-client-gen:END -->
