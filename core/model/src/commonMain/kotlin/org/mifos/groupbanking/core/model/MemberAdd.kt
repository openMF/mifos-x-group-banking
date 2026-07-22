/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.model

/**
 * Domain model for the member-add form's submission payload — pure business shape, no wire
 * concerns. Assembled by `MemberAddViewModel` from the validated form fields and submitted to
 * `MemberRepository.createMember`, which fans this single payload out across the create-chain
 * (`create_client` -> `assign_member_role` -> optional `upload_photo`).
 *
 * Field names mirror `idea-layer/screens/member-add/api.yaml#dtos.CreateMemberRequest` (the
 * registry's ABSTRACTED, app-facing shape) directly — not the raw per-operation Fineract wire
 * field names (`firstname`/`mobileNo`/...) that the chained wire DTOs
 * (`CreateMemberRequestDto`/reused `UpdateMemberRoleRequestDto`) actually carry over HTTP. See
 * `MemberAddMappers.kt` kdoc for the request-splitting rationale and `MemberAddDto.kt` kdoc for
 * why `groupId` is `String` here (matching `nav_params.groupId` / the abbreviated registry block)
 * but `Long` on the wire `CreateMemberRequestDto` (converted at the mapper boundary).
 *
 * [role] reuses the SHARED [MemberRole] enum (already used by member-list/member-profile) — the
 * registry's `dtos.MemberRole` block (`CHAIRPERSON`/`TREASURER`/`SECRETARY`/`MEMBER`) is an exact
 * value-set match, so no new enum was introduced.
 *
 * See API.md#models — CreateMemberRequest.
 */
data class CreateMemberRequest(
    val firstName: String,
    val lastName: String,
    val phone: String,
    val photoUri: String?,
    val role: MemberRole,
    val groupId: String,
    val officeId: Long,
    val activationDate: String,
)

/**
 * Domain model for the successful outcome of the member-add create-chain — the composite result
 * across all three chained calls. [memberId] is derived from [fineractClientId] (`.toString()`)
 * since the create-chain's wire responses carry no separate composite/display ID (unlike the
 * canonical [Member.id] `"MBR-..."` format, which this create response does not produce) — a
 * documented simplification, not a registry field.
 *
 * See API.md#models — MemberCreationResult.
 */
data class MemberCreationResult(
    val memberId: String,
    val fineractClientId: Long,
    val groupId: String,
    val role: MemberRole,
    val photoUploaded: Boolean,
)
