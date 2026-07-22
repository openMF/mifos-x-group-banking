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
 * Wire composite for the group-dashboard screen — the client-side fan-in of the FOUR parallel
 * COMP-GRP-001 companion calls (`get_group`, `get_viewer_role`, `get_group_corpus`,
 * `get_group_accounts`). NOT returned directly by any single endpoint; assembled by
 * `GroupRepository` from the 4 individual responses (mirrors the `GroupConfig` "constructed in
 * GroupRepository" precedent already documented on `api.yaml#dtos.GroupConfig`). Kept
 * `@Serializable` for round-trip test coverage and potential future direct-composite endpoint use.
 *
 * See API.md#dtos — GroupDashboardResponse.
 */
@Serializable
data class GroupDashboardResponseDto(
    @SerialName("group") val group: GroupDetailDto,
    @SerialName("viewerRole") val viewerRole: ViewerRoleInfoDto,
    @SerialName("corpus") val corpus: GroupCorpusDto,
    @SerialName("accounts") val accounts: GroupAccountsDto,
) {
    companion object {
        /** Bumped when this DTO shape changes (registry `version:` driven). See EC30. */
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for `get_group` — `GET /companion/groups/{groupId}` (COMP-GRP-001 read path).
 * **Deliberately NOT the same type as the group-list `GroupDto`** — see `GroupDetail.kt` kdoc
 * (domain layer) for the full field-shape divergence rationale; this endpoint returns
 * [fineractCenterId]/[cycleLengthMonths]/[meetingFrequency]/[overdueLoansCount]/[typeConfig] and
 * does NOT return `groupType`/`viewerRole`/`lastMeetingDate`/`healthIndicator`/`overdueRate`.
 *
 * See API.md#dtos — GroupDetail.
 */
@Serializable
data class GroupDetailDto(
    @SerialName("id") val id: String,
    @SerialName("fineractCenterId") val fineractCenterId: Long,
    @SerialName("name") val name: String,
    @SerialName("cycleNumber") val cycleNumber: Int,
    @SerialName("cycleLengthMonths") val cycleLengthMonths: Int,
    @SerialName("meetingFrequency") val meetingFrequency: String,
    @SerialName("memberCount") val memberCount: Int,
    @SerialName("overdueLoansCount") val overdueLoansCount: Int,
    @SerialName("status") val status: String,
    @SerialName("typeConfig") val typeConfig: GroupInstanceConfigDto,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for the PER-GROUP configured instance embedded on [GroupDetailDto.typeConfig].
 * **Naming-collision note (Station 3 — same class of issue already documented on
 * `SavingsTransactionDto.kt`)**: `idea-layer/screens/group-dashboard/api.yaml#dtos.GroupTypeConfig`
 * declares this SNAKE_CASE shape under the bare name `GroupTypeConfig` — the SAME name already
 * used by the CAMELCASE COMP-DT-003 seed-catalogue row (`GroupTypeConfigDto` in
 * `GroupTypeConfigDto.kt`, `typeSlug`/`displayName`/`tagline`/... — a generic group-TYPE
 * template). The two are NOT the same wire shape (this one has NO `displayName`/`tagline`/
 * `maxMembers`/`minMembers`; the catalogue row has no `shareValue`/`contributionAmount`/
 * `fineAmount`/`shareoutFormula`/`payoutOrderMethod`). `@SerialName`s below are literal
 * snake_case per `api.yaml#dtos.GroupTypeConfig` (this endpoint's field is sourced from the raw
 * `group_type_config` Fineract datatable row, unlike the companion-normalized camelCase
 * elsewhere on this feature) — Hard Rule 5 requires matching the DECLARING feature's contract
 * exactly, not the sibling feature's casing. Named [GroupInstanceConfigDto] to avoid the Kotlin
 * class-name clash with [GroupTypeConfigDto] while flagging the collision for Station 3.
 * [poolModel] reuses the SHARED [SavingsMechanismDto] enum (3 known values match `pool_model`'s
 * documented `ROTATING_PAYOUT | ACCUMULATING | NONE` exactly) and [groupType] reuses the SHARED
 * [GroupTypeSlugDto] enum (datatable-seeded long-form slug, matching COMP-DT-003 casing) — both
 * per the feature's explicit "reuse GroupTypeSlug/SavingsMechanism" instruction.
 *
 * See API.md#dtos — GroupInstanceConfig.
 */
@Serializable
data class GroupInstanceConfigDto(
    @SerialName("group_type") val groupType: GroupTypeSlugDto = GroupTypeSlugDto.UNKNOWN,
    @SerialName("pool_model") val poolModel: SavingsMechanismDto = SavingsMechanismDto.UNKNOWN,
    @SerialName("contribution_model") val contributionModel: GroupContributionModelDto = GroupContributionModelDto.UNKNOWN,
    @SerialName("shareout_formula") val shareoutFormula: String,
    @SerialName("payout_order_method") val payoutOrderMethod: String,
    @SerialName("share_value") val shareValue: Double,
    @SerialName("contribution_amount") val contributionAmount: Double,
    @SerialName("social_fund_enabled") val socialFundEnabled: Boolean,
    @SerialName("cycle_length_months") val cycleLengthMonths: Int,
    @SerialName("loan_multiplier") val loanMultiplier: Double,
    @SerialName("interest_rate") val interestRate: Double,
    @SerialName("fine_amount") val fineAmount: Double,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire enum for [GroupInstanceConfigDto.contributionModel] — distinct from the catalogue-level
 * [ContributionModeDto] (see [GroupInstanceConfigDto] kdoc). [UNKNOWN] fallback per T7/EC30 so a
 * server-added contribution model never crashes an old client.
 */
@Serializable(with = GroupContributionModelDto.Serializer::class)
enum class GroupContributionModelDto {
    @SerialName("FIXED_AMOUNT") FIXED_AMOUNT,
    @SerialName("SHARE_BASED_VARIABLE") SHARE_BASED_VARIABLE,
    @SerialName("FIXED_NEGOTIATED") FIXED_NEGOTIATED,
    @SerialName("UNKNOWN") UNKNOWN,
    ;

    internal object Serializer : KSerializer<GroupContributionModelDto> by unknownFallbackEnumSerializer(
        "GroupContributionModelDto", entries, UNKNOWN,
    )
}

/**
 * Wire DTO for `get_viewer_role` — `GET /companion/groups/{groupId}/my-role`. [role] reuses the
 * SHARED [ViewerRoleDto] enum (declared in `GroupDto.kt`) rather than introducing a new one — its
 * value-set exactly matches this endpoint's documented `role` values.
 *
 * See API.md#dtos — ViewerRoleInfo.
 */
@Serializable
data class ViewerRoleInfoDto(
    @SerialName("role") val role: ViewerRoleDto = ViewerRoleDto.UNKNOWN,
    @SerialName("memberId") val memberId: Long,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for `get_group_corpus` — `GET /companion/groups/{groupId}/corpus`. ACCUMULATING-type
 * fields are always populated; ROTATING_PAYOUT-type fields ([rotationPosition],
 * [nextRecipientName], [nextRecipientPosition]) are nullable, populated only for rotating pool
 * models.
 *
 * See API.md#dtos — GroupCorpus.
 */
@Serializable
data class GroupCorpusDto(
    @SerialName("currentBalance") val currentBalance: Double,
    @SerialName("openingBalance") val openingBalance: Double,
    @SerialName("totalContributionsThisCycle") val totalContributionsThisCycle: Double,
    @SerialName("totalLoansOutstanding") val totalLoansOutstanding: Double,
    @SerialName("lastUpdated") val lastUpdated: String,
    @SerialName("rotationPosition") val rotationPosition: Int? = null,
    @SerialName("nextRecipientName") val nextRecipientName: String? = null,
    @SerialName("nextRecipientPosition") val nextRecipientPosition: Int? = null,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for a single row of `get_group_accounts.recentActivity` (last 10 items backing the
 * `activity_feed_section`).
 *
 * See API.md#dtos — ActivityItem.
 */
@Serializable
data class ActivityItemDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: ActivityTypeDto = ActivityTypeDto.UNKNOWN,
    @SerialName("description") val description: String,
    @SerialName("amount") val amount: Double? = null,
    @SerialName("date") val date: String,
    @SerialName("memberName") val memberName: String? = null,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire enum for [ActivityItemDto.type] (`MEETING | DEPOSIT | LOAN | PENALTY | SHARE_OUT` per
 * `api.yaml`). [UNKNOWN] fallback per T7/EC30 so a server-added activity type never crashes an
 * old client.
 */
@Serializable
enum class ActivityTypeDto {
    @SerialName("MEETING") MEETING,
    @SerialName("DEPOSIT") DEPOSIT,
    @SerialName("LOAN") LOAN,
    @SerialName("PENALTY") PENALTY,
    @SerialName("SHARE_OUT") SHARE_OUT,
    @SerialName("UNKNOWN") UNKNOWN,
}

/**
 * Wire DTO for `get_group_accounts` — `GET /companion/groups/{groupId}/accounts`. Savings/loan
 * summary plus the last-10 [recentActivity] feed. [shareOutProjection] is populated only for
 * ACCUMULATING pool models.
 *
 * See API.md#dtos — GroupAccounts.
 */
@Serializable
data class GroupAccountsDto(
    @SerialName("savingsBalance") val savingsBalance: Double,
    @SerialName("loansOutstanding") val loansOutstanding: Double,
    @SerialName("activeLoanCount") val activeLoanCount: Int,
    @SerialName("shareOutProjection") val shareOutProjection: Double? = null,
    @SerialName("recentActivity") val recentActivity: List<ActivityItemDto> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

/**
 * Wire DTO for the CLIENT-SIDE-CONSTRUCTED savings/loan rule set (`api.yaml#dtos.GroupConfig`
 * says: "Not returned directly by any API endpoint; constructed in GroupRepository from the
 * GroupTypeConfig embedded in get_group response"). Kept `@Serializable` for round-trip test
 * coverage. [shareMin]/[shareMax]/[minimumDisbursementThreshold] have NO wire source anywhere in
 * `api.yaml` today (confirmed gap) — always decode/construct as `null` until the contract adds
 * them.
 *
 * See API.md#dtos — GroupConfig.
 */
@Serializable
data class GroupConfigDto(
    @SerialName("shareValue") val shareValue: Double? = null,
    @SerialName("shareMin") val shareMin: Int? = null,
    @SerialName("shareMax") val shareMax: Int? = null,
    @SerialName("contributionAmount") val contributionAmount: Double? = null,
    @SerialName("loanMultiplier") val loanMultiplier: Double? = null,
    @SerialName("interestRate") val interestRate: Double? = null,
    @SerialName("cycleLengthMonths") val cycleLengthMonths: Int,
    @SerialName("fineAmount") val fineAmount: Double? = null,
    @SerialName("minimumDisbursementThreshold") val minimumDisbursementThreshold: Double? = null,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}
