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
- `LoanApplyRepository` / `LoanApplyRepositoryImpl` — loan-apply form repository
  (`getGroupMembers` standalone read, `loadTemplate` 5-way parallel combine into
  `LoanApplyTemplate`, `applyLoan` submit). See API.md#repositories.
- `SyncQueueRepository` / `SyncQueueRepositoryImpl` — the SHARED offline write-queue seam
  (`observePending`/`observeCounts`/`observePendingByType`/`observeFailed`/
  `observeConflictCount`/`getItem` reads; `enqueue`/`markSyncing`/`markSynced`/`markFailed`/
  `retryAll` writes), Room-backed (`sync_queue` table), consumed by member-add/loan-request
  (enqueue) and sync-status (observe/retry). See API.md#repositories.
- `SyncManager` / `SyncManagerImpl` — the sync-status feature's `batch_sync` drain coordinator
  (`triggerSync`/`getLastSyncAt`/`retryItem`), wraps `BatchSyncApi` + `SyncQueueRepository` +
  `SyncMetadataStore`. See API.md#repositories.
- `ChangePinRepository` / `ChangePinRepositoryImpl` — the settings screen's change-PIN mutation
  repository (`changePin`, wraps `ChangePinApi` directly, no read-stream). See
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
losing it — see API.md's offline-queue note. The loan-apply ViewModel injects
`LoanApplyRepository` directly — `getGroupMembers` on-mount alongside the group-scoped inputs
`loadTemplate` needs, `loadTemplate` fired once a member AND product are both selected
(`OnMemberSelected`/`OnProductSelected`), `applyLoan` on `OnSubmit` (blocked while offline —
`NetworkMonitor`-gated at the ViewModel layer, no offline queue for loan submissions per this
feature's risk policy). The sync-status ViewModel injects `SyncQueueRepository` directly for its
on-mount/on-refresh local reads (`observePending`/`observePendingByType`/`observeFailed`/
`observeConflictCount` — no network call, per `data-flow.yaml#cache.strategy: no_cache`) and
`SyncManager` for `OnSyncNow` (`triggerSync`, gated on `NetworkMonitor.isOnline && !isSyncing`
at the ViewModel layer) / `OnRetryOperation` (`retryItem(itemId)`). The settings ViewModel
injects `ChangePinRepository` directly — `changePin` on the change-PIN dialog's submit action
(`OnSubmitPinChange`); `AuthRepository` is a separate injection for session-level concerns and is
never wrapped/extended by `ChangePinRepository`.

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
- `LoanApplyRepository` is Store5-free TODAY even though `business_logic.kind: composite` —
  no `AppStoreRegistry.LoanApply` entry exists yet (SP-03 `kmp-store-gen` has not run for this
  feature). No try-catch — `loadTemplate` is a `coroutineScope`+`async` 5-way parallel combine
  with a plain sequential Error-check (no `.catch{}` swallow); `getGroupMembers`/`applyLoan` are
  plain `when` chains over `LoanApplyApi`'s `NetworkResult`.
- `SyncManager`/`SyncQueueRepository` are Store5-free **BY DESIGN**, not a pending-migration
  gap like `GroupCreateRepository.getOffices` above — `sync-status`'s `data-flow.yaml` declares
  `cache.strategy: no_cache` on every entry (a direct local-read / direct-submit shape, never a
  cached network projection), so there is no `AppStoreRegistry` entry and no
  `core/store/SyncStatusStore.kt` to wrap (RULE-IMPLEMENT-STORE5-001 §7). No try-catch in either
  — `SyncQueueRepositoryImpl` is plain Room delegation (local writes never throw for this
  schema); `SyncManagerImpl` only branches on `BatchSyncApi`'s `NetworkResult`, never catches.
- `ChangePinRepository` is on the legacy/mutation path — settings' `business_logic.kind: crud`
  (RULE-IDEA-IMPL-INTELLIGENCE-001 AC-03i zero-regression), Store5-free (a pure fire-and-forget
  write, no cached entity to back with `.asScreenStream()`). No try-catch —
  `ChangePinRepositoryImpl.changePin` is a plain `when` over `ChangePinApi`'s `NetworkResult`;
  `ChangePinApiImpl` (core/network) is the sole layer allowed to catch exceptions. Deliberately
  separate from `AuthRepository` (not extended/wrapped).

## 5. Data

`AuthRepository` composes: `CompanionAuthApi` (core/network, wire calls) →
`LoginSignupMappers` (DTO→domain) → `CompanionSessionStore` (core/datastore, token
persistence). Domain types (`AuthSession`, `UserProfile`, `LoginCredentials`,
`SelfRegistration`) live in `core/model`.

## 6. Errors

`AuthRepository` methods return `NetworkResult<T, NetworkError>` unchanged from the Service on
the error branch — no re-wrapping, no swallowing. `currentSession` has no error channel (local
prefs read only); absence is represented as `null`, never an exception. `ChangePinRepository`
likewise returns `NetworkResult<ChangePinResult, NetworkError>` unchanged from `ChangePinApi` on
the error branch — no re-wrapping, no swallowing.

## 7. Testing

`commonTest` — `AuthRepositoryTest` (fake `CompanionAuthApi` + fake `CompanionSessionStore`;
≥3 cases per mutation method covering success + ≥2 distinct error branches, plus
`currentSession`/`clearSession` coverage); `InvitationRepositoryTest` (fake `InvitationApi`;
≥3 cases per method incl. `joinGroup`'s associate-then-mark-accepted call-order assertion, the
associate-failure short-circuit, and the mark-accepted-failure-is-non-fatal case);
`GroupCreateRepositoryTest` (fake `GroupCreateApi`; ≥5 cases per method incl. `getOffices`'
default-orderBy/empty-list/error-passthrough cases and `createGroup`'s
domain→DTO mapping assertion + validation/conflict/server-error passthrough cases);
`LoanApplyRepositoryTest` (fake `LoanApplyApi`; 12 cases, all green — `getGroupMembers`
success/404/empty, `loadTemplate`'s all-5-succeed derived-eligibility assertion plus one
short-circuit case per read (products/template/savings/corpus/config each independently
verified to propagate its error), `applyLoan` success/400/500 incl. the
domain→DTO purpose-id mapping assertion); `SyncQueueRepositoryTest` (in-memory `SyncQueueDao`
fake; enqueue/observePending/observeCounts/markSyncing/markFailed+retryAll coverage plus the
`observePendingByType`/`observeFailed`/`observeConflictCount`/`getItem` extension methods);
`SyncManagerTest` (fake `SyncQueueRepository` + fake `BatchSyncApi` + fake `SyncMetadataStore`;
covers `triggerSync`'s empty-backlog short-circuit, mark-syncing-before-submit ordering,
all-success + mixed-200/409/500 + network-error drain paths (incl. `lastSyncAt` stamped only on
a successful drain), `getLastSyncAt` delegation, and `retryItem`'s success/failure/network-error/
item-not-found cases); `ChangePinRepositoryTest` (uniquely-named `FakeChangePinApi`, file-private
— avoids the K2 same-package private-declaration collision `RepositoryTestFakes.kt` documents;
5 cases — success mapping, new-PIN-threaded-into-both-wire-fields assertion, and 400/401/500
error-passthrough).

## 8. Observability

Kermit tag `AuthRepository` — debug on submit, info on success (with `userId`), error on every
failure branch (mirrors the Service's own logging one layer down). Kermit tag
`InvitationRepository` — debug on each call start, info on success (incl.
`expired`/`alreadyUsed` flags on `validateCode` success, `resourceId` on `joinGroup` success),
error on every failure branch including the non-fatal mark-accepted failure (still logged, does
not fail the call). Kermit tag `GroupCreateRepository` — debug on each call start (incl.
`name`/`officeId` on `createGroup`), info on success (incl. `groupId`/office count), error on
every failure branch. Kermit tag `LoanApplyRepository` — debug on each call start (incl.
`groupId`/`clientId`/`productId` on `loadTemplate`), info on success (incl. `maxEligibleAmount`
on `loadTemplate`, `loanId` on `applyLoan`), error on the specific read that failed within the
5-way `loadTemplate` combine (not a generic "combine failed" message). Kermit tag `SyncManager`
— debug on drain start (pending-row count) and per-row retry, info on drain completion (folded
success/failed/conflict counts) and per-item retry outcome, error on the specific failed
row's/transport failure's status code (not a generic "sync failed" message); no-op retries
(item not found) are info-logged, not silently dropped. Kermit tag `ChangePinRepository` — debug
on submit, info on success (with `resourceId`), error on every failure branch (mirrors the
Service's own logging one layer down).

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
`LoanApplyRepository.loadTemplate` is the SAME upgrade-pending shape for a `composite`
`business_logic.kind` one-shot form-prefill combine (as opposed to `getOffices`' simple
pass-through) — once `kmp-store-gen` emits a composite `LoanApplyStore` (mirroring
`GroupDashboardStore`/`MemberProfileStore`'s dynamic-key `NETWORK_WITH_CACHE` pattern) and
registers `AppStoreRegistry.LoanApply`, upgrade `loadTemplate`'s body to
`loanApplyStore.asScreenStream(key)` and move the 5-way parallel-combine logic into that store's
fetcher. `SyncManager`/`SyncQueueRepository` are NOT an upgrade-pending pair — do not migrate
them to Store5 when touching this feature again; `data-flow.yaml#cache.strategy: no_cache` is
the declared terminal shape (RULE-IMPLEMENT-STORE5-001 §7), same rationale as
`InvitationRepository`. Adding a new offline-queue mutation feature: enqueue into the EXISTING
`SyncQueueRepository` (never a new per-feature queue table) with a new `operationType` string,
and extend `SyncClassifier.kt`'s `operationTypeToEntityType`/`operationTypeToSyncOperation`
mapping tables (`core/model`) so the sync-status screen's per-entity badges classify it
correctly.
<!-- kmp-client-gen:END -->
