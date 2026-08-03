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

import kotlinx.datetime.Instant
import org.mifos.groupbanking.core.model.GroupPreview
import org.mifos.groupbanking.core.model.GroupRole
import org.mifos.groupbanking.core.model.Invitation
import org.mifos.groupbanking.core.model.InvitationAcceptance
import org.mifos.groupbanking.core.model.InvitationAcceptanceResult
import org.mifos.groupbanking.core.model.JoinGroupRequest
import org.mifos.groupbanking.core.model.JoinGroupResult
import org.mifos.groupbanking.core.network.model.AssociateClientsRequestDto
import org.mifos.groupbanking.core.network.model.AssociateClientsResponseDto
import org.mifos.groupbanking.core.network.model.GroupPreviewDto
import org.mifos.groupbanking.core.network.model.GroupRoleDto
import org.mifos.groupbanking.core.network.model.InvitationRowDto
import org.mifos.groupbanking.core.network.model.MarkAcceptedRequestDto
import org.mifos.groupbanking.core.network.model.MarkAcceptedResponseDto

/**
 * DTO <-> domain mappers for the join-with-code wire contracts (COMP-DT-004 invitations
 * datatable + COMP-GRP-003 associate-clients). Every field on every DTO declared in
 * `JoinWithCodeDto.kt` is mapped — no field left unmapped. `GroupRoleDto`/[GroupRole] and
 * `GroupTypeSlugDto`/`GroupTypeSlug` are REUSED from the login-signup + group-type-picker
 * contracts — this file does NOT redeclare `GroupRoleDto.toDomainModel()` (already declared in
 * `LoginSignupMappers.kt`, same package) or `GroupTypeSlugDto.toDomainModel()` (already declared
 * in `GroupTypeConfigMappers.kt`, same package); both are reused as-is (top-level functions in
 * the same `org.mifos.groupbanking.core.network.mapper` package are visible without import). This
 * file adds only the missing `GroupRole.toDto()` direction (needed for the outgoing
 * `AssociateClientsRequestDto`), which neither existing mapper file declares.
 */

// ---------- InvitationRowDto -> Invitation ----------

fun InvitationRowDto.toDomainModel(): Invitation = Invitation(
    token = token,
    groupId = groupId,
    inviterClientId = inviterClientId,
    invitedEmailPhone = invitedEmailPhone,
    roleToAssign = roleToAssign.toDomainModel(),
    expiresAt = Instant.parse(expiresAt),
    acceptedAt = acceptedAt?.let { Instant.parse(it) },
)

/** Domain -> wire direction for [GroupRole] — the DTO -> domain direction already exists as
 * `GroupRoleDto.toDomainModel()` in `LoginSignupMappers.kt` (same package, reused here). */
fun GroupRole.toDto(): GroupRoleDto = when (this) {
    GroupRole.ORGANIZER -> GroupRoleDto.ORGANIZER
    GroupRole.MEMBER -> GroupRoleDto.MEMBER
    GroupRole.TREASURER -> GroupRoleDto.TREASURER
    GroupRole.SECRETARY -> GroupRoleDto.SECRETARY
    GroupRole.UNKNOWN -> GroupRoleDto.UNKNOWN
}

// ---------- GroupPreviewDto -> GroupPreview ----------

fun GroupPreviewDto.toDomainModel(): GroupPreview = GroupPreview(
    groupId = groupId,
    groupName = groupName,
    groupType = groupType.toDomainModel(),
    organizerName = organizerName,
    memberCount = memberCount,
    officeId = officeId,
    roleToAssign = roleToAssign.toDomainModel(),
)

// `GroupTypeSlugDto.toDomainModel()` is reused as-is from `GroupTypeConfigMappers.kt` (same
// package) — this endpoint already emits the same long-form slugs as COMP-DT-003, so no adapter
// is needed here (contrast `GroupMappers.kt`'s `GroupTypeDto.toDomainModel()`, which DOES adapt
// because `GroupDto`'s short-form wire values differ from the long-form slug set).

// ---------- JoinGroupRequest <-> AssociateClientsRequestDto/ResponseDto ----------

fun JoinGroupRequest.toDto(): AssociateClientsRequestDto = AssociateClientsRequestDto(
    clientIds = clientIds,
    roleToAssign = roleToAssign.toDto(),
)

fun AssociateClientsRequestDto.toDomainModel(): JoinGroupRequest = JoinGroupRequest(
    clientIds = clientIds,
    roleToAssign = roleToAssign.toDomainModel(),
)

fun AssociateClientsResponseDto.toDomainModel(): JoinGroupResult = JoinGroupResult(
    resourceId = resourceId,
    groupId = groupId,
    clientIds = clientIds,
)

// ---------- InvitationAcceptance <-> MarkAcceptedRequestDto/ResponseDto ----------

fun InvitationAcceptance.toDto(): MarkAcceptedRequestDto = MarkAcceptedRequestDto(
    acceptedAt = acceptedAt.toString(),
)

fun MarkAcceptedRequestDto.toDomainModel(): InvitationAcceptance = InvitationAcceptance(
    acceptedAt = Instant.parse(acceptedAt),
)

/** Flattens the wire's nested `changes.accepted_at` into a single top-level domain field. */
fun MarkAcceptedResponseDto.toDomainModel(): InvitationAcceptanceResult = InvitationAcceptanceResult(
    resourceId = resourceId,
    acceptedAt = Instant.parse(changes.acceptedAt),
)
