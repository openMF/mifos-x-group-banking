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
 * Domain composite for the group-dashboard screen (COMP-GRP-001 — `get_group` + `get_viewer_role`
 * + `get_group_corpus` + `get_group_accounts` fetched in parallel and fanned-in client-side; no
 * single endpoint returns this shape). Pure business shape, no wire concerns.
 *
 * [group] is deliberately typed [GroupDetail], NOT the group-list [Group] — see [GroupDetail]
 * kdoc for the flagged field-shape divergence. [GroupConfig] (the derived savings/loan rule set
 * consumed by `savings_summary_card`) is intentionally NOT a field here: per
 * `idea-layer/screens/group-dashboard/api.yaml#dtos.GroupConfig` it is "constructed in
 * GroupRepository" by merging [group].typeConfig ([GroupInstanceConfig]) with the SEPARATELY
 * fetched catalogue [GroupTypeConfig] (COMP-DT-003, `defaultLoanMultiplier` /
 * `defaultInterestRatePct`) — that cross-source merge is repository-layer business logic, out of
 * scope for this DTO/mapper layer. See API.md#models — GroupDashboard.
 */
data class GroupDashboard(
    val group: GroupDetail,
    val viewerRole: ViewerRoleInfo,
    val corpus: GroupCorpus,
    val accounts: GroupAccounts,
)

/**
 * Domain model for the group-dashboard identity/header section (`get_group` —
 * `/companion/groups/{groupId}`). **Deliberately NOT the same type as the group-list [Group]**
 * (COMP-GRP-001 `/companion/groups/mine` row) even though both are conceptually "a group" and
 * `idea-layer/screens/group-dashboard/ui.yaml#state_model` pseudocodes its state field as
 * `group: Group?` — the two wire shapes genuinely diverge: this endpoint returns
 * [fineractCenterId]/[cycleLengthMonths]/[meetingFrequency]/[overdueLoansCount]/[typeConfig]
 * (none of which [Group] carries) and does NOT return `groupType`/`viewerRole`/`lastMeetingDate`/
 * `healthIndicator`/`overdueRate` (all non-null, required fields on [Group] with no default) —
 * forcing [Group] reuse here would require fabricating values with no wire source. **Flagged for
 * the cross-feature repair station (Station 3)**: either widen [Group] to a superset covering
 * both endpoints, or keep the two identity shapes formally distinct as they are today.
 *
 * See API.md#models — GroupDetail.
 */
data class GroupDetail(
    val id: String,
    val fineractCenterId: Long,
    val name: String,
    val cycleNumber: Int,
    val cycleLengthMonths: Int,
    val meetingFrequency: String,
    val memberCount: Int,
    val overdueLoansCount: Int,
    val status: String,
    val typeConfig: GroupInstanceConfig,
)

/**
 * Domain model for the PER-GROUP configured savings/loan/payout rule instance embedded on
 * [GroupDetail.typeConfig] (`get_group` response). **Naming-collision note (same pattern as
 * `SavingsTransactionDto`'s documented three-way collision)**: `idea-layer/screens/group-dashboard/
 * api.yaml#dtos.GroupTypeConfig` declares this shape under the bare name `GroupTypeConfig` — the
 * SAME name already used by the COMP-DT-003 seed-catalogue row (`core.model.GroupTypeConfig`,
 * `typeSlug`/`displayName`/`tagline`/`savingsMechanism`/... — a generic TEMPLATE describing a
 * group TYPE) reused elsewhere per this feature's explicit instruction to reuse
 * `GroupTypeConfig`/`SavingsMechanism`/`GroupTypeSlug` "do NOT duplicate". The two shapes are NOT
 * interchangeable: this one describes THIS SPECIFIC group's configured instance values
 * (`shareValue`, `contributionAmount`, `fineAmount`, `shareoutFormula`, ...) sourced from the raw
 * `group_type_config` Fineract datatable row for THIS group, not the generic seed defaults. Named
 * [GroupInstanceConfig] here to avoid the Kotlin class-name clash while flagging the source
 * naming collision for Station 3 (`GroupTypeConfig` used for two different shapes across
 * `group-type-picker`/`group-dashboard`). [poolModel] reuses the SHARED [SavingsMechanism] enum
 * (its 3 known values match `pool_model`'s declared `ROTATING_PAYOUT | ACCUMULATING | NONE`
 * exactly) and [groupType] reuses the SHARED [GroupTypeSlug] enum (the datatable-seeded slug,
 * long-form — matching COMP-DT-003's catalogue casing, not group-list's short-form
 * `CBO`/`BURIAL`) — both per the explicit reuse instruction. [contributionModel] is a NEW enum
 * ([GroupContributionModel]) because its 3-value set (`FIXED_AMOUNT` / `SHARE_BASED_VARIABLE` /
 * `FIXED_NEGOTIATED`) does not match the existing [ContributionMode] enum's set
 * (`SHARE_BASED_VARIABLE` / `FIXED` / `MINIMAL`) — only `SHARE_BASED_VARIABLE` overlaps.
 *
 * See API.md#models — GroupInstanceConfig.
 */
data class GroupInstanceConfig(
    val groupType: GroupTypeSlug,
    val poolModel: SavingsMechanism,
    val contributionModel: GroupContributionModel,
    val shareoutFormula: String,
    val payoutOrderMethod: String,
    val shareValue: Double,
    val contributionAmount: Double,
    val socialFundEnabled: Boolean,
    val cycleLengthMonths: Int,
    val loanMultiplier: Double,
    val interestRate: Double,
    val fineAmount: Double,
)

/**
 * Domain enum for how a group's members contribute at the PER-GROUP-INSTANCE level (distinct
 * from the catalogue-level [ContributionMode] — see [GroupInstanceConfig] kdoc for why the two
 * are not unified). [UNKNOWN] absorbs any wire value this client build does not yet recognize.
 * See API.md#models — GroupContributionModel.
 */
enum class GroupContributionModel {
    FIXED_AMOUNT,
    SHARE_BASED_VARIABLE,
    FIXED_NEGOTIATED,
    UNKNOWN,
}

/**
 * Domain model for the `get_viewer_role` response (`/companion/groups/{groupId}/my-role`) —
 * resolves the authenticated user's role + member id in THIS specific group from
 * `dt_member_role`. [role] reuses the SHARED [ViewerRole] enum (already declared on `Group.kt`
 * for the group-list row) — its value-set (`ORGANIZER`/`MEMBER`/`TREASURER`/`CHAIRPERSON`/
 * `SECRETARY`/`UNKNOWN`) exactly matches this endpoint's documented `role` values, so no new
 * enum is introduced.
 *
 * See API.md#models — ViewerRoleInfo.
 */
data class ViewerRoleInfo(
    val role: ViewerRole,
    val memberId: Long,
)

/**
 * Domain model for the `get_group_corpus` response (`/companion/groups/{groupId}/corpus`).
 * ACCUMULATING-type fields ([currentBalance], [openingBalance], [totalContributionsThisCycle],
 * [totalLoansOutstanding]) are always populated; ROTATING_PAYOUT-type fields ([rotationPosition],
 * [nextRecipientName], [nextRecipientPosition]) are populated only when the group's pool model is
 * ROTATING_PAYOUT (mutually exclusive with the shareout-projection axis, per
 * `idea-layer/screens/group-dashboard/ui.yaml` corpus_card vs rotation_card visibility rules).
 * [lastUpdated] is kept as a raw [String] rather than a parsed date/time type — the wire format is
 * unconfirmed in `api.yaml` (may be a full timestamp, not a bare date) — matching this codebase's
 * existing precedent of keeping unconfirmed-precision date/time wire fields as `String`
 * (`MemberDashboard.nextRecipientEta`).
 *
 * See API.md#models — GroupCorpus.
 */
data class GroupCorpus(
    val currentBalance: Double,
    val openingBalance: Double,
    val totalContributionsThisCycle: Double,
    val totalLoansOutstanding: Double,
    val lastUpdated: String,
    val rotationPosition: Int?,
    val nextRecipientName: String?,
    val nextRecipientPosition: Int?,
)

/**
 * Domain model for a single recent-activity row on `get_group_accounts.recentActivity` (last 10
 * items backing the `activity_feed_section` on the group-dashboard screen). [date] is kept as a
 * raw [String] — same unconfirmed-precision rationale as [GroupCorpus.lastUpdated].
 *
 * See API.md#models — ActivityItem.
 */
data class ActivityItem(
    val id: String,
    val type: ActivityType,
    val description: String,
    val amount: Double?,
    val date: String,
    val memberName: String?,
)

/**
 * Domain enum for the kind of a [ActivityItem] row. [UNKNOWN] absorbs any wire value this client
 * build does not yet recognize.
 * See API.md#models — ActivityType.
 */
enum class ActivityType {
    MEETING,
    DEPOSIT,
    LOAN,
    PENALTY,
    SHARE_OUT,
    UNKNOWN,
}

/**
 * Domain model for the `get_group_accounts` response (`/companion/groups/{groupId}/accounts`) —
 * savings + loan account summary plus the last-10 [recentActivity] feed. [shareOutProjection] is
 * populated only for ACCUMULATING pool models.
 *
 * See API.md#models — GroupAccounts.
 */
data class GroupAccounts(
    val savingsBalance: Double,
    val loansOutstanding: Double,
    val activeLoanCount: Int,
    val shareOutProjection: Double?,
    val recentActivity: List<ActivityItem>,
)

/**
 * Domain model for the CLIENT-SIDE-CONSTRUCTED savings/loan rule set consumed by the
 * `savings_summary_card` bindings (`config.shareValue`, `config.shareMin`, `config.shareMax`,
 * `config.contributionAmount`, `config.loanMultiplier`, `config.interestRate`). Per
 * `idea-layer/screens/group-dashboard/api.yaml#dtos.GroupConfig`: "Not returned directly by any
 * API endpoint; constructed in GroupRepository from the GroupTypeConfig embedded in get_group
 * response." That construction is a REPOSITORY-layer concern spanning two wire sources
 * ([GroupInstanceConfig] for [shareValue]/[contributionAmount]/[fineAmount]/[cycleLengthMonths],
 * PLUS the separately-fetched catalogue `GroupTypeConfig.defaultLoanMultiplier`/
 * `defaultInterestRatePct` for [loanMultiplier]/[interestRate]) — out of scope for this
 * DTO/mapper layer, which only exposes the shape + a partial single-source derivation helper (see
 * `GroupDashboardMappers.kt`). [shareMin]/[shareMax]/[minimumDisbursementThreshold] currently
 * have NO wire source anywhere in `api.yaml` (confirmed gap — `ui.yaml` demo_data shows them but
 * no `api.yaml` field backs them); they map to `null` until the backend/api.yaml contract adds
 * them.
 *
 * See API.md#models — GroupConfig.
 */
data class GroupConfig(
    val shareValue: Double?,
    val shareMin: Int?,
    val shareMax: Int?,
    val contributionAmount: Double?,
    val loanMultiplier: Double?,
    val interestRate: Double?,
    val cycleLengthMonths: Int,
    val fineAmount: Double?,
    val minimumDisbursementThreshold: Double?,
)
