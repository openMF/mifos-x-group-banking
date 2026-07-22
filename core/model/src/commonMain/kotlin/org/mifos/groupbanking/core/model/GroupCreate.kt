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

import kotlinx.serialization.Serializable

/**
 * Domain model for the group-create wizard's submission payload (COMP-GRP-001 — `POST
 * /companion/groups`) — pure business shape, no wire concerns. Assembled from the wizard's 4
 * steps (identity, meeting schedule, type-adaptive rules, review) and submitted as one
 * companion-API orchestration call.
 *
 * `@Serializable` (added by `kmp-viewmodel-gen`, group-create feature generation): required so a
 * future `SubmitOutbox<CreateGroupRequest>` / `RoomSubmitOutbox` / `DraftSubmitHandler` binding
 * (offline-queue upgrade path, see `GroupCreateRepository` KDoc "Offline-queue wiring" note) can
 * persist this payload. No DI binding for that outbox exists yet — out of `feature/group-create`
 * module ownership (core/data) — `GroupCreateViewModel` currently falls back to a plain
 * network-attempt + `ShowOfflineSyncDialog` on failure rather than a half-built outbox wiring.
 *
 * See API.md#models — CreateGroupRequest.
 */
@Serializable
data class CreateGroupRequest(
    val name: String,
    val officeId: Long,
    val userId: Long,
    val currency: String,
    val meetingDay: String,
    val meetingTime: String,
    val typeConfig: CreateGroupTypeConfig,
)

/**
 * Domain model for the type-adaptive rule set collected by the group-create wizard — forwarded
 * to provision the `group_type_config` datatable row for the new group. [groupType] and
 * [poolModel] reuse the SHARED [GroupTypeSlug] / [SavingsMechanism] domain enums (already used by
 * [GroupTypeConfig], the group-type-picker catalogue model) rather than introducing duplicates —
 * their value-sets are identical for this purpose. [contributionModel], [shareoutFormula], and
 * [payoutOrderMethod] are feature-local enums with no existing domain counterpart (see
 * `GroupCreateMappers.kt` kdoc for why [ContributionMode] was not reused).
 *
 * `@Serializable` — see [CreateGroupRequest] KDoc (same offline-outbox forward-compat rationale).
 *
 * See API.md#models — CreateGroupTypeConfig.
 */
@Serializable
data class CreateGroupTypeConfig(
    val groupType: GroupTypeSlug,
    val poolModel: SavingsMechanism,
    val contributionModel: ContributionModel,
    val shareoutFormula: ShareoutFormula,
    val payoutOrderMethod: PayoutOrderMethod,
    val shareValue: Double,
    val contributionAmount: Double,
    val socialFundEnabled: Boolean,
    val socialFundPercent: Double,
    val cycleLengthMonths: Int,
    val loanMultiplier: Double,
    val interestRate: Double,
    val fineAmount: Double,
    val maxMembers: Int,
)

/**
 * Domain model for the successful outcome of a group-create submission — the new group's
 * identity plus the shareable invite code presented on the wizard's success screen.
 *
 * See API.md#models — GroupCreationResult.
 */
data class GroupCreationResult(
    val groupId: String,
    val fineractCenterId: Long,
    val inviteCode: String,
)

/**
 * Domain model for a single office row, used by the group-create wizard's office dropdown.
 * [externalId] is nullable — not every office carries one (see `GroupCreateDto.kt#OfficeDto`
 * kdoc for the registry-vs-operation-schema gap this reflects).
 *
 * See API.md#models — Office.
 */
data class Office(
    val id: Long,
    val name: String,
    val nameDecorated: String,
    val externalId: String?,
)

/**
 * Domain enum mirroring the wire `ContributionModelDto` one-to-one (pure Kotlin — no
 * `@Serializable`). Distinct from [ContributionMode] (the catalogue axis on [GroupTypeConfig]) —
 * see `GroupCreateMappers.kt` kdoc for the reuse-vs-new-enum rationale. [UNKNOWN] absorbs any
 * wire value this client build does not yet recognize.
 * See API.md#models — ContributionModel.
 */
enum class ContributionModel {
    FIXED_AMOUNT,
    SHARE_BASED_VARIABLE,
    FIXED_NEGOTIATED,
    UNKNOWN,
}

/**
 * Domain enum mirroring the wire `ShareoutFormulaDto` one-to-one — how the accumulated corpus is
 * distributed at cycle-end. [UNKNOWN] absorbs any wire value this client build does not yet
 * recognize.
 * See API.md#models — ShareoutFormula.
 */
enum class ShareoutFormula {
    NONE,
    PRORATA_SHARES,
    PRORATA_SAVINGS,
    EQUAL,
    INVESTMENT_PROPORTIONAL,
    UNKNOWN,
}

/**
 * Domain enum mirroring the wire `PayoutOrderMethodDto` one-to-one — how a lending-enabled group
 * decides who receives the rotating pot / loan next. [UNKNOWN] absorbs any wire value this
 * client build does not yet recognize.
 * See API.md#models — PayoutOrderMethod.
 */
enum class PayoutOrderMethod {
    FIXED_ORDER,
    LOTTERY,
    AUCTION,
    NEED_BASED,
    NA,
    UNKNOWN,
}
