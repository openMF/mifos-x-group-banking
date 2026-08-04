/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.mapper

import kotlinx.serialization.json.Json
import org.mifos.groupbanking.core.model.CreateMemberRequest
import org.mifos.groupbanking.core.model.MemberCreationResult
import org.mifos.groupbanking.core.network.model.CreateMemberRequestDto
import org.mifos.groupbanking.core.network.model.CreateMemberResponseDto
import org.mifos.groupbanking.core.network.model.MemberAddOfflinePayloadDto
import org.mifos.groupbanking.core.network.model.UpdateMemberRoleRequestDto

/**
 * Domain <-> DTO mappers for the member-add create-chain
 * (`idea-layer/screens/member-add/api.yaml` — `create_client` -> `assign_member_role` -> optional
 * `upload_photo`). Every field on every DTO declared for this feature is mapped — no field left
 * unmapped.
 *
 * [toAssignMemberRoleRequestDto] reuses the SHARED `UpdateMemberRoleRequestDto` (declared in
 * `MemberProfileDto.kt`) — member-add's `assign_member_role` (POST) and member-profile's
 * `update_member_role` (PUT) target the LITERAL SAME `/datatables/dt_member_role/{clientId}`
 * endpoint with an identical body/response shape, so no duplicate DTO pair was introduced (see
 * `MemberAddDto.kt` kdoc). [MemberRole.toDto] is likewise reused from `MemberProfileMappers.kt`
 * (same package — not redeclared here).
 */

/**
 * Domain -> DTO for step 1 (`POST /clients`). [CreateMemberRequest.photoUri] is deliberately NOT
 * carried — the photo is uploaded separately in step 3 via a (DTO-less) multipart body, never
 * part of the `/clients` create payload. [locale]/[dateFormat] are Fineract API boilerplate
 * parameters with no domain-model counterpart (every client-create call sends the same
 * locale/date-format pair) — defaulted here rather than threaded through
 * `CreateMemberRequest`, which stays free of Fineract wire plumbing.
 */
fun CreateMemberRequest.toCreateMemberRequestDto(
    locale: String = "en",
    dateFormat: String = "dd MMMM yyyy",
): CreateMemberRequestDto = CreateMemberRequestDto(
    firstname = firstName,
    lastname = lastName,
    mobileNo = phone,
    active = true,
    activationDate = activationDate,
    officeId = officeId,
    groupId = groupId.toLong(),
    locale = locale,
    dateFormat = dateFormat,
)

/**
 * Domain -> DTO for step 2 (`POST /datatables/dt_member_role/{clientId}`), reusing the SHARED
 * `UpdateMemberRoleRequestDto`. Built from the SAME [CreateMemberRequest] used for step 1 —
 * [CreateMemberRequest.role] / [CreateMemberRequest.groupId] / [CreateMemberRequest.activationDate]
 * double as the role-assignment's `assignedDate`, matching
 * `api.yaml#api.assign_member_role.body` — no separate domain request type is needed.
 */
fun CreateMemberRequest.toAssignMemberRoleRequestDto(): UpdateMemberRoleRequestDto = UpdateMemberRoleRequestDto(
    role = role.toDto(),
    groupId = groupId.toLong(),
    assignedDate = activationDate,
)

/**
 * Server-parity Json config reused for the member-add SyncQueue payload (same private-to-file
 * precedent as `LoanRequestMappers.syncQueueJson`).
 */
private val memberAddSyncQueueJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/**
 * Serializes this [CreateMemberRequest] to the FLATTENED [MemberAddOfflinePayloadDto] JSON body the
 * companion orchestration route `POST /companion/members` expects, for
 * `SyncQueueRepository.enqueue(targetTable = "/companion/members")` when `cmp-network-monitor`
 * reports offline. Collapses the online two-call chain (create-client + assign-role) into one
 * queueable op that the companion (`HandleCreateMember`) replays server-side on drain — an offline
 * member-add is durably queued, not dropped (the prior offline branch surfaced a `Network` error
 * and lost it). `role` is the uppercase enum name (== [org.mifos.groupbanking.core.network.model.MemberRoleDto]
 * wire value); `assignedDate` reuses [CreateMemberRequest.activationDate], matching the online
 * `toAssignMemberRoleRequestDto`.
 */
fun CreateMemberRequest.toOfflinePayloadJson(
    locale: String = "en",
    dateFormat: String = "dd MMMM yyyy",
): String {
    val dto = MemberAddOfflinePayloadDto(
        firstname = firstName,
        lastname = lastName,
        mobileNo = phone,
        active = true,
        activationDate = activationDate,
        officeId = officeId,
        groupId = groupId.toLong(),
        locale = locale,
        dateFormat = dateFormat,
        role = role.name,
        assignedDate = activationDate,
    )
    return memberAddSyncQueueJson.encodeToString(MemberAddOfflinePayloadDto.serializer(), dto)
}

/**
 * Assembles the composite create-chain result from step 1's wire response plus the ORIGINAL
 * [request] (steps 2/3 echo no new business data beyond `resourceId`, already captured by
 * [photoUploaded]). [photoUploaded] is passed by the caller (repository layer) as
 * `uploadPhotoResponse != null` — photo capture is optional per `ui.yaml#actions.OnPhotoRemoved`.
 */
fun CreateMemberResponseDto.toDomainModel(
    request: CreateMemberRequest,
    photoUploaded: Boolean,
): MemberCreationResult = MemberCreationResult(
    memberId = clientId.toString(),
    fineractClientId = clientId,
    groupId = request.groupId,
    role = request.role,
    photoUploaded = photoUploaded,
)
