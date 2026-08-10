/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.service.memberadd

import kpt.core.base.network.NetworkError
import kpt.core.base.network.NetworkResult
import kpt.core.network.model.CreateMemberRequestDto
import kpt.core.network.model.CreateMemberResponseDto
import kpt.core.network.model.UpdateMemberRoleRequestDto
import kpt.core.network.model.UpdateMemberRoleResponseDto
import kpt.core.network.model.UploadMemberPhotoResponseDto

/**
 * Ktor client for the member-add create-chain — three raw-Fineract writes
 * (`create_client` -> `assign_member_role` -> optional `upload_photo`). See
 * `idea-layer/screens/member-add/api.yaml#api` + API.md#services for the endpoint contract.
 *
 * Every method returns [NetworkResult] — never a raw [Result] envelope, never a thrown
 * exception (Mandatory Rule 2). `NetworkResult`/`NetworkError` are the framework's
 * `core-base/network` sealed types (consumed, never edited — Hard Rule #8). This Service is
 * SERVICE-ONLY — the create-chain orchestration (create -> assign role -> optional photo, with
 * non-fatal photo failure) is composed by
 * [kpt.core.data.repository.MemberAddRepositoryImpl], not declared here.
 *
 * All three endpoints declare `offline_queue` (`sync_queue` table,
 * `CREATE_MEMBER`/`ASSIGN_MEMBER_ROLE`/`UPLOAD_MEMBER_PHOTO` operations) per
 * `data-flow.yaml#entries[0].offline_behavior` (`strategy: queue_for_sync`). This Service makes
 * no offline decisions itself — a caller (ViewModel, informed by `NetworkMonitor`) is
 * responsible for pre-flight offline detection + `SyncQueueRepository.enqueue(...)`, or for
 * enqueueing a retry on a transport-level [NetworkResult.Error]. See
 * [kpt.core.data.repository.MemberAddRepository] KDoc for the full seam.
 */
interface MemberAddApi {

    /**
     * `POST /clients` (`api.yaml#api.create_client`). Creates the Fineract client backing the
     * new group member. [request] carries Fineract's own literal wire field names
     * (`firstname`/`lastname`/`mobileNo`/...). The returned [CreateMemberResponseDto.clientId]
     * is threaded into [assignMemberRole] and [uploadMemberPhoto] (both address
     * `/{clientId}` in their endpoint path). 400 -> [NetworkError.BAD_REQUEST] ("invalid fields
     * or duplicate phone").
     */
    suspend fun createClient(request: CreateMemberRequestDto): NetworkResult<CreateMemberResponseDto, NetworkError>

    /**
     * `POST /datatables/dt_member_role/{clientId}` (`api.yaml#api.assign_member_role`). Writes
     * the newly created member's role to the custom `dt_member_role` datatable. Reuses the
     * SHARED [UpdateMemberRoleRequestDto]/[UpdateMemberRoleResponseDto] pair (declared for
     * member-profile's `update_member_role`, same endpoint, byte-identical body/response shape —
     * see `MemberAddDto.kt` KDoc). [clientId] is the string form of step 1's
     * `CreateMemberResponseDto.clientId`. 400 -> [NetworkError.BAD_REQUEST] ("invalid role").
     */
    suspend fun assignMemberRole(
        clientId: String,
        request: UpdateMemberRoleRequestDto,
    ): NetworkResult<UpdateMemberRoleResponseDto, NetworkError>

    /**
     * `POST /clients/{clientId}/images` (`api.yaml#api.upload_photo`, `multipart/form-data`).
     * Uploads the optional member profile photo. commonMain-safe: takes raw [photoBytes]
     * (`ByteArray`) rather than a platform `File` — the platform layer (androidMain/iosMain)
     * reads the picked/captured URI into bytes before calling this method; no `java.io.File` /
     * `NSData` leaks into this interface. 400 -> [NetworkError.BAD_REQUEST] ("file too large,
     * max 2MB").
     */
    suspend fun uploadMemberPhoto(
        clientId: String,
        photoBytes: ByteArray,
        fileName: String,
    ): NetworkResult<UploadMemberPhotoResponseDto, NetworkError>
}
