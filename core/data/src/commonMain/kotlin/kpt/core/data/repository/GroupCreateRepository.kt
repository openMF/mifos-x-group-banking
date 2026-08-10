/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.data.repository

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.model.CreateGroupRequest
import kpt.core.model.GroupCreationResult
import kpt.core.model.Office

/**
 * Group-create wizard repository (COMP-GRP-001). Wraps `GroupCreateApi` (core/network).
 *
 * **Store5 branch (SP-04):** `group-create`'s `business_logic.kind` for [createGroup] is a
 * mutation orchestration flow (`processor`, `cache_strategy: no_cache` per
 * `data-flow.yaml#entries[trigger=on_action,action=OnSubmit].cache`) — there is no read-stream to
 * back with a Store5 cache, so per RULE-IMPLEMENT-STORE5-001 / RULE-IDEA-IMPL-INTELLIGENCE-001
 * this repository surfaces [NetworkResult] directly rather than `.asScreenStream()` /
 * `.asPagingScreenStream()` / `MutableStore.write(...)` — same branch as
 * [InvitationRepositoryImpl] / [AuthRepositoryImpl]. No `org.mobilenativefoundation.store` import
 * anywhere in this stack.
 *
 * **KNOWN SC2 GAP — [getOffices]:** `data-flow.yaml`'s `on_mount` entry DOES declare a genuine
 * read-stream cache strategy (`stale_while_revalidate`, `ttl_seconds=3600`, `offices_cache`
 * table, offline `show_cached`) — per RULE-IMPLEMENT-SCALE-CODEGEN-001 SC2 this SHOULD eventually
 * be a Store5-backed `.asScreenStream()` read (mirroring [GroupTypeConfigRepositoryImpl]'s exact
 * precedent for another single-key seed/catalogue read). No `OfficeStore` exists yet in
 * `AppStoreRegistry` — `core/store` is owned by the upstream `kmp-store-gen` generator, not by
 * this module (Hard Rule #6/module-ownership). [getOffices] is therefore, for now, a plain
 * pass-through mirroring [InvitationRepository]'s Store5-free shape — NOT half-built as a fake
 * Store5 stream. Upgrade path once `kmp-store-gen` emits `core/store/OfficeStore.kt` +
 * registers `AppStoreRegistry.Office`: replace this method's body with
 * `officeStore.asScreenStream(key = OFFICE_LIST_KEY, ...)`, same shape as
 * `GroupTypeConfigRepositoryImpl.groupTypeConfigsStream`.
 *
 * No try-catch anywhere in this repository (Mandatory Rule 4) — every implementation method is
 * a plain `when` over the service's sealed [NetworkResult]; `GroupCreateApiImpl` is the sole
 * layer allowed to catch exceptions.
 *
 * **Offline-queue wiring (data-flow.yaml `offline_queue`: table `sync_queue`, operation
 * `CREATE_GROUP_ORCHESTRATE`):** this repository does NOT embed the offline-queue mechanism
 * itself — per this project's own `core-base/store` convention (CLAUDE.md "MUTABLE" archetype
 * row: `DraftSubmitHandler` wraps a ViewModel-level `SubmitHandler`, not the Repository), the
 * group-create ViewModel (feature-layer, out of this generator's scope) wires
 * `viewModelScope.draftSubmitHandler<CreateGroupRequest, GroupCreationResult>(outbox = get(),
 * formKey = "group_create")` and calls `draftHandler.submit(request) { createGroup(it) }` around
 * [createGroup]. See this feature's final client-layer generation report for the two follow-ups
 * this needs (out of `core/data`+`core/network` ownership): (1) `CreateGroupRequest` marked
 * `@Serializable` in `core/model`; (2) an `OutboxQualifiers.GroupCreate` +
 * `RoomSubmitOutbox<CreateGroupRequest>` DI binding.
 *
 * See API.md#repositories — GroupCreateRepository.
 */
interface GroupCreateRepository {

    /**
     * Fetches every office for the wizard's office dropdown, ordered by [orderBy] (defaults to
     * `"name"`). See the SC2 gap note above — currently a direct pass-through of
     * `GroupCreateApi.getOffices`, not yet Store5-cached.
     */
    suspend fun getOffices(orderBy: String = "name"): NetworkResult<List<Office>, NetworkError>

    /**
     * Submits the group-create wizard's Step 4 payload (COMP-GRP-001). On success returns the
     * new group's identity + shareable invite code. See the offline-queue wiring note above —
     * the caller (ViewModel) is responsible for wrapping this call in a `DraftSubmitHandler` to
     * get offline-resilient behaviour; this method itself performs one unconditional network
     * attempt and surfaces whatever `GroupCreateApi.createGroup` returns.
     */
    suspend fun createGroup(request: CreateGroupRequest): NetworkResult<GroupCreationResult, NetworkError>

    /**
     * Durably enqueues [request] to the offline write-queue ([SyncQueueRepository]) when
     * `NetworkMonitor` reports offline, returning the generated queue row id. The row targets the
     * full companion route `/companion/groups` so the sync-drain replays it through the companion's
     * `/batches` self-dispatch back to `HandleCreateGroup` — an offline group-create is queued and
     * later drained, never silently dropped (the prior offline branch surfaced an error dialog and
     * lost the write). The ViewModel calls this in its pre-flight `!networkMonitor.isOnline.value`
     * branch instead of [createGroup].
     */
    suspend fun enqueueOffline(request: CreateGroupRequest): Long
}
