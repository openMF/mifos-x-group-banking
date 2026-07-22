/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package org.mifos.groupbanking.core.network.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire request DTO for the group-create wizard's single companion-API orchestration call —
 * `POST /companion/groups` (COMP-GRP-001). One call orchestrates: (1) createCenter in Fineract,
 * (2) activate group, (3) associateClients, (4) assignRole (organizer for the creator), (5)
 * provision the `group_type_config` datatable row from [typeConfig]. Replaces the previous raw
 * Fineract `/centers` + `/datatables/dt_group_config` two-step. Top-level fields are camelCase
 * (companion-bridge convention, like `GroupDto`/`AuthResponseDto`) — [typeConfig] itself is the
 * raw `group_type_config` datatable payload and uses snake_case `@SerialName`s (see
 * [CreateGroupTypeConfigDto]).
 *
 * See API.md#dtos — CreateGroupOrchestrationRequest.
 */
@Serializable
data class CreateGroupRequestDto(
    @SerialName("name") val name: String,
    @SerialName("officeId") val officeId: Long,
    @SerialName("userId") val userId: Long,
    @SerialName("currency") val currency: String,
    @SerialName("meetingDay") val meetingDay: String,
    @SerialName("meetingTime") val meetingTime: String,
    @SerialName("typeConfig") val typeConfig: CreateGroupTypeConfigDto,
) {
    companion object {
        /** Bumped when this request shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire response DTO for COMP-GRP-001 — the newly created group's identity plus the shareable
 * invite code for onboarding new members.
 *
 * See API.md#dtos — CreateGroupOrchestrationResponse.
 */
@Serializable
data class CreateGroupResponseDto(
    @SerialName("groupId") val groupId: String,
    @SerialName("fineractCenterId") val fineractCenterId: Long,
    @SerialName("inviteCode") val inviteCode: String,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for the `typeConfig` payload nested inside [CreateGroupRequestDto] — the
 * type-adaptive rule set the group-create wizard collects across its 4 steps, forwarded
 * verbatim to provision the `group_type_config` Fineract datatable row (COMP-GRP-001 step 5).
 * Field `@SerialName`s are RAW snake_case datatable column names (Hard Rule 5 — must match
 * exactly), matching the convention already used for other raw-datatable DTOs in this module
 * (see `InvitationRowDto`/`MarkAcceptedRequestDto` in `JoinWithCodeDto.kt`).
 *
 * [groupType] reuses [GroupTypeDto] (short-form wire values `VSLA`/`CBO`/`BURIAL`/... from
 * `GroupDto.kt`), NOT the long-form [GroupTypeSlugDto] used by the group-type-picker catalogue
 * (`GroupTypeConfigDto.kt`) — the create-wizard's `typeConfig.group_type` wire value-set in
 * `idea-layer/screens/group-create/api.yaml` is the SHORT form, so [GroupTypeDto] is the correct
 * reuse (its existing `toDomainModel()` mapper already adapts both short- and long-form wire
 * enums onto the single shared `core.model.GroupTypeSlug`).
 *
 * [poolModel] reuses [SavingsMechanismDto] as-is — its value-set (`ACCUMULATING`/
 * `ROTATING_PAYOUT`/`NONE`) is identical to `typeConfig.pool_model`'s declared values, so no new
 * enum was introduced.
 *
 * [contributionModel] is a NEW enum ([ContributionModelDto]) — its wire value-set
 * (`FIXED_AMOUNT`/`SHARE_BASED_VARIABLE`/`FIXED_NEGOTIATED`) differs from the existing
 * [ContributionModeDto] (`SHARE_BASED_VARIABLE`/`FIXED`/`MINIMAL`), so it was NOT reused — same
 * precedent as `GroupTypeDto` vs `GroupTypeSlugDto` coexisting for differing wire endpoints.
 *
 * [shareoutFormula] and [payoutOrderMethod] are also NEW enums — no existing wire enum in this
 * module covers either axis.
 *
 * See API.md#dtos — CreateGroupOrchestrationRequest.typeConfig (mirrors
 * `idea-layer/screens/group-create/api.yaml#dtos.GroupTypeConfig`).
 */
@Serializable
data class CreateGroupTypeConfigDto(
    @SerialName("group_type") val groupType: GroupTypeDto = GroupTypeDto.UNKNOWN,
    @SerialName("pool_model") val poolModel: SavingsMechanismDto = SavingsMechanismDto.UNKNOWN,
    @SerialName("contribution_model") val contributionModel: ContributionModelDto = ContributionModelDto.UNKNOWN,
    @SerialName("shareout_formula") val shareoutFormula: ShareoutFormulaDto = ShareoutFormulaDto.UNKNOWN,
    @SerialName("payout_order_method") val payoutOrderMethod: PayoutOrderMethodDto = PayoutOrderMethodDto.UNKNOWN,
    @SerialName("share_value") val shareValue: Double,
    @SerialName("contribution_amount") val contributionAmount: Double,
    @SerialName("social_fund_enabled") val socialFundEnabled: Boolean,
    @SerialName("social_fund_percent") val socialFundPercent: Double,
    @SerialName("cycle_length_months") val cycleLengthMonths: Int,
    @SerialName("loan_multiplier") val loanMultiplier: Double,
    @SerialName("interest_rate") val interestRate: Double,
    @SerialName("fine_amount") val fineAmount: Double,
    @SerialName("max_members") val maxMembers: Int,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for a single office row — `GET /offices` (office dropdown, `orderBy=name` default).
 * Cached SWR (`ttl=3600`, `offline=show_cached`). [externalId] is nullable/optional: the
 * operation's response schema declares it, but the abbreviated
 * `idea-layer/screens/group-create/api.yaml#dtos.Office` registry block omits it — treated as
 * optional per Hard Rule 4 (never invent a field as non-null-required beyond what the narrower of
 * the two declarations supports); flagged for the cross-feature repair station to reconcile the
 * two declarations upstream.
 *
 * See API.md#dtos — Office.
 */
@Serializable
data class OfficeDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("nameDecorated") val nameDecorated: String,
    @SerialName("externalId") val externalId: String? = null,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire enum for the group-create wizard's contribution-model axis
 * (`typeConfig.contribution_model`). Distinct from [ContributionModeDto] (the catalogue axis on
 * `GroupTypeConfigDto`) — see [CreateGroupTypeConfigDto] kdoc for the reuse-vs-new-enum
 * rationale. [UNKNOWN] fallback per T7/EC30 so a server-added value never crashes an old client.
 */
@Serializable(with = ContributionModelDto.Serializer::class)
enum class ContributionModelDto {
    @SerialName("FIXED_AMOUNT") FIXED_AMOUNT,
    @SerialName("SHARE_BASED_VARIABLE") SHARE_BASED_VARIABLE,
    @SerialName("FIXED_NEGOTIATED") FIXED_NEGOTIATED,
    @SerialName("UNKNOWN") UNKNOWN,
    ;

    internal object Serializer : KSerializer<ContributionModelDto> by unknownFallbackEnumSerializer(
        "ContributionModelDto", entries, UNKNOWN,
    )
}

/**
 * Wire enum for the group-create wizard's share-out formula axis
 * (`typeConfig.shareout_formula`) — how the accumulated corpus is distributed at cycle-end.
 * [UNKNOWN] fallback per T7/EC30.
 */
@Serializable
enum class ShareoutFormulaDto {
    @SerialName("NONE") NONE,
    @SerialName("PRORATA_SHARES") PRORATA_SHARES,
    @SerialName("PRORATA_SAVINGS") PRORATA_SAVINGS,
    @SerialName("EQUAL") EQUAL,
    @SerialName("INVESTMENT_PROPORTIONAL") INVESTMENT_PROPORTIONAL,
    @SerialName("UNKNOWN") UNKNOWN,
}

/**
 * Wire enum for the group-create wizard's loan payout-order axis
 * (`typeConfig.payout_order_method`) — how a lending-enabled group decides who receives the
 * rotating pot / loan next. [UNKNOWN] fallback per T7/EC30.
 */
@Serializable(with = PayoutOrderMethodDto.Serializer::class)
enum class PayoutOrderMethodDto {
    @SerialName("FIXED_ORDER") FIXED_ORDER,
    @SerialName("LOTTERY") LOTTERY,
    @SerialName("AUCTION") AUCTION,
    @SerialName("NEED_BASED") NEED_BASED,
    @SerialName("NA") NA,
    @SerialName("UNKNOWN") UNKNOWN,
    ;

    internal object Serializer : KSerializer<PayoutOrderMethodDto> by unknownFallbackEnumSerializer(
        "PayoutOrderMethodDto", entries, UNKNOWN,
    )
}
