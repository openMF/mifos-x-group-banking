/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.data.repository

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import org.mifos.groupbanking.core.model.CreateMemberRequest
import org.mifos.groupbanking.core.model.MemberCreationResult

/**
 * Member-add create-chain mutation-orchestration repository (`idea-layer/screens/member-add`).
 * Wraps `MemberAddApi` (core/network).
 *
 * **Store5 branch (SP-04):** member-add's `business_logic.kind` is a mutation orchestration flow
 * (`processor`) — every endpoint in `api.yaml#api` is a write with an `offline_queue` block
 * (`sync_queue` table); there is no read-stream to back with a Store5 cache. Per
 * RULE-IMPLEMENT-STORE5-001 / RULE-IDEA-IMPL-INTELLIGENCE-001 this repository surfaces
 * [NetworkResult] directly rather than `.asScreenStream()` / `.asPagingScreenStream()` /
 * `MutableStore.write(...)` — same branch as [InvitationRepositoryImpl] / [GroupCreateRepositoryImpl].
 * No `org.mobilenativefoundation.store` import anywhere in this stack.
 *
 * No try-catch anywhere in this repository (Mandatory Rule 4) — [MemberAddRepositoryImpl] is a
 * plain `when` chain over the service's sealed [NetworkResult]; `MemberAddApiImpl` is the sole
 * layer allowed to catch exceptions.
 *
 * **Offline-queue seam (VM-layer, per project convention — same pattern as
 * `InvitationRepository` / `GroupCreateRepository`):** every endpoint declares
 * `api.yaml#api[].offline_queue` (`table: sync_queue`) and `data-flow.yaml#offline_behavior`
 * (`strategy: queue_for_sync`, `operations: [CREATE_MEMBER, ASSIGN_MEMBER_ROLE,
 * UPLOAD_MEMBER_PHOTO]`). This repository makes NO offline decisions itself — it is a pure
 * network passthrough. The ViewModel, informed by `NetworkMonitor`, is responsible for:
 * (a) pre-flight `SyncQueueRepository.enqueue(...)` when already offline (never calling
 * [createMember] at all), or (b) enqueueing a retry when a transport-level
 * [NetworkResult.Error] surfaces from a call that started online and lost connectivity
 * mid-chain. `SyncQueueRepository` itself lives outside this generation step (declared in
 * `api.yaml#dependencies.repositories`).
 *
 * See API.md#repositories — MemberAddRepository.
 */
interface MemberAddRepository {

    /**
     * Orchestrates the full member-add create-chain (`flow.yaml#on_submit`):
     * 1. `createClient` — creates the Fineract client from [request]. A failure here
     *    SHORT-CIRCUITS the whole call; [assignMemberRole]/[uploadMemberPhoto] on the wrapped
     *    `MemberAddApi` are never attempted, and the [NetworkResult.Error] is returned as-is.
     * 2. `assignMemberRole` — writes [request]'s role to the `dt_member_role` datatable for the
     *    newly created client. A failure here ALSO fails the overall call (the client now exists
     *    without a role — a documented partial-creation state the caller/ViewModel surfaces via
     *    `error_paths` and the offline-queue retry seam above); its [NetworkResult.Error] is
     *    returned as-is and the optional photo upload is never attempted.
     * 3. `uploadMemberPhoto` — ONLY attempted when [photoBytes] is non-null (mirrors
     *    `ui.yaml#actions.OnPhotoRemoved` — photo capture is optional). Failure here is
     *    BEST-EFFORT / non-fatal: per `api.yaml#api.upload_photo` being the last, optional step
     *    of the chain, a photo-upload failure does NOT fail [createMember] — the returned
     *    [NetworkResult.Success] still carries the [MemberCreationResult] from steps 1-2, with
     *    [MemberCreationResult.photoUploaded] set to `false`.
     *
     * The upload filename is derived from [CreateMemberRequest.photoUri]'s last path segment
     * (falling back to a generic `"member-photo.jpg"` when [request]'s `photoUri` is null/blank
     * despite [photoBytes] being supplied) — [photoBytes] itself is the platform-read file bytes
     * (commonMain-safe: no `java.io.File`/`NSData` crosses this boundary), captured by the
     * caller via `ImagePickerHelper` per `api.yaml#dependencies.services`.
     */
    suspend fun createMember(
        request: CreateMemberRequest,
        photoBytes: ByteArray? = null,
    ): NetworkResult<MemberCreationResult, NetworkError>
}
