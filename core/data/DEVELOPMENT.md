<!-- generated-by: kmp-client-gen -->
<!-- kmp-client-gen:BEGIN -->
# core/data — DEVELOPMENT.md

## 1. Module Identity

`core/data` is the Repository composition layer of the KMP module-placement roster
(`core/registries/KMP_MODULE_PLACEMENT.yaml` → `Repository`/`RepositoryImpl`). It composes
`core/network` (Services) + `core/datastore` (prefs) + `core/database` (Room) + `core/store`
(Store5) behind a per-feature Repository interface. Never edits `core/data/infra/**`
(framework-shared infra sub-path).

## 2. Public API

- `AuthRepository` / `AuthRepositoryImpl` — companion auth mutation + session-read repository
  (COMP-AUTH-001/002/003). See API.md#repositories.
- `InvitationRepository` / `InvitationRepositoryImpl` — join-with-code mutation orchestration
  repository (`validateCode` / `fetchGroupPreview` / `joinGroup` — associate-then-mark-accepted,
  COMP-DT-004 + COMP-GRP-003). See API.md#repositories.
- `UserDataRepository` / `UserDataRepositoryImpl` — template-provided user preferences
  (theme/language/passcode), unrelated to the companion auth session.
- `UserLogoutManager` / `UserLogoutManagerImpl` — logout event bus + cache-clear orchestration.
- `GroupCreateRepository` / `GroupCreateRepositoryImpl` — group-create wizard repository
  (`getOffices` office dropdown + `createGroup` companion orchestration, COMP-GRP-001). See
  API.md#repositories.

## 3. Consumers

Feature ViewModels are the only consumers of `core/data` Repositories — e.g. a future
`login-signup` ViewModel injects `AuthRepository` and wires `login`/`selfRegister` behind a
`SubmitHandler`/`DraftSubmitHandler` (per this project's `core-base/store` input-screen
convention), and observes `currentSession` for the on-mount token-presence check. The
`join-with-code` ViewModel injects `InvitationRepository` directly — `validateCode` on
`OnValidateCode`, `fetchGroupPreview` chained on a valid non-expired/non-used result, `joinGroup`
on `OnConfirmJoin` (threading `clientId` from `AuthRepository.currentSession` and `rowId` from
wherever the contract-gap resolution lands — see API.md's KNOWN GAP note). The group-create
wizard ViewModel injects `GroupCreateRepository` directly — `getOffices` on-mount for the office
dropdown, `createGroup` on `OnSubmit` wrapped in a `viewModelScope.draftSubmitHandler<
CreateGroupRequest, GroupCreationResult>(...)` (this project's offline-resilient input-screen
convention) so a network failure at submit time persists the payload to the outbox instead of
losing it — see API.md's offline-queue note.

## 4. Boundaries

- `AuthRepository` is on the **legacy/mutation path**, not the Store5 path: `login-signup`'s
  `business_logic.kind` is `processor` (a write+session-read flow), which
  RULE-IMPLEMENT-STORE5-001 / RULE-IDEA-IMPL-INTELLIGENCE-001 scope OUT of `.asScreenStream()` —
  Store5 is reserved for read-streams. `AuthRepositoryImpl` therefore surfaces
  `NetworkResult<T, NetworkError>` directly rather than `ScreenState<V>`.
- **No try-catch in `AuthRepositoryImpl`** — every method is a plain `when` over
  `CompanionAuthApi`'s sealed `NetworkResult`; `CompanionAuthApiImpl` (core/network) is the sole
  layer allowed to catch exceptions.
- Session-token persistence flows through `core/datastore`'s `CompanionSessionStore`
  (SECURE-Settings-backed) — `AuthRepositoryImpl` never touches `Settings` directly.
- Store5-backed Repositories (when a future feature declares a read-stream) read via
  `.asScreenStream()` / `.asPagingScreenStream()` and write via `MutableStore.write(...)` — never
  a hand-rolled DAO bypass.
- `GroupCreateRepository` is likewise on the legacy/mutation path for `createGroup`
  (`business_logic.kind: processor`) — no try-catch, plain `when` over `GroupCreateApi`'s
  `NetworkResult`. `getOffices` has a DECLARED SC2 cache strategy it does not yet honour (no
  `OfficeStore` in `AppStoreRegistry` today) — flagged, not silently ignored; see API.md's
  KNOWN SC2 GAP note.

## 5. Data

`AuthRepository` composes: `CompanionAuthApi` (core/network, wire calls) →
`LoginSignupMappers` (DTO→domain) → `CompanionSessionStore` (core/datastore, token
persistence). Domain types (`AuthSession`, `UserProfile`, `LoginCredentials`,
`SelfRegistration`) live in `core/model`.

## 6. Errors

`AuthRepository` methods return `NetworkResult<T, NetworkError>` unchanged from the Service on
the error branch — no re-wrapping, no swallowing. `currentSession` has no error channel (local
prefs read only); absence is represented as `null`, never an exception.

## 7. Testing

`commonTest` — `AuthRepositoryTest` (fake `CompanionAuthApi` + fake `CompanionSessionStore`;
≥3 cases per mutation method covering success + ≥2 distinct error branches, plus
`currentSession`/`clearSession` coverage); `InvitationRepositoryTest` (fake `InvitationApi`;
≥3 cases per method incl. `joinGroup`'s associate-then-mark-accepted call-order assertion, the
associate-failure short-circuit, and the mark-accepted-failure-is-non-fatal case);
`GroupCreateRepositoryTest` (fake `GroupCreateApi`; ≥5 cases per method incl. `getOffices`'
default-orderBy/empty-list/error-passthrough cases and `createGroup`'s
domain→DTO mapping assertion + validation/conflict/server-error passthrough cases).

## 8. Observability

Kermit tag `AuthRepository` — debug on submit, info on success (with `userId`), error on every
failure branch (mirrors the Service's own logging one layer down). Kermit tag
`InvitationRepository` — debug on each call start, info on success (incl.
`expired`/`alreadyUsed` flags on `validateCode` success, `resourceId` on `joinGroup` success),
error on every failure branch including the non-fatal mark-accepted failure (still logged, does
not fail the call). Kermit tag `GroupCreateRepository` — debug on each call start (incl.
`name`/`officeId` on `createGroup`), info on success (incl. `groupId`/office count), error on
every failure branch.

## 9. Evolution

Adding a new companion-backed mutation: extend `AuthRepository`/`AuthRepositoryImpl` (or
`InvitationRepository`/`InvitationRepositoryImpl`/`GroupCreateRepository`/
`GroupCreateRepositoryImpl`) following the same `when`-over-`NetworkResult` shape; if the new
feature's `business_logic.kind` is NOT `crud`/`nav_only`/`processor` (i.e. it declares a genuine
read-stream), route it through the Store5 path instead (SP-04) — do not retrofit this
Repository's mutation shape onto a read-stream feature. `GroupCreateRepository.getOffices` is
the concrete example of this boundary: once `kmp-store-gen` emits `core/store/OfficeStore.kt` +
registers `AppStoreRegistry.Office`, upgrade its body to `officeStore.asScreenStream(...)`
(mirroring `GroupTypeConfigRepositoryImpl`) instead of leaving the plain pass-through in place.
<!-- kmp-client-gen:END -->
