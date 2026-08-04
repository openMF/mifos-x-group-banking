/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire request DTO for step 1 of the member-add create-chain — `POST /clients`
 * (`idea-layer/screens/member-add/api.yaml#api.create_client`). Field `@SerialName`s are
 * Fineract's OWN literal `/clients` resource field names (`mobileNo`/`dateFormat` are
 * camelCase — Fineract's own API convention, not a project snake_case datatable column; Hard
 * Rule 5 "match exactly" applied here to the operation's literal wire names).
 *
 * **Registry divergence (flagged for the cross-feature repair station, same "abbreviated dtos
 * block vs operation-level body" pattern documented on `OfficeDto`/`CreateGroupTypeConfigDto`):**
 * `api.yaml#dtos.CreateMemberRequest` declares an ABSTRACTED, app-facing shape (`firstName`,
 * `lastName`, `phone`, `photoUri`, `role`, `groupId: String`, `officeId`, `activationDate`) that
 * does NOT correspond 1:1 to any single wire call in this create-chain — it mirrors the DOMAIN
 * model `core.model.CreateMemberRequest` instead (see `MemberAdd.kt` kdoc). This DTO instead
 * mirrors `api.yaml#api.create_client.body` (the literal wire body for step 1), which is the
 * authoritative source for what actually round-trips over HTTP. Note also `groupId` is `Long`
 * here (per the operation body) vs `String` in the abbreviated dtos block / domain model —
 * converted at the mapper boundary (`MemberAddMappers.kt`).
 *
 * See API.md#dtos — CreateMemberRequest.
 */
@Serializable
data class CreateMemberRequestDto(
    @SerialName("firstname") val firstname: String,
    @SerialName("lastname") val lastname: String,
    @SerialName("mobileNo") val mobileNo: String,
    @SerialName("active") val active: Boolean,
    @SerialName("activationDate") val activationDate: String,
    @SerialName("officeId") val officeId: Long,
    @SerialName("groupId") val groupId: Long,
    @SerialName("locale") val locale: String,
    @SerialName("dateFormat") val dateFormat: String,
) {
    companion object {
        /** Bumped when this request shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * FLATTENED offline-sync payload for the member-add create-chain — the fields of
 * [CreateMemberRequestDto] plus the `role` + `assignedDate` that the (post-drain) role write needs.
 * The ONLINE path chains two client-side calls (`POST /clients` -> `POST /datatables/dt_member_role/
 * {clientId}`), which cannot be a single SyncQueue row because the role write depends on the
 * clientId minted by the first call. So the OFFLINE path enqueues THIS single shape to the
 * companion orchestration route `POST /companion/members` (`HandleCreateMember`), which performs
 * the whole chain server-side on drain. `role` is the uppercase wire enum (`CHAIRPERSON`/…/`MEMBER`,
 * matching [MemberRoleDto]'s `@SerialName`s).
 */
@Serializable
data class MemberAddOfflinePayloadDto(
    @SerialName("firstname") val firstname: String,
    @SerialName("lastname") val lastname: String,
    @SerialName("mobileNo") val mobileNo: String,
    @SerialName("active") val active: Boolean,
    @SerialName("activationDate") val activationDate: String,
    @SerialName("officeId") val officeId: Long,
    @SerialName("groupId") val groupId: Long,
    @SerialName("locale") val locale: String,
    @SerialName("dateFormat") val dateFormat: String,
    @SerialName("role") val role: String,
    @SerialName("assignedDate") val assignedDate: String,
)

/**
 * Wire response DTO for step 1 (`POST /clients`) — the newly created Fineract client's identity.
 * [resourceId] and [clientId] carry the same value on `/clients` create responses (Fineract
 * convention); both are declared per `api.yaml#api.create_client.response.fields` — no field
 * left unmapped. [clientId] is the value threaded into steps 2 (`assign_member_role`) and 3
 * (`upload_photo`), both of which address `/{clientId}` in their endpoint path.
 *
 * See API.md#dtos — CreateMemberResponse.
 */
@Serializable
data class CreateMemberResponseDto(
    @SerialName("resourceId") val resourceId: Long,
    @SerialName("clientId") val clientId: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire response DTO for step 3 (optional) — `POST /clients/{clientId}/images`
 * (`api.yaml#api.upload_photo.response`).
 *
 * **No request DTO is generated for this step** — `body_type: multipart/form-data` with a raw
 * `file: File` field is NOT expressible as a `kotlinx.serialization` JSON DTO; the multipart body
 * is constructed directly in the API service implementation (`MultiPartFormDataContent` over the
 * platform file bytes from filekit-core's captured/selected `photoUri`), never round-tripped as
 * JSON. Flagged as an intentional generation gap — see the parent generation report.
 *
 * See API.md#dtos — UploadMemberPhotoResponse.
 */
@Serializable
data class UploadMemberPhotoResponseDto(
    @SerialName("resourceId") val resourceId: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

// ---------------------------------------------------------------------------------------------
// Step 2 (assign_member_role, POST /datatables/dt_member_role/{clientId}) is DELIBERATELY NOT
// re-declared here. Its body (`role`/`groupId`/`assignedDate`) and response (`resourceId`) are
// BYTE-IDENTICAL to member-profile's `update_member_role` (PUT, same endpoint) — see
// `UpdateMemberRoleRequestDto`/`UpdateMemberRoleResponseDto` in `MemberProfileDto.kt`. Both
// features write the literal same `/datatables/dt_member_role/{clientId}` Fineract datatable
// row, so member-add's create-chain reuses those two DTOs directly instead of introducing a
// byte-for-byte duplicate pair (same precedent as `MemberRoleInfoDto.role` reusing the SHARED
// `MemberRoleDto` rather than a feature-local clone). [MemberRoleDto] (`MemberDto.kt`) is reused
// unchanged for the `role` field itself, per the generation brief.
// ---------------------------------------------------------------------------------------------
