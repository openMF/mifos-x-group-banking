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
import org.mifos.groupbanking.core.model.CreateInviteRequest
import org.mifos.groupbanking.core.model.GeneratedInvite
import org.mifos.groupbanking.core.model.MemberRole
import org.mifos.groupbanking.core.model.PendingInvite
import org.mifos.groupbanking.core.network.model.CreateInviteRequestDto
import org.mifos.groupbanking.core.network.model.GeneratedInviteDto
import org.mifos.groupbanking.core.network.model.PendingInviteDto

/**
 * Wire<->domain mappers for the member-invite (organizer-side) client stack. `role_to_assign`
 * round-trips as a LOWERCASE wire string (`member`/`treasurer`/`secretary`/`chairperson`, per
 * `api.yaml#api.create_invite.body.role_to_assign.enum`) — NOT an uppercase enum `@SerialName` —
 * so the string<->[MemberRole] conversion lives HERE at the mapper boundary rather than on the
 * DTO. Any unrecognized wire value degrades to [MemberRole.MEMBER] (the safest least-privilege
 * default), same tolerant-fallback convention as the `.UNKNOWN` branches in the other DTO enums.
 *
 * See API.md#mappers — MemberInviteMappers.
 */

/** Domain [MemberRole] -> lowercase wire `role_to_assign` string. */
internal fun MemberRole.toInviteWireRole(): String = when (this) {
    MemberRole.CHAIRPERSON -> "chairperson"
    MemberRole.TREASURER -> "treasurer"
    MemberRole.SECRETARY -> "secretary"
    MemberRole.MEMBER -> "member"
    MemberRole.UNKNOWN -> "member"
}

/** Lowercase wire `role_to_assign` string -> domain [MemberRole]; unknown values degrade to MEMBER. */
internal fun String.toMemberRole(): MemberRole = when (lowercase()) {
    "chairperson" -> MemberRole.CHAIRPERSON
    "treasurer" -> MemberRole.TREASURER
    "secretary" -> MemberRole.SECRETARY
    "member" -> MemberRole.MEMBER
    else -> MemberRole.MEMBER
}

/** [CreateInviteRequest] -> wire body DTO (the `groupId` is a path param, not a body field). */
fun CreateInviteRequest.toDto(): CreateInviteRequestDto = CreateInviteRequestDto(
    invitedEmailPhone = invitedEmailPhone,
    roleToAssign = roleToAssign.toInviteWireRole(),
    expiresAt = expiresAt,
)

/**
 * Server-parity Json config reused for the member-invite SyncQueue payload round-trip (same
 * private-to-file precedent as `LoanRequestMappers.syncQueueJson` / `GroupCreateMappers`).
 */
private val memberInviteSyncQueueJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/**
 * Serializes this [CreateInviteRequestDto] to the exact JSON body the ONLINE
 * `MemberInviteApi.createInvite` POSTs to `/companion/datatables/invitations/{groupId}`, for
 * `SyncQueueRepository.enqueue(targetTable = "/companion/datatables/invitations/{groupId}")` when
 * `cmp-network-monitor` reports offline. The queued row replays through the companion `/batches`
 * self-dispatch back to `HandleGenerateInvite` — an offline invite is durably queued, not dropped.
 * (`groupId` is a path param, carried in the targetTable route — not this body.)
 */
fun CreateInviteRequestDto.toJsonPayload(): String =
    memberInviteSyncQueueJson.encodeToString(CreateInviteRequestDto.serializer(), this)

/** Wire generate-invite response -> domain [GeneratedInvite]. */
fun GeneratedInviteDto.toDomainModel(): GeneratedInvite = GeneratedInvite(
    token = token,
    inviteLink = inviteLink,
    rowId = rowId,
)

/** Wire pending-invite row -> domain [PendingInvite]. */
fun PendingInviteDto.toDomainModel(): PendingInvite = PendingInvite(
    rowId = rowId,
    token = token,
    invitedEmailPhone = invitedEmailPhone,
    roleToAssign = roleToAssign.toMemberRole(),
    expiresAt = expiresAt,
    acceptedAt = acceptedAt,
)
